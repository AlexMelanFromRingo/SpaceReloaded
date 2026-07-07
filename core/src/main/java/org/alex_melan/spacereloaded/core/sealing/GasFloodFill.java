package org.alex_melan.spacereloaded.core.sealing;

import org.alex_melan.spacereloaded.core.geometry.LongHashSet;
import org.alex_melan.spacereloaded.core.geometry.LongQueue;
import org.alex_melan.spacereloaded.core.geometry.PackedPos;
import org.alex_melan.spacereloaded.core.voxel.GasPermeability;
import org.alex_melan.spacereloaded.core.voxel.VoxelView;

/**
 * Проверка замкнутости объёма трёхмерным flood fill по 26 направлениям.
 *
 * <p>Портировано из RoomCheckerPlugin (RoomAnalyzer). 26 направлений — осознанный
 * геймплейный выбор (конституция, принцип VIII): газ «просачивается» через
 * диагональные щели, поэтому экономия на угловых блоках наказывается утечкой,
 * как неполная рамка портала. Метрика радиуса — расстояние Чебышёва
 * (куб поиска), как в исходном плагине.
 *
 * <p>Отличия от плагина-прототипа:
 * <ul>
 *   <li>позиции — упакованные long вместо объектов, очередь и множество без боксинга;</li>
 *   <li>статусы {@code LEAK} (коснулись вакуума — доказанная дыра) и
 *       {@code UNBOUNDED} (вышли за радиус) разделены — в плагине оба случая
 *       считались «утечкой»;</li>
 *   <li>ранний выход на первой утечке, если не запрошена диагностика;</li>
 *   <li>мир — иммутабельный {@link VoxelView}-снимок: сам алгоритм можно гонять
 *       в фоновом потоке (принцип IV).</li>
 * </ul>
 */
public final class GasFloodFill {

    /** 26 направлений: 6 граней + 12 рёбер + 8 углов (массив из RoomCheckerPlugin). */
    static final int[][] DIRECTIONS_3D = {
            // Основные 6 направлений
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1},
            // Диагональные на плоскостях
            {1, 1, 0}, {1, -1, 0}, {-1, 1, 0}, {-1, -1, 0},
            {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
            {0, 1, 1}, {0, 1, -1}, {0, -1, 1}, {0, -1, -1},
            // Трёхмерные диагонали
            {1, 1, 1}, {1, 1, -1}, {1, -1, 1}, {1, -1, -1},
            {-1, 1, 1}, {-1, 1, -1}, {-1, -1, 1}, {-1, -1, -1}
    };

    private GasFloodFill() {
    }

    public static SealingResult analyze(VoxelView view, SealingRequest request) {
        long startNanos = System.nanoTime();
        long origin = request.origin();
        int maxRadius = request.maxRadius();
        boolean diagnostic = request.diagnostic();

        LongHashSet leakPoints = new LongHashSet(8);
        LongHashSet escapePoints = new LongHashSet(8);

        if (view.permeabilityAt(origin) != GasPermeability.OPEN) {
            return new SealingResult(SealingStatus.INVALID_ORIGIN,
                    new LongHashSet(1), leakPoints, escapePoints, 0, System.nanoTime() - startNanos);
        }

        LongHashSet visited = new LongHashSet(1024);
        LongQueue queue = new LongQueue(256);
        visited.add(origin);
        queue.enqueue(origin);

        int blocksVisited = 0;

        while (!queue.isEmpty()) {
            long current = queue.dequeue();
            blocksVisited++;

            for (int[] dir : DIRECTIONS_3D) {
                long neighbor = PackedPos.offset(current, dir[0], dir[1], dir[2]);

                if (visited.contains(neighbor)) {
                    continue;
                }

                // Сначала радиус (как в плагине): за пределы куба поиска не заходим
                if (PackedPos.chebyshevDistance(origin, neighbor) > maxRadius) {
                    escapePoints.add(neighbor);
                    if (!diagnostic) {
                        return new SealingResult(SealingStatus.UNBOUNDED, visited,
                                leakPoints, escapePoints, blocksVisited, System.nanoTime() - startNanos);
                    }
                    continue;
                }

                switch (view.permeabilityAt(neighbor)) {
                    case BLOCKED, OUT_OF_BOUNDS -> {
                        // Стена (границы мира — тоже стена, как в плагине)
                    }
                    case VACUUM -> {
                        leakPoints.add(neighbor);
                        if (!diagnostic) {
                            return new SealingResult(SealingStatus.LEAK, visited,
                                    leakPoints, escapePoints, blocksVisited, System.nanoTime() - startNanos);
                        }
                    }
                    case OPEN -> {
                        visited.add(neighbor);
                        queue.enqueue(neighbor);
                    }
                }
            }
        }

        SealingStatus status;
        if (!leakPoints.isEmpty()) {
            status = SealingStatus.LEAK;
        } else if (!escapePoints.isEmpty()) {
            status = SealingStatus.UNBOUNDED;
        } else {
            status = SealingStatus.SEALED;
        }

        return new SealingResult(status, visited, leakPoints, escapePoints,
                blocksVisited, System.nanoTime() - startNanos);
    }
}
