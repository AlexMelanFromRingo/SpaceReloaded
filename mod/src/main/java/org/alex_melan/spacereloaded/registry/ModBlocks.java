package org.alex_melan.spacereloaded.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.sealing.AtmosphereControllerBlock;

import java.util.function.Function;

public final class ModBlocks {

    /** Герметичная обшивка — базовый строительный блок баз (T020). */
    public static final Block HULL_PLATING = register("hull_plating", Block::new,
            BlockBehaviour.Properties.of()
                    .strength(3.0f, 8.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());

    /** Контроллер атмосферы — точка отсчёта проверки герметичности (T023). */
    public static final AtmosphereControllerBlock ATMOSPHERE_CONTROLLER = register("atmosphere_controller",
            AtmosphereControllerBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(3.5f, 8.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 4));

    /**
     * Регистрация блока + BlockItem по контракту 26.2: id задаётся заранее
     * через Properties.setId (реестры BlockIds/ItemIds разделены).
     */
    private static <T extends Block> T register(String name,
                                                Function<BlockBehaviour.Properties, T> factory,
                                                BlockBehaviour.Properties properties) {
        Identifier id = Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        T block = Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(properties.setId(blockKey)));

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        return block;
    }

    public static void init() {
        // Класслоадинг триггерит статические регистрации
    }

    private ModBlocks() {
    }
}
