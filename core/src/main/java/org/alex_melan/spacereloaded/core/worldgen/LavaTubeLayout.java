package org.alex_melan.spacereloaded.core.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;

/**
 * Лавовая трубка Луны (004, FR-240, D38): детерминированная функция региона 128×128 блоков.
 * Трубка — полилиния из 4–8 сегментов по 15–20 блоков с поворотами ≤ 35°, центр на глубине
 * 12–30 блоков под местной поверхностью, эллиптическое сечение с плоским полом, 1–2 провала-
 * окна до поверхности. Размеры уменьшены против реальных (провал Marius Hills 58×49 м,
 * трубки — сотни метров) до игровых: ширина 7–13, высота 5–9 (research-physics §9).
 *
 * <p>Генерация мира пишет только в свой чанк, поэтому фича каждого чанка перебирает регионы
 * рядом, берёт их трубки ({@link #forRegion}) и вырезает клетки своего чанка: {@link #contains}
 * — чистая функция координат, поэтому куски трубки из разных чанков сшиваются без состояния.
 *
 * @param segments  сегменты оси трубки
 * @param halfWidth полуширина сечения, блоки
 * @param halfHeight полувысота сечения, блоки
 * @param skylights провалы-окна
 */
public record LavaTubeLayout(List<Segment> segments, double halfWidth, double halfHeight, List<Skylight> skylights) {

    /** Размер региона, блоки. */
    public static final int REGION = 128;

    /** Максимальный вынос трубки от своего региона: 8 сегментов × 20 блоков. */
    public static final int MAX_REACH = 8 * 20 + 8;

    /** Доля полувысоты под осью, ниже которой — пол (плоское дно застывшего потока). */
    private static final double FLOOR_FRACTION = 0.7;

    /** Сегмент оси: концы в XZ и глубина центра под поверхностью на концах. */
    public record Segment(double x0, double z0, double x1, double z1, double depth0, double depth1) {
    }

    /** Провал-окно: вертикальный колодец до поверхности. */
    public record Skylight(double x, double z, double radius) {
    }

    /** Трубка региона или пусто (с вероятностью 1 − chance). */
    public static Optional<LavaTubeLayout> forRegion(long worldSeed, int regionX, int regionZ, double chance) {
        SplittableRandom random = new SplittableRandom(mix(worldSeed, regionX, regionZ));
        if (random.nextDouble() >= chance) {
            return Optional.empty();
        }
        double x = regionX * (double) REGION + 24 + random.nextDouble() * 80;
        double z = regionZ * (double) REGION + 24 + random.nextDouble() * 80;
        double heading = random.nextDouble() * 2 * Math.PI;
        double depth = 12 + random.nextDouble() * 18;
        int count = 4 + random.nextInt(5);
        List<Segment> segments = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double length = 15 + random.nextDouble() * 5;
            heading += Math.toRadians(-35 + random.nextDouble() * 70);
            double nx = x + Math.cos(heading) * length;
            double nz = z + Math.sin(heading) * length;
            double nd = Math.max(12, Math.min(30, depth + (-3 + random.nextDouble() * 6)));
            segments.add(new Segment(x, z, nx, nz, depth, nd));
            x = nx;
            z = nz;
            depth = nd;
        }
        double halfWidth = 3.5 + random.nextDouble() * 3;
        double halfHeight = 2.5 + random.nextDouble() * 2;
        int lights = 1 + random.nextInt(2);
        List<Skylight> skylights = new ArrayList<>(lights);
        for (int i = 0; i < lights; i++) {
            Segment s = segments.get(random.nextInt(segments.size()));
            double t = 0.3 + random.nextDouble() * 0.4;
            skylights.add(new Skylight(s.x0 + (s.x1 - s.x0) * t, s.z0 + (s.z1 - s.z0) * t,
                    3 + random.nextDouble()));
        }
        return Optional.of(new LavaTubeLayout(List.copyOf(segments), halfWidth, halfHeight, List.copyOf(skylights)));
    }

    /** Смешивание зерна мира и координат региона (SplitMix-подобно). */
    static long mix(long seed, int rx, int rz) {
        long h = seed ^ 0x9E3779B97F4A7C15L;
        h ^= rx * 0xC2B2AE3D27D4EB4FL;
        h = Long.rotateLeft(h, 31) * 0x165667B19E3779F9L;
        h ^= rz * 0x27D4EB2F165667C5L;
        h ^= h >>> 29;
        return h * 0x94D049BB133111EBL;
    }

    /**
     * Внутри ли точка полости трубки. {@code surfaceY} — высота местной поверхности колонки:
     * ось трубки идёт на заданной глубине под ней.
     */
    public boolean contains(double x, double y, double z, double surfaceY) {
        for (Segment s : segments) {
            double dx = s.x1 - s.x0;
            double dz = s.z1 - s.z0;
            double len2 = dx * dx + dz * dz;
            double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((x - s.x0) * dx + (z - s.z0) * dz) / len2));
            double px = s.x0 + dx * t;
            double pz = s.z0 + dz * t;
            double horizontal = Math.hypot(x - px, z - pz);
            if (horizontal > halfWidth) {
                continue;
            }
            double centerY = surfaceY - (s.depth0 + (s.depth1 - s.depth0) * t);
            double dy = y - centerY;
            if (dy < -halfHeight * FLOOR_FRACTION) {
                continue;
            }
            double e = (horizontal / halfWidth) * (horizontal / halfWidth) + (dy / halfHeight) * (dy / halfHeight);
            if (e <= 1) {
                return true;
            }
        }
        return false;
    }

    /** Колонка — провал-окно (вырезается от полости до поверхности). */
    public boolean isSkylight(double x, double z) {
        for (Skylight light : skylights) {
            if (Math.hypot(x - light.x, z - light.z) <= light.radius) {
                return true;
            }
        }
        return false;
    }

    /** Наибольшая глубина потолка трубки под поверхностью в колонке окна (для вырезания колодца). */
    public double maxCenterDepth() {
        double max = 0;
        for (Segment s : segments) {
            max = Math.max(max, Math.max(s.depth0, s.depth1));
        }
        return max;
    }

    /** Габарит в XZ с запасом полуширины: {minX, minZ, maxX, maxZ}. */
    public double[] bounds() {
        double minX = Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (Segment s : segments) {
            minX = Math.min(minX, Math.min(s.x0, s.x1));
            minZ = Math.min(minZ, Math.min(s.z0, s.z1));
            maxX = Math.max(maxX, Math.max(s.x0, s.x1));
            maxZ = Math.max(maxZ, Math.max(s.z0, s.z1));
        }
        double m = halfWidth + 1;
        return new double[] {minX - m, minZ - m, maxX + m, maxZ + m};
    }

    /** Пересекает ли габарит квадрат чанка [x, x+16) × [z, z+16). */
    public boolean touchesChunk(int chunkMinX, int chunkMinZ) {
        double[] b = bounds();
        return b[2] >= chunkMinX && b[0] < chunkMinX + 16 && b[3] >= chunkMinZ && b[1] < chunkMinZ + 16;
    }

    /** Регионы, трубки которых могут дотянуться до чанка. */
    public static int regionRadius() {
        return (MAX_REACH + REGION - 1) / REGION;
    }
}
