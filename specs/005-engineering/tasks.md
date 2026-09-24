# Tasks: Инженерия — трансмиссия, станки, молот, мультиблоки, детали двигателей

**Input**: spec.md, plan.md, research*.md, data-model.md, contracts/ из `/specs/005-engineering/`

**Tests**: тесты ядра обязательны (конституция VII). Нумерация T500+.

Пути: `K/` = `core/src/main/java/org/alex_melan/spacereloaded/core/`, `KT/` = `core/src/test/java/org/alex_melan/spacereloaded/core/`, `M/` = `mod/src/main/java/org/alex_melan/spacereloaded/`, `C/` = `mod/src/client/java/org/alex_melan/spacereloaded/client/`, `R/` = `mod/src/main/resources/`, стенд — `mod/src/gametest/java/.../SpaceReloadedClientGameTest.java`.

## Phase 1: Setup

- [X] T500 Поля конфига по `contracts/config.md` с валидацией в `M/config/SpaceReloadedConfig.java`

## Phase 2: Foundational (ядро)

- [X] T501 [P] `KT/kinetics/KineticGraphTest.java` + `K/kinetics/KineticGraph.java` (r, e, JAM, TOO_LARGE, порядок BFS)
- [X] T502 [P] `KT/kinetics/KineticSolverTest.java` + `K/kinetics/KineticSolver.java`, `K/kinetics/NodeLoad.java` (неявный шаг, покой, генерация, моменты через узлы)
- [X] T503 [P] `KT/kinetics/ShaftStrengthTest.java` + `K/kinetics/ShaftStrength.java`; `FlywheelTest` + `Flywheel.java`; `WindRotorTest` + `WindRotor.java`
- [X] T504 [P] `KT/industry/MachiningToleranceTest.java` + `K/industry/MachiningTolerance.java`; `EngineQualityTest` + `EngineQuality.java`
- [X] T505 [P] `KT/industry/ElectrolysisStackTest.java` + `ElectrolysisStack.java`; `ColumnYieldTest` + `ColumnYield.java`
- [X] T506 [P] `KT/multiblock/MultiblockTemplateTest.java` + `K/multiblock/MultiblockTemplate.java`

## Phase 3: US1 Трансмиссия (P1) 🎯 MVP

- [X] T507 [US1] `M/kinetics/KineticBlock.java` (интерфейс связности) и блоки: `ShaftBlock` (дерево/сталь), `GearBlock` (малая/большая), `GearboxBlock`, `ClutchBlock`, `MotorBlock`
- [X] T508 [US1] `M/kinetics/KineticBlockEntity.java` (ω, угол₀, t₀, синк), `MotorBlockEntity.java` (энергия ↔ механика)
- [X] T509 [US1] `M/kinetics/KineticNetworks.java`: сбор графа из мира (правила D50), решатель, сон/пробуждение, разрушение валов, муфта, заморозка при выгрузке, синк; хуки в `IndustryStructures`/`SpaceReloaded`
- [X] T510 [US1] Регистрации (`ModBlocks`, `ModBlockEntities`, `ModItems`, вкладка), вспомогательные блоки-роторы, ресурсы (blockstates/models/textures/loot/tags), рецепты
- [X] T511 [US1] `C/render/KineticRenderer.java` (вращение моделей-роторов), регистрация
- [X] T512 [US1] Jade + ПКМ-отчёт узла; локализация
- [X] T513 [US1] Стенд `testTransmission` + `testShaftShear`

## Phase 4: US2 Маховик и станки (P2)

- [X] T514 [US2] `FlywheelBlock`/`BlockEntity` (инерция, разрыв), `PressBlock`/`BE`, `LatheBlock`/`BE` (контейнеры, окна ω, нагрузки)
- [X] T515 [US2] Рецепт-типы `pressing`, `machining` (`M/machine/recipe/`, `ModRecipes`), предметы листов, рецепты листов
- [X] T516 [US2] Анимации (шток пресса, ротор маховика, шпиндель), звуки (мотор, пресс, станок)
- [X] T517 [US2] Стенд `testPressFlywheel`

## Phase 5: US3 Молот, шаблоны, мультиблоки, руководство (P3)

- [X] T518 [US3] Реестр шаблонов (`M/multiblock/MultiblockTemplates.java`, синхронизируемый), JSON шаблонов (стек, колонна, реголитовый реактор, катапульта)
- [X] T519 [US3] `EngineerHammerItem` (формирование, ошибка + частица, отчёты 004)
- [X] T520 [US3] Электролизная ячейка, тарелка колонны; формирование в `ElectrolyzerBlockEntity`/`RefineryBlockEntity` (N, K, претензии клеток), выходы по ядру; частицы работы
- [X] T521 [US3] `EngineerManualItem` + `C/gui/EngineerManualScreen.java`
- [X] T522 [US3] Стенд `testStackColumn`

## Phase 6: US4 Детали и качество (P4)

- [X] T523 [US4] Компоненты `machining_step`, `machining_delta_sq`, `part_quality`; предметы полуфабрикатов и деталей; тултипы
- [X] T524 [US4] Допуски в пресс/токарный (среднее отклонение ω за операцию), брак, балансировка; рецепты цепочек
- [X] T525 [US4] `EngineBlock` (quality 0…10) для трёх двигателей; `AssemblyRecipe` — уровень из деталей / 5 кустарно; `PartPropertiesResolver` — множители; рецепты точных двигателей; скан-отчёт — строка качества
- [X] T526 [US4] Стенд `testEngineQuality`

## Phase 7: US5 Ветроколесо (P5)

- [X] T527 [US5] `WindHubBlock`/`BE`, `SailBlock`, ветер тел, ресурсы

## Phase 8: Polish

- [X] T528 Стенд `testExplosionSealing` (T024), закрыть TODO
- [X] T529 Достижения (4), локализация en/ru/uk полная
- [X] T530 Документация: README, GUIDE, progression, ADDONS, ROADMAP; сайт
- [X] T531 Полная проверка (build + стенд), коммит

## Phase 9: Дополнения по ходу (запросы автора)

- [X] T532 Сборочный стол: 9 входов (сетка 3×3) с миграцией старых сохранений (выход 5 → 9)
- [X] T533 Таблица масс предметов (`spacereloaded:item_mass`, блок = 10 л материала × ρ) для капсулы катапульты; гайд и руководство
- [X] T534 Облик сформированных мультиблоков: `FormableBlock` (formed) — ячейки с шиной, колонна с кольцами и узким хитбоксом, оболочка реактора
- [X] T535 Сайт: генератор `tools/sitegen/`, изометрические иконки, страница «Мультиблоки» по слоям, адаптивная вёрстка
- [X] T536 Визуальный проход, волна 1: рабочее состояние `lit` у 8 станков (свет, частицы, звуки по процессу), кабели под током (гистерезис по переданной энергии), смотровое стекло бака, шкала конденсатора, трекер солнечной панели, рама открытого люка, круглые шестерни с зубьями, концы валов у мотора/муфты/редуктора, патрон станка, ступица ветроколеса, хитбоксы сопел и дисков, звуки мотора и заряда катапульты; `CosmeticState` — смена облика не будит пересчёты
- [ ] T537 Визуальный проход, волна 2: модели командного модуля, спутников, пушки (откат), ЦУПа, ректенны, стыковочного узла, контроллера атмосферы (вентилятор)

## Dependencies

Setup → ядро → US1 → US2 → US4; US3 зависит только от ядра (T506) и регистраций; US5 — от US1. Polish — последним.
