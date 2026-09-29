package com.snc.energies.blockentity;

import com.snc.energies.entity.MercajeiroEntity;
import com.snc.energies.registry.SncEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

/**
 * Invisible anchor above the shelf row: the Mercadão's "manager". It spawns
 * and keeps one Mercajeiro at the counter (the NPC that serves the purchase
 * menu), mirroring how the Adventures MarketSystem manages the Gago. Shelves
 * ask it where the attendant stands; nothing else about money or stock lives
 * here — those stay on each shelf block entity and on the Adventures wallet.
 */
public class MercadaoAnchorBlockEntity extends BlockEntity {
    private static final int RESPAWN_DELAY_TICKS = 60;

    private UUID npcId;
    private int respawnTimer = -1;

    public MercadaoAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(com.snc.energies.registry.SncBlockEntities.MERCADAO_ANCHOR, pos, state);
    }

    /** Counter position where the Mercajeiro stands (right below the anchor). */
    public BlockPos npcAt() {
        return worldPosition.below();
    }

    /** Server tick: keeps exactly one attendant alive at the post. */
    public void serverTick() {
        if (!(level instanceof ServerLevel world)) return;
        if (npcId != null) {
            if (world.getEntity(npcId) instanceof MercajeiroEntity living && living.isAlive()) {
                respawnTimer = -1;
                return;
            }
            npcId = null;
            respawnTimer = RESPAWN_DELAY_TICKS; // short pause before respawning
        }
        if (respawnTimer > 0) {
            respawnTimer--;
            return;
        }
        spawnAttendant(world);
    }

    private void spawnAttendant(ServerLevel world) {
        BlockPos spot = npcAt();
        MercajeiroEntity npc = new MercajeiroEntity(SncEntities.MERCAJEIRO, world);
        npc.setPos(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
        npc.setHomePost(spot);
        npc.setYRot(180.0F);
        world.addFreshEntity(npc);
        npcId = npc.getUUID();
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (npcId != null) output.store("Npc", UUIDUtil.CODEC, npcId);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        npcId = input.read("Npc", UUIDUtil.CODEC).orElse(null);
    }
}
