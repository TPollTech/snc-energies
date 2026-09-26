package com.snc.energies.energy;

import java.util.*;
import com.snc.energies.block.*;
import com.snc.energies.blockentity.IndustrialBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Bounded, loaded-chunk traversal; transfer commits only accepted quantities. */
public final class SteamNetwork {
    private SteamNetwork(){}
    public static int distribute(Level level,IndustrialBlockEntity source,int budget) {
        int moved=0;
        Set<BlockPos> seen=new HashSet<>();
        Set<BlockPos> recipients=new HashSet<>();
        ArrayDeque<BlockPos> queue=new ArrayDeque<>();
        IndustrialBlock block=(IndustrialBlock)source.getBlockState().getBlock();
        for(int i=0;i<block.kind.cells();i++) {
            BlockPos part=block.cell(source.getBlockPos(),source.getBlockState().getValue(MachineBlock.FACING),i);
            if(level.hasChunkAt(part)&&block.owns(level.getBlockState(part),source.getBlockState(),i)){seen.add(part);queue.add(part);}
        }
        while(!queue.isEmpty() && seen.size()<1024 && moved<budget && source.steam()>0) {
            for(Direction direction:Direction.values()) {
                BlockPos next=queue.peek().relative(direction);
                if(!level.hasChunkAt(next)||!seen.add(next))continue;
                var state=level.getBlockState(next);
                if(state.getBlock() instanceof SteamPipeBlock){queue.add(next);continue;}
                var target=IndustrialBlock.controller(level,next,state);
                if(target==null||target==source||!target.kind().steamConsumer()||!recipients.add(target.getBlockPos()))continue;
                int accepted=target.receiveSteam(Math.min(budget-moved,source.steam()));
                source.removeSteam(accepted);moved+=accepted;
            }
            queue.remove();
        }
        return moved;
    }
}
