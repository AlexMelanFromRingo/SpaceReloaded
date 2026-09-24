# Tasks: Лунная индустрия — катапульта, ловушка масс, реголитовый реактор, лунные точки интереса

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/, quickstart.md из `/specs/004-lunar-industry/`

**Tests**: тесты ядра ОБЯЗАТЕЛЬНЫ (конституция VII): JUnit-задача каждого класса ядра стоит перед реализацией. Стенд получает сценарии катапульты, ловушки и реактора.

**Organization**: Setup → Foundational (ядро + регистрации + звуки + хук событий) → US1 катапульта → US2 ловушка → US3 реактор → US4 точки интереса → US5 прогрессия → Polish. Нумерация T400+ (003 закончилась на T348).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: параллельно (разные файлы, нет зависимостей от незавершённых задач)
- Пути от корня репозитория. `M/` = `mod/src/main/java/org/alex_melan/spacereloaded/`, `C/` = `mod/src/client/java/org/alex_melan/spacereloaded/client/`, `R/` = `mod/src/main/resources/`, `K/` = `core/src/main/java/org/alex_melan/spacereloaded/core/`, `KT/` = `core/src/test/java/org/alex_melan/spacereloaded/core/`
- Сборка: `JAVA_HOME=~/.sdkman/candidates/java/25.0.3-tem ./gradlew …`; стенд: `DISPLAY=:0 timeout 570 ./gradlew :mod:runClientGametest`

---

## Phase 1: Setup

- [X] T400 Поля конфига по `contracts/config.md` с Javadoc-обоснованием и проверками в `validate()` в `M/config/SpaceReloadedConfig.java`
- [X] T401 `TransferSpec` += `body_radius` (м, 0), `parking_altitude` (м, 100 000); `ThermalSpec` += `shelter_temperature` (по умолчанию NaN → = temperature); геттеры `PlanetProfile.bodyRadius()/parkingAltitude()/shelterTemperature()` в `M/registry/ModRegistries.java`
- [X] T402 [P] Профили: `moon.json` (1737400, 100000, shelter 17), `earth.json` (6371000, 200000), `mars.json` (3389500, 200000), `asteroid_belt.json` (470000, 10000) в `R/data/spacereloaded/spacereloaded/planets/`

## Phase 2: Foundational

- [X] T403 [P] Тест `KT/industry/MassDriverBallisticsTest.java`: √(2aL) одной секции и суммы тиров; v∞ из Δv и обратно (Δv(v∞) round-trip); v_req Луны 2517 ± 1 %; энергия ½mv²/η; q Земли при 2517 > 1 МПа; тепловой поток; недостающие секции
- [X] T404 `K/industry/MassDriverBallistics.java` + `K/industry/LaunchSolution.java` (record + enum Reason) по data-model, формулы и упрощения в Javadoc
- [X] T405 [P] Тест `KT/industry/RailLayoutTest.java` (обрыв на 0, предел длины, смешанные тиры → массив ускорений) и `K/industry/RailLayout.java`
- [X] T406 [P] Тест `KT/industry/CatcherOddsTest.java` (радиус от n, предел; эмпирическая доля 10 000 сэмплов vs 1 − exp(−r²/2σ²) ± 2 %; детерминизм зерна) и `K/industry/CatcherOdds.java`
- [X] T407 [P] Тест `KT/industry/RegolithYieldTest.java` (средние 10 000 циклов ± 2 %) и `K/industry/RegolithYield.java`
- [X] T408 [P] Тест `KT/industry/ReactorShellTest.java` (валидный куб для 4 направлений; каждая из 26 клеток-ошибок; центр не воздух) и `K/industry/ReactorShell.java`
- [X] T409 [P] Тест `KT/worldgen/LavaTubeLayoutTest.java` (детерминизм; сечение 7–13 × 5–9; крыша ≥ 4 вне окон; окно открыто до поверхности; объединение по чанкам = целиком) и `K/worldgen/LavaTubeLayout.java`
- [X] T410 [P] Тест `KT/worldgen/ShelterRuleTest.java` и `K/worldgen/ShelterRule.java`
- [X] T411 [P] `tools/gen_sounds.py` (sox synth → ogg: mass_driver_charge, mass_driver_fire, mass_driver_sled, catcher_catch, regolith_reactor_hum, orbital_cannon_fire) → `R/assets/spacereloaded/sounds/*.ogg` + `R/assets/spacereloaded/sounds.json`
- [X] T412 `M/registry/ModSounds.java` (SoundEvent.createVariableRangeEvent + регистрация), вызов из `M/SpaceReloaded.java`
- [X] T413 `M/industry/IndustryStructures.java`: карта «клетка → владелец-структура» по уровню, `onBlockChanged(level, pos)`, `bodyFor(level)` + `testBodyOverride`; вызов из `M/mixin/ServerLevelMixin.java`
- [X] T414 `M/industry/IndustryAdvancements.java` — `award(ServerPlayer, String id)` и `awardNearby(level, pos, radius, id)` (API проверить javap: ServerAdvancementManager.get, PlayerAdvancements.award)

## Phase 3: US1 — Катапульта (P1) 🎯 MVP

**Independent Test**: стенд `testMassDriver` — отказ «атмосфера», «рельс короток», успешный выстрел со списанием энергии = отчёт ± 1 %.

- [X] T415 [P] [US1] `M/industry/CoilBlock.java` (тир, IN_RAIL, AXIS) и `M/industry/CapacitorBlock.java` + `CapacitorBlockEntity.java` (MachineBlockEntity-накопитель, ensureAdjacentCableNetworks)
- [X] T416 [US1] `M/industry/MassDriverBreechBlock.java` (HorizontalDirectional, BE, ПКМ-отчёт, Sneak+ПКМ-выстрел, ПКМ программой — цель, redstone-фронт, компаратор, меню ChestMenu.twoRows)
- [X] T417 [US1] `M/industry/MassDriverBreechBlockEntity.java`: инвентарь 18 (WorldlyContainer), скан рельса/батареи → RailLayout/MassDriverBallistics, `solve()` → LaunchSolution, `tryFire(player)`, списание энергии по батарее, синхронизация update tag (contracts/network.md), NBT
- [X] T418 [US1] `M/industry/PodTransitState.java` (SavedData + Codec, очередь, `enqueue`) — приём/промах в US2
- [X] T419 [P] [US1] `M/industry/CargoPodEntity.java` (визуал, noSave, life 60) + регистрация в `M/registry/ModEntities.java`; `C/render/CargoPodRenderer.java` + `CargoPodRenderState.java`
- [X] T420 [US1] Регистрации блоков/BE/предметов (`mass_driver_breech`, `steel_coil`, `superconducting_coil`, `capacitor`, `mass_driver_sled`, `cargo_pod`) в `M/registry/ModBlocks.java`, `ModBlockEntities.java`, `ModItems.java`, творческая вкладка, EnergyStorage.SIDED для конденсатора
- [X] T421 [US1] `C/render/MassDriverRenderer.java` + `MassDriverRenderState.java`: волна, вспышка, салазки, свечение заряда (research-api §1–2), регистрация в `C/SpaceReloadedClient.java`
- [X] T422 [P] [US1] Ресурсы: blockstates/models/items/textures (`tools/gen_textures.py`) для 6 блоков/предметов, лут-таблицы, `mineable/pickaxe`, теги
- [X] T423 [US1] Сообщения/локализация `message.spacereloaded.mass_driver.*` en/ru/uk в `R/assets/spacereloaded/lang/`
- [X] T424 [US1] Сценарий `testMassDriver` (BX+800) в `mod/src/gametest/java/.../SpaceReloadedClientGameTest.java`

## Phase 4: US2 — Ловушка масс (P2)

**Independent Test**: стенд `testMassCatcher` — приём груза и капсулы при покрытии.

- [X] T425 [P] [US2] `M/industry/MassCatcherBlock.java`, `MassCatcherBlockEntity.java` (Container 27, сетка → CatcherOdds, отчёт, привязка программы) и `M/industry/CatcherNetBlock.java`
- [X] T426 [US2] `PodTransitState.tick(server)`: билет за 40 тиков (`M/planet/ModTickets.java` + POD_ARRIVAL), прибытие, σ по покрытию, приём/промах, визуал и звук, счётчики, достижения
- [X] T427 [US2] Флаг программы-ловушки: ПКМ программой по ловушке (компоненты PROGRAM_PAD/цели) в `M/industry/MassCatcherBlock.java`, `M/registry/ModDataComponents.java`
- [X] T428 [P] [US2] Ресурсы и локализация ловушки/сетки
- [X] T429 [US2] Сценарий `testMassCatcher` (BX+840) + round-trip кодека `PodTransit`

## Phase 5: US3 — Реголитовый реактор (P3)

**Independent Test**: стенд `testRegolithReactor`.

- [X] T430 [P] [US3] `M/industry/RegolithReactorBlock.java` (FACING, LIT), `RefractoryLiningBlock` (простой блок в ModBlocks)
- [X] T431 [US3] `M/industry/RegolithReactorBlockEntity.java`: шаблон → ReactorShell, цикл, RegolithYield, O₂ в баллон/буфер, WorldlyContainer, гул, достижение
- [X] T432 [US3] `M/industry/RegolithReactorMenu.java` + `C/gui/RegolithReactorScreen.java` + регистрации ModMenus/клиент
- [X] T433 [US3] `C/render/RegolithReactorRenderer.java` (светящееся окно)
- [X] T434 [P] [US3] Предметы `slag`, блок `sintered_regolith` (взрывостойкость 40, airtight), рецепт electric_smelting; ресурсы/локализация реактора
- [X] T435 [US3] Сценарий `testRegolithReactor` (BX+880)

## Phase 6: US4 — Точки интереса (P4)

- [X] T436 [US4] `M/worldgen/LavaTubeFeature.java` (chunk-local вырезание по LavaTubeLayout) и `M/worldgen/CrashedProbeFeature.java` (кратер, обломки, бочка с лутом); регистрация в `M/registry/ModWorldgen.java`
- [X] T437 [P] [US4] worldgen JSON (configured/placed) + `moon_plains.json` + `R/data/spacereloaded/loot_table/chests/crashed_probe.json`
- [X] T438 [US4] `Thermal.temperature(level, pos)` с ShelterRule; контроллер атмосферы по своей позиции (`M/network/Thermal.java`, `M/sealing/AtmosphereControllerBlockEntity.java`)

## Phase 7: US5 — Прогрессия, достижения, звуки (P5)

- [X] T439 [P] [US5] `lunar_bricks` (+ рецепт 2×2), crushing `asteroid_stone` → титановая пыль, лут `asteroid_stone` + сырой титан
- [X] T440 [P] [US5] Рецепты сборочного стола по `contracts/datapack.md` (10 шт.)
- [X] T441 [US5] 8 достижений JSON + выдача: Сабатье (`M/machine/SabatierReactorBlockEntity.java`), спутник, пушка (`M/cannon/OrbitalCannonBlockEntity.java` + звук ModSounds), катапульта, улов, лунный воздух, тик «под поверхностью» (`M/industry/IndustryAdvancements.java`), пояс (changed_dimension)
- [X] T442 [US5] Jade: казённик/ловушка/реактор в `M/compat/JadePlugin.java` и `mod/src/client/java/.../compat/JadeClientPlugin.java`

## Phase 8: Polish

- [X] T443 [P] Локализация en/ru/uk: все новые ключи (блоки, предметы, достижения, звуки-субтитры, сообщения), проверка отсутствия пропусков
- [X] T444 [P] Документация: README.md, README.ru.md, docs/GUIDE.ru.md (таблица длин рельса/энергий/радиусов сетки), docs/ADDONS.md (новые поля профиля, теги, генерация), specs/001-space-mod-core/progression.md (фаза 14), inspiration-backlog.md (статус), specs/ROADMAP.md
- [X] T445 `python3 tools/gen_site.py` — регенерация сайта/книги рецептов
- [X] T446 Полная проверка: `./gradlew build` + стенд; исправить регрессии
- [X] T447 Коммит среза фичи

## Dependencies

- Setup → Foundational → US1 → US2 (ловушка использует транзит и программы из US1); US3, US4, US5 зависят только от Foundational и параллельны US1/US2.
- Polish — после всех историй.

## Parallel Examples

- Foundational: T403/T405/T406/T407/T408/T409/T410/T411 — разные файлы ядра/утилит.
- US1: T415, T419, T422 параллельно; T417 после T415/T416.
- US3 и US4 можно вести параллельно с US2.

## Implementation Strategy

MVP = Setup + Foundational + US1 (катапульта с отчётом, выстрелом, анимацией). Затем US2 замыкает логистику, US3 даёт кислород, US4/US5 — мир и прогрессия. Каждая история проверяется своим сценарием стенда перед переходом к следующей.
