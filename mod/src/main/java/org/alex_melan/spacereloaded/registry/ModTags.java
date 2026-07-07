package org.alex_melan.spacereloaded.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.alex_melan.spacereloaded.SpaceReloaded;

/** Теги расширяемости (FR-002, FR-013): датапаки могут дополнять списки без кода. */
public final class ModTags {
    /** Герметичные блоки. Полные непрозрачные кубы герметичны и без тега (автоправило). */
    public static final TagKey<Block> AIRTIGHT = block("airtight");
    /** Принудительно газопроницаемые (решётки, вентиляция) — сильнее автоправила. */
    public static final TagKey<Block> PASSES_GAS = block("passes_gas");
    /** К чему визуально стыкуются энергокабели. */
    public static final TagKey<Block> ENERGY_CONNECTABLE = block("energy_connectable");
    /** Проводники энергосети (кабель, колонна РИТЭГов) — образуют сеть. */
    public static final TagKey<Block> ENERGY_CONDUIT = block("energy_conduit");
    /** Детали, захватываемые сборкой ракеты (FR-020). */
    public static final TagKey<Block> ROCKET_PARTS = block("rocket_parts");

    /** Что электролизёр принимает как источник воды (льды). */
    public static final TagKey<Item> ELECTROLYZER_INPUT = item("electrolyzer_input");
    /** Сырьё перегонного куба (нефтеносный сланец). */
    public static final TagKey<Item> REFINERY_INPUT = item("refinery_input");

    private static TagKey<Item> item(String name) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, name));
    }

    private static TagKey<Block> block(String name) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, name));
    }

    private ModTags() {
    }
}
