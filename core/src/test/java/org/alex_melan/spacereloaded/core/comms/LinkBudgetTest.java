package org.alex_melan.spacereloaded.core.comms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Бюджет линии DSN (research-design §6.1, SC-007). */
class LinkBudgetTest {

    @Test
    void marsAndMoon() {
        assertEquals(4.86e6, LinkBudget.rate(100, 3, 34, 2.25e11), 0.3e6);
        assertEquals(37.8e3, LinkBudget.rate(100, 3, 3, 2.25e11), 3e3);
        assertEquals(2.89e8, LinkBudget.rate(20, 1, 3, 3.84e8), 0.2e8);
    }
}
