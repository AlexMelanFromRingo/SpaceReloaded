# Implementation Plan: Полёт 2.0 — ступени, ориентация, атмосфера, точность орбитального удара

**Branch**: `002-staged-flight-physics` | **Date**: 2026-09-06 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-staged-flight-physics/spec.md`

## Summary

Четыре связных расширения полётной модели поверх готовых систем 001: (1) ступени — новая роль детали «разделитель», деление структуры горизонтальными плоскостями в ядре, последовательный Циолковский по ступеням, «активный вид» структуры для существующего интегратора, отделение ступени в отдельную `RocketEntity` без экипажа; (2) ориентация — командуемые тангаж/крен из штатного ввода WASD и взгляда пилота, отрабатываются уже существующим PD-контуром гиродинов; (3) атмосфера — экспоненциальный профиль плотности в профиле тела, квадратичное сопротивление в интеграторе полёта и баллистике, индекс нагрева Саттона-Грейвса, численный симулятор подъёма для скан-отчёта; (4) точность орудия — режим наведения по спутниковому покрытию, рассеивание, прогноз удара в терминале. Вся физика — в `core` с JUnit против аналитики; `mod` — адаптеры, синхронизация, HUD/экраны, датапак, стенд, документация.

## Technical Context

**Language/Version**: Java 25 (Temurin 25.0.3 через sdkman; `options.release = 25`)

**Primary Dependencies**: Minecraft 26.2 (деобфусцированный), Fabric Loader 0.19.3, Fabric API 0.154.2+26.2 (networking, keymapping, HUD, client gametest), Team Reborn Energy 5.0.0; без новых зависимостей

**Storage**: NBT сущностей (состояние ступеней, командуемые углы не сохраняются — сбрасываются), датапак-реестр планет (новые поля атмосферы), реестр part_properties (новая роль), JSON-конфиг мода (новые поля со значениями по умолчанию)

**Testing**: JUnit 5 в `core` (обязательно, против аналитики); клиентский gametest-стенд в `mod/src/gametest` (сценарии ступеней, атмосферы, точности)

**Target Platform**: клиент + выделенный сервер, одиночная и мультиплеер

**Project Type**: Minecraft-мод, два Gradle-модуля (core + mod)

**Performance Goals**: шаг полёта многоступенчатого стека из 500 деталей < 1 мс; скан с моделированием подъёма < 100 мс; TPS ≥ 19.5 при 10 летящих аппаратах; никаких per-tick аллокаций сверх существующих (активный вид структуры кэшируется и пересобирается только при смене ступени/топлива)

**Constraints**: главный поток — единственный мутатор мира; чанки отброшенных ступеней — авто-протухающие STRIKE-билеты с предельным временем жизни; косой вход лома исключён из-за бюджета чанков; клиент шлёт только ввод (штатный `Input` + один C2S-пакет отделения)

**Scale/Scope**: 1 новый блок, 1 новая роль детали, 3 новых поля профиля тела, ~14 полей конфига, 1 C2S-пакет, 1 клавиша, 2 расширенных S2C-пакета, 1 тип урона, ~8 классов ядра + ~8 тест-сьютов, 3 сценария стенда, документация на 3 языках

## Constitution Check

*GATE: пройдено до Phase 0; перепроверено после Phase 1 (см. конец файла).*

| Принцип | Соответствие плана |
|---|---|
| I. Физика прежде всего | Δv ступеней — последовательный Циолковский; сопротивление ½ρv²C_dA; терминальная скорость следует из баланса сил, не задаётся; нагрев √ρ·v³; импульс разделения сохраняет суммарный импульс. Каждое упрощение (плоскости, пропорциональный дренаж, постоянный C_d, вертикальный вход, игровая высота шкалы) документируется рядом с формулой — §D11–D14 |
| II. Ядро без движка | `StageLayout`, `StagedPerformance`, `AtmosphereProfile`, `DragBody`, `Aerothermal`, `AscentSimulator`, `StrikeSolution` — чистый Java в `core`; `mod` только маппит `RocketData`/`PlanetProfile`/конфиг в записи ядра |
| III. Событийность | Раскладка ступеней считается при сборке/загрузке и кэшируется; активный вид структуры пересобирается только при смене ступени или заметном изменении топлива (порог), не каждый тик; прогноз удара считается по запросу терминала |
| IV. Дисциплина потоков | Все новые расчёты — в главном потоке и укладываются в бюджет (< 1 мс); фоновых задач не добавляется |
| V. Межпространственная целостность | Отброшенная ступень — обычная `RocketEntity` с полётными STRIKE-билетами (persist + keep-dimension-active) и предельным временем жизни из конфига; ниже мира — утилизация; снаряд с режимом наведения персистентен |
| VI. Сервер авторитетен | Углы — из серверного `getLastClientInput()`; отделение — C2S-пакет, сервер проверяет «первый пассажир, в полёте, есть нижняя ступень»; рассеивание — серверный random; клиент только рисует HUD/терминал |
| VII. Тесты ядра | JUnit на каждый новый класс ядра против аналитики: сумма Циолковского, терминальная скорость, замкнутая форма подъёма в вакууме, распределение рассеивания, обратная совместимость в вакууме — §Testing |
| VIII. Осознанный хардкор | Отброшенная ступень падает честно (на пусковой стол, если пилот не наклонил ракету); горизонтальная скорость при посадке так же смертельна; без спутников лом не наводится; без гиродинов ракета не управляется — ничего из этого не «чинится» |

Нарушений нет. Complexity Tracking не требуется.

## Project Structure

### Documentation (this feature)

```text
specs/002-staged-flight-physics/
├── spec.md              # Спецификация (готова)
├── plan.md              # Этот файл
├── research.md          # Phase 0: решения по физике и API (готов)
├── data-model.md        # Phase 1: модель данных ядра и mod-слоя
├── quickstart.md        # Phase 1: как проверить фичу руками и стендом
├── contracts/           # Phase 1: конфиг, пакеты, датапак, клавиши
│   ├── config.md
│   ├── network.md
│   └── datapack.md
├── checklists/requirements.md
└── tasks.md             # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
core/src/main/java/org/alex_melan/spacereloaded/core/
├── rocketry/
│   ├── PartRole.java                 # + SEPARATOR
│   ├── PartProperties.java           # + separator(mass) фабрика
│   ├── FlightEnvironment.java        # + AtmosphereProfile (VACUUM по умолчанию), density(y)
│   ├── FlightIntegrator.java         # + квадратичное сопротивление (DragBody из структуры)
│   ├── RocketStructure.java          # + dragBody(cd): площади проекций из габаритов
│   ├── StageLayout.java              # NEW: деление на ступени, валидация, activeView()
│   ├── StagedPerformance.java        # NEW: ЛТХ по ступеням, суммарный Δv
│   ├── AscentSimulator.java          # NEW: численный подъём (авто-отделение, сопротивление)
│   └── Aerothermal.java              # NEW: индекс нагрева √ρ·v³, скоростной напор
├── atmosphere/
│   ├── AtmosphereProfile.java        # NEW: ρ₀, H, datum → ρ(y)
│   └── DragBody.java                 # NEW: C_d + площади проекций, сила сопротивления
└── ballistics/
    ├── ProjectileSpec.java           # massKg, cd, areaM2 (квадратичная модель)
    ├── BallisticIntegrator.java      # step(state, spec, gravity, density, dt); impactForecast()
    └── StrikeSolution.java           # NEW: режим наведения, смещение, радиус рассеивания
core/src/test/java/.../core/
├── rocketry/StageLayoutTest.java, StagedPerformanceTest.java, AscentSimulatorTest.java,
│            AerothermalTest.java, FlightIntegratorDragTest.java, FlightPerformanceBudgetTest.java
├── atmosphere/AtmosphereProfileTest.java
└── ballistics/BallisticsTest.java (обновлён), StrikeSolutionTest.java

mod/src/main/java/org/alex_melan/spacereloaded/
├── registry/ModBlocks.java           # + STAGE_SEPARATOR
├── registry/ModDamageTypes.java      # + REENTRY_HEAT
├── registry/ModRegistries.java       # PlanetProfile + atmosphere_density/scale_height/datum_y
├── registry/ModCreativeTab.java      # + разделитель
├── config/SpaceReloadedConfig.java   # + поля наклона/аэро/нагрева/разделения/рассеивания + validate
├── planet/PlanetManager.java         # + atmosphere(level) → AtmosphereProfile ядра
├── rocket/RocketData.java            # + toLayout(); Entry без изменений (role "separator")
├── rocket/RocketAssembler.java       # + валидация ступеней (ошибки с позицией)
├── rocket/RocketEntity.java          # ступени (StageState), команды ориентации, сопротивление,
│                                     #   нагрев, отделение, синк HUD, NBT, тест-хуки
├── rocket/RocketInteractions.java    # скан: StagedPerformance + AscentSimulator → расширенный отчёт
├── rocket/StageSeparation.java       # NEW: расщепление RocketData по ступени, спавн обломка, импульс
├── network/StageSeparatePayload.java # NEW: C2S «отделить ступень»
├── network/ModNetworking.java        # + регистрация/обработчик отделения
├── network/ScanReportPayload.java    # + ступени, отчёт подъёма, maxQ
├── network/CannonStatePayload.java   # + guided, spread, impactSpeed, impactEnergy
├── network/SpaceNetworkState.java    # + setCoverage (стенд/админ)
├── cannon/OrbitalCannonBlockEntity.java  # режим наведения, смещение, прогноз
├── cannon/KineticProjectileEntity.java   # квадратичное сопротивление по атмосфере, флаг guided
└── impact/MeteorEntity.java          # квадратичное сопротивление
mod/src/client/java/org/alex_melan/spacereloaded/client/
├── SpaceReloadedClient.java          # + клавиша отделения (X) → StageSeparatePayload
├── gui/RocketHud.java                # + ступень, Δv, углы, предупреждение нагрева
├── gui/ScanReportScreen.java         # + список ступеней, отчёт подъёма
└── gui/CannonTerminalScreen.java     # + режим наведения, прогноз удара
mod/src/main/resources/
├── assets/spacereloaded/{blockstates,models/block,items,textures/block}/stage_separator*
├── assets/spacereloaded/lang/{en_us,ru_ru,uk_ua}.json
├── data/spacereloaded/spacereloaded/part_properties/stage_separator.json
├── data/spacereloaded/spacereloaded/planets/{earth,mars}.json      # атмосфера
├── data/spacereloaded/{recipe/assembly_stage_separator.json, loot_table/blocks/stage_separator.json,
│    tags/block/rocket_parts.json, advancement/staging.json, damage_type/reentry_heat.json}
└── data/minecraft/tags/block/mineable/pickaxe.json
mod/src/gametest/.../SpaceReloadedClientGameTest.java  # + testStaging, testAtmosphere, testStrikeGuidance
tools/gen_textures.py, tools/gen_site.py               # текстура разделителя, описание, разделы гайда
docs/, README.md, README.ru.md, specs/001-space-mod-core/progression.md, docs/ADDONS.md
```

**Structure Decision**: структура 001 сохраняется; новый пакет `core/atmosphere` для профиля и аэродинамического тела (используется и ракетами, и баллистикой — поэтому не в `rocketry`). Ступени — в `rocketry` рядом с интегратором. В `mod` появляется один новый класс-оператор `StageSeparation` (по образцу `DockingSystem`), чтобы `RocketEntity` не рос ещё на 300 строк.

## Ключевые проектные решения

### D11. Ступени в ядре — «активный вид» вместо переписывания интегратора (FR-061…FR-064)

- `PartRole.SEPARATOR`; `StageLayout.of(structure)` сортирует уникальные Y разделителей, ступень i = детали с `y ∈ (sepY[i−1], sepY[i]]`, последняя — всё выше последнего разделителя. Разделители на одном Y — одна плоскость.
- Валидация (`StageLayout.validate(structure, commandPos)`): разделитель на Y ≥ Y командного модуля → `stage_above_command`; ниже нижнего разделителя нет деталей → `stage_empty_below`; после отсечения ступеней < k оставшиеся детали не связаны гранями с командным модулем → `stage_disconnected` (с позицией первой несвязанной детали). Ошибки возвращаются как ключ + упакованная позиция, `RocketAssembler` переводит в `Result.Error` с мировыми координатами.
- `StageLayout.activeView(activeIndex, stagePropellant[])` → `RocketStructure`: ступени ниже активной отсутствуют; активная — как есть, топливо = `stagePropellant[active]` пропорционально ёмкостям; верхние ступени: ENGINE → `hull(mass)` (нет тяги), TANK → `hull(mass + propellant)` (топливо как груз), остальное без изменений (гиродины верхних ступеней работают). `FlightIntegrator.step` вызывается с этим видом и `FlightState.propellantKg = stagePropellant[active]` — интерфейс интегратора не меняется, `withTotalPropellant` затрагивает только активные баки (у остальных ёмкость 0).
- Упрощение (документируется в Javadoc): внутри ступени баки дренируются пропорционально ёмкости; центр масс верхних ступеней учитывает их топливо как фиксированную массу.

### D12. ЛТХ ступеней и симулятор подъёма (FR-063, FR-084)

- `StagedPerformance.calculate(layout, stagePropellant, gravity)`: для i от нижней: `view_i = activeView(i)`, `perf_i = PerformanceCalculator.calculate(view_i, gravity)` — даёт TWR на момент зажигания от массы оставшегося стека и Δv_i при верхних ступенях как грузе (так как их баки стали корпусом). Суммарный Δv = Σ Δv_i. Одноступенчатый стек ⇒ `view_0 == structure` ⇒ числа совпадают с текущими (SC-001, 0.1%).
- `AscentSimulator.simulate(layout, propellant[], env, cd, targetAltitude)`: полу-неявный Эйлер dt = 0.05 с, полная тяга, вертикально; когда активная ступень выгорела и есть следующая — мгновенное отделение (масса стека уменьшается); после выгорания последней — свободный полёт до апогея; выход: `AscentReport(reached, apexM, deltaVSpentToTarget, maxQPa, timeToTargetS, stagesUsed)`. Предельное модельное время — константа (600 с) против вырожденных конфигов. В вакууме результат равен замкнутой форме `h(t) = u·τ·(w·ln w − w + 1) − g·t²/2` (проверяется тестом, SC-005).
- Δv, «потраченный до высоты», — интеграл `Σ (F/m)·dt` по активным шагам до пересечения `targetAltitude` (честная стоимость с гравитационными и аэродинамическими потерями). Скан показывает его вместо `1.15·√(2gh)`.

### D13. Атмосфера и аэродинамика (FR-080…FR-085)

- `AtmosphereProfile(surfaceDensity, scaleHeight, datumY)`, `density(y) = ρ₀·exp(−max(0, y−datum)/H)`; `VACUUM = (0, 1, 0)`. `FlightEnvironment(gravity, atmosphere)`; старый конструктор `FlightEnvironment(gravity)` = вакуум (обратная совместимость тестов и вызовов).
- `DragBody(cd, areaX, areaY, areaZ)`: `force(v, ρ) = −½ρ|v|·C_d·A_eff·v`, `A_eff = A_x|v̂_x| + A_y|v̂_y| + A_z|v̂_z|` (проекция коробки на направление движения). `RocketStructure.dragBody(cd)` берёт габариты AABB структуры. Упрощение: постоянный C_d = 0.5 (блочная ракета как тупое тело между конусом 0.3 и кубом 1.05).
- `FlightIntegrator`: `accel += drag/m` до интегрирования скорости. Полу-неявный Эйлер с квадратичным сопротивлением устойчив при `ρ·C_d·A·|v|·dt/m ≪ 1`; при игровых массах (≥ 250 кг) и скоростях (≤ 500 м/с) множитель < 0.05 — стабильно; на всякий случай сила сопротивления за шаг ограничена так, чтобы не менять знак скорости (клиппинг — документируется).
- `Aerothermal.heatIndex(ρ, v) = √ρ·v³` (Саттон-Грейвс без коэффициента и радиуса затупления — они уходят в порог конфига); `dynamicPressure(ρ, v) = ½ρv²`. Порог нагрева по умолчанию 6·10⁶ (≈ 176 м/с у поверхности Земли; штатный подъём к 450 м не превышает ~120 м/с).
- Баллистика: `ProjectileSpec(massKg, cd, areaM2)`; `BallisticIntegrator.step(state, spec, gravity, density, dt)`; `impactForecast(spec, altitude, muzzle, gravity, atmosphere)` — численный прогон до высоты цели для терминала. Линейная модель удаляется (тесты обновляются). Лом: C_d 0.1, A 0.03 м² (Ø 0.2 м) — на Земле теряет ~2 м/с² на 1500 м/с, т.е. проценты; метеорит: C_d 1.0, A 1.0 м².
- Профили тел: Земля ρ₀ = 1.225, H = 110 (игровые м), datum 63; Марс ρ₀ = 0.020, H = 150, datum 64; остальные — без полей (вакуум). Обоснование игровой высоты шкалы: высота перехода Земли 450 м; при H = 110 плотность на ней ≈ 3% приземной — «атмосфера кончается до орбиты».

### D14. Ориентация из штатного ввода (FR-070…FR-074)

- В `RocketEntity.tick` при пилоте: `Input` → направление наклона в горизонтали: `forward = (−sin yaw, cos yaw)`, `right = (cos yaw, sin yaw)` от `pilot.getYHeadRot()`; `dir = forward·(W−S) + right·(D−A)`; если `|dir| > 0`: командуемый угол `θ` растёт со скоростью `attitudeRateDegPerSec` до `attitudeMaxDeg`, иначе убывает к 0. `pitchCmd = θ·dir_z`, `rollCmd = −θ·dir_x` — знаки согласованы с `FlightIntegrator.rotate` (положительный крен наклоняет тягу к −X, положительный тангаж — к +Z); при совместном наклоне разложение приближённое (документируется).
- `ControlInput(throttle, pitchCmd, rollCmd, stabilize = true)`; без гиродинов интегратор команду не отрабатывает по построению (`gyroTorqueNm == 0`), HUD получает флаг «нет управления» из synched-поля `hasGyro`.
- Без пилота командуемые углы = 0 (автопилот держит вертикаль). Коллизия — по неповёрнутой коробке (упрощение v1 сохраняется). Синхронизация: `DATA_CMD_PITCH/ROLL` для HUD.

### D15. Отделение ступени (FR-065…FR-068)

- `StageSeparatePayload` (C2S, пустой) → `ModNetworking`: игрок — `RocketEntity.getFirstPassenger()`, борт `launched`, `activeStage < stageCount−1` → `StageSeparation.separate(level, rocket)`; иначе `sendOverlayMessage` с причиной.
- `StageSeparation.separate`: делит `RocketData` по `layout.stages[active]` (по образцу `DockingSystem.spawnPart`): нижняя часть → новая `RocketEntity` (`launched = true`, без пассажиров, `stagePropellant = [остаток активной]`, `debris = true` для предельного времени жизни), позиция = база текущего аппарата; верхняя часть — та же сущность с новой `RocketData` (нормализованные координаты, высота уменьшена на высоту сброшенной ступени), `activeStage = 0` новой раскладки, `flight.propellantKg = stagePropellant[новой активной]`. Скорости: `v_lower = v − J/m_lower`, `v_upper = v + J/m_upper` вдоль оси аппарата (J = `stageSeparationImpulseNs`), суммарный импульс сохраняется. Экипаж переносится (`positionRider` пересчитает кресла), полётная программа/цель/груз остаются на верхней части.
- Автопилот: если `autopilot && stagePropellant[active] ≤ 0.5 && есть следующая ступень` → отделение на том же тике.
- Обломок: обычная `RocketEntity` без пилота — тяга 0, полёт, `touchesGround → land()` (мягко — парковка, жёстко — `crashInto`), ниже `minY − 64` — `discard()`; `stageDebrisMaxTicks` — предохранитель. STRIKE-билеты держатся как у любого летящего борта.
- Персистентность: NBT `stage_active`, `stage_propellant` (список), `debris`, `debris_ticks`; при отсутствии — раскладка по разделителям, топливо распределяется по ёмкости ступеней. `DATA_STAGE`, `DATA_STAGE_COUNT`, `DATA_STAGE_FUEL`, `DATA_DELTA_V` (пересчёт раз в 10 тиков) — для HUD.
- Заправка/слив (`refuel/drain`): распределение по ступеням пропорционально свободной ёмкости/остатку; `propellantKg()` возвращает сумму (совместимость с колонной, рукавом, ЦУП, Jade).
- Стыковка: `rocketDataForDocking()` — сумма; после операции ступени получают топливо пропорционально ёмкости (документированное упрощение).

### D16. Точность орудия (FR-090…FR-093)

- `StrikeSolution.solve(random, guided, guidedSpread, unguidedSpread)` → смещение `(dx, dz)` равномерно в круге радиуса `spread` (`r = R·√u`, угол равномерный — равномерная плотность по площади). Наводимый: `R = cannonGuidedSpreadBlocks` (0.5 ⇒ ≤ 1 блок), ненаводимый: `cannonUnguidedSpreadBlocks` (10).
- `OrbitalCannonBlockEntity.tryFire`: `guided = SpaceNetworkState.hasCoverage(targetLevel.dimension())`; точка спавна = метка + смещение; колонна предупреждения — по метке; лог и сообщение включают режим. `KineticProjectileEntity.configure(..., guided)` + NBT; сопротивление — по `AtmosphereProfile` целевого измерения на текущей высоте.
- `snapshot()` → `CannonStatePayload` + `guided`, `spreadBlocks`, `impactSpeedMs`, `impactEnergyMJ` (прогноз `BallisticIntegrator.impactForecast` для атмосферы цели). Терминал рисует режим и прогноз.
- Спутник, развёрнутый после выстрела, на летящий лом не влияет (режим зафиксирован при выстреле).

### D17. Ассеты и данные

- Блок `stage_separator`: `cube_column` (текстура кольца с пироболтами генерируется `gen_textures.py`), `rocket_parts`, `mineable/pickaxe`, лут, рецепт сборочного стола (2 стали + титановый сплав + порох), `part_properties` (role `separator`, 120 кг), достижение `staging` (родитель `rocketry`), 3 локализации, книга рецептов (`OUR_BLOCK_TEX/DESC/PHASE`).
- Тип урона `reentry_heat` (JSON + `ModDamageTypes`), сообщение о смерти.
- Конфиг: см. `contracts/config.md`; валидация диапазонов.

## Testing (принцип VII)

- `core`: `StageLayoutTest` (деление, разделители на одном Y, ошибки валидации с позицией, активный вид: массы/тяга/ёмкость), `StagedPerformanceTest` (двухступенчатый Δv = сумма двух членов Циолковского в 1%; одноступенчатый = `PerformanceCalculator` в 0.1%; TWR ступеней), `AtmosphereProfileTest` (профиль, вакуум), `FlightIntegratorDragTest` (терминальная скорость в 2%; вакуум = старая траектория в 0.1%; стабильность на 500 м/с), `AerothermalTest` (монотонность, порог, напор), `AscentSimulatorTest` (вакуум = замкнутая форма в 1%; Земля дороже Луны; авто-отделение), `BallisticsTest` (квадратичное сопротивление снижает скорость удара; вакуум без изменений; прогноз удара), `StrikeSolutionTest` (наводимый ≤ 1; ненаводимый в круге; среднее из 20 > 1; равномерность по площади), `FlightPerformanceBudgetTest` (500 деталей: 1000 шагов < 1 с, скан < 100 мс).
- `mod` (стенд): `testStaging` (скан двухступенчатого: 2 ступени и Δv > одноступенчатого; беспилотный старт → авто-отделение → два аппарата; обломок исчезает/паркуется ≤ 60 с; ручное отделение с пилотом — валидация «только пилот/только в полёте»), `testAtmosphere` (флаг нагрева на Земле при −150 м/с и отсутствие на Луне; скан на Земле показывает отчёт подъёма), `testStrikeGuidance` (без покрытия — смещение ≤ 10 и лог; с покрытием — ≤ 1); `testOrbitalCannon` получает явное покрытие, чтобы остаться детерминированным.

## Риски и меры

| Риск | Вероятность | Мера |
|---|---|---|
| Регрессия существующих полётов из-за сопротивления (стенд/баланс) | Средняя | В вакууме — тождество со старым поведением (тест); земной подъём эталонной ракеты проверен симулятором (< 5% Δv потерь при TWR ≈ 2) |
| Обломок ступени падает на стартовый стол игрока | Высокая (по замыслу) | Осознанный хардкор + документация: наклоняйте ракету; обломок с мягким касанием остаётся аппаратом |
| Штатный ввод WASD при езде на сущности не доходит до сервера | Низкая | `Input` уже используется для jump/sprint (T054); проверяется стендом через серверный `setLastClientInput` |
| Нестабильность полу-неявного Эйлера с квадратичным сопротивлением при экстремальном датапаке (ρ₀ = 100) | Низкая | Клиппинг силы за шаг (не меняет знак скорости) + валидация диапазона плотности в реестре (≤ 50 кг/м³) |
| Рост `RocketEntity` (уже 1100 строк) | Средняя | Логика отделения — в `StageSeparation`, состояние ступеней — во вложенной записи `StageState`; тест-хуки публичные, но без новых зависимостей |
| Сохранения из 001: ракеты без NBT ступеней | Средняя | Fallback: раскладка по разделителям (их нет → одна ступень), топливо по ёмкости |

## Constitution Check (после Phase 1)

Повторная проверка после проектирования данных и контрактов: новых классов с зависимостями от Minecraft в `core` нет (`AtmosphereProfile`, `DragBody`, `StrikeSolution` — записи + статические функции); все мутации мира — в `tick`/обработчиках главного потока; билеты — только существующие типы с TTL; ввод — штатный пакет игрока и один валидируемый C2S. Отступлений нет.
