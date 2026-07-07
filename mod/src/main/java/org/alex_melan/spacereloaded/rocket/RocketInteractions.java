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

    private RocketInteractions() {
    }

    public static void assemble(ServerLevel level, BlockPos commandPos, ServerPlayer player) {
        PartPropertiesResolver resolver = new PartPropertiesResolver(level);
        RocketAssembler.Result result = RocketAssembler.scan(level, commandPos, resolver,
                SpaceReloaded.config().rocketMaxBlocks);

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
                level.playSound(null, commandPos, SoundEvents.IRON_DOOR_OPEN,
                        SoundSource.BLOCKS, 1.5f, 0.7f);
            }
        }
    }
}
