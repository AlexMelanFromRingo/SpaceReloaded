# Data Model: Лунная индустрия

## Ядро (`core.industry`, `core.worldgen`)

| Сущность | Поля | Правила |
|---|---|---|
| `RailLayout` | `int[] tiers` (по секциям от казённика), `boolean truncatedByLimit` | строится из массива клеток: 0 = не катушка → обрыв; длина ≤ предела |
| `MassDriverBallistics` (статические) | `muzzleVelocity(double[] accel, L_s)`, `excessVelocity(μ, r_p, Δv)`, `requiredVelocity(g, R, h_park, Δv)`, `shotEnergyJ(m, v, η)`, `dynamicPressure(ρ, v)`, `heatFlux(ρ, v, r_n)`, `missingSections(v_req, v_max, a_best, L_s)` | все величины СИ; Δv ≤ 0 → v∞ = 0 |
| `LaunchSolution` (record) | `vMax, vReq, massKg, energyJ, energyUnits, dynamicPressurePa, heatFlux, Reason reason, int missingSections` | `Reason`: OK, NO_RADIUS, RAIL_SHORT, ATMOSPHERE_PRESSURE, ATMOSPHERE_HEAT, NO_ENERGY, NO_TARGET, NO_POD, MUZZLE_BLOCKED, RAIL_UNLOADED, RECHARGING |
| `CatcherOdds` (статические) | `captureRadius(n, r0, k, rMax)`, `captureProbability(r, σ)`, `sampleOffset(seed, σ) → double[2]` | σ > 0; зерно → детерминированный Box–Muller |
| `RegolithYield` (record) | `oxygen, ironDust, titaniumChance, slag`; `roll(RandomGenerator) → Output(oxygen, iron, titanium, slag)` | шанс в [0,1] |
| `ReactorShell` | `validate(Cell[27], int controllerIndex) → Result(ok, int badIndex, Cell expected)` | индексация x+3z+9y; центр 13 = AIR; контроллер — центр боковой грани (4, 10, 12, 14, 16, 22 кроме верх/низ: 10, 12, 14, 16) |
| `LavaTubeLayout` | `seed, regionX, regionZ`; `segments: List<Segment(x0,z0,x1,z1,depth0,depth1,halfW,halfH)>`, `skylights: List<(x,z,r)>`; `contains(x,y,z,surfaceY)`, `isSkylight(x,z)` | регион 128; глубина крыши ≥ 4 вне окон |
| `ShelterRule` | `isSheltered(boolean skyVisible, int rockAbove, int minRock)` | — |

## Мод

| Сущность | Хранение | Поля |
|---|---|---|
| `MassDriverBreechBlockEntity` | NBT | инвентарь 18 (0 — капсула), цель (`dimension`, `pos`), `railSections[]` (тиры), `lastShotTick`, `sledReturnUntil`, `lastRedstone`, кэш отчёта |
| `CapacitorBlockEntity` | NBT (`MachineBlockEntity`) | energy |
| `MassCatcherBlockEntity` | NBT | инвентарь 27, `netBlocks`, `caught`, `lost`, `lastEvent` (ключ + аргументы) |
| `RegolithReactorBlockEntity` | NBT | инвентарь 5, energy, `formed`, `badCell`, `progress`, `oxygenBuffer` |
| `PodTransitState` | SavedData (Codec, оверворлд) | `List<PodTransit(UUID id, List<ItemStack> contents, ResourceKey<Level> origin, ResourceKey<Level> targetDim, BlockPos targetPos, long arrivalTick, long seed, Optional<UUID> shooter, boolean ticketed)>` |
| `CargoPodEntity` | не сохраняется | `life` |
| Блок-состояния | — | `CoilBlock`: `IN_RAIL`, `AXIS`(X/Z); казённик/контроллер: `FACING`, `LIT`(контроллер); `MassCatcher`: — |

## Переходы состояний

- Казённик: `UNFORMED → READY → (выстрел) → RECHARGING (sledReturnUntil) → READY`; любое изменение линии → пересчёт.
- Капсула: `в казённике → PodTransit (ticketed=false) → (arrival−40) ticketed=true → (arrival) CAUGHT | LOST → удалена`.
- Реактор: `UNFORMED ↔ FORMED(IDLE ↔ RUNNING ↔ BUFFER_FULL)`; разформирование сбрасывает `progress`.

## Профиль планеты (расширения)

- `TransferSpec`: `body_radius` (м, 0 = нет), `parking_altitude` (м, 100 000 по умолчанию).
- `ThermalSpec`: `shelter_temperature` (°C, по умолчанию = `temperature`).
