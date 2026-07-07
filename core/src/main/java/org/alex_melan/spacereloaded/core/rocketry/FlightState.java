package org.alex_melan.spacereloaded.core.rocketry;

import org.alex_melan.spacereloaded.core.geometry.Vec3d;

/**
 * Состояние полёта. Ориентация v1 — тангаж (вокруг X) и крен (вокруг Z),
 * рысканье исключено: вертикальная тяга не создаёт момент вокруг Y
 * (см. FlightIntegrator). Углы в радианах, скорости в м/с.
 *
 * @param pos          позиция (мировые координаты, блоки=метры)
 * @param vel          скорость
 * @param pitch        тангаж, рад (вращение вокруг X)
 * @param roll         крен, рад (вращение вокруг Z)
 * @param pitchRate    угловая скорость тангажа, рад/с
 * @param rollRate     угловая скорость крена, рад/с
 * @param propellantKg остаток топлива (суммарный)
 */
public record FlightState(
        Vec3d pos,
        Vec3d vel,
        double pitch,
        double roll,
        double pitchRate,
        double rollRate,
        double propellantKg
) {
    public static FlightState atRest(Vec3d pos, double propellantKg) {
        return new FlightState(pos, Vec3d.ZERO, 0, 0, 0, 0, propellantKg);
    }
}
