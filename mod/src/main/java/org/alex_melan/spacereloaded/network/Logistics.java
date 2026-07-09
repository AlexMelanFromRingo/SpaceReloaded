package org.alex_melan.spacereloaded.network;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.alex_melan.spacereloaded.registry.ModRegistries;

/**
 * Правила глубокой логистики (Phase 12): межпланетный БЕСПИЛОТНЫЙ перелёт к
 * цели с requires_coverage требует спутникового покрытия на измерении вылета
 * (ретрансляция команд наземного ЦУПа). Пилотируемый рейс не требует: экипаж
 * командует аппаратом напрямую.
 */
public final class Logistics {

    private Logistics() {
    }

    public static boolean coverageSatisfied(MinecraftServer server, ResourceKey<Level> fromDimension,
                                            ModRegistries.PlanetProfile target, boolean unmanned) {
        if (!target.requiresCoverage() || !unmanned) {
            return true;
        }
        return SpaceNetworkState.get(server).hasCoverage(fromDimension);
    }
}
