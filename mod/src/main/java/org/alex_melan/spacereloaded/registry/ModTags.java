package org.alex_melan.spacereloaded.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.alex_melan.spacereloaded.SpaceReloaded;

/** Теги расширяемости (FR-002, FR-013): датапаки могут дополнять списки без кода. */
public final class ModTags {
    /** Герметичные блоки. Полные непрозрачные кубы герметичны и без тега (автоправило). */
    public static final TagKey<Block> AIRTIGHT = block("airtight");
    /** Принудительно газопроницаемые (решётки, вентиляция) — сильнее автоправила. */
    public static final TagKey<Block> PASSES_GAS = block("passes_gas");

    private static TagKey<Block> block(String name) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, name));
    }

    private ModTags() {
    }
}
