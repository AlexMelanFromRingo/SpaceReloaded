# Data Model: Полёт 2.0 (Phase 1)

Ядро (`core`) не знает о Minecraft; позиции — `long` (`PackedPos`), векторы — `Vec3d` ядра. Mod-слой маппит `RocketData`, `PlanetProfile` и конфиг в записи ядра.

## core/rocketry — ступени

```java
enum PartRole { HULL, ENGINE, TANK, COMMAND, SEAT, GYRO, CLAMP, DECORATIVE, SEPARATOR }
// SEPARATOR: только масса + плоскость разделения; после отделения остаётся на нижней ступени

record Stage(int index, List<PlacedPart> parts, int topY,          // topY = Y плоскости разделителя (или maxY стека)
             double dryMassKg, double propellantCapacityKg,
             boolean hasEngines)

record StageLayout(List<Stage> stages) {                            // снизу вверх, ≥ 1 ступень
    static StageLayout of(RocketStructure structure);              // деление по уникальным Y разделителей
    static Optional<StageError> validate(RocketStructure s, long commandPos);
    RocketStructure activeView(int active, double[] stagePropellantKg); // см. plan D11
    RocketStructure remainingAfter(int dropped);                   // структура без ступеней ≤ dropped (для расчётов)
    double[] distributeByCapacity(double totalPropellantKg);       // fallback для старых сохранений/стыковки
}
record StageError(String key, long packedPos)   // key ∈ {stage_above_command, stage_empty_below, stage_disconnected}

record StagePerformance(int index, double stackMassKg, double dryMassKg, double propellantKg,
                        double thrustN, double twr, double deltaV, List<PerformanceWarning> warnings)
record StagedReport(List<StagePerformance> stages, double totalDeltaV)
final class StagedPerformance { static StagedReport calculate(StageLayout, double[] propellant, double gravity); }
```

Инварианты: `stages` покрывают все детали ровно один раз; `stages[i].topY < stages[i+1].topY`; командный модуль ∈ верхней ступени; одноступенчатый стек ⇒ `activeView(0) ≡ structure` и `totalDeltaV == PerformanceCalculator.deltaV` (0.1%).

## core/rocketry — подъём и нагрев

```java
record AscentReport(boolean reachedTarget, double apexM, double deltaVSpentToTargetMs,
                    double maxDynamicPressurePa, double timeToTargetS, int stagesUsed)
final class AscentSimulator {
    static AscentReport simulate(StageLayout layout, double[] propellant, FlightEnvironment env,
                                 double cd, double startY, double targetY);   // dt 0.05, ≤ 600 с модельного времени
}
final class Aerothermal {
    static double heatIndex(double density, double speed);        // √ρ·v³
    static double dynamicPressure(double density, double speed);  // ½ρv²
}
```

## core/atmosphere

```java
record AtmosphereProfile(double surfaceDensity, double scaleHeightM, double datumY) {
    static final AtmosphereProfile VACUUM;                 // (0, 1, 0)
    double density(double y);                              // ρ₀·exp(−max(0, y−datum)/H); 0 в вакууме
    boolean isVacuum();
}
record DragBody(double cd, double areaX, double areaY, double areaZ) {
    double effectiveArea(Vec3d velocity);                  // Σ A_i·|v̂_i|
    Vec3d force(Vec3d velocity, double density);           // −½ρ|v|·C_d·A_eff·v
}
record FlightEnvironment(double gravity, AtmosphereProfile atmosphere)   // + ctor (gravity) = вакуум
// RocketStructure.dragBody(cd) → DragBody из габаритов AABB структуры
```

## core/ballistics

```java
record ProjectileSpec(double massKg, double cd, double areaM2)          // квадратичная модель (линейная удалена)
final class BallisticIntegrator {
    static State step(State s, ProjectileSpec spec, double gravity, double density, double dt);
    static double etaToAltitude(...);                                   // без изменений
    record Forecast(double impactSpeedMs, double impactEnergyJ, double flightTimeS)
    static Forecast impactForecast(ProjectileSpec spec, double dropAltitude, double muzzleSpeed,
                                   double gravity, AtmosphereProfile atmosphere, double targetY);
}
record StrikeSolution(boolean guided, double spreadRadius, double offsetX, double offsetZ) {
    static StrikeSolution solve(RandomSource-подобный double[] u, boolean guided, double guidedR, double unguidedR);
    // r = R·√u₁, φ = 2π·u₂ — равномерно по площади круга; RandomSource остаётся в mod
}
```

## mod-слой

| Объект | Хранение | Ключевые поля / изменения |
|---|---|---|
| `RocketEntity.StageState` | NBT `stage_active:int`, `stage_propellant:double[]`, `debris:bool`, `debris_ticks:int` | активная ступень, топливо по ступеням; `flight.propellantKg` = топливо активной; без NBT — `layout.distributeByCapacity(total)` |
| `RocketEntity` synched | `DATA_STAGE`, `DATA_STAGE_COUNT`, `DATA_STAGE_FUEL`, `DATA_DELTA_V`, `DATA_CMD_PITCH`, `DATA_CMD_ROLL`, `DATA_HAS_GYRO`, `DATA_HEATING` | HUD: ступень, топливо ступени, Δv остаток (раз в 10 тиков), командуемые углы, флаг гиродинов, нагрев |
| `RocketEntity` transient | `cmdTiltDeg`, `cmdDirX/Z`, `heatTicks` | не сохраняются: без пилота обнуляются |
| `StageSeparation` | статический оператор | `separate(level, rocket) → Component`; расщепление `RocketData`, спавн обломка, импульс |
| `PlanetProfile` | датапак `planets/*.json` | + `atmosphere_density` (кг/м³, default 0), `scale_height` (м, default 110), `datum_y` (default 63); валидация 0 ≤ ρ ≤ 50 |
| `RocketPartEntry` | датапак `part_properties/*.json` | role `separator` |
| `KineticProjectileEntity` | NBT + `guided:bool`, `cd`, `area` | сопротивление по атмосфере измерения на текущей высоте |
| `MeteorEntity` | NBT | `cd`, `area` из конфига |
| `OrbitalCannonBlockEntity` | без новых полей | режим наведения считается при выстреле и в `snapshot()` |
| `SpaceNetworkState` | SavedData | + `setCoverage(dim, n)` для стенда/админа |
| `SpaceReloadedConfig` | JSON | см. `contracts/config.md` |

## Теги, реестры, ассеты

- `#spacereloaded:rocket_parts` += `stage_separator`; `#minecraft:mineable/pickaxe` += `stage_separator`.
- `data/spacereloaded/damage_type/reentry_heat.json`; лут `blocks/stage_separator.json`; рецепт `assembly_stage_separator.json`; достижение `staging.json` (родитель `rocketry`).
- Локализация: блок, достижение, HUD (ступень/Δv/углы/нагрев/нет гиродинов), сообщения отделения (5), ошибки сборки (3), скан (ступени, подъём, напор), терминал (наведение, прогноз), смерть от нагрева, клавиша.

## Сетевые пакеты (см. contracts/network.md)

| Пакет | Направление | Поля |
|---|---|---|
| `StageSeparatePayload` | C2S | пусто (действие = отделить активную ступень своего борта) |
| `ScanReportPayload` | S2C | + `stages: List<StageLine(index, deltaV, twr, hasEngines)>`, `ascentReached`, `ascentDeltaV`, `ascentApex`, `maxQ` |
| `CannonStatePayload` | S2C | + `guided`, `spreadBlocks`, `impactSpeedMs`, `impactEnergyMJ` |
