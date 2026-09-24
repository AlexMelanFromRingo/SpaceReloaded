package org.alex_melan.spacereloaded.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.alex_melan.spacereloaded.SpaceReloaded;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Массы предметов груза (датапак-реестр {@code spacereloaded:item_mass}): записи
 * {@code {"items": ["id" | "#тег", …], "kg": масса}}. Соглашение мода: блок-предмет — 10 литров
 * материала с реальной плотностью (реголит 1.5 т/м³ → 15 кг, лёд 0.92 → 9.2 кг, сталь 7.85 →
 * 78.5 кг), слиток/пыль — 1/9 блока, еда ≈ 0.3 кг. Пропорции честные, абсолют — игровой: 1 м³
 * блока-мира не таскают в инвентаре. Приоритет: точный id → тег → по умолчанию (блок 10 кг,
 * предмет 1 кг). Сейчас масса учитывается капсулой катапульты (004); аддоны дополняют таблицу.
 */
public final class ItemMasses {

    /** Запись таблицы. */
    public record Entry(List<String> items, double kg) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.listOf().fieldOf("items").forGetter(Entry::items),
                Codec.doubleRange(0, 1e6).fieldOf("kg").forGetter(Entry::kg)
        ).apply(i, Entry::new));
    }

    public static final ResourceKey<Registry<Entry>> ITEM_MASS = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "item_mass"));

    public static final double DEFAULT_BLOCK_KG = 10.0;
    public static final double DEFAULT_ITEM_KG = 1.0;

    private static Registry<Entry> cachedRegistry;
    private static Map<Item, Double> byItem = Map.of();
    private static Map<TagKey<Item>, Double> byTag = Map.of();

    private ItemMasses() {
    }

    private static synchronized void refresh(RegistryAccess access) {
        Registry<Entry> registry = access.lookupOrThrow(ITEM_MASS);
        if (registry == cachedRegistry) {
            return;
        }
        Map<Item, Double> items = new HashMap<>();
        Map<TagKey<Item>, Double> tags = new HashMap<>();
        for (Entry entry : registry) {
            for (String id : entry.items()) {
                if (id.startsWith("#")) {
                    tags.put(TagKey.create(Registries.ITEM, Identifier.parse(id.substring(1))), entry.kg());
                } else {
                    BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).ifPresent(it -> items.put(it, entry.kg()));
                }
            }
        }
        byItem = items;
        byTag = tags;
        cachedRegistry = registry;
    }

    /** Масса одного предмета, кг. */
    public static double massOf(RegistryAccess access, ItemStack stack) {
        refresh(access);
        Double exact = byItem.get(stack.getItem());
        if (exact != null) {
            return exact;
        }
        for (var e : byTag.entrySet()) {
            if (stack.is(e.getKey())) {
                return e.getValue();
            }
        }
        return stack.getItem() instanceof BlockItem ? DEFAULT_BLOCK_KG : DEFAULT_ITEM_KG;
    }

    /** Масса стопки, кг. */
    public static double massOfStack(RegistryAccess access, ItemStack stack) {
        return stack.isEmpty() ? 0 : massOf(access, stack) * stack.getCount();
    }
}
