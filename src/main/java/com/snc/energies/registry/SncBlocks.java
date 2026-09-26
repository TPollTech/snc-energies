package com.snc.energies.registry;

import com.snc.energies.SncEnergies;
import com.snc.energies.block.CableBlock;
import com.snc.energies.block.CoalGeneratorBlock;
import com.snc.energies.block.CrusherBlock;
import com.snc.energies.block.ElectricFurnaceBlock;
import com.snc.energies.block.EnergyCubeBlock;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * Central block registry (AGENTS.md rule: registration lives in one place only).
 */
public final class SncBlocks {
    public static final java.util.Map<IndustryKind, com.snc.energies.block.IndustrialBlock> INDUSTRY = registerIndustry();
    private static java.util.Map<IndustryKind, com.snc.energies.block.IndustrialBlock> registerIndustry() {
        var blocks = new java.util.EnumMap<IndustryKind, com.snc.energies.block.IndustrialBlock>(IndustryKind.class);
        for (var kind : IndustryKind.values()) blocks.put(kind, (com.snc.energies.block.IndustrialBlock)register(kind.id,
            p -> new com.snc.energies.block.IndustrialBlock(p, kind), blkProps(4, 8).noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE)));
        return java.util.Collections.unmodifiableMap(blocks);
    }
    public static final Block STEAM_PIPE = register("steam_pipe", com.snc.energies.block.SteamPipeBlock::new,
        Block.Properties.ofFullCopy(Blocks.GLASS).strength(1).noOcclusion());
    public static final Block ITEM_PIPE = register("item_pipe", com.snc.energies.block.ItemPipeBlock::new,
        Block.Properties.ofFullCopy(Blocks.GLASS).strength(1).noOcclusion());
    public static final Block TIN_ORE = register("tin_ore", Block::new, Block.Properties.ofFullCopy(Blocks.COPPER_ORE));
    public static final Block DEEPSLATE_TIN_ORE = register("deepslate_tin_ore", Block::new, Block.Properties.ofFullCopy(Blocks.DEEPSLATE_COPPER_ORE));
    public static final Block GRAIN_MILL = register("grain_mill", com.snc.energies.block.WorkshopBlock::new,
        blkProps(3.5f, 6).noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE));
    public static final Block SEED_PRESS = register("seed_press", com.snc.energies.block.WorkshopBlock::new,
        blkProps(3.5f, 6).noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE));
    public static final Block RICE_CROP = register("rice_crop", p -> new com.snc.energies.block.RegionalCropBlock(p, () -> SncItems.RICE_SEEDS),
        Block.Properties.ofFullCopy(Blocks.WHEAT));
    public static final Block SOY_CROP = register("soy_crop", p -> new com.snc.energies.block.RegionalCropBlock(p, () -> SncItems.SOY_SEEDS),
        Block.Properties.ofFullCopy(Blocks.WHEAT));
    public static final Block MATE_CROP = register("mate_crop", p -> new com.snc.energies.block.RegionalCropBlock(p, () -> SncItems.MATE_SEEDS),
        Block.Properties.ofFullCopy(Blocks.WHEAT));
    public static final Block WOOD_STOVE_PART = register("wood_stove_part", com.snc.energies.block.WoodStovePartBlock::new,
        blkProps(3.5f,6.0f).pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE).noOcclusion()
            .lightLevel(state -> state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 12 : 0));
    public static final Block WOOD_STOVE = register("wood_stove", com.snc.energies.block.WoodStoveBlock::new,
            blkProps(3.5f, 6.0f).pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE).noOcclusion().lightLevel(state -> state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 12 : 0));
	public static final Block COAL_GENERATOR = register("coal_generator", CoalGeneratorBlock::new, blkProps(6.0f, 8.0f));
	public static final Block ELECTRIC_FURNACE = register("electric_furnace", ElectricFurnaceBlock::new, blkProps(4.5f, 8.0f));
	public static final Block CRUSHER = register("crusher", CrusherBlock::new, blkProps(5.0f, 8.0f));
	public static final Block ENERGY_CUBE = register("energy_cube", EnergyCubeBlock::new, blkProps(5.0f, 10.0f));
	public static final Block ENERGY_CABLE = register("energy_cable", CableBlock::new, Block.Properties.ofFullCopy(Blocks.GLASS)
			.mapColor(MapColor.METAL)
			.strength(0.4f, 0.4f)
			.noOcclusion());
	public static final Block VOLTAITE_ORE = register("voltaite_ore", Block::new, Block.Properties.ofFullCopy(Blocks.IRON_ORE));
	public static final Block DEEPSLATE_VOLTAITE_ORE = register("deepslate_voltaite_ore", Block::new, Block.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE));

	private static Block register(String name, Function<Block.Properties, Block> factory, Block.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SncEnergies.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}

	private SncBlocks() {
	}

	private static Block.Properties blkProps(float hardness, float resistance) {
		return Block.Properties.of()
				.mapColor(MapColor.METAL)
				.strength(hardness, resistance)
				.requiresCorrectToolForDrops()
				.sound(SoundType.METAL);
	}

	/** Simple load-time sanity log. */
	public static void bootstrap() {
		SncEnergies.LOGGER.info("SNC Energies blocks constructed");
	}

	/** Helper for the items registry to create block items safely. */
	public static BlockItem itemOf(Block block) {
		return new BlockItem(block, new Item.Properties()
				.setId(ResourceKey.create(Registries.ITEM, BuiltInRegistries.BLOCK.getKey(block)))
				.useBlockDescriptionPrefix());
	}
}
