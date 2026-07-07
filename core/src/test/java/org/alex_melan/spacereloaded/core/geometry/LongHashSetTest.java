package org.alex_melan.spacereloaded.core.geometry;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LongHashSetTest {

    @Test
    void addContainsAndDuplicates() {
        LongHashSet set = new LongHashSet(4);
        assertTrue(set.add(42));
        assertFalse(set.add(42));
        assertTrue(set.contains(42));
        assertFalse(set.contains(43));
        assertEquals(1, set.size());
    }

    @Test
    void matchesJavaUtilSetUnderChurn() {
        LongHashSet set = new LongHashSet(8);
        Set<Long> reference = new HashSet<>();
        Random random = new Random(20260707);
        for (int i = 0; i < 50_000; i++) {
            long v = random.nextInt(20_000) - 10_000;
            assertEquals(reference.add(v), set.add(v));
        }
        assertEquals(reference.size(), set.size());
        for (long v : reference) {
            assertTrue(set.contains(v));
        }
    }

    @Test
    void iteratorAndToArrayYieldAllElements() {
        LongHashSet set = new LongHashSet(4);
        for (long v = -5; v <= 5; v++) {
            set.add(v);
        }
        Set<Long> collected = new HashSet<>();
        var it = set.iterator();
        while (it.hasNext()) {
            collected.add(it.nextLong());
        }
        assertEquals(11, collected.size());
        assertEquals(11, set.toArray().length);
    }

    @Test
    void sentinelValueRejected() {
        LongHashSet set = new LongHashSet(4);
        assertThrows(IllegalArgumentException.class, () -> set.add(Long.MIN_VALUE));
    }
}
