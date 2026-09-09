# Data Model: Межпланетная логистика

## Ядро (`core`, чистая Java)

### TransferOrbits (утилиты)
- `hohmannBurns(mu, r1, r2) → Burns(departureMs, arrivalMs)` — импульсы перигея/апогея между круговыми орбитами.
- `hohmannTotal(mu, r1, r2)` — сумма.
- `injectionFromParking(muBody, rPark, vInfinity)` — √(v∞² + 2μ/r) − √(μ/r).
- `interplanetaryDeparture(muSun, r1, r2, muDep, rParkDep)` — инъекция с v∞ = импульс отбытия Гомана.
- `interplanetaryArrival(muSun, r1, r2, muArr, rParkArr)` — захват в парковочную орбиту с v∞ = импульс прибытия.

### TransferBurn
- Вход: `StageLayout`, `double[] propellantKg`, `int activeStage`, `double deltaVMs`.
- Выход `Result(double[] propellantKg, int activeStage, boolean achieved, double shortfallMs, int stagesDropped)`.
- Инвариант: Σ топлива не растёт; `activeStage` не убывает; при `achieved` нехватка = 0.

### LandingBudget
- `touchdownSpeed(v0, g, h)`; `propulsiveDeltaV(vTouch, twr)` (TWR ≤ 1 → `POSITIVE_INFINITY`).

### MissionPlanner
- `Leg(FlightEnvironment env, double startY, double transitionY, double transferDeltaVMs, double cd, Landing landing)`; `Landing(double gravity, double arrivalHeightM, double arrivalSpeedMs)` (null — прибытие на платформу).
- `Reason { OK, ASCENT_UNREACHABLE, TRANSFER_SHORTFALL, LANDING_SHORTFALL, LANDING_TWR }`.
- `LegReport(AscentReport ascent, double transferDeltaVMs, double landingDeltaVMs, double landingTwr, Reason reason, double shortfallMs)`.
- `MissionReport(boolean feasible, Reason reason, double shortfallMs, List<LegReport> legs, double remainingDeltaVMs, double[] propellantAfter, int activeStageAfter, double firstAscentTimeS)`.
- `plan(layout, propellant, active, legs, marginFraction)`.
- `AscentSimulator.AscentReport` += `double[] propellantAfter`, `int activeStageAfter`; новая перегрузка `simulate(layout, propellant, activeStage, env, cd, startY, targetY)`.

### WetWorkshopPlanner (`core.station`)
- `Kind { SHELL, INTERIOR, KEEP }`; `Cell(long packedPos, Kind kind)`; `Plan(List<Cell> cells, int shellCount, int interiorCount, int keepCount)`.
- `plan(RocketStructure)`; `hatchCell(Plan, int x, int y, int z)` — клетка плана в (x,y,z), если SHELL.
- Правило: TANK/HULL с соседом вне структуры → SHELL; TANK/HULL без → INTERIOR; прочие роли → KEEP.

## Мод

### PlanetProfile.transfer: TransferSpec
- `TransferSpec(Map<Identifier, Double> deltaV)`; JSON: `"transfer_delta_v": {"spacereloaded:moon": 3955.0}`; `deltaVTo(Identifier)` → 0.0 по умолчанию.

### RocketEntity (новые поля)
- `boolean routeContinues` (NBT `route_continues`), `long relaunchAtTick` (NBT `relaunch_at`), `boolean transferWarned` (не сохраняется), synched `DATA_TRANSFER_DV` (float).
- Состояния: `parked` → (`launchUnmanned` ok) → `launched+autopilot` → `transition` (списание) → `parked` на платформе (`routeContinues` → таймер) → `launchUnmanned` … → прибытие спуском → `descentMode` → `land`.

### CargoTerminalBlockEntity
- Поля: `Identifier destinationDimension` (nullable), `GlobalPos pad` (nullable), `int frequency`, `Mode mode {HOLD, AUTO}`, `State state`, `String stateArg` (свободный текст цифр), `long dwellStart`, `int lastCargo`, `double lastFuel`, `UUID servicing`, `Set<UUID> seen` (ограниченный, для счётчика прибытий), `int departures`, `int arrivals`, `long lastLaunchTick`.
- `State { NO_PROGRAM, HOLD, WAITING_CRAFT, CREW_ABOARD, SERVICING, WAITING_WINDOW, NO_COVERAGE, REFUSED, LAUNCHED }`.
- Переходы: каждые `cargoLineCheckIntervalTicks`: выбрать борт → (нет) WAITING_CRAFT; (экипаж) CREW_ABOARD; (HOLD) HOLD; (снимок изменился) SERVICING с перезапуском; (выдержка прошла) планировщик → REFUSED(причина, м/с) | окно → WAITING_WINDOW(тики) | покрытие → NO_COVERAGE | старт → LAUNCHED, `departures++`.

### DockingPortBlock
- Состояния: `OPEN`, `CYCLING`, `POWERED` (от люка) + `FACING` (горизонтальное). Передняя клетка = `pos.relative(FACING)`.

### WetWorkshop (адаптер)
- `convert(ServerLevel, RocketEntity, BlockPos port, Direction facing) → Component`; порядок: проверки → груз → топливо → блоки → discard.

### Блоки/предметы
- `cargo_terminal` (EntityBlock), `docking_port` (HermeticHatch + FACING), `module_hull` (Block, airtight), `mars_ice` (Block, ELECTROLYZER_INPUT).

## Датапак (см. contracts/datapack.md)
- Профили планет: `transfer_delta_v`.
- Worldgen: `configured_feature/mars_ice.json`, `placed_feature/mars_ice.json`, `biome/mars_plains.json` (шаг underground_ores).
- Теги: `block/airtight` += module_hull, docking_port; `item/electrolyzer_input` += mars_ice; `minecraft:mineable/pickaxe` += 4 блока.
- Рецепты сборочного стола: `assembly_cargo_terminal`, `assembly_docking_port`.
- Достижения: `cargo_line`, `wet_workshop`, `mars`, `mars_return`.

## Конфиг (см. contracts/config.md)
`cargoLineCheckIntervalTicks`, `cargoLineDwellTicks`, `cargoLineDeltaVMarginPercent`, `autopilotRelaunchDelayTicks`, `transferDeltaVScale`, `wetWorkshopMaxResidualFraction`, `arrivalHeightM`.
