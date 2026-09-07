package org.alex_melan.spacereloaded.rocket;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.core.geometry.PackedPos;
import org.alex_melan.spacereloaded.core.geometry.Vec3d;
import org.alex_melan.spacereloaded.core.rocketry.FlightIntegrator;
import org.alex_melan.spacereloaded.core.rocketry.FlightState;
import org.alex_melan.spacereloaded.core.rocketry.StageLayout;
import org.alex_melan.spacereloaded.registry.ModEntities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Отделение ступени в полёте (Полёт 2.0, FR-066/FR-067, D15). Плоскость —
 * верх активной ступени: всё на ней и ниже (включая разделитель) становится
 * обломком — самостоятельной {@link RocketEntity} без экипажа с заглушёнными
 * двигателями и своим остатком топлива; верхний стек остаётся той же сущностью
 * с экипажем, целью, программой и грузом.
 *
 * <p>Импульс разделения J (конфиг) сохраняет суммарный импульс системы:
 * Δv_верх = +J/m_верх, Δv_низ = −J/m_низ вдоль оси аппарата.
 */
public final class StageSeparation {

    private StageSeparation() {
    }

    private record Bounds(int minX, int minY, int minZ, int sizeX, int sizeZ) {
    }

    /**
     * @return сообщение пилоту: успех (номер отброшенной ступени, осталось) либо причина отказа
     */
    public static Component separate(ServerLevel level, RocketEntity rocket) {
        StageLayout layout = rocket.layout();
        int active = rocket.activeStage();
        if (layout == null || active >= layout.stageCount() - 1) {
            return Component.translatable("message.spacereloaded.stage.last");
        }
        int topY = layout.stage(active).topY();
        RocketData data = rocket.rocketData();
        List<RocketData.Entry> lower = new ArrayList<>();
        List<RocketData.Entry> upper = new ArrayList<>();
        for (RocketData.Entry entry : data.blocks()) {
            (PackedPos.unpackY(entry.localPos()) <= topY ? lower : upper).add(entry);
        }
        if (lower.isEmpty() || upper.isEmpty()) {
            return Component.translatable("message.spacereloaded.stage.last");
        }

        double[] stages = rocket.stagePropellantSnapshot();
        double lowerFuel = active < stages.length ? Math.max(0, stages[active]) : 0;
        double[] upperStages = active + 1 < stages.length
                ? Arrays.copyOfRange(stages, active + 1, stages.length)
                : new double[layout.stageCount() - active - 1];
        double upperFuel = 0;
        for (double kg : upperStages) {
            upperFuel += kg;
        }
        double lowerMass = dryMass(lower) + lowerFuel;
        double upperMass = dryMass(upper) + upperFuel;

        // Сохранение импульса: части расходятся вдоль оси аппарата
        FlightState state = rocket.flightState();
        Vec3d axis = FlightIntegrator.axis(state.pitch(), state.roll());
        double impulse = SpaceReloaded.config().stageSeparationImpulseNs;
        Vec3d deltaUpper = axis.scale(impulse / Math.max(1.0, upperMass));
        Vec3d deltaLower = axis.scale(-impulse / Math.max(1.0, lowerMass));

        Vec3 base = new Vec3(rocket.getX() - rocket.halfX(), rocket.getY(), rocket.getZ() - rocket.halfZ());

        // Обломок: отдельная сущность, летит без экипажа до удара, парковки или утилизации
        Bounds lowerBounds = bounds(lower);
        RocketEntity fragment = new RocketEntity(ModEntities.ROCKET, level);
        fragment.setPos(base.x + lowerBounds.minX() + lowerBounds.sizeX() / 2.0,
                base.y + lowerBounds.minY(),
                base.z + lowerBounds.minZ() + lowerBounds.sizeZ() / 2.0);
        fragment.setAssembly(new RocketData(normalize(lower, lowerBounds), lowerFuel));
        fragment.markDebris(state.vel().add(deltaLower), state.pitch(), state.roll());
        level.addFreshEntity(fragment);

        // Верхний стек: та же сущность, координаты нормализованы, позиция поднята на высоту ступени
        Bounds upperBounds = bounds(upper);
        Vec3 newPos = new Vec3(base.x + upperBounds.minX() + upperBounds.sizeX() / 2.0,
                base.y + upperBounds.minY(),
                base.z + upperBounds.minZ() + upperBounds.sizeZ() / 2.0);
        rocket.applyRemainingStack(new RocketData(normalize(upper, upperBounds), upperFuel),
                newPos, upperStages, deltaUpper);

        level.playSound(null, rocket.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.NEUTRAL, 1.2f, 1.7f);
        level.sendParticles(ParticleTypes.POOF, newPos.x, newPos.y, newPos.z,
                30, rocket.halfX(), 0.3, rocket.halfZ(), 0.05);
        SpaceReloaded.LOGGER.info("Отделение ступени {}: обломок {} бл. ({} кг), остаток {} бл. ({} кг)",
                active + 1, lower.size(), Math.round(lowerMass), upper.size(), Math.round(upperMass));
        return Component.translatable("message.spacereloaded.stage.separated",
                active + 1, rocket.stageCount());
    }

    private static double dryMass(List<RocketData.Entry> entries) {
        double mass = 0;
        for (RocketData.Entry entry : entries) {
            mass += entry.massKg();
        }
        return mass;
    }

    private static Bounds bounds(List<RocketData.Entry> entries) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (RocketData.Entry entry : entries) {
            long local = entry.localPos();
            minX = Math.min(minX, PackedPos.unpackX(local));
            minY = Math.min(minY, PackedPos.unpackY(local));
            minZ = Math.min(minZ, PackedPos.unpackZ(local));
            maxX = Math.max(maxX, PackedPos.unpackX(local));
            maxZ = Math.max(maxZ, PackedPos.unpackZ(local));
        }
        return new Bounds(minX, minY, minZ, maxX - minX + 1, maxZ - minZ + 1);
    }

    /** Локальные координаты от нуля (как у {@code DockingSystem.spawnPart}). */
    private static List<RocketData.Entry> normalize(List<RocketData.Entry> entries, Bounds bounds) {
        List<RocketData.Entry> normalized = new ArrayList<>(entries.size());
        for (RocketData.Entry entry : entries) {
            long local = entry.localPos();
            normalized.add(new RocketData.Entry(entry.state(),
                    PackedPos.pack(PackedPos.unpackX(local) - bounds.minX(),
                            PackedPos.unpackY(local) - bounds.minY(),
                            PackedPos.unpackZ(local) - bounds.minZ()),
                    entry.massKg(), entry.role(), entry.thrustN(), entry.ispSec(),
                    entry.fuel(), entry.capacityKg(), entry.gyroTorqueNm()));
        }
        return normalized;
    }
}
