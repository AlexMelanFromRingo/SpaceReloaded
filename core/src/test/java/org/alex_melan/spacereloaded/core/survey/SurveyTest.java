package org.alex_melan.spacereloaded.core.survey;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Разведка 009: GSD в ближнем ИК, разделение смеси, время пробега и затухание георадара. */
class SurveyTest {

    /** 2 мкм, D = 10 см: 2.44 м со 100 км (Луна), 4.88 м с 200 км (Земля, Марс). */
    @Test
    void swirResolution() {
        assertEquals(2.44, SpectralMapping.gsd(100e3, 0.1), 0.01);
        assertEquals(4.88, SpectralMapping.gsd(200e3, 0.1), 0.01);
        assertEquals(2, SpectralMapping.minScale(100e3, 0.1));   // 4 м/пикс
        assertEquals(3, SpectralMapping.minScale(200e3, 0.1));   // 8 м/пикс
    }

    @Test
    void mixtureThreshold() {
        assertEquals("ice", SpectralMapping.classify(Map.of("ice", 30), 100, 0.25));
        assertNull(SpectralMapping.classify(Map.of("ice", 20), 100, 0.25));
        assertEquals("kerogen", SpectralMapping.classify(Map.of("ice", 26, "kerogen", 40), 100, 0.25));
    }

    /** Пустота на 8 м под реголитом (ε 3): эхо на 92 нс, яркое; сигнал сквозь влажную почву гаснет. */
    @Test
    void radarVoidUnderRegolith() {
        assertEquals(92.4, GroundRadar.twoWayNs(8, 3), 0.2);
        var column = List.of(new GroundRadar.Layer(8, 3, 0.005), new GroundRadar.Layer(4, 1, 0),
                new GroundRadar.Layer(10, 3, 0.005));
        double[] tr = GroundRadar.trace(column, 500e6, 60);
        int k = (int) Math.round(92.4 / GroundRadar.SAMPLE_NS);
        assertTrue(tr[k] > 0.2, "эхо от потолка трубки " + tr[k]);
        var wet = List.of(new GroundRadar.Layer(3, 15, 0.1), new GroundRadar.Layer(4, 1, 0));
        double[] w = GroundRadar.trace(wet, 500e6, 60);
        for (double v : w) {
            assertEquals(0, v, 1e-12);
        }
        assertEquals(0.394, GroundRadar.attenuationDbPerM(500e6, 3, 0.005), 0.005);
        assertEquals(8.0, GroundRadar.depthAt(k, 3), 0.2);
    }

    /** Предел глубины для пустоты в сухом реголите — около 20 м при диапазоне 60 дБ. */
    @Test
    void radarDepthLimit() {
        int deepest = 0;
        for (int d = 1; d < 40; d++) {
            var col = List.of(new GroundRadar.Layer(d, 3, 0.005), new GroundRadar.Layer(4, 1, 0));
            double[] tr = GroundRadar.trace(col, 500e6, 60);
            for (double v : tr) {
                if (v > 0) {
                    deepest = d;
                }
            }
        }
        assertTrue(deepest >= 18 && deepest <= 24, "предел " + deepest + " м");
    }
}
