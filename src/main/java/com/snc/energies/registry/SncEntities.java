package com.snc.energies.registry;

import com.snc.energies.SncEnergies;
import com.snc.energies.entity.HarvesterEntity;
import com.snc.energies.entity.PlanterEntity;
import com.snc.energies.entity.TractorEntity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Entity registry (AGENTS.md rule: registration lives in one place only).
 * Additive only: never reuse an identifier for something else (world stability).
 */
public final class SncEntities {
    public static final EntityType<TractorEntity> TRACTOR = register("tractor",
        EntityType.Builder.<TractorEntity>of(
            (EntityType<TractorEntity> type, net.minecraft.world.level.Level level) -> new TractorEntity(type, level),
            MobCategory.MISC)
            .sized(2.2F, 2.0F)
            .eyeHeight(1.6F)
            .passengerAttachments(1.4F)
            .clientTrackingRange(10)
            .updateInterval(3));

    public static final EntityType<PlanterEntity> PLANTER = register("planter",
        EntityType.Builder.<PlanterEntity>of(
            (EntityType<PlanterEntity> type, net.minecraft.world.level.Level level) -> new PlanterEntity(type, level),
            MobCategory.MISC)
            .sized(2.6F, 1.7F)
            .eyeHeight(1.2F)
            .clientTrackingRange(10)
            .updateInterval(3));

    public static final EntityType<HarvesterEntity> HARVESTER = register("harvester",
        EntityType.Builder.<HarvesterEntity>of(
            (EntityType<HarvesterEntity> type, net.minecraft.world.level.Level level) -> new HarvesterEntity(type, level),
            MobCategory.MISC)
            .sized(3.2F, 3.0F)
            .eyeHeight(2.4F)
            .passengerAttachments(2.4F)
            .clientTrackingRange(10)
            .updateInterval(3));

    public static final EntityType<com.snc.energies.entity.GrainCartEntity> GRAIN_CART = register("grain_cart",
        EntityType.Builder.<com.snc.energies.entity.GrainCartEntity>of(
            (EntityType<com.snc.energies.entity.GrainCartEntity> type, net.minecraft.world.level.Level level) ->
                new com.snc.energies.entity.GrainCartEntity(type, level),
            MobCategory.MISC)
            .sized(2.8F, 2.2F)
            .eyeHeight(1.6F)
            .clientTrackingRange(10)
            .updateInterval(3));

    public static final EntityType<com.snc.energies.entity.MercajeiroEntity> MERCAJEIRO = register("mercajeiro",
        EntityType.Builder.<com.snc.energies.entity.MercajeiroEntity>of(
            (EntityType<com.snc.energies.entity.MercajeiroEntity> type, net.minecraft.world.level.Level level) ->
                new com.snc.energies.entity.MercajeiroEntity(type, level),
            MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .eyeHeight(1.62F)
            .clientTrackingRange(10)
            .updateInterval(2));

    @SuppressWarnings("unchecked")
    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, SncEnergies.id(name));
        return (EntityType<T>) Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    private SncEntities() {
    }

    /** Living-attribute registration (FabricDefaultAttributeRegistry). */
    public static void registerAttributes() {
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry
            .register(MERCAJEIRO, com.snc.energies.entity.MercajeiroEntity.createAttributes());
    }

    public static void bootstrap() {
        SncEnergies.LOGGER.info("SNC Energies entities registered");
    }
}
