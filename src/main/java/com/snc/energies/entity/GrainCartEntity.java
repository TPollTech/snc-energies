package com.snc.energies.entity;

import com.snc.energies.registry.SncEntities;
import com.snc.energies.registry.SncItems;

import net.minecraft.core.NonNullList;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * SNC 90-C carreta graneleira: the harvester's detachable grain cart.
 *
 * It carries the crop the combine unloads into it (exact stack conservation:
 * a full cart pauses the transfer without voiding anything) and follows the
 * harvester's rear hitch while attached, mirroring how the SNC 75-P planter
 * rides the tractor. Detached, it parks in the world with its load preserved
 * and its own panel can be opened to unload by hand.
 */
public class GrainCartEntity extends VehicleEntity implements Container {
    public static final int ROWS = 3;
    public static final int SLOTS_PER_ROW = 5;
    public static final int TOTAL_SLOTS = ROWS * SLOTS_PER_ROW;
    /** Hitch point in model pixels behind the harvester origin (rear axle). */
    public static final double HITCH_DISTANCE = 40.0 / 16.0;
    /** Search radius when the harvester couples a parked cart. */
    public static final double HITCH_REACH = 4.5;

    private static final EntityDataAccessor<Boolean> DATA_TOWED =
        SynchedEntityData.defineId(GrainCartEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_WHEEL_ROTATION =
        SynchedEntityData.defineId(GrainCartEntity.class, EntityDataSerializers.FLOAT);

    private final NonNullList<ItemStack> grainSlots = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    public GrainCartEntity(EntityType<? extends GrainCartEntity> type, Level level) {
        super(type, level);
    }

    public GrainCartEntity(Level level, double x, double y, double z) {
        this(SncEntities.GRAIN_CART, level);
        setPos(x, y, z);
        setDeltaMovement(Vec3.ZERO);
        xOld = x;
        yOld = y;
        zOld = z;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TOWED, false);
        builder.define(DATA_WHEEL_ROTATION, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        // The towing harvester poses this entity every server tick; without a
        // tow it never moves by itself.
        setDeltaMovement(Vec3.ZERO);
    }

    /** Client mirror of the towing harvester's state; server copies it every tick. */
    public void mirrorHarvester(HarvesterEntity harvester) {
        setTowed(true);
        setWheelRotation(harvester.getWheelRotation());
        setYRot(harvester.getYRot());
    }

    public void park() {
        setTowed(false);
        setWheelRotation(0.0F);
    }

    // ======================= render mirrors =======================

    public boolean isTowed() {
        return entityData.get(DATA_TOWED);
    }

    public void setTowed(boolean towed) {
        entityData.set(DATA_TOWED, towed);
    }

    public void setWheelRotation(float value) {
        entityData.set(DATA_WHEEL_ROTATION, value);
    }

    public float getWheelRotation() {
        return entityData.get(DATA_WHEEL_ROTATION);
    }

    // ======================= grain transfer =======================

    /**
     * Traficante.entregar contract for bulk cargo: merge into matching stacks
     * first, then empty slots, preserving components; returns whatever did
     * not fit (empty when everything was stored).
     */
    public ItemStack storeGrain(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < TOTAL_SLOTS && !remaining.isEmpty(); slot++) {
            ItemStack current = grainSlots.get(slot);
            if (!current.isEmpty() && ItemStack.isSameItemSameComponents(current, remaining)) {
                int room = Math.min(current.getMaxStackSize(), getMaxStackSize()) - current.getCount();
                int moved = Math.min(room, remaining.getCount());
                if (moved > 0) {
                    current.grow(moved);
                    remaining.shrink(moved);
                    setChanged();
                }
            }
        }
        for (int slot = 0; slot < TOTAL_SLOTS && !remaining.isEmpty(); slot++) {
            if (grainSlots.get(slot).isEmpty()) {
                grainSlots.set(slot, remaining.split(Math.min(remaining.getCount(), getMaxStackSize())));
                setChanged();
            }
        }
        return remaining;
    }

    public boolean isFull() {
        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {
            ItemStack current = grainSlots.get(slot);
            if (current.isEmpty() || current.getCount() < Math.min(current.getMaxStackSize(), getMaxStackSize())) {
                return false;
            }
        }
        return true;
    }

    // ======================= interaction =======================

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 hitLocation) {
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new com.snc.energies.menu.GrainCartMenu.Provider(this));
        }
        return InteractionResult.SUCCESS;
    }

    // ======================= container =======================

    @Override public int getContainerSize() { return TOTAL_SLOTS; }
    @Override public boolean isEmpty() {
        for (ItemStack stack : grainSlots) if (!stack.isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getItem(int slot) { return grainSlots.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack split = ContainerHelper.removeItem(grainSlots, slot, amount);
        if (!split.isEmpty()) setChanged();
        return split;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(grainSlots, slot);
    }
    @Override public void setItem(int slot, ItemStack stack) {
        grainSlots.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }
    @Override public void setChanged() {
    }
    @Override public boolean stillValid(Player player) {
        return isAlive() && player.distanceToSqr(this) <= 64.0;
    }
    @Override public void clearContent() {
        grainSlots.clear();
        setChanged();
    }
    @Override public int getMaxStackSize() {
        return 64;
    }

    // ======================= vehicle plumbing =======================

    @Override
    public LivingEntity getControllingPassenger() {
        return null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    /** Never blocks the towing harvester (or anything else): the cart rides welded. */
    @Override
    public boolean canBeCollidedWith(Entity other) {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return false;
    }

    @Override
    public Item getDropItem() {
        return SncItems.GRAIN_CART;
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide() && (reason == Entity.RemovalReason.KILLED
            || reason == Entity.RemovalReason.DISCARDED)) {
            Containers.dropContents(level(), blockPosition(), this);
        }
        super.remove(reason);
    }

    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        return super.hurtServer(world, source, amount);
    }

    // ======================= persistence =======================

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        ContainerHelper.loadAllItems(input, grainSlots);
        setTowed(false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        ContainerHelper.saveAllItems(output, grainSlots, true);
    }
}
