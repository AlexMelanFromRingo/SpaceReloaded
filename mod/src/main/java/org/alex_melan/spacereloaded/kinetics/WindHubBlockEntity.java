package org.alex_melan.spacereloaded.kinetics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.config.SpaceReloadedConfig;
import org.alex_melan.spacereloaded.core.kinetics.NodeLoad;
import org.alex_melan.spacereloaded.core.kinetics.WindRotor;
import org.alex_melan.spacereloaded.network.MarsClimate;
import org.alex_melan.spacereloaded.planet.PlanetManager;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModBlocks;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Ступица ветроколеса (005, FR-340, D59): P = ½ρπR²v³·C_p(λ). R — расстояние до самого дальнего
 * паруса связной группы в плоскости, перпендикулярной оси; ρ — плотность атмосферы тела у
 * поверхности (вакуум — колесо не вращается); v — ветер тела из конфига (Марс в бурю — сильнее).
 * Момент τ(ω) нелинеен — решателю отдаётся касательная a − b·ω в текущей точке.
 */
public class WindHubBlockEntity extends KineticBlockEntity {

    private static final int MAX_SAILS = 256;
    private double radius;
    private int sails;
    private long lastScan = Long.MIN_VALUE / 2;
    private double windSpeed;
    private double density;

    public WindHubBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIND_HUB, pos, state);
    }

    @Override
    public void serverTick(ServerLevel level) {
        super.serverTick(level);
        if (level.getGameTime() - lastScan >= 200) {
            lastScan = level.getGameTime();
            scanSails(level);
            updateWind(level);
            KineticNetworks.wake(level, getBlockPos());
        }
    }

    private void updateWind(ServerLevel level) {
        SpaceReloadedConfig config = SpaceReloaded.config();
        density = PlanetManager.aero(level).density(getBlockPos().getY());
        String id = level.dimension().identifier().getPath();
        if ("mars".equals(id)) {
            windSpeed = MarsClimate.stormActive(level) ? config.windSpeedMarsStorm : config.windSpeedMars;
        } else {
            windSpeed = density > 0 ? config.windSpeedEarth : 0;
        }
    }

    private void scanSails(ServerLevel level) {
        Direction.Axis axis = getBlockState().getValue(KineticAxisBlock.AXIS);
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(getBlockPos());
        seen.add(getBlockPos());
        double maxR = 0;
        int count = 0;
        while (!queue.isEmpty() && count < MAX_SAILS) {
            BlockPos cur = queue.poll();
            for (Direction d : Direction.values()) {
                if (d.getAxis() == axis) {
                    continue;
                }
                BlockPos n = cur.relative(d);
                if (seen.add(n) && level.isLoaded(n) && level.getBlockState(n).is(ModBlocks.SAIL)) {
                    queue.add(n);
                    count++;
                    maxR = Math.max(maxR, Math.sqrt(n.distSqr(getBlockPos())));
                }
            }
        }
        sails = count;
        radius = count == 0 ? 0 : maxR + 0.5;
    }

    @Override
    public NodeLoad load(ServerLevel level) {
        double friction = SpaceReloaded.config().kineticFrictionTorqueNm;
        double inertia = 20 + sails * 15.0;
        if (radius <= 0 || density <= 0 || windSpeed <= 0) {
            return new NodeLoad(0, 0, friction, inertia);
        }
        double w = Math.abs(omega);
        double t0 = WindRotor.torque(density, radius, windSpeed, w);
        double h = 0.01 * Math.max(1, w);
        double slope = Math.max(0, -(WindRotor.torque(density, radius, windSpeed, w + h) - t0) / h);
        return new NodeLoad(t0 + slope * w, slope, friction, inertia);
    }

    @Override
    protected void extraReport(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("message.spacereloaded.kinetic.wind",
                sails, String.format(Locale.ROOT, "%.1f", radius), String.format(Locale.ROOT, "%.1f", windSpeed),
                String.format(Locale.ROOT, "%.3f", density),
                String.format(Locale.ROOT, "%.1f", Math.abs(sourcePower) / 1000)));
    }
}
