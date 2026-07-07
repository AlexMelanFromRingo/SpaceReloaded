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
