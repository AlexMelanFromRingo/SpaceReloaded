package org.alex_melan.spacereloaded.core.industry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Катапульта (004, SC-001): кинематика против v² = 2aL, требуемая скорость против
 * патч-коник 003 (тот же v∞, что заложен в таблицу 822 м/с), энергия ½mv²/η,
 * атмосферный запрет на Земле и Марсе.
 */
class MassDriverBallisticsTest {

    private static final double G = MassDriverBallistics.G0;
    // Профиль Луны мода: g 1.62, R 1737.4 км, парковка 100 км, Луна → орбита Земли 822 м/с
    private static final double MOON_G = 1.62;
    private static final double MOON_R = 1_737_400;
    private static final double MOON_PARK = 100_000;
    private static final double MOON_TO_ORBIT = 822;

    @Test
    void singleSectionMatchesKinematics() {
        // a = 1000 g, L = 144 м → v = √(2·9806.65·144) ≈ 1680.6 м/с (скорость низкой лунной орбиты)
        double[] accel = new double[144];
        java.util.Arrays.fill(accel, 1000 * G);
        assertEquals(Math.sqrt(2 * 1000 * G * 144), MassDriverBallistics.muzzleVelocity(accel, 1.0), 1e-9);
    }

    @Test
    void mixedTiersSumSquares() {
        // v² = 2·(a₁L₁ + a₂L₂): 10 секций 1000 g + 5 секций 3000 g
        double[] accel = new double[15];
        for (int i = 0; i < 15; i++) {
            accel[i] = (i < 10 ? 1000 : 3000) * G;
        }
        double expected = Math.sqrt(2 * (10 * 1000 * G + 5 * 3000 * G));
        assertEquals(expected, MassDriverBallistics.muzzleVelocity(accel, 1.0), 1e-9);
    }

    @Test
    void excessVelocityInvertsInjection() {
        double mu = MOON_G * MOON_R * MOON_R;
        double rPark = MOON_R + MOON_PARK;
        double vInf = MassDriverBallistics.excessVelocity(mu, rPark, MOON_TO_ORBIT);
        double back = Math.sqrt(vInf * vInf + 2 * mu / rPark) - Math.sqrt(mu / rPark);
        assertEquals(MOON_TO_ORBIT, back, 1e-6);
        assertEquals(832, vInf, 832 * 0.01);
    }

    @Test
    void moonRequiredVelocityIs2517() {
        double v = MassDriverBallistics.requiredVelocity(MOON_G, MOON_R, MOON_PARK, MOON_TO_ORBIT);
        assertEquals(2517, v, 2517 * 0.01);
        // Выше второй космической Луны (2376 м/с): капсула не падает обратно
        assertTrue(v > Math.sqrt(2 * MOON_G * MOON_R));
    }

    @Test
    void zeroDeltaVGivesEscapeSpeed() {
        double v = MassDriverBallistics.requiredVelocity(MOON_G, MOON_R, MOON_PARK, 0);
        // Δv 0 меньше отлётного импульса → v∞ = 0 → v_req ≤ v_esc
        assertTrue(v <= Math.sqrt(2 * MOON_G * MOON_R) + 1e-9);
    }

    @Test
    void railLengthForMoon() {
        double v = MassDriverBallistics.requiredVelocity(MOON_G, MOON_R, MOON_PARK, MOON_TO_ORBIT);
        // L = v²/2a: тир 2 (3000 g) ≤ 128 блоков, тир 1 (1000 g) ≤ 400 (SC-002)
        assertTrue(v * v / (2 * 3000 * G) <= 128);
        assertTrue(v * v / (2 * 1000 * G) <= 400);
        assertEquals(108, MassDriverBallistics.missingSections(v, 0, 3000 * G, 1.0), 1);
    }

    @Test
    void shotEnergy() {
        // 250 кг при 2517 м/с, η 0.85: ½·250·2517²/0.85 ≈ 0.9317 ГДж
        double e = MassDriverBallistics.shotEnergyJ(250, 2517, 0.85);
        assertEquals(0.5 * 250 * 2517 * 2517 / 0.85, e, 1e-3);
        assertEquals(9.317e8, e, 9.317e8 * 0.01);
    }

    @Test
    void earthAtmosphereForbids() {
        // Земля, ρ₀ = 1.225: q = ½·1.225·v² при v ≈ 11 км/с для любого выхода; даже при
        // лунных 2517 м/с напор ≈ 3.9 МПа > предела капсулы 1 МПа
        assertTrue(MassDriverBallistics.dynamicPressure(1.225, 2517) > 1.0e6);
    }

    @Test
    void marsHeatForbids() {
        // Марс: g 3.72, R 3389.5 км, парковка 200 км, Δv 2103, ρ₀ 0.02
        double v = MassDriverBallistics.requiredVelocity(3.72, 3_389_500, 200_000, 2103);
        assertTrue(v > 5000, "Марс: v_req > 5 км/с, было " + v);
        double q = MassDriverBallistics.dynamicPressure(0.02, v);
        assertTrue(q < 1.0e6, "напор Марса ниже предела — запрещает нагрев");
        assertTrue(MassDriverBallistics.heatFlux(0.02, v, 0.5) > 5.0e6);
    }

    @Test
    void vacuumHasNoHeat() {
        assertEquals(0.0, MassDriverBallistics.heatFlux(0.0, 2517, 0.5));
        assertEquals(0.0, MassDriverBallistics.dynamicPressure(0.0, 2517));
    }
}
