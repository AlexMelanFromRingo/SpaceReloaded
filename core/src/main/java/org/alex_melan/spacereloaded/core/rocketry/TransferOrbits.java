package org.alex_melan.spacereloaded.core.rocketry;

/**
 * Небесная механика перелётов (003, FR-101, D20). Источник табличных Δv профилей
 * планет: значения датапака выведены этими формулами по реальным параметрам орбит
 * и сверяются тестом ядра, чтобы «магические числа» не оторвались от физики.
 *
 * <p>Модель — патч-коники: гелиоцентрический (или планетоцентрический для Луны)
 * переход Гомана между круговыми компланарными орбитами плюс инъекция/захват из
 * парковочной орбиты по гиперболическому избытку скорости v∞.
 *
 * <p>Упрощения (задокументированы здесь и в датапак-гайде): орбиты круговые и
 * компланарные; фазирование — механика окон перелёта; захват у тела с атмосферой
 * (Земля, Марс) считается аэродинамическим и бесплатным; импульсы мгновенные.
 */
public final class TransferOrbits {

    /** Два импульса перелёта Гомана (или инъекция + захват), м/с. */
    public record Burns(double departureMs, double arrivalMs) {
        public double total() {
            return departureMs + arrivalMs;
        }
    }

    private TransferOrbits() {
    }

    /** Скорость круговой орбиты v = √(μ/r). */
    public static double circularSpeed(double mu, double r) {
        return Math.sqrt(mu / r);
    }

    /**
     * Импульсы Гомана между круговыми орбитами r1 → r2 (любое направление):
     * по vis-viva v² = μ(2/r − 1/a), a = (r1 + r2)/2:
     * Δv₁ = |v_transfer(r1) − v_circ(r1)|, Δv₂ = |v_circ(r2) − v_transfer(r2)|.
     */
    public static Burns hohmannBurns(double mu, double r1, double r2) {
        double a = (r1 + r2) / 2;
        double vTransfer1 = Math.sqrt(mu * (2 / r1 - 1 / a));
        double vTransfer2 = Math.sqrt(mu * (2 / r2 - 1 / a));
        return new Burns(Math.abs(vTransfer1 - circularSpeed(mu, r1)),
                Math.abs(circularSpeed(mu, r2) - vTransfer2));
    }

    public static double hohmannTotal(double mu, double r1, double r2) {
        return hohmannBurns(mu, r1, r2).total();
    }

    /**
     * Инъекция с круговой парковочной орбиты на гиперболу с избытком v∞ (или
     * захват с неё — та же величина по симметрии): по сохранению энергии скорость
     * в перицентре v_p = √(v∞² + 2μ/r), импульс Δv = v_p − √(μ/r).
     */
    public static double injectionFromParking(double muBody, double rPark, double vInfinity) {
        return Math.sqrt(vInfinity * vInfinity + 2 * muBody / rPark) - circularSpeed(muBody, rPark);
    }

    /**
     * Отбытие к телу на орбите r2 с парковочной орбиты тела на орбите r1:
     * v∞ = импульс отбытия гелиоцентрического Гомана.
     */
    public static double interplanetaryDeparture(double muSun, double r1, double r2,
                                                 double muDeparture, double rParkDeparture) {
        return injectionFromParking(muDeparture, rParkDeparture, hohmannBurns(muSun, r1, r2).departureMs());
    }

    /**
     * Захват в парковочную орбиту тела на орбите r2 при прилёте с r1:
     * v∞ = импульс прибытия гелиоцентрического Гомана. Для тел с атмосферой
     * мод считает захват аэродинамическим (0) — см. таблицу датапака.
     */
    public static double interplanetaryArrival(double muSun, double r1, double r2,
                                               double muArrival, double rParkArrival) {
        return injectionFromParking(muArrival, rParkArrival, hohmannBurns(muSun, r1, r2).arrivalMs());
    }

    /**
     * Перелёт к спутнику планеты (Земля → Луна): TLI — импульс перигея Гомана
     * от парковочной орбиты до орбиты спутника; в апогее относительная скорость
     * к спутнику равна импульсу апогея Гомана, она и есть v∞ для LOI в
     * парковочную орбиту спутника.
     */
    public static Burns lunarTransfer(double muPrimary, double rPark, double rMoonOrbit,
                                      double muMoon, double rParkMoon) {
        Burns hohmann = hohmannBurns(muPrimary, rPark, rMoonOrbit);
        double loi = injectionFromParking(muMoon, rParkMoon, hohmann.arrivalMs());
        return new Burns(hohmann.departureMs(), loi);
    }
}
