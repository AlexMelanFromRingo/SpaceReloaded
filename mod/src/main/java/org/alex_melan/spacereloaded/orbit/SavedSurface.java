package org.alex_melan.spacereloaded.orbit;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.material.MapColor;

import java.util.HashMap;
import java.util.Map;

/**
 * Поверхность сохранённых на диск, но выгруженных чанков (009): спутник видит всю местность, а у
 * сервера данные есть для любой уже сгенерированной — её читаем из region-файлов в фоновом потоке
 * (чтение NBT асинхронно, разбор — вне главного потока, живые чанки не трогаются; принцип IV).
 * Неразведанная местность остаётся рельефом генератора.
 */
final class SavedSurface {

    /** Верхний видимый блок столбца. */
    record Top(int y, BlockState state) {
    }

    private static final LevelChunkSection[] NONE = new LevelChunkSection[0];

    private final ServerLevel level;
    private final PalettedContainerFactory palettes;
    private final Map<Long, LevelChunkSection[]> chunks = new HashMap<>();

    SavedSurface(ServerLevel level) {
        this.level = level;
        this.palettes = PalettedContainerFactory.create(level.registryAccess());
    }

    private LevelChunkSection[] sections(int cx, int cz) {
        return chunks.computeIfAbsent(ChunkPos.pack(cx, cz), key -> {
            try {
                var tag = level.getChunkSource().chunkMap.read(new ChunkPos(cx, cz)).join();
                if (tag.isEmpty() || SerializableChunkData.getChunkStatusFromTag(tag.get()) != ChunkStatus.FULL) {
                    return NONE;
                }
                var data = SerializableChunkData.parse(level, palettes, tag.get());
                int min = level.getMinSectionY();
                LevelChunkSection[] out = new LevelChunkSection[level.getSectionsCount()];
                for (var s : data.sectionData()) {
                    int i = s.y() - min;
                    if (i >= 0 && i < out.length) {
                        out[i] = s.chunkSection();
                    }
                }
                return out;
            } catch (RuntimeException e) {
                return NONE;   // чанк старой версии или повреждён — как неразведанный
            }
        });
    }

    /** Верхний блок с цветом карты в сохранённом чанке; null — чанка на диске нет. */
    Top top(int x, int z) {
        LevelChunkSection[] s = sections(x >> 4, z >> 4);
        if (s == NONE) {
            return null;
        }
        int min = level.getMinSectionY();
        for (int i = s.length - 1; i >= 0; i--) {
            if (s[i] == null || s[i].hasOnlyAir()) {
                continue;
            }
            for (int ly = 15; ly >= 0; ly--) {
                BlockState state = s[i].getBlockState(x & 15, ly, z & 15);
                if (!state.isAir() && state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) != MapColor.NONE) {
                    return new Top(((min + i) << 4) + ly, state);
                }
            }
        }
        return null;
    }
}
