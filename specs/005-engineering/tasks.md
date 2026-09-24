# Tasks: Инженерия — трансмиссия, станки, молот, мультиблоки, детали двигателей

**Input**: spec.md, plan.md, research*.md, data-model.md, contracts/ из `/specs/005-engineering/`

**Tests**: тесты ядра обязательны (конституция VII). Нумерация T500+.

Пути: `K/` = `core/src/main/java/org/alex_melan/spacereloaded/core/`, `KT/` = `core/src/test/java/org/alex_melan/spacereloaded/core/`, `M/` = `mod/src/main/java/org/alex_melan/spacereloaded/`, `C/` = `mod/src/client/java/org/alex_melan/spacereloaded/client/`, `R/` = `mod/src/main/resources/`, стенд — `mod/src/gametest/java/.../SpaceReloadedClientGameTest.java`.

## Phase 1: Setup

- [ ] T500 Поля конфига по `contracts/config.md` с валидацией в `M/config/SpaceReloadedConfig.java`

## Phase 2: Foundational (ядро)

- [ ] T501 [P] `KT/kinetics/KineticGraphTest.java` + `K/kinetics/KineticGraph.java` (r, e, JAM, TOO_LARGE, порядок BFS)
- [ ] T502 [P] `KT/kinetics/KineticSolverTest.java` + `K/kinetics/KineticSolver.java`, `K/kinetics/NodeLoad.java` (неявный шаг, покой, генерация, моменты через узлы)
- [ ] T503 [P] `KT/kinetics/ShaftStrengthTest.java` + `K/kinetics/ShaftStrength.java`; `FlywheelTest` + `Flywheel.java`; `WindRotorTest` + `WindRotor.java`
- [ ] T504 [P] `KT/industry/MachiningToleranceTest.java` + `K/industry/MachiningTolerance.java`; `EngineQualityTest` + `EngineQuality.java`
- [ ] T505 [P] `KT/industry/ElectrolysisStackTest.java` + `ElectrolysisStack.java`; `ColumnYieldTest` + `ColumnYield.java`
- [ ] T506 [P] `KT/multiblock/MultiblockTemplateTest.java` + `K/multiblock/MultiblockTemplate.java`

## Phase 3: US1 Трансмиссия (P1) 🎯 MVP

- [ ] T507 [US1] `M/kinetics/KineticBlock.java` (интерфейс связности) и блоки: `ShaftBlock` (дерево/сталь), `GearBlock` (малая/большая), `GearboxBlock`, `ClutchBlock`, `MotorBlock`
- [ ] T508 [US1] `M/kinetics/KineticBlockEntity.java` (ω, угол₀, t₀, синк), `MotorBlockEntity.java` (энергия ↔ механика)
- [ ] T509 [US1] `M/kinetics/KineticNetworks.java`: сбор графа из мира (правила D50), решатель, сон/пробуждение, разрушение валов, муфта, заморозка при выгрузке, синк; хуки в `IndustryStructures`/`SpaceReloaded`
- [ ] T510 [US1] Регистрации (`ModBlocks`, `ModBlockEntities`, `ModItems`, вкладка), вспомогательные блоки-роторы, ресурсы (blockstates/models/textures/loot/tags), рецепты
- [ ] T511 [US1] `C/render/KineticRenderer.java` (вращение моделей-роторов), регистрация
- [ ] T512 [US1] Jade + ПКМ-отчёт узла; локализация
- [ ] T513 [US1] Стенд `testTransmission` + `testShaftShear`

## Phase 4: US2 Маховик и станки (P2)

- [ ] T514 [US2] `FlywheelBlock`/`BlockEntity` (инерция, разрыв), `PressBlock`/`BE`, `LatheBlock`/`BE` (контейнеры, окна ω, нагрузки)
- [ ] T515 [US2] Рецепт-типы `pressing`, `machining` (`M/machine/recipe/`, `ModRecipes`), предметы листов, рецепты листов
- [ ] T516 [US2] Анимации (шток пресса, ротор маховика, шпиндель), звуки (мотор, пресс, станок)
- [ ] T517 [US2] Стенд `testPressFlywheel`

## Phase 5: US3 Молот, шаблоны, мультиблоки, руководство (P3)

- [ ] T518 [US3] Реестр шаблонов (`M/multiblock/MultiblockTemplates.java`, синхронизируемый), JSON шаблонов (стек, колонна, реголитовый реактор, катапульта)
- [ ] T519 [US3] `EngineerHammerItem` (формирование, ошибка + частица, отчёты 004)
- [ ] T520 [US3] Электролизная ячейка, тарелка колонны; формирование в `ElectrolyzerBlockEntity`/`RefineryBlockEntity` (N, K, претензии клеток), выходы по ядру; частицы работы
- [ ] T521 [US3] `EngineerManualItem` + `C/gui/EngineerManualScreen.java`
- [ ] T522 [US3] Стенд `testStackColumn`

## Phase 6: US4 Детали и качество (P4)

- [ ] T523 [US4] Компоненты `machining_step`, `machining_delta_sq`, `part_quality`; предметы полуфабрикатов и деталей; тултипы
- [ ] T524 [US4] Допуски в пресс/токарный (среднее отклонение ω за операцию), брак, балансировка; рецепты цепочек
- [ ] T525 [US4] `EngineBlock` (quality 0…10) для трёх двигателей; `AssemblyRecipe` — уровень из деталей / 5 кустарно; `PartPropertiesResolver` — множители; рецепты точных двигателей; скан-отчёт — строка качества
- [ ] T526 [US4] Стенд `testEngineQuality`

## Phase 7: US5 Ветроколесо (P5)

- [ ] T527 [US5] `WindHubBlock`/`BE`, `SailBlock`, ветер тел, ресурсы

## Phase 8: Polish

- [ ] T528 Стенд `testExplosionSealing` (T024), закрыть TODO
- [ ] T529 Достижения (4), локализация en/ru/uk полная
- [ ] T530 Документация: README, GUIDE, progression, ADDONS, ROADMAP; сайт
- [ ] T531 Полная проверка (build + стенд), коммит

## Dependencies

Setup → ядро → US1 → US2 → US4; US3 зависит только от ядра (T506) и регистраций; US5 — от US1. Polish — последним.
