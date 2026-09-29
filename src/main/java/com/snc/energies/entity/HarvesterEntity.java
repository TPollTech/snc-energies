package com.snc.energies.entity;

import java.util.List;

import com.snc.energies.registry.SncBlocks;
import com.snc.energies.registry.SncEntities;
import com.snc.energies.registry.SncItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * SNC 90 combine harvester with the approved golden rig.
 *
 * Server authority follows the same 26.3 vehicle pattern as the SNC 75
 * tractor: the client only sends its input and this entity reads it on the
 * server through getLastClientInput(). Harvesting runs along the same three
 * rows the planter sows, only fully grown crops are cut, every product goes
 * to the onboard tank and a full tank pauses cutting without destroying any
 * crop. Fuel is the vegetable-oil portion, one 400-tick portion per 80 ticks
 * of driving.
 */
public class HarvesterEntity extends VehicleEntity implements Container {
    private static final float WHEELS_PER_ENGINE_TURN = 12.0F;
    private static final float STEERING_SPEED = 3.2F;
    private static final float FORWARD_SPEED = 0.42F;
    private static final float REVERSE_SPEED = 0.22F;
    private static final int OIL_BURN_PER_TICK = 1;
    /** One 400-tick oil portion drives 80 ticks of work. */
    public static final int TICKS_PER_OIL_PORTION = 80;

    private static final EntityDataAccessor<Integer> DATA_OIL =
        SynchedEntityData.defineId(HarvesterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_STEERING =
        SynchedEntityData.defineId(HarvesterEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_WORKING =
        SynchedEntityData.defineId(HarvesterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HEADER =
        SynchedEntityData.defineId(HarvesterEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int OIL_CAPACITY = 5200;
    public static final int OIL_PER_ITEM = 400;

    public static int oilCapacity() {
        return OIL_CAPACITY;
    }
    /** One row of the nine-slot tank grid mirrors one planter row. */
    public static final int ROWS = 3;
    public static final int SLOTS_PER_ROW = 9;
    public static final int TOTAL_SLOTS = ROWS * SLOTS_PER_ROW;

    private static final Item[] SEED_ITEMS = {
        Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.CARROT, Items.POTATO,
        SncItems.RICE_SEEDS, SncItems.SOY_SEEDS, SncItems.MATE_SEEDS
    };

    private final NonNullList<ItemStack> tankSlots = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
    private float steeringDelta;
    private float wheelRotation;
    private int oilTicks;
    /** Detached grain-cart implement riding on the rear hitch, if any. */
    private GrainCartEntity attachedCart;

    public HarvesterEntity(EntityType<HarvesterEntity> type, Level level) {
        super(type, level);
    }

    public HarvesterEntity(Level level, double x, double y, double z) {
        this(SncEntities.HARVESTER, level);
        setPos(x, y, z);
        setDeltaMovement(Vec3.ZERO);
        xOld = x;
        yOld = y;
        zOld = z;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OIL, 0);
        builder.define(DATA_STEERING, 0.0F);
        builder.define(DATA_WORKING, false);
        builder.define(DATA_HEADER, false);
    }

    // ======================= driving =======================

    @Override
    public void tick() {
        super.tick();
        xo = getX();
        yo = getY();
        zo = getZ();
        if (level().isClientSide()) {
            if (isWorking() && getOil() > 0 && random.nextFloat() < 0.2F) {
                Vec3 rear = position().add(forward().scale(-2.2));
                level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    rear.x, getY() + 2.0, rear.z, 0, 0.015, 0);
            }
            return;
        }
        if (isWorking() && getOil() <= 0) setWorking(false);  // ran dry: engine dies
        if (!isWorking()) {
            steeringDelta *= 0.6F;
            setSteering(steeringDelta);
            setDeltaMovement(Vec3.ZERO);
            tickAttachment();
            return;
        }
        if (!(getControllingPassenger() instanceof ServerPlayer driver)) {
            // Idle with the engine on: coast to a stop instead of freezing.
            steeringDelta *= 0.6F;
            setSteering(steeringDelta);
            setDeltaMovement(getDeltaMovement().x * 0.7F, getDeltaMovement().y, getDeltaMovement().z * 0.7F);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(0, getDeltaMovement().y, 0);
            tickAttachment();
            return;
        }
        net.minecraft.world.entity.player.Input input = driver.getLastClientInput();
        if (input.left() == input.right()) {
            steeringDelta *= 0.72F;
        } else {
            steeringDelta += input.left() ? STEERING_SPEED : -STEERING_SPEED;
        }
        steeringDelta = Mth.clamp(steeringDelta, -18.0F, 18.0F);
        setSteering(steeringDelta);
        setYRot(getYRot() + Mth.wrapDegrees(steeringDelta) * 0.45F);
        float throttle = input.forward() ? FORWARD_SPEED : input.backward() ? -REVERSE_SPEED : 0;
        if (throttle != 0) {
            Vec3 motion = forward().scale(throttle);
            setDeltaMovement(motion.x, getDeltaMovement().y, motion.z);
        } else {
            setDeltaMovement(0, getDeltaMovement().y, 0);
        }
        burnOil();
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(0, getDeltaMovement().y, 0);
        float distance = (float) Math.hypot(getX() - xo, getZ() - zo);
        wheelRotation += distance * 360.0F * WHEELS_PER_ENGINE_TURN;
        if (tickCount % 40 == 0) {
            level().playSound(null, blockPosition(), SoundEvents.MINECART_RIDING,
                SoundSource.NEUTRAL, 0.35F, 0.5F);
        }
        if (distance > 0.01F && isHeaderLowered()) harvestRows();
        tickAttachment();
    }

    /** Test seam: one server drive step without a riding player; honors the
     * engine and oil gates like the real tick. */
    public void driveTick(float throttle) {
        if (level().isClientSide() || !isWorking() || getOil() <= 0) return;
        xo = getX();
        yo = getY();
        zo = getZ();
        burnOil();
        Vec3 motion = throttle == 0 ? Vec3.ZERO : forward().scale(throttle);
        setDeltaMovement(motion.x, getDeltaMovement().y, motion.z);
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(0, getDeltaMovement().y, 0);
        float distance = (float) Math.hypot(getX() - xo, getZ() - zo);
        wheelRotation += distance * 360.0F * WHEELS_PER_ENGINE_TURN;
        // Pose the cart before harvesting so the hitch stays welded.
        tickAttachment();
        if (distance > 0.01F && isHeaderLowered()) harvestRows();
    }

    // ======================= grain-cart implement =======================

    /** Server tick: mirrors the towing state onto the coupled cart. */
    private void tickAttachment() {
        if (attachedCart == null) return;
        if (attachedCart.isRemoved()) {
            attachedCart = null;
            return;
        }
        attachedCart.mirrorHarvester(this);
        Vec3 hitch = position().add(forward().scale(-GrainCartEntity.HITCH_DISTANCE));
        attachedCart.setPos(hitch.x, getY(), hitch.z);
        attachedCart.setYRot(getYRot());
        attachedCart.setXRot(0);
    }

    /** Couples the nearest free-standing grain cart behind the machine. */
    public boolean attachCart() {
        if (hasAttachedCart()) return false;
        Vec3 hitch = position().add(forward().scale(-GrainCartEntity.HITCH_DISTANCE));
        GrainCartEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (GrainCartEntity candidate : level().getEntitiesOfClass(GrainCartEntity.class,
            new AABB(hitch.x - GrainCartEntity.HITCH_REACH, getY() - 2, hitch.z - GrainCartEntity.HITCH_REACH,
                hitch.x + GrainCartEntity.HITCH_REACH, getY() + 3, hitch.z + GrainCartEntity.HITCH_REACH))) {
            double distance = candidate.distanceToSqr(hitch.x, getY(), hitch.z);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        if (best == null) return false;
        attachedCart = best;
        if (!level().isClientSide()) {
            level().playSound(null, blockPosition(), SoundEvents.CHAIN_PLACE,
                SoundSource.NEUTRAL, 0.7F, 1.0F);
        }
        return true;
    }

    /** Uncouples the cart: it stays in the world with its load preserved. */
    public boolean detachCart() {
        if (attachedCart == null || attachedCart.isRemoved()) {
            attachedCart = null;
            return false;
        }
        attachedCart.park();
        attachedCart = null;
        if (!level().isClientSide()) {
            level().playSound(null, blockPosition(), SoundEvents.CHAIN_BREAK,
                SoundSource.NEUTRAL, 0.7F, 1.0F);
        }
        return true;
    }

    public boolean hasAttachedCart() {
        return attachedCart != null && !attachedCart.isRemoved();
    }

    /** Empties the grain tank into the coupled cart, preserving stacks exactly. */
    public boolean unloadIntoCart() {
        if (!hasAttachedCart()) return false;
        boolean moved = false;
        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {
            ItemStack stack = tankSlots.get(slot);
            if (stack.isEmpty()) continue;
            ItemStack remainder = attachedCart.storeGrain(stack);
            if (remainder.getCount() < stack.getCount()) moved = true;
            tankSlots.set(slot, remainder);
            if (!remainder.isEmpty()) break;  // cart full: stop without voiding
        }
        if (moved) {
            setChanged();
            if (!level().isClientSide()) {
                level().playSound(null, blockPosition(), SoundEvents.CROP_BREAK,
                    SoundSource.NEUTRAL, 0.6F, 0.55F);
            }
        }
        return moved;
    }

    private void burnOil() {
        if (++oilTicks >= TICKS_PER_OIL_PORTION) {
            oilTicks = 0;
            setOil(getOil() - 1);
            if (getOil() <= 0) setWorking(false);  // out of fuel: engine dies silently
        }
    }

    private Vec3 forward() {
        float yawRad = getYRot() * ((float) Math.PI / 180.0F);
        return new Vec3(-Mth.sin(yawRad), 0, Mth.cos(yawRad));
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public net.minecraft.world.entity.MoveSimulationType getMoveSimulationType() {
        return net.minecraft.world.entity.MoveSimulationType.SERVER_AND_CLIENT;
    }

    // ======================= harvesting =======================

    /** Sweep depth: the header cuts one to three blocks ahead of the machine,
     * matching the planter's row cadence. */
    private static final int HEADER_DEPTH = 3;

    /** Cuts a three-row strip ahead of the header, mature crops only. */
    private void harvestRows() {
        ServerLevel world = (ServerLevel) level();
        Direction facing = Direction.fromYRot(getYRot());
        Direction left = facing.getCounterClockWise();
        for (int depth = 1; depth <= HEADER_DEPTH; depth++) {
            for (int row = 0; row < ROWS; row++) {
                BlockPos target = BlockPos.containing(
                    getX() + left.getStepX() * (row - 1),
                    getY(),
                    getZ() + left.getStepZ() * (row - 1)).relative(facing, depth);
                tryHarvestOne(world, target);
            }
        }
    }

    private void tryHarvestOne(ServerLevel world, BlockPos target) {
        BlockState state = world.getBlockState(target);
        if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) return;
        List<ItemStack> drops = List.copyOf(crop.getDrops(state, world, target, null));
        // Refuse the whole row if the tank cannot hold every product: no partial
        // cuts, no destroyed crops, no voided items.
        for (ItemStack drop : drops) {
            if (!hasRoomFor(drop)) return;
        }
        for (ItemStack drop : drops) {
            insertStack(drop);
        }
        world.setBlock(target, crop.defaultBlockState(), 3);
        world.gameEvent(this, net.minecraft.world.level.gameevent.GameEvent.BLOCK_CHANGE, target);
        world.playSound(null, target, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 0.55F, 0.9F);
    }

    private boolean hasRoomFor(ItemStack stack) {
        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {
            ItemStack current = tankSlots.get(slot);
            if (current.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(current, stack)
                && current.getCount() + stack.getCount() <= Math.min(current.getMaxStackSize(), getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }

    private void insertStack(ItemStack stack) {
        for (int slot = 0; slot < TOTAL_SLOTS && !stack.isEmpty(); slot++) {
            ItemStack current = tankSlots.get(slot);
            if (!current.isEmpty() && ItemStack.isSameItemSameComponents(current, stack)) {
                int room = Math.min(current.getMaxStackSize(), getMaxStackSize()) - current.getCount();
                int moved = Math.min(room, stack.getCount());
                if (moved > 0) {
                    current.grow(moved);
                    stack.shrink(moved);
                    setChanged();
                }
            }
        }
        for (int slot = 0; slot < TOTAL_SLOTS && !stack.isEmpty(); slot++) {
            if (tankSlots.get(slot).isEmpty()) {
                tankSlots.set(slot, stack.split(Math.min(stack.getCount(), getMaxStackSize())));
                setChanged();
            }
        }
    }

    // ======================= controls (menu buttons) =======================

    /** Server-authoritative ignition: refuses without oil, stops silently without it. */
    public boolean toggleWorking() {
        if (isWorking()) {
            setWorking(false);
            return true;
        }
        if (getOil() <= 0) return false;
        setWorking(true);
        return true;
    }

    /** Header (cutting platform): raised keeps the machine off the crops. */
    public boolean cycleHeader() {
        setHeaderLowered(!isHeaderLowered());
        return true;
    }

    public boolean isHeaderLowered() {
        return !entityData.get(DATA_HEADER);
    }

    public void setHeaderLowered(boolean lowered) {
        entityData.set(DATA_HEADER, !lowered);
    }

    // ======================= supply panel sync =======================

    public int getOil() {
        return entityData.get(DATA_OIL);
    }

    public void setOil(int value) {
        entityData.set(DATA_OIL, Math.clamp(value, 0, OIL_CAPACITY));
    }

    /** Free room in the oil tank (menu supply slot uses it). */
    public int oilRoom() {
        return OIL_CAPACITY - getOil();
    }

    /** Adds oil, clamped to capacity; returns what was actually accepted. */
    public int addOil(int amount) {
        int accepted = Math.clamp(amount, 0, oilRoom());
        if (accepted > 0) setOil(getOil() + accepted);
        return accepted;
    }

    public boolean isWorking() {
        return entityData.get(DATA_WORKING);
    }

    public void setWorking(boolean working) {
        entityData.set(DATA_WORKING, working);
    }

    public float getSteering() {
        return entityData.get(DATA_STEERING);
    }

    public void setSteering(float value) {
        entityData.set(DATA_STEERING, value);
    }

    public float getWheelRotation() {
        return wheelRotation;
    }

    // ======================= interaction =======================

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 hitLocation) {
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(SncItems.VEGETABLE_OIL) && getOil() < OIL_CAPACITY) {
            if (!level().isClientSide()) {
                int accepted = Math.min(OIL_CAPACITY - getOil(), OIL_PER_ITEM);
                setOil(getOil() + accepted);
                stack.shrink(1);
                level().playSound(null, blockPosition(), SoundEvents.BUCKET_EMPTY,
                    SoundSource.NEUTRAL, 0.6F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.isEmpty()) {
            if (!level().isClientSide() && player.startRiding(this)) {
                return InteractionResult.SUCCESS;
            }
            return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    // ======================= container (grain tank) =======================

    @Override public int getContainerSize() { return TOTAL_SLOTS; }
    @Override public boolean isEmpty() {
        for (ItemStack stack : tankSlots) if (!stack.isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getItem(int slot) { return tankSlots.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack split = ContainerHelper.removeItem(tankSlots, slot, amount);
        if (!split.isEmpty()) setChanged();
        return split;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(tankSlots, slot);
    }
    @Override public void setItem(int slot, ItemStack stack) {
        tankSlots.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }
    @Override public void setChanged() {
    }
    @Override public boolean stillValid(Player player) {
        return isAlive() && player.distanceToSqr(this) <= 64.0;
    }
    @Override public void clearContent() {
        tankSlots.clear();
        setChanged();
    }
    @Override public int getMaxStackSize() {
        return 64;
    }

    // ======================= persistence =======================

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        ContainerHelper.loadAllItems(input, tankSlots);
        setOil(input.getIntOr("Oil", 0));
        setWorking(input.getBooleanOr("Working", false));
        setHeaderLowered(input.getBooleanOr("HeaderLowered", false));
        setSteering(input.getFloatOr("Steering", 0.0F));
        steeringDelta = getSteering();
        wheelRotation = input.getFloatOr("WheelRotation", 0.0F);
        oilTicks = input.getIntOr("OilTicks", 0);
        attachedCart = null;
        input.getInt("AttachedCart").ifPresent(id -> {
            if (level() instanceof ServerLevel world && world.getEntity(id) instanceof GrainCartEntity cart) {
                attachedCart = cart;
            }
        });
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        ContainerHelper.saveAllItems(output, tankSlots, true);
        output.putInt("Oil", getOil());
        output.putBoolean("Working", isWorking());
        output.putBoolean("HeaderLowered", isHeaderLowered());
        output.putFloat("Steering", getSteering());
        output.putFloat("WheelRotation", wheelRotation);
        output.putInt("OilTicks", oilTicks);
        if (hasAttachedCart()) output.putInt("AttachedCart", attachedCart.getId());
    }

    // ======================= vehicle plumbing =======================

    @Override
    public Item getDropItem() {
        return SncItems.HARVESTER;
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
}
