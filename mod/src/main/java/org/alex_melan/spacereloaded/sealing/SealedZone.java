package org.alex_melan.spacereloaded.sealing;

import net.minecraft.core.BlockPos;
import org.alex_melan.spacereloaded.core.geometry.LongHashSet;
import org.alex_melan.spacereloaded.core.geometry.PackedPos;
import org.alex_melan.spacereloaded.core.sealing.SealingStatus;

/**
 * Кэшированная герметичная зона (FR-003): владелец-контроллер, объём (воздух),
 * граница (стены). Мутируется только в главном потоке через ZoneManager.
 */
public final class SealedZone {
    private final BlockPos controllerPos;
    private SealingStatus status = SealingStatus.INVALID_ORIGIN;
    private LongHashSet volume = new LongHashSet(1);
    /** Все индексируемые ячейки: объём + 26-граница + точки утечки. */
    private LongHashSet footprint = new LongHashSet(1);

    public SealedZone(BlockPos controllerPos) {
        this.controllerPos = controllerPos;
    }

    public BlockPos controllerPos() {
        return controllerPos;
    }

    public SealingStatus status() {
        return status;
    }

    public boolean isSealed() {
        return status == SealingStatus.SEALED;
    }

    public LongHashSet volume() {
        return volume;
    }

    public LongHashSet footprint() {
        return footprint;
    }

    void update(SealingStatus status, LongHashSet volume, LongHashSet footprint) {
        this.status = status;
        this.volume = volume;
        this.footprint = footprint;
    }

    /** Граница зоны: непроницаемые 26-соседи всех ячеек объёма. Считается в фоне. */
    static LongHashSet computeBoundary(LongHashSet volume) {
        LongHashSet boundary = new LongHashSet(Math.max(16, volume.size()));
        var it = volume.iterator();
        while (it.hasNext()) {
            long cell = it.nextLong();
            for (int[] dir : DIRECTIONS) {
                long neighbor = PackedPos.offset(cell, dir[0], dir[1], dir[2]);
                if (!volume.contains(neighbor)) {
                    boundary.add(neighbor);
                }
            }
        }
        return boundary;
    }

    private static final int[][] DIRECTIONS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1},
            {1, 1, 0}, {1, -1, 0}, {-1, 1, 0}, {-1, -1, 0},
            {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
            {0, 1, 1}, {0, 1, -1}, {0, -1, 1}, {0, -1, -1},
            {1, 1, 1}, {1, 1, -1}, {1, -1, 1}, {1, -1, -1},
            {-1, 1, 1}, {-1, 1, -1}, {-1, -1, 1}, {-1, -1, -1}
    };
}
