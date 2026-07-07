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
import org.alex_melan.spacereloaded.energy.BatteryBlock;
import org.alex_melan.spacereloaded.energy.CableBlock;
import org.alex_melan.spacereloaded.energy.CreativePowerBlockEntity;
import org.alex_melan.spacereloaded.energy.MachineBlock;
import org.alex_melan.spacereloaded.energy.RtgBlock;
import org.alex_melan.spacereloaded.energy.SolarPanelBlockEntity;
import org.alex_melan.spacereloaded.machine.AssemblyTableBlockEntity;
import org.alex_melan.spacereloaded.machine.CoalGeneratorBlockEntity;
import org.alex_melan.spacereloaded.machine.CrusherBlockEntity;
import org.alex_melan.spacereloaded.machine.ElectrolyzerBlockEntity;
import org.alex_melan.spacereloaded.machine.RefineryBlockEntity;
import org.alex_melan.spacereloaded.machine.ElectricFurnaceBlockEntity;
import org.alex_melan.spacereloaded.machine.ProcessingMachineBlock;
import org.alex_melan.spacereloaded.rocket.AssemblyPylonBlock;
import org.alex_melan.spacereloaded.rocket.FuelTankBlock;
import org.alex_melan.spacereloaded.rocket.FuelingPumpBlock;
import org.alex_melan.spacereloaded.rocket.LaunchPadBlock;
import org.alex_melan.spacereloaded.rocket.RocketSeatBlock;
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

    /** РИТЭГ (FR-011): KSP-стиль — колонна с радиаторами. */
    public static final Block RTG = register("rtg", RtgBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(3.5f, 9.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(state -> 7));

    /** Аккумулятор (FR-010): 5 уровней заряда + GUI. */
    public static final Block BATTERY = register("battery", BatteryBlock::new,
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

    /** Угольный генератор — стартовая энергетика (жжёт печное топливо). */
    public static final Block COAL_GENERATOR = register("coal_generator",
            props -> new ProcessingMachineBlock(props, CoalGeneratorBlockEntity::new,
                    () -> ModBlockEntities.COAL_GENERATOR),
            BlockBehaviour.Properties.of().strength(3.5f, 8.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 8));

    // --- Станки (US3, T041) ---
    public static final Block CRUSHER = register("crusher",
            props -> new ProcessingMachineBlock(props, CrusherBlockEntity::new,
                    () -> ModBlockEntities.CRUSHER),
            BlockBehaviour.Properties.of().strength(3.5f, 8.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());
    public static final Block ELECTRIC_FURNACE = register("electric_furnace",
            props -> new ProcessingMachineBlock(props, ElectricFurnaceBlockEntity::new,
                    () -> ModBlockEntities.ELECTRIC_FURNACE),
            BlockBehaviour.Properties.of().strength(3.5f, 8.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 6));
    public static final Block ASSEMBLY_TABLE = register("assembly_table",
            props -> new ProcessingMachineBlock(props, AssemblyTableBlockEntity::new,
                    () -> ModBlockEntities.ASSEMBLY_TABLE),
            BlockBehaviour.Properties.of().strength(3.5f, 8.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());

    // --- Руды (US3, T040) ---
    public static final Block TITANIUM_ORE = register("titanium_ore", Block::new,
            BlockBehaviour.Properties.of().strength(3.0f, 3.0f).requiresCorrectToolForDrops());
    public static final Block DEEPSLATE_TITANIUM_ORE = register("deepslate_titanium_ore", Block::new,
            BlockBehaviour.Properties.of().strength(4.5f, 3.0f).requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE));
    public static final Block DEEPSLATE_TUNGSTEN_ORE = register("deepslate_tungsten_ore", Block::new,
            BlockBehaviour.Properties.of().strength(5.0f, 3.0f).requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE));

    // --- Инженерные блоки — детали ракет (US3 → US4); поведение сущности в Phase 6 ---
    public static final Block ROCKET_HULL = register("rocket_hull", Block::new,
            BlockBehaviour.Properties.of().strength(3.0f, 10.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());
    public static final Block FUEL_TANK = register("fuel_tank", FuelTankBlock::new,
            BlockBehaviour.Properties.of().strength(3.0f, 10.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());
    public static final Block ROCKET_ENGINE = register("rocket_engine", Block::new,
            BlockBehaviour.Properties.of().strength(3.5f, 10.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion());
    public static final Block COMMAND_MODULE = register("command_module", Block::new,
            BlockBehaviour.Properties.of().strength(3.5f, 10.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());
    /** Гидролоксовый двигатель: ниже тяга, выше Isp — для орбиты и Луны. */
    public static final Block HYDROLOX_ENGINE = register("hydrolox_engine", Block::new,
            BlockBehaviour.Properties.of().strength(3.5f, 10.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion());

    /** Заправочная колонка: автозаправка припаркованной ракеты у площадки. */
    public static final Block FUELING_PUMP = register("fueling_pump", FuelingPumpBlock::new,
            BlockBehaviour.Properties.of().strength(3.0f, 8.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());

    public static final Block GYROSCOPE = register("gyroscope", Block::new,
            BlockBehaviour.Properties.of().strength(3.5f, 10.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());

    // --- Луна (US6) ---
    public static final Block MOON_REGOLITH = register("moon_regolith", Block::new,
            BlockBehaviour.Properties.of().strength(0.6f).sound(SoundType.GRAVEL));
    public static final Block MOON_STONE = register("moon_stone", Block::new,
            BlockBehaviour.Properties.of().strength(2.0f, 7.0f).requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE));
    public static final Block MOON_ICE = register("moon_ice", Block::new,
            BlockBehaviour.Properties.of().strength(1.2f).requiresCorrectToolForDrops()
                    .sound(SoundType.GLASS));

    /** Нефтеносный сланец — сырьё перегонки (глубины Земли). */
    public static final Block OIL_SHALE = register("oil_shale", Block::new,
            BlockBehaviour.Properties.of().strength(3.5f, 3.0f).requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE));

    /** Перегонный куб: сланец → топливо (земная ветка). */
    public static final Block REFINERY = register("refinery",
            props -> new ProcessingMachineBlock(props, RefineryBlockEntity::new,
                    () -> ModBlockEntities.REFINERY),
            BlockBehaviour.Properties.of().strength(3.5f, 8.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 5));

    /** Электролизёр (US6 ISRU): лёд → топливо + кислород. */
    public static final Block ELECTROLYZER = register("electrolyzer",
            props -> new ProcessingMachineBlock(props, ElectrolyzerBlockEntity::new,
                    () -> ModBlockEntities.ELECTROLYZER),
            BlockBehaviour.Properties.of().strength(3.5f, 8.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 5));

    // --- Стартовая инфраструктура и кресло (US4, срез 2) ---
    public static final Block LAUNCH_PAD = register("launch_pad", LaunchPadBlock::new,
            BlockBehaviour.Properties.of().strength(3.0f, 10.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().noOcclusion());
    public static final Block ASSEMBLY_PYLON = register("assembly_pylon", AssemblyPylonBlock::new,
            BlockBehaviour.Properties.of().strength(3.0f, 10.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().noOcclusion());
    public static final Block ROCKET_SEAT = register("rocket_seat", RocketSeatBlock::new,
            BlockBehaviour.Properties.of().strength(2.0f, 6.0f).sound(SoundType.METAL)
                    .noOcclusion());

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
