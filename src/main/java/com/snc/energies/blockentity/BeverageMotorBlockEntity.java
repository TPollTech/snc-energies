package com.snc.energies.blockentity;

import java.util.List;

import com.snc.energies.compat.AdventuresAutomation;
import com.snc.energies.compat.AdventuresBeverages;
import com.snc.energies.energy.EnergyStorage;
import com.snc.energies.energy.SimpleEnergyStorage;
import com.snc.energies.registry.SncBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The machine engine: a powered dock that motorizes one Adventures machine.
 *
 * The engine docks against the machine in front of it (its facing side),
 * reaches it through the public {@link Container} interface — the same surface
 * hoppers use, no fake player — and then, on a fixed cadence: drains finished
 * outputs into its collect buffer and feeds the input slot from its feed
 * buffer, all through {@link AdventuresAutomation} so every move is
 * all-or-nothing. The machine keeps every one of its own rules (timing,
 * water, output slots): the engine only supplies and collects.
 *
 * Without energy it pauses untouched — a batch already running inside the
 * docked machine still finishes on the machine's own tick, and nothing is
 * lost or duplicated by the pause.
 */
public class BeverageMotorBlockEntity extends MachineBlockEntity implements net.minecraft.world.WorldlyContainer {
    public static final long CAPACITY = 10_000L;
    /** Energy per engine step (one feed or harvest pass). */
    public static final long STEP_COST = 80L;
    /** Ticks between engine steps. */
    public static final int STEP_INTERVAL = 40;

    /** Buffer slot 0 feeds the machine; slot 1 collects its outputs. */
    public static final int FEED = 0;
    public static final int COLLECT = 1;

    private final SimpleEnergyStorage energy = new SimpleEnergyStorage(CAPACITY);
    private int cooldown;

    public BeverageMotorBlockEntity(BlockPos pos, BlockState state) {
        super(SncBlockEntities.BEVERAGE_MOTOR, pos, state, 2);
    }

    private record Docked(AdventuresBeverages.Step step, AdventuresBeverages.Layout layout, Container machine) {}

    /** The docked machine in front of the engine, matched against the beverage contract. */
    private Docked docked() {
        if (level == null || !(level instanceof net.minecraft.server.level.ServerLevel server)) return null;
        Direction face = getBlockState().getValue(com.snc.energies.block.MachineBlock.FACING);
        BlockPos machinePos = worldPosition.relative(face);
        if (!level.isLoaded(machinePos) || !(level.getBlockEntity(machinePos) instanceof Container container)) return null;
        if (!(level.getBlockEntity(machinePos) instanceof net.minecraft.world.level.block.entity.BlockEntity machineEntity)) return null;
        AdventuresBeverages.Layout layout = AdventuresBeverages.layoutOf(machineEntity);
        if (layout == null) return null;
        for (AdventuresBeverages.Step step : AdventuresBeverages.steps()) {
            String dockedId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(machinePos).getBlock()).getPath();
            if (step.machine().equals(dockedId)) return new Docked(step, layout, container);
        }
        return null;
    }

    /** True when the buffer holds something some motorized chain accepts. */
    public boolean acceptsForFeed(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (AdventuresBeverages.Step step : AdventuresBeverages.steps()) {
            if (ItemStack.isSameItemSameComponents(stack, step.sample())) return true;
        }
        return false;
    }

    @Override
    public void serverTick() {
        if (!canTick() || cooldown > 0) {
            if (cooldown > 0 && --cooldown == 0) setChanged();
            return;
        }
        if (energy.getEnergy() < STEP_COST) return; // pauses without touching anything
        Docked target = docked();
        if (target == null) return;

        boolean drained = AdventuresAutomation.harvest(target.machine(), target.layout().outputSlot(),
                target.layout().extraSlot(), this, COLLECT);
        boolean fed = AdventuresAutomation.feed(target.machine(), target.layout().inputSlot(),
                this, FEED, target.step().inputCount());
        if (drained | fed) {
            energy.extract(STEP_COST, false);
            cooldown = STEP_INTERVAL;
            setChanged();
        }
    }

    @Override
    public EnergyStorage getEnergyStorage(Direction side) {
        return energy;
    }

    @Override
    public long getEnergy() {
        return energy.getEnergy();
    }

    @Override
    public long getCapacity() {
        return CAPACITY;
    }

    @Override
    public int getProgress() {
        return STEP_INTERVAL - cooldown; // fills up toward the next engine step
    }

    @Override
    public int getMaxProgress() {
        return STEP_INTERVAL;
    }

    @Override
    protected String defaultLangKey() {
        return "block.snc_energies.beverage_motor";
    }

    /** Hoppers insert machine inputs through any side and pull collected outputs from below. */
    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{FEED, COLLECT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == FEED && acceptsForFeed(stack) && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == COLLECT;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.save(output);
        output.putInt("Cooldown", cooldown);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.load(input);
        cooldown = Math.clamp(input.getInt("Cooldown").orElse(0), 0, STEP_INTERVAL);
    }
}
