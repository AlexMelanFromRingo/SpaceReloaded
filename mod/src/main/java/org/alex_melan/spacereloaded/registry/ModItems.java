package org.alex_melan.spacereloaded.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import org.alex_melan.spacereloaded.SpaceReloaded;

public final class ModItems {

    /** Рендер-ассет маски: assets/spacereloaded/equipment/oxygen_mask.json + слой humanoid. */
    private static final ResourceKey<EquipmentAsset> OXYGEN_MASK_ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID,
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "oxygen_mask"));

    /**
     * Кислородная маска (минимальный скафандр P1): надета на голову — вакуум не
     * душит. Запас кислорода и баллоны — позже (пока маска «вечная», TODO).
     */
    public static final Item OXYGEN_MASK = register("oxygen_mask", properties -> new Item(properties
            .stacksTo(1)
            .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD)
                    .setAsset(OXYGEN_MASK_ASSET)
                    .build())));

    /** Баллон со сжатым кислородом: расходуется маской в вакууме, заряжается электролизёром. */
    public static final Item OXYGEN_CANISTER = register("oxygen_canister",
            properties -> new org.alex_melan.spacereloaded.sealing.OxygenCanisterItem(properties.durability(1200)));

    /** Заправочный рукав: бак → ракета (и обратно с sneak). */
    public static final Item FUELING_HOSE = register("fueling_hose",
            properties -> new Item(properties.stacksTo(1)));

    // --- Материалы промышленной цепочки (US3, T040/T042) ---
    /** Вольфрамовый лом — боеприпас орбитальной пушки (US7). */
    public static final Item TUNGSTEN_ROD = register("tungsten_rod",
            properties -> new Item(properties.stacksTo(16)));
    /** Целеуказатель: метка на поверхности для наведения пушки. */
    public static final Item TARGETING_DESIGNATOR = register("targeting_designator",
            properties -> new org.alex_melan.spacereloaded.cannon.TargetingDesignatorItem(
                    properties.stacksTo(1)));

    public static final Item RAW_TITANIUM = simple("raw_titanium");
    public static final Item RAW_TUNGSTEN = simple("raw_tungsten");
    public static final Item TITANIUM_INGOT = simple("titanium_ingot");
    public static final Item TUNGSTEN_INGOT = simple("tungsten_ingot");
    public static final Item STEEL_INGOT = simple("steel_ingot");
    public static final Item TITANIUM_ALLOY_INGOT = simple("titanium_alloy_ingot");
    public static final Item TITANIUM_DUST = simple("titanium_dust");
    public static final Item TUNGSTEN_DUST = simple("tungsten_dust");
    public static final Item IRON_DUST = simple("iron_dust");
    public static final Item COAL_DUST = simple("coal_dust");
    /** Шихта: железная пыль + угольная пыль — полуфабрикат стали. */
    public static final Item STEEL_BLEND = simple("steel_blend");
    public static final Item CARBON_FIBER = simple("carbon_fiber");

    private static Item simple(String name) {
        return register(name, Item::new);
    }

    private static Item register(String name, java.util.function.Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
    }

    public static void init() {
    }

    private ModItems() {
    }
}
