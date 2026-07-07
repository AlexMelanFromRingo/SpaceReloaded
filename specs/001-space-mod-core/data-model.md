# Data Model: SpaceReloaded (Phase 1)

Типы ядра (`core`) не знают о Minecraft; позиции — `long`-упакованные координаты или собственные записи. Fabric-слой маппит их на ванильные типы.

## core/geometry

```java
record Vec3l(int x, int y, int z)          // блочная позиция ядра; pack()/unpack() в long
record Vec3d(double x, double y, double z) // непрерывная величина (CoM, скорости)
```

## core/voxel

```java
interface VoxelView {                       // снапшот региона, потокобезопасный (immutable)
    GasPermeability permeabilityAt(long packedPos);
}
enum GasPermeability { OPEN, BLOCKED, VACUUM, OUT_OF_BOUNDS }
// VACUUM = заведомо «наружа» (космос, небо): контакт заполнения с ней — доказанная
// утечка (LEAK), в отличие от выхода за радиус (UNBOUNDED). OUT_OF_BOUNDS = стена.
final class ArrayVoxelGrid implements VoxelView  // для тестов и как целевой формат снапшота
```

## core/sealing

```java
record SealingRequest(long origin, int maxRadius, boolean diagnostic)
enum SealingStatus { SEALED, LEAK, UNBOUNDED, INVALID_ORIGIN }
record SealingResult(
    SealingStatus status,
    LongHashSet volume,       // посещённые OPEN-ячейки; при раннем выходе — частичный обход
    LongHashSet leakPoints,   // контакты с VACUUM; diagnostic=false ⇒ не более одной
    LongHashSet escapePoints, // выходы за радиус; diagnostic=false ⇒ не более одной
    int blocksVisited, long elapsedNanos)
final class GasFloodFill      // 26 направлений (DIRECTIONS_3D из RoomCheckerPlugin),
                              // чебышёвский радиус, early-exit при !diagnostic
```

Инварианты: `SEALED ⇒ leakPoints и escapePoints пусты`; `LEAK ⇒ ≥1 контакт с VACUUM` (при диагностике LEAK приоритетнее UNBOUNDED); `UNBOUNDED` отличим от `LEAK` (граница радиуса — не «дыра»); `INVALID_ORIGIN` — старт в стене. Семантика 26 направлений — конституция, принцип VIII.

## core/rocketry

```java
record PartProperties(double massKg, PartRole role,
    double thrustN, double ispSec, String fuelType, double tankCapacityL,
    double gyroTorqueNm, boolean seat)
enum PartRole { HULL, ENGINE, TANK, COMMAND, SEAT, GYRO, DECORATIVE }

record RocketStructure(Map<Vec3l, PartRef> parts)   // PartRef = id детали + ссылка на свойства
record RocketPerformance(
    double dryMassKg, double fuelMassKg, double totalMassKg,
    Vec3d centerOfMass, Vec3d centerOfThrust,
    double totalThrustN, double effectiveIspSec,
    Vec3d momentOfInertia,        // диагональная аппроксимация, точечные массы
    double twr, double deltaV,
    List<Warning> warnings)       // ASYMMETRIC_THRUST, TWR_BELOW_ONE, NO_FUEL, NO_COMMAND...

final class PerformanceCalculator   // structure + gravity -> RocketPerformance
final class FlightIntegrator        // step(FlightState, ControlInput, dt) -> FlightState
record FlightState(Vec3d pos, Vec3d vel, Quat orientation, Vec3d angVel,
                   double fuelMassKg, boolean enginesOn)
record ControlInput(double throttle, double pitchCmd, double rollCmd, boolean stabilize)
```

## core/ballistics

```java
record ProjectileSpec(double massKg, double dragCoeff)
final class BallisticIntegrator     // step(pos, vel, gravity, dt); eta(target) для упреждения
final class ImpactEnergy            // E=½mv² -> радиус разрушения ~ cbrt(E/E0), профиль кратера
```

## core/energy

```java
interface EnergyNode { long capacity(); long stored(); NodeKind kind(); } // SOURCE/SINK/BUFFER/WIRE
final class EnergyGraph   // топология сети, кэш связности, распределение за тик; событийная инвалидация
```

## mod-слой (Fabric): персистентные сущности

| Объект | Хранение | Ключевые поля |
|---|---|---|
| `AtmosphereControllerBlockEntity` | NBT block entity | статус зоны, объём (`long[]` упакованный), уровень атмосферы, энергобуфер |
| `ZoneManager` | runtime per-world | `Long2ObjectMap<zoneId>` обратный индекс позиция→зона для O(1) инвалидации |
| `RocketEntity` / `LanderEntity` | NBT entity (палитровая сериализация структуры) | структура (палитра BlockState + long-позиции + NBT блок-сущностей), `FlightState`, топливо по бакам, сиденья, владелец-lease |
| `LaunchPad` (multiblock) | NBT block entity | зона сборки, привязанная капсула |
| `OrbitalCannonBlockEntity` | NBT block entity | цель (dimension + BlockPos), заряд, боезапас |
| `KineticProjectileEntity` | NBT entity | `ProjectileSpec`, скорость, целевое измерение/точка, ETA, lease-id |
| `Planet` (датапак-реестр, Codec) | JSON `data/*/planets/*.json` | dimension, gravity, atmosphere{present,breathable,pressure}, temperatureK, solarEfficiency, orbitDimension, deltaVToOrbit, parentBody |

## Теги (расширяемость, FR-013)

- `#spacereloaded:airtight` — герметичные блоки (+ автоправило: полные непрозрачные кубы герметичны по умолчанию, конфиг)
- `#spacereloaded:rocket_parts` — детали, захватываемые сборкой
- `#spacereloaded:passes_gas` — принудительно проницаемые (решётки)
- Датапак-реестры: `spacereloaded:planets`, `spacereloaded:part_properties`, `spacereloaded:fuels`

## Сетевые пакеты (custom payloads)

| Пакет | Направление | Поля |
|---|---|---|
| `RocketControlC2S` | клиент→сервер | throttle, pitch, roll, stabilize, stage |
| `RocketStateS2C` | сервер→клиенты (tracking) | entityId, pos, vel, quat, fuel |
| `ZoneStatusS2C` | сервер→клиент | zoneId, status, atmosphereLevel |
| `CannonFireC2S` | клиент→сервер | cannonPos, targetDim, targetPos |
| `AssemblyResultS2C` | сервер→клиент | ok/ошибка + позиция проблемного блока + RocketPerformance |
