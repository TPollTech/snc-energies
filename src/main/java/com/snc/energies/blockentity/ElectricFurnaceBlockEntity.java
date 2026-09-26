package com.snc.energies.blockentity;

import java.util.Optional;

import com.snc.energies.energy.EnergyStorage;
import com.snc.energies.energy.SimpleEnergyStorage;
import com.snc.energies.registry.SncBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Smelts items using SNC energy (40 E/t, 2x faster than the vanilla furnace).
 */
public class ElectricFurnaceBlockEntity extends MachineBlockEntity implements net.minecraft.world.WorldlyContainer {
	public static final long CAPACITY = 10_000L;
	public static final long ENERGY_PER_TICK = 40L;
	public static final int SMELT_TICKS = 100; // vanilla is 200

	private final SimpleEnergyStorage energy = new SimpleEnergyStorage(CAPACITY);
	private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> quickCheck =
			RecipeManager.createCheck(RecipeType.SMELTING);
	private int progress;

	public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(SncBlockEntities.ELECTRIC_FURNACE, pos, state, 2);
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

		SmeltingRecipe recipe = null;
		if (!input.isEmpty() && level instanceof ServerLevel serverLevel) {
			Optional<RecipeHolder<SmeltingRecipe>> found = quickCheck.getRecipeFor(new SingleRecipeInput(input), serverLevel);
			recipe = found.map(RecipeHolder::value).orElse(null);
		}

		ItemStack result = recipe == null ? ItemStack.EMPTY : recipe.assemble(new SingleRecipeInput(input));
		boolean canProcess = !result.isEmpty() && energy.getEnergy() >= ENERGY_PER_TICK && outputFits(output, result);

		if (canProcess) {
			progress++;
			energy.extract(ENERGY_PER_TICK, false);
			if (progress >= SMELT_TICKS) {
				progress = 0;
				if (output.isEmpty()) {
					setItem(1, result);
				} else {
					output.grow(result.getCount());
				}
				input.shrink(1);
				setChanged();
			}
		} else if (progress > 0) {
			progress = Math.max(0, progress - 2); // regresses slowly when starved
		}

		boolean lit = canProcess;
		if (lit != wasLit) {
			setLit(lit, state);
		}
		if (canProcess) {
			setChanged();
		}
	}

	private static boolean outputFits(ItemStack output, ItemStack result) {
		return output.isEmpty() || (ItemStack.isSameItemSameComponents(output, result)
				&& output.getCount() + result.getCount() <= output.getMaxStackSize());
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
		return SMELT_TICKS;
	}

	@Override
	protected String defaultLangKey() {
		return "block.snc_energies.electric_furnace";
	}

	/** Hoppers insert ingredients from any side and pull finished smelts from any side. */
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
