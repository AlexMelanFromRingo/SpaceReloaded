package org.alex_melan.spacereloaded.rocket;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.core.geometry.PackedPos;
import org.alex_melan.spacereloaded.registry.ModEntities;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Стыковка ступеней (US6, T071/T074): плоскость разделения — блок
 * «стыковочный узел» (role=clamp). Расстыковка делит припаркованную ракету
 * на носитель (узел и всё выше) и лендер (всё ниже); топливо делится
 * пропорционально ёмкостям баков. Стыковка — обратный захват в радиусе:
 * лендер под узлом доводится в столбец узла и вливается в структуру,
 * топливо суммируется. Никакой магии инвентаря — только сущности в мире.
 */
public final class DockingSystem {

    /** Радиус захвата по горизонтали, блоки (T074: «упрощённая стыковка»). */
    private static final double CAPTURE_RADIUS = 3.0;
    /** Насколько глубоко под узлом ищем лендер, блоки. */
    private static final double CAPTURE_DEPTH = 6.0;

    private DockingSystem() {
    }

    /** Локальный Y нижнего стыковочного узла структуры (если есть). */
    public static OptionalInt clampLocalY(RocketData data) {
        return data.blocks().stream()
                .filter(entry -> entry.role().equals("clamp"))
                .mapToInt(entry -> PackedPos.unpackY(entry.localPos()))
                .min();
    }

    /**
     * Расстыковка: всё ниже узла — лендер, узел и выше — носитель.
     * Обе части остаются припаркованными сущностями (носитель висит
     * на своей высоте — на орбите это и есть парковка).
     */
    public static Component undock(ServerLevel level, RocketEntity rocket, int clampY) {
        RocketData data = rocket.rocketDataForDocking();
        List<RocketData.Entry> lower = new ArrayList<>();
        List<RocketData.Entry> upper = new ArrayList<>();
        for (RocketData.Entry entry : data.blocks()) {
            (PackedPos.unpackY(entry.localPos()) < clampY ? lower : upper).add(entry);
        }
        if (lower.isEmpty()) {
            return Component.translatable("message.spacereloaded.dock.nothing_below");
        }
        boolean landerFlyable = lower.stream().anyMatch(e -> e.role().equals("engine"))
                && lower.stream().anyMatch(e -> e.role().equals("command") || e.role().equals("seat"));
        if (!landerFlyable) {
            return Component.translatable("message.spacereloaded.dock.lander_invalid");
        }

        // Топливо — пропорционально ёмкости каждой части
        double totalCapacity = data.blocks().stream().mapToDouble(RocketData.Entry::capacityKg).sum();
        double fraction = totalCapacity <= 0 ? 0
                : Math.clamp(data.propellantKg() / totalCapacity, 0, 1);
        double lowerFuel = lower.stream().mapToDouble(RocketData.Entry::capacityKg).sum() * fraction;
        double upperFuel = upper.stream().mapToDouble(RocketData.Entry::capacityKg).sum() * fraction;

        rocket.ejectPassengers();
        Vec3 base = new Vec3(rocket.getX() - rocket.halfX(), rocket.getY(),
                rocket.getZ() - rocket.halfZ());
        spawnPart(level, lower, lowerFuel, base);
        spawnPart(level, upper, upperFuel, base);
        rocket.discard();
        return Component.translatable("message.spacereloaded.dock.undocked",
                lower.size(), upper.size());
    }

    /**
     * Стыковка: под узлом носителя ищется припаркованный лендер (захват
     * в радиусе), его блоки вливаются в структуру носителя строго в столбец
     * узла, топливо суммируется. Типы топлива должны совпадать.
     */
    public static Component dock(ServerLevel level, RocketEntity carrier, int clampY) {
        RocketData carrierData = carrier.rocketDataForDocking();
        double clampWorldY = carrier.getY() + clampY;
        Vec3 clampColumn = clampColumnCenter(carrier, carrierData, clampY);

        RocketEntity lander = null;
        for (RocketEntity candidate : level.getEntities(
                EntityTypeTest.forClass(RocketEntity.class),
                new AABB(clampColumn.x - CAPTURE_RADIUS, clampWorldY - CAPTURE_DEPTH,
                        clampColumn.z - CAPTURE_RADIUS,
                        clampColumn.x + CAPTURE_RADIUS, clampWorldY,
                        clampColumn.z + CAPTURE_RADIUS),
                entity -> entity != carrier && entity.isParked())) {
            lander = candidate;
            break;
        }
        if (lander == null) {
            return Component.translatable("message.spacereloaded.dock.no_lander");
        }
        String carrierFuel = carrier.rocketFuelType();
        String landerFuel = lander.rocketFuelType();
        if (!carrierFuel.isEmpty() && !landerFuel.isEmpty() && !carrierFuel.equals(landerFuel)) {
            return Component.translatable("message.spacereloaded.dock.fuel_mismatch",
                    landerFuel, carrierFuel);
        }

        RocketData landerData = lander.rocketDataForDocking();
        int landerHeight = landerData.blocks().stream()
                .mapToInt(e -> PackedPos.unpackY(e.localPos())).max().orElse(0) + 1;
        // Доводка захвата: лендер выравнивается в столбец узла, верхний ряд — под узел
        int clampLx = clampLocalX(carrierData, clampY);
        int clampLz = clampLocalZ(carrierData, clampY);
        int landerCenterLx = (int) Math.round(lander.halfX() - 0.5);
        int landerCenterLz = (int) Math.round(lander.halfZ() - 0.5);

        List<RocketData.Entry> merged = new ArrayList<>(carrierData.blocks());
        for (RocketData.Entry entry : landerData.blocks()) {
            long local = entry.localPos();
            merged.add(new RocketData.Entry(entry.state(),
                    PackedPos.pack(
                            PackedPos.unpackX(local) - landerCenterLx + clampLx,
                            PackedPos.unpackY(local) + clampY - landerHeight,
                            PackedPos.unpackZ(local) - landerCenterLz + clampLz),
                    entry.massKg(), entry.role(), entry.thrustN(), entry.ispSec(),
                    entry.fuel(), entry.capacityKg(), entry.gyroTorqueNm()));
        }
        double fuel = carrierData.propellantKg() + landerData.propellantKg();

        carrier.ejectPassengers();
        lander.ejectPassengers();
        Vec3 base = new Vec3(carrier.getX() - carrier.halfX(), carrier.getY(),
                carrier.getZ() - carrier.halfZ());
        lander.discard();
        carrier.discard();
        spawnPart(level, merged, fuel, base);
        return Component.translatable("message.spacereloaded.dock.docked",
                landerData.blocks().size(), Math.round(fuel));
    }

    /** Спавн части структуры с нормализацией локальных координат в ноль. */
    private static void spawnPart(ServerLevel level, List<RocketData.Entry> entries,
                                  double propellantKg, Vec3 oldBase) {
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
        List<RocketData.Entry> normalized = new ArrayList<>(entries.size());
        for (RocketData.Entry entry : entries) {
            long local = entry.localPos();
            normalized.add(new RocketData.Entry(entry.state(),
                    PackedPos.pack(PackedPos.unpackX(local) - minX,
                            PackedPos.unpackY(local) - minY,
                            PackedPos.unpackZ(local) - minZ),
                    entry.massKg(), entry.role(), entry.thrustN(), entry.ispSec(),
                    entry.fuel(), entry.capacityKg(), entry.gyroTorqueNm()));
        }
        double sizeX = maxX - minX + 1;
        double sizeZ = maxZ - minZ + 1;
        RocketEntity part = new RocketEntity(ModEntities.ROCKET, level);
        part.setPos(oldBase.x + minX + sizeX / 2.0, oldBase.y + minY, oldBase.z + minZ + sizeZ / 2.0);
        part.setAssembly(new RocketData(normalized, propellantKg));
        level.addFreshEntity(part);
    }

    private static Vec3 clampColumnCenter(RocketEntity rocket, RocketData data, int clampY) {
        return new Vec3(rocket.getX() - rocket.halfX() + clampLocalX(data, clampY) + 0.5,
                rocket.getY() + clampY,
                rocket.getZ() - rocket.halfZ() + clampLocalZ(data, clampY) + 0.5);
    }

    private static int clampLocalX(RocketData data, int clampY) {
        return data.blocks().stream()
                .filter(e -> e.role().equals("clamp") && PackedPos.unpackY(e.localPos()) == clampY)
                .mapToInt(e -> PackedPos.unpackX(e.localPos())).findFirst().orElse(0);
    }

    private static int clampLocalZ(RocketData data, int clampY) {
        return data.blocks().stream()
                .filter(e -> e.role().equals("clamp") && PackedPos.unpackY(e.localPos()) == clampY)
                .mapToInt(e -> PackedPos.unpackZ(e.localPos())).findFirst().orElse(0);
    }
}
