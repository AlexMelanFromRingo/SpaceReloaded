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
    public long cannonEnergyCapacity = 200_000;
    /** Стоимость выстрела, E (разгон лома до дульной скорости). */
    public long cannonEnergyPerShot = 50_000;
    /** Магазин: максимум ломов в пушке. */
    public int cannonMaxRods = 16;
    /** Масса вольфрамового лома, кг (~0.1 м³ вольфрама). */
    public double cannonRodMassKg = 2000;
    /** Высота входа снаряда в атмосферу над целью, м. */
    public double cannonDropAltitude = 350;
    /** Скорость, приданная пушкой (вниз), м/с. */
    public double cannonMuzzleSpeed = 80;
    /** Линейное сопротивление лома в атмосфере, 1/с (обтекаемый — почти 0). */
    public double cannonDragCoeff = 0.01;
    /** Игровой множитель радиуса кратера поверх E^(1/3)-подобия ядра. */
    public double cannonCraterMultiplier = 2.0;
    /** Жёсткий предел радиуса кратера, блоки (бюджет производительности). */
    public int cannonMaxCraterRadius = 12;
    /** Блоки с взрывостойкостью >= порога выживают в кратере (обсидиан — защита). */
    public double cannonMaxBlockResistance = 100.0;
    /** Кулдаун выстрела, тики (защита от случайного залпа). */
    public int cannonCooldownTicks = 40;

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
    /** Сопротивление метеорита (обтекаемый — почти нет). */
    public double meteorDragCoeff = 0.01;
    /** Множитель радиуса кратера метеорита. */
    public double meteorCraterMultiplier = 1.5;
    /** Предел радиуса кратера метеорита, блоки. */
    public int meteorMaxCraterRadius = 6;
    /** Метеоритного железа за удар (диапазон). */
    public int meteorIronMin = 2;
    public int meteorIronMax = 5;
    /** Блоки взрывостойкости ≥ порога переживают удар метеорита (обсидиан-бункер). */
    public double meteorMaxBlockResistance = 100.0;

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
    }
}
