package com.snc.energies;

import com.snc.energies.entity.GrainCartEntity;
import com.snc.energies.entity.HarvesterEntity;
import com.snc.energies.menu.GrainCartMenu;
import com.snc.energies.menu.HarvesterMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * SNC 90-C grain cart milestone: cargo panel contract, server-authoritative
 * hitch buttons, welded rear hitch that follows one drive step, exact stack
 * conservation on unload (a packed cart pauses the transfer without voiding),
 * detach preserving the load, and removal dropping every cargo slot.
 */
final class GrainCartFunctionalTest {
    private final ServerLevel level;

    private GrainCartFunctionalTest(ServerLevel level) { this.level = level; }

    static void run(ServerLevel level, ServerPlayer player) { new GrainCartFunctionalTest(level).run(player); }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        SncEnergies.LOGGER.info("FUNCTIONAL PASS: {}", message);
    }

    private void clear(int x0, int z0) {
        for (int x = x0 - 1; x <= x0 + 11; x++)
            for (int z = z0 - 1; z <= z0 + 11; z++)
                for (int y = 116; y <= 132; y++)
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
    }

    /** The functional world persists between runs: failed passes leave
     * vehicle ghosts the hitch search would couple instead of ours. The box
     * spans the forced-spawn region shared with the tractor test. */
    private void purgeGhosts() {
        for (net.minecraft.world.entity.Entity ghost : level.getEntitiesOfClass(
            net.minecraft.world.entity.Entity.class, new AABB(40, 110, 0, 56, 135, 26))) {
            if (ghost instanceof HarvesterEntity || ghost instanceof GrainCartEntity) {
                ghost.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            }
        }
    }

    private int cartLoad(GrainCartEntity cart, Item item) {
        int total = 0;
        for (int slot = 0; slot < GrainCartEntity.TOTAL_SLOTS; slot++) {
            ItemStack stack = cart.getItem(slot);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private int tankLoad(HarvesterEntity harvester, Item item) {
        int total = 0;
        for (int slot = 0; slot < HarvesterEntity.TOTAL_SLOTS; slot++) {
            ItemStack stack = harvester.getItem(slot);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private void run(ServerPlayer player) {
        clear(44, 2);
        purgeGhosts();
        // Flat pad inside the forced spawn chunks: entity-manager lookups only
        // see tracked sections, and the proven ones here are chunks 0-3.
        for (int x = 44; x <= 54; x++)
            for (int z = 1; z <= 13; z++)
                level.setBlock(new BlockPos(x, 120, z), Blocks.DIRT.defaultBlockState(), 3);

        // Parked cart exactly behind the harvester (yaw 0 faces +Z, so the
        // rear hitch sits at z - HITCH_DISTANCE).
        GrainCartEntity cart = new GrainCartEntity(level, 47.5, 121.0, 1.5);
        level.addFreshEntity(cart);
        HarvesterEntity harvester = new HarvesterEntity(level, 47.5, 121.0, 3.5);
        harvester.setYRot(0.0F);
        level.addFreshEntity(harvester);

        // 1. Panel contract: the cart exposes its fifteen-slot cargo menu.
        check(new GrainCartMenu.Provider(cart).createMenu(14, player.getInventory(), player) instanceof GrainCartMenu,
            "grain cart exposes its cargo panel menu");

        // 2. Hitch buttons stay server-authoritative on the harvester panel.
        player.setPos(47.5, 122.0, 3.5);
        HarvesterMenu panel = new HarvesterMenu(15, player.getInventory(), harvester);
        check(panel.clickMenuButton(player, HarvesterMenu.BUTTON_CART) && harvester.hasAttachedCart(),
            "panel hitch button couples the parked cart");
        check(!panel.clickMenuButton(player, HarvesterMenu.BUTTON_UNLOAD_CART),
            "unload button refuses an empty tank without error");
        check(panel.clickMenuButton(player, HarvesterMenu.BUTTON_CART) && !harvester.hasAttachedCart(),
            "panel hitch button uncouples through the same toggle");

        // 3. Entity-side coupling: weld at the hitch after a drive step.
        check(harvester.attachCart(), "hitch couples the parked grain cart");
        check(!harvester.attachCart(), "second coupling is refused while one cart is attached");
        harvester.setOil(HarvesterEntity.OIL_PER_ITEM);
        harvester.setWorking(true);
        harvester.setHeaderLowered(false);  // raised: nothing to harvest on the bare pad
        harvester.driveTick(0.2F);
        check(Math.abs(cart.getZ() - (harvester.getZ() - GrainCartEntity.HITCH_DISTANCE)) < 0.01,
            "cart rides welded at the harvester's rear hitch after a drive step");
        check(cart.isTowed(), "towed mirror syncs to the renderer state");

        // 4. Unload conserves stacks exactly, components and all.
        harvester.setItem(0, new ItemStack(Items.WHEAT, 64));
        harvester.setItem(1, new ItemStack(Items.WHEAT, 30));
        harvester.setItem(2, new ItemStack(Items.CARROT, 40));
        check(harvester.unloadIntoCart(), "unload moves grain into the attached cart");
        check(tankLoad(harvester, Items.WHEAT) == 0 && tankLoad(harvester, Items.CARROT) == 0
            && cartLoad(cart, Items.WHEAT) == 94 && cartLoad(cart, Items.CARROT) == 40,
            "unload conserves every grain exactly (94 wheat, 40 carrots)");

        // 5. A packed cart pauses the transfer without voiding the remainder.
        for (int slot = 0; slot < HarvesterEntity.TOTAL_SLOTS; slot++) {
            harvester.setItem(slot, new ItemStack(Items.WHEAT, 64));
        }
        check(harvester.unloadIntoCart(), "second unload runs until no cart slot can take more");
        check(cartLoad(cart, Items.WHEAT) == 14 * 64,
            "cart packs its fourteen wheat slots exactly");
        check(cartLoad(cart, Items.CARROT) == 40, "the carrot slot is untouched by the wheat pass");
        check(!cart.isFull(), "a slot below its stack cap keeps the cart from reporting full");
        check(tankLoad(harvester, Items.WHEAT) == HarvesterEntity.TOTAL_SLOTS * 64 - (14 * 64 - 94),
            "the packed cart pauses the transfer and keeps the remainder in the tank");

        // 6. Detach parks the cart with its load; unload refuses without one.
        check(harvester.detachCart() && !harvester.hasAttachedCart() && !cart.isTowed(),
            "hitch uncouples the cart and parks it");
        check(cartLoad(cart, Items.WHEAT) == 14 * 64 && cartLoad(cart, Items.CARROT) == 40,
            "detached cart keeps every grain in its cargo");
        check(!harvester.unloadIntoCart(), "unload refuses without an attached cart");

        // 7. Removal drops the cargo before the entity is gone (the suite runs
        // inside END_SERVER_TICK, so the spawned ItemEntities are asserted by
        // the container drain instead of the flaky-in-tick drop lookup).
        cart.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        boolean cargoDropped = cart.isRemoved();
        for (int slot = 0; slot < GrainCartEntity.TOTAL_SLOTS; slot++) {
            cargoDropped &= cart.getItem(slot).isEmpty();
        }
        check(cargoDropped, "removal drops every cargo slot before the cart is gone");

        // Cleanup (chunk-torn-down rule).
        harvester.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        clear(44, 2);
        purgeGhosts();
    }
}
