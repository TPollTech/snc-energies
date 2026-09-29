package com.snc.energies.entity;


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
import java.util.List;
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
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * SNC 75 tractor with the approved three-row planter implement.
 *
 * Server authority follows the 26.3 vehicle pattern: the client only sends its
 * input, this entity reads it on the server through getLastClientInput().
 * Seeding works on already tilled farmland, never replaces blocks or crops,
 * and a seed is only consumed when a row position actually plants one.
 */
public class TractorEntity extends VehicleEntity implements Container {
    /** Twelve engine turns per wheel revolution (0.70-block rolling radius). */
    private static final float WHEELS_PER_ENGINE_TURN = 12.0F;
    private static final float STEERING_SPEED = 3.2F;
    private static final float FORWARD_SPEED = 0.42F;
    private static final float REVERSE_SPEED = 0.22F;
    /** Motor oil runs 5,200 ticks; one tick of travel costs one tick of oil. */
    private static final int OIL_BURN_PER_TICK = 1;

    private static final EntityDataAccessor<Integer> DATA_OIL =
        SynchedEntityData.defineId(TractorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_ROW_SLOTS =
        SynchedEntityData.defineId(TractorEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> DATA_STEERING =
        SynchedEntityData.defineId(TractorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_WORKING =
        SynchedEntityData.defineId(TractorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PLANTER_RAISED =
        SynchedEntityData.defineId(TractorEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int OIL_CAPACITY = 5200;

    public static int oilCapacity() {
        return OIL_CAPACITY;
    }

    public static final int SEEDS_PER_ROW = 64;
    public static final int ROWS = 3;
    public static final int TOTAL_SLOTS = ROWS * SEEDS_PER_ROW;
    /** One vegetable-oil portion equals 400 burn ticks, reusing the stove's value. */
    public static final int OIL_PER_ITEM = 400;

    private static final Item[] SEED_ITEMS = {
        Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.CARROT, Items.POTATO,
        SncItems.RICE_SEEDS, SncItems.SOY_SEEDS, SncItems.MATE_SEEDS
    };

    /** Supply-panel acceptance rule for one planter row. */
    public static boolean isSeed(ItemStack stack) {
        Item item = stack.getItem();
        for (Item seed : SEED_ITEMS) if (seed == item) return true;
        return false;
    }

    private final NonNullList<ItemStack> seedSlots = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
    private float steeringDelta;
    private float wheelRotation;
    private int rowCursor;
    /** Detached planter implement riding on the three-point hitch, if any. */
    private PlanterEntity attachedPlanter;

    public TractorEntity(EntityType<TractorEntity> type, Level level) {
        super(type, level);
    }

    public TractorEntity(Level level, double x, double y, double z) {
        this(SncEntities.TRACTOR, level);
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
        builder.define(DATA_ROW_SLOTS, (byte) 0);
        builder.define(DATA_STEERING, 0.0F);
        builder.define(DATA_WORKING, false);
        builder.define(DATA_PLANTER_RAISED, false);
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
                Vec3 rear = position().add(forward().scale(-1.3));
                level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    rear.x, getY() + 1.1, rear.z, 0, 0.015, 0);
            }
            return;
        }
        if (isWorking() && getOil() <= 0) setWorking(false);  // ran dry
        if (!isWorking()) {
            steeringDelta *= 0.6F;
            setSteering(steeringDelta);
            tickAttachment();
            return;
        }
        if (!(getControllingPassenger() instanceof ServerPlayer driver)) {
            // Idle: the engine stays on (toggle off on the panel), the tractor
            // coasts to a stop and the coupled implement keeps its pose.
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
        setOil(getOil() - OIL_BURN_PER_TICK);
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(0, getDeltaMovement().y, 0);
        float distance = (float) Math.hypot(getX() - xo, getZ() - zo);
        wheelRotation += distance * 360.0F * WHEELS_PER_ENGINE_TURN;
        if (tickCount % 40 == 0) {
            level().playSound(null, blockPosition(), SoundEvents.MINECART_RIDING,
                SoundSource.NEUTRAL, 0.35F, 0.55F);
        }
        if (distance > 0.01F && isPlanterLowered()) plantRows();
        tickAttachment();
    }

    /** Test seam: performs one server drive step without requiring a riding
     * player; still honors the engine and oil gates like the real tick. */
    public void driveTick(float throttle) {
        if (level().isClientSide() || !isWorking() || getOil() <= 0) return;
        xo = getX();
        yo = getY();
        zo = getZ();
        setOil(getOil() - OIL_BURN_PER_TICK);
        Vec3 motion = throttle == 0 ? Vec3.ZERO : forward().scale(throttle);
        setDeltaMovement(motion.x, getDeltaMovement().y, motion.z);
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(0, getDeltaMovement().y, 0);
        float distance = (float) Math.hypot(getX() - xo, getZ() - zo);
        wheelRotation += distance * 360.0F * WHEELS_PER_ENGINE_TURN;
        // Pose the implement before planting so the row cells come from the
        // welded hitch position, not from wherever the implement was parked.
        tickAttachment();
        if (distance > 0.01F && isPlanterLowered()) plantRows();
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

    /**
     * The tractor simulates its own physics on both sides: the client predicts
     * with the riding player's local input and the server authoritatively moves
     * using getLastClientInput(), the 26.3 vehicle contract.
     */
    @Override
    public net.minecraft.world.entity.MoveSimulationType getMoveSimulationType() {
        return net.minecraft.world.entity.MoveSimulationType.SERVER_AND_CLIENT;
    }

    // ======================= implement attachment =======================

    private static final double HITCH_REACH = 4.5;

    /** Server tick: validates the link, mirrors state and keeps the implement posed. */
    private void tickAttachment() {
        if (attachedPlanter == null) return;
        if (attachedPlanter.isRemoved()) {
            attachedPlanter = null;
            return;
        }
        attachedPlanter.mirrorTractor(this);
        Vec3 hitch = position().add(forward().scale(-PlanterEntity.HITCH_DISTANCE));
        attachedPlanter.setPos(hitch.x, getY(), hitch.z);
        attachedPlanter.setYRot(getYRot());
        attachedPlanter.setXRot(0);
    }

    /** Couples the nearest free-standing planter behind the tractor. */
    public boolean attachPlanter() {
        if (attachedPlanter != null && !attachedPlanter.isRemoved()) return false;
        Vec3 hitch = position().add(forward().scale(-PlanterEntity.HITCH_DISTANCE));
        List<PlanterEntity> candidates = level().getEntitiesOfClass(PlanterEntity.class,
            new AABB(hitch.x - HITCH_REACH, getY() - 2, hitch.z - HITCH_REACH,
                hitch.x + HITCH_REACH, getY() + 3, hitch.z + HITCH_REACH));
        PlanterEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (PlanterEntity candidate : candidates) {
            double d = candidate.distanceToSqr(hitch.x, getY(), hitch.z);
            if (d < bestDistance) {
                bestDistance = d;
                best = candidate;
            }
        }
        if (best == null) return false;
        attachedPlanter = best;
        if (!level().isClientSide()) {
            level().playSound(null, blockPosition(), SoundEvents.CHAIN_PLACE,
                SoundSource.NEUTRAL, 0.7F, 1.0F);
        }
        return true;
    }

    /** Uncouples the implement: it stays in the world with its tanks preserved. */
    public boolean detachPlanter() {
        if (attachedPlanter == null || attachedPlanter.isRemoved()) {
            attachedPlanter = null;
            return false;
        }
        attachedPlanter.park();
        attachedPlanter = null;
        if (!level().isClientSide()) {
            level().playSound(null, blockPosition(), SoundEvents.CHAIN_BREAK,
                SoundSource.NEUTRAL, 0.7F, 1.0F);
        }
        return true;
    }

    public boolean hasAttachedPlanter() {
        return attachedPlanter != null && !attachedPlanter.isRemoved();
    }

    // ======================= planting =======================

    /** Plants along the three toolbar rows, one seed per position. */
    private void plantRows() {
        ServerLevel world = (ServerLevel) level();
        Direction facing = Direction.fromYRot(getYRot());
        Direction left = facing.getCounterClockWise();
        boolean planted = false;
        // With the SNC 75-P attached, the implement's own tanks feed the rows
        // and the row origin sits at the hitch; otherwise the internal tanks do.
        PlanterEntity implement = hasAttachedPlanter() ? attachedPlanter : null;
        double originX = implement != null ? implement.getX() : getX();
        double originZ = implement != null ? implement.getZ() : getZ();
        for (int row = 0; row < ROWS; row++) {
            boolean seeded = implement != null ? implementRowHasSeeds(implement, row) : hasSeedFor(row);
            if (!seeded) continue;
            // The crop cell is the one the planter's feet occupy (resting on the
            // tilled surface), offset sideways per row and three blocks ahead.
            BlockPos target = BlockPos.containing(
                originX + left.getStepX() * (row - 1),
                getY(),
                originZ + left.getStepZ() * (row - 1)).relative(facing, 3);
            if (tryPlantOne(world, target, row, implement)) planted = true;
        }
        if (planted) setRowSlots();
    }

    private boolean implementRowHasSeeds(PlanterEntity implement, int row) {
        for (int slot = row * PlanterEntity.SEEDS_PER_ROW; slot < (row + 1) * PlanterEntity.SEEDS_PER_ROW; slot++) {
            if (!implement.getItem(slot).isEmpty()) return true;
        }
        return false;
    }

    /** Plants one seed if the position is free; returns whether a seed was spent. */
    private boolean tryPlantOne(ServerLevel world, BlockPos target, int row, PlanterEntity implement) {
        BlockState below = world.getBlockState(target.below());
        if (!below.is(Blocks.FARMLAND)) return false;
        BlockState at = world.getBlockState(target);
        if (!at.isAir()) return false;
        int seedId = implement != null ? implementSeedId(implement, row) : rowSeedId(row);
        BlockState crop = cropFor(seedId);
        if (crop == null) return false;
        world.setBlock(target, crop, 3);
        world.gameEvent(this, GameEvent.BLOCK_PLACE, target);
        if (implement != null) takeImplementSeed(implement, row);
        else takeSeed(row);
        if (rowCursor % 4 == 0) {
            world.playSound(null, target, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        return true;
    }

    private static int implementSeedId(PlanterEntity implement, int row) {
        for (int slot = row * PlanterEntity.SEEDS_PER_ROW; slot < (row + 1) * PlanterEntity.SEEDS_PER_ROW; slot++) {
            ItemStack stack = implement.getItem(slot);
            if (!stack.isEmpty()) return seedId(stack.getItem());
        }
        return -1;
    }

    private static void takeImplementSeed(PlanterEntity implement, int row) {
        for (int slot = row * PlanterEntity.SEEDS_PER_ROW; slot < (row + 1) * PlanterEntity.SEEDS_PER_ROW; slot++) {
            ItemStack stack = implement.getItem(slot);
            if (!stack.isEmpty()) {
                stack.shrink(1);
                if (stack.isEmpty()) implement.setItem(slot, ItemStack.EMPTY);
                else implement.setChanged();
                return;
            }
        }
    }

    /** Seeds are keyed by slot row: 0 vanilla, 1 beetroot, 2 carrot, 3 potato, 4-6 SNC crops. */
    private static int seedId(Item item) {
        for (int i = 0; i < SEED_ITEMS.length; i++) if (SEED_ITEMS[i] == item) return i;
        return -1;
    }

    private static BlockState cropFor(int seedId) {
        if (seedId < 0) return null;
        BlockState state = switch (seedId) {
            case 0 -> Blocks.WHEAT.defaultBlockState();
            case 1 -> Blocks.BEETROOTS.defaultBlockState();
            case 2 -> Blocks.CARROTS.defaultBlockState();
            case 3 -> Blocks.POTATOES.defaultBlockState();
            case 4 -> SncBlocks.RICE_CROP.defaultBlockState();
            case 5 -> SncBlocks.SOY_CROP.defaultBlockState();
            case 6 -> SncBlocks.MATE_CROP.defaultBlockState();
            default -> null;
        };
        // defaultBlockState already carries the minimum age for every vanilla crop.
        return state;
    }

    private int rowSeedId(int row) {
        for (int slot = row * SEEDS_PER_ROW; slot < (row + 1) * SEEDS_PER_ROW; slot++) {
            ItemStack stack = seedSlots.get(slot);
            if (!stack.isEmpty()) return seedId(stack.getItem());
        }
        return -1;
    }

    private boolean hasSeedFor(int row) {
        return rowSeedId(row) >= 0;
    }

    private void takeSeed(int row) {
        for (int slot = row * SEEDS_PER_ROW; slot < (row + 1) * SEEDS_PER_ROW; slot++) {
            ItemStack stack = seedSlots.get(slot);
            if (!stack.isEmpty()) {
                stack.shrink(1);
                if (stack.isEmpty()) seedSlots.set(slot, ItemStack.EMPTY);
                rowCursor++;
                return;
            }
        }
    }

    private void setRowSlots() {
        byte packed = 0;
        for (int row = 0; row < ROWS; row++) if (hasSeedFor(row)) packed |= (byte) (1 << row);
        entityData.set(DATA_ROW_SLOTS, packed);
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

    /** Three-point hitch: raised keeps the implement off the ground; lowered works. */
    public boolean cyclePlanterLift() {
        setPlanterRaised(!isPlanterRaised());
        return true;
    }

    public boolean isPlanterRaised() {
        return entityData.get(DATA_PLANTER_RAISED);
    }

    public void setPlanterRaised(boolean raised) {
        entityData.set(DATA_PLANTER_RAISED, raised);
    }

    public boolean isPlanterLowered() {
        return !isPlanterRaised();
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

    /** Reflects whether each of the three rows still has seeds (read-only mirror). */
    public boolean rowHasSeeds(int row) {
        return (entityData.get(DATA_ROW_SLOTS) & (1 << row)) != 0;
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
                    SoundSource.NEUTRAL, 0.6F, 1.1F);
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

    // ======================= container (supply panel) =======================

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

    // ======================= persistence =======================

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        // VehicleEntity adds nothing here; Entity's own data is handled by saveWithoutId.
        ContainerHelper.loadAllItems(input, seedSlots);
        setOil(input.getIntOr("Oil", 0));
        setWorking(input.getBooleanOr("Working", false));
        setPlanterRaised(input.getBooleanOr("PlanterRaised", false));
        setSteering(input.getFloatOr("Steering", 0.0F));
        steeringDelta = getSteering();
        wheelRotation = input.getFloatOr("WheelRotation", 0.0F);
        attachedPlanter = null;
        input.getInt("AttachedPlanter").ifPresent(id -> {
            if (level() instanceof ServerLevel world && world.getEntity(id) instanceof PlanterEntity planter) {
                attachedPlanter = planter;
            }
        });
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        ContainerHelper.saveAllItems(output, seedSlots, true);
        output.putInt("Oil", getOil());
        output.putBoolean("Working", isWorking());
        output.putBoolean("PlanterRaised", isPlanterRaised());
        output.putFloat("Steering", getSteering());
        output.putFloat("WheelRotation", wheelRotation);
        if (hasAttachedPlanter()) output.putInt("AttachedPlanter", attachedPlanter.getId());
    }

    // ======================= vehicle plumbing =======================

    @Override
    public Item getDropItem() {
        return SncItems.TRACTOR;
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
