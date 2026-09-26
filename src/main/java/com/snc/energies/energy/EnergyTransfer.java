package com.snc.energies.energy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.snc.energies.blockentity.CableBlockEntity;
import com.snc.energies.blockentity.CoalGeneratorBlockEntity;
import com.snc.energies.blockentity.EnergyCubeBlockEntity;
import com.snc.energies.blockentity.EnergyProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Routes power through loaded cables, preserving energy and prioritizing machines over batteries. */
public final class EnergyTransfer {
    private EnergyTransfer() {}
    private record Target(BlockEntity entity, EnergyStorage storage) {}

    public static long distribute(Level level, BlockPos from, EnergyStorage source, long maxAmount) {
        long budget = Math.min(maxAmount, source.getEnergy());
        if (budget <= 0) return 0;
        boolean batterySource = level.getBlockEntity(from) instanceof EnergyCubeBlockEntity;
        List<Target> targets = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        seen.add(from);
        pending.add(from);
        var originState=level.getBlockState(from);
        if (originState.getBlock() instanceof com.snc.energies.block.WoodStoveBlock
                && originState.getValue(com.snc.energies.block.WoodStoveBlock.ASSEMBLED)) {
            for(int i=1;i<com.snc.energies.block.WoodStoveBlock.CELLS.length;i++) {
                BlockPos part=com.snc.energies.block.WoodStoveBlock.cell(from,originState.getValue(com.snc.energies.block.MachineBlock.FACING),i);
                if (level.hasChunkAt(part) && com.snc.energies.block.WoodStoveBlock.owns(level.getBlockState(part),originState,i)) {
                    seen.add(part); pending.add(part);
                }
            }
        }
        while (!pending.isEmpty() && seen.size() <= 4096) {
            BlockPos pos = pending.removeFirst();
            for (Direction side : Direction.values()) {
                BlockPos next = pos.relative(side);
                if (!level.hasChunkAt(next) || !seen.add(next)) continue;
                BlockEntity entity = level.getBlockEntity(next);
                if (entity instanceof CableBlockEntity) {
                    pending.add(next);
                } else if (entity instanceof EnergyProvider provider
                        && !(entity instanceof CoalGeneratorBlockEntity)
                        && !(batterySource && entity instanceof EnergyCubeBlockEntity)) {
                    EnergyStorage storage = provider.getEnergyStorage(side.getOpposite());
                    if (storage != null && !storage.isFull()) targets.add(new Target(entity, storage));
                }
            }
        }
        targets.sort((a, b) -> {
            int priority = Boolean.compare(a.entity() instanceof EnergyCubeBlockEntity, b.entity() instanceof EnergyCubeBlockEntity);
            return priority != 0 ? priority : Long.compare(a.storage().getEnergy() * b.storage().getCapacity(),
                    b.storage().getEnergy() * a.storage().getCapacity());
        });
        long transferred = 0;
        for (Target target : targets) {
            long amount = Math.min(budget - transferred, source.getEnergy());
            long accepted = target.storage().insert(amount, false);
            source.extract(accepted, false);
            if (accepted > 0) target.entity().setChanged();
            transferred += accepted;
            if (transferred >= budget) break;
        }
        return transferred;
    }
}
