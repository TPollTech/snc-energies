package com.snc.energies.blockentity;

import com.snc.energies.energy.EnergyStorage;
import com.snc.energies.energy.EnergyTransfer;
import com.snc.energies.energy.SimpleEnergyStorage;
import com.snc.energies.registry.SncBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Burns coal/charcoal and pushes energy to neighbours at 160 E/t.
 */
public class CoalGeneratorBlockEntity extends MachineBlockEntity implements net.minecraft.world.WorldlyContainer {
	public static final long CAPACITY = 64_000L;
	public static final long MAX_OUTPUT = 160L;
	public static final int COAL_BURN_TICKS = 1600;

	private final SimpleEnergyStorage energy = new SimpleEnergyStorage(CAPACITY);
	private int burnTimeRemaining;
	private int burnTimeTotal;

	public CoalGeneratorBlockEntity(BlockPos pos, BlockState state) {
		this(SncBlockEntities.COAL_GENERATOR, pos, state);
	}

	protected CoalGeneratorBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state, 1);
	}

	protected int fuelBurnTicks(ItemStack stack) { return burnTicksFor(stack); }
	protected long generationRate() { return MAX_OUTPUT; }
	@Override
	public void serverTick() {
		if (!canTick()) {
			return;
		}
		BlockState state = getBlockState();
		boolean lit = state.getValue(BlockStateProperties.LIT);
		boolean changed = false;

		// Consume one fuel item only when there is room for generation.
		if (burnTimeRemaining <= 0 && !energy.isFull()) {
			ItemStack fuel = getItem(0);
			int ticks = fuelBurnTicks(fuel);
			if (ticks > 0) {
				fuel.shrink(1);
				burnTimeRemaining = ticks;
				burnTimeTotal = ticks;
				changed = true;
			}
		}
		boolean burning = burnTimeRemaining > 0 && !energy.isFull();
		if (burning) {
			energy.insert(generationRate(), false);
			burnTimeRemaining--;
			changed = true;
		}
		if (burning != lit) {
			setLit(burning, state);
		}

		// always push the buffer out
		long pushed = EnergyTransfer.distribute(level, worldPosition, energy, generationRate());
		if (pushed > 0) {
			changed = true;
		}
		if (changed) {
			setChanged();
		}
	}

	public static boolean isFuelItem(ItemStack stack) {
		return burnTicksFor(stack) > 0;
	}

	private static int burnTicksFor(ItemStack stack) {
		if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
			return COAL_BURN_TICKS;
		}
		if (stack.is(Items.COAL_BLOCK)) {
			return COAL_BURN_TICKS * 10;
		}
		return 0;
	}

	/** Fuel slots only accept burnables the generator can use. */
	public static class FuelSlot extends Slot {
		public FuelSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return isFuelItem(stack);
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
	public int getBurnTime() {
		return burnTimeRemaining;
	}

	public int getBurnTimeTotal() {
		return burnTimeTotal;
	}

	@Override
	protected String defaultLangKey() {
		return "block.snc_energies.coal_generator";
	}

	/** Automation contract: fuel goes in from any side; nothing is ever extracted. */
	@Override public int[] getSlotsForFace(net.minecraft.core.Direction side) { return new int[]{0}; }

	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
		return slot == 0;
	}

	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
		return false;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		energy.save(output);
		output.putInt("BurnTime", burnTimeRemaining);
		output.putInt("BurnTotal", burnTimeTotal);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		energy.load(input);
		burnTimeRemaining = input.getInt("BurnTime").orElse(0);
		burnTimeTotal = input.getInt("BurnTotal").orElse(0);
	}
}
