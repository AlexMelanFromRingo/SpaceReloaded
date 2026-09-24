package org.alex_melan.spacereloaded.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.alex_melan.spacereloaded.SpaceReloaded;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Конфиг мода (T018): жёсткие лимиты — константы конфига, а не «сколько получится»
 * (конституция, «Бюджет производительности»).
 */
public final class SpaceReloadedConfig {
    /** Лимит радиуса герметичной зоны (метрика Чебышёва). */
    public int sealingMaxRadius = 32;
    /**
     * true — заливка по 26 направлениям: газ уходит через диагональную щель,
     * углы обязаны быть заложены (замысел мода). false — классические
     * 6 направлений: строить проще, угловой блок больше не нужен.
     */
    public boolean sealingDiagonalLeaks = true;
    /** Максимум одновременных фоновых пересчётов зон (FR-051). */
    public int sealingMaxConcurrentJobs = 2;
    /** Тик-интервал проверки удушья сущностей вне зон. */
    public int vacuumCheckIntervalTicks = 10;
    /** Урон вакуума за одну проверку (полсердца). */
    public float vacuumDamage = 1.0f;
    /** Урон среды без полного скафандра (маска дышит, но холод/радиация грызут). */
    public float exposureDamage = 1.0f;
    /** Импульс взрывной декомпрессии, м/с на сущность у пробоины (FR-007). */
    public double decompressionImpulse = 1.5;
    /** Лимит блоков в сборке ракеты (FR-020). */
    public int rocketMaxBlocks = 4096;
    /** TTL chunk ticket'ов межпространственных операций, тики (D6). */
    public int ticketTtlTicks = 600;

    // --- Энергия (US2, единицы Team Reborn Energy) ---
    /** Выработка солнечной панели, E/тик (день, открытое небо). */
    public long solarEnergyPerTick = 20;
    /** Множитель панели в безатмосферном измерении (нет атмосферного ослабления). */
    public double solarVacuumMultiplier = 1.5;
    /** Выработка РИТЭГа, E/тик — слабо, но всегда. */
    public long rtgEnergyPerTick = 4;
    /** Выработка угольного генератора, E/тик горения. */
    public long coalGeneratorEnergyPerTick = 12;
    /** Ёмкость аккумулятора, E. */
    public long batteryCapacity = 100_000;
    /** Максимальный ввод/вывод аккумулятора, E/тик. */
    public long batteryMaxTransfer = 256;
    /** Пропускная способность кабельной сети, E/тик на сеть. */
    public long cableThroughput = 128;
    /** Внутренний буфер генераторов, E. */
    public long generatorBufferCapacity = 2_000;
    /** Потребление контроллера атмосферы, E/с (FR-009). */
    public long controllerEnergyPerSecond = 40;
    /** Буфер контроллера атмосферы, E. */
    public long controllerEnergyCapacity = 4_000;

    // --- Станки (US3) ---
    /** Потребление станка во время работы, E/тик. */
    public long machineEnergyPerTick = 8;
    /** Длительность операции дробилки, тики. */
    public int crusherTicks = 100;
    /** Длительность операции электропечи, тики. */
    public int electricFurnaceTicks = 120;
    /** Длительность операции сборочного стола, тики. */
    public int assemblyTicks = 160;

    /** Длительность операции электролизёра, тики. */
    public int electrolyzerTicks = 100;
    /** Топлива за операцию, кг. */
    public double electrolyzerFuelPerOp = 50.0;
    /** Кислорода в баллон за операцию (единицы прочности). */
    public int electrolyzerOxygenPerOp = 300;
    /** Длительность операции перегонного куба, тики. */
    public int refineryTicks = 120;
    /** Топлива за операцию перегонки, кг. */
    public double refineryFuelPerOp = 100.0;

    // --- Шлюзы (US2) ---
    /** Длительность цикла выравнивания давления люка, тики. */
    public int airlockCycleTicks = 40;
    /** Радиус интерлока: люк не откроется, если другой открытый люк ближе (Чебышёв). */
    public int airlockInterlockRadius = 5;

    // --- Орбитальная кинетическая пушка (US7) ---
    /** Буфер энергии пушки, E. */
    public long cannonEnergyCapacity = 400_000;
    /** Стоимость выстрела, E (разгон лома до дульной скорости). */
    public long cannonEnergyPerShot = 150_000;
    /** Магазин: максимум ломов в пушке. */
    public int cannonMaxRods = 16;
    /** Масса вольфрамового лома, кг (~0.1 м³ вольфрама). */
    public double cannonRodMassKg = 2000;
    /** Высота входа снаряда в атмосферу над целью, м. */
    public double cannonDropAltitude = 2400;
    /** Скорость, приданная пушкой (вниз), м/с. */
    public double cannonMuzzleSpeed = 1500;
    /** Игровой множитель радиуса кратера поверх E^(1/3)-подобия ядра. */
    public double cannonCraterMultiplier = 1.0;
    /** Жёсткий предел радиуса кратера, блоки (бюджет производительности). */
    public int cannonMaxCraterRadius = 16;
    /** Блоки с взрывостойкостью >= порога выживают в кратере (обсидиан — защита). */
    public double cannonMaxBlockResistance = 100.0;
    /** Кулдаун выстрела, тики (защита от случайного залпа). */
    public int cannonCooldownTicks = 200;

    // --- Метеориты (backlog AR/GC) ---
    /** Метеоритные дожди на безатмосферных телах включены. */
    public boolean meteorsEnabled = true;
    /** Интервал проверки спавна, тики. */
    public int meteorCheckIntervalTicks = 200;
    /** Шанс метеорита на игрока за проверку. */
    public double meteorChancePerCheck = 0.12;
    /** Масса метеорита, кг (задаёт кратер). */
    public double meteorMassKg = 800;
    /** Начальная скорость вниз, м/с. */
    public double meteorSpeed = 55;
    /** Высота спавна над игроком, м. */
    public double meteorSpawnAltitude = 120;
    /** Мин/макс горизонтальный отступ от игрока, м. */
    public int meteorMinHorizontal = 10;
    public int meteorHorizontalRange = 44;
    /** Множитель радиуса кратера метеорита. */
    public double meteorCraterMultiplier = 1.5;
    /** Предел радиуса кратера метеорита, блоки. */
    public int meteorMaxCraterRadius = 6;
    // --- Химический контур Марса (Phase 11) ---
    /** Буфер энергии сборщика/реактора, E. */
    public long chemMachineEnergyCapacity = 8_000;
    /** Потребление химических машин, E/тик. */
    public long chemMachineEnergyPerTick = 10;
    /** Тики на порцию CO2 сборщиком (co2-мир). */
    public int collectorTicks = 80;
    /** Множитель длительности на воздушном мире (CO2 в воздухе Земли — след). */
    public int collectorAirPenalty = 8;
    /** Тики на порцию метанокса реактором Сабатье. */
    public int sabatierTicks = 120;
    /** Метанокса за операцию, кг. */
    public double sabatierFuelPerOp = 60.0;
    /** Буфер топлива реактора, кг. */
    public double sabatierBufferCapacity = 600.0;

    // --- Тепловая модель (Phase 14) ---
    /** Комфортная температура базы, °C (нулевая доп-нагрузка климат-контроля). */
    public double thermalComfort = 20.0;
    /** Масштаб теплопотерь: при |T−комфорт| = scale энергия контроллера ×2. */
    public double thermalLoadScale = 120.0;
    /** Энергия ректенны за один энергоспутник, E/тик (чистое небо). */
    public long rectennaEnergyPerSat = 30;

    // --- Пылевые бури Марса (Phase 12) ---
    public boolean dustStormsEnabled = true;
    /** Интервал проверки старта бури, тики. */
    public int dustStormCheckInterval = 600;
    /** Шанс начать бурю за проверку. */
    public double dustStormChance = 0.05;
    /** Длительность бури, тики. */
    public int dustStormDurationTicks = 2400;
    /** Множитель солнечной эффективности в бурю (режет ~90%). */
    public double dustStormSolarMultiplier = 0.1;
    /** Абразивный урон бури за тик-проверку (незащищённым вне зоны). */
    public float dustStormDamage = 1.0f;

    /** Метеоритного железа за удар (диапазон). */
    public int meteorIronMin = 2;
    public int meteorIronMax = 5;
    /** Блоки взрывостойкости ≥ порога переживают удар метеорита (обсидиан-бункер). */
    public double meteorMaxBlockResistance = 100.0;

    // --- Полёт 2.0: ориентация (US2) ---
    /** Предел командуемого наклона, градусы — граница модели малых углов и неповёрнутой коллизии. */
    public double attitudeMaxDeg = 45.0;
    /** Скорость роста командуемого угла при удержании клавиши и возврата при отпускании, °/с. */
    public double attitudeRateDegPerSec = 30.0;

    // --- Полёт 2.0: аэродинамика (US3) ---
    /** C_d ракеты: блочная ракета — тупое тело, между конусом (0.3) и кубом (1.05). */
    public double rocketDragCoefficient = 0.5;
    /** C_d вольфрамового лома: заострённое тело, теряет проценты скорости в атмосфере. */
    public double cannonRodDragCoefficient = 0.1;
    /** Площадь сечения лома, м² (Ø ≈ 0.2 м при массе 2 т вольфрама). */
    public double cannonRodAreaM2 = 0.03;
    /** C_d метеорита: неправильное тело. */
    public double meteorDragCoefficient = 1.0;
    /** Площадь сечения метеорита, м². */
    public double meteorAreaM2 = 1.0;
    /**
     * Порог индекса нагрева √ρ·v³ (закон Саттона-Грейвса без коэффициента и радиуса
     * затупления — они свёрнуты сюда): 6·10⁶ ≈ 176 м/с у поверхности Земли, штатный
     * подъём к высоте перехода (~120 м/с) не греет.
     */
    public double reentryHeatIndexThreshold = 6.0e6;
    /** Урон экипажу за интервал при превышении порога нагрева без возвратной капсулы в стеке. */
    public float reentryHeatDamage = 2.0f;
    /** Интервал урона нагрева, тики (делитель тик-цикла — обязан быть >= 1). */
    public int reentryHeatIntervalTicks = 20;
    /** Порог скоростного напора ½ρv² для предупреждения скан-отчёта, Па (~239 м/с у поверхности Земли). */
    public double maxDynamicPressurePa = 35_000.0;

    // --- Полёт 2.0: ступени (US1) ---
    /** Импульс раздвигания частей при отделении ступени, Н·с: Δv каждой части = J/m, суммарный импульс сохраняется. */
    public double stageSeparationImpulseNs = 3000.0;
    /** Предельное время жизни обломка ступени, тики — предохранитель от вечного падения (принцип V). */
    public int stageDebrisMaxTicks = 6000;

    // --- Полёт 2.0: точность орудия (US4) ---
    /** Радиус рассеивания наводимого лома (есть спутниковое покрытие целевого измерения), блоки. */
    public double cannonGuidedSpreadBlocks = 0.5;
    /** Радиус рассеивания без спутникового покрытия целевого измерения, блоки («разброс до N»). */
    public double cannonUnguidedSpreadBlocks = 10.0;

    // --- Межпланетная логистика (003): линии, перелёты, wet workshop ---
    /** Интервал проверки грузового терминала, тики (делитель тик-цикла). */
    public int cargoLineCheckIntervalTicks = 20;
    /** Выдержка «груз и топливо не менялись» перед автозапуском, тики (10 с: погрузчик и колонка работают раз в 10 тиков). */
    public int cargoLineDwellTicks = 200;
    /** Запас планировщика к Δv перелёта и посадки, % (навигационные ошибки, неидеальные импульсы). */
    public double cargoLineDeltaVMarginPercent = 5.0;
    /** Задержка пересадки автопилота на промежуточной платформе, тики (время «перепрограммирования»). */
    public int autopilotRelaunchDelayTicks = 100;
    /**
     * Балансовый множитель таблицы перелётов профилей планет (НЕ физика: таблица выведена из
     * уравнений Гомана/патч-коник по реальным орбитам). 1.0 — честные значения, 0 — списание отключено.
     */
    public double transferDeltaVScale = 1.0;
    /** Максимальная доля топлива (от ёмкости) для конверсии борта в модуль станции; остаток стравливается. */
    public double wetWorkshopMaxResidualFraction = 0.05;
    /** Высота прибытия над маяком/точкой спуска, м — общая для перехода и планировщика посадки. */
    public double arrivalHeightM = 180.0;

    // --- Лунная индустрия (004): катапульта, ловушка масс, реголитовый реактор ---
    /** Предельное ускорение стальной катушки, g: О'Нил закладывал ~1000 g для сыпучих грузов в «вёдрах». */
    public double coilTier1AccelG = 1000.0;
    /** Предельное ускорение сверхпроводящей катушки, g (только груз в капсуле; аппаратура — 30–100 g). */
    public double coilTier2AccelG = 3000.0;
    /** Длина секции рельса, м (1 блок = 1 м). */
    public double massDriverSectionLengthM = 1.0;
    /** Предел длины рельса, секций — ограничивает число прогружаемых чанков (Луна: 324 секции тира 1). */
    public int massDriverMaxSections = 400;
    /** КПД линейного синхронного двигателя η: E = ½mv²/η. */
    public double massDriverEfficiency = 0.85;
    /**
     * Масштаб «Дж на единицу энергии мода». 15 кДж — масштаб, неявно заданный орбитальной
     * пушкой (150 000 E на лом 2000 кг при 1500 м/с = 2.25 ГДж). Генераторы мода в этом
     * масштабе не физичны (панель 20 E/т ≈ 6 МВт) — балансовое упрощение.
     */
    public double massDriverJoulesPerEnergy = 15_000.0;
    /** Свободные клетки за дульным срезом, без которых выстрел запрещён. */
    public int massDriverClearance = 8;
    /** Перезарядка катапульты = время возврата салазок к казённику, тики. */
    public int massDriverSledReturnTicks = 200;
    /** Визуальная длительность волны по рельсу, тики (реальный разгон ≈ 0.1 с — художественно замедлен). */
    public int massDriverWaveTicks = 10;
    /** Ёмкость конденсатора, E. */
    public long capacitorCapacity = 250_000L;
    /** Приём конденсатора из сети, E/т. */
    public long capacitorMaxInsert = 2_000L;
    /** Предел числа конденсаторов в батарее казённика. */
    public int capacitorMaxBlocks = 32;
    /** Масса пустой грузовой капсулы, кг. */
    public double podDryMassKg = 100.0;
    /**
     * Масштаб таблицы масс предметов для капсулы (1.0 — как в датапаке {@code item_mass}:
     * блок — 10 л материала с реальной плотностью, слиток — 1/9 блока). Балансовый множитель.
     */
    public double podKgPerItem = 1.0;
    /** Предел скоростного напора капсулы у дульного среза, Па. */
    public double podMaxDynamicPressurePa = 1_000_000.0;
    /** Предел теплового потока капсулы (Саттон–Грейвс), Вт/м². */
    public double podMaxHeatFluxWm2 = 5_000_000.0;
    /** Игровое время перелёта капсулы к ловушке, тики (реально ~3 суток — балансовое упрощение). */
    public int podTransitTicks = 600;
    /** σ рассеивания прибытия при спутниковом покрытии орбиты цели, блоки (как наводимый лом). */
    public double podSigmaCovered = 0.5;
    /** σ рассеивания прибытия без покрытия, блоки (как ненаводимый лом). */
    public double podSigmaUncovered = 10.0;
    /** Радиус захвата ловушки без сетки, блоки. */
    public double catcherBaseRadius = 1.5;
    /** Прирост радиуса захвата на √(число секций сетки): радиус круга той же площади ≈ 0.56·√n. */
    public double catcherRadiusPerSqrtNet = 0.6;
    /** Предел радиуса захвата, блоки. */
    public double catcherMaxRadius = 12.0;
    /** Предел числа секций сетки ловушки. */
    public int catcherNetMaxBlocks = 441;
    /** Только для стенда: ловушка работает вне орбитальной платформы. */
    public boolean massCatcherAnyDimension = false;
    /** Длительность цикла реголитового реактора, тики. */
    public int reactorCycleTicks = 200;
    /** Энергия цикла реактора, E (электролиз расплава ~21 кВт·ч/кг O₂, масштаб от электролизёра мода). */
    public long reactorEnergyPerCycle = 1_600L;
    /** Кислород из блока реголита, единицы баллона (половина льда: реголит беднее и дороже). */
    public int reactorOxygenPerBlock = 150;
    /** Шанс титановой пыли за цикл (ильменит в морском реголите). */
    public double reactorTitaniumChance = 0.2;
    /** Внутренний буфер кислорода реактора, единицы. */
    public int reactorOxygenBuffer = 1_500;
    /** Толща непрозрачных блоков над головой, начиная с которой позиция — «укрытие» (стабильная температура). */
    public int shelterMinRockBlocks = 4;

    // --- Инженерия (005): трансмиссия, станки, мультиблоки, детали ---
    /** Предел числа блоков механической сети. */
    public int kineticMaxBlocks = 512;
    /** КПД зацепления цилиндрических шестерён (прямозубые ≈ 0.98). */
    public double gearMeshEfficiency = 0.98;
    /** КПД конической пары углового редуктора. */
    public double bevelEfficiency = 0.97;
    /** Момент трения на блок сети (подшипники), Н·м. */
    public double kineticFrictionTorqueNm = 2.0;
    /** Диаметр вала, м (визуал 4 px = 0.25 м) — для предела кручения τ_max = πd³τ_y/16. */
    public double shaftDiameterM = 0.25;
    /** Предел текучести стали при сдвиге τ_y ≈ 0.58·σ_y, Па. */
    public double steelShaftShearPa = 230e6;
    /** Скалывание дерева вдоль волокон, Па. */
    public double woodShaftShearPa = 8e6;
    /** Предельный момент фрикционной муфты-предохранителя, Н·м. */
    public double clutchSlipTorqueNm = 50_000;
    /** Пиковая мощность мотора-генератора, Вт (P_max = τ_st·ω₀/4). */
    public double motorPowerW = 300_000;
    /** Обороты холостого хода мотора, об/мин. */
    public double motorNoLoadRpm = 1500;
    /** КПД мотора-генератора в обе стороны. */
    public double motorEfficiency = 0.92;
    /** Энергобуфер мотора, E. */
    public long motorEnergyBuffer = 200;
    /** Момент инерции стального маховика, кг·м² (сплошной диск r 0.5 м, толщина 0.5 м). */
    public double flywheelInertia = 385;
    /** Предельная угловая скорость маховика, рад/с (σ = (3+ν)/8·ρω²r² ≤ 400 МПа). */
    public double flywheelMaxOmega = 703;
    /** Потолок радиуса взрыва при разрыве маховика. */
    public double flywheelBurstMaxRadius = 6;
    /** Разрыв маховика разрушает блоки (серверы могут выключить). */
    public boolean flywheelBurstBreaksBlocks = true;
    /** Работа одного удара пресса, Дж (1 МН × 5 см). */
    public double pressStrokeJ = 50_000;
    /** Длительность удара, тики (0.2 с). */
    public int pressStrokeTicks = 4;
    /** Период цикла пресса, тики. */
    public int pressCycleTicks = 40;
    /** Рабочее окно и номинал оборотов пресса, об/мин. */
    public double pressMinRpm = 60;
    public double pressMaxRpm = 600;
    public double pressNominalRpm = 300;
    /** Номинальная мощность токарного станка, Вт (момент резания = P/ω_ном). */
    public double latheNominalPowerW = 30_000;
    /** Рабочее окно и номинал оборотов шпинделя, об/мин. */
    public double latheMinRpm = 300;
    public double latheMaxRpm = 1200;
    public double latheNominalRpm = 750;
    /** Погрешность операции токарного станка и пресса при номинальных оборотах, мкм. */
    public double toleranceLatheUm = 10;
    public double tolerancePressUm = 20;
    /** Рост погрешности на единицу относительного отклонения оборотов. */
    public double toleranceSpeedFactor = 2;
    /** Порог брака по суммарной погрешности, мкм. */
    public double toleranceScrapUm = 100;
    /** Пределы мультиблоков: ячейки стека, тарелки колонны. */
    public int stackMaxCells = 15;
    public int columnMinTrays = 4;
    public int columnMaxTrays = 16;
    /** Синхронизация ω клиенту: относительный порог и минимальный интервал, тики. */
    public double kineticSyncThreshold = 0.03;
    public int kineticSyncMinTicks = 5;
    /** Визуальный предел скорости вращения в рендере, рад/с (выше — стробоскоп). */
    public double kineticVisualMaxOmega = 40;
    /** Скорость ветра у поверхности, м/с: Земля, Марс, Марс в пылевую бурю. */
    public double windSpeedEarth = 8;
    public double windSpeedMars = 8;
    public double windSpeedMarsStorm = 25;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static SpaceReloadedConfig load(Path configDir) {
        Path file = configDir.resolve("spacereloaded.json");
        if (Files.exists(file)) {
            try {
                SpaceReloadedConfig config = GSON.fromJson(Files.readString(file), SpaceReloadedConfig.class);
                if (config != null) {
                    config.validate();
                    return config;
                }
            } catch (IOException | RuntimeException e) {
                SpaceReloaded.LOGGER.error("Не удалось прочитать {}, используются значения по умолчанию", file, e);
            }
        }
        SpaceReloadedConfig defaults = new SpaceReloadedConfig();
        try {
            Files.createDirectories(configDir);
            Files.writeString(file, GSON.toJson(defaults));
        } catch (IOException e) {
            SpaceReloaded.LOGGER.warn("Не удалось записать конфиг по умолчанию в {}", file, e);
        }
        return defaults;
    }

    private void validate() {
        if (sealingMaxRadius < 4 || sealingMaxRadius > 128) {
            throw new IllegalArgumentException("sealingMaxRadius должен быть в [4, 128]");
        }
        if (sealingMaxConcurrentJobs < 1 || sealingMaxConcurrentJobs > 8) {
            throw new IllegalArgumentException("sealingMaxConcurrentJobs должен быть в [1, 8]");
        }
        if (rocketMaxBlocks < 8 || rocketMaxBlocks > 65_536) {
            throw new IllegalArgumentException("rocketMaxBlocks должен быть в [8, 65536]");
        }
        // Делители тик-циклов: 0/отрицательное = ArithmeticException в цикле тиков
        if (vacuumCheckIntervalTicks < 1) {
            throw new IllegalArgumentException("vacuumCheckIntervalTicks должен быть >= 1");
        }
        if (meteorCheckIntervalTicks < 1) {
            throw new IllegalArgumentException("meteorCheckIntervalTicks должен быть >= 1");
        }
        if (dustStormCheckInterval < 1) {
            throw new IllegalArgumentException("dustStormCheckInterval должен быть >= 1");
        }
        if (dustStormChance < 0 || dustStormChance > 1) {
            throw new IllegalArgumentException("dustStormChance должен быть в [0, 1]");
        }
        if (dustStormDurationTicks < 1) {
            throw new IllegalArgumentException("dustStormDurationTicks должен быть >= 1");
        }
        if (meteorChancePerCheck < 0 || meteorChancePerCheck > 1) {
            throw new IllegalArgumentException("meteorChancePerCheck должен быть в [0, 1]");
        }
        if (meteorHorizontalRange < meteorMinHorizontal || meteorMinHorizontal < 0) {
            throw new IllegalArgumentException("meteorHorizontalRange должен быть >= meteorMinHorizontal >= 0");
        }
        if (meteorMaxCraterRadius < 0 || meteorMassKg <= 0) {
            throw new IllegalArgumentException("meteorMaxCraterRadius >= 0 и meteorMassKg > 0");
        }
        if (meteorIronMax < meteorIronMin || meteorIronMin < 0) {
            throw new IllegalArgumentException("meteorIronMax должен быть >= meteorIronMin >= 0");
        }
        // Полёт 2.0
        if (attitudeMaxDeg < 5 || attitudeMaxDeg > 60) {
            throw new IllegalArgumentException("attitudeMaxDeg должен быть в [5, 60]");
        }
        if (attitudeRateDegPerSec < 1 || attitudeRateDegPerSec > 180) {
            throw new IllegalArgumentException("attitudeRateDegPerSec должен быть в [1, 180]");
        }
        if (rocketDragCoefficient < 0 || rocketDragCoefficient > 3
                || cannonRodDragCoefficient < 0 || cannonRodDragCoefficient > 3
                || meteorDragCoefficient < 0 || meteorDragCoefficient > 3) {
            throw new IllegalArgumentException("коэффициенты сопротивления должны быть в [0, 3]");
        }
        if (cannonRodAreaM2 <= 0 || cannonRodAreaM2 > 10 || meteorAreaM2 <= 0 || meteorAreaM2 > 50) {
            throw new IllegalArgumentException("cannonRodAreaM2 в (0, 10], meteorAreaM2 в (0, 50]");
        }
        if (reentryHeatIndexThreshold <= 0 || maxDynamicPressurePa <= 0) {
            throw new IllegalArgumentException("reentryHeatIndexThreshold и maxDynamicPressurePa должны быть > 0");
        }
        if (reentryHeatDamage < 0 || reentryHeatDamage > 40) {
            throw new IllegalArgumentException("reentryHeatDamage должен быть в [0, 40]");
        }
        if (reentryHeatIntervalTicks < 1) {
            throw new IllegalArgumentException("reentryHeatIntervalTicks должен быть >= 1");
        }
        if (stageSeparationImpulseNs < 0 || stageSeparationImpulseNs > 1_000_000) {
            throw new IllegalArgumentException("stageSeparationImpulseNs должен быть в [0, 1e6]");
        }
        if (stageDebrisMaxTicks < 20) {
            throw new IllegalArgumentException("stageDebrisMaxTicks должен быть >= 20");
        }
        if (cannonGuidedSpreadBlocks < 0 || cannonGuidedSpreadBlocks > 4
                || cannonUnguidedSpreadBlocks < 0 || cannonUnguidedSpreadBlocks > 64) {
            throw new IllegalArgumentException("cannonGuidedSpreadBlocks в [0, 4], cannonUnguidedSpreadBlocks в [0, 64]");
        }
        // Межпланетная логистика (003)
        if (cargoLineCheckIntervalTicks < 1 || cargoLineDwellTicks < 1 || autopilotRelaunchDelayTicks < 1) {
            throw new IllegalArgumentException("cargoLineCheckIntervalTicks, cargoLineDwellTicks, autopilotRelaunchDelayTicks должны быть >= 1");
        }
        if (cargoLineDeltaVMarginPercent < 0 || cargoLineDeltaVMarginPercent > 100) {
            throw new IllegalArgumentException("cargoLineDeltaVMarginPercent должен быть в [0, 100]");
        }
        if (transferDeltaVScale < 0 || transferDeltaVScale > 10) {
            throw new IllegalArgumentException("transferDeltaVScale должен быть в [0, 10]");
        }
        if (wetWorkshopMaxResidualFraction < 0 || wetWorkshopMaxResidualFraction > 1) {
            throw new IllegalArgumentException("wetWorkshopMaxResidualFraction должен быть в [0, 1]");
        }
        if (arrivalHeightM < 20 || arrivalHeightM > 400) {
            throw new IllegalArgumentException("arrivalHeightM должен быть в [20, 400]");
        }
        // Лунная индустрия (004)
        if (coilTier1AccelG < 1 || coilTier1AccelG > 100_000 || coilTier2AccelG < coilTier1AccelG) {
            throw new IllegalArgumentException("coilTier1AccelG должен быть в [1, 100000], coilTier2AccelG >= coilTier1AccelG");
        }
        if (massDriverSectionLengthM < 0.25 || massDriverSectionLengthM > 4 || massDriverMaxSections < 8
                || massDriverMaxSections > 1024) {
            throw new IllegalArgumentException("massDriverSectionLengthM в [0.25, 4], massDriverMaxSections в [8, 1024]");
        }
        if (massDriverEfficiency <= 0 || massDriverEfficiency > 1 || massDriverJoulesPerEnergy <= 0) {
            throw new IllegalArgumentException("massDriverEfficiency в (0, 1], massDriverJoulesPerEnergy > 0");
        }
        if (massDriverClearance < 1 || massDriverClearance > 64 || massDriverSledReturnTicks < 20
                || massDriverWaveTicks < 2 || massDriverWaveTicks > 100) {
            throw new IllegalArgumentException("massDriverClearance [1, 64], massDriverSledReturnTicks >= 20, massDriverWaveTicks [2, 100]");
        }
        if (capacitorCapacity < 1000 || capacitorMaxInsert < 1 || capacitorMaxBlocks < 1 || capacitorMaxBlocks > 256) {
            throw new IllegalArgumentException("capacitorCapacity >= 1000, capacitorMaxInsert >= 1, capacitorMaxBlocks в [1, 256]");
        }
        if (podDryMassKg <= 0 || podKgPerItem < 0 || podMaxDynamicPressurePa <= 0 || podMaxHeatFluxWm2 <= 0
                || podTransitTicks < 60 || podSigmaCovered <= 0 || podSigmaUncovered <= 0) {
            throw new IllegalArgumentException("параметры капсулы: масса > 0, пределы > 0, podTransitTicks >= 60, σ > 0");
        }
        if (catcherBaseRadius < 0 || catcherRadiusPerSqrtNet < 0 || catcherMaxRadius < catcherBaseRadius
                || catcherNetMaxBlocks < 1 || catcherNetMaxBlocks > 4096) {
            throw new IllegalArgumentException("параметры ловушки: радиусы >= 0, max >= base, catcherNetMaxBlocks в [1, 4096]");
        }
        if (reactorCycleTicks < 1 || reactorEnergyPerCycle < 0 || reactorOxygenPerBlock < 0
                || reactorTitaniumChance < 0 || reactorTitaniumChance > 1 || reactorOxygenBuffer < 0) {
            throw new IllegalArgumentException("параметры реактора: цикл >= 1, энергия/O2/буфер >= 0, шанс в [0, 1]");
        }
        if (shelterMinRockBlocks < 1 || shelterMinRockBlocks > 64) {
            throw new IllegalArgumentException("shelterMinRockBlocks должен быть в [1, 64]");
        }
        // Инженерия (005)
        if (kineticMaxBlocks < 8 || kineticMaxBlocks > 4096) {
            throw new IllegalArgumentException("kineticMaxBlocks в [8, 4096]");
        }
        if (gearMeshEfficiency <= 0 || gearMeshEfficiency > 1 || bevelEfficiency <= 0 || bevelEfficiency > 1
                || motorEfficiency <= 0 || motorEfficiency > 1) {
            throw new IllegalArgumentException("КПД зацеплений и мотора в (0, 1]");
        }
        if (kineticFrictionTorqueNm < 0 || shaftDiameterM <= 0 || steelShaftShearPa <= 0 || woodShaftShearPa <= 0
                || clutchSlipTorqueNm <= 0) {
            throw new IllegalArgumentException("параметры валов и муфты > 0");
        }
        if (motorPowerW <= 0 || motorNoLoadRpm <= 0 || motorEnergyBuffer < 1) {
            throw new IllegalArgumentException("параметры мотора > 0");
        }
        if (flywheelInertia <= 0 || flywheelMaxOmega <= 0 || flywheelBurstMaxRadius < 0) {
            throw new IllegalArgumentException("параметры маховика > 0");
        }
        if (pressStrokeJ <= 0 || pressStrokeTicks < 1 || pressCycleTicks <= pressStrokeTicks
                || pressMinRpm <= 0 || pressMaxRpm <= pressMinRpm || pressNominalRpm <= 0) {
            throw new IllegalArgumentException("параметры пресса: удар > 0, цикл > удара, окно min < max");
        }
        if (latheNominalPowerW <= 0 || latheMinRpm <= 0 || latheMaxRpm <= latheMinRpm || latheNominalRpm <= 0) {
            throw new IllegalArgumentException("параметры токарного станка > 0, окно min < max");
        }
        if (toleranceLatheUm < 0 || tolerancePressUm < 0 || toleranceSpeedFactor < 0 || toleranceScrapUm <= 0) {
            throw new IllegalArgumentException("параметры допусков >= 0, брак > 0");
        }
        if (stackMaxCells < 1 || columnMinTrays < 1 || columnMaxTrays < columnMinTrays) {
            throw new IllegalArgumentException("пределы мультиблоков");
        }
        if (kineticSyncThreshold <= 0 || kineticSyncMinTicks < 1 || kineticVisualMaxOmega <= 0) {
            throw new IllegalArgumentException("параметры синхронизации вращения > 0");
        }
        if (windSpeedEarth < 0 || windSpeedMars < 0 || windSpeedMarsStorm < 0) {
            throw new IllegalArgumentException("скорость ветра >= 0");
        }
    }
}
