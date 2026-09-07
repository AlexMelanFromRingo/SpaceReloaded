package org.alex_melan.spacereloaded.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
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
    /**
     * Скан-отчёт без сборки (AR-стиль): Δv, TWR, масса, предупреждения и
     * оценка Δv до первой цели профиля. Возвращает сводку (для стенда).
     */
    public static Component scanFromPylon(ServerLevel level, BlockPos pylonPos, ServerPlayer player) {
        formComplex(level, pylonPos);
        RocketAssembler.Result result = scanVolumeAtPylon(level, pylonPos, player);
        if (result == null) {
            return Component.translatable("message.spacereloaded.assembly.no_pad");
        }
        switch (result) {
            case RocketAssembler.Result.Error(String key, BlockPos pos) -> {
                Component message = Component.translatable(key,
                        pos.getX() + " " + pos.getY() + " " + pos.getZ());
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
                        org.alex_melan.spacereloaded.network.ScanReportPayload.error(key));
                return message;
            }
            case RocketAssembler.Result.Ok ok -> {
                ScanResult scan = scanResult(level, ok, pylonPos.getY());
                Component stats = Component.translatable("message.spacereloaded.rocket.stats",
                        String.format(Locale.ROOT, "%.0f", scan.performance().totalMassKg()),
                        String.format(Locale.ROOT, "%.0f", scan.performance().dryMassKg()),
                        String.format(Locale.ROOT, "%.0f", scan.performance().totalThrustN() / 1000),
                        String.format(Locale.ROOT, "%.2f", scan.performance().twr()),
                        String.format(Locale.ROOT, "%.0f", scan.staged().totalDeltaV()));
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, scan.payload());
                return stats;
            }
        }
    }

    /** Результат скана (Полёт 2.0): ЛТХ стека, ступени и пакет для экрана; используется стендом. */
    public record ScanResult(RocketPerformance performance,
                             org.alex_melan.spacereloaded.core.rocketry.StagedPerformance.StagedReport staged,
                             org.alex_melan.spacereloaded.network.ScanReportPayload payload) {
    }

    /**
     * Полный расчёт скана: ЛТХ, ступени (D12) и оценка достижимости высоты перехода.
     * TWR и тяга — активной (нижней) ступени: именно она отрывает стек от стола.
     */
    public static ScanResult scanResult(ServerLevel level, RocketAssembler.Result.Ok ok, int padY) {
        double gravity = org.alex_melan.spacereloaded.planet.PlanetManager.gravity(level);
        RocketPerformance performance = PerformanceCalculator.calculate(ok.structure(), gravity);
        var layout = org.alex_melan.spacereloaded.core.rocketry.StageLayout.of(ok.structure());
        double[] stageFuel = layout.propellantByStage();
        var staged = org.alex_melan.spacereloaded.core.rocketry.StagedPerformance
                .calculate(layout, stageFuel, gravity);
        var first = staged.stages().get(0);

        // Подъём (FR-084, D12): численное моделирование вертикального подъёма с
        // сопротивлением, гравитационными потерями и авто-отделением ступеней —
        // вместо оценки 1.15·√(2gh). Старт — низ стека, цель — высота перехода тела.
        var profile = org.alex_melan.spacereloaded.planet.PlanetManager.profileFor(level);
        var config = SpaceReloaded.config();
        double startY = ok.origin().getY();
        double targetY = profile.map(p -> (double) p.transitionAltitude()).orElse(startY);
        var ascent = org.alex_melan.spacereloaded.core.rocketry.AscentSimulator.simulate(
                layout, stageFuel, org.alex_melan.spacereloaded.planet.PlanetManager.environment(level),
                config.rocketDragCoefficient, startY, targetY);
        boolean reached = ascent.reachedTarget();
        double needed = ascent.deltaVSpentToTargetMs();
        boolean maxQExceeded = ascent.maxDynamicPressurePa() > config.maxDynamicPressurePa;

        java.util.LinkedHashSet<String> warnings = new java.util.LinkedHashSet<>();
        for (PerformanceWarning warning : first.warnings()) {
            warnings.add(warning.name());
        }
        for (var stage : staged.stages()) {
            if (stage.warnings().contains(PerformanceWarning.STAGE_NO_ENGINE)) {
                warnings.add(PerformanceWarning.STAGE_NO_ENGINE.name());
            }
        }
        if (maxQExceeded) {
            warnings.add(PerformanceWarning.MAX_Q_EXCEEDED.name());
        }
        java.util.List<org.alex_melan.spacereloaded.network.ScanReportPayload.StageLine> lines =
                staged.stages().stream()
                        .map(s -> new org.alex_melan.spacereloaded.network.ScanReportPayload.StageLine(
                                s.index(), s.deltaV(), s.twr(), layout.stage(s.index()).hasEngines()))
                        .toList();
        var payload = new org.alex_melan.spacereloaded.network.ScanReportPayload(
                ok.blocks().size(), performance.totalMassKg(), performance.dryMassKg(),
                first.thrustN(), first.twr(), staged.totalDeltaV(), needed,
                java.util.List.copyOf(warnings), "", lines, reached, needed,
                ascent.apexM(), ascent.maxDynamicPressurePa(), maxQExceeded);
        return new ScanResult(performance, staged, payload);
    }

    /** Общий каркас скана площадки от пилона (null — нет площадки). */
    private static RocketAssembler.Result scanVolumeAtPylon(ServerLevel level, BlockPos pylonPos,
                                                            ServerPlayer player) {
        return runPylonScan(level, pylonPos, player);
    }

    /**
     * Скан без пакета игроку (стенд): результат расчёта либо null, если площадки
     * нет или стек не прочитался (ошибка уходит игроку сообщением).
     */
    @org.jetbrains.annotations.Nullable
    public static ScanResult scanResultAtPylon(ServerLevel level, BlockPos pylonPos, ServerPlayer player) {
        formComplex(level, pylonPos);
        RocketAssembler.Result result = runPylonScan(level, pylonPos, player);
        if (result instanceof RocketAssembler.Result.Ok ok) {
            return scanResult(level, ok, pylonPos.getY());
        }
        if (result instanceof RocketAssembler.Result.Error(String key, BlockPos pos)) {
            player.sendSystemMessage(Component.translatable(key,
                    pos.getX() + " " + pos.getY() + " " + pos.getZ()));
        }
        return null;
    }

    public static void assembleFromPylon(ServerLevel level, BlockPos pylonPos, ServerPlayer player) {
        formComplex(level, pylonPos);
        RocketAssembler.Result scanned = runPylonScan(level, pylonPos, player);
        if (scanned != null) {
            finishAssembly(level, player, scanned);
        }
    }

    /** Каркас: колонна пилона + флудфилл плит + скан объёма (null — нет пада). */
    private static RocketAssembler.Result runPylonScan(ServerLevel level, BlockPos pylonPos,
                                                       ServerPlayer player) {
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
            return null;
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
        return RocketAssembler.scanVolume(level, cells, minY, maxY,
                resolver, SpaceReloaded.config().rocketMaxBlocks);
    }

    /**
     * Формирование стартового комплекса (визуальный отклик): пад ≥ 9 плит +
     * пилон ≥ 3 блока, стоящий на паде, — вся конструкция получает FORMED=true
     * (жёлтая разметка). Вызывается при установке плит/пилона и перед сборкой.
     */
    public static void formComplex(ServerLevel level, BlockPos seed) {
        // Найти затравку-пад: сам блок, под пилоном либо сбоку от его низа
        BlockPos padSeed = null;
        if (level.getBlockState(seed).is(org.alex_melan.spacereloaded.registry.ModBlocks.LAUNCH_PAD)) {
            padSeed = seed;
        } else if (level.getBlockState(seed).is(org.alex_melan.spacereloaded.registry.ModBlocks.ASSEMBLY_PYLON)) {
            BlockPos base = seed;
            while (level.getBlockState(base.below()).is(org.alex_melan.spacereloaded.registry.ModBlocks.ASSEMBLY_PYLON)) {
                base = base.below();
            }
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
        }
        if (padSeed == null) {
            return;
        }
        // Флудфилл плит (4 направления, как при сборке)
        java.util.List<BlockPos> pads = new java.util.ArrayList<>();
        java.util.Set<BlockPos> visited = new java.util.HashSet<>();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        queue.add(padSeed.immutable());
        visited.add(padSeed.immutable());
        while (!queue.isEmpty() && pads.size() <= MAX_PAD_CELLS) {
            BlockPos cell = queue.poll();
            pads.add(cell);
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos next = cell.relative(dir);
                if (visited.add(next)
                        && level.getBlockState(next).is(org.alex_melan.spacereloaded.registry.ModBlocks.LAUNCH_PAD)) {
                    queue.add(next);
                }
            }
        }
        // Колонны пилонов, стоящие на плитах
        java.util.List<BlockPos> pylons = new java.util.ArrayList<>();
        int tallest = 0;
        for (BlockPos pad : pads) {
            BlockPos cursor = pad.above();
            int height = 0;
            while (level.getBlockState(cursor).is(org.alex_melan.spacereloaded.registry.ModBlocks.ASSEMBLY_PYLON)) {
                pylons.add(cursor.immutable());
                cursor = cursor.above();
                height++;
            }
            tallest = Math.max(tallest, height);
        }
        boolean formed = pads.size() >= 9 && tallest >= 3;
        for (BlockPos pad : pads) {
            BlockState state = level.getBlockState(pad);
            if (state.getValue(LaunchPadBlock.FORMED) != formed) {
                level.setBlock(pad, state.setValue(LaunchPadBlock.FORMED, formed), 3);
            }
        }
        for (BlockPos pylon : pylons) {
            BlockState state = level.getBlockState(pylon);
            if (state.getValue(AssemblyPylonBlock.FORMED) != formed) {
                level.setBlock(pylon, state.setValue(AssemblyPylonBlock.FORMED, formed), 3);
            }
        }
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
                // Полёт 2.0: топливо по ступеням — честные суммы баков каждой ступени
                var layout = org.alex_melan.spacereloaded.core.rocketry.StageLayout.of(ok.structure());
                double[] stageFuel = layout.propellantByStage();
                var staged = org.alex_melan.spacereloaded.core.rocketry.StagedPerformance
                        .calculate(layout, stageFuel, 9.81);

                player.sendSystemMessage(Component.translatable(
                        "message.spacereloaded.rocket.assembled", ok.blocks().size()));
                player.sendSystemMessage(Component.translatable(
                        "message.spacereloaded.rocket.stats",
                        String.format(Locale.ROOT, "%.0f", performance.totalMassKg()),
                        String.format(Locale.ROOT, "%.0f", performance.dryMassKg()),
                        String.format(Locale.ROOT, "%.0f", staged.stages().get(0).thrustN() / 1000),
                        String.format(Locale.ROOT, "%.2f", staged.stages().get(0).twr()),
                        String.format(Locale.ROOT, "%.0f", staged.totalDeltaV())));
                for (PerformanceWarning warning : staged.stages().get(0).warnings()) {
                    player.sendSystemMessage(Component.translatable(
                            "message.spacereloaded.rocket.warning." + warning.name()));
                }
                player.sendSystemMessage(Component.translatable(
                        "message.spacereloaded.rocket.mount_hint"));

                // Груз из отсеков — в сущность (до изъятия блоков!)
                java.util.List<net.minecraft.world.item.ItemStack> cargo = new java.util.ArrayList<>();
                for (RocketAssembler.ScannedBlock block : ok.blocks()) {
                    if (level.getBlockEntity(block.worldPos())
                            instanceof CargoHoldBlockEntity hold) {
                        for (int slot = 0; slot < hold.getContainerSize(); slot++) {
                            net.minecraft.world.item.ItemStack stack = hold.getItem(slot);
                            if (!stack.isEmpty()) {
                                cargo.add(stack.copy());
                            }
                        }
                        hold.clearContent();
                    }
                }
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
                rocket.setAssembly(RocketData.fromScan(ok.blocks(), performance.propellantMassKg()),
                        stageFuel);
                rocket.setCargo(cargo);
                level.addFreshEntity(rocket);
                level.playSound(null, ok.commandWorldPos(), SoundEvents.IRON_DOOR_OPEN,
                        SoundSource.BLOCKS, 1.5f, 0.7f);
            }
        }
    }
}
