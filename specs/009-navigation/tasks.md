# Tasks: Навигация (T900+)

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/, quickstart.md из `/specs/009-navigation/`

**Tests**: тесты ядра обязательны (конституция VII); стенд — сценарий на каждую историю.

Пути: `K/` = `core/src/main/java/org/alex_melan/spacereloaded/core/`, `KT/` = `core/src/test/java/org/alex_melan/spacereloaded/core/`, `M/` = `mod/src/main/java/org/alex_melan/spacereloaded/`, `C/` = `mod/src/client/java/org/alex_melan/spacereloaded/client/`, `R/` = `mod/src/main/resources/`, стенд — `mod/src/gametest/java/.../SpaceReloadedClientGameTest.java`.

## Phase 1: Setup

- [x] T900 Генераторы `tools/gen_009.py` (орбиты в профилях, сигнатуры, диэлектрики, рецепты, модели, достижения), `tools/gen_textures_009.py`, `tools/lang_009.py`
- [x] T901 Поля конфига 009 (`spectralDetectFraction`, `radarFrequencyMhz`, `radarDynamicRangeDb`, `calendarEpochDayJ2000`) с валидацией в `M/config/SpaceReloadedConfig.java`

## Phase 2: Foundational

- [x] T902 [P] `KT/orbit/EphemerisTest.java` + `K/orbit/Ephemeris.java` (Кеплер, период, положение Земли на J2000, энергия орбиты)
- [x] T903 [P] `KT/orbit/LambertTest.java` + `K/orbit/Lambert.java` (эталон Curtis 5.2, переход 180°, совпадение с Гоманом на круговых орбитах)
- [x] T904 [P] `KT/orbit/InterplanetaryTransferTest.java` + `K/orbit/InterplanetaryTransfer.java` (InSight, минимумы 2018–2044, между окнами ≥ 1.5 ×, возврат, Церера)
- [x] T905 [P] `K/orbit/GameCalendar.java` + тест (эпоха, ×130, первое окно через ½ суток) — тест в `EphemerisTest.calendar`
- [x] T906 `M/registry/ModRegistries.java`: `TransferSpec` — `orbit`, `mu`, `aerocapture`; профили `R/data/spacereloaded/spacereloaded/planets/*.json`

## Phase 3: US1 Перелёт по небесной механике (P1) 🎯 MVP

- [x] T907 [US1] `M/planet/TransferCosts.java` (Ламберт или таблица, кэш, `nextAffordableTick`, `minimumInPeriod`)
- [x] T908 [US1] `M/rocket/RocketEntity.java`: переход, `nextHopCost`, старт беспилотника — через `TransferCosts`; `M/planet/TransferWindows.java` — не для тел с орбитой; `M/logistics/MissionPlanning.java`
- [x] T909 [US1] `M/comms/DsnBlockEntity.java` и `K/orbit/OrbitalImaging` — общий календарь и эфемериды
- [x] T910 [US1] Стенд `testLambertTransfer` (минимум дня, отказ между окнами, Луна табличная)

## Phase 4: US2 Карта Δv и планировщик (P2)

- [x] T911 [US2] `C/gui/PlanetMapScreen.java`: цены рёбер, минимум и дни до него, Δv стека пилотируемой ракеты (по ступеням клиенту не синхронизируется — показан стек)
- [x] T912 [US2] `C/gui/PorkchopScreen.java`: Δv(дата), тепловая карта «дата × время полёта», изолинии, отметки
- [x] T913 [US2] Отказ `BUDGET_TODAY` с датой; грузовая линия уходит в доступный день; стенд `testDeltaVMap`

## Phase 5: US3 Гиперспектральный спутник (P3)

- [x] T914 [P] [US3] `KT/survey/SpectralMappingTest.java` + `K/survey/SpectralMapping.java` (GSD 2 мкм, доля смеси, порог)
- [x] T915 [US3] Предметы `hyperspectral_satellite`, `mineral_map`; `SpaceNetworkState.spectralSats`; полезная нагрузка рейса; руда галенита и приёмник PbS (кремний слеп дальше 1.1 мкм)
- [x] T916 [US3] Спектральный режим `M/orbit/OrbitalImages.java` (вместо отдельного `SpectralMaps`): заказ в ЦУПе, проявление, легенда; `M/orbit/SavedSurface.java` — разведанная выгруженная местность из region-файлов; сигнатуры `R/data/spacereloaded/spacereloaded/spectral/`
- [x] T917 [US3] Стенд `testMineralMap` (выход сланца отмечен, под грунтом — нет, масштаб по дифракции)

## Phase 6: US4 Георадар ровера (P4)

- [x] T918 [P] [US4] `KT/survey/GroundRadarTest.java` + `K/survey/GroundRadar.java` (время пробега, Френель, затухание, порог)
- [x] T919 [US4] Предметы `ground_radar`, `radargram`; ровер — радар, буфер трасс, печать; диэлектрики `R/data/spacereloaded/spacereloaded/dielectric/`
- [x] T920 [US4] `C/gui/RadargramScreen.java` (путь × глубина, шкала метров); стенд `testGroundRadar`

## Phase 7: Polish

- [x] T921 3 достижения, GUIDE, ADDONS, README, ROADMAP, сайт (содержание, фаза 19); кадры README (карта Δv, «свиная отбивная», радарограмма) — *кадр карты минералов не снят: мир стенда плоский и травяной, карта вышла бы сплошной «растительностью»; карта проверена стендом*
- [x] T922 `tools/check_zfight.py`, полный прогон (build + стенд), коммит, слияние, push

## Dependencies

Setup → Foundational (T902–T906) → US1 → US2 (карте нужны цены US1). US3 и US4 независимы от US1/US2
и друг от друга (нужны только T900–T901). Polish — последним.

## Parallel Example

T902, T903, T905, T914, T918 — разные файлы ядра, параллельно.

## Implementation Strategy

MVP — US1 (честная цена перелёта). Затем US2 (видно и планируемо), затем разведка US3, US4.
