# Tasks: Полёт 2.0 — ступени, ориентация, атмосфера, точность орбитального удара

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/, quickstart.md из `/specs/002-staged-flight-physics/`

**Prerequisites**: plan.md (обязательно), spec.md (истории), data-model.md, contracts/, research.md

**Tests**: тесты ядра ОБЯЗАТЕЛЬНЫ (конституция VII): JUnit-задача каждого класса ядра стоит перед его реализацией и обязана быть красной до неё. Стенд (`mod/src/gametest`) получает сценарий на каждую историю.

**Organization**: фазы Setup → Foundational → по одной фазе на историю (US1 ступени → US2 ориентация → US3 атмосфера → US4 орудие) → Polish (стенд, документация, локализация, финальная проверка). Нумерация T200+ (001 закончилась на T111).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: можно параллельно (разные файлы, нет зависимостей от незавершённых задач)
- **[Story]**: история спеки (US1…US4); у Setup/Foundational/Polish метки нет
- Пути — от корня репозитория; `core/…` — чистая Java без Minecraft, `mod/…` — Fabric-слой

## Path Conventions

- Ядро: `core/src/main/java/org/alex_melan/spacereloaded/core/…`, тесты `core/src/test/java/…`
- Мод: `mod/src/main/java/org/alex_melan/spacereloaded/…`, клиент `mod/src/client/java/…`, ресурсы `mod/src/main/resources/…`, стенд `mod/src/gametest/java/…/SpaceReloadedClientGameTest.java`
- Сборка: `JAVA_HOME=~/.sdkman/candidates/java/25.0.3-tem ./gradlew …`

---

## Phase 1: Setup (общая инфраструктура)

**Purpose**: поля конфига, тип урона и служебные хуки, нужные нескольким историям

- [X] T200 Добавить поля конфига по `contracts/config.md` (attitudeMaxDeg, attitudeRateDegPerSec, rocketDragCoefficient, cannonRodDragCoefficient, cannonRodAreaM2, meteorDragCoefficient, meteorAreaM2, reentryHeatIndexThreshold, reentryHeatDamage, reentryHeatIntervalTicks, maxDynamicPressurePa, stageSeparationImpulseNs, stageDebrisMaxTicks, cannonGuidedSpreadBlocks, cannonUnguidedSpreadBlocks) с Javadoc-обоснованием значений и проверками диапазонов в `validate()`; старые `cannonDragCoeff`/`meteorDragCoeff` пока оставить (удаляются в T248) в `mod/src/main/java/org/alex_melan/spacereloaded/config/SpaceReloadedConfig.java`
- [X] T201 [P] Тип урона входного нагрева: `REENTRY_HEAT` + фабрика `reentryHeat(level)` в `mod/src/main/java/org/alex_melan/spacereloaded/registry/ModDamageTypes.java`; JSON `mod/src/main/resources/data/spacereloaded/damage_type/reentry_heat.json` (message_id `spacereloaded.reentry_heat`, scaling never, effects burning); добавить в `mod/src/main/resources/data/minecraft/tags/damage_type/bypasses_armor.json`
- [X] T202 [P] `setCoverage(ResourceKey<Level>, int)` (0 удаляет запись, `setDirty`) в `mod/src/main/java/org/alex_melan/spacereloaded/network/SpaceNetworkState.java` — для стенда и будущих админ-команд

---

## Phase 2: Foundational (блокирует истории)

**Purpose**: роль детали и профиль атмосферы — данные, на которые опираются US1/US3/US4

**⚠️ CRITICAL**: без этих задач истории не компилируются

- [X] T203 Роль `SEPARATOR` в `core/src/main/java/org/alex_melan/spacereloaded/core/rocketry/PartRole.java` (Javadoc: только масса, плоскость разделения, остаётся на нижней ступени) и фабрика `PartProperties.separator(double massKg)` в `core/src/main/java/org/alex_melan/spacereloaded/core/rocketry/PartProperties.java`; убедиться, что `RocketData.Entry.toProperties` и `mod/src/main/java/org/alex_melan/spacereloaded/rocket/PartPropertiesResolver.java` принимают строку роли `separator` без спецобработки
- [X] T204 [P] JUnit `core/src/test/java/org/alex_melan/spacereloaded/core/atmosphere/AtmosphereProfileTest.java`: `VACUUM.density(любая y) == 0`, `density(datum) == ρ₀`, `density(datum + H) == ρ₀/e` (1e-9), ниже datum — ρ₀ (не растёт), `isVacuum()`; `FlightEnvironment(g)` эквивалентен `FlightEnvironment(g, VACUUM)`
- [X] T205 Реализовать `core/src/main/java/org/alex_melan/spacereloaded/core/atmosphere/AtmosphereProfile.java` (record `surfaceDensity, scaleHeightM, datumY`, `VACUUM`, `density(y)`, валидация ρ₀ ≥ 0, H > 0) и расширить `core/src/main/java/org/alex_melan/spacereloaded/core/rocketry/FlightEnvironment.java` до `(gravity, atmosphere)` с совместимым конструктором `(gravity)` = вакуум; `EARTH` остаётся вакуумным (тесты 001 не меняются) — T204 зелёный
- [X] T206 Поля `atmosphere_density` (default 0), `scale_height` (default 110), `datum_y` (default 63) в `PlanetProfile` + валидация диапазонов (ρ ∈ [0,50], H ∈ [1,1e5]) в `mod/src/main/java/org/alex_melan/spacereloaded/registry/ModRegistries.java`; `PlanetManager.atmosphere(ServerLevel)` → `AtmosphereProfile` ядра (без профиля — вакуум) и `PlanetManager.environment(ServerLevel)` → `FlightEnvironment` в `mod/src/main/java/org/alex_melan/spacereloaded/planet/PlanetManager.java`; значения Земли (1.225/110/63) в `mod/src/main/resources/data/spacereloaded/spacereloaded/planets/earth.json` и Марса (0.020/150/64) в `…/planets/mars.json`

**Checkpoint**: ядро компилируется, все старые тесты и `AtmosphereProfileTest` зелёные; атмосферные поля читаются из датапака, но на полёт пока не влияют

---

## Phase 3: User Story 1 — Многоступенчатая ракета (Priority: P1) 🎯 MVP

**Goal**: разделитель ступеней делит стек на ступени; скан и HUD показывают Δv ступеней; в полёте горит активная ступень; отделение пилотом (клавиша X) или автопилотом; обломок падает честно; всё персистентно

**Independent Test**: двухступенчатый стек → скан «Ступени: 2», Δv сумма = последовательный Циолковский; беспилотный старт → авто-отделение → два аппарата, обломок разрушен/припаркован ≤ 60 с, верхняя ступень доходит до перехода

### Tests for User Story 1 (ядро, сначала красные)

- [X] T210 [P] [US1] JUnit `core/src/test/java/org/alex_melan/spacereloaded/core/rocketry/StageLayoutTest.java`: (а) стек с разделителями на Y=2 и Y=5 → 3 ступени с правильными деталями и `topY`; (б) два разделителя на одном Y → одна плоскость; (в) без разделителей → 1 ступень, `activeView(0) == structure` (те же массы/тяга/ёмкость); (г) `validate`: разделитель на уровне командного модуля → `stage_above_command` с позицией; нет деталей под нижним разделителем → `stage_empty_below`; верхняя деталь, связанная только через нижнюю ступень → `stage_disconnected`; (д) `activeView(1, [0, 800])`: двигатели/баки верхней ступени превращены в корпус с массой dry+propellant, тяга только от активной, ёмкость только у активной; (е) `distributeByCapacity(total)` пропорционально ёмкостям ступеней
- [X] T211 [P] [US1] JUnit `core/src/test/java/org/alex_melan/spacereloaded/core/rocketry/StagedPerformanceTest.java`: двухступенчатый стек (ступень 1: 2 двигателя 60 кН/300 с + бак 2000 кг + разделитель; ступень 2: двигатель 32 кН/450 с + бак 2000 кг + командный модуль) → `totalDeltaV` = `Isp₁g₀ln(m₀/(m₀−p₁)) + Isp₂g₀ln(m₀'/(m₀'−p₂))` в 1%, где m₀' — масса стека без ступени 1; одноступенчатый → равен `PerformanceCalculator.deltaV` в 0.1%; TWR ступени 2 считается от m₀'; ступень без двигателей → Δv 0 и предупреждение `STAGE_NO_ENGINE`

### Implementation for User Story 1

- [X] T212 [US1] Реализовать `core/src/main/java/org/alex_melan/spacereloaded/core/rocketry/StageLayout.java` по data-model (`Stage`, `StageError`, `of`, `validate(structure, commandPos)`, `activeView`, `remainingAfter`, `distributeByCapacity`), Javadoc с упрощениями D11 — T210 зелёный
- [X] T213 [US1] Реализовать `core/src/main/java/org/alex_melan/spacereloaded/core/rocketry/StagedPerformance.java` (`StagePerformance`, `StagedReport`, `calculate(layout, propellant[], gravity)` через `activeView` + `PerformanceCalculator`) и добавить `STAGE_NO_ENGINE` в `core/src/main/java/org/alex_melan/spacereloaded/core/rocketry/PerformanceWarning.java` — T211 зелёный
- [X] T214 [P] [US1] Блок `STAGE_SEPARATOR` (обычный `Block`, 3.0/10.0, металл, кирка) в `mod/src/main/java/org/alex_melan/spacereloaded/registry/ModBlocks.java`; в креативную вкладку после `DOCKING_CLAMP` в `mod/src/main/java/org/alex_melan/spacereloaded/registry/ModCreativeTab.java`
- [X] T215 [P] [US1] Данные разделителя: `mod/src/main/resources/data/spacereloaded/spacereloaded/part_properties/stage_separator.json` (role separator, 120 кг); `…/tags/block/rocket_parts.json` и `mod/src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json` (+ stage_separator); лут `…/loot_table/blocks/stage_separator.json`; рецепт `…/recipe/assembly_stage_separator.json` (2 стали + титановый сплав + порох); достижение `…/advancement/staging.json` (родитель `spacereloaded:rocketry`, inventory_changed)
- [X] T216 [P] [US1] Ассеты разделителя: `mod/src/main/resources/assets/spacereloaded/blockstates/stage_separator.json`, `models/block/stage_separator.json` (`cube_column`: end/side), `items/stage_separator.json`; функция `stage_separator()` в `tools/gen_textures.py` (тёмное стальное кольцо с оранжевыми пироболтами `stage_separator_side.png`, крестовина `stage_separator_end.png`) и запуск генератора → PNG в `assets/spacereloaded/textures/block/`
- [X] T217 [US1] Валидация ступеней при скане: в `buildResult` после сборки структуры вызывать `StageLayout.validate(structure, localCommandPos)` и возвращать `Result.Error("message.spacereloaded.assembly.<key>", мировая позиция)` в `mod/src/main/java/org/alex_melan/spacereloaded/rocket/RocketAssembler.java`; helper `RocketData.toLayout()` и `stagePropellantFromScan(structure)` (суммы топлива баков по ступеням) в `mod/src/main/java/org/alex_melan/spacereloaded/rocket/RocketData.java`
- [X] T218 [US1] Состояние ступеней в `mod/src/main/java/org/alex_melan/spacereloaded/rocket/RocketEntity.java`: поля `layout`, `activeStage`, `double[] stagePropellant`, кэш `activeView` (пересборка при смене ступени); `setAssembly(data, stagePropellant[])` (старый `setAssembly(data)` распределяет по ёмкости); `tick` интегрирует `activeView` с `flight.propellantKg = stagePropellant[active]` и записывает остаток обратно; `refuel/drain` по ступеням пропорционально свободной ёмкости/остатку, `propellantKg()` — сумма; `disassembleInto` льёт в баки долю своей ступени; `transition/postArrival` переносят массив ступеней; NBT `stage_active`/`stage_propellant`/`debris`/`debris_ticks` с fallback `distributeByCapacity`; synched `DATA_STAGE`, `DATA_STAGE_COUNT`, `DATA_STAGE_FUEL`, `DATA_DELTA_V` (раз в 10 тиков через `StagedPerformance`); обломок (`debris`): предельное время жизни из конфига, `discard()` ниже `minY − 64`; публичные тест-хуки `activeStage()`, `stageCount()`, `stagePropellantKg(int)`, `ignite(ServerLevel)`, `launchUnmanned(ServerLevel)` (вынести из `startAutopilot`), `setFlightVelocity(Vec3)`
- [X] T219 [US1] Оператор отделения `mod/src/main/java/org/alex_melan/spacereloaded/rocket/StageSeparation.java`: `separate(ServerLevel, RocketEntity) → Component` — делит `RocketData` по `layout.stages[active]`, спавнит обломок (`RocketEntity` launched, без пассажиров, `debris=true`, топливо активной ступени) на базе текущего аппарата, верхнему стеку задаёт нормализованную `RocketData` (смещение вниз на высоту ступени, позиция сущности поднята соответственно), `stagePropellant` без первой, `activeStage=0`; импульс ±J/m вдоль оси аппарата (сохранение импульса), пассажиры остаются в верхнем; `RocketEntity.requestStageSeparation(ServerPlayer)` (проверки: пилот = первый пассажир, `launched`, есть нижняя ступень → сообщения `message.spacereloaded.stage.*`) и авто-отделение автопилота при `stagePropellant[active] ≤ 0.5`
- [X] T220 [US1] C2S `mod/src/main/java/org/alex_melan/spacereloaded/network/StageSeparatePayload.java` (пустая запись, TYPE/CODEC), регистрация и обработчик (пилот своего борта → `requestStageSeparation`, ответ в оверлей) в `mod/src/main/java/org/alex_melan/spacereloaded/network/ModNetworking.java`; клавиша `key.spacereloaded.stage` (GLFW X) → отправка при `consumeClick()` и посадке в `RocketEntity` в `mod/src/client/java/org/alex_melan/spacereloaded/client/SpaceReloadedClient.java`
- [X] T221 [US1] Скан ступеней: `ScanReportPayload` + поля `stages` (список `StageLine`), `ascentReached`, `ascentDeltaV`, `ascentApexM`, `maxQPa`, `maxQExceeded` (в US1 подъём заполняется старой оценкой `1.15·√(2gh)` и `reached = deltaV ≥ needed`; US3 заменит) в `mod/src/main/java/org/alex_melan/spacereloaded/network/ScanReportPayload.java`; `RocketInteractions.scanFromPylon` считает `StagedPerformance` и шлёт ступени, предупреждения ступеней в `mod/src/main/java/org/alex_melan/spacereloaded/rocket/RocketInteractions.java`; экран рисует «Ступени: N» и строки «Ступень i: Δv X · TWR Y» с высотой панели по числу строк в `mod/src/client/java/org/alex_melan/spacereloaded/client/gui/ScanReportScreen.java`
- [X] T222 [US1] HUD: строка «Ступень i/N · топливо ступени · Δv» и обновлённая подсказка полёта (X — отделить) в `mod/src/client/java/org/alex_melan/spacereloaded/client/gui/RocketHud.java`
- [X] T223 [US1] Локализация US1 (блок, достижение, клавиша, HUD ступени, сообщения `stage.*`, ошибки сборки `stage_*`, `warning.STAGE_NO_ENGINE`, `screen.scan.stages/stage_line`) в `mod/src/main/resources/assets/spacereloaded/lang/en_us.json`, `ru_ru.json`, `uk_ua.json`
- [X] T224 [US1] Стенд `testStaging` в `mod/src/gametest/java/org/alex_melan/spacereloaded/gametest/SpaceReloadedClientGameTest.java`: сборка двухступенчатого стека на площадке (двигатель, бак | разделитель | двигатель, бак, командный модуль), `StagedPerformance` по результату скана: 2 ступени и суммарный Δv больше одноступенчатого варианта; сборка → `launchUnmanned` → ожидание авто-отделения (2 сущности `RocketEntity` в области) → обломок исчезает/паркуется ≤ 1200 тиков; отдельно: `requestStageSeparation` на стоянке отклоняется («not launched»), после `ignite` с игроком в кресле — отделяет; добавить вызов в `runTest`

**Checkpoint**: US1 самостоятельно играбельна: ступени в скане, HUD, полёте и падении

---

## Phase 4: User Story 2 — Ориентация и гравитационный разворот (Priority: P2)

**Goal**: WASD наклоняет ракету относительно взгляда пилота через гиродины (предел 45°, плавный ход), тяга даёт горизонтальную скорость, HUD показывает углы; без гиродинов — пояснение

**Independent Test**: ракета с гироскопом, удержание «вперёд» 5 с на тяге → горизонтальная скорость ≥ 5 м/с по взгляду; без гироскопа угол не меняется (< 1°)

### Tests for User Story 2

- [X] T229 [P] [US2] JUnit `core/src/test/java/org/alex_melan/spacereloaded/core/rocketry/AttitudeCommandTest.java`: рампа командуемого угла (рост со скоростью rate до max, спад к 0 при отпускании, не выходит за пределы); `toPitchRoll(θ, dirX, dirZ)` даёт такие углы, что `FlightIntegrator.rotate((0,1,0), pitch, roll)` имеет горизонтальную составляющую, сонаправленную с `(dirX, dirZ)` (косинус угла > 0.99 при θ = 20° для 8 направлений); нулевое направление → углы 0

### Implementation for User Story 2

- [X] T230 [US2] Реализовать `core/src/main/java/org/alex_melan/spacereloaded/core/rocketry/AttitudeCommand.java` (record `tiltDeg, dirX, dirZ`; `step(input dirX/dirZ, dt, rateDegPerSec, maxDeg)`, `pitchRad()/rollRad()` по D14 с Javadoc про приближение совместного наклона) — T229 зелёный
- [X] T231 [US2] Ввод ориентации в `mod/src/main/java/org/alex_melan/spacereloaded/rocket/RocketEntity.java`: из `Input.forward/backward/left/right` и `pilot.getYHeadRot()` строить направление (forward = (−sin yaw, cos yaw), right = (cos yaw, sin yaw)), обновлять `AttitudeCommand` с параметрами конфига, передавать `pitchCmd/rollCmd` в `ControlInput`; без пилота — команда обнуляется; synched `DATA_CMD_PITCH`, `DATA_CMD_ROLL`, `DATA_HAS_GYRO` (есть ли деталь с `gyroTorqueNm > 0` в оставшемся стеке)
- [X] T232 [P] [US2] HUD: строка «Наклон: факт° (команда°)» и «нет управления ориентацией — нужен гироскоп» при команде без гиродинов в `mod/src/client/java/org/alex_melan/spacereloaded/client/gui/RocketHud.java`
- [X] T233 [US2] Локализация US2 (`hud.spacereloaded.rocket.attitude`, `hud.spacereloaded.rocket.no_gyro`, подсказка полёта с WASD) в `mod/src/main/resources/assets/spacereloaded/lang/en_us.json`, `ru_ru.json`, `uk_ua.json`
- [X] T234 [US2] Стенд `testAttitude` в `mod/src/gametest/java/org/alex_melan/spacereloaded/gametest/SpaceReloadedClientGameTest.java`: стек с гироскопом, игрок садится (`startRiding`), `setLastClientInput(forward+jump)`, `ignite`, 100 тиков → горизонтальная скорость ≥ 5 м/с и `DATA_CMD_PITCH/ROLL` ≠ 0; тот же стек без гироскопа → фактический наклон < 1°; убрать аппараты после сценария

**Checkpoint**: US1 и US2 работают вместе: наклонённый старт, отделение, обломок падает в стороне

---

## Phase 5: User Story 3 — Атмосфера: сопротивление, терминальная скорость, нагрев (Priority: P3)

**Goal**: квадратичное сопротивление для ракет/ломов/метеоритов по профилю тела, терминальная скорость, входной нагрев без капсулы, численный симулятор подъёма в скан-отчёте с пиковым напором

**Independent Test**: падение без тяги в атмосфере сходится к v_t (ядро, 2%); в вакууме траектория тождественна старой; скан на Земле показывает отчёт подъёма; флаг нагрева на Земле при −150 м/с и его отсутствие на Луне (стенд)

### Tests for User Story 3 (ядро, сначала красные)

- [X] T240 [P] [US3] JUnit `core/src/test/java/org/alex_melan/spacereloaded/core/rocketry/FlightIntegratorDragTest.java`: (а) постоянная плотность (H огромная), без тяги, 60 с → |v_y| = √(2mg/(ρC_dA)) в 2%; (б) вакуум: 10 с подъёма при `FlightEnvironment(g, VACUUM)` тождественны `FlightEnvironment(g)` (1e-9); (в) старт на 500 м/с при ρ=1.225 не даёт NaN и знак скорости не меняется за шаг (клиппинг); (г) `RocketStructure.dragBody(0.5)` для стека 1×3×1: areaY = 1, areaX = areaZ = 3
- [X] T241 [P] [US3] JUnit `core/src/test/java/org/alex_melan/spacereloaded/core/rocketry/AerothermalTest.java`: `heatIndex(ρ, v) = √ρ·v³` (точные значения), монотонность по ρ и v, `dynamicPressure = ½ρv²`, ноль в вакууме
- [X] T242 [P] [US3] JUnit `core/src/test/java/org/alex_melan/spacereloaded/core/rocketry/AscentSimulatorTest.java`: (а) вакуум, одноступенчатый эталон, цель 450 м → `timeToTarget` и `deltaVSpentToTarget` совпадают с замкнутой формой (h(t) = u·τ·(w ln w − w + 1) − gt²/2; Δv = u·ln(1/w)) в 1%; (б) та же ракета на Земле (ρ₀ 1.225, H 110) тратит больше Δv, чем на Луне (g 1.62, вакуум); (в) двухступенчатый стек использует 2 ступени (`stagesUsed = 2`) и достигает цели, недостижимой одной; (г) TWR < 1 → `reached=false`, apex = старт; (д) `maxDynamicPressurePa` > 0 в атмосфере и 0 в вакууме
- [X] T243 [P] [US3] Обновить `core/src/test/java/org/alex_melan/spacereloaded/core/ballistics/BallisticsTest.java` под `ProjectileSpec(mass, cd, area)` и `step(…, density, dt)`: вакуум (density 0) — старые аналитические тесты без изменений; `dragReducesImpactSpeed` при ρ = 1.225; `impactForecast` лома (2000 кг, 0.1, 0.03) с 2400 м на 1500 м/с: скорость удара на Земле ниже, чем на Марсе (ρ₀ 0.02), обе выше 1400 м/с; энергия = ½mv²

### Implementation for User Story 3

- [X] T244 [US3] Реализовать `core/src/main/java/org/alex_melan/spacereloaded/core/atmosphere/DragBody.java` (`effectiveArea`, `force`), `RocketStructure.dragBody(cd)` в `core/src/main/java/org/alex_melan/spacereloaded/core/rocketry/RocketStructure.java` (габариты AABB по `PackedPos`) и член сопротивления в `core/src/main/java/org/alex_melan/spacereloaded/core/rocketry/FlightIntegrator.java` (`step(structure, state, input, env, dt)` → перегрузка с `DragBody`; старая сигнатура = без сопротивления; клиппинг силы за шаг; Javadoc с формулой и упрощением C_d) — T240 зелёный
- [X] T245 [P] [US3] Реализовать `core/src/main/java/org/alex_melan/spacereloaded/core/rocketry/Aerothermal.java` — T241 зелёный
- [X] T246 [US3] Реализовать `core/src/main/java/org/alex_melan/spacereloaded/core/rocketry/AscentSimulator.java` + `AscentReport` (dt 0.05, полная тяга, авто-отделение, coast до апогея, лимит 600 с модельного времени, интеграл Δv до цели, пиковый напор) — T242 зелёный
- [X] T247 [P] [US3] `ProjectileSpec(massKg, cd, areaM2)` в `core/src/main/java/org/alex_melan/spacereloaded/core/ballistics/ProjectileSpec.java`; `BallisticIntegrator.step(state, spec, gravity, density, dt)` (квадратичное сопротивление, клиппинг), `Forecast` и `impactForecast(spec, dropAltitude, muzzle, gravity, atmosphere, targetY)` в `core/src/main/java/org/alex_melan/spacereloaded/core/ballistics/BallisticIntegrator.java` — T243 зелёный
- [X] T248 [US3] Атмосфера в моде: `RocketEntity.tick` строит `FlightEnvironment` через `PlanetManager.environment(level)` и `DragBody` из конфига (`rocketDragCoefficient`), считает `Aerothermal.heatIndex` по плотности на своей высоте: при превышении порога и отсутствии `RETURN_CAPSULE` в стеке — урон `reentryHeat` всем пассажирам раз в `reentryHeatIntervalTicks`, `DATA_HEATING=true`, плазменные частицы; `KineticProjectileEntity` и `MeteorEntity` переходят на `ProjectileSpec(mass, cd, area)` + плотность по `PlanetManager.atmosphere(level).density(getY())` (NBT `cd`/`area` у снаряда); `OrbitalCannonBlockEntity.tryFire` передаёт `cannonRodDragCoefficient`/`cannonRodAreaM2`; удалить `cannonDragCoeff`/`meteorDragCoeff` из `mod/src/main/java/org/alex_melan/spacereloaded/config/SpaceReloadedConfig.java` — файлы: `mod/src/main/java/org/alex_melan/spacereloaded/rocket/RocketEntity.java`, `…/cannon/KineticProjectileEntity.java`, `…/impact/MeteorEntity.java`, `…/cannon/OrbitalCannonBlockEntity.java`
- [X] T249 [US3] Скан с симулятором: `RocketInteractions.scanFromPylon` вызывает `AscentSimulator.simulate(layout, propellant, PlanetManager.environment(level), cd, minY площадки, transitionAltitude)` и заполняет `ascent*`/`maxQ*` поля (`requiredDeltaV = ascentDeltaV`), вердикт «готов» только при `reached` в `mod/src/main/java/org/alex_melan/spacereloaded/rocket/RocketInteractions.java`; экран рисует «Подъём: достигнет/не достигнет H м · Δv до перехода · апогей» и «Макс. напор Q кПа» (красным при превышении) в `mod/src/client/java/org/alex_melan/spacereloaded/client/gui/ScanReportScreen.java`
- [X] T250 [P] [US3] HUD-предупреждение «⚠ НАГРЕВ — нет теплозащиты» при `DATA_HEATING` в `mod/src/client/java/org/alex_melan/spacereloaded/client/gui/RocketHud.java`
- [X] T251 [US3] Локализация US3 (`hud.spacereloaded.rocket.heating`, `screen.spacereloaded.scan.ascent_ok/ascent_fail/max_q/max_q_warn`, `death.attack.spacereloaded.reentry_heat`) в `mod/src/main/resources/assets/spacereloaded/lang/en_us.json`, `ru_ru.json`, `uk_ua.json`
- [X] T252 [US3] Стенд `testAtmosphere` в `mod/src/gametest/java/org/alex_melan/spacereloaded/gametest/SpaceReloadedClientGameTest.java`: собранный стек без капсулы поднять на y=300 в оверворлде, `ignite` без пилота + `setFlightVelocity(0, −150, 0)` → через ≤ 10 тиков `DATA_HEATING` true; тот же приём на Луне (`spacereloaded:moon`, чанки форсировать) → false; скан на Земле: `ascentReached` для эталонной ракеты true и `maxQPa > 0`; убрать аппараты

**Checkpoint**: ракеты чувствуют атмосферу; в вакууме всё как раньше; скан говорит правду

---

## Phase 6: User Story 4 — Точность орбитального удара (Priority: P4)

**Goal**: наводимый лом при спутниковом покрытии (≤ 1 бл.), рассеивание без него (10 бл.), режим и прогноз удара в терминале, сопротивление лома по атмосфере цели

**Independent Test**: без покрытия кратер смещён ≤ 10 бл.; с покрытием — в метке; терминал показывает режим и скорость удара (на Марсе выше)

### Tests for User Story 4

- [X] T260 [P] [US4] JUnit `core/src/test/java/org/alex_melan/spacereloaded/core/ballistics/StrikeSolutionTest.java`: `solve(u1, u2, guided=true, 0.5, 10)` → |offset| ≤ 0.5 для всех u; `guided=false` → |offset| ≤ 10; при 20 равномерных выборках среднее |offset| > 1; равномерность по площади: доля выборок с r < R/√2 ≈ 50% (детерминированная сетка u); `spreadRadius` в результате соответствует режиму

### Implementation for User Story 4

- [X] T261 [US4] Реализовать `core/src/main/java/org/alex_melan/spacereloaded/core/ballistics/StrikeSolution.java` (record `guided, spreadRadius, offsetX, offsetZ`; `solve(double u1, double u2, boolean guided, double guidedR, double unguidedR)`, r = R√u1, φ = 2πu2) — T260 зелёный
- [X] T262 [US4] Наведение в моде: `OrbitalCannonBlockEntity.tryFire` определяет `guided = SpaceNetworkState.get(server).hasCoverage(targetLevel.dimension())`, решает `StrikeSolution` (random уровня, радиусы из конфига), спавнит лом в метке + смещение, колонна и гром — по метке, сообщение `message.spacereloaded.cannon.fired_mode` с режимом; `snapshot()` добавляет `guided`, `spreadBlocks`, прогноз `impactForecast` для атмосферы цели (`impactSpeedMs`, `impactEnergyMJ`); `KineticProjectileEntity.configure(…, guided)` + NBT `guided`; поля и кодек в `mod/src/main/java/org/alex_melan/spacereloaded/network/CannonStatePayload.java` — файлы: `mod/src/main/java/org/alex_melan/spacereloaded/cannon/OrbitalCannonBlockEntity.java`, `…/cannon/KineticProjectileEntity.java`
- [X] T263 [P] [US4] Терминал: строки «Наведение: по спутнику · ≤ 1 бл.» / «без покрытия · разброс до N бл.» и «Удар: ~V м/с · E ГДж (тело)» с увеличением высоты панели в `mod/src/client/java/org/alex_melan/spacereloaded/client/gui/CannonTerminalScreen.java`
- [X] T264 [US4] Локализация US4 (`screen.spacereloaded.cannon.guidance_sat/guidance_none/impact`, `message.spacereloaded.cannon.fired_mode`, режимы `cannon.mode.*`) в `mod/src/main/resources/assets/spacereloaded/lang/en_us.json`, `ru_ru.json`, `uk_ua.json`
- [X] T265 [US4] Стенд: `testStrikeGuidance` в `mod/src/gametest/java/org/alex_melan/spacereloaded/gametest/SpaceReloadedClientGameTest.java` — платформа 41×41 из камня, `setCoverage(OVERWORLD, 0)`, выстрел → поиск центра кратера (ближайший к метке столбец с воздухом на уровне платформы, оценка по crater radius) → смещение ≤ `cannonUnguidedSpreadBlocks` + 1, лог значения; `setCoverage(OVERWORLD, 1)`, восстановить платформу, кулдаун, выстрел → метка в кратере и центр ≤ 1.5 бл.; `testOrbitalCannon` в начале получает `setCoverage(OVERWORLD, 1)` (детерминизм существующего сценария); проверить `snapshot()` содержит `guided` и `impactSpeedMs > 1000`

**Checkpoint**: орудие связано со спутниковой сетью; существующий сценарий пушки стабилен

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: бюджет производительности, документация, сайт, финальная проверка

- [X] T270 [P] JUnit `core/src/test/java/org/alex_melan/spacereloaded/core/rocketry/FlightPerformanceBudgetTest.java`: стек 500 деталей (2 ступени) — 1000 шагов `FlightIntegrator` с сопротивлением < 1 с и `StagedPerformance` + `AscentSimulator` < 100 мс (прогрев + измерение, мягкий порог ×2 для CI)
- [X] T271 [P] README: пункты про ступени, наклон, атмосферу/нагрев и наведение орудия в `README.md` и `README.ru.md` (без эмодзи и тире-оформления, как принято в репозитории)
- [X] T272 [P] Гайд игрока: разделы «Ступени», «Ориентация», «Атмосфера и вход», обновление «Орбитальная пушка» (наведение), таблица управления (WASD, X) в `docs/GUIDE.ru.md`
- [X] T273 [P] Прогрессия и бэклог: Фаза 5 (разделитель, гироскоп как управление), Фаза 9 (спутник = наведение), новая строка таблицы зависимостей в `specs/001-space-mod-core/progression.md`; статус-комментарий (посадка по плотности атмосферы — частично, разведспутник как наведение — сделано) в `specs/001-space-mod-core/inspiration-backlog.md`; ссылка на 002 в разделе «Статус» `specs/001-space-mod-core/tasks.md`
- [X] T274 [P] Датапак-гайд: поля `atmosphere_density`/`scale_height`/`datum_y` профиля тела и роль `separator` детали в `docs/ADDONS.md`; новые API-факты (запись `Input`, `getYHeadRot`, `TestInput`) в `specs/001-space-mod-core/api-notes-26.2.md`
- [X] T275 Сайт: `OUR_BLOCK_TEX["stage_separator"]`, `DESC` (разделитель; обновить `gyroscope`, `satellite`, `orbital_cannon`, `return_capsule`), `PHASE["stage_separator"]=5`, разделы гайда (ступени/наклон/атмосфера/наведение, таблица управления, FAQ) в `tools/gen_site.py`; запуск `python3 tools/gen_site.py` → `docs/index.html`, `docs/recipes.html`
- [X] T276 (2026-09-07: build + 74 теста ядра зелёные, стенд 25 сценариев ✓) Финальная проверка: `JAVA_HOME=~/.sdkman/candidates/java/25.0.3-tem ./gradlew build :core:test` зелёный; `xvfb-run --auto-servernum ./gradlew :mod:runClientGametest` — все сценарии ✓ (лог стенда сохранить в отчёт); исправить регрессии; отметить задачи `[X]` в `specs/002-staged-flight-physics/tasks.md`
- [X] T278 Замечание плейтеста («персонаж просто задыхается в космосе»): герметичная кабина — экипаж в креслах ракеты с командным модулем/капсулой не получает урон вакуума и среды (`RocketEntity.hasPressurizedCabin`, `VacuumHazard.tick`); обломок и стек без модуля не защищают; гайд и сайт дополнены
- [X] T277 Коммит рабочего среза на ветке `002-staged-flight-physics` (сообщение на русском по стилю репозитория, Co-Authored-By), без push

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: без зависимостей
- **Foundational (Phase 2)**: после Setup; блокирует все истории (роль детали — US1, профиль атмосферы — US3/US4)
- **US1 (Phase 3)**: после Foundational; независима
- **US2 (Phase 4)**: после Foundational; использует `RocketEntity`, поэтому идёт после T218 (тот же файл), но логически независима от ступеней
- **US3 (Phase 5)**: после Foundational; `AscentSimulator` (T246) использует `StageLayout` (T212) — зависит от US1-ядра; правки `RocketEntity` (T248) — после T218/T231
- **US4 (Phase 6)**: после T247 (квадратичная баллистика и прогноз) и T202
- **Polish (Phase 7)**: после всех историй

### Within Each User Story

- Тесты ядра пишутся и падают ДО реализации класса ядра
- Ядро → данные/ассеты → сущность/оператор → сеть/клавиша → экраны/HUD → локализация → стенд

### Parallel Opportunities

- Setup: T201 ∥ T202 после T200 (разные файлы)
- Foundational: T204 ∥ T203; T206 после T205
- US1: T210 ∥ T211; T214 ∥ T215 ∥ T216 (после T203); T221/T222 ∥ после T218
- US2: T229 → T230 → T231; T232 ∥ T231
- US3: T240 ∥ T241 ∥ T242 ∥ T243; T245 ∥ T247 ∥ T244; T250 ∥ T249
- US4: T260 → T261 → T262; T263 ∥ T262
- Polish: T270 ∥ T271 ∥ T272 ∥ T273 ∥ T274; T275 после T272

---

## Parallel Example: User Story 1

```bash
# Красные тесты ядра параллельно:
Task: "JUnit StageLayoutTest в core/src/test/.../rocketry/StageLayoutTest.java"
Task: "JUnit StagedPerformanceTest в core/src/test/.../rocketry/StagedPerformanceTest.java"

# Данные и ассеты блока параллельно с ядром:
Task: "Блок STAGE_SEPARATOR в mod/.../registry/ModBlocks.java + вкладка"
Task: "part_properties/теги/лут/рецепт/достижение разделителя"
Task: "blockstate/model/item + gen_textures.py"
```

---

## Implementation Strategy

### MVP First (только User Story 1)

1. Phase 1 → Phase 2 → Phase 3 (US1)
2. **STOP и проверить**: `:core:test` (StageLayout/StagedPerformance зелёные) + `testStaging` на стенде
3. Демонстрация: двухступенчатый стек, скан, отделение, обломок

### Incremental Delivery

1. US1 → ступени играбельны (MVP)
2. US2 → наклон, обломки падают в стороне
3. US3 → атмосфера, нагрев, честный скан (и вакуум без изменений)
4. US4 → наведение орудия по спутникам
5. Polish → бюджет, документация, сайт, финальная проверка, коммит

---

## Notes

- Каждое физическое упрощение — в Javadoc рядом с формулой (конституция I, FR-103): плоскости разделения, пропорциональный дренаж внутри ступени, постоянный C_d, клиппинг силы сопротивления, приближение совместного наклона, неповёрнутая коллизия, вертикальный вход лома, игровая высота шкалы
- Имена API 26.2 не писать по памяти: спорные сигнатуры проверять `javap` по `~/.gradle/caches/fabric-loom/26.2/minecraft-common.jar`
- Делители тик-циклов (`reentryHeatIntervalTicks`) — обязательно ≥ 1 в `validate()`
- Не трогать поведение в вакууме: тесты 001 остаются без изменений ожиданий
