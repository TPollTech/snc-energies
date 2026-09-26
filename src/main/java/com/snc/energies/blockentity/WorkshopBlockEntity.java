package com.snc.energies.blockentity;

import com.snc.energies.energy.EnergyStorage;
import com.snc.energies.registry.*;
import com.snc.energies.menu.WorkshopMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A hand-powered workshop. Both outputs are committed atomically. */
public final class WorkshopBlockEntity extends MachineBlockEntity implements WorldlyContainer {
    public static final int MAX_WORK = 480;
    private int work;
    private int progress;
    private String activeRecipe = "";
    private long lastCrankTick = Long.MIN_VALUE;
    public WorkshopBlockEntity(BlockPos pos, BlockState state) { super(SncBlockEntities.WORKSHOP, pos, state, 3); }
    public boolean isPress() { return getBlockState().is(SncBlocks.SEED_PRESS); }
    public WorkshopRecipes.Process recipe() { return WorkshopRecipes.find(isPress(), getItem(0)); }
    public int work() { return work; }
    public boolean crank() {
        if (level == null || level.isClientSide() || level.getGameTime() == lastCrankTick || work >= MAX_WORK) return false;
        lastCrankTick = level.getGameTime();
        work = Math.min(MAX_WORK, work + 120);
        setChanged();
        return true;
    }
    private boolean fits(int slot, ItemStack result) {
        ItemStack existing = getItem(slot);
        return result.isEmpty() || (existing.isEmpty() ? result.getCount() <= result.getMaxStackSize()
                : ItemStack.isSameItemSameComponents(existing, result) && existing.getCount() + result.getCount() <= existing.getMaxStackSize());
    }
    /** Stable status codes shared with the menu: ingredient, work, running, output. */
    public int status() {
        var recipe = recipe();
        if (recipe == null || getItem(0).getCount() < recipe.count()) return 0;
        if (!fits(1, recipe.product()) || !fits(2, recipe.byproduct())) return 3;
        return work == 0 ? 1 : 2;
    }
    @Override public void serverTick() {
        if (!canTick()) return;
        var recipe = recipe();
        String current = recipe == null ? "" : recipe.id();
        if (!activeRecipe.equals(current)) { progress = 0; activeRecipe = current; setChanged(); }
        if (status() != 2) return;
        work--;
        progress++;
        if (progress >= recipe.ticks()) {
            getItem(0).shrink(recipe.count());
            addOutput(1, recipe.product());
            addOutput(2, recipe.byproduct());
            progress = 0;
        }
        setChanged();
    }
    private void addOutput(int slot, ItemStack result) {
        if (result.isEmpty()) return;
        if (getItem(slot).isEmpty()) setItem(slot, result); else getItem(slot).grow(result.getCount());
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && WorkshopRecipes.find(isPress(), stack) != null; }
    @Override public int[] getSlotsForFace(Direction side) { return side == Direction.DOWN ? new int[]{1, 2} : new int[]{0}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return side != Direction.DOWN && canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return side == Direction.DOWN && slot > 0; }
    @Override public EnergyStorage getEnergyStorage(Direction side) { return null; }
    @Override public int getProgress() { return progress; }
    @Override public int getMaxProgress() { var r = recipe(); return r == null ? 1 : r.ticks(); }
    @Override protected String defaultLangKey() { return isPress() ? "block.snc_energies.seed_press" : "block.snc_energies.grain_mill"; }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new WorkshopMenu(id, inventory, this); }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide()) { Containers.dropContents(level, worldPosition, this); clearContent(); }
        super.preRemoveSideEffects(pos, state);
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Work", work); output.putInt("Progress", progress); output.putString("ActiveRecipe", activeRecipe);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        work = Math.clamp(input.getIntOr("Work", 0), 0, MAX_WORK);
        activeRecipe = input.getStringOr("ActiveRecipe", "");
        progress = Math.clamp(input.getIntOr("Progress", 0), 0, getMaxProgress() - 1);
    }
}
