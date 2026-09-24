package org.alex_melan.spacereloaded.core.industry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Допуски, качество двигателя, стек и колонна (005). */
class EngineeringIndustryTest {

    @Test
    void rssToleranceAndQuality() {
        double sum = 0;
        for (int i = 0; i < 4; i++) {
            sum = MachiningTolerance.accumulate(sum, 10);
        }
        assertEquals(20.0, Math.sqrt(sum), 1e-9); // √(4·10²)
        assertEquals(0.8, MachiningTolerance.quality(sum, 100), 1e-9);
        assertEquals(5.0, Math.sqrt(MachiningTolerance.balance(100)), 1e-9); // δ 10 → 5
        assertTrue(MachiningTolerance.isScrap(101 * 101, 100));
        assertFalse(MachiningTolerance.isScrap(99 * 99, 100));
        // 5 % отклонения оборотов при k = 2 — +10 % погрешности
        assertEquals(11.0, MachiningTolerance.operationDelta(10, 0.05, 2), 1e-9);
        assertEquals(0.05, MachiningTolerance.deviation(0.95 * 100, 100), 1e-9);
    }

    @Test
    void stableChainGivesGoodQuality() {
        // 7 операций: 4 токарных (10 мкм) и 3 пресса (20 мкм) при отклонении 2 %
        double sum = 0;
        for (int i = 0; i < 4; i++) {
            sum = MachiningTolerance.accumulate(sum, MachiningTolerance.operationDelta(10, 0.02, 2));
        }
        for (int i = 0; i < 3; i++) {
            sum = MachiningTolerance.accumulate(sum, MachiningTolerance.operationDelta(20, 0.02, 2));
        }
        assertTrue(MachiningTolerance.quality(sum, 100) >= 0.55, "q = " + MachiningTolerance.quality(sum, 100));
    }

    @Test
    void engineQualityMultipliers() {
        assertEquals(1.0, EngineQuality.ispMultiplier(0.8), 1e-12);
        assertEquals(1.0, EngineQuality.thrustMultiplier(0.8), 1e-12);
        assertEquals(1.0164, EngineQuality.ispMultiplier(1.0), 0.0005);
        assertEquals(1.0587, EngineQuality.thrustMultiplier(1.0), 0.001);
        assertEquals(0.9760, EngineQuality.ispMultiplier(0.5), 0.001);
        assertEquals(8, EngineQuality.level(0.8));
        assertEquals(0.5, EngineQuality.fromLevel(5));
    }

    @Test
    void stackAndColumn() {
        assertEquals(8, ElectrolysisStack.unitsPerCycle(7, 64));
        assertEquals(3, ElectrolysisStack.unitsPerCycle(7, 3));
        assertEquals(8 * 800, ElectrolysisStack.energyPerCycle(800, 8));
        assertEquals(100, ColumnYield.yield(4, 100), 1e-9);
        assertEquals(137, ColumnYield.yield(8, 100), 1);
        assertEquals(150, ColumnYield.yield(12, 100), 1);
        assertEquals(155, ColumnYield.yield(16, 100), 1);
        assertEquals(100, ColumnYield.yield(2, 100), 1e-9);
    }
}
