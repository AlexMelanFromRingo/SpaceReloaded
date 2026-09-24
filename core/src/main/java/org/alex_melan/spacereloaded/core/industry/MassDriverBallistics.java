package org.alex_melan.spacereloaded.core.industry;

/**
 * Физика электромагнитной катапульты (004, FR-204…FR-208, D30/D31/D34): кинематика
 * рельса, требуемая скорость выхода на траекторию к ловушке, энергия выстрела и
 * атмосферный запрет. Все величины — СИ (1 блок = 1 м).
 *
 * <p>Упрощения (задокументированы здесь и в research.md): импульс мгновенный (разгон
 * длится доли секунды); вращение тела не учитывается; аэрозахват у Земли бесплатен (как
 * в таблице перелётов 003); атмосферный критерий — напор/нагрев на срезе, а не интеграл
 * по столбу (укороченные шкалы высот мода занижают столб ~75×, интеграл дал бы ложное «можно»).
 */
public final class MassDriverBallistics {

    /** Стандартное ускорение свободного падения, м/с² (перевод «g» тиров катушек). */
    public static final double G0 = 9.80665;

    /** Коэффициент Саттона–Грейвса для CO₂/воздуха, кг^½/м (порядок 1.7·10⁻⁴). */
    public static final double SUTTON_GRAVES_K = 1.7415e-4;

    private MassDriverBallistics() {
    }

    /**
     * Дульная скорость рельса из секций с кусочно-постоянным ускорением: на каждой секции
     * v² растёт на 2·aᵢ·Lᵢ (v² = v₀² + 2aL), поэтому v = √(2·Σ aᵢ·Lᵢ) — точное решение,
     * в котором слабая секция просто даёт меньший прирост.
     *
     * @param accel        предельные ускорения секций, м/с²
     * @param sectionLengthM длина одной секции, м
     */
    public static double muzzleVelocity(double[] accel, double sectionLengthM) {
        double sum = 0;
        for (double a : accel) {
            sum += a * sectionLengthM;
        }
        return Math.sqrt(2 * sum);
    }

    /**
     * Избыток скорости v∞, соответствующий табличному Δv перелёта из круговой парковочной
     * орбиты r_p (обращение инъекции патч-коник 003: Δv = √(v∞² + 2μ/r_p) − √(μ/r_p)):
     * v∞ = √((Δv + √(μ/r_p))² − 2μ/r_p). Δv меньше отлётного импульса (v∞² < 0) → 0.
     */
    public static double excessVelocity(double mu, double rPark, double tableDeltaV) {
        double vCirc = Math.sqrt(mu / rPark);
        double vPeri = tableDeltaV + vCirc;
        double sq = vPeri * vPeri - 2 * mu / rPark;
        return sq <= 0 ? 0.0 : Math.sqrt(sq);
    }

    /**
     * Требуемая дульная скорость для прямого выхода с поверхности на ту же гиперболу, что
     * заложена в таблицу перелёта: по сохранению энергии v_req = √(v∞² + 2μ/R), μ = g·R²
     * (профиль задаёт поверхностную g). Луна (g 1.62, R 1737.4 км, парковка 100 км, Δv 822):
     * v∞ ≈ 832, v_req ≈ 2517 м/с.
     */
    public static double requiredVelocity(double surfaceGravity, double bodyRadius, double parkingAltitude,
                                          double tableDeltaV) {
        double mu = surfaceGravity * bodyRadius * bodyRadius;
        double vInf = excessVelocity(mu, bodyRadius + parkingAltitude, tableDeltaV);
        return Math.sqrt(vInf * vInf + 2 * mu / bodyRadius);
    }

    /** Энергия выстрела из сети, Дж: кинетическая ½mv² делится на КПД линейного двигателя η. */
    public static double shotEnergyJ(double massKg, double velocity, double efficiency) {
        return 0.5 * massKg * velocity * velocity / efficiency;
    }

    /** Скоростной напор q = ½ρv², Па. */
    public static double dynamicPressure(double density, double velocity) {
        return 0.5 * density * velocity * velocity;
    }

    /**
     * Тепловой поток в критической точке по Саттону–Грейвсу q̇ = k·√(ρ/r_n)·v³, Вт/м²
     * (r_n — радиус носа капсулы, м).
     */
    public static double heatFlux(double density, double velocity, double noseRadius) {
        if (density <= 0) {
            return 0.0;
        }
        return SUTTON_GRAVES_K * Math.sqrt(density / noseRadius) * velocity * velocity * velocity;
    }

    /**
     * Сколько секций лучшего тира не хватает рельсу: n = ⌈(v_req² − v_max²)/(2·a·L_s)⌉.
     */
    public static int missingSections(double vRequired, double vMax, double bestAccel, double sectionLengthM) {
        if (vMax >= vRequired) {
            return 0;
        }
        return (int) Math.ceil((vRequired * vRequired - vMax * vMax) / (2 * bestAccel * sectionLengthM));
    }
}
