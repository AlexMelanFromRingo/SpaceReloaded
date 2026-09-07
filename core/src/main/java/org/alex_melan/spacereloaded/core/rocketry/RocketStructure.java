package org.alex_melan.spacereloaded.core.rocketry;

import org.alex_melan.spacereloaded.core.geometry.LongHashSet;

import java.util.List;

/**
 * Снимок структуры ракеты: набор деталей в локальных координатах.
 * Создаётся сборщиком (BFS по тегу в fabric-слое) и неизменяем.
 */
public record RocketStructure(List<PlacedPart> parts) {
    public RocketStructure {
        parts = List.copyOf(parts);
        LongHashSet seen = new LongHashSet(parts.size());
        for (PlacedPart part : parts) {
            if (!seen.add(part.packedPos())) {
                throw new IllegalArgumentException("duplicate part position");
            }
        }
    }

    public double totalPropellantKg() {
        double total = 0;
        for (PlacedPart part : parts) {
            total += part.propellantKg();
        }
        return total;
    }

    public double totalPropellantCapacityKg() {
        double total = 0;
        for (PlacedPart part : parts) {
            total += part.properties().propellantCapacityKg();
        }
        return total;
    }

    /**
     * Аэродинамическое тело структуры (Полёт 2.0, FR-081): площади проекций коробки
     * габаритов (блоки = метры) с заданным C_d. Пустая структура — без сопротивления.
     */
    public org.alex_melan.spacereloaded.core.atmosphere.DragBody dragBody(double cd) {
        if (parts.isEmpty() || cd <= 0) {
            return org.alex_melan.spacereloaded.core.atmosphere.DragBody.NONE;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (PlacedPart part : parts) {
            long pos = part.packedPos();
            minX = Math.min(minX, org.alex_melan.spacereloaded.core.geometry.PackedPos.unpackX(pos));
            minY = Math.min(minY, org.alex_melan.spacereloaded.core.geometry.PackedPos.unpackY(pos));
            minZ = Math.min(minZ, org.alex_melan.spacereloaded.core.geometry.PackedPos.unpackZ(pos));
            maxX = Math.max(maxX, org.alex_melan.spacereloaded.core.geometry.PackedPos.unpackX(pos));
            maxY = Math.max(maxY, org.alex_melan.spacereloaded.core.geometry.PackedPos.unpackY(pos));
            maxZ = Math.max(maxZ, org.alex_melan.spacereloaded.core.geometry.PackedPos.unpackZ(pos));
        }
        double sizeX = maxX - minX + 1;
        double sizeY = maxY - minY + 1;
        double sizeZ = maxZ - minZ + 1;
        return new org.alex_melan.spacereloaded.core.atmosphere.DragBody(cd,
                sizeY * sizeZ, sizeX * sizeZ, sizeX * sizeY);
    }

    /** Структура с другим суммарным запасом топлива, распределённым пропорционально ёмкостям. */
    public RocketStructure withTotalPropellant(double propellantKg) {
        double capacity = totalPropellantCapacityKg();
        double fraction = capacity <= 0 ? 0 : Math.clamp(propellantKg / capacity, 0, 1);
        List<PlacedPart> updated = parts.stream()
                .map(p -> new PlacedPart(p.packedPos(), p.properties(),
                        p.properties().propellantCapacityKg() * fraction))
                .toList();
        return new RocketStructure(updated);
    }
}
