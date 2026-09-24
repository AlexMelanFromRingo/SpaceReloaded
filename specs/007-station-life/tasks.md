# Tasks: Жизнь на станции (T700+)

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/, quickstart.md из `/specs/007-station-life/`

**Tests**: тесты ядра обязательны (конституция VII).

Пути: `K/` = `core/src/main/java/org/alex_melan/spacereloaded/core/`, `KT/` = `core/src/test/java/org/alex_melan/spacereloaded/core/`, `M/` = `mod/src/main/java/org/alex_melan/spacereloaded/`, `C/` = `mod/src/client/java/org/alex_melan/spacereloaded/client/`, `R/` = `mod/src/main/resources/`, стенд — `mod/src/gametest/java/.../SpaceReloadedClientGameTest.java`.

## Phase 1: Setup

- [ ] T700 Поля конфига (уставка давления, пороги, быстрота насоса, орбитальная g, лимиты кольца) с валидацией в `M/config/SpaceReloadedConfig.java`

## Phase 2: Foundational (ядро)

- [X] T701 [P] `K/sealing/FirstOrderMix.java` (общая первопорядковая математика) + перевод `K/electronics/CleanroomAir.java` на неё без регрессий
- [X] T702 [P] `KT/lifesupport/CabinAtmosphereTest.java` + `K/lifesupport/CabinAtmosphere.java` (массы → p, pᵢ; отрезки; время пересечения порога; критическое истечение)
- [X] T703 [P] `KT/lifesupport/MetabolismTest.java` + `K/lifesupport/Metabolism.java` (BVAD, Клейбер, уровни эффектов)
- [X] T704 [P] `KT/lifesupport/ScrubberTest.java` + `K/lifesupport/Scrubber.java` (LiOH, цеолит, выход Сабатье)
- [X] T705 [P] `KT/station/AirlockCycleTest.java` + `K/station/AirlockCycle.java` (t откачки, потери, наддув, сила на люк)
- [X] T706 [P] `KT/lifesupport/CropModelTest.java` + `K/lifesupport/CropModel.java` (культуры, DLI, точка компенсации, мощность лампы)
- [X] T707 [P] `KT/station/SpinGravityTest.java` + `K/station/SpinGravity.java`, `K/station/RotatingAssembly.java` (g, градиент, v_t, Кориолис, L, E, топливо, I, дисбаланс)
- [X] T708 [P] `KT/vehicle/TerramechanicsTest.java` + `K/vehicle/Terramechanics.java` (Беккер: z, R_c, H(s), склон, v_max, Вт·ч/км)
- [X] T709 [P] `KT/orbit/OrbitalImagingTest.java` + `K/orbit/OrbitalImaging.java` (GSD, T, полоса, T_cov)

## Phase 3: US1 Воздух станции и шлюз (P1) 🎯 MVP

- [ ] T710 [US1] `M/lifesupport/LifeSupportState.java` (SavedData газа зон, события зоны, сверка источников раз в секунду, плановая метка режима); хуки в `ZoneManager`
- [ ] T711 [US1] `M/lifesupport/GasTankBlock*` (баки O₂/N₂), отдача O₂ из электролизёра и реголитового реактора в бак, `air_separator` (Земля), N₂ из сборщика атмосферы Марса
- [ ] T712 [US1] Контроллер атмосферы: наполнение из баков до уставки, климат за энергию; удаление шкалы «атмосфера 0..1»; все зоны без бесплатного воздуха; зоны в атмосфере запирают окружающий воздух
- [ ] T713 [US1] Дыхание и эффекты: `M/lifesupport/CrewHazard.java` (пороги pCO₂/pO₂), `VacuumHazard` — пригодность зоны по давлению и pO₂; баллон маски 0.42 кг O₂
- [ ] T714 [US1] Поглотитель `co2_scrubber` с картриджами LiOH и цеолит; выдача CO₂ в Сабатье/контейнер
- [ ] T715 [US1] Насос шлюза `airlock_pump` (тамбур-зона), цикл люка по `AirlockCycle`, интерлок по Δp, защитный интерлок скафандра (конфиг)
- [ ] T716 [US1] Телеметрия и течеискатель: p, pO₂, pCO₂, запас, время до порога; HUD-индикатор газа у клиента
- [ ] T717 [US1] Регистрации, рецепты, текстуры/модели, лут, локализация US1
- [ ] T718 [US1] Стенд `testCabinAir`, `testAirlock`

## Phase 4: US2 Оранжерея (P2)

- [ ] T719 [US2] Датапак культур `spacereloaded:crops` (реестр), `hydroponic_tray` (посадка, свет от неба/лампы, вода, удобрение, сбор интегралом, события зоне)
- [ ] T720 [US2] `grow_lamp` (мощность по культуре), `biomass_oxidizer`, солома; точка компенсации CO₂
- [ ] T721 [US2] Ресурсы, рецепты, локализация; стенд `testGreenhouse`

## Phase 5: US3 Невесомость и кольцо (P3)

- [ ] T722 [US3] Орбита 0.02 g (`earth_orbit.json`), `CrewState` (сутки невесомости, «Слабость» при высадке, лечение в ≥ 0.3 g)
- [ ] T723 [US3] `spin_hub`: сборка (снимок и флудфилл в фоне, изоляция, лимит), I и дисбаланс, своя динамика ω; `rim_thruster` (топливо), `despin_motor` (противовращение)
- [ ] T724 [US3] Игрок в кольце: гравитация по высоте, Кориолис, выход через обод ω·r; разрушение при дисбалансе; вращение неба у клиента
- [ ] T725 [US3] Ресурсы, рецепты, локализация; стенд `testSpinRing`

## Phase 6: US4 Ровер (P4)

- [ ] T726 [US4] Детали ровера (шасси, колесо, Ni–Fe батарея, сиденье), `RoverEntity` (сборка/разборка, пассажиры, отсек), грунт `soil` в профилях тел
- [ ] T727 [US4] Физика ровера по `Terramechanics` (скорость, буксование, склон, занос), расход батареи; `rover_charger`; клиентский рендер и ввод
- [ ] T728 [US4] Ресурсы, рецепты, локализация; стенд `testRover`

## Phase 7: US5 Орбитальная съёмка (P5, стретч)

- [ ] T729 [US5] `imaging_satellite`, заказ в ЦУП, `orbital_image` (карта рельефа и типов поверхности), время готовности по `OrbitalImaging`

## Phase 8: Polish

- [ ] T730 Достижения (4), руководство и гайд (GUIDE, ADDONS, README), сайт, ROADMAP
- [ ] T731 Полный прогон (build + стенд), коммит, слияние, push

## Dependencies

Setup → ядро → US1 → US2 (газ зоны); US3 и US4 зависят только от ядра и Setup; US5 — от спутниковой сети 002/003 и 006. Polish — последним.

## Parallel Opportunities

T701–T709 — разные файлы ядра, параллельно. После US1: US3 и US4 независимы друг от друга.

## Implementation Strategy

MVP — US1 (газ зоны, поглотители, шлюз): сразу меняет жизнь на любой базе. Далее US2 → US3 → US4, стретч US5 при наличии времени.
