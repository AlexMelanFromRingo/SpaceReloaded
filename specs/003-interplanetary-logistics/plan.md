# Implementation Plan: Межпланетная логистика — cargo-линии, wet workshop, марсианский ISRU-цикл

**Branch**: `003-interplanetary-logistics` | **Date**: 2026-09-07 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/003-interplanetary-logistics/spec.md`

## Summary

Три пласта эндгейма поверх «Полёта 2.0»: (1) честная цена перелёта — таблица Δv в профиле тела (Гоман/патч-коники), списание топлива по Циолковскому с учётом ступеней при каждом переходе, планировщик бюджета всего маршрута (подъём численно, перелёт по таблице, пропульсивная посадка с TWR) и запрет беспилотного старта при нехватке; (2) грузовой терминал — блок-автомат линии «пэд A ↔ пэд B» (выдержка по грузу/топливу, проверки бюджета, окна и покрытия, автозапуск, пересадка автопилота на промежуточной орбите, статус в терминале, Jade и ЦУПе); (3) wet workshop — стыковочный порт (люк с функцией стыковки) превращает припаркованный пустой борт в герметичный модуль из блоков (оболочка → «обшивка модуля», интерьер → воздух, люк напротив порта), герметичность подтверждается штатным флудфиллом; (4) замыкание марсианского цикла: подповерхностный марсианский лёд, достижения «Красная планета»/«Обратный билет».

Вся физика — в чистом ядре `core` с JUnit против аналитики; мод-слой — адаптеры, блоки, сеть, документация, E2E-стенд.

## Technical Context

**Language/Version**: Java 25 (toolchain `~/.sdkman/candidates/java/25.0.3-tem`), Gradle, Fabric Loom 26.2

**Primary Dependencies**: Minecraft 26.2, Fabric Loader + Fabric API, Team Reborn Energy 5, Jade (мягкая зависимость, entrypoint `jade`), JUnit 5 (core)

**Storage**: NBT сущностей и блок-сущностей (`ValueInput/ValueOutput`), SavedData `SpaceNetworkState` (без изменений), датапак-реестр планет (`spacereloaded/planets/*.json`, синхронизируется на клиент)

**Testing**: `./gradlew build` (74 теста ядра + новые ≈ 25), `timeout 570 ./gradlew :mod:runClientGametest` (один `runTest`, 25 сценариев + 2 новых + проверка льда)

**Target Platform**: сервер/клиент Fabric, WSL2 для стенда (DISPLAY=:0)

**Project Type**: многомодульный Gradle: `core` (чистая Java) + `mod` (Fabric)

**Performance Goals**: планировщик маршрута из 3 хопов для стека 500 деталей — < 300 мс (SC-003); терминал — одна проверка раз в 20 тиков, планировщик только после выдержки (не каждый тик); HUD-строка перелёта — без симуляции (только таблица + Циолковский), раз в 10 тиков

**Constraints**: главный поток ≤ 1 мс/тик в штатном режиме; лимит 16 полей RecordCodecBuilder (профиль планеты уже на пределе → вложенный MapCodec); стенд: игрок не покидает оверворлд, сущности в чужом измерении не отслеживаются → сценарии линии проверяют отказ/автозапуск на стороне старта, прибытие — тестами ядра

**Scale/Scope**: ядро +6 классов/+7 тестов; мод +4 блока (терминал, порт, обшивка, марсианский лёд), +2 BE-класса, +1 пакет `logistics`, изменения `RocketEntity`/`ModRegistries`/HUD/ЦУП/Jade/конфига; датапак: профили планет, worldgen, теги, рецепты, достижения, лут; локализация 3 языка; документация 6 файлов + регенерация сайта

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Принцип | Соответствие |
|---|---|
| I. Физика прежде всего | Δv перелётов — из уравнений Гомана/патч-коник по реальным μ и радиусам (тест ядра сверяет JSON с формулой); списание — Циолковский по ступеням; посадка — v·TWR/(TWR−1) с выводом в комментарии; упрощения (аэрозахват бесплатен, мгновенное списание, обломки перелёта не спавнятся) документированы рядом с формулами и в ADDONS. `transferDeltaVScale` явно помечен балансовым |
| II. Ядро без движка | `TransferOrbits`, `TransferBurn`, `LandingBudget`, `MissionPlanner`, `WetWorkshopPlanner`, расширение `AscentSimulator` — без импортов Minecraft; мод-адаптеры `MissionPlanning`, `WetWorkshop` собирают входы из реестров |
| III. Событийность | терминал тикает раз в `cargoLineCheckIntervalTicks`, планировщик запускается только после выдержки и не чаще интервала; HUD-цена перелёта — раз в 10 тиков без симуляции; конверсия — по действию игрока |
| IV. Дисциплина потоков | все мутации мира (запуск, установка программы, конверсия) — в главном потоке из тикера BE / обработчика клика; планировщик синхронный (бюджет < 300 мс на 500 деталей — приемлемо для редкого события; стек обычного борта — единицы мс) |
| V. Межпространственная целостность | переход по-прежнему держит билеты TRANSITION вокруг старта и прибытия; отказ перелёта не телепортирует и не теряет борт; пересадка перезапускает борт из припаркованного состояния |
| VI. Сервер авторитетен | вся логика линии, списания, конверсии — на сервере; клиент получает только synched data HUD и текст статусов |
| VII. Тесты ядра | JUnit до/вместе с реализацией: Гоман vs эталон, Циолковский, посадка, планировщик (отказ/одобрение/Марс-возврат), план оболочки + флудфилл, бюджет производительности |
| VIII. Осознанный хардкор | нехватка Δv = честный отказ/незавершённый перелёт; топливо при конверсии теряется; линия без окна сжигает топливо — предупреждение, но не «чинится» |
| Конфиг | все лимиты — поля `SpaceReloadedConfig` с валидацией |
| Расширяемость | таблица перелётов, лёд, обшивка, порт — данные/теги датапака; линия работает с любыми маяками/планетами аддонов |

Отступлений нет.

## Project Structure

### Documentation (this feature)

```text
specs/003-interplanetary-logistics/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── config.md
│   ├── datapack.md
│   └── network.md
├── checklists/requirements.md
└── tasks.md   (/speckit-tasks)
```

### Source Code (repository root)

```text
core/src/main/java/org/alex_melan/spacereloaded/core/
├── rocketry/
│   ├── AscentSimulator.java      # + перегрузка с активной ступенью; AscentReport += propellantAfter, activeStageAfter
│   ├── TransferOrbits.java       # NEW: Гоман, инъекция из парковочной орбиты (патч-коники)
│   ├── TransferBurn.java         # NEW: списание Δv по Циолковскому через ступени
│   ├── LandingBudget.java        # NEW: скорость касания и Δv пропульсивной посадки
│   └── MissionPlanner.java       # NEW: бюджет маршрута по хопам (Leg/LegReport/MissionReport/Reason)
└── station/
    └── WetWorkshopPlanner.java   # NEW: оболочка/интерьер/сохранить + клетка люка
core/src/test/java/org/alex_melan/spacereloaded/core/
├── rocketry/{TransferOrbitsTest, TransferBurnTest, LandingBudgetTest, MissionPlannerTest, AscentSimulatorTest(+)}.java
├── rocketry/FlightPerformanceBudgetTest.java (+ планировщик 3 хопа < 300 мс)
└── station/WetWorkshopPlannerTest.java

mod/src/main/java/org/alex_melan/spacereloaded/
├── logistics/                    # NEW пакет
│   ├── MissionPlanning.java      # адаптер: маршрут по реестру планет → List<Leg> → MissionReport (+ текст причины)
│   ├── CargoTerminalBlock.java
│   ├── CargoTerminalBlockEntity.java
│   ├── WetWorkshop.java          # адаптер конверсии: план → блоки мира, люк, топливо, груз
│   └── DockingPortBlock.java     # extends HermeticHatchBlock + FACING; Sneak+ПКМ → WetWorkshop.convert
├── rocket/RocketEntity.java      # списание при transition(), HUD DATA_TRANSFER_DV, планировщик в launchUnmanned, пересадка
├── rocket/StageSeparation.java   # + upperStack(...) для отбрасывания сгоревших в перелёте ступеней без обломка
├── rocket/MissionControlBlock.java  # + терминалы в радиусе
├── registry/ModRegistries.java   # TransferSpec MAP_CODEC → PlanetProfile.transfer()
├── registry/{ModBlocks, ModBlockEntities, ModTags}.java  # cargo_terminal, docking_port, module_hull, mars_ice
├── config/SpaceReloadedConfig.java
├── compat/JadePlugin.java        # данные терминала
└── network/… (без новых пакетов: HUD через synched data)
mod/src/client/java/org/alex_melan/spacereloaded/
├── client/gui/RocketHud.java     # строка перелёта
└── compat/JadeClientPlugin.java  # строки терминала
mod/src/main/resources/
├── data/spacereloaded/spacereloaded/planets/{earth,earth_orbit,moon,mars,asteroid_belt}.json  # transfer_delta_v
├── data/spacereloaded/worldgen/{configured_feature,placed_feature}/mars_ice.json, biome/mars_plains.json
├── data/spacereloaded/tags/{block/airtight.json, item/electrolyzer_input.json, block/rocket_parts? (нет)}, data/minecraft/tags/block/mineable/pickaxe.json
├── data/spacereloaded/recipe/assembly_{cargo_terminal,docking_port}.json
├── data/spacereloaded/advancement/{cargo_line,wet_workshop,mars,mars_return}.json
├── data/spacereloaded/loot_table/blocks/{cargo_terminal,docking_port,module_hull,mars_ice}.json
├── assets/spacereloaded/{blockstates,models/block,items,textures/block}/… (4 блока)
└── assets/spacereloaded/lang/{en_us,ru_ru,uk_ua}.json
mod/src/gametest/java/.../SpaceReloadedClientGameTest.java  # testCargoLine (BX+720), testWetWorkshop (BX+760), лёд в testPlanetTerrain
tools/gen_textures.py             # cargo_terminal, docking_port, module_hull, mars_ice
tools/gen_site.py + docs/*.html   # регенерация
README.md, README.ru.md, docs/GUIDE.ru.md, docs/ADDONS.md, specs/001-space-mod-core/{progression.md, inspiration-backlog.md, tasks.md, api-notes-26.2.md}
```

**Structure Decision**: сохраняется двухмодульная структура; новый пакет ядра `core.station` (планировщик оболочки не относится к rocketry) и пакет мода `logistics` (терминал, порт, адаптеры планирования/конверсии), чтобы `rocket/` не разрастался.

## Ключевые проектные решения

### D20. Таблица перелётов — данные профиля, выведенные из небесной механики (FR-100, FR-101)

Профиль планеты получает вложенную запись `transfer_delta_v: { "<entry id цели>": м/с }` через `MapCodec` (лимит 16 полей `RecordCodecBuilder` уже исчерпан; тот же приём, что `AtmosphereSpec`). Значения по умолчанию для тел мода считаются в тесте ядра из `TransferOrbits` (Гоман между круговыми орбитами + инъекция из парковочной орбиты по патч-коникам: Δv = √(v∞² + 2μ/r) − √(μ/r)) и сверяются с JSON (допуск 1 %). Упрощения: круговые компланарные орбиты, аэрозахват у тел с атмосферой бесплатен (прямой вход), фазирование — механика окон 001. Пары без записи стоят 0 (совместимость с аддонами). `transferDeltaVScale` в конфиге — балансовый множитель, документирован как не-физика.

### D21. Списание при переходе — Циолковский через ступени (FR-102, FR-103)

`TransferBurn.apply(layout, propellant[], active, Δv)`: для активной ступени m₀ = масса «активного вида» (сухая масса оставшегося стека + всё топливо), v_e = Isp_eff·g₀ (эффективный Isp вида из `PerformanceCalculator`); доступное Δv_s = v_e·ln(m₀/(m₀ − p_s)). Хватает — p_after = m₀·e^(−Δv/v_e) − (m₀ − p_s); нет — ступень сгорает целиком, отбрасывается (следующий вид без её сухой массы), остаток Δv переносится. В `RocketEntity.transition()` списание выполняется до телепорта; сгоревшие ступени убираются через `StageSeparation.upperStack` без спавна обломка (задокументированное упрощение: обломок остаётся на орбите перелёта). При нехватке — переход не выполняется, пилоту одно предупреждение за подъём (`transferWarned`, сбрасывается ниже высоты перехода − 20 как `windowWarned`).

### D22. Бюджет посадки (FR-105)

`LandingBudget`: v_кас = √(v₀² + 2gh) (v₀ = 5 м/с — скорость прибытия, h — высота прибытия из конфига, общая с `transition()`), Δv = v_кас·TWR/(TWR−1) — вывод: торможение постоянной тягой a = a_т − g, время t = v/(a_т − g), потрачено a_т·t. TWR ≤ 1 → посадка невозможна (причина LANDING_TWR). Атмосферное торможение не учитывается — консервативно, документировано.

### D23. Планировщик маршрута (FR-105…FR-107)

`MissionPlanner.plan(layout, propellant[], active, List<Leg>, margin)`: последовательно по хопам — `AscentSimulator` (расширен: стартовая активная ступень, возврат остатка топлива и активной ступени) от стартовой высоты до высоты перехода в среде хопа; затем `TransferBurn` на Δv перелёта·(1+margin); на финальном хопе со спуском — `TransferBurn` на Δv посадки·(1+margin) с TWR оставшегося стека при гравитации цели. Отчёт: выполнимость, причина (`ASCENT_UNREACHABLE | TRANSFER_SHORTFALL | LANDING_SHORTFALL | LANDING_TWR | OK`), нехватка м/с, отчёты хопов, остаток Δv, ступени, время первого подъёма. Мод-адаптер `MissionPlanning` строит хопы по `Navigation.route` и профилям (гравитация + аэропрофиль без загрузки измерений), стартовая высота промежуточных хопов — высота орбитальной платформы. Любой беспилотный старт (`launchUnmanned`: пульт, терминал, пересадка) проходит планировщик; пилотируемый — нет (FR-107).

### D24. Грузовой терминал (FR-110…FR-118)

Блок + BE в `logistics`; тикер раз в `cargoLineCheckIntervalTicks`. Зона — как у погрузчика (±8 по горизонтали, 48 вверх). Программа хранится компонентами того же смысла, что у предмета (цель — id измерения, маяк, канал); установка в борт — через существующий `RocketEntity.installProgram` (терминал собирает временный стек программы → без дублирования логики). Обслуженность — снимок (число предметов груза, кг топлива, id борта); изменение перезапускает выдержку. Перед стартом: планировщик → окно (на `now + время подъёма`) → покрытие (`Logistics.coverageSatisfied`) → `installProgram` → `launchUnmanned`. Состояние — enum + числовые аргументы, сохраняется в NBT; ПКМ — статус в чат (как ЦУП v1), Sneak+ПКМ — AUTO/HOLD; Jade показывает состояние и счётчики; ЦУП перечисляет терминалы из блок-сущностей чанков в радиусе 64.

### D25. Пересадка автопилота (FR-116)

`RocketEntity`: флаг `routeContinues` (unmanned-автопилот, финальная цель дальше хопа) и `relaunchAtTick`. В `postArrival(parked)` при `routeContinues` ставится `relaunchAtTick = now + autopilotRelaunchDelayTicks`; в `tick()` припаркованный борт с наступившим сроком вызывает `tryLaunchUnmanned` (планировщик/окно/покрытие); отказ — повтор через задержку (лог раз в отказ). Оба поля в NBT.

### D26. Wet workshop (FR-120…FR-128)

`WetWorkshopPlanner.plan(structure)`: роль TANK/HULL (грузовой отсек — hull) → SHELL, если хотя бы один из 6 соседей вне структуры, иначе INTERIOR; остальные роли → KEEP. `hatchCell(plan, portFrontCell)` — клетка SHELL, прилегающая к передней грани порта. Тест герметичности в ядре: план 5×5×5 → `ArrayVoxelGrid` (SHELL/KEEP — непроницаемо, INTERIOR — воздух, люк закрыт) → `GasFloodFill` из интерьера → sealed. `DockingPortBlock extends HermeticHatchBlock` + `FACING` (ПКМ — люк, Sneak+ПКМ — конверсия). `WetWorkshop.convert`: проверки (припаркован, без пассажиров, не обломок, остаток ≤ `wetWorkshopMaxResidualFraction`·ёмкости, все клетки-цели — воздух, клетка люка есть), затем: груз → `popResource` у порта, топливо стравлено (сообщение), блоки: SHELL → `module_hull`, INTERIOR → воздух, KEEP → прежнее состояние (баки KEEP не бывает), люк → `hermetic_hatch` закрытый; сущность удаляется. Работает в любом измерении.

### D27. Марсианский лёд и достижения (FR-130…FR-133)

Блок `mars_ice` (свойства как у `moon_ice`), ore-feature в `minecraft:red_sandstone` (базовый блок Марса) размером 9, 6 жил на чанк, высоты 4–90, подключён в `mars_plains`; тег `electrolyzer_input`; лут — сам блок; `mineable/pickaxe`. Достижения `mars` (changed_dimension → mars, родитель `orbit`) и `mars_return` (changed_dimension from mars → earth_orbit, родитель `mars`); `cargo_line` (терминал в инвентаре, родитель `docking`), `wet_workshop` (порт в инвентаре, родитель `sealed`).

### D28. HUD и сообщения (FR-104)

`DATA_TRANSFER_DV` (float) — цена следующего хопа (перелёт + посадка при спуске, по таблице и `LandingBudget` с TWR текущего вида) раз в 10 тиков вместе с `DATA_DELTA_V`; HUD-строка `hud.spacereloaded.rocket.transfer` красная при нехватке. Тексты отказов планировщика — ключи `message.spacereloaded.mission.<reason>` с аргументами (нужно/есть/нехватка).

## Testing (принцип VII)

- Ядро: `TransferOrbitsTest` (Гоман LEO→GEO 3.89 км/с ±1 %; v∞ = 0 → (√2−1)·v_circ; Земля→Марс 2945/2649; JSON-значения Луна 3955, Марс 3613, возвраты 822/2103, пояс 9650/4800 ±1 %), `TransferBurnTest` (одна ступень — точный Циолковский; переход через ступень; нехватка), `LandingBudgetTest` (TWR 2 → 2·v; TWR→∞ → v; TWR ≤ 1 → бесконечность), `MissionPlannerTest` (1 бак к Луне — TRANSFER_SHORTFALL с нехваткой > 1000; 3 бака — OK; метанокс на Марсе → орбита — OK; слабый двигатель — LANDING_TWR; запас применён), `AscentSimulatorTest` (+ остаток топлива согласован с Δv), `WetWorkshopPlannerTest` (3×3×3 → 26/1; 5×5×5 → 98/27; столбик; двигатели KEEP; люк; флудфилл sealed/leak при открытом люке), `FlightPerformanceBudgetTest` (+ 3 хопа 500 деталей < 300 мс).
- Стенд: `testCargoLine` (BX+720): терминал HOLD→AUTO, программа на орбитальный маяк, борт с пустым баком → состояние отказа с «м/с»/TWR; заправка (`refuel`) → после выдержки борт стартует (launched, departures = 1); борт с пассажиром игнорируется. `testWetWorkshop` (BX+760): стек 5×5 (двигатели, 5 слоёв баков, командный модуль), порт, конверсия через `WetWorkshop.convert` сервер-хуком → 27 воздух / 98 обшивка / люк / двигатели; контроллер внутри → SEALED. Лёд: скан 3×3 чанков Марса в `testPlanetTerrain`.
- Сборка: `JAVA_HOME=~/.sdkman/candidates/java/25.0.3-tem ./gradlew build`, стенд в foreground `timeout 570 ./gradlew :mod:runClientGametest`.

## Риски и меры

| Риск | Мера |
|---|---|
| Реальные Δv (3.6–4 км/с) ужесточают баланс для существующих сохранений | документация с таблицей и примерами стеков (3 бака = Луна); `transferDeltaVScale` для серверов; пилотируемые старты не блокируются |
| Терминал отправляет чужой борт | по умолчанию HOLD; экипажный борт игнорируется; состояние видно в Jade/ЦУПе |
| Планировщик дорог для больших стеков | запускается только после выдержки и раз в интервал; бюджет проверен тестом |
| Стенд не видит прибытие в другое измерение | сценарий проверяет отказ/старт/счётчики на стороне старта; списание и пересадка — тесты ядра + логика без измерений |
| Конверсия портит существующие блоки | предварительная проверка всех клеток на воздух, отказ до мутаций |
| Порт как подкласс люка ломает группы люков | группа собирается по `instanceof HermeticHatchBlock` — порт участвует в группах как люк; FACING не влияет |

## Constitution Check (после Phase 1)

Повторная проверка по data-model/contracts: новые сущности ядра чистые; все мутации — главный поток; лимиты в конфиге (`contracts/config.md`); датапак-контракт задокументирован (`contracts/datapack.md`); тесты перечислены. Отступлений нет — план готов к `/speckit-tasks`.
