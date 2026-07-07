package org.alex_melan.spacereloaded.core.rocketry;

import org.alex_melan.spacereloaded.core.geometry.PackedPos;
import org.alex_melan.spacereloaded.core.geometry.Vec3d;

/**
 * Деталь на позиции в локальной сетке ракеты.
 *
 * @param packedPos    локальная позиция ({@code PackedPos})
 * @param properties   физические свойства
 * @param propellantKg текущая заправка бака (0 для не-баков)
 */
public record PlacedPart(long packedPos, PartProperties properties, double propellantKg) {
    public PlacedPart {
        if (propellantKg < 0) {
            throw new IllegalArgumentException("propellantKg < 0");
        }
        if (propellantKg > properties.propellantCapacityKg()) {
            throw new IllegalArgumentException("propellant exceeds tank capacity");
        }
    }

    public static PlacedPart of(int x, int y, int z, PartProperties properties) {
        return new PlacedPart(PackedPos.pack(x, y, z), properties, 0);
    }

    public static PlacedPart filledTank(int x, int y, int z, PartProperties properties) {
        return new PlacedPart(PackedPos.pack(x, y, z), properties, properties.propellantCapacityKg());
    }

    /** Геометрический центр блока — точка приложения массы и силы. */
    public Vec3d center() {
        return new Vec3d(
                PackedPos.unpackX(packedPos) + 0.5,
                PackedPos.unpackY(packedPos) + 0.5,
                PackedPos.unpackZ(packedPos) + 0.5
        );
    }
}
