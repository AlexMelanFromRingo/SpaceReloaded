package org.alex_melan.spacereloaded.multiblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.core.multiblock.MultiblockTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Датапак-шаблоны мультиблоков (005, FR-321, D55): синхронизируемый реестр
 * {@code spacereloaded:multiblock} — молот проверяет по нему структуры на сервере, руководство
 * рисует их схемы на клиенте. Шаблон аддона появляется в обоих без Java.
 */
public final class MultiblockTemplates {

    /** Клетка: смещение (локальные x, y, z) и матчер (id блока или «#тег»). */
    public record CellEntry(List<Integer> offset, String block) {
        public static final Codec<CellEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.listOf(3, 3).fieldOf("offset").forGetter(CellEntry::offset),
                Codec.STRING.fieldOf("block").forGetter(CellEntry::block)
        ).apply(i, CellEntry::new));

        MultiblockTemplate.Cell toCore() {
            return new MultiblockTemplate.Cell(offset.get(0), offset.get(1), offset.get(2), block);
        }
    }

    /** Повторяемый сегмент. */
    public record RepeatEntry(List<CellEntry> cells, List<Integer> step, int min, int max, int display) {
        public static final Codec<RepeatEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                CellEntry.CODEC.listOf().fieldOf("cells").forGetter(RepeatEntry::cells),
                Codec.INT.listOf(3, 3).fieldOf("step").forGetter(RepeatEntry::step),
                Codec.INT.fieldOf("min").forGetter(RepeatEntry::min),
                Codec.INT.fieldOf("max").forGetter(RepeatEntry::max),
                Codec.INT.optionalFieldOf("display", 4).forGetter(RepeatEntry::display)
        ).apply(i, RepeatEntry::new));
    }

    /** Шаблон. */
    public record TemplateEntry(Identifier key, List<CellEntry> cells, Optional<RepeatEntry> repeat) {
        public static final Codec<TemplateEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("key").forGetter(TemplateEntry::key),
                CellEntry.CODEC.listOf().optionalFieldOf("cells", List.of()).forGetter(TemplateEntry::cells),
                RepeatEntry.CODEC.optionalFieldOf("repeat").forGetter(TemplateEntry::repeat)
        ).apply(i, TemplateEntry::new));

        public MultiblockTemplate toCore() {
            return new MultiblockTemplate(cells.stream().map(CellEntry::toCore).toList(),
                    repeat.map(r -> new MultiblockTemplate.Repeat(r.cells().stream().map(CellEntry::toCore).toList(),
                            r.step().get(0), r.step().get(1), r.step().get(2), r.min(), r.max(), r.display())));
        }

        /** Все клетки для показа: фиксированные + repeat × display (+ ключ в начале координат). */
        public List<MultiblockTemplate.Cell> displayCells() {
            List<MultiblockTemplate.Cell> out = new ArrayList<>();
            out.add(new MultiblockTemplate.Cell(0, 0, 0, key.toString()));
            cells.forEach(c -> out.add(c.toCore()));
            repeat.ifPresent(r -> {
                for (int k = 0; k < r.display(); k++) {
                    for (CellEntry c : r.cells()) {
                        out.add(new MultiblockTemplate.Cell(c.offset().get(0) + k * r.step().get(0),
                                c.offset().get(1) + k * r.step().get(1), c.offset().get(2) + k * r.step().get(2),
                                c.block()));
                    }
                }
            });
            return out;
        }
    }

    public static final ResourceKey<Registry<TemplateEntry>> MULTIBLOCKS = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "multiblock"));

    private MultiblockTemplates() {
    }

    /** Шаблон с данным ключевым блоком. */
    public static Optional<TemplateEntry> forKey(RegistryAccess access, Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return access.lookupOrThrow(MULTIBLOCKS).stream().filter(t -> t.key().equals(id)).findFirst();
    }

    public static Optional<TemplateEntry> byId(RegistryAccess access, Identifier id) {
        return access.lookupOrThrow(MULTIBLOCKS).getOptional(id);
    }

    /** Совпадает ли состояние с матчером. */
    public static boolean matches(BlockState state, String matcher) {
        if (matcher.startsWith("#")) {
            return state.is(TagKey.create(Registries.BLOCK, Identifier.parse(matcher.substring(1))));
        }
        if ("minecraft:air".equals(matcher)) {
            return state.isAir();
        }
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(Identifier.parse(matcher));
    }

    /** Мировая позиция локальной клетки при данном лице ключа. */
    public static BlockPos worldPos(BlockPos key, Direction face, int x, int y, int z) {
        int[] w = MultiblockTemplate.toWorld(x, y, z, face.getStepX(), face.getStepZ());
        return key.offset(w[0], w[1], w[2]);
    }

    /** Проверка шаблона в мире (незагруженные клетки — не совпадают). */
    public static MultiblockTemplate.Result match(Level level, BlockPos key, Direction face, TemplateEntry template) {
        return template.toCore().match((x, y, z, matcher) -> {
            BlockPos pos = worldPos(key, face, x, y, z);
            return level.isLoaded(pos) && matches(level.getBlockState(pos), matcher);
        });
    }

    /** Клетки структуры (для претензий): фиксированные + повторы + следующая клетка сегмента. */
    public static List<BlockPos> cells(BlockPos key, Direction face, TemplateEntry template, int repeats) {
        List<BlockPos> out = new ArrayList<>();
        out.add(key);
        for (CellEntry c : template.cells()) {
            out.add(worldPos(key, face, c.offset().get(0), c.offset().get(1), c.offset().get(2)));
        }
        template.repeat().ifPresent(r -> {
            for (int k = 0; k <= Math.min(repeats, r.max() - 1); k++) {
                for (CellEntry c : r.cells()) {
                    out.add(worldPos(key, face, c.offset().get(0) + k * r.step().get(0),
                            c.offset().get(1) + k * r.step().get(1), c.offset().get(2) + k * r.step().get(2)));
                }
            }
        });
        return out;
    }
}
