package com.snc.energies.blockentity;

import com.snc.energies.energy.EnergyStorage;
import net.minecraft.world.MenuProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Base for all SNC machines: containers, energy exposure, custom name and
 * native block entity ticking.
 */
public abstract class MachineBlockEntity extends BlockEntity implements EnergyProvider, Container, Nameable, MenuProvider {
	protected final NonNullList<ItemStack> items;


	protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
		super(type, pos, state);
		this.items = NonNullList.withSize(slots, ItemStack.EMPTY);
	}

	/** Called by the native block ticker for loaded machines, server side only. */
	public abstract void serverTick();

	@Override
	public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,
			net.minecraft.world.entity.player.Inventory inventory, Player player) {
		if (this instanceof CoalGeneratorBlockEntity generator) return new com.snc.energies.menu.CoalGeneratorMenu(id, inventory, generator);
		if (this instanceof ElectricFurnaceBlockEntity furnace) return new com.snc.energies.menu.ElectricFurnaceMenu(id, inventory, furnace);
		if (this instanceof CrusherBlockEntity crusher) return new com.snc.energies.menu.CrusherMenu(id, inventory, crusher);
		return null;
	}

	// ---- energy data accessors used by menus ----

	public long getEnergy() {
		return 0;
	}

	public long getCapacity() {
		return 0;
	}

	public int getProgress() {
		return 0;
	}

	public int getMaxProgress() {
		return 1;
	}

	public int getBurnTime() {
		return 0;
	}

	// ---- container ----

	@Override
	public int getContainerSize() {
		return items.size();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : items) {
			if (!stack.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		return items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
		if (!removed.isEmpty()) {
			setChanged();
		}
		return removed;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(items, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack);
		if (stack.getCount() > getMaxStackSize()) {
			stack.setCount(getMaxStackSize());
		}
		setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return this.level != null && this.level.getBlockEntity(this.worldPosition) == this
				&& player.distanceToSqr(
						this.worldPosition.getX() + 0.5,
						this.worldPosition.getY() + 0.5,
						this.worldPosition.getZ() + 0.5) <= 64.0;
	}

	@Override
	public void clearContent() {
		items.clear();
	}

	// ---- naming ----

	@Override
	public Component getName() {
		return Component.translatable(defaultLangKey());
	}

	@Override
	public Component getCustomName() {
		return null;
	}

	@Override
	public Component getDisplayName() {
		return getName();
	}

	protected abstract String defaultLangKey();

	// ---- persistence (MC 26.2 ValueInput/ValueOutput API) ----

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		ContainerHelper.loadAllItems(input, items);
	}

	// ---- helpers ----

	/** Sets the machine lit state on the block, syncing to clients. */
	protected void setLit(boolean lit, BlockState state) {
		if (state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) != lit) {
			if (level != null) {
				level.setBlock(worldPosition, state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, lit), 3);
			}
		}
	}

	/** True while the chunk this machine lives in can tick. */
	protected boolean canTick() {
		return level != null && !isRemoved() && level.shouldTickBlocksAt(worldPosition);
	}
}
