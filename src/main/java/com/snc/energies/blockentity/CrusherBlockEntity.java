package com.snc.energies.blockentity;

import com.snc.energies.energy.EnergyStorage;
import com.snc.energies.energy.SimpleEnergyStorage;
import com.snc.energies.registry.SncBlockEntities;
import com.snc.energies.registry.SncItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Crushes ores into dust: 1 ore -> 2 dust (Mekanism style).
 * Recipes are hard-mapped for v0.1.0 (see {@link #crushResult}).
 */
public class CrusherBlockEntity extends MachineBlockEntity implements net.minecraft.world.WorldlyContainer {
	public static final long CAPACITY = 10_000L;
	public static final long ENERGY_PER_TICK = 30L;
	public static final int CRUSH_TICKS = 150;

	private final SimpleEnergyStorage energy = new SimpleEnergyStorage(CAPACITY);
	private int progress;

	public CrusherBlockEntity(BlockPos pos, BlockState state) {
		super(SncBlockEntities.CRUSHER, pos, state, 2);
	}

	@Override
	public void serverTick() {
		if (!canTick()) {
			return;
		}
		BlockState state = getBlockState();
		boolean wasLit = state.getValue(BlockStateProperties.LIT);

		ItemStack input = getItem(0);
		ItemStack output = getItem(1);

		ItemStack result = crushResult(input);
		boolean canProcess = !result.isEmpty()
				&& energy.getEnergy() >= ENERGY_PER_TICK
				&& (output.isEmpty()
						|| (output.getItem() == result.getItem() && output.getCount() + result.getCount() <= output.getMaxStackSize()));

		if (canProcess) {
			progress++;
			energy.extract(ENERGY_PER_TICK, false);
			if (progress >= CRUSH_TICKS) {
				progress = 0;
				if (output.isEmpty()) {
					setItem(1, result.copy());
				} else {
					output.grow(result.getCount());
				}
				input.shrink(1);
				setChanged();
			}
		} else if (progress > 0) {
			progress = Math.max(0, progress - 2);
		}

		boolean lit = canProcess;
		if (lit != wasLit) {
			setLit(lit, state);
		}
		if (canProcess) {
			setChanged();
		}
	}

	/** Hard recipe map for v0.1.0. */
	private static ItemStack crushResult(ItemStack input) {
		if (input.isEmpty()) {
			return ItemStack.EMPTY;
		}
		if (input.is(Items.RAW_IRON)) return new ItemStack(SncItems.IRON_DUST, 2);
		if (input.is(SncItems.RAW_TIN)) return new ItemStack(SncItems.TIN_DUST, 2);
		if (input.is(SncItems.RAW_VOLTAITE)) {
			return new ItemStack(SncItems.VOLTAITE_DUST, 2);
		}
		if (input.is(Items.RAW_COPPER)) {
			return new ItemStack(SncItems.COPPER_DUST, 2);
		}
		if (input.is(Items.RAW_GOLD)) {
			return new ItemStack(SncItems.GOLD_DUST, 2);
		}
		return ItemStack.EMPTY;
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
		return progress;
	}

	@Override
	public int getMaxProgress() {
		return CRUSH_TICKS;
	}

	@Override
	protected String defaultLangKey() {
		return "block.snc_energies.crusher";
	}

	/** Hoppers insert ore from any side and pull dust from any side. */
	@Override public int[] getSlotsForFace(net.minecraft.core.Direction side) { return new int[]{0, 1}; }

	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
		return slot == 0 && canPlaceItem(slot, stack);
	}

	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
		return slot == 1;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		energy.save(output);
		output.putInt("Progress", progress);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		energy.load(input);
		progress = input.getInt("Progress").orElse(0);
	}
}
