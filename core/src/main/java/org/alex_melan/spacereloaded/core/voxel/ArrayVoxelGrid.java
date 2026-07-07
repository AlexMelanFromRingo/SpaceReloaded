package org.alex_melan.spacereloaded.core.voxel;

import org.alex_melan.spacereloaded.core.geometry.PackedPos;

/**
 * Плотный воксельный снимок на byte[] — целевой формат снапшота региона
 * и строительный материал тестовых сцен.
 */
public final class ArrayVoxelGrid implements VoxelView {
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final byte[] cells;

    private ArrayVoxelGrid(int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ, byte[] cells) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.cells = cells;
    }

    @Override
    public GasPermeability permeabilityAt(long packedPos) {
        int x = PackedPos.unpackX(packedPos) - minX;
        int y = PackedPos.unpackY(packedPos) - minY;
        int z = PackedPos.unpackZ(packedPos) - minZ;
        if (x < 0 || x >= sizeX || y < 0 || y >= sizeY || z < 0 || z >= sizeZ) {
            return GasPermeability.OUT_OF_BOUNDS;
        }
        return PERMEABILITIES[cells[index(x, y, z)]];
    }

    private int index(int x, int y, int z) {
        return x + sizeX * (z + sizeZ * y);
    }

    private static final GasPermeability[] PERMEABILITIES = GasPermeability.values();

    public static Builder builder(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return new Builder(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** Билдер; координаты границ включительны. */
    public static final class Builder {
        private final int minX;
        private final int minY;
        private final int minZ;
        private final int sizeX;
        private final int sizeY;
        private final int sizeZ;
        private final byte[] cells;

        private Builder(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            if (maxX < minX || maxY < minY || maxZ < minZ) {
                throw new IllegalArgumentException("inverted bounds");
            }
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.sizeX = maxX - minX + 1;
            this.sizeY = maxY - minY + 1;
            this.sizeZ = maxZ - minZ + 1;
            this.cells = new byte[sizeX * sizeY * sizeZ];
        }

        public Builder fillAll(GasPermeability permeability) {
            java.util.Arrays.fill(cells, (byte) permeability.ordinal());
            return this;
        }

        /** Заполняет параллелепипед (границы включительно). */
        public Builder fillBox(int x1, int y1, int z1, int x2, int y2, int z2, GasPermeability permeability) {
            byte value = (byte) permeability.ordinal();
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    for (int x = x1; x <= x2; x++) {
                        cells[localIndex(x, y, z)] = value;
                    }
                }
            }
            return this;
        }

        public Builder set(int x, int y, int z, GasPermeability permeability) {
            cells[localIndex(x, y, z)] = (byte) permeability.ordinal();
            return this;
        }

        private int localIndex(int x, int y, int z) {
            int lx = x - minX;
            int ly = y - minY;
            int lz = z - minZ;
            if (lx < 0 || lx >= sizeX || ly < 0 || ly >= sizeY || lz < 0 || lz >= sizeZ) {
                throw new IndexOutOfBoundsException("(" + x + "," + y + "," + z + ") outside grid");
            }
            return lx + sizeX * (lz + sizeZ * ly);
        }

        public ArrayVoxelGrid build() {
            return new ArrayVoxelGrid(minX, minY, minZ, sizeX, sizeY, sizeZ, cells.clone());
        }
    }
}
