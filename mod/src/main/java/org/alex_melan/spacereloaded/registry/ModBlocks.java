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
                    .lightLevel(state -> state.getValue(org.alex_melan.spacereloaded.machine.MachineActivity.ACTIVE) ? 6 : 0));

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
            BlockBehaviour.Properties.of().noOcclusion()
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
                    .lightLevel(state -> state.getValue(org.alex_melan.spacereloaded.machine.MachineActivity.ACTIVE) ? 8 + 4 : 0));

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
                    .lightLevel(state -> state.getValue(org.alex_melan.spacereloaded.machine.MachineActivity.ACTIVE) ? 6 + 4 : 0));
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
    public static final Block ROCKET_ENGINE = register("rocket_engine", org.alex_melan.spacereloaded.rocket.EngineBlock::new,
            BlockBehaviour.Properties.of().strength(3.5f, 10.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion());
    public static final Block COMMAND_MODULE = register("command_module",
            props -> new ShapedBlock(props, net.minecraft.world.phys.shapes.Shapes.or(Block.box(0, 0, 0, 16, 4, 16),
                    Block.box(1, 4, 1, 15, 8, 15), Block.box(2.5, 8, 2.5, 13.5, 11.5, 13.5),
                    Block.box(4, 11.5, 4, 12, 16, 12))),
            BlockBehaviour.Properties.of().strength(3.5f, 10.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().noOcclusion());
    /** Орбитальная кинетическая пушка (US7): работает только на орбите. */
    public static final Block ORBITAL_CANNON = register("orbital_cannon",
            org.alex_melan.spacereloaded.cannon.OrbitalCannonBlock::new,
            BlockBehaviour.Properties.of().strength(6.0f, 1200.0f).sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops().noOcclusion());

    /** Посадочный маяк: точка прибытия полётной программы (беспилотные рейсы). */
    public static final Block LANDING_BEACON = register("landing_beacon", Block::new,
            BlockBehaviour.Properties.of().strength(3.0f, 9.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().lightLevel(state -> 10));

    /** ЦУП: телеметрия всех бортов в радиусе (v1 — отчёт в чат, GUI позже). */
    public static final Block MISSION_CONTROL = register("mission_control",
            org.alex_melan.spacereloaded.rocket.MissionControlBlock::new,
            BlockBehaviour.Properties.of().strength(3.5f, 9.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().noOcclusion());

    /** Вентиляционная решётка: выглядит цельно, но газ проходит (тег passes_gas). */
    public static final Block VENT_GRATE = register("vent_grate", Block::new,
            BlockBehaviour.Properties.of().strength(2.5f, 6.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());

    /** Грузовой отсек ракеты: 15 слотов, хопперы и погрузчик. */
    public static final Block CARGO_HOLD = register("cargo_hold",
            org.alex_melan.spacereloaded.rocket.CargoHoldBlock::new,
            BlockBehaviour.Properties.of().strength(3.0f, 10.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());

    /** Погрузчик: автоматическая погрузка/разгрузка припаркованного борта. */
    public static final Block CARGO_LOADER = register("cargo_loader",
            org.alex_melan.spacereloaded.rocket.CargoLoaderBlock::new,
            BlockBehaviour.Properties.of().strength(3.5f, 9.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());

    /**
     * Грузовой терминал (003, FR-110): автомат челночной линии — хранит полётную
     * программу и отправляет обслуженный беспилотный борт из своей зоны.
     */
    public static final Block CARGO_TERMINAL = register("cargo_terminal",
            org.alex_melan.spacereloaded.logistics.CargoTerminalBlock::new,
            BlockBehaviour.Properties.of().strength(3.5f, 9.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());

    /** Обшивка модуля (003, FR-124): герметичная стенка из бака/корпуса — только конверсией. */
    public static final Block MODULE_HULL = register("module_hull", Block::new,
            BlockBehaviour.Properties.of().strength(3.0f, 8.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());

    /** Стыковочный порт (003, FR-120): герметичный люк с направлением и функцией стыковки-конверсии. */
    public static final org.alex_melan.spacereloaded.logistics.DockingPortBlock DOCKING_PORT =
            register("docking_port", org.alex_melan.spacereloaded.logistics.DockingPortBlock::new,
                    BlockBehaviour.Properties.of().strength(3.5f, 10.0f).sound(SoundType.METAL)
                            .requiresCorrectToolForDrops().noOcclusion());

    /** Экран телеметрии: настенная панель, светится по статусу зоны. */
    public static final Block TELEMETRY_SCREEN = register("telemetry_screen",
            org.alex_melan.spacereloaded.sealing.TelemetryScreenBlock::new,
            BlockBehaviour.Properties.of().strength(2.5f, 6.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(
                            org.alex_melan.spacereloaded.sealing.TelemetryScreenBlock.STATUS) > 0 ? 7 : 0));

    /** Атмосферный сборщик (Phase 11): сжимает CO2 из атмосферы измерения. */
    public static final Block ATMOSPHERIC_COLLECTOR = register("atmospheric_collector",
            props -> new org.alex_melan.spacereloaded.machine.ChemMachineBlock<>(props,
                    org.alex_melan.spacereloaded.machine.AtmosphericCollectorBlockEntity::new,
                    () -> ModBlockEntities.ATMOSPHERIC_COLLECTOR),
            BlockBehaviour.Properties.of().strength(3.5f, 9.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());

    /** Реактор Сабатье (Phase 11): CO2 + лёд → метанокс в соседние баки. */
    public static final Block SABATIER_REACTOR = register("sabatier_reactor",
            props -> new org.alex_melan.spacereloaded.machine.ChemMachineBlock<>(props,
                    org.alex_melan.spacereloaded.machine.SabatierReactorBlockEntity::new,
                    () -> ModBlockEntities.SABATIER_REACTOR),
            BlockBehaviour.Properties.of().strength(3.5f, 9.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(org.alex_melan.spacereloaded.machine.MachineActivity.ACTIVE) ? 7 : 0));

    /** Тарелка-перехватчик (Phase 12 CTF): уводит грузы с открытого канала на свою площадку. */
    public static final Block INTERCEPTOR_DISH = register("interceptor_dish",
            org.alex_melan.spacereloaded.network.InterceptorDishBlock::new,
            BlockBehaviour.Properties.of().strength(3.5f, 9.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());

    /** Астероидный камень (Phase 14): порода пояса астероидов, при добыче даёт металлы/лёд. */
    public static final Block ASTEROID_STONE = register("asteroid_stone", Block::new,
            BlockBehaviour.Properties.of().strength(4.0f, 4.0f).sound(SoundType.STONE)
                    .requiresCorrectToolForDrops());

    /** Энергоспутник (Phase 14): на орбите добавляет мощность для наземных ректенн. */
    public static final Block POWER_SATELLITE = register("power_satellite",
            props -> new ShapedBlock(props, Block.box(0, 0, 4, 16, 12, 12)),
            BlockBehaviour.Properties.of().strength(2.5f, 6.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().noOcclusion());

    /** Ректенна (Phase 14): принимает энергию с энергоспутников при чистом небе. */
    public static final Block RECTENNA = register("rectenna",
            props -> new org.alex_melan.spacereloaded.energy.RectennaBlock(props),
            BlockBehaviour.Properties.of().strength(3.0f, 6.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().noOcclusion());

    /** Спутник (Phase 12): полезная нагрузка ракеты; на орбите разворачивается в узел связи. */
    public static final Block SATELLITE = register("satellite",
            props -> new ShapedBlock(props, Block.box(0, 3, 4, 16, 16, 12)),
            BlockBehaviour.Properties.of().strength(2.5f, 6.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().noOcclusion());

    /** Спутник-камера (007, US5): телескоп Кассегрена D = 10 см, пушбрум-линейка 10⁴ пикселей. */
    public static final Block IMAGING_SATELLITE = register("imaging_satellite",
            props -> new ShapedBlock(props, net.minecraft.world.phys.shapes.Shapes.or(Block.box(5, 0, 5, 11, 16, 11),
                    Block.box(0, 1.5, 7, 16, 9.5, 9))),
            BlockBehaviour.Properties.of().strength(2.5f, 6.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().noOcclusion());

    /** Возвратная капсула (T075): лёгкий командный пост с теплозащитой. */
    public static final Block RETURN_CAPSULE = register("return_capsule", Block::new,
            BlockBehaviour.Properties.of().strength(3.5f, 12.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());

    /** Метанокислородный двигатель: середина по тяге и Isp, топливо ISRU-Марса. */
    public static final Block METHALOX_ENGINE = register("methalox_engine", org.alex_melan.spacereloaded.rocket.EngineBlock::new,
            BlockBehaviour.Properties.of().strength(3.5f, 10.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().noOcclusion());

    /** Стыковочный узел (US6): плоскость разделения ступеней ракеты. */
    public static final Block DOCKING_CLAMP = register("docking_clamp", Block::new,
            BlockBehaviour.Properties.of().strength(3.5f, 10.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());

    /**
     * Разделитель ступеней (Полёт 2.0, FR-060): пироболтовое кольцо, задаёт
     * плоскость отделения ступени в полёте; после отделения уходит вниз с обломком.
     */
    public static final Block STAGE_SEPARATOR = register("stage_separator", Block::new,
            BlockBehaviour.Properties.of().strength(3.0f, 10.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());

    /** Гидролоксовый двигатель: ниже тяга, выше Isp — для орбиты и Луны. */
    public static final Block HYDROLOX_ENGINE = register("hydrolox_engine", org.alex_melan.spacereloaded.rocket.EngineBlock::new,
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
    /** Лунный ильменит: главный источник титана после первого полёта. */
    public static final Block MOON_TITANIUM_ORE = register("moon_titanium_ore", Block::new,
            BlockBehaviour.Properties.of()
                    .strength(3.5f, 3.0f)
                    .requiresCorrectToolForDrops());
    /** Марсианские жилы вольфрама: снаряды и орудия дешевле не станут. */
    public static final Block MARS_TUNGSTEN_ORE = register("mars_tungsten_ore", Block::new,
            BlockBehaviour.Properties.of()
                    .strength(4.5f, 3.0f)
                    .requiresCorrectToolForDrops());

    public static final Block MOON_ICE = register("moon_ice", Block::new,
            BlockBehaviour.Properties.of().strength(1.2f).requiresCorrectToolForDrops()
                    .sound(SoundType.GLASS));

    /** Марсианский лёд (003, FR-130): подповерхностные жилы в красном песчанике — сырьё Сабатье. */
    public static final Block MARS_ICE = register("mars_ice", Block::new,
            BlockBehaviour.Properties.of().strength(1.4f).requiresCorrectToolForDrops()
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
                    .lightLevel(state -> state.getValue(org.alex_melan.spacereloaded.machine.MachineActivity.ACTIVE) ? 5 + 4 : 0));

    /** Электролизёр (US6 ISRU): лёд → топливо + кислород. */
    public static final Block ELECTROLYZER = register("electrolyzer",
            props -> new ProcessingMachineBlock(props, ElectrolyzerBlockEntity::new,
                    () -> ModBlockEntities.ELECTROLYZER),
            BlockBehaviour.Properties.of().strength(3.5f, 8.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(org.alex_melan.spacereloaded.machine.MachineActivity.ACTIVE) ? 5 + 4 : 0));

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

    // --- Лунная индустрия (004) ---
    /** Казённик электромагнитной катапульты (FR-200). */
    public static final Block MASS_DRIVER_BREECH = register("mass_driver_breech",
            org.alex_melan.spacereloaded.industry.MassDriverBreechBlock::new,
            BlockBehaviour.Properties.of().strength(5.0f, 12.0f).sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops());
    /** Стальная катушка — секция рельса тира 1. */
    public static final Block STEEL_COIL = register("steel_coil",
            props -> new org.alex_melan.spacereloaded.industry.CoilBlock(props, 1),
            BlockBehaviour.Properties.of().strength(4.0f, 10.0f).sound(SoundType.COPPER)
                    .requiresCorrectToolForDrops().noOcclusion());
    /** Сверхпроводящая катушка — секция рельса тира 2. */
    public static final Block SUPERCONDUCTING_COIL = register("superconducting_coil",
            props -> new org.alex_melan.spacereloaded.industry.CoilBlock(props, 2),
            BlockBehaviour.Properties.of().strength(4.0f, 10.0f).sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops().noOcclusion());
    /** Конденсатор батареи катапульты (FR-202). */
    public static final Block CAPACITOR = register("capacitor",
            org.alex_melan.spacereloaded.industry.CapacitorBlock::new,
            BlockBehaviour.Properties.of().strength(3.5f, 8.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());
    /** Салазки рельса — только модель для рендера анимации (без предмета). */
    public static final Block MASS_DRIVER_SLED = registerNoItem("mass_driver_sled", Block::new,
            BlockBehaviour.Properties.of().strength(1.0f).noOcclusion().noLootTable());
    /** Ловушка масс (FR-220). */
    public static final Block MASS_CATCHER = register("mass_catcher",
            org.alex_melan.spacereloaded.industry.MassCatcherBlock::new,
            BlockBehaviour.Properties.of().strength(4.0f, 10.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());
    /** Секция сетки-уловителя. */
    public static final Block CATCHER_NET = register("catcher_net", Block::new,
            BlockBehaviour.Properties.of().strength(1.5f, 4.0f).sound(SoundType.CHAIN).noOcclusion());
    /** Контроллер реголитового реактора (FR-230): светится окном, пока идёт электролиз. */
    public static final Block REGOLITH_REACTOR = register("regolith_reactor",
            org.alex_melan.spacereloaded.industry.RegolithReactorBlock::new,
            BlockBehaviour.Properties.of().strength(5.0f, 12.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(
                            org.alex_melan.spacereloaded.industry.RegolithReactorBlock.LIT) ? 13 : 0));
    /** Огнеупорная футеровка оболочки реактора. */
    public static final Block REFRACTORY_LINING = register("refractory_lining", org.alex_melan.spacereloaded.multiblock.FormableBlock::new,
            BlockBehaviour.Properties.of().strength(4.0f, 30.0f).sound(SoundType.DEEPSLATE_BRICKS)
                    .requiresCorrectToolForDrops());
    /**
     * Спечённый реголит (FR-235): плотный строительный блок обваловки; взрывостойкость 40 —
     * между камнем (6) и обсидианом (1200), ниже порога выживания метеорита — защищает толщиной.
     */
    public static final Block SINTERED_REGOLITH = register("sintered_regolith", Block::new,
            BlockBehaviour.Properties.of().strength(3.0f, 40.0f).sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops());
    /** Лунный кирпич (FR-250): строительный блок из лунного камня. */
    public static final Block LUNAR_BRICKS = register("lunar_bricks", Block::new,
            BlockBehaviour.Properties.of().strength(2.0f, 6.0f).sound(SoundType.STONE)
                    .requiresCorrectToolForDrops());

    // --- Инженерия (005): трансмиссия и станки ---
    public static final Block WOODEN_SHAFT = register("wooden_shaft",
            props -> new org.alex_melan.spacereloaded.kinetics.KineticAxisBlock(props, org.alex_melan.spacereloaded.kinetics.KineticBlock.Kind.SHAFT, 0.2,
                    org.alex_melan.spacereloaded.kinetics.KineticBlock.Material.WOOD, true, 0.125, org.alex_melan.spacereloaded.kinetics.KineticBlockEntity::new,
                    () -> ModBlockEntities.KINETIC),
            BlockBehaviour.Properties.of().strength(1.5f, 3.0f).sound(SoundType.WOOD).noOcclusion());
    public static final Block STEEL_SHAFT = register("steel_shaft",
            props -> new org.alex_melan.spacereloaded.kinetics.KineticAxisBlock(props, org.alex_melan.spacereloaded.kinetics.KineticBlock.Kind.SHAFT, 3.0,
                    org.alex_melan.spacereloaded.kinetics.KineticBlock.Material.STEEL, true, 0.125, org.alex_melan.spacereloaded.kinetics.KineticBlockEntity::new,
                    () -> ModBlockEntities.KINETIC),
            BlockBehaviour.Properties.of().strength(3.0f, 8.0f).sound(SoundType.METAL).noOcclusion()
                    .requiresCorrectToolForDrops());
    public static final Block SMALL_GEAR = register("small_gear",
            props -> new org.alex_melan.spacereloaded.kinetics.KineticAxisBlock(props, org.alex_melan.spacereloaded.kinetics.KineticBlock.Kind.SMALL_GEAR, 10.0,
                    org.alex_melan.spacereloaded.kinetics.KineticBlock.Material.NONE, true, 0.5, org.alex_melan.spacereloaded.kinetics.KineticBlockEntity::new,
                    () -> ModBlockEntities.KINETIC),
            BlockBehaviour.Properties.of().strength(3.0f, 8.0f).sound(SoundType.METAL).noOcclusion()
                    .requiresCorrectToolForDrops());
    public static final Block LARGE_GEAR = register("large_gear",
            props -> new org.alex_melan.spacereloaded.kinetics.KineticAxisBlock(props, org.alex_melan.spacereloaded.kinetics.KineticBlock.Kind.LARGE_GEAR, 40.0,
                    org.alex_melan.spacereloaded.kinetics.KineticBlock.Material.NONE, true, 0.5, org.alex_melan.spacereloaded.kinetics.KineticBlockEntity::new,
                    () -> ModBlockEntities.KINETIC),
            BlockBehaviour.Properties.of().strength(3.0f, 8.0f).sound(SoundType.METAL).noOcclusion()
                    .requiresCorrectToolForDrops());
    public static final Block GEARBOX = register("gearbox", org.alex_melan.spacereloaded.kinetics.GearboxBlock::new,
            BlockBehaviour.Properties.of().strength(3.5f, 8.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final Block CLUTCH = register("clutch", org.alex_melan.spacereloaded.kinetics.ClutchBlock::new,
            BlockBehaviour.Properties.of().strength(3.5f, 8.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final Block MOTOR = register("motor",
            props -> new org.alex_melan.spacereloaded.kinetics.KineticAxisBlock(props, org.alex_melan.spacereloaded.kinetics.KineticBlock.Kind.MOTOR, 5.0,
                    org.alex_melan.spacereloaded.kinetics.KineticBlock.Material.STEEL, false, 0.5, org.alex_melan.spacereloaded.kinetics.MotorBlockEntity::new,
                    () -> ModBlockEntities.MOTOR),
            BlockBehaviour.Properties.of().strength(4.0f, 10.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final Block FLYWHEEL = register("flywheel",
            props -> new org.alex_melan.spacereloaded.kinetics.KineticAxisBlock(props, org.alex_melan.spacereloaded.kinetics.KineticBlock.Kind.FLYWHEEL, 385.0,
                    org.alex_melan.spacereloaded.kinetics.KineticBlock.Material.STEEL, true, 0.5, org.alex_melan.spacereloaded.kinetics.FlywheelBlockEntity::new,
                    () -> ModBlockEntities.FLYWHEEL),
            BlockBehaviour.Properties.of().strength(5.0f, 12.0f).sound(SoundType.NETHERITE_BLOCK).noOcclusion()
                    .requiresCorrectToolForDrops());
    public static final Block MECHANICAL_PRESS = register("mechanical_press",
            props -> new org.alex_melan.spacereloaded.kinetics.KineticMachineBlock(props, org.alex_melan.spacereloaded.kinetics.KineticBlock.Kind.PRESS, 20.0,
                    org.alex_melan.spacereloaded.kinetics.PressBlockEntity::new, () -> ModBlockEntities.PRESS),
            BlockBehaviour.Properties.of().strength(4.0f, 10.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final Block LATHE = register("lathe",
            props -> new org.alex_melan.spacereloaded.kinetics.KineticMachineBlock(props, org.alex_melan.spacereloaded.kinetics.KineticBlock.Kind.LATHE, 5.0,
                    org.alex_melan.spacereloaded.kinetics.LatheBlockEntity::new, () -> ModBlockEntities.LATHE),
            BlockBehaviour.Properties.of().strength(4.0f, 10.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    /** Ступица ветроколеса (005, FR-340) и парус. */
    public static final Block WIND_HUB = register("wind_hub",
            props -> new org.alex_melan.spacereloaded.kinetics.KineticAxisBlock(props, org.alex_melan.spacereloaded.kinetics.KineticBlock.Kind.WIND_HUB, 20.0,
                    org.alex_melan.spacereloaded.kinetics.KineticBlock.Material.STEEL, false, 0.5, org.alex_melan.spacereloaded.kinetics.WindHubBlockEntity::new,
                    () -> ModBlockEntities.WIND_HUB),
            BlockBehaviour.Properties.of().strength(3.0f, 8.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final Block SAIL = register("sail", Block::new,
            BlockBehaviour.Properties.of().strength(0.8f, 1.0f).sound(SoundType.WOOL).noOcclusion());
    /** Электролизная ячейка стека (005, FR-322). */
    public static final Block ELECTROLYSIS_CELL = register("electrolysis_cell", org.alex_melan.spacereloaded.multiblock.FormableBlock::new,
            BlockBehaviour.Properties.of().strength(3.0f, 8.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    /** Ректификационная тарелка колонны (005, FR-323). */
    public static final Block DISTILLATION_TRAY = register("distillation_tray",
            props -> new org.alex_melan.spacereloaded.multiblock.FormableBlock(props, net.minecraft.world.phys.shapes.Shapes.box(0.125, 0, 0.125, 0.875, 1, 0.875)),
            BlockBehaviour.Properties.of().strength(3.0f, 8.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .noOcclusion());
    /** Модели вращающихся частей — только для BER (без предметов). */
    public static final Block ROTOR_STEEL_SHAFT = registerNoItem("rotor_steel_shaft", Block::new,
            BlockBehaviour.Properties.of().noOcclusion().noLootTable());
    public static final Block ROTOR_WOODEN_SHAFT = registerNoItem("rotor_wooden_shaft", Block::new,
            BlockBehaviour.Properties.of().noOcclusion().noLootTable());
    public static final Block ROTOR_SMALL_GEAR = registerNoItem("rotor_small_gear", Block::new,
            BlockBehaviour.Properties.of().noOcclusion().noLootTable());
    public static final Block ROTOR_LARGE_GEAR = registerNoItem("rotor_large_gear", Block::new,
            BlockBehaviour.Properties.of().noOcclusion().noLootTable());
    public static final Block ROTOR_FLYWHEEL = registerNoItem("rotor_flywheel", Block::new,
            BlockBehaviour.Properties.of().noOcclusion().noLootTable());
    public static final Block ROTOR_SHAFT_STUB = registerNoItem("rotor_shaft_stub", Block::new,
            BlockBehaviour.Properties.of().noOcclusion().noLootTable());
    public static final Block ROTOR_LATHE_CHUCK = registerNoItem("rotor_lathe_chuck", Block::new,
            BlockBehaviour.Properties.of().noOcclusion().noLootTable());
    public static final Block ROTOR_WIND_HUB = registerNoItem("rotor_wind_hub", Block::new,
            BlockBehaviour.Properties.of().noOcclusion().noLootTable());
    public static final Block ROTOR_SOLAR_ARRAY = registerNoItem("rotor_solar_array", Block::new,
            BlockBehaviour.Properties.of().noOcclusion().noLootTable());
    public static final Block ROTOR_MONO_SOLAR_ARRAY = registerNoItem("rotor_mono_solar_array", Block::new,
            BlockBehaviour.Properties.of().noOcclusion().noLootTable());
    public static final Block ROTOR_PRESS_RAM = registerNoItem("rotor_press_ram", Block::new,
            BlockBehaviour.Properties.of().noOcclusion().noLootTable());

    // --- Материалы и электроника (006) ---
    public static final Block CHEMICAL_REACTOR = process("chemical_reactor",
            org.alex_melan.spacereloaded.electronics.ChemicalReactorBlockEntity::new, () -> ModBlockEntities.CHEMICAL_REACTOR, 9);
    public static final Block DEPOSITION_REACTOR = process("deposition_reactor",
            org.alex_melan.spacereloaded.electronics.DepositionReactorBlockEntity::new, () -> ModBlockEntities.DEPOSITION_REACTOR, 12);
    public static final Block DIFFUSION_FURNACE = process("diffusion_furnace",
            org.alex_melan.spacereloaded.electronics.DiffusionFurnaceBlockEntity::new, () -> ModBlockEntities.DIFFUSION_FURNACE, 11);
    public static final Block ETCH_BATH = process("etch_bath",
            org.alex_melan.spacereloaded.electronics.EtchBathBlockEntity::new, () -> ModBlockEntities.ETCH_BATH, 4);
    public static final Block CRYSTAL_PULLER = register("crystal_puller",
            props -> new org.alex_melan.spacereloaded.kinetics.KineticMachineBlock(props, org.alex_melan.spacereloaded.kinetics.KineticBlock.Kind.LATHE, 30.0,
                    org.alex_melan.spacereloaded.electronics.CrystalPullerBlockEntity::new, () -> ModBlockEntities.CRYSTAL_PULLER),
            BlockBehaviour.Properties.of().strength(4.0f, 10.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final Block WAFER_SAW = register("wafer_saw",
            props -> new org.alex_melan.spacereloaded.kinetics.KineticMachineBlock(props, org.alex_melan.spacereloaded.kinetics.KineticBlock.Kind.LATHE, 5.0,
                    org.alex_melan.spacereloaded.electronics.WaferSawBlockEntity::new, () -> ModBlockEntities.WAFER_SAW),
            BlockBehaviour.Properties.of().strength(4.0f, 10.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    /** Фильтровентиляционный модуль (HEPA): учитывается чистой комнатой, в стене которой стоит. */
    public static final Block FAN_FILTER_UNIT = register("fan_filter_unit", Block::new,
            BlockBehaviour.Properties.of().strength(3.0f, 8.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> 6));
    public static final Block LITHOGRAPHY_STATION = process("lithography_station",
            org.alex_melan.spacereloaded.electronics.LithographyStationBlockEntity::new, () -> ModBlockEntities.LITHOGRAPHY_STATION, 7);
    public static final Block HALITE_ORE = register("halite_ore", Block::new,
            BlockBehaviour.Properties.of().strength(2.5f, 3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final Block FLUORITE_ORE = register("fluorite_ore", Block::new,
            BlockBehaviour.Properties.of().strength(3.0f, 3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final Block SPODUMENE_ORE = register("spodumene_ore", Block::new,
            BlockBehaviour.Properties.of().strength(3.5f, 3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
    /** Анортозит лунных нагорий (CaAl₂Si₂O₈) — сырьё алюминия и кремния в реголитовом реакторе. */
    public static final Block ANORTHOSITE = register("anorthosite", Block::new,
            BlockBehaviour.Properties.of().strength(2.5f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
    /** Баки из алюмомедного и алюминий-литиевого сплавов (масса по материалу — part_properties). */
    public static final Block ALUMINIUM_FUEL_TANK = register("aluminium_fuel_tank",
            org.alex_melan.spacereloaded.rocket.FuelTankBlock::new,
            BlockBehaviour.Properties.of().strength(3.0f, 10.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final Block AL_LI_FUEL_TANK = register("al_li_fuel_tank",
            org.alex_melan.spacereloaded.rocket.FuelTankBlock::new,
            BlockBehaviour.Properties.of().strength(3.0f, 10.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    /** Монокристаллическая солнечная панель: КПД 21 % против 15 % мультикремния → ×1.4. */
    public static final Block MONO_SOLAR_PANEL = register("mono_solar_panel",
            props -> new MachineBlock<>(props, SolarPanelBlockEntity::new,
                    () -> ModBlockEntities.SOLAR_PANEL, SolarPanelBlockEntity::serverTick),
            BlockBehaviour.Properties.of().strength(2.5f, 6.0f).sound(SoundType.METAL).noOcclusion()
                    .requiresCorrectToolForDrops());

    /** Процессная машина 006 (меню, lit, свет в работе). */
    private static Block process(String name,
                                 java.util.function.BiFunction<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState,
                                         ? extends org.alex_melan.spacereloaded.electronics.ProcessMachineBlockEntity> factory,
                                 java.util.function.Supplier<net.minecraft.world.level.block.entity.BlockEntityType<
                                         ? extends org.alex_melan.spacereloaded.electronics.ProcessMachineBlockEntity>> type,
                                 int light) {
        return register(name, props -> new org.alex_melan.spacereloaded.electronics.ProcessBlock(props, factory, type),
                BlockBehaviour.Properties.of().strength(3.5f, 9.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                        .noOcclusion()
                        .lightLevel(state -> state.getValue(org.alex_melan.spacereloaded.machine.MachineActivity.ACTIVE) ? light : 0));
    }

    // --- Тяжёлая индустрия (008): стойка жизнеобеспечения ---
    public static final Block ECLSS_CONTROLLER = register("eclss_controller",
            props -> new org.alex_melan.spacereloaded.multiblock.ControllerBlock<>(props,
                    org.alex_melan.spacereloaded.eclss.EclssControllerBlockEntity::new, () -> ModBlockEntities.ECLSS_CONTROLLER,
                    org.alex_melan.spacereloaded.eclss.EclssControllerBlockEntity::serverTick),
            industrial().lightLevel(s -> s.getValue(org.alex_melan.spacereloaded.multiblock.ControllerBlock.ACTIVE) ? 6 : 0));
    public static final Block ECLSS_RACK_FRAME = register("eclss_rack_frame",
            org.alex_melan.spacereloaded.multiblock.FormableBlock::new, industrial());
    public static final Block OGS_MODULE = module("ogs_module");
    public static final Block SABATIER_MODULE = module("sabatier_module");
    public static final Block CDRA_MODULE = module("cdra_module");
    public static final Block WRS_MODULE = module("wrs_module");
    public static final Block ECLSS_BLANK_PANEL = module("eclss_blank_panel");
    public static final Block ECLSS_FAN = registerNoItem("eclss_fan", Block::new,
            BlockBehaviour.Properties.of().noOcclusion().noLootTable());
    public static final Block ECLSS_PISTON = registerNoItem("eclss_piston", Block::new,
            BlockBehaviour.Properties.of().noOcclusion().noLootTable());

    // --- 008: реактор деления Kilopower ---
    public static final Block CONTROL_ROD_DRIVE = register("control_rod_drive",
            props -> new org.alex_melan.spacereloaded.multiblock.ControllerBlock<>(props,
                    org.alex_melan.spacereloaded.nuclear.ReactorBlockEntity::new, () -> ModBlockEntities.REACTOR,
                    org.alex_melan.spacereloaded.nuclear.ReactorBlockEntity::serverTick),
            industrial().noOcclusion().lightLevel(s -> s.getValue(org.alex_melan.spacereloaded.multiblock.ControllerBlock.ACTIVE) ? 5 : 0));
    public static final Block REACTOR_CORE = register("reactor_core",
            org.alex_melan.spacereloaded.multiblock.FormableBlock::new, industrial().strength(8f, 30f));
    public static final Block BEO_REFLECTOR = register("beo_reflector",
            org.alex_melan.spacereloaded.multiblock.FormableBlock::new, industrial());
    public static final Block HEAT_PIPE = register("heat_pipe",
            props -> new org.alex_melan.spacereloaded.multiblock.FormableBlock(props, Block.box(5, 0, 5, 11, 16, 11)),
            industrial().noOcclusion());
    public static final Block STIRLING_CONVERTOR = register("stirling_convertor",
            props -> new org.alex_melan.spacereloaded.multiblock.FormableBlock(props, Block.box(3, 0, 3, 13, 13, 13)),
            industrial().noOcclusion());
    public static final Block REACTOR_POWER_CAP = register("reactor_power_cap",
            org.alex_melan.spacereloaded.multiblock.FormableBlock::new, industrial());
    public static final Block RADIATOR_PANEL = register("radiator_panel",
            org.alex_melan.spacereloaded.multiblock.FormableBlock::new, industrial().noOcclusion());
    public static final Block CONTROL_ROD = registerNoItem("control_rod", Block::new,
            BlockBehaviour.Properties.of().noOcclusion().noLootTable());
    public static final Block STIRLING_PISTON = registerNoItem("stirling_piston", Block::new,
            BlockBehaviour.Properties.of().noOcclusion().noLootTable());
    // --- 008: каскад газовых центрифуг ---
    public static final Block CASCADE_CONTROLLER = register("cascade_controller",
            props -> new org.alex_melan.spacereloaded.multiblock.ControllerBlock<>(props,
                    org.alex_melan.spacereloaded.nuclear.CascadeBlockEntity::new, () -> ModBlockEntities.CASCADE,
                    org.alex_melan.spacereloaded.nuclear.CascadeBlockEntity::serverTick),
            industrial().lightLevel(s -> s.getValue(org.alex_melan.spacereloaded.multiblock.ControllerBlock.ACTIVE) ? 5 : 0));
    public static final Block GAS_CENTRIFUGE = register("gas_centrifuge",
            props -> new org.alex_melan.spacereloaded.multiblock.FormableBlock(props, Block.box(2, 0, 2, 14, 16, 14)),
            industrial().noOcclusion());
    public static final Block CENTRIFUGE_ROTOR = registerNoItem("centrifuge_rotor", Block::new,
            BlockBehaviour.Properties.of().noOcclusion().noLootTable());
    // --- 008: воздухоразделительная колонна ---
    public static final Block ASU_SUMP = register("asu_sump",
            props -> new org.alex_melan.spacereloaded.multiblock.ControllerBlock<>(props,
                    org.alex_melan.spacereloaded.cryo.AirColumnBlockEntity::new, () -> ModBlockEntities.ASU,
                    org.alex_melan.spacereloaded.cryo.AirColumnBlockEntity::serverTick),
            industrial().noOcclusion());
    public static final Block ASU_TRAY = register("asu_tray",
            props -> new org.alex_melan.spacereloaded.multiblock.FormableBlock(props, Block.box(1.5, 0, 1.5, 14.5, 16, 14.5)),
            industrial().noOcclusion());
    public static final Block ASU_HEAT_EXCHANGER = register("asu_heat_exchanger",
            props -> new org.alex_melan.spacereloaded.multiblock.FormableBlock(props, Block.box(1, 0, 1, 15, 13, 15)),
            industrial().noOcclusion());
    public static final Block ASU_COMPRESSOR = register("asu_compressor",
            props -> new org.alex_melan.spacereloaded.kinetics.KineticMachineBlock(props,
                    org.alex_melan.spacereloaded.kinetics.KineticBlock.Kind.COMPRESSOR, 3.0,
                    org.alex_melan.spacereloaded.cryo.AsuCompressorBlockEntity::new, () -> ModBlockEntities.ASU_COMPRESSOR),
            industrial());
    public static final Block ASU_EXPANDER_WHEEL = registerNoItem("asu_expander_wheel", Block::new,
            BlockBehaviour.Properties.of().noOcclusion().noLootTable());
    public static final Block ASU_COLUMN_CAP = registerNoItem("asu_column_cap", Block::new,
            BlockBehaviour.Properties.of().noOcclusion().noLootTable());
    public static final Block BERYL_ORE = register("beryl_ore", Block::new,
            BlockBehaviour.Properties.of().strength(3.5f, 3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final Block BORAX_ORE = register("borax_ore", Block::new,
            BlockBehaviour.Properties.of().strength(1.5f, 3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final Block URANINITE_ORE = register("uraninite_ore", Block::new,
            BlockBehaviour.Properties.of().strength(4.5f, 3.0f).sound(SoundType.DEEPSLATE).requiresCorrectToolForDrops());

    private static BlockBehaviour.Properties industrial() {
        return BlockBehaviour.Properties.of().strength(3.5f, 9.0f).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    private static Block module(String name) {
        return register(name, org.alex_melan.spacereloaded.multiblock.FacingFormableBlock::new, industrial().noOcclusion());
    }

    // --- Жизнь на станции (007) ---
    public static final Block AIR_SEPARATOR = register("air_separator",
            props -> new MachineBlock<>(props, org.alex_melan.spacereloaded.lifesupport.AirSeparatorBlockEntity::new,
                    () -> ModBlockEntities.AIR_SEPARATOR, org.alex_melan.spacereloaded.lifesupport.AirSeparatorBlockEntity::serverTick),
            BlockBehaviour.Properties.of().strength(3.5f, 9.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final Block AIRLOCK_PUMP = register("airlock_pump",
            props -> new MachineBlock<>(props, org.alex_melan.spacereloaded.lifesupport.AirlockPumpBlockEntity::new,
                    () -> ModBlockEntities.AIRLOCK_PUMP, org.alex_melan.spacereloaded.lifesupport.AirlockPumpBlockEntity::serverTick),
            BlockBehaviour.Properties.of().strength(3.5f, 9.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final Block CO2_SCRUBBER = process("co2_scrubber",
            org.alex_melan.spacereloaded.lifesupport.Co2ScrubberBlockEntity::new, () -> ModBlockEntities.CO2_SCRUBBER, 3);
    public static final Block HYDROPONIC_TRAY = register("hydroponic_tray", org.alex_melan.spacereloaded.lifesupport.HydroponicTrayBlock::new,
            BlockBehaviour.Properties.of().strength(2.0f, 6.0f).sound(SoundType.METAL).noOcclusion());
    public static final Block GROW_LAMP = register("grow_lamp", org.alex_melan.spacereloaded.lifesupport.GrowLampBlock::new,
            BlockBehaviour.Properties.of().strength(1.5f, 4.0f).sound(SoundType.GLASS).noOcclusion()
                    .lightLevel(state -> state.getValue(org.alex_melan.spacereloaded.lifesupport.GrowLampBlock.LIT) ? 14 : 0));
    public static final Block BIOMASS_OXIDIZER = process("biomass_oxidizer",
            org.alex_melan.spacereloaded.lifesupport.BiomassOxidizerBlockEntity::new, () -> ModBlockEntities.BIOMASS_OXIDIZER, 8);
    public static final Block SPIN_HUB = register("spin_hub", org.alex_melan.spacereloaded.station.SpinHubBlock::new,
            BlockBehaviour.Properties.of().strength(5.0f, 12.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final Block RIM_THRUSTER = register("rim_thruster", org.alex_melan.spacereloaded.station.RimThrusterBlock::new,
            BlockBehaviour.Properties.of().strength(3.0f, 8.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final Block DESPIN_MOTOR = register("despin_motor", org.alex_melan.spacereloaded.station.DespinMotorBlock::new,
            BlockBehaviour.Properties.of().strength(4.0f, 10.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final Block ROVER_CHARGER = register("rover_charger",
            props -> new MachineBlock<>(props, org.alex_melan.spacereloaded.vehicle.RoverChargerBlockEntity::new,
                    () -> ModBlockEntities.ROVER_CHARGER, org.alex_melan.spacereloaded.vehicle.RoverChargerBlockEntity::serverTick),
            BlockBehaviour.Properties.of().strength(3.0f, 8.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    /** Модели частей ровера для рендера (без предметов). */
    public static final Block ROVER_BODY_MODEL = registerNoItem("rover_body_model", Block::new,
            BlockBehaviour.Properties.of().noOcclusion().noLootTable());
    public static final Block ROVER_WHEEL_MODEL = registerNoItem("rover_wheel_model", Block::new,
            BlockBehaviour.Properties.of().noOcclusion().noLootTable());
    public static final Block ROVER_BATTERY_MODEL = registerNoItem("rover_battery_model", Block::new,
            BlockBehaviour.Properties.of().noOcclusion().noLootTable());
    public static final Block GAS_TANK = register("gas_tank", org.alex_melan.spacereloaded.lifesupport.GasTankBlock::new,
            BlockBehaviour.Properties.of().strength(3.0f, 10.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());

    private static <T extends Block> T registerNoItem(String name,
                                                      Function<BlockBehaviour.Properties, T> factory,
                                                      BlockBehaviour.Properties properties) {
        Identifier id = Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        return Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(properties.setId(blockKey)));
    }

    /**
     * Регистрация блока + BlockItem по контракту 26.2: id задаётся заранее
     * через Properties.setId (реестры BlockIds/ItemIds разделены).
     */
    private static <T extends Block> T register(String name,
                                                Function<BlockBehaviour.Properties, T> factory,
                                                BlockBehaviour.Properties properties) {
        Identifier id = Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        // цвет на картах и орбитальных снимках — «вид сверху» модели (tools/gen_map_colors.py)
        properties.mapColor(ModMapColors.of(name));
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
