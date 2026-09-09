package org.alex_melan.spacereloaded.core.rocketry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Небесная механика перелётов (003, FR-101): Гоман между круговыми орбитами и
 * инъекция из парковочной орбиты по патч-коникам сверяются с эталонами, а
 * табличные Δv профилей мода (datapack) — с формулами по реальным параметрам орбит.
 */
class TransferOrbitsTest {

    // Реальные параметры (μ — гравитационные параметры, м³/с²; радиусы — м)
    private static final double MU_SUN = 1.32712440018e20;
    private static final double MU_EARTH = 3.986004418e14;
    private static final double MU_MARS = 4.282837e13;
    private static final double MU_MOON = 4.9048695e12;
    private static final double AU = 1.495978707e11;
    private static final double R_EARTH = 6.371e6;
    private static final double R_MARS = 3.3895e6;
    private static final double R_MOON = 1.7374e6;
    private static final double MOON_ORBIT = 3.844e8;
    private static final double MARS_ORBIT = 1.5237 * AU;
    private static final double BELT_ORBIT = 2.7 * AU;
    private static final double PARK_EARTH = R_EARTH + 200e3;
    private static final double PARK_MARS = R_MARS + 200e3;
    private static final double PARK_MOON = R_MOON + 100e3;

    // Значения таблицы transfer_delta_v профилей мода (м/с)
    private static final double TABLE_ORBIT_TO_MOON = 3955;
    private static final double TABLE_MOON_TO_ORBIT = 822;
    private static final double TABLE_ORBIT_TO_MARS = 3613;
    private static final double TABLE_MARS_TO_ORBIT = 2103;
    private static final double TABLE_ORBIT_TO_BELT = 9650;
    private static final double TABLE_BELT_TO_ORBIT = 4800;

    private static void assertWithin1Percent(double expected, double actual, String what) {
        assertTrue(Math.abs(actual - expected) <= 0.01 * expected,
                what + ": ожидалось " + expected + ", получено " + actual);
    }

    /** Классический Гоман LEO(300 км) → GEO: 2.43 + 1.47 = 3.89 км/с. */
    @Test
    void hohmannLeoToGeoMatchesTextbook() {
        TransferOrbits.Burns burns = TransferOrbits.hohmannBurns(MU_EARTH, R_EARTH + 300e3, 42_164e3);
        assertWithin1Percent(2426, burns.departureMs(), "импульс перигея");
        assertWithin1Percent(1467, burns.arrivalMs(), "импульс апогея");
        assertWithin1Percent(3893, TransferOrbits.hohmannTotal(MU_EARTH, R_EARTH + 300e3, 42_164e3), "сумма");
    }

    /** Инъекция при v∞ = 0 — уход на параболу: (√2 − 1)·v_circ. */
    @Test
    void injectionWithZeroExcessIsEscapeBurn() {
        double vCirc = TransferOrbits.circularSpeed(MU_EARTH, PARK_EARTH);
        assertEquals((Math.sqrt(2) - 1) * vCirc,
                TransferOrbits.injectionFromParking(MU_EARTH, PARK_EARTH, 0), 1e-6);
    }

    /** Гелиоцентрический Гоман Земля → Марс: 2.94 / 2.65 км/с. */
    @Test
    void earthToMarsHeliocentricBurns() {
        TransferOrbits.Burns burns = TransferOrbits.hohmannBurns(MU_SUN, AU, MARS_ORBIT);
        assertWithin1Percent(2945, burns.departureMs(), "v∞ отбытия");
        assertWithin1Percent(2649, burns.arrivalMs(), "v∞ прибытия");
    }

    /** Таблица Марса: TMI из LEO 200 км = 3613; TEI из LMO 200 км = 2103 (аэрозахват у Земли бесплатен). */
    @Test
    void marsTableMatchesPatchedConics() {
        double tmi = TransferOrbits.interplanetaryDeparture(MU_SUN, AU, MARS_ORBIT, MU_EARTH, PARK_EARTH);
        double tei = TransferOrbits.interplanetaryArrival(MU_SUN, AU, MARS_ORBIT, MU_MARS, PARK_MARS);
        assertWithin1Percent(TABLE_ORBIT_TO_MARS, tmi, "орбита → Марс");
        assertWithin1Percent(TABLE_MARS_TO_ORBIT, tei, "Марс → орбита (TEI = MOI по симметрии)");
    }

    /** Таблица Луны: TLI 3133 + LOI 822 = 3955; возврат — TEI 822. */
    @Test
    void moonTableMatchesLunarTransfer() {
        TransferOrbits.Burns lunar = TransferOrbits.lunarTransfer(MU_EARTH, PARK_EARTH, MOON_ORBIT, MU_MOON, PARK_MOON);
        assertWithin1Percent(3133, lunar.departureMs(), "TLI");
        assertWithin1Percent(822, lunar.arrivalMs(), "LOI");
        assertWithin1Percent(TABLE_ORBIT_TO_MOON, lunar.total(), "орбита → Луна");
        assertWithin1Percent(TABLE_MOON_TO_ORBIT, lunar.arrivalMs(), "Луна → орбита");
    }

    /** Таблица пояса (2.7 а.е.): инъекция 4850 + уравнивание 4800 = 9650; возврат 4800. */
    @Test
    void beltTableMatchesHohmann() {
        double injection = TransferOrbits.interplanetaryDeparture(MU_SUN, AU, BELT_ORBIT, MU_EARTH, PARK_EARTH);
        double matching = TransferOrbits.hohmannBurns(MU_SUN, AU, BELT_ORBIT).arrivalMs();
        assertWithin1Percent(4850, injection, "инъекция к поясу");
        assertWithin1Percent(4800, matching, "уравнивание с астероидом");
        assertWithin1Percent(TABLE_ORBIT_TO_BELT, injection + matching, "орбита → пояс");
        assertWithin1Percent(TABLE_BELT_TO_ORBIT, matching, "пояс → орбита (аэрозахват у Земли)");
    }

    /** Гоман симметричен по направлению: r1→r2 и r2→r1 дают те же импульсы местами. */
    @Test
    void hohmannIsSymmetric() {
        TransferOrbits.Burns out = TransferOrbits.hohmannBurns(MU_SUN, AU, MARS_ORBIT);
        TransferOrbits.Burns back = TransferOrbits.hohmannBurns(MU_SUN, MARS_ORBIT, AU);
        assertEquals(out.departureMs(), back.arrivalMs(), 1e-6);
        assertEquals(out.arrivalMs(), back.departureMs(), 1e-6);
    }
}
