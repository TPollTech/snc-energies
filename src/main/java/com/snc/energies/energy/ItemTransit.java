package com.snc.energies.energy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.snc.energies.block.IndustrialBlock;
import com.snc.energies.block.ItemPipeBlock;
import com.snc.energies.blockentity.ItemPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Routing for the item duct network. Each duct tick inspects its immediate
 * neighbours first and, failing that, walks the network up to a bounded
 * radius. Exactly one item moves per step and every move is committed only
 * when the destination keeps it, so interrupted ticks never duplicate or
 * delete items.
 *
 * Direction convention (verified against vanilla hopper bytecode): the face
 * passed to Container/WorldlyContainer calls is the container face towards
 * the duct, exactly as hoppers pass facing.getOpposite().
 */
public final class ItemTransit {
    private ItemTransit() {}

    private record Window(BlockPos pos, Direction approach, Container container) {}

    /** Move the duct's buffered item onward: adjacent windows first, then a bounded walk. */
    public static boolean moveFrom(Level level, BlockPos ductPos, ItemPipeBlockEntity duct) {
        ItemStack buffer = duct.getItem(0);
        // Filter applies at entry only: an already-buffered item always keeps moving,
        // so changing the whitelist can never strand an item inside the duct.
        if (buffer.isEmpty()) return false;
        List<Window> windows = windowsAround(level, ductPos);
        if (windows.isEmpty()) windows = networkWindows(level, ductPos);
        if (windows.isEmpty()) return false;
        List<Window> usable = new ArrayList<>();
        for (Window window : windows) {
            if (canInsert(window, buffer)) usable.add(window);
        }
        if (usable.isEmpty()) return false;
        Window target = usable.get(level.getRandom().nextInt(usable.size()));
        ItemStack moved = duct.drain(target.approach());
        if (moved.isEmpty()) return false;
        ItemStack leftover = HopperBlockEntity.addItem(duct, target.container(), moved, target.approach());
        if (!leftover.isEmpty()) {
            duct.setItem(0, leftover);
            return false;
        }
        duct.setChanged();
        target.container().setChanged();
        return true;
    }

    /** Duct cells, machines and hoppers adjacent to the given cell. */
    private static List<Window> windowsAround(Level level, BlockPos ductPos) {
        List<Window> windows = new ArrayList<>();
        for (Direction side : Direction.values()) {
            BlockPos next = ductPos.relative(side);
            if (!level.hasChunkAt(next) || !level.isLoaded(next)) continue;
            if (level.getBlockState(next).getBlock() instanceof ItemPipeBlock) continue;
            Container container = containerOf(level, next);
            if (container != null) windows.add(new Window(next, side.getOpposite(), container));
        }
        return windows;
    }

    /** Breadth-first walk across duct cells to every machine or hopper fronting the network. */
    private static List<Window> networkWindows(Level level, BlockPos ductPos) {
        List<Window> windows = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        seen.add(ductPos);
        queue.add(ductPos);
        while (!queue.isEmpty() && seen.size() <= 512) {
            BlockPos current = queue.removeFirst();
            for (Direction side : Direction.values()) {
                BlockPos next = current.relative(side);
                if (!level.hasChunkAt(next) || !level.isLoaded(next) || !seen.add(next)) continue;
                BlockState state = level.getBlockState(next);
                if (state.getBlock() instanceof ItemPipeBlock) {
                    queue.add(next);
                    continue;
                }
                Container container = containerOf(level, next);
                if (container != null) windows.add(new Window(next, side.getOpposite(), container));
            }
        }
        return windows;
    }

    /**
     * The controller speaks for every industrial cell; other containers expose
     * their block entity. Ducts themselves are excluded by the callers.
     */
    private static Container containerOf(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof IndustrialBlock) {
            return IndustrialBlock.controller(level, pos, state);
        }
        return level.getBlockEntity(pos) instanceof Container container ? container : null;
    }

    private static boolean canInsert(Window window, ItemStack stack) {
        for (int slot : slotsOf(window.container(), window.approach())) {
            if (window.container().canPlaceItem(slot, stack)
                    && (!(window.container() instanceof WorldlyContainer worldly)
                        || worldly.canPlaceItemThroughFace(slot, stack, window.approach()))) {
                return true;
            }
        }
        return false;
    }

    private static int[] slotsOf(Container container, Direction face) {
        return container instanceof WorldlyContainer worldly ? worldly.getSlotsForFace(face) : flatSlots(container);
    }

    /** Shared extraction view over plain containers (used by duct suction too). */
    public static int[] flatSlots(Container container) {
        int[] slots = new int[container.getContainerSize()];
        for (int i = 0; i < slots.length; i++) slots[i] = i;
        return slots;
    }
}
