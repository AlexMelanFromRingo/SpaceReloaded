package org.alex_melan.spacereloaded.core.nuclear;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Каскад центрифуг (research-design §3.2, SC-004). */
class EnrichmentTest {

    @Test
    void stagesAndProduct() {
        assertEquals(13.5, Enrichment.stages(Enrichment.NATURAL, 0.20, 1.3), 0.1);
        assertEquals(28.7, Enrichment.stages(Enrichment.NATURAL, 0.93, 1.3), 0.1);
        assertEquals(0.22, Enrichment.product(14, Enrichment.NATURAL, 1.3), 0.01);
        assertEquals(0.935, Enrichment.product(29, Enrichment.NATURAL, 1.3), 0.01);
    }

    @Test
    void massBalanceAndSwu() {
        assertEquals(1 / 201.2, Enrichment.productPerFeed(Enrichment.NATURAL, 0.93, Enrichment.TAILS), 1e-4);
        // 1 кг ВОУ 93 % из природного при отвале 0.25 % — ≈ 216 ЕРР (201 кг питания)
        assertEquals(216, Enrichment.swu(1, Enrichment.NATURAL, 0.93, Enrichment.TAILS), 2);
    }
}
