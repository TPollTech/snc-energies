package com.snc.energies.blockentity;

import com.snc.energies.economy.MercadaoCatalog;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.HashMap;
import java.util.Map;

/**
 * One Mercadão shelf: exposes the purchase menu for its shelf line and tracks
 * the daily stock in the same spirit as the Adventures MarketInventory (used
 * per offer, restored at the 07:00 in-game restock, persisted in the BE).
 * The BE owns no items: stock is a counter per offer, and delivery happens
 * straight into the buyer's inventory (Traficante.entregar contract).
 */
public class MercadaoShelfBlockEntity extends BlockEntity implements MenuProvider {
    /** Restock hour (in-game ticks) matching the Adventures market virada. */
    public static final int RESTOCK_TICK = 0;

    private String shelf = MercadaoCatalog.SHELF_BEVERAGES;
    private long lastRestockDay = -1;
    private final Map<String, Integer> usedStock = new HashMap<>();

    public MercadaoShelfBlockEntity(BlockPos pos, BlockState state) {
        super(com.snc.energies.registry.SncBlockEntities.MERCADAO_SHELF, pos, state);
    }

    public String shelf() {
        return shelf;
    }

    public void setShelf(String shelf) {
        this.shelf = shelf;
        setChanged();
    }

    /** The Mercajeiro position, found by scanning the local market area. */
    public BlockPos npcAt() {
        if (level == null) return null;
        for (BlockPos candidate : BlockPos.betweenClosed(worldPosition.offset(-2, 1, -2),
                worldPosition.offset(2, 3, 2))) {
            if (level.getBlockEntity(candidate) instanceof MercadaoAnchorBlockEntity anchor) {
                return anchor.npcAt();
            }
        }
        return null;
    }

    /** One sold unit: consume one unit of the daily stock (post-debit). */
    public void consumeStock(MercadaoCatalog.Offer offer) {
        restockIfNeeded(day());
        usedStock.merge(offer.id(), 1, Integer::sum);
        setChanged();
    }

    public int remaining(MercadaoCatalog.Offer offer) {
        restockIfNeeded(day());
        return Math.max(0, offer.dailyStock() - usedStock.getOrDefault(offer.id(), 0));
    }

    /** Render-thread mirror: computes the stock without any mutation. */
    public int remainingReadonly(MercadaoCatalog.Offer offer) {
        return Math.max(0, offer.dailyStock() - usedStock.getOrDefault(offer.id(), 0));
    }

    private long day() {
        return level.getGameTime() / 24000L;
    }

    private void restockIfNeeded(long day) {
        if (day != lastRestockDay) {
            lastRestockDay = day;
            usedStock.clear();
            setChanged();
        }
    }

    /** Server tick: keeps the daily restock honest across time changes. */
    public void serverTick() {
        if (level == null || level.isClientSide()) return;
        restockIfNeeded(day());
    }

    /** Test seam: runs the real day-rollover path without waiting 24k ticks. */
    public void advanceDayForTest() {
        restockIfNeeded(day() + 1);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("Shelf", shelf);
        output.putLong("LastRestockDay", lastRestockDay);
        CompoundTag used = new CompoundTag();
        usedStock.forEach(used::putInt);
        output.store("UsedStock", CompoundTag.CODEC, used);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        shelf = input.getString("Shelf").orElse(MercadaoCatalog.SHELF_BEVERAGES);
        lastRestockDay = input.getLongOr("LastRestockDay", -1);
        usedStock.clear();
        input.read("UsedStock", CompoundTag.CODEC).ifPresent(tag -> {
            for (String key : tag.keySet()) usedStock.put(key, tag.getIntOr(key, 0));
        });
    }

    // ======================= purchase transaction =======================

    /**
     * Server-authoritative sale, following the Adventures Traficante.comprar
     * contract exactly: stock check → wallet debit → stock consumption →
     * delivery into the inventory (dropped at the player when full) →
     * feedback sound. A failed debit never consumes stock. No second ledger
     * exists anywhere in this flow.
     */
    public boolean trySell(net.minecraft.server.level.ServerPlayer player,
                           MercadaoCatalog.Offer offer) {
        if (level == null || level.isClientSide() || !player.isAlive()) return false;
        if (level != player.level()) return false;
        if (remaining(offer) <= 0) {
            player.playSound(net.minecraft.sounds.SoundEvents.VILLAGER_NO, 0.7F, 1.0F);
            player.sendSystemMessage(Component.translatable("gui.snc_energies.mercadao.sold_out"));
            return false;
        }
        if (!com.snc.energies.economy.AdventuresMoney.withdraw(player, offer.price())) {
            player.playSound(net.minecraft.sounds.SoundEvents.VILLAGER_NO, 0.7F, 1.0F);
            player.sendSystemMessage(Component.translatable("gui.snc_energies.mercadao.no_money",
                offer.price()));
            return false;
        }
        consumeStock(offer);
        deliver(player, offer.stack());
        player.playSound(net.minecraft.sounds.SoundEvents.VILLAGER_YES, 0.8F, 1.0F);
        player.sendSystemMessage(Component.translatable("gui.snc_energies.mercadao.purchased",
            offer.stack().getHoverName(), offer.price()));
        return true;
    }

    /** Traficante.entregar contract: inventory first, world drop as fallback. */
    private static void deliver(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.item.ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            stack.setCount(1);
            stack.setPopTime(10);
            player.level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                player.level(), player.getX(), player.getY() + 0.5, player.getZ(), stack));
        } else {
            player.getInventory().setChanged();
        }
    }

    // ======================= purchase menu =======================

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.snc_energies.mercadao_shelf");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new com.snc.energies.menu.MercadaoMenu(id, inventory, this);
    }

    /** Counter reach: generous, but still physical (like stillValid on BEs). */
    public boolean stillValid(Player player) {
        if (isRemoved()) return false;
        return player.isWithinBlockInteractionRange(worldPosition, 4.0);
    }
}
