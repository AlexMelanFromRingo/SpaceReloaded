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
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.energy.BatteryBlockEntity;
import org.alex_melan.spacereloaded.energy.CableBlock;
import org.alex_melan.spacereloaded.energy.CreativePowerBlockEntity;
import org.alex_melan.spacereloaded.energy.MachineBlock;
import org.alex_melan.spacereloaded.energy.RtgBlockEntity;
import org.alex_melan.spacereloaded.energy.SolarPanelBlockEntity;
import org.alex_melan.spacereloaded.sealing.AtmosphereControllerBlock;
import org.alex_melan.spacereloaded.sealing.HermeticHatchBlock;

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

    /** Герметичное стекло: прозрачное, но держит атмосферу (тег airtight). */
    public static final Block HERMETIC_GLASS = register("hermetic_glass",
            TransparentBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(2.5f, 8.0f)
                    .sound(SoundType.GLASS)
                    .noOcclusion());

    /** Герметичный люк с интерлоком — строительный элемент шлюза (T034). */
    public static final HermeticHatchBlock HERMETIC_HATCH = register("hermetic_hatch",
            HermeticHatchBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(3.5f, 8.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion());

    /** Солнечная панель (FR-011). */
    public static final Block SOLAR_PANEL = register("solar_panel",
            props -> new MachineBlock<>(props, SolarPanelBlockEntity::new,
                    () -> ModBlockEntities.SOLAR_PANEL, SolarPanelBlockEntity::serverTick),
            BlockBehaviour.Properties.of()
                    .strength(2.5f, 6.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());

    /** РИТЭГ (FR-011). */
    public static final Block RTG = register("rtg",
            props -> new MachineBlock<>(props, RtgBlockEntity::new,
                    () -> ModBlockEntities.RTG, RtgBlockEntity::serverTick),
            BlockBehaviour.Properties.of()
                    .strength(3.5f, 9.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 7));

    /** Аккумулятор (FR-010). */
    public static final Block BATTERY = register("battery",
            props -> new MachineBlock<>(props, BatteryBlockEntity::new,
                    () -> ModBlockEntities.BATTERY, BatteryBlockEntity::serverTick),
            BlockBehaviour.Properties.of()
                    .strength(3.0f, 8.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());

    /** Энергокабель: сеть с кэшируемой топологией (T032). */
    public static final Block ENERGY_CABLE = register("energy_cable", CableBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(1.5f, 5.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion());

    /** Креативный источник энергии — для тестов (quickstart) и отладки. */
    public static final Block CREATIVE_POWER = register("creative_power",
            props -> new MachineBlock<>(props, CreativePowerBlockEntity::new,
                    () -> ModBlockEntities.CREATIVE_POWER, CreativePowerBlockEntity::serverTick),
            BlockBehaviour.Properties.of()
                    .strength(-1.0f, 3_600_000.0f) // неразрушим, как бедрок
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 10));

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
