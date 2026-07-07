# Tasks: SpaceReloaded — космическая программа в Minecraft

**Input**: spec.md, plan.md, research.md, data-model.md из `/specs/001-space-mod-core/`

**Организация**: фазы Setup/Foundational + по одной фазе на user story (US1…US7). `[P]` = можно параллельно. Тесты обязательны для `core` (конституция, VII).

## Phase 1: Setup (каркас проекта)

- [ ] T001 Многомодульный Gradle-проект: `settings.gradle` (core, mod), `gradle.properties` c версиями из research.md R1 (MC 26.2, Loader 0.19.3, Loom 1.17-SNAPSHOT, Fabric API 0.154.2+26.2, Java 25), wrapper Gradle 9.5.1
- [ ] T002 `core/build.gradle`: java-library, JUnit 5, release=25
- [ ] T003 `mod/build.gradle`: плагин `net.fabricmc.fabric-loom`, `splitEnvironmentSourceSets`, зависимости `minecraft`/`fabric-loader`/`fabric-api` через `implementation`, `include project(':core')`
- [ ] T004 `fabric.mod.json` (id=spacereloaded, entrypoints main+client, depends java>=25, minecraft ~26.2), mixins-конфиги (compatibilityLevel JAVA_25), иконка-заглушка
- [ ] T005 Каркас entrypoint'ов `SpaceReloaded.java` / `SpaceReloadedClient.java`, логгер; `./gradlew build` зелёный — коммит «скелет»

## Phase 2: Foundational (ядро, блокирует все истории)

- [ ] T010 [P] `core/geometry`: `Vec3l` c pack/unpack в long (формат ванильного BlockPos.asLong для совместимости), `Vec3d`, `LongQueue`/`LongSet` минимальные структуры
- [ ] T011 [P] `core/voxel`: `VoxelView`, `GasPermeability`, `ArrayVoxelGrid` (bounding box + byte[]), билдер для тестовых сцен
- [ ] T012 Порт алгоритма из RoomCheckerPlugin → `core/sealing/GasFloodFill`: 26 направлений (сохранить массив DIRECTIONS_3D), чебышёвский радиус, статусы SEALED/LEAK/UNBOUNDED, early-exit при не-diagnostic, long-упаковка вместо объектов BlockVector3
- [ ] T013 JUnit `GasFloodFillTest`: замкнутый куб 5×5×5 = SEALED; диагональная щель = LEAK; открытый объём = UNBOUNDED; закрытая/открытая дверь; полость 10к блоков < 200 мс; early-exit vs diagnostic
- [ ] T014 [P] `core/rocketry`: PartProperties/RocketStructure/RocketPerformance + `PerformanceCalculator` (CoM, центр тяги, момент инерции точечными массами, TWR, Δv по Циолковскому, предупреждения)
- [ ] T015 [P] JUnit `PerformanceCalculatorTest`: Δv против аналитики (1%), CoM симметричной/асимметричной решётки, предупреждение при снятом двигателе
- [ ] T016 [P] `core/ballistics`: BallisticIntegrator + ImpactEnergy; JUnit против аналитики свободного падения
- [ ] T017 `core/rocketry/FlightIntegrator`: полу-неявный Эйлер, Мещерский, моменты, гиродины; JUnit: вертикальный подъём против аналитики, момент от асимметрии
- [ ] T018 Конфиг мода (json): лимиты (радиус зоны, блоки ракеты, параллельные пересчёты, TTL lease)

## Phase 3: US1 — Герметичная база (P1) 🎯 MVP

- [ ] T020 Реестры: блоки каркаса (герметичный корпус, стекло, дверь герметичная, вентиляция), тег `#spacereloaded:airtight`, `#spacereloaded:passes_gas`, автоправило полных кубов
- [ ] T021 `SectionSnapshotter`: палитровые копии секций в кубе радиуса (main thread) → `VoxelView`-адаптер с резолвом проницаемости (тег + свойства двери + полнота коллизии)
- [ ] T022 `ZoneManager` per-world: executor, `CompletableFuture`-конвейер snapshot→fill→apply (`server.execute`), коалесинг событий, лимит параллельных задач, обратный индекс позиция→зона (Long2ObjectMap)
- [ ] T023 `AtmosphereControllerBlockEntity` + блок: запуск проверки, хранение зоны в NBT, статусы, энергобуфер-заглушка (реальная энергия в US2)
- [ ] T024 Инвалидация по событиям: Fabric block-events + взрывы + смена BlockState двери; проверка принадлежности O(1); каскад на смежные зоны
- [ ] T025 Урон вакуума: тестовое измерение `spacereloaded:test_void` (безатмосферное), тик-проверка сущностей вне зон, минимальный скафандр (шлем с O₂-компонентом)
- [ ] T026 Взрывная декомпрессия: импульс сущностям к точке пробоя, партиклы/звук утечки
- [ ] T027 `ZoneStatusS2C` + HUD-индикатор (HudElementRegistry), debug-команды `/spacereloaded debug zone|vacuum`
- [ ] T028 Ручной сценарий quickstart.md проходит целиком; gametest (если жив в 26.2) на SEALED/LEAK — **коммит-веха MVP**

## Phase 4: US2 — Шлюзы и энергия (P2)

- [ ] T030 Решение D7: проверить порт Team Reborn Energy на 26.2; выбрать путь (интерфейсы TR / свой `core/energy`)
- [ ] T031 `core/energy`: EnergyGraph c кэшем топологии (если свой путь по T030)
- [ ] T032 Блоки: кабель, солнечная панель (день/ночь × solarEfficiency измерения), РИТЭГ, аккумулятор
- [ ] T033 Контроллер атмосферы потребляет энергию; деградация без энергии + оповещение
- [ ] T034 Шлюз: блок-пара дверей + контроллер шлюза, интерлок, цикл выравнивания; зона шлюза не инвалидирует основную
- [ ] T035 Тесты: JUnit на EnergyGraph; сценарий шлюзования вручную/gametest

## Phase 5: US3 — Промышленность (P3)

- [ ] T040 Руды и материалы: титан, вольфрам, сера-побочка 26.2; worldgen-фичи руд Луны/Земли
- [ ] T041 Станки: дробилка, электропечь, электролизёр, сборочный стол (рецепты MapCodec+StreamCodec, прогресс с сохранением)
- [ ] T042 Цепочки: сталь → титановый сплав → вольфрамовые заготовки → углепластик; инженерные блоки (обшивка, шпангоут, бак, сопло, теплозащита)
- [ ] T043 Датапак-реестр `part_properties` + `fuels` (Codec) — свойства деталей данными
- [ ] T044 JEI/REI-совместимость рецептов (если порт существует) или `/spacereloaded recipes dump`

## Phase 6: US4 — Ракета как сущность (P4)

- [ ] T050 **СПАЙК рендера (риск D4)**: минимальная сущность с 3 блоками, запечённый меш через Blaze3D на 26.2; выход — выбор пути (кэш-буфер / fallback поблочно)
- [ ] T051 `RocketAssembler`: BFS по 6 граням от командного модуля по тегу `#spacereloaded:rocket_parts`, лимит, ошибка с позицией лишнего блока; маппинг на `RocketStructure` ядра
- [ ] T052 `RocketEntity`: палитровая NBT-сериализация структуры, консолидация инвентарей/баков, удаление блоков из мира / восстановление при разборке
- [ ] T053 Интеграция `PerformanceCalculator`: `AssemblyResultS2C`, бортовой GUI с ЛТХ и предупреждениями
- [ ] T054 Полётный контроллер: серверный тик `FlightIntegrator`, `RocketControlC2S`/`RocketStateS2C`, интерполяция на клиенте, сиденья/positionRider
- [ ] T055 Коллизия v1: составной AABB вертикально; OBB для пассажиров при наклоне; крушение по семплам корпуса, разрушения от энергии удара
- [ ] T056 `TicketLease` (D6) + удержание чанков полёта; парковка при выходе пилота; персистентность FlightState
- [ ] T057 Сценарий SC-004: симметричная vs асимметричная ракета; gyros удерживают вертикаль

## Phase 7: US5 — Орбита (P5)

- [ ] T060 Измерение `spacereloaded:earth_orbit` (void-генератор, звёзды, низкая гравитация через dimension attributes), реестр `planets` (Codec) с Землёй/орбитой
- [ ] T061 Переход по высоте+Δv: телепорт сущности с пассажирами (lease с обеих сторон), соответствие координат старта ячейке орбиты (спиральная сетка станций)
- [ ] T062 Недобор Δv = честное падение (баллистика); вход в атмосферу — эффекты
- [ ] T063 Станционный якорь: выделение плота станции, sealing/энергия работают в орбитальном измерении (проверка US1/US2 сценариев там)

## Phase 8: US6 — Луна, Lander, возврат (P6)

- [ ] T070 Измерение `spacereloaded:moon`: ChunkGenerator (реголит, кратеры), профиль планеты, лёд в кратерах
- [ ] T071 `LanderEntity`: отделение от ракеты (ступень паркуется на орбите), управляемая посадка с расходом топлива
- [ ] T072 Персистентность Lander: NBT, выгрузка чанков, рестарт (SC-006)
- [ ] T073 ISRU: цепочка лёд→вода→электролиз→H₂/O₂ на станках US3; заправочный интерфейс Lander
- [ ] T074 Взлёт Lander, выход на орбиту, стыковка со ступенью (упрощённая: захват в радиусе)
- [ ] T075 Возвратная капсула: сборка на LaunchPad механикой US4 из титана; вход в атмосферу Земли, посадка
- [ ] T076 Сквозной сценарий SC-007 (полная петля) — ручной плейтест

## Phase 9: US7 — Орбитальная пушка (P7)

- [ ] T080 Мультиблок пушки + терминал (координаты, измерение) + предмет-целеуказатель
- [ ] T081 `KineticProjectileEntity`: баллистика ядра, межпространственный переход с сохранением скорости, персистентность
- [ ] T082 Упреждающий lease чанков цели по ETA; кратер из E_кин (ImpactEnergy), импульс и урон сущностям; освобождение lease (SC-005)
- [ ] T083 Предупреждающие эффекты в зоне поражения (свечение, звук, задержка из конфига)
- [ ] T084 Боезапас: вольфрамовые ломы из US3; энергопотребление выстрела

## Phase 10: Polish

- [ ] T090 Локализация ru_ru + en_us; звуки; финальные текстуры
- [ ] T091 Профилирование: spark/tracy на сценах SC-002; тюнинг лимитов
- [ ] T092 Публикация: README, лицензия, CI (GitHub Actions: gradle build + core tests), Modrinth-манифест

## Dependencies

- Phase 2 блокирует все истории; US1→US2 (энергия подключается к контроллеру); US4 требует материалов US3 (для сценария, не для кода); US5→US4; US6→US5+US3; US7→US5+US3. US3 независима после Phase 2.

## Статус

- [ ] Phase 1 — T001…T005
- [ ] Phase 2 — T010…T018
- [ ] Остальное — в работе
