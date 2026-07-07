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
