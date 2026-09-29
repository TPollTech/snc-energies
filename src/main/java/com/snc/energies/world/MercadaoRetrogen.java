package com.snc.energies.world;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.snc.energies.SncEnergies;
import com.snc.energies.registry.SncBlocks;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import com.mojang.brigadier.CommandDispatcher;

import java.util.ArrayDeque;

/**
 * Optional backfill of the Mercadão into chunks explored before the worldgen
 * fix (0.6.5). Off by default; enables per world with
 * /gamerule snc_mercadao_retrogen true (or the admin command below). Loaded
 * eligible chunks are queued and processed at most N per tick, so filling a
 * base never freezes the server.
 *
 * Eligibility: a 48-block-radius search finds no registered Mercadão and the
 * chunk's air box (ground height, 6 blocks up) can hold the 11x9 footprint.
 * Placement rolls the same random used by worldgen, anchored at the
 * chunk-local ground surface, overwriting only air; biomes untouched.
 */
public final class MercadaoRetrogen {
    /** 26.3 gamerules are registry entries: the rule id must be lowercase. */
    public static final String RULE_NAME = "snc_mercadao_retrogen";
    /** Identifier of the structure whose presence blocks retrogen in the area. */
    private static final Identifier MERCADAO = SncEnergies.id("mercadao");
    /** Vanilla plains village: the positive control for the proximity search. */
    private static final Identifier VILLAGE_PLAINS = Identifier.fromNamespaceAndPath("minecraft", "village_plains");
    private static final int FOOTPRINT_RADIUS = 16;
    private static final int PER_TICK = 2;
    private static final int MAX_PER_LOAD = 4;

    /** Gamerule instance registered against the vanilla GAME_RULE registry. */
    private static final net.minecraft.world.level.gamerules.GameRule<Boolean> RETROGEN =
        new net.minecraft.world.level.gamerules.GameRule<>(
            net.minecraft.world.level.gamerules.GameRuleCategory.register(SncEnergies.id("retrogen")),
            net.minecraft.world.level.gamerules.GameRuleType.BOOL,
            BoolArgumentType.bool(),
            (visitor, rule) -> visitor.visitBoolean(rule),
            com.mojang.serialization.Codec.BOOL,
            value -> value ? 1 : 0,
            Boolean.FALSE,
            net.minecraft.world.flag.FeatureFlags.VANILLA_SET);
    static {
        // 26.3: gamerules live in a BuiltInRegistry; the static init of this
        // class runs from bootstrap(), before the vanilla rules are used.
        net.minecraft.core.Registry.register(
            net.minecraft.core.registries.BuiltInRegistries.GAME_RULE,
            SncEnergies.id(RULE_NAME), RETROGEN);
    }

    private static final ArrayDeque<ChunkPos> QUEUE = new ArrayDeque<>();
    private static MinecraftServer server;

    private MercadaoRetrogen() {
    }

    public static void bootstrap() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            MercadaoRetrogen.server = server;
            QUEUE.clear();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            MercadaoRetrogen.server = null;
            QUEUE.clear();
        });
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, newChunk) -> {
            if (newChunk || !level.dimension().equals(Level.OVERWORLD)) return;
            if (QUEUE.size() >= 512) return;
            if (QUEUE.size() < MAX_PER_LOAD) QUEUE.add(chunk.getPos());
        });
        ServerTickEvents.END_SERVER_TICK.register(MercadaoRetrogen::tick);
        CommandRegistrationCallback.EVENT.register(MercadaoRetrogen::registerCommands);
        SncEnergies.LOGGER.info("SNC Energies mercadao retrogen registered");
    }

    private static void tick(MinecraftServer current) {
        if (current != server) return;
        ServerLevel overworld = current.overworld();
        for (int i = 0; i < PER_TICK && !QUEUE.isEmpty(); i++) {
            ChunkPos pos = QUEUE.poll();
            LevelChunk chunk = overworld.getChunkSource().getChunkNow(pos.x(), pos.z());
            if (chunk != null && isEligible(overworld, chunk) && place(overworld, chunk)) {
                SncEnergies.LOGGER.info("RETROGEN mercadao placed at chunk {},{}", pos.x(), pos.z());
            }
        }
    }

    /** The gamerule instance, for tests and admin tooling. */
    public static net.minecraft.world.level.gamerules.GameRule<Boolean> rule() { return RETROGEN; }

    /** Synchronous, force path used by the admin command: checks then places. */
    public static boolean force(ServerLevel level, LevelChunk chunk) { return place(level, chunk); }

    public static boolean enabled(ServerLevel level) {
        return level.getGameRules().get(RETROGEN);
    }

    /** Full eligibility probe: gamerule on, control resolves, no nearby market. */
    public static boolean check(ServerLevel level, LevelChunk chunk) { return isEligible(level, chunk); }

    private static boolean isEligible(ServerLevel level, LevelChunk chunk) {
        if (!enabled(level)) return false;
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var mercadao = structures.get(MERCADAO);
        var village = structures.get(VILLAGE_PLAINS);
        if (mercadao.isEmpty() || village.isEmpty()) return false;
        // The village control proves the structure engine works in this level;
        // if vanilla's own placements resolve, the proximity search is meaningful.
        BlockPos center = new BlockPos((chunk.getPos().x() << 4) + 8, 64, (chunk.getPos().z() << 4) + 8);
        var ref = level.findNearestMapStructure(
            net.minecraft.core.HolderSet.direct(village.get()), center, 1, false);
        if (ref == null) return false;
        return level.findNearestMapStructure(
            net.minecraft.core.HolderSet.direct(mercadao.get()), center, 2, false) == null;
    }

    private static boolean place(ServerLevel level, LevelChunk chunk) {
        var templates = level.getServer().getStructureTemplateManager();
        var template = templates.get(SncEnergies.id("mercadao"));
        if (template.isEmpty()) {
            SncEnergies.LOGGER.warn("RETROGEN mercadao template missing");
            return false;
        }
        var structure = template.get();
        var size = structure.getSize(Rotation.NONE);
        BlockPos corner = surfaceAnchor(level, chunk, size);
        if (corner == null) return false;
        var settings = new StructurePlaceSettings()
            .setRotation(Rotation.NONE)
            .setMirror(Mirror.NONE)
            .setIgnoreEntities(true);
        // The template already contains the mercadao_anchor cell, which keeps
        // the attendant alive exactly like worldgen placements do.
        return structure.placeInWorld(level, corner, corner, settings,
            level.getRandom(), 2);
    }

    /**
     * Chunk-local flat spot at ground level: the highest non-air block in the
     * center column, capped at sea level, then verified against the footprint.
     */
    private static BlockPos surfaceAnchor(ServerLevel level, LevelChunk chunk, net.minecraft.core.Vec3i size) {
        int centerX = chunk.getPos().getMinBlockX() + 2;
        int centerZ = chunk.getPos().getMinBlockZ() + 2;
        int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
            centerX, centerZ);
        BlockPos corner = new BlockPos(centerX, top, centerZ);
        // Reject water and other fluid ground: the market must sit on dry land.
        if (!level.getFluidState(corner.below()).isEmpty()) return null;
        for (int dx = -FOOTPRINT_RADIUS; dx <= FOOTPRINT_RADIUS; dx += 8) {
            for (int dz = -FOOTPRINT_RADIUS; dz <= FOOTPRINT_RADIUS; dz += 8) {
                BlockPos probe = corner.offset(dx, 1, dz);
                for (int dy = 0; dy < size.getY() + 1; dy++) {
                    if (!level.getBlockState(probe.above(dy)).isAir()) return null;
                }
            }
        }
        return corner;
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher,
                                         CommandBuildContext buildContext,
                                         Commands.CommandSelection selection) {
        dispatcher.register(Commands.literal("retrogen")
            .requires(source -> source.permissions().hasPermission(
                new net.minecraft.server.permissions.Permission.HasCommandLevel(
                    net.minecraft.server.permissions.PermissionLevel.ADMINS)))
            .then(Commands.literal("mercadao")
                .executes(context ->
                    retrogen(context.getSource(), context.getSource().getLevel(), true))
                .then(Commands.literal("here")
                    .executes(context -> retrogen(context.getSource(), context.getSource().getLevel(), false)))
                .then(Commands.literal("at")
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> {
                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
                            ServerLevel level = context.getSource().getLevel();
                            LevelChunk chunk = level.getChunkSource().getChunkNow(
                                pos.getX() >> 4, pos.getZ() >> 4);
                            if (chunk == null) {
                                context.getSource().sendFailure(
                                    Component.literal("chunk not loaded").withStyle(ChatFormatting.RED));
                                return 0;
                            }
                            boolean ok = MercadaoRetrogen.force(level, chunk);
                            feedback(context.getSource(), chunk.getPos(), ok);
                            return ok ? 1 : 0;
                        })))));
    }

    private static int retrogen(CommandSourceStack source, ServerLevel level, boolean checkFirst) {
        var player = source.getPlayer();
        ChunkPos pos = player != null ? player.chunkPosition()
            : ChunkPos.containing(BlockPos.containing(source.getPosition()));
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x(), pos.z());
        if (chunk == null) {
            source.sendFailure(Component.literal("chunk not loaded").withStyle(ChatFormatting.RED));
            return 0;
        }
        if (checkFirst && !check(level, chunk)) {
            source.sendFailure(Component.literal(
                "Retrogen não se aplica aqui: gamerule " + RULE_NAME + " desligada ou Mercadão próximo")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        boolean ok = MercadaoRetrogen.force(level, chunk);
        feedback(source, pos, ok);
        return ok ? 1 : 0;
    }

    private static void feedback(CommandSourceStack source, ChunkPos pos, boolean placed) {
        if (placed) {
            source.sendSuccess(() -> Component.literal(
                "Mercadão retrogen colocado em chunk " + pos.x() + ", " + pos.z()), true);
        } else {
            source.sendFailure(Component.literal(
                "Retrogen não aplicado: sem espaço livre no nível do chão, chão líquido ou template ausente")
                .withStyle(ChatFormatting.RED));
        }
    }
}
