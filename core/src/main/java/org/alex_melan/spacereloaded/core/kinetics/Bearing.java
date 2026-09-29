package org.alex_melan.spacereloaded.core.kinetics;

/**
 * Потери ротора (011): подшипник качения и аэродинамика диска.
 * <ul>
 *   <li>Подшипник: M = μ·F·d/2, F = m·g (вес ротора), μ ≈ 0.0015 — шариковый/роликовый подшипник
 *       (SKF, «Rolling bearings», коэффициент трения 0.001–0.002); плюс уплотнения и смазка — нижний
 *       предел момента. На Луне и на орбите вес меньше — ротор выбегает дольше.</li>
 *   <li>Трение покоя: пусковой момент подшипника качения в 1.5–2 раза выше рабочего
 *       ({@link #BREAKAWAY}).</li>
 *   <li>Аэродинамика вращающегося диска: M = ½·C_m·ρ·ω²·r⁵ (обе стороны, турбулентный режим
 *       C_m ≈ 0.01 с учётом зубьев); в вакууме ρ = 0 — маховик в вакуумном кожухе, как у настоящих
 *       накопителей.</li>
 * </ul>
 */
public final class Bearing {

    public static final double MU_ROLLING = 0.0015;
    public static final double BREAKAWAY = 1.5;
    public static final double DISK_MOMENT_COEFFICIENT = 0.01;

    private Bearing() {
    }

    /** Масса сплошного диска по моменту инерции: m = 2I/r². */
    public static double diskMass(double inertia, double radiusM) {
        return radiusM <= 0 ? 0 : 2 * inertia / (radiusM * radiusM);
    }

    /** Момент трения подшипника, Н·м: μ·m·g·r_вала, не меньше предела уплотнений. */
    public static double frictionTorque(double massKg, double gravity, double shaftRadiusM, double floorNm) {
        return Math.max(floorNm, MU_ROLLING * massKg * gravity * shaftRadiusM);
    }

    /** Аэродинамический момент диска при ω, Н·м. */
    public static double windageTorque(double density, double omega, double radiusM) {
        return 0.5 * DISK_MOMENT_COEFFICIENT * density * omega * omega * Math.pow(radiusM, 5);
    }

    /** Линеаризованный коэффициент вязкого сопротивления b = M/ω = ½·C_m·ρ·|ω|·r⁵ (для решателя). */
    public static double windageCoefficient(double density, double omega, double radiusM) {
        return 0.5 * DISK_MOMENT_COEFFICIENT * density * Math.abs(omega) * Math.pow(radiusM, 5);
    }
}
