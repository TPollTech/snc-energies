package com.snc.energies.entity;

import com.snc.energies.registry.SncEntities;
import com.snc.energies.registry.SncItems;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
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
 * SNC 75-P: the detachable three-row planter implement.
 *
 * It carries its own seed tanks (mirroring the tractor's panel rows) and follows
 * the tractor's rear three-point hitch while attached; the lift command comes
 * from the tractor. Detached, it parks in the world with its seeds preserved.
 */
public class PlanterEntity extends VehicleEntity implements Container {
    public static final int SEEDS_PER_ROW = 64;
    public static final int ROWS = 3;
    public static final int TOTAL_SLOTS = ROWS * SEEDS_PER_ROW;
    /** Hitch point in model pixels behind the tractor origin (golden rear hitch). */
    public static final double HITCH_DISTANCE = 27.0 / 16.0;

    private static final net.minecraft.network.syncher.EntityDataAccessor<Byte> DATA_ROW_SLOTS =
        net.minecraft.network.syncher.SynchedEntityData.defineId(PlanterEntity.class,
            net.minecraft.network.syncher.EntityDataSerializers.BYTE);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_RAISED =
        net.minecraft.network.syncher.SynchedEntityData.defineId(PlanterEntity.class,
            net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_WORKING =
        net.minecraft.network.syncher.SynchedEntityData.defineId(PlanterEntity.class,
            net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> DATA_WHEEL_ROTATION =
        net.minecraft.network.syncher.SynchedEntityData.defineId(PlanterEntity.class,
            net.minecraft.network.syncher.EntityDataSerializers.FLOAT);

    private final NonNullList<ItemStack> seedSlots = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
    private float wheelRotation;

    public PlanterEntity(EntityType<PlanterEntity> type, Level level) {
        super(type, level);
    }

    public PlanterEntity(Level level, double x, double y, double z) {
        this(SncEntities.PLANTER, level);
        setPos(x, y, z);
        setDeltaMovement(Vec3.ZERO);
        xOld = x;
        yOld = y;
        zOld = z;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ROW_SLOTS, (byte) 0);
        builder.define(DATA_RAISED, false);
        builder.define(DATA_WORKING, false);
        builder.define(DATA_WHEEL_ROTATION, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        setDeltaMovement(Vec3.ZERO);
    }

    /** Client mirror of the towing tractor's state; server copies it every tick. */
    public void mirrorTractor(TractorEntity tractor) {
        setWorking(tractor.isWorking());
        setRaised(tractor.isPlanterRaised());
        setWheelRotation(tractor.getWheelRotation());
        setYRot(tractor.getYRot());
    }

    public void park() {
        setWorking(false);
        setRaised(false);
        setWheelRotation(0.0F);
    }

    // ======================= render mirrors =======================

    public boolean isRaised() {
        return entityData.get(DATA_RAISED);
    }

    public void setRaised(boolean raised) {
        entityData.set(DATA_RAISED, raised);
    }

    public boolean isWorking() {
        return entityData.get(DATA_WORKING);
    }

    public void setWorking(boolean working) {
        entityData.set(DATA_WORKING, working);
    }

    public void setWheelRotation(float value) {
        entityData.set(DATA_WHEEL_ROTATION, value);
    }

    public float getWheelRotation() {
        return entityData.get(DATA_WHEEL_ROTATION);
    }

    public boolean rowHasSeeds(int row) {
        return (entityData.get(DATA_ROW_SLOTS) & (1 << row)) != 0;
    }

    private void setRowSlots() {
        byte packed = 0;
        for (int row = 0; row < ROWS; row++) {
            for (int slot = row * SEEDS_PER_ROW; slot < (row + 1) * SEEDS_PER_ROW; slot++) {
                if (!seedSlots.get(slot).isEmpty()) {
                    packed |= (byte) (1 << row);
                    break;
                }
            }
        }
        entityData.set(DATA_ROW_SLOTS, packed);
    }

    // ======================= interaction =======================

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 hitLocation) {
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new com.snc.energies.menu.PlanterMenu.Provider(this));
        }
        return InteractionResult.SUCCESS;
    }

    // ======================= container (seed tanks) =======================

    @Override public int getContainerSize() { return TOTAL_SLOTS; }
    @Override public boolean isEmpty() {
        for (ItemStack stack : seedSlots) if (!stack.isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getItem(int slot) { return seedSlots.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack split = ContainerHelper.removeItem(seedSlots, slot, amount);
        if (!split.isEmpty()) setChanged();
        return split;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(seedSlots, slot);
    }
    @Override public void setItem(int slot, ItemStack stack) {
        seedSlots.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }
    @Override public void setChanged() {
        setRowSlots();
    }
    @Override public boolean stillValid(Player player) {
        return isAlive() && player.distanceToSqr(this) <= 64.0;
    }
    @Override public void clearContent() {
        seedSlots.clear();
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

    /** Never blocks the towing tractor (or anything else): the implement rides welded. */
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
        return SncItems.PLANTER;
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
        ContainerHelper.loadAllItems(input, seedSlots);
        setRowSlots();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        ContainerHelper.saveAllItems(output, seedSlots, true);
    }
}
