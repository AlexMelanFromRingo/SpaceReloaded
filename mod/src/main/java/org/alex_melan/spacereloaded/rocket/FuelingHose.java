package org.alex_melan.spacereloaded.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Заправочный рукав: ПКМ по баку — подключить, ПКМ по припаркованной ракете —
 * перекачать из бака в ракету, sneak+ПКМ по ракете — слить обратно в бак.
 * Привязка живёт в памяти сервера (перелогин — переподключить рукав).
 */
public final class FuelingHose {

    /** Дотяжка рукава от бака до ракеты, блоки. */
    public static final double REACH = 16.0;

    private static final Map<UUID, GlobalPos> LINKS = new HashMap<>();

    private FuelingHose() {
    }

    public static void link(ServerPlayer player, ServerLevel level, BlockPos tankPos) {
        LINKS.put(player.getUUID(), GlobalPos.of(level.dimension(), tankPos.immutable()));
        player.sendOverlayMessage(Component.translatable("message.spacereloaded.hose.linked"));
    }

    /** @return бак, к которому подключен рукав игрока (в этом же измерении и в радиусе) */
    public static FuelTankBlockEntity linkedTank(ServerPlayer player, ServerLevel level, double x, double y, double z) {
        GlobalPos link = LINKS.get(player.getUUID());
        if (link == null || !link.dimension().equals(level.dimension())) {
            return null;
        }
        BlockPos pos = link.pos();
        if (pos.distToCenterSqr(x, y, z) > REACH * REACH) {
            return null;
        }
        return level.getBlockEntity(pos) instanceof FuelTankBlockEntity tank ? tank : null;
    }

    /** Перекачка бак → ракета. */
    public static void pumpToRocket(ServerPlayer player, ServerLevel level, RocketEntity rocket) {
        FuelTankBlockEntity tank = linkedTank(player, level, rocket.getX(), rocket.getY(), rocket.getZ());
        if (tank == null) {
            player.sendOverlayMessage(Component.translatable("message.spacereloaded.hose.no_tank"));
            return;
        }
        double accepted = rocket.refuel(tank.propellantKg());
        if (accepted <= 0) {
            player.sendOverlayMessage(Component.translatable("message.spacereloaded.hose.rocket_full"));
            return;
        }
        tank.setPropellantKg(tank.propellantKg() - accepted);
        player.sendOverlayMessage(Component.translatable("message.spacereloaded.hose.pumped",
                String.format(Locale.ROOT, "%.0f", accepted)));
    }

    /** Слив ракета → бак. */
    public static void drainFromRocket(ServerPlayer player, ServerLevel level, RocketEntity rocket) {
        FuelTankBlockEntity tank = linkedTank(player, level, rocket.getX(), rocket.getY(), rocket.getZ());
        if (tank == null) {
            player.sendOverlayMessage(Component.translatable("message.spacereloaded.hose.no_tank"));
            return;
        }
        double drained = rocket.drain(tank.capacityKg() - tank.propellantKg());
        if (drained <= 0) {
            player.sendOverlayMessage(Component.translatable("message.spacereloaded.hose.nothing"));
            return;
        }
        tank.fill(drained);
        player.sendOverlayMessage(Component.translatable("message.spacereloaded.hose.drained",
                String.format(Locale.ROOT, "%.0f", drained)));
    }
}
