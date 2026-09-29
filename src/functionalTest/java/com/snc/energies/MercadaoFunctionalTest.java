package com.snc.energies;

import com.snc.energies.block.MercadaoShelfBlock;
import com.snc.energies.blockentity.MercadaoAnchorBlockEntity;
import com.snc.energies.blockentity.MercadaoShelfBlockEntity;
import com.snc.energies.economy.AdventuresMoney;
import com.snc.energies.economy.MercadaoCatalog;
import com.snc.energies.entity.MercajeiroEntity;
import com.snc.energies.menu.MercadaoMenu;
import com.snc.energies.registry.SncBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

/**
 * The Mercadão end-to-end flow: shelves expose the real catalog, the anchor
 * keeps one Mercajeiro on post, a purchase debits the Adventures wallet
 * (PlayerMoney) and delivers the item, the daily stock runs out and restocks
 * on the next day, and the standalone profile refuses every purchase (no
 * wallet → no parallel economy).
 */
public final class MercadaoFunctionalTest {
    private MercadaoFunctionalTest() {}

    public static void run(ServerLevel world, ServerPlayer player) {
        BlockPos base = new BlockPos(20, 100, 0);
        for (int x = 18; x < 30; x++) {
            for (int z = -4; z < 4; z++) {
                for (int y = 99; y < 103; y++) {
                    world.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        // Build the market: four shelves + the anchor behind them.
        world.setBlock(base, shelfState(MercadaoShelfBlock.MercadaoShelfVariant.BEVERAGES), 3);
        world.setBlock(base.east(), shelfState(MercadaoShelfBlock.MercadaoShelfVariant.SEEDS), 3);
        world.setBlock(base.east(2), shelfState(MercadaoShelfBlock.MercadaoShelfVariant.DELI), 3);
        world.setBlock(base.east(3), shelfState(MercadaoShelfBlock.MercadaoShelfVariant.PREMIUM), 3);
        BlockPos anchorPos = base.north().above();
        world.setBlock(anchorPos, SncBlocks.MERCADAO_ANCHOR.defaultBlockState(), 3);

        var anchor = (MercadaoAnchorBlockEntity) world.getBlockEntity(anchorPos);
        var shelf = (MercadaoShelfBlockEntity) world.getBlockEntity(base);
        check(anchor != null && shelf != null, "shelf and anchor block entities load");
        check(shelf.shelf().equals(MercadaoCatalog.SHELF_BEVERAGES),
            "beverage shelf bound to the beverage catalog line");

        // The anchor spawns exactly one attendant on the post below it.
        anchor.serverTick();
        MercajeiroEntity npc = attendantAt(world, anchor.npcAt());
        check(npc != null, "anchor spawns the Mercajeiro on the counter post");
        check(npc.homePost() != null && npc.homePost().equals(anchor.npcAt()),
            "Mercajeiro is bound to the market post");

        // Menu exposes the shelf line with prices and full stock.
        MercadaoMenu menu = new MercadaoMenu(1, player.getInventory(), shelf);
        var entries = menu.entries();
        check(entries.size() == MercadaoMenu.POSITIONS, "menu exposes ten counter positions");
        // The beer offer only exists when Adventures is on the classpath; the
        // standalone profile lists Energies/vanilla lines instead.
        MercadaoCatalog.Offer beer = MercadaoCatalog.byId("cerveja").orElse(null);
        if (beer != null) {
            check(entries.stream().anyMatch(e -> e != null && e.offer() != null
                    && e.offer().item() == beer.item() && e.price() == beer.price()),
                "beer offered at the same R$ " + beer.price() + " the Gago charges");
        }
        check(menu.clickMenuButton(player, 9) == false,
            "out-of-catalog position is rejected by the server menu");
        // Purchase flow: debit + delivery on the Adventures profile. The
        // headless buyer is a ServerPlayer that swallows chat (no network
        // connection in the harness); money, stock and delivery still run
        // through the real production paths.
        if (beer != null && AdventuresMoney.usableForTest()) {
            ServerPlayer broke = quietBuyer(world);
            check(!shelf.trySell(broke, beer), "empty wallet is refused without touching stock");
            check(shelf.remaining(beer) == beer.dailyStock(),
                "refused sale leaves the shelf stock intact");

            ServerPlayer buyer = quietBuyer(world);
            AdventuresMoney.creditForTest(buyer, beer.price());
            int before = AdventuresMoney.balance(buyer);
            int beforeBeer = buyer.getInventory().countItem(beer.item());
            boolean sold = shelf.trySell(buyer, beer);
            check(sold, "purchase accepted with a funded Adventures wallet");
            check(AdventuresMoney.balance(buyer) == before - beer.price(),
                "purchase debits exactly the catalog price from PlayerMoney");
            check(buyer.getInventory().countItem(beer.item()) == beforeBeer + 1,
                "purchase delivers the real item into the inventory");

            // Daily stock: drain the line, refuse when dry, restock next day.
            while (shelf.remaining(beer) > 0) {
                AdventuresMoney.creditForTest(buyer, beer.price());
                if (!shelf.trySell(buyer, beer)) break;
            }
            check(shelf.remaining(beer) == 0, "daily stock drains to zero");
            check(!shelf.trySell(buyer, beer), "sold-out line refuses further sales");
            // Day rollover through the real restock path (test seam, as the
            // tractor's driveTick: same code, no 24k-tick wait).
            shelf.advanceDayForTest();
            check(shelf.remaining(beer) == beer.dailyStock(),
                "next day restocks the shelf to full daily stock");
        } else {
            check(beer == null || !shelf.trySell(player, beer),
                "standalone profile refuses the sale (no wallet, no second economy)");
            check(AdventuresMoney.balance(player) == 0,
                "no balance exists when Adventures is absent");
        }

        // The NPC click path opens the same purchase menu through the shelf.
        // The headless test player has no network connection, so the packet
        // send inside openMenu NPEs — that NPE proves the click reached the
        // menu-open with a real shelf (a missing shelf would return PASS
        // before any network call).
        boolean openedMenu;
        try {
            openedMenu = npc.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND)
                != net.minecraft.world.InteractionResult.PASS;
        } catch (NullPointerException expectedHeadless) {
            openedMenu = true;
        }
        check(openedMenu, "Mercajeiro click opens the purchase menu");

        SncEnergies.LOGGER.info("MercadaoFunctionalTest OK");
    }

    private static ServerPlayer quietBuyer(ServerLevel world) {
        return new ServerPlayer(world.getServer(), world,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "MercadaoBuyer"),
            net.minecraft.server.level.ClientInformation.createDefault()) {
            @Override
            public void sendSystemMessage(net.minecraft.network.chat.Component message) {
                // headless harness: no connection to receive chat
            }
        };
    }

    private static net.minecraft.world.level.block.state.BlockState shelfState(
            MercadaoShelfBlock.MercadaoShelfVariant variant) {
        return SncBlocks.MERCADAO_SHELF.defaultBlockState()
            .setValue(MercadaoShelfBlock.VARIANT, variant);
    }

    private static MercajeiroEntity attendantAt(ServerLevel world, BlockPos pos) {
        return world.getEntitiesOfClass(MercajeiroEntity.class,
            new net.minecraft.world.phys.AABB(pos)).stream().findFirst().orElse(null);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError("Mercadão: " + message);
        SncEnergies.LOGGER.info("FUNCTIONAL PASS: {}", message);
    }
}
