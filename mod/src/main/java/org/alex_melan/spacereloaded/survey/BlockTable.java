package org.alex_melan.spacereloaded.survey;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Таблица «блок или #тег → запись» датапак-реестра (009: сигнатуры минералов, диэлектрики).
 * Точный id важнее тега; пересобирается при смене экземпляра реестра (перезагрузка датапаков).
 */
final class BlockTable<T> {

    private final Function<T, List<String>> blocksOf;
    private Registry<T> cached;
    private Map<Block, T> byBlock = Map.of();
    private Map<TagKey<Block>, T> byTag = Map.of();

    BlockTable(Function<T, List<String>> blocksOf) {
        this.blocksOf = blocksOf;
    }

    synchronized void refresh(Registry<T> registry) {
        if (registry == cached) {
            return;
        }
        Map<Block, T> blocks = new HashMap<>();
        Map<TagKey<Block>, T> tags = new LinkedHashMap<>();
        for (T entry : registry) {
            for (String id : blocksOf.apply(entry)) {
                if (id.startsWith("#")) {
                    tags.put(TagKey.create(Registries.BLOCK, Identifier.parse(id.substring(1))), entry);
                } else {
                    BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id)).ifPresent(b -> blocks.put(b, entry));
                }
            }
        }
        byBlock = blocks;
        byTag = tags;
        cached = registry;
    }

    T get(BlockState state) {
        T exact = byBlock.get(state.getBlock());
        if (exact != null) {
            return exact;
        }
        for (var e : byTag.entrySet()) {
            if (state.is(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }
}
