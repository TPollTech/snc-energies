package com.snc.energies.blockentity;

import com.snc.energies.compat.AdventuresUvBridge;
import com.snc.energies.energy.EnergyStorage;
import com.snc.energies.energy.SimpleEnergyStorage;
import com.snc.energies.registry.SncBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The opt-in electric variant of the Adventures UV lamp.
 *
 * Contract (PROGRESSAO.md): the original lamp keeps working untouched; this
 * variant is an explicit choice, with its own SNC energy buffer and its own
 * independent redstone gate. It never toggles vanilla game state — the crop
 * maturation itself stays in the Adventures crop's own logic, which checks
 * for a lit UV lamp nearby; this lamp simply is one, without needing the
 * daylight trick or redstone-pulse rituals of the original.
 */
public class ElectricUvLampBlockEntity extends MachineBlockEntity {
    public static final long CAPACITY = 4_000L;
    /** Energy per tick while the lamp is emitting (1 lot per second, 1.200 E). */
    public static final long DRAIN_PER_TICK = 20L;
    /** Operation cycles (crop random ticks) granted per energy lot. */
    public static final int CYCLES_PER_LOT = 600;
    public static final long LOT_COST = 1_200L;

    private final SimpleEnergyStorage energy = new SimpleEnergyStorage(CAPACITY);
    private int cyclesLeft;
    private int lit;

    public ElectricUvLampBlockEntity(BlockPos pos, BlockState state) {
        super(SncBlockEntities.ELECTRIC_UV_LAMP, pos, state, 0);
    }

    public boolean isEmitting() {
        return lit > 0;
    }

    @Override
    public void serverTick() {
        if (!canTick()) return;
        if (cyclesLeft > 0 && energy.getEnergy() >= DRAIN_PER_TICK) {
            energy.extract(DRAIN_PER_TICK, false);
            cyclesLeft--;
            if (lit != 1) setLitState(true);
            return;
        }
        if (cyclesLeft == 0 && energy.getEnergy() >= LOT_COST) {
            energy.extract(LOT_COST, false);
            cyclesLeft = CYCLES_PER_LOT;
            if (lit != 1) setLitState(true);
            setChanged();
            return;
        }
        if (lit != 0) setLitState(false);
    }

    private void setLitState(boolean emitting) {
        lit = emitting ? 1 : 0;
        if (level != null) {
            level.setBlock(worldPosition, getBlockState().setValue(
                    com.snc.energies.block.ElectricUvLampBlock.EMITTING, emitting), 3);
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
        return cyclesLeft;
    }

    @Override
    public int getMaxProgress() {
        return CYCLES_PER_LOT;
    }

    @Override
    protected String defaultLangKey() {
        return "block.snc_energies.electric_uv_lamp";
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.save(output);
        output.putInt("CyclesLeft", cyclesLeft);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.load(input);
        cyclesLeft = Math.clamp(input.getInt("CyclesLeft").orElse(0), 0, CYCLES_PER_LOT);
    }
}
