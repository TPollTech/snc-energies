package com.snc.energies.registry;

import java.util.Set;

import com.snc.energies.SncEnergies;
import com.snc.energies.blockentity.CableBlockEntity;
import com.snc.energies.blockentity.CoalGeneratorBlockEntity;
import com.snc.energies.blockentity.CrusherBlockEntity;
import com.snc.energies.blockentity.ElectricFurnaceBlockEntity;
import com.snc.energies.blockentity.EnergyCubeBlockEntity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Block entity type registry (AGENTS.md rule: one place only).
 * MC 26.2 has no BlockEntityType.Builder: direct constructor + registration.
 */
public final class SncBlockEntities {
    public static final BlockEntityType<com.snc.energies.blockentity.IndustrialBlockEntity> INDUSTRIAL = register(
        "industrial", new BlockEntityType<>(com.snc.energies.blockentity.IndustrialBlockEntity::new, Set.copyOf(SncBlocks.INDUSTRY.values())));
    public static final BlockEntityType<com.snc.energies.blockentity.WorkshopBlockEntity> WORKSHOP = register(
        "workshop", new BlockEntityType<>(com.snc.energies.blockentity.WorkshopBlockEntity::new, Set.of(SncBlocks.GRAIN_MILL, SncBlocks.SEED_PRESS)));
    public static final BlockEntityType<com.snc.energies.blockentity.WoodStoveBlockEntity> WOOD_STOVE = register(
            "wood_stove", new BlockEntityType<>(com.snc.energies.blockentity.WoodStoveBlockEntity::new, Set.of(SncBlocks.WOOD_STOVE)));
	public static final BlockEntityType<CoalGeneratorBlockEntity> COAL_GENERATOR = register(
			"coal_generator", new BlockEntityType<>(CoalGeneratorBlockEntity::new, Set.of(SncBlocks.COAL_GENERATOR)));
	public static final BlockEntityType<ElectricFurnaceBlockEntity> ELECTRIC_FURNACE = register(
			"electric_furnace", new BlockEntityType<>(ElectricFurnaceBlockEntity::new, Set.of(SncBlocks.ELECTRIC_FURNACE)));
	public static final BlockEntityType<CrusherBlockEntity> CRUSHER = register(
			"crusher", new BlockEntityType<>(CrusherBlockEntity::new, Set.of(SncBlocks.CRUSHER)));
	public static final BlockEntityType<EnergyCubeBlockEntity> ENERGY_CUBE = register(
			"energy_cube", new BlockEntityType<>(EnergyCubeBlockEntity::new, Set.of(SncBlocks.ENERGY_CUBE)));    public static final BlockEntityType<CableBlockEntity> ENERGY_CABLE = register(
			"energy_cable", new BlockEntityType<>(CableBlockEntity::new, Set.of(SncBlocks.ENERGY_CABLE)));
    public static final BlockEntityType<com.snc.energies.blockentity.ItemPipeBlockEntity> ITEM_PIPE = register(
        "item_pipe", new BlockEntityType<>(com.snc.energies.blockentity.ItemPipeBlockEntity::new, Set.of(SncBlocks.ITEM_PIPE)));
    public static final BlockEntityType<com.snc.energies.blockentity.MercadaoShelfBlockEntity> MERCADAO_SHELF = register(
        "mercadao_shelf", new BlockEntityType<>(com.snc.energies.blockentity.MercadaoShelfBlockEntity::new, Set.of(SncBlocks.MERCADAO_SHELF)));
    public static final BlockEntityType<com.snc.energies.blockentity.MercadaoAnchorBlockEntity> MERCADAO_ANCHOR = register(
        "mercadao_anchor", new BlockEntityType<>(com.snc.energies.blockentity.MercadaoAnchorBlockEntity::new, Set.of(SncBlocks.MERCADAO_ANCHOR)));
    public static final BlockEntityType<com.snc.energies.blockentity.SiloBlockEntity> SILO = register(
        "silo", new BlockEntityType<>(com.snc.energies.blockentity.SiloBlockEntity::new, Set.of(SncBlocks.SILO)));
    public static final BlockEntityType<com.snc.energies.blockentity.BeverageMotorBlockEntity> BEVERAGE_MOTOR = register(
        "beverage_motor", new BlockEntityType<>(com.snc.energies.blockentity.BeverageMotorBlockEntity::new, Set.of(SncBlocks.BEVERAGE_MOTOR)));
    public static final BlockEntityType<com.snc.energies.blockentity.ElectricUvLampBlockEntity> ELECTRIC_UV_LAMP = register(
        "electric_uv_lamp", new BlockEntityType<>(com.snc.energies.blockentity.ElectricUvLampBlockEntity::new, Set.of(SncBlocks.ELECTRIC_UV_LAMP)));

	private SncBlockEntities() {
	}

	private static <T extends BlockEntity> BlockEntityType<T> register(String name, BlockEntityType<T> type) {
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
				ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, SncEnergies.id(name)), type);
		return type;
	}

	public static void bootstrap() {
		SncEnergies.LOGGER.info("SNC Energies block entities registered");
	}
}
