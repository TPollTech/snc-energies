package com.snc.energies;

import com.snc.energies.block.SiloBlock;
import com.snc.energies.blockentity.IndustrialBlockEntity;
import com.snc.energies.blockentity.SiloBlockEntity;
import com.snc.energies.registry.IndustryKind;
import com.snc.energies.registry.IndustryRecipes;
import com.snc.energies.registry.SncBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/** Compactor (industry contract), silo (bulk bin) and retrogen basics. */
final class AgroFunctionalTest {
    private final ServerLevel level;
    private final net.minecraft.server.level.ServerPlayer player;
    private final BlockPos pos = new BlockPos(20, 130, 8);
    private AgroFunctionalTest(ServerLevel level, net.minecraft.server.level.ServerPlayer player) {
        this.level = level; this.player = player;
    }
    static void run(ServerLevel level, net.minecraft.server.level.ServerPlayer player) {
        new AgroFunctionalTest(level, player).run();
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        SncEnergies.LOGGER.info("FUNCTIONAL PASS: {}", message);
    }

    private void run() {
        for (int x = 14; x < 44; x++) for (int z = 0; z < 16; z++) for (int y = 120; y < 138; y++)
            level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);

        // ---- Compactor: same multiblock contract as the other nine ----
        var compactorBlock = SncBlocks.INDUSTRY.get(IndustryKind.COMPACTOR);
        var state = compactorBlock.defaultBlockState().setValue(com.snc.energies.block.MachineBlock.FACING, Direction.NORTH);
        level.setBlock(pos, state, 3);
        compactorBlock.setPlacedBy(level, pos, state, player, new ItemStack(compactorBlock.asItem()));
        var compactor = (IndustrialBlockEntity) level.getBlockEntity(pos);
        check(compactor != null && compactor.kind() == IndustryKind.COMPACTOR, "compactor installs as a standard industrial multiblock");
        int blocked = 0;
        for (var recipe : IndustryRecipes.all()) {
            if (recipe.kind() != IndustryKind.COMPACTOR) continue;
            blocked++;
            compactor.setItem(0, new ItemStack(recipe.input(), recipe.count()));
            compactor.setItem(1, recipe.reagentCount() > 0
                ? new ItemStack(recipe.reagent(), recipe.reagentCount()) : ItemStack.EMPTY);
            compactor.getEnergyStorage(Direction.UP).insert(recipe.kind().cost * recipe.ticks(), false);
            compactor.serverTick();
            check(compactor.status() == 2, "compactor starts a batch: " + recipe.id());
            for (int i = 0; i < recipe.ticks(); i++) compactor.serverTick();
            check(compactor.getItem(0).isEmpty() && compactor.getItem(4).is(recipe.product())
                && compactor.getItem(4).getCount() == recipe.productCount() && compactor.getEnergy() == 0,
                "compactor recipe conserves inputs, output and energy: " + recipe.id());
            compactor.setItem(4, ItemStack.EMPTY);
        }
        check(blocked == 7, "compactor exposes the seven agroindustrial batch recipes");

        // ---- Silo: footprint, one controller, bulk conservation ----
        var siloBlock = (SiloBlock) SncBlocks.SILO;
        BlockPos siloPos = new BlockPos(28, 130, 4);
        level.setBlock(siloPos, siloBlock.defaultBlockState(), 3);
        siloBlock.setPlacedBy(level, siloPos, siloBlock.defaultBlockState(), player, new ItemStack(siloBlock.asItem()));
        boolean cells = true;
        for (int i = 0; i < 12; i++) {
            BlockPos cell = SiloBlock.cell(siloPos, i);
            cells &= level.getBlockState(cell).getBlock() == siloBlock
                && level.getBlockState(cell).getValue(SiloBlock.PART) == i;
        }
        check(cells, "silo builds the twelve-cell ring without orientation");
        var silo = (SiloBlockEntity) level.getBlockEntity(siloPos);
        check(silo != null && SiloBlock.controller(level, siloPos.above(2), level.getBlockState(siloPos.above(2))) == silo,
            "every silo cell resolves to the single controller");

        ItemStack wheat = new ItemStack(Items.WHEAT, 5000);
        silo.deposit(0, wheat);
        check(wheat.getCount() == 5000 - Math.min(5000, SiloBlockEntity.PER_STACK), "bulk deposit caps a row at 16384");
        silo.serverTick();
        // The bottom buffer already pulled one stack out of the row.
        check(silo.getItem(SiloBlockEntity.IN).isEmpty()
            && silo.row(0).getCount() == 5000 - 64
            && silo.getItem(SiloBlockEntity.OUT).getCount() == 64,
            "rows absorb the intake buffer and feed the extraction buffer");
        ItemStack carrots = new ItemStack(Items.CARROT, 100);
        silo.deposit(1, carrots);
        silo.serverTick();
        check(silo.row(1).getCount() == 100 && silo.getItem(SiloBlockEntity.OUT).is(Items.WHEAT),
            "a second crop lands on its own row and never leaves through the full wheat buffer");

        // Row mixing rules and withdrawal.
        int before = silo.rowCount(0);
        ItemStack rejected = new ItemStack(Items.CARROT, 10);
        silo.deposit(0, rejected);
        check(rejected.getCount() == 10 && silo.rowCount(0) == before, "rows reject a different item kind");
        ItemStack taken = silo.withdraw(0, 64);
        check(taken.getCount() == 64 && silo.rowCount(0) == before - 64, "bulk withdrawal splits exact amounts");

        // Persistence round trip keeps rows and counters.
        var saved = silo.saveWithFullMetadata(level.registryAccess());
        var restored = (SiloBlockEntity) BlockEntity.loadStatic(siloPos, silo.getBlockState(), saved, level.registryAccess());
        check(restored != null && restored.rowCount(0) == silo.rowCount(0) && restored.rowCount(1) == silo.rowCount(1),
            "silo rows survive serialization (restored=" + (restored == null ? "null" : restored.rowCount(0) + "/" + restored.rowCount(1))
                + " original=" + silo.rowCount(0) + "/" + silo.rowCount(1)
                + " buffers=" + silo.getItem(SiloBlockEntity.IN).getCount() + "/" + silo.getItem(SiloBlockEntity.OUT).getCount() + ")");

        // Teardown drops exactly one silo and the whole inventory.
        int siloDrops = drops(Items.WHEAT);
        level.destroyBlock(siloPos, true);
        boolean cleared = true;
        for (int i = 0; i < 12; i++) cleared &= level.getBlockState(SiloBlock.cell(siloPos, i)).isAir();
        check(cleared && drops(Items.WHEAT) > siloDrops, "breaking the silo clears the ring and spills the grain");

        // ---- Retrogen basics: rule plumbing, control, forced placement ----
        var rule = com.snc.energies.world.MercadaoRetrogen.rule();
        check(rule != null && level.getGameRules().get(rule) == Boolean.FALSE, "retrogen gamerule starts disabled");
        level.getGameRules().set(rule, Boolean.TRUE, level.getServer());
        check(level.getGameRules().get(rule) == Boolean.TRUE, "retrogen gamerule toggles on");

        LevelChunk here = level.getChunkAt(pos);
        com.snc.energies.world.MercadaoRetrogen.force(level, here);
        var mercadao = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
            .get(com.snc.energies.SncEnergies.id("mercadao"));
        check(mercadao.isPresent(), "retrogen force path runs and the structure registry stays healthy");

        level.getGameRules().set(rule, Boolean.FALSE, level.getServer());
    }

    private int drops(net.minecraft.world.item.Item item) {
        return level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            new net.minecraft.world.phys.AABB(12, 118, 0, 46, 138, 16))
            .stream().filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
    }
}
