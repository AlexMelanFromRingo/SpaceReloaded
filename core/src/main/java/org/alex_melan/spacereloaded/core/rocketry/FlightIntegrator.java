package org.alex_melan.spacereloaded.core.rocketry;

import org.alex_melan.spacereloaded.core.geometry.Vec3d;

/**
 * Серверная динамика полёта (FR-023, принцип I).
 *
 * <p>Модель:
 * <ul>
 *   <li>полу-неявный Эйлер (скорость, затем позиция) — стабилен на игровых dt;</li>
 *   <li>реактивное движение по Мещерскому в форме постоянной тяги:
 *       F = ṁ·v_e, масса убывает как ṁ = throttle·Σ F_i/(Isp_i·g₀);</li>
 *   <li>момент от несовпадения центра тяги с ЦМ: для силы (0,f,0) в точке r
 *       относительно ЦМ τ = r×F = (−r_z·f, 0, r_x·f) — чистые тангаж и крен,
 *       рысканья нет (поэтому v1 обходится углами pitch/roll);</li>
 *   <li>гиродины — feedforward-компенсация известного момента двигателей
 *       плюс PD-регулятор к (pitchCmd, rollCmd), момент ограничен ресурсом
 *       гиродинов; коэффициенты PD нормированы на инерцию
 *       (ω_n = 2 рад/с, ζ = 1 — критическое демпфирование);</li>
 *   <li>упрощения v1 (задокументированы): углы малы — момент считается в
 *       локальной решётке без полного поворота тензора инерции; топливо
 *       дренируется из всех баков пропорционально; сопротивление воздуха
 *       не моделируется (добавится с атмосферами планет).</li>
 * </ul>
 */
public final class FlightIntegrator {

    private static final double GYRO_KP = 4.0;  // ω_n² при ω_n = 2 рад/с
    private static final double GYRO_KD = 4.0;  // 2ζω_n при ζ = 1

    private FlightIntegrator() {
    }

    public static FlightState step(RocketStructure structure, FlightState state,
                                   ControlInput input, FlightEnvironment env, double dt) {
        if (dt <= 0) {
            throw new IllegalArgumentException("dt must be > 0");
        }

        // Текущее распределение масс: топливо из state, пропорционально по бакам
        RocketStructure current = structure.withTotalPropellant(state.propellantKg());
        MassProperties mp = massProperties(current);

        // Тяга и расход
        boolean hasPropellant = state.propellantKg() > 0;
        double throttle = hasPropellant ? input.throttle() : 0;
        double thrust = throttle * mp.totalThrustN;
        double massFlow = throttle * mp.fullMassFlowKgS;
        double newPropellant = Math.max(0, state.propellantKg() - massFlow * dt);

        // Направление тяги: локальный +Y, повёрнутый креном (Z), затем тангажом (X)
        Vec3d thrustDir = rotate(new Vec3d(0, 1, 0), state.pitch(), state.roll());

        Vec3d accel = Vec3d.ZERO;
        if (mp.totalMassKg > 0) {
            accel = thrustDir.scale(thrust / mp.totalMassKg);
        }
        accel = accel.add(new Vec3d(0, -env.gravity(), 0));

        Vec3d vel = state.vel().add(accel.scale(dt));
        Vec3d pos = state.pos().add(vel.scale(dt));

        // Момент от двигателей относительно ЦМ (локальная решётка, малые углы)
        double engineTorqueX = 0;
        double engineTorqueZ = 0;
        for (PlacedPart part : current.parts()) {
            if (part.properties().role() == PartRole.ENGINE) {
                double f = throttle * part.properties().thrustN();
                Vec3d r = part.center().subtract(mp.centerOfMass);
                engineTorqueX += -r.z() * f;
                engineTorqueZ += r.x() * f;
            }
        }

        double torqueX = engineTorqueX;
        double torqueZ = engineTorqueZ;

        // Гиродины: feedforward известного момента двигателей (чистый PD оставил бы
        // статическую ошибку θ_ss = τ/(kp·I) при постоянном возмущении) + PD к заданной
        // ориентации. Момент ограничен ресурсом гиродинов по каждой оси (упрощение v1).
        if (input.stabilize() && mp.gyroTorqueNm > 0) {
            double cmdX = -engineTorqueX
                    + (-GYRO_KP * (state.pitch() - input.pitchCmd()) - GYRO_KD * state.pitchRate()) * mp.inertia.x();
            double cmdZ = -engineTorqueZ
                    + (-GYRO_KP * (state.roll() - input.rollCmd()) - GYRO_KD * state.rollRate()) * mp.inertia.z();
            torqueX += Math.clamp(cmdX, -mp.gyroTorqueNm, mp.gyroTorqueNm);
            torqueZ += Math.clamp(cmdZ, -mp.gyroTorqueNm, mp.gyroTorqueNm);
        }

        double pitchRate = state.pitchRate() + (mp.inertia.x() > 1e-9 ? torqueX / mp.inertia.x() : 0) * dt;
        double rollRate = state.rollRate() + (mp.inertia.z() > 1e-9 ? torqueZ / mp.inertia.z() : 0) * dt;
        double pitch = state.pitch() + pitchRate * dt;
        double roll = state.roll() + rollRate * dt;

        return new FlightState(pos, vel, pitch, roll, pitchRate, rollRate, newPropellant);
    }

    /** Поворот вектора: сначала крен вокруг Z, затем тангаж вокруг X (порядок фиксирован). */
    static Vec3d rotate(Vec3d v, double pitch, double roll) {
        // Rz(roll)
        double x1 = v.x() * Math.cos(roll) - v.y() * Math.sin(roll);
        double y1 = v.x() * Math.sin(roll) + v.y() * Math.cos(roll);
        double z1 = v.z();
        // Rx(pitch)
        double y2 = y1 * Math.cos(pitch) - z1 * Math.sin(pitch);
        double z2 = y1 * Math.sin(pitch) + z1 * Math.cos(pitch);
        return new Vec3d(x1, y2, z2);
    }

    private record MassProperties(double totalMassKg, Vec3d centerOfMass, Vec3d inertia,
                                  double totalThrustN, double fullMassFlowKgS, double gyroTorqueNm) {
    }

    private static MassProperties massProperties(RocketStructure structure) {
        double mass = 0;
        Vec3d moment = Vec3d.ZERO;
        double thrust = 0;
        double massFlow = 0;
        double gyro = 0;
        for (PlacedPart part : structure.parts()) {
            double m = part.properties().massKg() + part.propellantKg();
            mass += m;
            moment = moment.add(part.center().scale(m));
            if (part.properties().role() == PartRole.ENGINE) {
                thrust += part.properties().thrustN();
                massFlow += part.properties().thrustN()
                        / (part.properties().ispSec() * PerformanceCalculator.G0);
            }
            gyro += part.properties().gyroTorqueNm();
        }
        Vec3d com = mass > 0 ? moment.scale(1.0 / mass) : Vec3d.ZERO;
        double ix = 0;
        double iz = 0;
        for (PlacedPart part : structure.parts()) {
            double m = part.properties().massKg() + part.propellantKg();
            Vec3d d = part.center().subtract(com);
            ix += m * (d.y() * d.y() + d.z() * d.z());
            iz += m * (d.x() * d.x() + d.y() * d.y());
        }
        return new MassProperties(mass, com, new Vec3d(ix, 0, iz), thrust, massFlow, gyro);
    }
}
