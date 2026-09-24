# Implementation Plan: Лунная индустрия — электромагнитная катапульта, ловушка масс, реголитовый реактор, лунные точки интереса

**Branch**: `004-lunar-industry` | **Date**: 2026-09-24 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/004-lunar-industry/spec.md`

## Summary

Луна становится промышленным узлом: (1) электромагнитная катапульта — первый в моде многоблочный механизм с кинематикой (v² = 2·Σaᵢ·Lᵢ), энергией выстрела ½mv²/η из конденсаторной батареи, честным запретом в атмосфере и анимацией (волна свечения катушек, вспышка, возврат салазок) через BlockEntityRenderer; (2) ловушка масс на орбите Земли с сеткой-уловителем — радиус захвата от площади сетки, гауссово рассеивание от спутникового покрытия, приём/промах с билетом чанка и сохранением «капсул в пути»; (3) реголитовый реактор MRE — шаблонный мультиблок 3×3×3 (реголит → O₂ в баллон + Fe/Ti-пыль + шлак), светящееся окно расплава; спечённый реголит для обваловки; (4) генерация Луны: лавовые трубки (детерминированные по регионам, chunk-local вырезание) и места крушения зондов с контейнером добычи; стабильная температура в укрытии; (5) замыкание прогрессии: потребители тупиковых ресурсов, титан пояса, 8 достижений, собственные синтезированные звуки.

Вся физика и геометрия — в чистом ядре (`core.industry`, `core.worldgen`) с JUnit против аналитики; мод-слой — блоки, BE, рендер, генерация, сеть, документация, E2E.

## Technical Context

**Language/Version**: Java 25 (`~/.sdkman/candidates/java/25.0.3-tem`), Gradle, Fabric Loom 26.2

**Primary Dependencies**: Minecraft 26.2, Fabric API (rendering-v1 `BlockEntityRendererRegistry`), Team Reborn Energy 5, Jade (мягкая), JUnit 5; утилиты сборки ресурсов: python3 + PIL (текстуры), sox + ffmpeg (синтез звуков .ogg)

**Storage**: NBT блок-сущностей (`ValueInput/ValueOutput`); новый SavedData `PodTransitState` (капсулы в пути); профиль планеты — новые поля в существующих MapCodec (`body_radius`, `parking_altitude` в TransferSpec; `shelter_temperature` в ThermalSpec) без роста числа полей RecordCodecBuilder

**Testing**: `./gradlew build` (105 тестов ядра + ≈ 30 новых), `timeout 570 ./gradlew :mod:runClientGametest` (27 сценариев + 3 новых)

**Target Platform**: Fabric клиент/сервер; стенд на WSL2 (DISPLAY=:0)

**Project Type**: двухмодульный Gradle: `core` (чистая Java) + `mod` (Fabric)

**Performance Goals**: пересчёт рельса — по событию, линия ≤ 400 секций, < 0.5 мс; простой казённика/ловушки/реактора ≤ 0.05 мс/тик; генерация трубки — O(сегментов региона × клеток чанка), < 2 мс на чанк; BER катапульты — один рендерер на казённик, отрисовка только в фазе анимации/заряда

**Constraints**: лимит 16 полей RecordCodecBuilder (профиль — только вложенные MapCodec); фичи генерации пишут только в свой чанк (плюс соседние по правилам WorldGenRegion); игрок стенда не покидает оверворлд → сценарии ловушки используют серверный хук прибытия, а приём проверяется на ловушке в оверворлде с флагом `allowCatcherAnyDimension` стенда (только гейм-тест); BER-анимация на клиенте — чистая функция времени от синхронизированных тиков

**Scale/Scope**: ядро +7 классов/+7 тестов; мод +12 блоков (казённик, 2 катушки, конденсатор, салазки-модель, ловушка, сетка, контроллер реактора, футеровка, спечённый реголит, лунный кирпич, контейнер зонда = ванильная бочка), +5 BE, +2 BER, +1 сущность (визуал капсулы), +1 меню/экран (реактор), +1 SavedData, +2 feature, +6 звуков, +8 достижений; датапак, локализация 3 языка, текстуры, документация, сайт

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Принцип | Соответствие |
|---|---|
| I. Физика прежде всего | v² = 2Σaᵢ·Lᵢ; v_req = √(v_esc² + v∞²) с v∞, обращённым из табличного Δv 003 по патч-коникам; E = ½mv²/η; q = ½ρv²; P захвата = 1 − exp(−r²/2σ²); выход MRE — из доли связанного O₂ и КПД извлечения. Упрощения (мгновенный импульс, аэрозахват бесплатен, игровое время перелёта, игровые кг на предмет, масштаб 1 E = 15 кДж из пушки, художественная длительность волны) — рядом с формулами и в research.md |
| II. Ядро без движка | `MassDriverBallistics`, `RailLayout`, `CatcherOdds`, `RegolithYield`, `ReactorShell`, `LavaTubeLayout`, `ShelterRule` — без импортов Minecraft; мод-адаптеры собирают входы (тиры клеток линии, профили, клетки оболочки) |
| III. Событийность | рельс/батарея/сетка/оболочка пересчитываются по изменению блоков (ServerLevelMixin уже рассылает `setBlock` → хук `IndustryStructures.onBlockChanged` с быстрой проверкой «клетка на оси рельса/в оболочке известной структуры»); тикеры простаивают; капсулы в пути — проверка головы очереди раз в 20 тиков |
| IV. Дисциплина потоков | все расчёты структур дешёвые (≤ 400 клеток) и выполняются синхронно в главном потоке при событии; фоновых задач нет; генерация — в воркерах генерации мира через WorldGenLevel (штатно для Feature) |
| V. Межпространственная целостность | прибытие капсулы: билет на чанк ловушки за 40 тиков до прибытия, освобождение после приёма/промаха; капсулы в пути — в SavedData, переживают перезапуск; визуальная сущность капсулы не несёт груз (исчезновение визуала ничего не теряет) |
| VI. Сервер авторитетен | выстрел, приём, реактор — сервер; клиент получает синхронизированные тики выстрела/заряд для анимации |
| VII. Тесты ядра | JUnit против аналитики до/вместе с кодом: кинематика, v_req Луны 2517 ± 1 %, энергия, напор Земли/Марса > предела, распределение захвата (10 000 выстрелов, допуск 2 %), выход реактора (10 000 циклов, 2 %), шаблон оболочки (все 26 ошибок), геометрия трубки (размеры, глубина, окно, детерминизм и сшивка по чанкам) |
| VIII. Осознанный хардкор | промах = потеря груза; короткий рельс = отказ с числом недостающих секций; атмосфера = запрет; пилот на катапульте не летает |
| Конфиг | все параметры — поля `SpaceReloadedConfig` с валидацией (`contracts/config.md`) |
| Расширяемость | радиус/парковка/температура укрытия — поля профиля (аддон-планеты); тиры катушек — теги блоков с ускорением в конфиге; таблица добычи зонда и частоты генерации — датапак |

Отступлений нет.

## Project Structure

### Documentation (this feature)

```text
specs/004-lunar-industry/
├── plan.md
├── research.md            # решения (сводка)
├── research-physics.md    # физика: катапульта, MRE, трубки (с источниками)
├── research-api.md        # MC 26.2: BER, звуки, синхронизация, генерация (javap)
├── data-model.md
├── quickstart.md
├── contracts/{config.md, datapack.md, network.md}
├── checklists/requirements.md
└── tasks.md   (/speckit-tasks)
```

### Source Code (repository root)

```text
core/src/main/java/org/alex_melan/spacereloaded/core/
├── industry/
│   ├── MassDriverBallistics.java  # NEW: v_max(Σ), v∞ из табличного Δv, v_req, энергия, напор, недостающие секции
│   ├── RailLayout.java            # NEW: раскладка линии из массива тиров (обрыв на первом не-катушке), предел длины
│   ├── CatcherOdds.java           # NEW: радиус захвата от площади сетки, P(r,σ), сэмпл смещения по зерну
│   ├── RegolithYield.java         # NEW: выход цикла (O₂, Fe, шанс Ti, шлак), бросок по RandomGenerator
│   └── ReactorShell.java          # NEW: проверка шаблона 3×3×3 по классам клеток, первая ошибка
└── worldgen/
    ├── LavaTubeLayout.java        # NEW: детерминированная трубка региона (полилиния, радиусы, окна) + membership(x,y,z)
    └── ShelterRule.java           # NEW: «укрытие»: нет неба и толща ≥ N → температура укрытия
core/src/test/java/.../core/industry/{MassDriverBallisticsTest, RailLayoutTest, CatcherOddsTest, RegolithYieldTest, ReactorShellTest}.java
core/src/test/java/.../core/worldgen/{LavaTubeLayoutTest, ShelterRuleTest}.java

mod/src/main/java/org/alex_melan/spacereloaded/
├── industry/                              # NEW пакет
│   ├── MassDriverBreechBlock.java / MassDriverBreechBlockEntity.java  # инвентарь 18 (капсула+17), цель, заряд, выстрел, анимационные тики
│   ├── CoilBlock.java                     # тир (два блока), состояния IN_RAIL + AXIS
│   ├── CapacitorBlock.java / CapacitorBlockEntity.java   # MachineBlockEntity-накопитель
│   ├── MassCatcherBlock.java / MassCatcherBlockEntity.java  # инвентарь 27, радиус, счётчики
│   ├── CatcherNetBlock.java
│   ├── RegolithReactorBlock.java / RegolithReactorBlockEntity.java / RegolithReactorMenu.java
│   ├── IndustryStructures.java            # адаптеры: сканирование линии/групп/оболочки → ядро; хук событий блоков
│   ├── PodTransitState.java               # SavedData: очередь капсул в пути, обработка прибытия (билеты)
│   ├── CargoPodEntity.java                # визуальная капсула (без груза, не сохраняется)
│   └── IndustryAdvancements.java          # выдача достижений программно (impossible-триггер + award)
├── worldgen/{LavaTubeFeature.java, CrashedProbeFeature.java}
├── registry/{ModBlocks, ModBlockEntities, ModItems, ModEntities, ModMenus, ModWorldgen, ModTags, ModSounds(NEW)}.java
├── registry/ModRegistries.java            # TransferSpec += body_radius, parking_altitude; ThermalSpec += shelter_temperature
├── network/Thermal.java                   # + temperature(level, pos) с ShelterRule
├── sealing/AtmosphereControllerBlockEntity.java  # нагрузка климата по позиции контроллера
├── cannon/OrbitalCannonBlockEntity.java   # звук ModSounds + достижение
├── network/… (спутник → достижение), machine/SabatierReactorBlockEntity.java (достижение)
├── mixin/ServerLevelMixin.java            # + IndustryStructures.onBlockChanged
├── config/SpaceReloadedConfig.java
└── compat/JadePlugin.java                 # казённик/ловушка/реактор
mod/src/client/java/org/alex_melan/spacereloaded/client/
├── render/{MassDriverRenderer, MassDriverRenderState, RegolithReactorRenderer, RegolithReactorRenderState, CargoPodRenderer, CargoPodRenderState}.java
├── gui/RegolithReactorScreen.java
└── SpaceReloadedClient.java               # регистрации BER, сущности, экрана
mod/src/main/resources/
├── assets/spacereloaded/{blockstates,models/block,models/item?,items,textures/block,textures/gui}/…
├── assets/spacereloaded/sounds.json + sounds/*.ogg (6)
├── assets/spacereloaded/lang/{en_us,ru_ru,uk_ua}.json
├── data/spacereloaded/spacereloaded/planets/{moon,asteroid_belt,mars,earth}.json  # радиус, парковка, укрытие
├── data/spacereloaded/worldgen/{configured_feature,placed_feature}/{lava_tube,crashed_probe}.json + biome/moon_plains.json
├── data/spacereloaded/loot_table/{chests/crashed_probe.json, blocks/*.json (новые), blocks/asteroid_stone.json(+титан)}
├── data/spacereloaded/recipe/… (≈ 14 новых)
├── data/spacereloaded/advancement/… (8 новых)
└── data/spacereloaded/tags/block/{airtight, coil_tier_1?…}.json, data/minecraft/tags/block/mineable/pickaxe.json
mod/src/gametest/java/.../SpaceReloadedClientGameTest.java  # testMassDriver (BX+800), testMassCatcher (BX+840), testRegolithReactor (BX+880)
tools/gen_textures.py (+ новые блоки), tools/gen_sounds.py (NEW, sox), tools/gen_site.py → docs/*.html
README.md, README.ru.md, docs/GUIDE.ru.md, docs/ADDONS.md, specs/001-space-mod-core/{progression.md, inspiration-backlog.md}, specs/ROADMAP.md
```

**Structure Decision**: новый пакет ядра `core.industry` (катапульта, ловушка, реактор) и `core.worldgen` (трубки, укрытие); пакет мода `industry` собирает все три мультиблока и транзит капсул, чтобы не раздувать `rocket/` и `machine/`.

## Ключевые проектные решения

### D30. Кинематика рельса и требуемая скорость (FR-204, FR-205)

`MassDriverBallistics.muzzleVelocity(double[] accel, double sectionLength)`: v = √(2·Σ aᵢ·Lᵢ) — точное решение для кусочно-постоянного ускорения (каждая секция разгоняет со своим пределом). `requiredVelocity(g, R, parkingAlt, tableDeltaV)`: μ = g·R² (профиль даёт поверхностную g), r_p = R + h_park; v∞ обращается из табличного Δv 003 (Δv = √(v∞² + 2μ/r_p) − √(μ/r_p)) → v∞ = √((Δv + √(μ/r_p))² − 2μ/r_p); затем v_req = √(v∞² + 2μ/R) — прямой выход с поверхности на ту же гиперболу. Луна (g 1.62, R 1 737 400, h 100 000, Δv 822): v∞ ≈ 832, v_req ≈ 2517 м/с (research-physics §1). Упрощения: мгновенный импульс, без вращения тела, аэрозахват у Земли бесплатен (как 003). Недостающие секции: n = ⌈(v_req² − v_max²)/(2·a_best·L_s)⌉ по лучшему тиру в линии.

### D31. Энергия и конденсаторы (FR-202, FR-206)

E_J = ½·m·v_req²/η; E_мод = E_J / `massDriverJoulesPerEnergy` (15 000 — масштаб орбитальной пушки: 150 000 E ↔ 2000 кг при 1500 м/с). m = `podDryMassKg` (100) + предметы × `podKgPerItem` (2). Полная капсула 17 × 64 = 1088 предметов → 2276 кг → ≈ 1.13 млн E; пустая пробная ≈ 25 000 E. Конденсатор — `MachineBlockEntity` (ёмкость `capacitorCapacity` 250 000, приём `capacitorMaxInsert` 2 000/т); батарея — связная по граням группа от казённика (≤ `capacitorMaxBlocks` 32), разряд — списание пропорционально заряду каждого блока. Энергия считается по v_req, а не v_max (лишняя длина бесплатна).

### D32. Структура катапульты (FR-200, FR-201, FR-203, FR-212)

Казённик — `HorizontalDirectionalBlock` с BE; линия — клетки `front + i·facing` для i = 1..`massDriverMaxSections` (400), пока клетка — катушка (тир по блоку: `steel_coil` → 1, `superconducting_coil` → 2); ускорения тиров — конфиг (`coilTier1AccelG` 1000, `coilTier2AccelG` 3000). Секция, уже принадлежащая другому казённику (BE хранит «владельца» через скан — клетка с IN_RAIL=true и осью, указывающей на другой казённик), не захватывается. Скан → `RailLayout` (ядро: обрыв на первом разрыве) → BE хранит длину/тиры; блоки катушек получают `IN_RAIL=true` + `AXIS` (визуал рельса). Пересчёт: `IndustryStructures.onBlockChanged(level, pos)` — быстрая проверка по карте «клетка → казённик» (Long2Object по уровню, обновляется при скане) и соседству казённика/конденсатора; иначе ничего. Зона вылета — `massDriverClearance` (8) клеток за срезом — воздух. Прогруженность — все чанки линии `hasChunkAt`. Отчёт ПКМ рукой — многострочное сообщение (ключи `message.spacereloaded.mass_driver.*`). Компаратор — заряд/требуемая энергия × 15.

### D33. Выстрел и анимация (FR-207, FR-210, FR-211)

Sneak+ПКМ / передний фронт редстоуна → `tryFire`: условия по порядку FR-207 (первый провал — причина). Успех: списание энергии, изъятие капсулы и груза, запись `PodTransit` в `PodTransitState` (содержимое как список ItemStack, цель, `arrivalTick = now + podTransitTicks`, зерно = `level.random.nextLong()`, стрелявший UUID, покрытие цели снимается в момент прибытия), визуальная `CargoPodEntity` у среза со скоростью `podVisualSpeed` (4 блока/тик) вдоль оси с подъёмом 10°, самоудаление через 60 тиков. BE синхронизирует клиенту `lastShotTick`, `railLength`, `chargeFraction`, `facing` (update tag + `sendBlockUpdated` с тем же состоянием). `MassDriverRenderer` (shouldRenderOffScreen, viewDistance 256): (а) заряд — свечение первых ⌈charge·8⌉ секций пульсом; (б) выстрел — волна: секция i горит, если `t·waveSpeed` в [i−3, i] (волна проходит рельс за `massDriverWaveTicks` = 10 тиков — художественно, реальный разгон 0.1 с), вспышка у среза при t ∈ [10, 14]; (в) салазки — модель блока `mass_driver_sled` на рельсе: после выстрела мгновенно у среза, затем едут к казённику с `t/rechargeTicks` (`massDriverSledReturnTicks` 200), это же — задержка до следующего выстрела. Свечение — `submitCustomGeometry` с `RenderTypes.lightning()` (аддитивные кубы чуть больше блока), свет `LightCoordsUtil.FULL_BRIGHT` (research-api §2).

### D34. Атмосфера (FR-208)

q = ½·ρ₀·v_req² (ρ₀ — плотность `AtmosphereSpec` у datum); предел `podMaxDynamicPressurePa` (1 МПа). Земля: 3.9 МПа, Марс (v_req ≈ 5.3 км/с при ρ 0.02): ≈ 0.28 МПа < 1 МПа — поэтому для Марса дополнительно нагрев: пиковый тепловой поток Саттона–Грейвса q̇ = k·√(ρ/r_n)·v³ против `podMaxHeatFluxWm2`; Марс превышает на порядки (research-physics §2). Интеграл по столбу атмосферы не считается: укороченная шкала высот мода занижает столб ~75× (research-physics §2) — задокументировано; критерий «на срезе» консервативен. Тело без плотности — проверка пропускается.

### D35. Ловушка масс и прибытие (FR-220…FR-226)

Ловушка — блок + BE (Container 27), работает только если измерение — орбитальная платформа (`profile.arrival == "platform"`; стенд — флаг конфига `massCatcherAnyDimension`, по умолчанию false). Сетка — связная по граням группа `catcher_net` в плоскости Y ловушки, касающаяся её (≤ `catcherNetMaxBlocks` 441). `CatcherOdds.captureRadius(n) = min(r₀ + k·√n, r_max)` (r₀ 1.5, k 0.6, r_max 12). Привязка: ПКМ полётной программой по ловушке → компоненты программы (цель = измерение ловушки, `PROGRAM_PAD` = позиция ловушки, флаг `PROGRAM_CATCHER`); ПКМ программой по казённику копирует цель в BE. `PodTransitState` (SavedData в оверворлде) хранит очередь, отсортированную по `arrivalTick`; тик сервера раз в 20 тиков: за 40 тиков до прибытия — билет `ModTickets.POD_ARRIVAL` (владелец — id капсулы, срок 200 тиков) на чанк ловушки; при прибытии: σ = покрытие измерения цели > 0 ? `podSigmaCovered` (0.5) : `podSigmaUncovered` (10); смещение = `CatcherOdds.sample(seed, σ)`; |смещение| ≤ r → приём (содержимое + капсула в контейнер, излишек — `popResource` над ловушкой, звук `catcher_catch`, визуал капсулы сверху, счётчик, достижение «Улов» стрелявшему), иначе промах (сообщение стрелявшему, если онлайн, счётчик потерь). Ловушка не найдена — промах «ловушка не найдена». Удаление записи — после обработки; билет снимается.

### D36. Реголитовый реактор (FR-230…FR-236)

Контроллер — направленный `MachineBlock` с BE (энергия 20 000, приём 200/т; инвентарь: 0 — сырьё, 1 — баллон, 2–4 — выходы Fe/Ti/шлак; буфер O₂ `reactorOxygenBuffer` 1500). Шаблон: 27 клеток куба, где контроллер — центр боковой грани, а FACING контроллера смотрит наружу; куб = контроллер − FACING·1 центр. `ReactorShell.validate(cells[27], controllerIndex)` в ядре: класс клетки (REFRACTORY / AIR / CONTROLLER / OTHER) → первая ошибка (индекс → смещение). Пересчёт по событиям (как D32: клетки куба регистрируются в карте структур). Цикл: `reactorCycleTicks` 200, энергия `reactorEnergyPerCycle` 1600 (списывается равномерно), по завершении — `RegolithYield.roll`: O₂ `reactorOxygenPerBlock` 150 ед., Fe-пыль 1 (гарантированно — 60 % массы металлов оформлено как 1 пыль), Ti-пыль с шансом `reactorTitaniumChance` 0.2, шлак 1. Сырьё: тег `spacereloaded:regolith_reactor_input` (moon_regolith, moon_stone). O₂ → баллон в слоте (урон предмета как в электролизёре), иначе буфер; полный буфер без баллона — стоп. Блок-состояние `LIT` (свет 13) + `RegolithReactorRenderer` рисует поверх окна на фасаде светящийся квад расплава с медленной пульсацией (FULL_BRIGHT). GUI — `RegolithReactorMenu` по образцу `ElectrolyzerMenu` (прогресс, энергия, буфер O₂, статус формирования). Воронки: `WorldlyContainer` — верх → 0, бок → 1, низ ← 2–4. Достижение «Лунный воздух» — игрокам в радиусе 16 при первом O₂.

### D37. Спечённый реголит и лунный кирпич (FR-235, FR-250)

Шлак → `electric_smelting` → `sintered_regolith` (твёрдость 3, взрывостойкость 40 — между камнем 6 и обсидианом 1200; ниже порога выживания метеорита 100, т.е. защищает толщиной, research-physics §5), тег `airtight`. `moon_stone` ×4 → `lunar_bricks` ×4 (ванильный крафт 2×2) — строительный блок; `asteroid_stone` → `crushing` → 1 титановая пыль (пояс = источник титана) + лут блока: сырой титан с шансом 0.15.

### D38. Лавовые трубки и укрытие (FR-240, FR-241, FR-243)

`LavaTubeLayout.forRegion(worldSeed, regionX, regionZ)` (регион 128×128 блоков): с вероятностью `tubeChancePerRegion` (0.85) — трубка: старт в регионе, направление — случайное, 4–8 сегментов по 15–20 блоков с поворотами ≤ 35°, центр на глубине 12–30 ниже datum-поверхности (относительная глубина, реальная высота берётся от heightmap в чанке), полуширина 3.5–6.5, полувысота 2.5–4.5 (эллиптическое сечение), 1–2 окна (вертикальный провал радиусом 3–4 до поверхности). `contains(x, y, z, surfaceY)` — точка внутри эллипсоидальной трубки вокруг ближайшего сегмента. `LavaTubeFeature` (одно размещение на чанк, `count 1`, без rarity) перебирает регионы, пересекающие чанк ±1 регион, и вырезает клетки только своего чанка (сшивка детерминирована — тест ядра: объединение по чанкам = целиковая трубка). Пол трубки — `moon_stone`, стены не меняются. `ShelterRule.isSheltered(skyVisible, rockAbove, minRock)`: нет неба и ≥ `shelterMinRockBlocks` (4) непрозрачных блоков над позицией → `Thermal.temperature(level, pos)` возвращает `shelter_temperature` профиля (Луна +17 °C, research-physics §9), иначе прежнюю формулу. Контроллер атмосферы считает нагрузку климата по своей позиции.

### D39. Места крушения зондов (FR-242)

`CrashedProbeFeature` (rarity 1/40 чанка, поверхность): кратер радиуса 3–5 существующей логикой формы (сферическая чаша), 6–20 обломков из набора {`rocket_hull`, `hull_plating`, `solar_panel`, `stage_separator`, `fuel_tank`} в разбросе ≤ 7 от центра (декоративные блоки-детали — они же добыча), бочка с `RandomizableContainer.setBlockEntityLootTable(level, random, pos, spacereloaded:chests/crashed_probe)` (API подтверждён javap). Таблица: 3–6 бросков из titanium_alloy, carbon_fiber, gyroscope, flight_program, tungsten_ingot, heat_shield, raw_titanium, canned_ration.

### D40. Звуки (FR-254)

`ModSounds`: `mass_driver_charge`, `mass_driver_fire`, `mass_driver_sled`, `catcher_catch`, `regolith_reactor_hum`, `orbital_cannon_fire` — `SoundEvent.createVariableRangeEvent(Identifier)` + `sounds.json`. Файлы — `tools/gen_sounds.py` (sox synth: разряд — белый шум + нисходящий синус 2 кГц→80 Гц с экспоненциальным спадом; заряд — восходящий тон; салазки — низкий гул с тремоло; улов — удар + звон; гул реактора — 60 Гц + гармоники, зацикливаемый 3 с; пушка — гром + металлический удар), вывод 44.1 кГц моно ogg vorbis. Приглушение в вакууме — существующий `SoundEngineMixin` (работает для всех звуков). Реактор проигрывает гул раз в 60 тиков при LIT.

### D41. Достижения (FR-253)

JSON-достижения с триггером `minecraft:impossible` + программная выдача `IndustryAdvancements.award(player, id)` (через `server.getAdvancements().get(id)` и `player.getAdvancements().award(holder, "done")`, API проверяется javap): `methalox` (Сабатье произвёл метанокс — игроки в радиусе 16), `satellite` (спутник развёрнут — игрок, развернувший), `cannon_fire` (пушка выстрелила — игрок-стрелок/владелец пульта), `mass_driver`, `mass_catch`, `lunar_air`, `underground` (тик игроков на Луне раз в 40 тиков: ShelterRule), `asteroid_belt` (ванильный `changed_dimension`). Родители: `methalox`←`mars`, `satellite`←`orbit`, `cannon_fire`←`cannon`, `mass_driver`←`moon`, `mass_catch`←`mass_driver`, `lunar_air`←`moon`, `underground`←`moon`, `asteroid_belt`←`orbit`.

### D42. Рецепты (FR-252)

Сборочный стол: казённик (4 стали, 2 титановых сплава, медь, компаратор… — точные входы в `contracts/datapack.md`), стальная катушка (медь ×4 + сталь), сверхпроводящая (стальная катушка + 2 вольфрама + 2 титанового сплава + лёд), конденсатор (батарея + 2 меди + углеволокно), капсула (3 титанового сплава + теплозащита), ловушка (4 стали + грузовой отсек + гироскоп), сетка (углеволокно + 2 стали → 4), контроллер реактора (4 вольфрама + электропечь + гермостекло), футеровка (лунный кирпич ×2 + вольфрам → 2).

## Testing (принцип VII)

- Ядро: `MassDriverBallisticsTest` (√(2aL) для одной секции и суммы; v_req Луны 2517 ± 1 %; v∞ → Δv круговой переход (обратимость); энергия ½mv²/η; q Земли > 1 МПа; недостающие секции), `RailLayoutTest` (обрыв, предел, смешанные тиры), `CatcherOddsTest` (радиус; эмпирическая доля приёма 10 000 сэмплов vs 1 − exp(−r²/2σ²) ± 2 %; детерминизм зерна), `RegolithYieldTest` (средние 10 000 циклов ± 2 %), `ReactorShellTest` (валидный куб; каждая из 26 клеток-ошибок; воздух в центре; контроллер не на грани), `LavaTubeLayoutTest` (детерминизм; размеры сечения в пределах; глубина крыши ≥ 4 кроме окон; окно выходит на поверхность; сшивка по чанкам), `ShelterRuleTest`.
- Стенд: игрок стенда не покидает оверворлд, а оверворлд имеет атмосферу, поэтому BE катапульты читает тело через `IndustryStructures.bodyFor(level)` с тестовым переопределением `IndustryStructures.testBodyOverride` (статическое поле, выставляется только гейм-тестом и сбрасывается в `finally`, как `cargoLineDwellTicks` в 003). `testMassDriver` (BX+800): (1) без переопределения — отказ «атмосфера»; (2) с профилем Луны: 40 секций тира 2 → отказ «рельс короток» с числом недостающих секций; 110 секций тира 2 + 5 конденсаторов от creative_power → выстрел: списанная энергия = отчёт ± 1 %, капсула и груз изъяты, запись в `PodTransitState`. `testMassCatcher` (BX+840): ловушка + сетка 5×5 в оверворлде при `massCatcherAnyDimension=true` и покрытии 1, запись транзита с прибытием «сейчас» → груз и пустая капсула в ловушке, счётчик приёмов 1. `testRegolithReactor` (BX+880): куб 3×3×3 + creative_power, 4 реголита, баллон → после циклов баллон заряжен, есть железо и шлак; выбить блок футеровки → «не сформирован», прогресс сброшен.
- Генерация: геометрия трубки — ядро (`LavaTubeLayoutTest`); размещение трубок и зондов в мире Луны — ручная проверка по quickstart (стенд не генерирует Луну: игрок не покидает оверворлд).

## Риски и меры

| Риск | Мера |
|---|---|
| BER большого размера отсекается по чанку | `shouldRenderOffScreen() = true`, `getViewDistance() = 256` (research-api §1) |
| Трубка ломает границы чанков генерации | вырезание строго внутри текущего чанка, трубка — детерминированная функция региона; тест сшивки |
| Энергия выстрела непомерна | конфиг `podKgPerItem`, `massDriverJoulesPerEnergy`; гайд с таблицей стоимости |
| Потеря груза при перезапуске | SavedData транзита (Codec), запись удаляется только после обработки; сценарий стенда проверяет round-trip кодека (encode → decode → то же содержимое) |
| Анимация дёргается на клиенте | чистая функция `(gameTime + partialTick − lastShotTick)` — без клиентского состояния |
| Звуки «пластиковые» | синтез параметризован в `gen_sounds.py`, ресурс-пак может заменить |

## Constitution Check (после Phase 1)

Сущности ядра чистые (data-model), мутации — главный поток, лимиты — в `contracts/config.md`, датапак-контракт — `contracts/datapack.md`, синхронизация — `contracts/network.md`. Отступлений нет — план готов к `/speckit-tasks`.
