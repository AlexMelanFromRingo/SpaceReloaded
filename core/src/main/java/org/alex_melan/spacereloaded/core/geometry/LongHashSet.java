package org.alex_melan.spacereloaded.core.geometry;

import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;

/**
 * Открытая адресация с линейным пробированием для множества long'ов —
 * без боксинга для {@code visited} flood fill.
 *
 * <p>Сентинел пустой ячейки — {@link Long#MIN_VALUE}: как упакованная позиция
 * это X = −33 554 432 (за мировой границей ±30M), в игровых данных не встречается.
 */
public final class LongHashSet {
    private static final long EMPTY = Long.MIN_VALUE;

    private long[] table;
    private int size;
    private int resizeThreshold;

    public LongHashSet(int expectedElements) {
        int cap = Integer.highestOneBit(Math.max(32, expectedElements * 2 - 1)) << 1;
        allocate(cap);
    }

    /** @return true, если элемент добавлен (его не было) */
    public boolean add(long value) {
        checkValue(value);
        int mask = table.length - 1;
        int slot = mix(value) & mask;
        while (table[slot] != EMPTY) {
            if (table[slot] == value) {
                return false;
            }
            slot = (slot + 1) & mask;
        }
        table[slot] = value;
        if (++size >= resizeThreshold) {
            rehash();
        }
        return true;
    }

    public boolean contains(long value) {
        checkValue(value);
        int mask = table.length - 1;
        int slot = mix(value) & mask;
        while (table[slot] != EMPTY) {
            if (table[slot] == value) {
                return true;
            }
            slot = (slot + 1) & mask;
        }
        return false;
    }

    public void addAll(LongHashSet other) {
        var it = other.iterator();
        while (it.hasNext()) {
            add(it.nextLong());
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public long[] toArray() {
        long[] out = new long[size];
        int i = 0;
        for (long v : table) {
            if (v != EMPTY) {
                out[i++] = v;
            }
        }
        return out;
    }

    public PrimitiveIterator.OfLong iterator() {
        return new PrimitiveIterator.OfLong() {
            private int index = advance(0);

            private int advance(int from) {
                while (from < table.length && table[from] == EMPTY) {
                    from++;
                }
                return from;
            }

            @Override
            public boolean hasNext() {
                return index < table.length;
            }

            @Override
            public long nextLong() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                long v = table[index];
                index = advance(index + 1);
                return v;
            }
        };
    }

    private static void checkValue(long value) {
        if (value == EMPTY) {
            throw new IllegalArgumentException("Long.MIN_VALUE is reserved as the empty sentinel");
        }
    }

    private static int mix(long value) {
        long h = value * 0x9E3779B97F4A7C15L;
        return (int) (h ^ (h >>> 32));
    }

    private void allocate(int capacity) {
        table = new long[capacity];
        java.util.Arrays.fill(table, EMPTY);
        resizeThreshold = capacity / 2;
    }

    private void rehash() {
        long[] old = table;
        allocate(table.length << 1);
        int reinserted = 0;
        int mask = table.length - 1;
        for (long v : old) {
            if (v == EMPTY) {
                continue;
            }
            int slot = mix(v) & mask;
            while (table[slot] != EMPTY) {
                slot = (slot + 1) & mask;
            }
            table[slot] = v;
            reinserted++;
        }
        size = reinserted;
    }
}
