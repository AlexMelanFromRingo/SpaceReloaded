package org.alex_melan.spacereloaded.core.geometry;

/**
 * Упаковка блочных координат в один long — формат совместим с ванильным
 * {@code BlockPos.asLong()}: X — 26 бит, Z — 26 бит, Y — 12 бит.
 * Это позволяет fabric-слою передавать позиции в ядро без конвертации.
 *
 * <p>Диапазоны: X,Z ∈ [-33 554 432; 33 554 431], Y ∈ [-2048; 2047] —
 * с запасом покрывает игровой мир (граница ±30M, высота ±2К).
 */
public final class PackedPos {
    private static final int X_BITS = 26;
    private static final int Z_BITS = 26;
    private static final int Y_BITS = 12;

    private static final long X_MASK = (1L << X_BITS) - 1;
    private static final long Y_MASK = (1L << Y_BITS) - 1;
    private static final long Z_MASK = (1L << Z_BITS) - 1;

    private static final int X_SHIFT = Y_BITS + Z_BITS; // 38
    private static final int Z_SHIFT = Y_BITS;          // 12

    private PackedPos() {
    }

    public static long pack(int x, int y, int z) {
        return ((x & X_MASK) << X_SHIFT) | ((z & Z_MASK) << Z_SHIFT) | (y & Y_MASK);
    }

    public static int unpackX(long packed) {
        return (int) (packed >> X_SHIFT); // арифметический сдвиг восстанавливает знак
    }

    public static int unpackY(long packed) {
        return (int) (packed << (64 - Y_BITS) >> (64 - Y_BITS));
    }

    public static int unpackZ(long packed) {
        return (int) (packed << (64 - X_SHIFT) >> (64 - Z_BITS));
    }

    public static long offset(long packed, int dx, int dy, int dz) {
        return pack(unpackX(packed) + dx, unpackY(packed) + dy, unpackZ(packed) + dz);
    }

    /** Расстояние Чебышёва между двумя позициями — метрика радиуса зоны. */
    public static int chebyshevDistance(long a, long b) {
        int dx = Math.abs(unpackX(a) - unpackX(b));
        int dy = Math.abs(unpackY(a) - unpackY(b));
        int dz = Math.abs(unpackZ(a) - unpackZ(b));
        return Math.max(dx, Math.max(dy, dz));
    }

    public static String toString(long packed) {
        return "(" + unpackX(packed) + ", " + unpackY(packed) + ", " + unpackZ(packed) + ")";
    }
}
