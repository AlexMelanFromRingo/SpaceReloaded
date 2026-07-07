package org.alex_melan.spacereloaded.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.core.rocketry.PerformanceCalculator;
import org.alex_melan.spacereloaded.core.rocketry.PerformanceWarning;
import org.alex_melan.spacereloaded.core.rocketry.RocketPerformance;
import org.alex_melan.spacereloaded.registry.ModEntities;

import java.util.Locale;

/**
 * Сборка ракеты по ПКМ на командном модуле (T051/T053): скан → отчёт о ЛТХ в чат
 * → блоки поднимаются в сущность. Флаги удаления блоков: без дропов, без
 * сайд-эффектов блок-сущностей (2|32|256).
 */
public final class RocketInteractions {

    private static final int LIFT_FLAGS = 2 | 32 | 256;
    /** Лимит клеток площадки (32×32). */
    private static final int MAX_PAD_CELLS = 1024;

    private RocketInteractions() {
    }

    /** Сборка со стартовой площадки: ПКМ по пилону (T051, AR-стиль). */
    public static void assembleFromPylon(ServerLevel level, BlockPos pylonPos, ServerPlayer player) {
        // Низ колонны пилона
        BlockPos base = pylonPos;
        while (level.getBlockState(base.below()).is(org.alex_melan.spacereloaded.registry.ModBlocks.ASSEMBLY_PYLON)) {
            base = base.below();
        }
        // Высота колонны
        int height = 0;
        BlockPos top = base;
        while (level.getBlockState(top).is(org.alex_melan.spacereloaded.registry.ModBlocks.ASSEMBLY_PYLON)) {
            height++;
            top = top.above();
        }
        // Площадка: плита под пилоном либо по горизонтали от его низа
        BlockPos padSeed = null;
        if (level.getBlockState(base.below()).is(org.alex_melan.spacereloaded.registry.ModBlocks.LAUNCH_PAD)) {
            padSeed = base.below();
        } else {
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                if (level.getBlockState(base.relative(dir)).is(org.alex_melan.spacereloaded.registry.ModBlocks.LAUNCH_PAD)) {
                    padSeed = base.relative(dir);
                    break;
                }
            }
        }
        if (padSeed == null) {
            player.sendSystemMessage(Component.translatable("message.spacereloaded.assembly.no_pad"));
            return;
        }
        // Клетки площадки: BFS по плитам в горизонтальной плоскости
        java.util.List<BlockPos> cells = new java.util.ArrayList<>();
        java.util.Set<BlockPos> visited = new java.util.HashSet<>();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        queue.add(padSeed.immutable());
        visited.add(padSeed.immutable());
        while (!queue.isEmpty() && cells.size() <= MAX_PAD_CELLS) {
            BlockPos cell = queue.poll();
            cells.add(cell);
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos next = cell.relative(dir);
                if (visited.add(next)
                        && level.getBlockState(next).is(org.alex_melan.spacereloaded.registry.ModBlocks.LAUNCH_PAD)) {
                    queue.add(next);
                }
            }
        }

        PartPropertiesResolver resolver = new PartPropertiesResolver(level);
        int minY = padSeed.getY() + 1;
        int maxY = padSeed.getY() + Math.max(1, height);
        RocketAssembler.Result result = RocketAssembler.scanVolume(level, cells, minY, maxY,
                resolver, SpaceReloaded.config().rocketMaxBlocks);
        finishAssembly(level, player, result);
    }

    public static void assemble(ServerLevel level, BlockPos commandPos, ServerPlayer player) {
        PartPropertiesResolver resolver = new PartPropertiesResolver(level);
        RocketAssembler.Result result = RocketAssembler.scan(level, commandPos, resolver,
                SpaceReloaded.config().rocketMaxBlocks);
        finishAssembly(level, player, result);
    }

    private static void finishAssembly(ServerLevel level, ServerPlayer player,
                                       RocketAssembler.Result result) {
        switch (result) {
            case RocketAssembler.Result.Error(String key, BlockPos pos) ->
                    player.sendSystemMessage(Component.translatable(key,
                            pos.getX() + " " + pos.getY() + " " + pos.getZ()));
            case RocketAssembler.Result.Ok ok -> {
                RocketPerformance performance =
                        PerformanceCalculator.calculate(ok.structure(), 9.81);

                player.sendSystemMessage(Component.translatable(
                        "message.spacereloaded.rocket.assembled", ok.blocks().size()));
                player.sendSystemMessage(Component.translatable(
                        "message.spacereloaded.rocket.stats",
                        String.format(Locale.ROOT, "%.0f", performance.totalMassKg()),
                        String.format(Locale.ROOT, "%.0f", performance.dryMassKg()),
                        String.format(Locale.ROOT, "%.0f", performance.totalThrustN() / 1000),
                        String.format(Locale.ROOT, "%.2f", performance.twr()),
                        String.format(Locale.ROOT, "%.0f", performance.deltaV())));
                for (PerformanceWarning warning : performance.warnings()) {
                    player.sendSystemMessage(Component.translatable(
                            "message.spacereloaded.rocket.warning." + warning.name()));
                }
                player.sendSystemMessage(Component.translatable(
                        "message.spacereloaded.rocket.mount_hint"));

                // Блоки — в сущность
                for (RocketAssembler.ScannedBlock block : ok.blocks()) {
                    level.setBlock(block.worldPos(), Blocks.AIR.defaultBlockState(), LIFT_FLAGS);
                }

                RocketEntity rocket = new RocketEntity(ModEntities.ROCKET, level);
                double sizeX = ok.blocks().stream()
                        .mapToInt(b -> org.alex_melan.spacereloaded.core.geometry.PackedPos.unpackX(b.localPos()))
                        .max().orElse(0) + 1;
                double sizeZ = ok.blocks().stream()
                        .mapToInt(b -> org.alex_melan.spacereloaded.core.geometry.PackedPos.unpackZ(b.localPos()))
                        .max().orElse(0) + 1;
                rocket.setPos(ok.origin().getX() + sizeX / 2.0, ok.origin().getY(),
                        ok.origin().getZ() + sizeZ / 2.0);
                rocket.setAssembly(RocketData.fromScan(ok.blocks(), performance.propellantMassKg()));
                level.addFreshEntity(rocket);
                level.playSound(null, ok.commandWorldPos(), SoundEvents.IRON_DOOR_OPEN,
                        SoundSource.BLOCKS, 1.5f, 0.7f);
            }
        }
    }
}
