package org.alex_melan.spacereloaded.core.eclss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Баланс стойки на одного человека (research-design §1.1, SC-001). */
class EclssBalanceTest {

    @Test
    void fullRackReturnsHalfTheOxygen() {
        var d = EclssBalance.day(0.84, 1.01, 0, true, true, true, false);
        assertEquals(0.945, d.water(), 0.001);
        assertEquals(0.57, d.co2Reduced() / d.co2Removed(), 0.01);   // H₂ хватает на 57 % CO₂
        assertEquals(0.472, d.makeup(), 0.002);                       // подпитка — половина воды
        assertEquals(0.21, d.ch4(), 0.003);
        assertEquals(0, d.h2Left(), 1e-9);
    }

    @Test
    void withoutSabatierAllWaterIsMakeup() {
        var d = EclssBalance.day(0.84, 1.01, 0, true, false, true, false);
        assertEquals(0.945, d.makeup(), 0.001);
        assertEquals(0, d.ch4(), 1e-9);
    }

    @Test
    void waterRecoveryAndEnergy() {
        var d = EclssBalance.day(0.84, 1.01, 2.0, true, true, true, true);
        assertEquals(0.472 - 0.935 * 2.0, d.water() - d.waterBack(), 0.002);
        assertEquals(0, d.makeup(), 1e-9);
        assertEquals(5.36, EclssBalance.OGS_KWH_PER_KG_O2, 0.01);
    }
}
