package org.alex_melan.spacereloaded.network;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.planet.PlanetManager;
import org.alex_melan.spacereloaded.registry.ModDamageTypes;
import org.alex_melan.spacereloaded.sealing.ZoneManager;

/**
 * Пылевые бури Марса (Phase 12, полировка): глобальное событие измерения из
 * {@link SpaceNetworkState}. В бурю солнечные панели теряют ~90% (см.
 * SolarPanelBlockEntity), незащищённых на поверхности точит абразивная пыль,
 * а воздух заполняется пеплом. Это заставляет полагаться на РИТЭГи и строить
 * закрытые ангары.
 */
public final class MarsClimate {

    private MarsClimate() {
    }

    /** Идёт ли сейчас буря в этом измерении (для солнечных панелей и т.п.). */
    public static boolean stormActive(ServerLevel level) {
        return SpaceNetworkState.get(level.getServer())
                .stormActive(level.dimension(), level.getGameTime());
    }

    public static void tick(ServerLevel level) {
        var config = SpaceReloaded.config();
        if (!config.dustStormsEnabled) {
            return;
        }
        var profile = PlanetManager.profileFor(level);
        if (profile.isEmpty() || !"co2".equals(profile.get().atmosphere())
                || "platform".equals(profile.get().arrival())) {
            return; // только пыльные CO2-поверхности (Марс)
        }
        long time = level.getGameTime();
        SpaceNetworkState net = SpaceNetworkState.get(level.getServer());

        if (time % config.dustStormCheckInterval == 0
                && !net.stormActive(level.dimension(), time)
                && level.getRandom().nextDouble() < config.dustStormChance) {
            net.startStorm(level.dimension(), time + config.dustStormDurationTicks);
            for (ServerPlayer player : level.players()) {
                player.sendSystemMessage(Component.translatable("message.spacereloaded.storm.start"));
            }
        }

        if (!net.stormActive(level.dimension(), time)) {
            return;
        }
        if (time % 10 != 0) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            level.sendParticles(ParticleTypes.ASH, player.getX(), player.getY() + 1, player.getZ(),
                    30, 6.0, 4.0, 6.0, 0.02);
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }
            // Абразивная пыль точит ЛЮБОГО вне герметичной зоны — скафандр от
            // вакуума спасает, от песчаной бури нет. Так буря гонит в закрытый
            // ангар даже полностью экипированных (иначе урон был бы поглощён
            // i-frames от удушья у незащищённых и не значил бы ничего).
            if (!ZoneManager.isInsideSealedZone(level, player.blockPosition())) {
                player.hurtServer(level, ModDamageTypes.exposure(level), config.dustStormDamage);
            }
        }
    }
}
