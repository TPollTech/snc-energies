package com.snc.energies.block;

import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

/** Vanilla growth and bonemeal rules with a late-bound seed registration. */
public final class RegionalCropBlock extends CropBlock {
    private final Supplier<Item> seed;
    public RegionalCropBlock(Properties properties, Supplier<Item> seed) {
        super(properties);
        this.seed = seed;
    }
    @Override public ItemLike getBaseSeedId() { return seed.get(); }
}
