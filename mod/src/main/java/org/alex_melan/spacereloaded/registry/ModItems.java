package org.alex_melan.spacereloaded.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.food.FoodProperties;
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

    private static final ResourceKey<EquipmentAsset> SPACE_SUIT_ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID,
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "space_suit"));

    /** Скафандр (EVA): полный сет (грудь+ноги+ботинки) + маска — защита от среды. */
    public static final Item SPACE_SUIT_CHESTPLATE = register("space_suit_chestplate",
            properties -> new Item(properties.stacksTo(1)
                    .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST)
                            .setAsset(SPACE_SUIT_ASSET).build())));
    public static final Item SPACE_SUIT_LEGGINGS = register("space_suit_leggings",
            properties -> new Item(properties.stacksTo(1)
                    .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.LEGS)
                            .setAsset(SPACE_SUIT_ASSET).build())));
    public static final Item SPACE_SUIT_BOOTS = register("space_suit_boots",
            properties -> new Item(properties.stacksTo(1)
                    .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.FEET)
                            .setAsset(SPACE_SUIT_ASSET).build())));

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

    /** Ключ связи (Phase 12 CTF): частота-канал для защиты маяков и перехвата. */
    public static final Item FREQUENCY_KEY = register("frequency_key",
            properties -> new org.alex_melan.spacereloaded.network.FrequencyKeyItem(properties.stacksTo(1)));

    /** Полётная программа: носитель маршрута для беспилотных рейсов. */
    public static final Item FLIGHT_PROGRAM = register("flight_program",
            properties -> new org.alex_melan.spacereloaded.rocket.FlightProgramItem(
                    properties.stacksTo(1)));

    /** Сжатый CO2 (Phase 11): собирается из атмосферы, сырьё реактора Сабатье. */
    public static final Item CARBON_DIOXIDE = simple("carbon_dioxide");

    /** Метеоритное железо (US: метеориты) — падает в кратере, ISRU-ниша железа. */
    public static final Item METEORIC_IRON = simple("meteoric_iron");

    /** Пустая консервная банка — тара для рационов (возвращается после еды). */
    public static final Item EMPTY_CAN = simple("empty_can");

    /** Консервированный рацион: сытная еда для дальних миссий, оставляет банку. */
    public static final Item CANNED_RATION = register("canned_ration", properties -> new Item(properties
            .stacksTo(16)
            .food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.9f).build())
            .component(DataComponents.USE_REMAINDER, new UseRemainder(new ItemStackTemplate(EMPTY_CAN)))));

    /** Сканер утечек (обвязка герметичности): указывает на пробой в зоне. */
    public static final Item LEAK_SCANNER = register("leak_scanner",
            properties -> new org.alex_melan.spacereloaded.sealing.LeakScannerItem(
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
    /** Абляционный экран возвратной капсулы: только из метеоритного железа. */
    public static final Item HEAT_SHIELD = simple("heat_shield");
    /** Грузовая капсула катапульты (004, FR-213): многоразовая, возвращается ловушкой масс. */
    public static final Item CARGO_POD = register("cargo_pod", props -> new Item(props.stacksTo(16)));
    /** Шлак реголитового реактора (004): спекается в спечённый реголит. */
    public static final Item SLAG = simple("slag");

    // --- Инженерия (005) ---
    public static final Item STEEL_PLATE = simple("steel_plate");
    public static final Item COPPER_PLATE = simple("copper_plate");
    public static final Item TITANIUM_ALLOY_PLATE = simple("titanium_alloy_plate");
    /** Полуфабрикаты деталей двигателя (шаг и Σδ² — компоненты). */
    public static final Item INCOMPLETE_TURBOPUMP = register("incomplete_turbopump",
            props -> new org.alex_melan.spacereloaded.machine.PartItem(props.stacksTo(1)));
    public static final Item INCOMPLETE_INJECTOR = register("incomplete_injector",
            props -> new org.alex_melan.spacereloaded.machine.PartItem(props.stacksTo(1)));
    public static final Item INCOMPLETE_NOZZLE = register("incomplete_nozzle",
            props -> new org.alex_melan.spacereloaded.machine.PartItem(props.stacksTo(1)));
    /** Детали двигателя (качество — компонент). */
    public static final Item TURBOPUMP = register("turbopump",
            props -> new org.alex_melan.spacereloaded.machine.PartItem(props.stacksTo(1)));
    public static final Item INJECTOR_PLATE = register("injector_plate",
            props -> new org.alex_melan.spacereloaded.machine.PartItem(props.stacksTo(1)));
    public static final Item REGEN_NOZZLE = register("regen_nozzle",
            props -> new org.alex_melan.spacereloaded.machine.PartItem(props.stacksTo(1)));

    /** Инженерный молот и руководство инженера (005). */
    public static final Item ENGINEER_HAMMER = register("engineer_hammer",
            props -> new org.alex_melan.spacereloaded.multiblock.EngineerHammerItem(props.stacksTo(1)));
    public static final Item ENGINEER_MANUAL = register("engineer_manual",
            props -> new org.alex_melan.spacereloaded.multiblock.EngineerManualItem(props.stacksTo(1)));

    // --- Материалы и электроника (006) ---
    public static final Item COPPER_WIRE = simple("copper_wire");
    public static final Item RELAY = simple("relay");
    public static final Item RELAY_LOGIC = simple("relay_logic");
    public static final Item FERRITE_CORE = simple("ferrite_core");
    public static final Item CORE_ROPE_MEMORY = simple("core_rope_memory");
    public static final Item SILICON_BLEND = register("silicon_blend", org.alex_melan.spacereloaded.electronics.MaterialItem::new);
    public static final Item METALLURGICAL_SILICON = register("metallurgical_silicon", org.alex_melan.spacereloaded.electronics.MaterialItem::new);
    public static final Item SILICON_DUST = register("silicon_dust", org.alex_melan.spacereloaded.electronics.MaterialItem::new);
    public static final Item CARBON_MONOXIDE = simple("carbon_monoxide");
    public static final Item BRINE = simple("brine");
    public static final Item HYDROGEN_CHLORIDE = simple("hydrogen_chloride");
    public static final Item CAUSTIC_SODA = simple("caustic_soda");
    public static final Item TRICHLOROSILANE = register("trichlorosilane", org.alex_melan.spacereloaded.electronics.MaterialItem::new);
    public static final Item POLYSILICON = register("polysilicon", org.alex_melan.spacereloaded.electronics.MaterialItem::new);
    public static final Item SILICON_BOULE = register("silicon_boule", org.alex_melan.spacereloaded.electronics.MaterialItem::new);
    /** Мультикремниевая пластина: после диффузии фосфора — солнечный элемент. */
    public static final Item MULTICRYSTALLINE_WAFER = register("multicrystalline_wafer", props -> new org.alex_melan.spacereloaded.electronics.MaterialItem(props.stacksTo(16)));
    public static final Item MULTICRYSTALLINE_SILICON = register("multicrystalline_silicon", org.alex_melan.spacereloaded.electronics.MaterialItem::new);
    public static final Item SILICON_WAFER = register("silicon_wafer", props -> new org.alex_melan.spacereloaded.electronics.MaterialItem(props.stacksTo(16)));
    public static final Item SOLAR_CELL = simple("solar_cell");
    public static final Item MONO_SOLAR_CELL = simple("mono_solar_cell");
    public static final Item ROCK_SALT = simple("rock_salt");
    public static final Item PHOTORESIST = simple("photoresist");
    public static final Item CERAMIC_PACKAGE = simple("ceramic_package");
    public static final Item DIE_LOGIC = simple("die_logic");
    public static final Item DIE_MICROPROCESSOR = simple("die_microprocessor");
    public static final Item DIE_RADHARD = simple("die_radhard");
    public static final Item LOGIC_CHIP = simple("logic_chip");
    public static final Item MICROPROCESSOR = simple("microprocessor");
    public static final Item RADHARD_PROCESSOR = simple("radhard_processor");
    public static final Item HYDROFLUORIC_ACID = simple("hydrofluoric_acid");
    public static final Item GLASS_FIBER = simple("glass_fiber");
    public static final Item EPOXY_RESIN = simple("epoxy_resin");
    public static final Item FR4_LAMINATE = simple("fr4_laminate");
    public static final Item COPPER_CLAD_LAMINATE = simple("copper_clad_laminate");
    public static final Item INCOMPLETE_CIRCUIT_BOARD = register("incomplete_circuit_board", props -> new org.alex_melan.spacereloaded.machine.PartItem(props.stacksTo(16)));
    public static final Item CIRCUIT_BOARD = register("circuit_board", props -> new org.alex_melan.spacereloaded.machine.PartItem(props.stacksTo(16)));
    public static final Item FLIGHT_COMPUTER = register("flight_computer", props -> new Item(props.stacksTo(16)));
    public static final Item FLUORITE = simple("fluorite");
    public static final Item CRYOLITE = simple("cryolite");
    public static final Item ALUMINA = simple("alumina");
    public static final Item ALUMINIUM_INGOT = simple("aluminium_ingot");
    public static final Item ALUMINIUM_COPPER_INGOT = simple("aluminium_copper_ingot");
    public static final Item SPODUMENE = simple("spodumene");
    public static final Item LITHIUM_CHLORIDE = simple("lithium_chloride");
    public static final Item LITHIUM_INGOT = simple("lithium_ingot");
    public static final Item ALUMINIUM_LITHIUM_INGOT = simple("aluminium_lithium_ingot");
    public static final Item NICKEL_INGOT = simple("nickel_ingot");
    public static final Item NICKEL_SUPERALLOY_INGOT = simple("nickel_superalloy_ingot");
    public static final Item SAPPHIRE = simple("sapphire");
    /** Шихты сплавов (сборочный стол → электропечь, как сталь): алюмомедная, алюминий-литиевая, жаропрочная, фосфатная. */
    public static final Item AL_CU_BLEND = simple("al_cu_blend");
    public static final Item AL_LI_BLEND = simple("al_li_blend");
    public static final Item SUPERALLOY_BLEND = simple("superalloy_blend");
    public static final Item PHOSPHATE_BLEND = simple("phosphate_blend");
    /** Кварцевый тигель Чохральского: растворяется в расплаве и трескается при остывании — один на загрузку. */
    public static final Item QUARTZ_CRUCIBLE = simple("quartz_crucible");
    /** Белый фосфор (процесс Вёлера) — донорная лигатура n-типа. */
    public static final Item PHOSPHORUS = simple("phosphorus");
    /** Серная кислота (контактный процесс: S → SO₂ → SO₃ → H₂SO₄). */
    public static final Item SULFURIC_ACID = simple("sulfuric_acid");
    /** Гипс CaSO₄ — побочный продукт HF из флюорита. */
    public static final Item GYPSUM = simple("gypsum");
    /** β-сподумен: после обжига 1050 °C решётка раскрывается и литий выщелачивается кислотой. */
    public static final Item BETA_SPODUMENE = simple("beta_spodumene");
    /** Сапфировая подложка Ø50 мм (T3, кремний-на-сапфире). */
    public static final Item SAPPHIRE_WAFER = register("sapphire_wafer", props -> new org.alex_melan.spacereloaded.electronics.MaterialItem(props.stacksTo(16)));
    /** Кремний-на-сапфире: эпитаксиальный слой кремния на сапфире — радиационно стойкая подложка. */
    public static final Item SOS_WAFER = register("sos_wafer", props -> new org.alex_melan.spacereloaded.electronics.MaterialItem(props.stacksTo(16)));
    /** Фотошаблон: контактная печать изнашивает его — 50 экспозиций. */
    public static final Item PHOTOMASK_LOGIC = register("photomask_logic", props -> new Item(props.durability(50)));
    /** Фотошаблон: контактная печать изнашивает его — 50 экспозиций. */
    public static final Item PHOTOMASK_MICROPROCESSOR = register("photomask_microprocessor", props -> new Item(props.durability(50)));
    /** Фотошаблон: контактная печать изнашивает его — 50 экспозиций. */
    public static final Item PHOTOMASK_RADHARD = register("photomask_radhard", props -> new Item(props.durability(50)));

    // --- Жизнь на станции (007) ---
    /** Гидроксид лития: каустификация хлорида лития щёлочью. */
    public static final Item LITHIUM_HYDROXIDE = simple("lithium_hydroxide");
    /** Картридж LiOH (Аполлон): прочность — граммы поглощаемого CO₂ (0.75 кг). */
    public static final Item LIOH_CARTRIDGE = register("lioh_cartridge", props -> new Item(props.durability(750)));
    /** Цеолит 5A (натриевый алюмосиликат). */
    public static final Item ZEOLITE = simple("zeolite");
    /** Цеолитовый слой (CDRA МКС): регенерируемый нагревом. */
    /** Солома — несъедобная биомасса пшеницы (60 % сухой массы), в окислитель. */
    public static final Item STRAW = simple("straw");
    /** Ровер (007): шасси (рама Al-2219, 2 места, электроника), мотор-колесо Ø 81 см, батарея Ni–Fe 8.7 кВт·ч. */
    public static final Item ROVER_CHASSIS = register("rover_chassis",
            props -> new org.alex_melan.spacereloaded.vehicle.RoverChassisItem(props.stacksTo(1)));
    public static final Item ROVER_WHEEL = register("rover_wheel", props -> new Item(props.stacksTo(4)));
    public static final Item NIFE_BATTERY = register("nife_battery", props -> new Item(props.stacksTo(1)));
    /** Главное зеркало Кассегрена Ø 10 см: стекло с напылением алюминия (007, US5). */
    public static final Item TELESCOPE_MIRROR = simple("telescope_mirror");
    /** Пушбрум-линейка ПЗС 10⁴ пикселей на кремниевой пластине в керамическом корпусе. */
    public static final Item IMAGE_SENSOR = simple("image_sensor");
    /** Орбитальный снимок: заказ в ЦУПе, проявляется в карту по готовности. */
    public static final Item ORBITAL_IMAGE = register("orbital_image",
            props -> new org.alex_melan.spacereloaded.orbit.OrbitalImageItem(props.stacksTo(1)));
    // --- 008: материалы реактора и топлива ---
    public static final Item BERYL = simple("beryl");
    public static final Item BERYLLIUM_HYDROXIDE = simple("beryllium_hydroxide");
    public static final Item BERYLLIUM_OXIDE = simple("beryllium_oxide");
    public static final Item SODIUM = simple("sodium");
    public static final Item BORAX = simple("borax");
    public static final Item BORIC_ACID = simple("boric_acid");
    public static final Item BORON_CARBIDE_BLEND = simple("boron_carbide_blend");
    public static final Item BORON_CARBIDE = simple("boron_carbide");
    public static final Item ZIRCON = simple("zircon");
    public static final Item ZIRCONIUM = simple("zirconium");
    public static final Item URANINITE = simple("uraninite");
    public static final Item YELLOWCAKE = simple("yellowcake");
    public static final Item URANIUM_DIOXIDE = simple("uranium_dioxide");
    public static final Item URANIUM_TETRAFLUORIDE = simple("uranium_tetrafluoride");
    public static final Item FLUORINE = simple("fluorine");
    public static final Item URANIUM_HEXAFLUORIDE = simple("uranium_hexafluoride");
    public static final Item DEPLETED_URANIUM_HEXAFLUORIDE = simple("depleted_uranium_hexafluoride");
    /** Топливная корзина Kilopower: сплав U-Zr, компоненты — U-235, обогащение, выгорание. */
    public static final Item FUEL_BASKET = register("fuel_basket", props -> new Item(props.stacksTo(1)));
    public static final Item ZEOLITE_BED = register("zeolite_bed", props -> new Item(props.stacksTo(1)));

    public static boolean isEnginePart(Item item) {
        return item == TURBOPUMP || item == INJECTOR_PLATE || item == REGEN_NOZZLE || item == CIRCUIT_BOARD;
    }

    public static boolean isIntermediatePart(Item item) {
        return item == INCOMPLETE_TURBOPUMP || item == INCOMPLETE_INJECTOR || item == INCOMPLETE_NOZZLE
                || item == INCOMPLETE_CIRCUIT_BOARD;
    }

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
