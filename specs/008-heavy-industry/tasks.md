# Tasks: Тяжёлая индустрия (T800+)

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/, quickstart.md из `/specs/008-heavy-industry/`

**Tests**: тесты ядра обязательны (конституция VII); стенд — сценарий на каждую историю.

Пути: `K/` = `core/src/main/java/org/alex_melan/spacereloaded/core/`, `KT/` = `core/src/test/java/org/alex_melan/spacereloaded/core/`, `M/` = `mod/src/main/java/org/alex_melan/spacereloaded/`, `C/` = `mod/src/client/java/org/alex_melan/spacereloaded/client/`, `R/` = `mod/src/main/resources/`, стенд — `mod/src/gametest/java/.../SpaceReloadedClientGameTest.java`.

## Phase 1: Setup

- [x] T800 Генераторы `tools/gen_008.py` (шаблоны, теги, рецепты, лут, массы, блокстейты, модели цельными объёмами), `tools/gen_textures_008.py`, `tools/lang_008.py`; звуки 008 в `tools/gen_sounds.py` и `R/assets/spacereloaded/sounds.json`
- [x] T801 Поля конфига 008 (`reactorRodSpeedPerS`, `reactorAlphaCentsPerK`, `reactorRodWorthDollars`, `centrifugeSwuPerDay`, `eafElectrodeKgPerTonne`, `dsnDriveDegPerS`, `dsnTelemetryBps`) с валидацией в `M/config/SpaceReloadedConfig.java`

## Phase 2: Foundational

- [x] T802 [P] `KT/eclss/EclssBalanceTest.java` + `K/eclss/EclssBalance.java` (стехиометрия электролиза и Сабатье, лимит H₂, возврат воды, энергия)
- [x] T803 [P] `KT/nuclear/PointKineticsTest.java` + `K/nuclear/PointKinetics.java` (мгновенный скачок, период, ρ ≥ β, S-кривая стержня, запас реактивности, Вэй–Вигнер, выгорание)
- [x] T804 [P] `KT/nuclear/ReactorThermalTest.java` + `K/nuclear/ReactorThermal.java` (Стирлинг 0.55 Карно, радиатор Стефана, равновесие с обратной связью)
- [x] T805 [P] `KT/nuclear/EnrichmentTest.java` + `K/nuclear/Enrichment.java` (ступени, продукт от N, баланс F = P + W, ЕРР)
- [x] T806 [P] `KT/cryo/AirSeparationTest.java` + `K/cryo/AirSeparation.java` (Фенске, чистота от N, энергия сжатия, аргон)
- [x] T807 [P] `KT/metallurgy/ArcFurnaceTest.java` + `K/metallurgy/ArcFurnace.java` (энергия плавки, время, электроды, продувка O₂)
- [x] T808 [P] `KT/comms/LinkBudgetTest.java` + `K/comms/LinkBudget.java` (усиление, скорость, таблица research-design §6)
- [x] T809 [P] `KT/thermal/BlackbodyTest.java` + `K/thermal/Blackbody.java` (цвет свечения по T)
- [x] T810 `M/multiblock/HammerTarget.java`; `EngineerHammerItem` через интерфейс; 4 старых контроллера реализуют его
- [x] T811 `C/render/anim/SmoothDrive.java` (пружина с ограничением скорости по времени кадра) и общий помощник свечения
- [x] T812 `C/gui/EngineerManualScreen.java`: прокрутка списка страниц

## Phase 3: US1 Стойка жизнеобеспечения (P1) 🎯 MVP

- [x] T813 [US1] Блоки `eclss_controller`, `eclss_rack_frame`, модули `ogs_module`, `sabatier_module`, `cdra_module`, `wrs_module`, `eclss_blank_panel`; шаблон `eclss_rack`
- [x] T814 [US1] `M/eclss/EclssControllerBlockEntity.java`: чтение модулей, вода (лёд/вёдра), H₂, CH₄ → метанокс с O₂ из баллона, вклад в `LifeSupportState`, экран баланса
- [x] T815 [US1] BER стойки: вентиляторы CDRA, пузыри OGS, насос WRS, подсветка Сабатье; звук
- [x] T816 [US1] Рецепты, текстуры, модели, локализация; стенд `testEclssRack`

## Phase 4: US2 Реактор деления (P2)

- [x] T817 [US2] Блоки `control_rod_drive` (ключ), `reactor_core`, `beo_reflector`, `heat_pipe`, `stirling_convertor`, `reactor_power_cap`, `radiator_panel`; шаблон `fission_reactor`
- [x] T818 [US2] `M/nuclear/ReactorBlockEntity.java`: кинетика и тепло каждый тик, стержень (экран/редстоун/SCRAM), энергия в сеть, повреждение и расплав, небо тела для радиатора
- [x] T819 [US2] BER реактора: ход стержня (плавно, SCRAM быстро), свечение тепловых труб цветом чёрного тела, поршни Стирлингов, марево радиатора; звук — *марево не делается: радиатор ≈ 400 K работает в вакууме, конвекции нет, излучение невидимо*
- [x] T820 [US2] Экран реактора (мощность, T, ρ в центах, период, стержень, SCRAM); стенд `testReactor`

## Phase 5: US3 Уран и каскад (P2)

- [x] T821 [US3] Руда `uraninite_ore` (ворлдген), предметы цепочки (`yellowcake`, `uranium_dioxide`, `uranium_tetrafluoride`, `uranium_hexafluoride` с компонентом обогащения, `kelp_ash`, `potassium_bifluoride`, `fluorine`, `zircon`, `zirconium`, `uranium_metal`, `fuel_basket`); рецепты на машинах 006
- [x] T822 [US3] Блоки `cascade_controller`, `gas_centrifuge`; шаблон `centrifuge_cascade`; `M/nuclear/CascadeBlockEntity.java` (баланс, ЕРР)
- [x] T823 [US3] BER каскада: вращение роторов, индикаторы; звук; стенд `testCascade`

## Phase 6: US4 Воздухоразделительная колонна (P3)

- [x] T824 [US4] Блоки `asu_sump` (ключ), `asu_tray`, `asu_condenser`, `asu_heat_exchanger`, `asu_compressor` (кинетический); шаблон `air_separation_column`; `argon_canister` — *конденсатор совмещён с теплообменником (одна клетка детандера), отдельного `asu_condenser` нет*
- [x] T825 [US4] `M/cryo/AirColumnBlockEntity.java`: поток от вала, чистота по Фенске, баллоны 007, аргон
- [x] T826 [US4] BER колонны: иней, пар, турбодетандер, уровень жидкого O₂; звук; стенд `testAirColumn`

## Phase 7: US5 Дуговая печь (P4)

- [x] T827 [US5] Блоки `eaf_controller`, `eaf_shell`, `eaf_roof`; `graphite_electrode`; шаблон `arc_furnace`
- [x] T828 [US5] `M/metallurgy/ArcFurnaceBlockEntity.java`: фазы плавки, энергия, электроды, продувка O₂, слив
- [x] T829 [US5] BER печи: корпус целиком (наклон), свод, электроды, дуга, струя металла; звук; стенд `testArcFurnace`

## Phase 8: US6 Антенна дальней связи (P5)

- [x] T830 [US6] Блоки `dsn_controller`, `dish_mount`, `dish_panel`, `feed_horn`; шаблон `deep_space_antenna`; `M/comms/DsnBlockEntity.java` (бюджет линии, цель, небо)
- [x] T831 [US6] `SpaceNetworkState.ground_links`, `Logistics.coverageSatisfied`, доставка снимков `OrbitalImages` со скоростью линии
- [x] T832 [US6] BER антенны: поворот тарелки по азимуту и углу места; звук; стенд `testDsnAntenna` — *азимут не нужен: небо Minecraft — одна дуга восток–запад, сопровождение по углу места покрывает все цели*

## Phase 9: Polish

- [ ] T833 5 достижений, GUIDE, ADDONS, README, ROADMAP, сайт (`content.MULTIBLOCKS`, 3D-модели); кадры README анимаций
- [ ] T834 `tools/check_zfight.py`, полный прогон (build + стенд), коммит, слияние, push

## Dependencies

Setup → Foundational → US1 → … ; US2 зависит от US3 только топливом (стенд реактора может брать корзину из креатива); US4 (кислород) полезна US5 (продувка), но не блокирует её. Polish — последним.

## Parallel Opportunities

T802–T809 — разные файлы ядра. После T810–T812 истории независимы по файлам.

## Implementation Strategy

MVP — US1 (стойка замыкает 007). Затем реактор с топливом (главная анимация), колонна, печь, антенна.
