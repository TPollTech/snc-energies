package com.snc.energies.blockentity;

import com.snc.energies.registry.SncBlockEntities;
import com.snc.energies.menu.WoodStoveMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

/** A wood-fired thermoelectric stove sharing the tested generator storage and burn cycle. */
public class WoodStoveBlockEntity extends CoalGeneratorBlockEntity {
    public static final long GENERATION_RATE = 80L;
    public WoodStoveBlockEntity(BlockPos pos, BlockState state) {
        super(SncBlockEntities.WOOD_STOVE, pos, state);
    }
    @Override protected void setLit(boolean lit, BlockState state) {
        super.setLit(lit,state);
        if (state.getValue(com.snc.energies.block.WoodStoveBlock.ASSEMBLED)) {
            for (int i=1;i<com.snc.energies.block.WoodStoveBlock.CELLS.length;i++) {
                BlockPos pos=com.snc.energies.block.WoodStoveBlock.cell(worldPosition,state.getValue(com.snc.energies.block.MachineBlock.FACING),i);
                if (!level.hasChunkAt(pos)) continue;
                BlockState part=level.getBlockState(pos);
                if(com.snc.energies.block.WoodStoveBlock.owns(part,state,i)) level.setBlock(pos,part.setValue(com.snc.energies.block.CoalGeneratorBlock.LIT,lit),2);
            }
        }
    }
    public static int woodBurnTicks(ItemStack stack) {
        if (stack.is(com.snc.energies.registry.SncItems.BIOMASS_BRIQUETTE)) return 500;
        if (stack.is(com.snc.energies.registry.SncItems.RICE_HUSK)) return 100;
        if (stack.is(com.snc.energies.registry.SncItems.SAWDUST)) return 100;
        if (stack.is(com.snc.energies.registry.SncItems.VEGETABLE_OIL)) return 400;
        int biomass = com.snc.energies.compat.AdventuresCompatibility.biomassBurnTicks(stack);
        if (biomass > 0) return biomass;
        if (stack.is(ItemTags.LOGS_THAT_BURN)) return 600;
        if (stack.is(ItemTags.PLANKS) && !stack.is(Items.CRIMSON_PLANKS) && !stack.is(Items.WARPED_PLANKS)) return 300;
        if (stack.is(Items.STICK)) return 100;
        return 0;
    }
    @Override protected int fuelBurnTicks(ItemStack stack) { return woodBurnTicks(stack); }
    @Override protected long generationRate() { return GENERATION_RATE; }
    @Override protected String defaultLangKey() { return "block.snc_energies.wood_stove"; }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new WoodStoveMenu(id, inventory, this);
    }
    public static class FuelSlot extends Slot {
        public FuelSlot(Container container, int index, int x, int y) { super(container,index,x,y); }
        @Override public boolean mayPlace(ItemStack stack) { return woodBurnTicks(stack) > 0; }
    }
}
