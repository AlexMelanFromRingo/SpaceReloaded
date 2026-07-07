package org.alex_melan.spacereloaded.sealing;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.Heightmap;
import org.alex_melan.spacereloaded.core.geometry.PackedPos;
import org.alex_melan.spacereloaded.core.voxel.GasPermeability;
import org.alex_melan.spacereloaded.core.voxel.VoxelView;

/**
 * Снимок региона для фонового flood fill (принцип IV, FR-005).
 *
 * <p>Снимается в ГЛАВНОМ потоке сервера: копируются {@code PalettedContainer}'ы
 * секций (O(палитра), не O(блоков)) и колонки heightmap'а. После создания
 * иммутабелен — ссылок на живые Chunk/Level не держит.
 *
 * <p>Непрогруженные чанки трактуются как {@code OUT_OF_BOUNDS} (стена):
 * зона, упирающаяся в невыгруженный чанк, честно не пройдёт проверку.
 */
public final class RegionSnapshot implements VoxelView {

    private final int minChunkX;
    private final int minChunkZ;
    private final int chunksX;
    private final int chunksZ;
    private final int minSectionY;
    private final int sectionsY;
    private final int minY;
    private final int maxY;
    private final boolean vacuumWorld;

    /** [chunkIndex][sectionIndex]; null — секция из чистого воздуха либо чанк не прогружен. */
    private final PalettedContainer<BlockState>[] [] sections;
    /** [chunkIndex][x + z*16] — высота верхнего непроходимого блока; null — чанк не прогружен. */
    private final int[][] surface;

    @SuppressWarnings("unchecked")
    private RegionSnapshot(int minChunkX, int minChunkZ, int chunksX, int chunksZ,
                           int minSectionY, int sectionsY, int minY, int maxY, boolean vacuumWorld) {
        this.minChunkX = minChunkX;
        this.minChunkZ = minChunkZ;
        this.chunksX = chunksX;
        this.chunksZ = chunksZ;
        this.minSectionY = minSectionY;
        this.sectionsY = sectionsY;
        this.minY = minY;
        this.maxY = maxY;
        this.vacuumWorld = vacuumWorld;
        this.sections = new PalettedContainer[chunksX * chunksZ][];
        this.surface = new int[chunksX * chunksZ][];
    }

    /** Вызывать строго из главного потока сервера. */
    public static RegionSnapshot capture(ServerLevel level, BlockPos origin, int radius, boolean vacuumWorld) {
        if (!level.getServer().isSameThread()) {
            throw new IllegalStateException("RegionSnapshot.capture must run on the server thread");
        }
        int margin = radius + 1; // fill проверяет соседей на границе радиуса
        int minBlockX = origin.getX() - margin;
        int maxBlockX = origin.getX() + margin;
        int minBlockZ = origin.getZ() - margin;
        int maxBlockZ = origin.getZ() + margin;
        int minY = Math.max(level.getMinY(), origin.getY() - margin);
        int maxY = Math.min(level.getMaxY(), origin.getY() + margin);

        int minChunkX = minBlockX >> 4;
        int maxChunkX = maxBlockX >> 4;
        int minChunkZ = minBlockZ >> 4;
        int maxChunkZ = maxBlockZ >> 4;
        int minSectionY = minY >> 4;
        int maxSectionY = maxY >> 4;

        RegionSnapshot snapshot = new RegionSnapshot(minChunkX, minChunkZ,
                maxChunkX - minChunkX + 1, maxChunkZ - minChunkZ + 1,
                minSectionY, maxSectionY - minSectionY + 1, minY, maxY, vacuumWorld);

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue; // не прогружен — останется null => OUT_OF_BOUNDS
                }
                int ci = snapshot.chunkIndex(cx, cz);

                @SuppressWarnings("unchecked")
                PalettedContainer<BlockState>[] copies = new PalettedContainer[snapshot.sectionsY];
                for (int sy = minSectionY; sy <= maxSectionY; sy++) {
                    LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sy));
                    if (!section.hasOnlyAir()) {
                        copies[sy - minSectionY] = section.getStates().copy();
                    }
                }
                snapshot.sections[ci] = copies;

                int[] heights = new int[256];
                for (int lx = 0; lx < 16; lx++) {
                    for (int lz = 0; lz < 16; lz++) {
                        heights[lx + (lz << 4)] = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, lx, lz);
                    }
                }
                snapshot.surface[ci] = heights;
            }
        }
        return snapshot;
    }

    @Override
    public GasPermeability permeabilityAt(long packedPos) {
        int x = PackedPos.unpackX(packedPos);
        int y = PackedPos.unpackY(packedPos);
        int z = PackedPos.unpackZ(packedPos);

        if (y < minY || y > maxY) {
            return GasPermeability.OUT_OF_BOUNDS; // границы мира — стена (как в плагине)
        }
        int cx = x >> 4;
        int cz = z >> 4;
        if (cx < minChunkX || cx >= minChunkX + chunksX || cz < minChunkZ || cz >= minChunkZ + chunksZ) {
            return GasPermeability.OUT_OF_BOUNDS;
        }
        int ci = chunkIndex(cx, cz);
        PalettedContainer<BlockState>[] copies = sections[ci];
        if (copies == null) {
            return GasPermeability.OUT_OF_BOUNDS; // чанк не был прогружен при снятии снимка
        }

        PalettedContainer<BlockState> container = copies[(y >> 4) - minSectionY];
        boolean aboveSurface = y > surface[ci][(x & 15) + ((z & 15) << 4)];
        if (container == null) {
            // Секция из чистого воздуха
            return (vacuumWorld && aboveSurface) ? GasPermeability.VACUUM : GasPermeability.OPEN;
        }
        BlockState state = container.get(x & 15, y & 15, z & 15);
        return GasPermeabilityResolver.resolve(state, aboveSurface, vacuumWorld);
    }

    private int chunkIndex(int cx, int cz) {
        return (cx - minChunkX) + (cz - minChunkZ) * chunksX;
    }
}
