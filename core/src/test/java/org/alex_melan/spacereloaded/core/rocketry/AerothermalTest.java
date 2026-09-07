package org.alex_melan.spacereloaded.core.rocketry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AerothermalTest {

    @Test
    void heatIndexIsSuttonGravesShape() {
        // √ρ·v³: ρ = 1.225, v = 100 → 1.1068·10⁶
        assertEquals(Math.sqrt(1.225) * 1.0e6, Aerothermal.heatIndex(1.225, 100), 1e-3);
        // Удвоение скорости — ×8, учетверение плотности — ×2
        assertEquals(8 * Aerothermal.heatIndex(1.0, 50), Aerothermal.heatIndex(1.0, 100), 1e-9);
        assertEquals(2 * Aerothermal.heatIndex(1.0, 50), Aerothermal.heatIndex(4.0, 50), 1e-9);
        assertTrue(Aerothermal.heatIndex(0.02, 200) < Aerothermal.heatIndex(1.225, 200), "Марс греет меньше Земли");
        assertEquals(0, Aerothermal.heatIndex(0, 300), 0, "в вакууме нагрева нет");
        assertEquals(0, Aerothermal.heatIndex(1.225, 0), 0);
    }

    /** Порог по умолчанию 6·10⁶ соответствует ~176 м/с у поверхности Земли. */
    @Test
    void defaultThresholdIsAboveOrdinaryAscentSpeeds() {
        double threshold = 6.0e6;
        double speedAtThreshold = Math.cbrt(threshold / Math.sqrt(1.225));
        assertEquals(176, speedAtThreshold, 1.0);
        assertTrue(Aerothermal.heatIndex(1.225, 120) < threshold, "штатный подъём не греет");
        assertTrue(Aerothermal.heatIndex(1.225, 250) > threshold, "отвесный вход греет");
    }

    @Test
    void dynamicPressureIsHalfRhoVSquared() {
        assertEquals(0.5 * 1.225 * 200 * 200, Aerothermal.dynamicPressure(1.225, 200), 1e-9);
        assertEquals(0, Aerothermal.dynamicPressure(0, 200), 0);
        // 35 кПа у поверхности Земли — около 239 м/с
        assertEquals(239, Math.sqrt(2 * 35_000 / 1.225), 1.0);
    }
}
