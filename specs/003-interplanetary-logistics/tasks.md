# Tasks: Межпланетная логистика — cargo-линии, wet workshop, марсианский ISRU-цикл

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/, quickstart.md из `/specs/003-interplanetary-logistics/`

**Prerequisites**: plan.md (обязательно), spec.md (истории), data-model.md, contracts/, research.md

**Tests**: тесты ядра ОБЯЗАТЕЛЬНЫ (конституция VII): JUnit-задача каждого класса ядра стоит перед его реализацией и обязана быть красной до неё. Стенд (`mod/src/gametest`) получает сценарии терминала и wet workshop, проверку льда.

**Organization**: фазы Setup → Foundational (ядро планирования) → US1 цена перелёта → US2 терминал → US3 wet workshop → US4 Марс → Polish (стенд, документация, локализация, финальная проверка). Нумерация T300+ (002 закончилась на T278).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: можно параллельно (разные файлы, нет зависимостей от незавершённых задач)
- **[Story]**: история спеки (US1…US4); у Setup/Foundational/Polish метки нет
- Пути — от корня репозитория; `core/…` — чистая Java без Minecraft, `mod/…` — Fabric-слой

## Path Conventions

- Ядро: `core/src/main/java/org/alex_melan/spacereloaded/core/…`, тесты `core/src/test/java/…`
- Мод: `mod/src/main/java/org/alex_melan/spacereloaded/…`, клиент `mod/src/client/java/…`, ресурсы `mod/src/main/resources/…`, стенд `mod/src/gametest/java/…/SpaceReloadedClientGameTest.java`
- Сборка: `JAVA_HOME=~/.sdkman/candidates/java/25.0.3-tem ./gradlew …`; стенд в foreground `timeout 570 ./gradlew :mod:runClientGametest`

---

## Phase 1: Setup (общая инфраструктура)

**Purpose**: конфиг, таблица перелётов в профиле, данные планет — общие для всех историй

- [X] T300 Добавить поля конфига по `contracts/config.md` (cargoLineCheckIntervalTicks=20, cargoLineDwellTicks=200, cargoLineDeltaVMarginPercent=5.0, autopilotRelaunchDelayTicks=100, transferDeltaVScale=1.0, wetWorkshopMaxResidualFraction=0.05, arrivalHeightM=180) с Javadoc-обоснованием и проверками диапазонов в `validate()` в `mod/src/main/java/org/alex_melan/spacereloaded/config/SpaceReloadedConfig.java`
- [X] T301 `TransferSpec(Map<Identifier, Double> deltaV)` с `MAP_CODEC` (`transfer_delta_v`, unboundedMap, по умолчанию пусто) и `deltaVTo(Identifier)`; 17-е поле `PlanetProfile.transfer` через вложенный MapCodec (лимит 16) + `transferDeltaVTo(Identifier)` в `mod/src/main/java/org/alex_melan/spacereloaded/registry/ModRegistries.java`
- [X] T302 [P] Таблица перелётов в профилях по `contracts/datapack.md`: `earth_orbit.json` (earth 100, moon 3955, mars 3613, asteroid_belt 9650), `moon.json` (earth_orbit 822), `mars.json` (earth_orbit 2103), `asteroid_belt.json` (earth_orbit 4800), `earth.json` (earth_orbit 0 явно) в `mod/src/main/resources/data/spacereloaded/spacereloaded/planets/`

---

## Phase 2: Foundational (ядро планирования — блокирует истории)

**Purpose**: небесная механика, списание по ступеням, посадка, планировщик маршрута

**⚠️ CRITICAL**: без этих классов истории US1/US2/US4 не компилируются

- [X] T303 [P] Тест `TransferOrbitsTest`: Гоман LEO(300 км)→GEO = 3.89 км/с ±1 %; инъекция при v∞=0 = (√2−1)·v_circ; Земля→Марс гелиоцентрические импульсы 2945/2649 ±1 %; JSON-значения мода (3955 = TLI 3133 + LOI 822 с v∞ 832; 3613; 822; 2103; 9650 = 4850 + 4800; 4800) сходятся с формулами ±1 % в `core/src/test/java/org/alex_melan/spacereloaded/core/rocketry/TransferOrbitsTest.java`
- [X] T304 `TransferOrbits`: `hohmannBurns`, `hohmannTotal`, `injectionFromParking`, `interplanetaryDeparture`, `interplanetaryArrival`, `lunarTransfer(muEarth, rPark, rMoonOrbit, muMoon, rParkMoon) → Burns(TLI, LOI)` с формулами и упрощениями в Javadoc в `core/src/main/java/org/alex_melan/spacereloaded/core/rocketry/TransferOrbits.java`
- [X] T305 [P] Тест `TransferBurnTest`: одна ступень — остаток равен m₀·e^(−Δv/ve) − m_dry ±0.1 %; Δv больше ступени — ступень сгорает, отбрасывается, остаток списан с верхней; нехватка — `achieved=false`, `shortfallMs` = Δv − доступное; Δv=0 — без изменений в `core/src/test/java/org/alex_melan/spacereloaded/core/rocketry/TransferBurnTest.java`
- [X] T306 `TransferBurn.apply(layout, propellant[], active, deltaV) → Result(propellant[], activeStage, achieved, shortfallMs, stagesDropped)` через `StageLayout.activeView` + `PerformanceCalculator` (effectiveIspSec, dry/total mass) в `core/src/main/java/org/alex_melan/spacereloaded/core/rocketry/TransferBurn.java`
- [X] T307 [P] Тест `LandingBudgetTest`: `touchdownSpeed(5, 3.72, 180)` = √(25+1339.2); TWR 2 → 2·v; TWR 3 → 1.5·v; TWR → ∞ → v; TWR ≤ 1 → бесконечность в `core/src/test/java/org/alex_melan/spacereloaded/core/rocketry/LandingBudgetTest.java`
- [X] T308 `LandingBudget.touchdownSpeed`, `propulsiveDeltaV` с выводом формулы (торможение постоянной тягой: t = v/(a_т − g), Δv = a_т·t) в `core/src/main/java/org/alex_melan/spacereloaded/core/rocketry/LandingBudget.java`
- [X] T309 [P] Расширить `AscentSimulatorTest`: перегрузка со стартовой активной ступенью; `propellantAfter`/`activeStageAfter` согласованы (Σ сожжённого = Δv_spent по Циолковскому ±2 %); старт с 1-й ступени двухступенчатого стека в `core/src/test/java/org/alex_melan/spacereloaded/core/rocketry/AscentSimulatorTest.java`
- [X] T310 `AscentSimulator`: перегрузка `simulate(layout, propellant, activeStage, env, cd, startY, targetY)`; `AscentReport` += `propellantAfter`, `activeStageAfter`; старая сигнатура делегирует в `core/src/main/java/org/alex_melan/spacereloaded/core/rocketry/AscentSimulator.java`
- [X] T311 [P] Тест `MissionPlannerTest`: 1 бак керолокс (2886 м/с) → орбита→Луна (3955) = `TRANSFER_SHORTFALL`, нехватка > 1000; 3 бака (4314) → OK, остаток > 0; метанокс 1 бак на Марсе (g 3.72, ρ 0.02/150, старт 64 → 240, перелёт 2103, посадка нет — платформа) → OK; тяжёлый стек с TWR < 1 при g цели → `LANDING_TWR`; запас 5 % увеличивает списание; 2 хопа (Земля→орбита→Луна) с промежуточным стартом с платформы в `core/src/test/java/org/alex_melan/spacereloaded/core/rocketry/MissionPlannerTest.java`
- [X] T312 `MissionPlanner` (`Leg`, `Landing`, `Reason`, `LegReport`, `MissionReport`, `plan(...)`) по data-model: подъём → списание перелёта·(1+margin) → посадка·(1+margin) с TWR при гравитации цели в `core/src/main/java/org/alex_melan/spacereloaded/core/rocketry/MissionPlanner.java`
- [X] T313 Бюджет производительности: планировщик 3 хопа для стека 500 деталей < 300 мс (мягко 600) в `core/src/test/java/org/alex_melan/spacereloaded/core/rocketry/FlightPerformanceBudgetTest.java`

**Checkpoint**: `./gradlew :core:test` зелёный — ядро планирования готово

---

## Phase 3: User Story 1 — Честная цена перелёта (Priority: P1) 🎯 MVP

**Goal**: списание Δv при переходе (по ступеням), предупреждение при нехватке, HUD-строка перелёта, планировщик в беспилотном старте, пересадка автопилота

**Independent Test**: сценарий спеки US1 — ракета с 3 баками переходит к Луне и теряет топливо по Циолковскому; с 1 баком — не переходит и получает предупреждение; беспилотный старт отклоняется с цифрами

- [X] T314 [US1] `StageSeparation.upperStack(RocketData, StageLayout, int newActive) → RocketData` (нормализованный верхний стек без обломка; вынести `bounds/normalize/dryMass` в общие статические помощники) в `mod/src/main/java/org/alex_melan/spacereloaded/rocket/StageSeparation.java`
- [X] T315 [US1] Адаптер `MissionPlanning`: `plan(ServerLevel, RocketEntity, Identifier finalTarget, GlobalPos pad)` → хопы по `Navigation.route` и профилям (среда из gravity+aero без загрузки измерений; стартовая высота 0-го хопа — Y борта, промежуточных — `PlanetManager.ORBIT_PLATFORM_Y`; перелёт × `transferDeltaVScale`; посадка на финальном спуске с `arrivalHeightM`, v₀ 5); `describe(MissionReport) → Component` с ключами `message.spacereloaded.mission.<reason>` в `mod/src/main/java/org/alex_melan/spacereloaded/logistics/MissionPlanning.java`
- [X] T316 [US1] `RocketEntity.transition()`: списание `TransferBurn` до телепорта (стоимость × scale); нехватка → `transferWarned` + оверлей `message.spacereloaded.rocket.transfer_short` и возврат без перехода; сгоревшие ступени — `applyRemainingStack(upperStack…)` без обломка; сброс `transferWarned` ниже высоты перехода − 20 в `mod/src/main/java/org/alex_melan/spacereloaded/rocket/RocketEntity.java`
- [X] T317 [US1] `RocketEntity`: `DATA_TRANSFER_DV` (цена следующего хопа: перелёт + посадка при спуске через `LandingBudget` с TWR текущего вида при гравитации цели), обновление раз в 10 тиков; клиентский `clientTransferDeltaV()`; `arrivalHeightM` из конфига вместо константы 180 в `mod/src/main/java/org/alex_melan/spacereloaded/rocket/RocketEntity.java`
- [X] T318 [US1] `RocketEntity`: `tryLaunchUnmanned(level) → LaunchResult(ok, message)` с планировщиком (FR-107) и проверкой окна на `now + время подъёма`; `launchUnmanned` делегирует; поля `routeContinues`/`relaunchAtTick` (NBT), установка в `transition()` для беспилотных с целью дальше хопа, `postArrival(parked)` ставит таймер, `tick()` перезапускает по сроку с повтором при отказе (лог) в `mod/src/main/java/org/alex_melan/spacereloaded/rocket/RocketEntity.java`
- [X] T319 [P] [US1] HUD-строка `hud.spacereloaded.rocket.transfer` (нужно/есть, красная при нехватке, скрыта при 0) в `mod/src/client/java/org/alex_melan/spacereloaded/client/gui/RocketHud.java`
- [X] T320 [US1] Сборка `./gradlew build` зелёная; ручная проверка логики отказа через существующий сценарий `testScanAndProgram` (программа устанавливается как прежде)

**Checkpoint**: цена перелёта работает для пилотируемых и беспилотных рейсов

---

## Phase 4: User Story 2 — Грузовой терминал и линия (Priority: P2)

**Goal**: блок-автомат линии с выдержкой, проверками, автозапуском, статусом в терминале/Jade/ЦУПе

**Independent Test**: сценарий спеки US2 (отказ по Δv → заправка → автозапуск; экипаж игнорируется; HOLD не запускает)

- [X] T321 [P] [US2] Блок `CARGO_TERMINAL` (EntityBlock, strength 3.5/9, металл) и регистрация BE `CARGO_TERMINAL` в `mod/src/main/java/org/alex_melan/spacereloaded/registry/ModBlocks.java`, `mod/src/main/java/org/alex_melan/spacereloaded/registry/ModBlockEntities.java`
- [X] T322 [US2] `CargoTerminalBlockEntity` по data-model: программа (dimension/pad/frequency), `Mode`, `State` + аргумент, снимок обслуживания, счётчики, NBT; `serverTick` раз в `cargoLineCheckIntervalTicks`: выбор борта (зона ±8/48, без пассажиров, не обломок, ближайший), выдержка, `MissionPlanning.plan`, окно, покрытие (`Logistics.coverageSatisfied`), `installProgram` временным стеком, `tryLaunchUnmanned`; `installProgram(ItemStack)`, `toggleMode()`, `statusLines() → List<Component>` в `mod/src/main/java/org/alex_melan/spacereloaded/logistics/CargoTerminalBlockEntity.java`
- [X] T323 [US2] `CargoTerminalBlock`: тикер; `useItemOn` с полётной программой → установка; `useWithoutItem`: Sneak → AUTO/HOLD, иначе статус в чат в `mod/src/main/java/org/alex_melan/spacereloaded/logistics/CargoTerminalBlock.java`
- [X] T324 [P] [US2] ЦУП: терминалы в радиусе 64 (обход блок-сущностей чанков) → строки состояния после списка бортов в `mod/src/main/java/org/alex_melan/spacereloaded/rocket/MissionControlBlock.java`
- [X] T325 [P] [US2] Jade: `TERMINAL_DATA` (state, arg, departures, arrivals, mode) в `mod/src/main/java/org/alex_melan/spacereloaded/compat/JadePlugin.java`; строки в `mod/src/client/java/org/alex_melan/spacereloaded/compat/JadeClientPlugin.java`
- [X] T326 [P] [US2] Ассеты терминала: blockstate, модель (cube_bottom_top: top/side, bottom launch_pad_side), item-модель, текстуры через `tools/gen_textures.py` (функция `cargo_terminal()`) в `mod/src/main/resources/assets/spacereloaded/{blockstates,models/block,items}/cargo_terminal.json`, `textures/block/cargo_terminal_{top,side}.png`
- [X] T327 [P] [US2] Данные терминала: рецепт `assembly_cargo_terminal.json` (сталь×2, медь, полётная программа), лут `loot_table/blocks/cargo_terminal.json`, теги `mineable/pickaxe` + `needs_iron_tool`, достижение `advancement/cargo_line.json` (родитель docking) в `mod/src/main/resources/data/…`

**Checkpoint**: линия Земля ↔ орбита работает без игрока

---

## Phase 5: User Story 3 — Wet workshop (Priority: P3)

**Goal**: порт-люк, планировщик оболочки, конверсия борта в герметичный модуль, обшивка модуля

**Independent Test**: сценарий спеки US3 — стек 5×5×5 → 27 воздух / 98 обшивка / люк / двигатели; контроллер внутри — герметично

- [X] T328 [P] [US3] Тест `WetWorkshopPlannerTest`: куб 3×3×3 баков → 26 SHELL / 1 INTERIOR; 5×5×5 → 98/27; столбик 1×1×5 → 5 SHELL, 0 INTERIOR; слой двигателей снизу и командный модуль сверху → KEEP; `hatchCell` для клетки у грани; флудфилл: план 5×5×5 в `ArrayVoxelGrid` (SHELL/KEEP непроницаемы, INTERIOR воздух, люк закрыт) из центра — sealed; люк открыт — leak в `core/src/test/java/org/alex_melan/spacereloaded/core/station/WetWorkshopPlannerTest.java`
- [X] T329 [US3] `WetWorkshopPlanner` (`Kind`, `Cell`, `Plan`, `plan(RocketStructure)`, `hatchCell(Plan, x, y, z)`) в `core/src/main/java/org/alex_melan/spacereloaded/core/station/WetWorkshopPlanner.java`
- [X] T330 [P] [US3] Блоки `MODULE_HULL` (как hull_plating) и `DOCKING_PORT` (`DockingPortBlock`) в `mod/src/main/java/org/alex_melan/spacereloaded/registry/ModBlocks.java`; теги `airtight` += оба в `mod/src/main/resources/data/spacereloaded/tags/block/airtight.json`
- [X] T331 [US3] `DockingPortBlock extends HermeticHatchBlock` + `FACING` (placement — лицом к игроку), `useWithoutItem`: Sneak → `WetWorkshop.convert`, иначе super в `mod/src/main/java/org/alex_melan/spacereloaded/logistics/DockingPortBlock.java`
- [X] T332 [US3] `WetWorkshop.convert(level, rocket, portPos, facing) → Component`: поиск борта у передней грани (AABB ±3), проверки (припаркован/без пассажиров/не обломок/остаток ≤ доля/клетки-цели воздух/клетка люка), груз → `popResource`, топливо стравлено, блоки по плану (SHELL → module_hull, INTERIOR → air, KEEP → state, люк → hermetic_hatch закрытый), `discard`, сообщение с числами в `mod/src/main/java/org/alex_melan/spacereloaded/logistics/WetWorkshop.java`
- [X] T333 [P] [US3] Ассеты: blockstate порта с `facing` (модель `orientable` с фронт-текстурой, open/cycling варианты как у люка), `module_hull` (cube_all), item-модели, текстуры через `tools/gen_textures.py` (`docking_port()`, `module_hull()`) в `mod/src/main/resources/assets/spacereloaded/…`
- [X] T334 [P] [US3] Данные: рецепт `assembly_docking_port.json` (сталь×2, люк, узел), лут `docking_port.json`/`module_hull.json`, теги pickaxe/iron, достижение `advancement/wet_workshop.json` (родитель sealed) в `mod/src/main/resources/data/…`

**Checkpoint**: модуль из борта герметичен на стенде

---

## Phase 6: User Story 4 — Марсианский цикл (Priority: P4)

**Goal**: лёд на Марсе, достижения Марса

**Independent Test**: сценарий спеки US4 — лёд найден в 3×3 чанках; тег принимает; планировщик одобряет возврат метанокса (T311)

- [X] T335 [P] [US4] Блок `MARS_ICE` (свойства как `MOON_ICE`) в `mod/src/main/java/org/alex_melan/spacereloaded/registry/ModBlocks.java`; тег `electrolyzer_input` += mars_ice в `mod/src/main/resources/data/spacereloaded/tags/item/electrolyzer_input.json`; лут `loot_table/blocks/mars_ice.json`; `mineable/pickaxe`
- [X] T336 [P] [US4] Worldgen: `configured_feature/mars_ice.json` (ore size 9 в red_sandstone), `placed_feature/mars_ice.json` (count 6, 4…90), `biome/mars_plains.json` += `spacereloaded:mars_ice` в `mod/src/main/resources/data/spacereloaded/worldgen/…`
- [X] T337 [P] [US4] Ассеты: blockstate/модель/item `mars_ice`, текстура через `tools/gen_textures.py` (`mars_ice()`: красный песчаник с ледяными прожилками) в `mod/src/main/resources/assets/spacereloaded/…`
- [X] T338 [P] [US4] Достижения `advancement/mars.json` (changed_dimension → spacereloaded:mars, родитель orbit, frame goal) и `advancement/mars_return.json` (from mars → earth_orbit, родитель mars, frame challenge) в `mod/src/main/resources/data/spacereloaded/advancement/`

---

## Phase 7: Polish — стенд, документация, локализация, финальная проверка

- [X] T339 Стенд `testCargoLine` (BX+720): площадка/пилон/борт (двигатель, бак пустой, отсек, командный модуль), терминал с программой (orbit, маяк 60/101/60), Sneak-переключение в AUTO через `toggleMode`; ждать состояние REFUSED с «м/с»/TWR; `refuel(2000)`; ждать `launched` в пределах выдержки + 3 интервалов, `departures()==1`; второй борт с пассажиром (`startRiding`) — состояние CREW_ABOARD; cleanup в `mod/src/gametest/java/org/alex_melan/spacereloaded/gametest/SpaceReloadedClientGameTest.java`
- [X] T340 Стенд `testWetWorkshop` (BX+760): площадка 5×5, пилон, слой двигателей 5×5, баки 5×5×5, командный модуль сверху; сборка; порт вплотную к стенке (facing к борту); `WetWorkshop.convert` сервер-хуком; проверить 27 воздух / 98 обшивка / люк / двигатели; контроллер + creative_power внутри → SEALED (локальный helper статуса) в `mod/src/gametest/java/org/alex_melan/spacereloaded/gametest/SpaceReloadedClientGameTest.java`
- [X] T341 Стенд: лёд Марса — скан 3×3 чанков (y 4…90) на `mars_ice` ≥ 1 в `testPlanetTerrain` в `mod/src/gametest/java/org/alex_melan/spacereloaded/gametest/SpaceReloadedClientGameTest.java`
- [X] T342 Регистрация сценариев в `runTest` и запуск стенда `timeout 570 ./gradlew :mod:runClientGametest` — зелёный
- [X] T343 [P] Локализация: все новые ключи (блоки, состояния терминала, сообщения планировщика/конверсии, HUD, Jade, достижения) в `mod/src/main/resources/assets/spacereloaded/lang/{en_us,ru_ru,uk_ua}.json`; проверка равенства наборов ключей
- [X] T344 [P] `docs/GUIDE.ru.md`: разделы «Цена перелёта (таблица Δv, примеры стеков)», «Грузовые линии», «Wet workshop», «Марс: обратный билет»
- [X] T345 [P] README.md / README.ru.md: пункты про честный Δv-бюджет, линии, wet workshop, лёд Марса
- [X] T346 [P] `docs/ADDONS.md`: `transfer_delta_v`, новые теги/блоки, обшивка без рецепта, порт; `specs/001-space-mod-core/progression.md`: фазы 8/10/11 дополнены; `inspiration-backlog.md`: пункты 1, 10, 16 отмечены как взятые; `tasks.md` (001): указатель «Фаза 17 → specs/003»; `api-notes-26.2.md`: находки
- [X] T347 `tools/gen_site.py`: новые блоки/рецепты в карточках, регенерация `docs/*.html`
- [X] T348 Финальная проверка: `JAVA_HOME=… ./gradlew build` (core tests) + стенд; обновить `spec.md` Status: Implemented; закрыть все задачи `[X]`

---

## Dependencies & Execution Order

- Setup (T300–T302) → Foundational (T303–T313) → US1 (T314–T320) → US2 (T321–T327) → US3 (T328–T334, независимо от US1/US2 кроме ModBlocks) → US4 (T335–T338, независимо) → Polish.
- US2 зависит от US1 (планировщик, `tryLaunchUnmanned`). US3 и US4 могут идти параллельно с US2.
- Тесты ядра (T303, T305, T307, T309, T311, T328) пишутся до реализации и должны быть красными.

## Parallel Examples

- Foundational: T303/T305/T307/T309/T311 — параллельно (разные тестовые файлы); реализации T304→T306→T308→T310→T312 последовательно (зависимости).
- US2: T324, T325, T326, T327 — параллельно после T322.
- US3: T330, T333, T334 — параллельно; T331/T332 после T329.
- US4: все четыре задачи параллельно.
- Polish: T343–T346 параллельно.

## Implementation Strategy

- MVP = Setup + Foundational + US1: честная цена перелёта видна пилоту, беспилотники не стартуют без бюджета.
- Далее US2 (линия), US3 (модуль), US4 (Марс) — каждая проверяется отдельно, затем стенд и документация.
