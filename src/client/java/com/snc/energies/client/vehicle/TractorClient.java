package com.snc.energies.client.vehicle;

import com.snc.energies.registry.SncEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Client-only registration; no renderer classes are loaded on a dedicated server. */
public final class TractorClient {
    private TractorClient() {}

    public static void bootstrap() {
        EntityRendererRegistry.register(SncEntities.TRACTOR, TractorRenderer::new);
        EntityRendererRegistry.register(SncEntities.HARVESTER, HarvesterRenderer::new);
        EntityRendererRegistry.register(SncEntities.PLANTER, PlanterRenderer::new);
        EntityRendererRegistry.register(SncEntities.GRAIN_CART, GrainCartRenderer::new);
    }
}
