package com.snc.energies.entity;

import com.snc.energies.blockentity.MercadaoShelfBlockEntity;
import com.snc.energies.menu.MercadaoMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.EnumSet;

/**
 * The Mercadão attendant: a humanoid shopkeeper (AbstractVillager, the same
 * base the Adventures Gago/Traficante use) parked behind the counter. He opens
 * the shelf purchase menu, keeps to his post (a dedicated goal), never fights
 * back and drops nothing when killed — the anchor respawns him quietly.
 */
public class MercajeiroEntity extends AbstractVillager {
    /** Distance (blocks) from the post before the attendant walks back. */
    private static final double POST_LEASH = 1.6;

    private BlockPos homePost;

    public MercajeiroEntity(EntityType<? extends AbstractVillager> type, Level level) {
        super(type, level);
    }

    /** Called by the anchor right after spawning. */
    public void setHomePost(BlockPos post) {
        this.homePost = post;
    }

    public BlockPos homePost() {
        return homePost;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.MOVEMENT_SPEED, 0.4)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(3, new StayAtPostGoal());
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F, 0.02F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** The shelf behind the counter; found in any horizontal direction so the
     * market building may face any way (the old south-only probe silently
     * broke the click-to-buy in rotated structures). */
    private MercadaoShelfBlockEntity servingShelf() {
        if (homePost == null) return null;
        if (servingShelf != null && servingShelf.isRemoved()) servingShelf = null;
        if (servingShelf != null) return servingShelf;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level().getBlockEntity(homePost.relative(direction)) instanceof MercadaoShelfBlockEntity shelf) {
                servingShelf = shelf;
                return shelf;
            }
        }
        return null;
    }

    private MercadaoShelfBlockEntity servingShelf;

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        MercadaoShelfBlockEntity shelf = servingShelf();
        if (shelf == null) return InteractionResult.PASS;
        player.openMenu(new SimpleMenuProvider(
            (id, inventory, p) -> new MercadaoMenu(id, inventory, shelf),
            shelf.getDisplayName()));
        return InteractionResult.SUCCESS_SERVER;
    }

    // ======================= AbstractVillager plumbing =======================

    @Override
    protected void updateTrades(ServerLevel level) {
        // Trading happens through the Mercadão menu, not vanilla offers.
        overrideOffers(new net.minecraft.world.item.trading.MerchantOffers());
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        // No villager economy XP: the Adventures wallet stays the sole economy.
    }

    @Override
    public net.minecraft.world.entity.AgeableMob getBreedOffspring(ServerLevel level, net.minecraft.world.entity.AgeableMob partner) {
        // The attendant is a fixed adult; no offspring.
        return null;
    }

    // ======================= persistence =======================

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        homePost = input.read("HomePost", BlockPos.CODEC).orElse(null);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (homePost != null) output.store("HomePost", BlockPos.CODEC, homePost);
    }

    /**
     * Keeps the attendant within arm's reach of the counter: if pushed away
     * he walks back and re-faces the aisle. Movement-only; never blocks the
     * look-at goals.
     */
    private final class StayAtPostGoal extends Goal {
        StayAtPostGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        private boolean atPost() {
            return homePost == null
                || distanceToSqr(homePost.getX() + 0.5, homePost.getY(), homePost.getZ() + 0.5)
                    <= POST_LEASH * POST_LEASH;
        }

        @Override
        public boolean canUse() {
            return homePost != null && !atPost();
        }

        @Override
        public boolean canContinueToUse() {
            return homePost != null && !atPost();
        }

        @Override
        public void start() {
            getNavigation().moveTo(homePost.getX() + 0.5, homePost.getY(), homePost.getZ() + 0.5, 0.5);
        }

        @Override
        public void stop() {
            getNavigation().stop();
            // Face the aisle (the shelf faces the street; the NPC stands north).
            if (homePost != null) setYRot(Mth.wrapDegrees(180.0F));
        }
    }
}
