# Research: SpaceReloaded (Phase 0)

**Date**: 2026-07-07. Все факты проверены по первоисточникам (raw-файлы GitHub, maven-metadata, официальные блоги) многоагентным исследованием с адверсариальной верификацией.

## R1. Тулчейн Fabric для Minecraft 26.2 — ВЕРИФИЦИРОВАНО

| Параметр | Значение | Источник |
|---|---|---|
| Minecraft | **26.2** «Chaos Cubed» (16.06.2026, текущий stable; data pack format 107.1, resource 88.0) | minecraft.wiki/w/Java_Edition_26.2 |
| Java | **JDK 25** (компиляция и рантайм; `options.release = 25`, `"java": ">=25"` в depends) | docs.fabricmc.net/develop/porting, fabric-example-mod@26.2 |
| Gradle | **9.5.1** (wrapper шаблона) | fabric-example-mod@26.2 |
| Loom | **1.17-SNAPSHOT** (релизная линия 1.17.13); id плагина сменился: `net.fabricmc.fabric-loom` | maven.fabricmc.net, порт-гайд |
| Fabric Loader | **0.19.3** | meta.fabricmc.net |
| Fabric API | **0.154.x+26.2** (0.154.2 на 07.07.2026) | Modrinth |
| Маппинги | **НЕТ ВООБЩЕ** — 26.1+ деобфусцирован; yarn закрыт, intermediary не существует; мод компилируется против официальных имён Mojang | fabricmc.net/2025/10/31/obfuscation.html, fabricmc.net/2026/03/14/261.html |
| Зависимости | `minecraft "com.mojang:minecraft:26.2"`, обычные `implementation` вместо `modImplementation`; `remapJar` → просто `jar` | fabric-example-mod@26.2 build.gradle |
| Mixins | Целятся в официальные имена; `compatibilityLevel: JAVA_25`; access widener: неймспейс `official` | шаблон 26.2 |
| gradle.properties | `org.gradle.configuration-cache=false` (несовместимость IntelliJ, loom#1349) | шаблон 26.2 |

Локально: JDK 25 = Temurin 25.0.3 через sdkman (`~/.sdkman/candidates/java/25.0.3-tem`), Gradle-wrapper внутри проекта.

**Важные переименования против старых mojmap**: `net.minecraft.resources.Identifier` (пример из шаблона), `ChunkPos.containing()/pack()` — статические фабрики. Официальные имена местами отличаются от привычных — проверять по реальному jar, не по памяти.

## R2. Изменения движка 26.1/26.2, влияющие на наш дизайн

- **Рендер**: 26.1 — последняя OpenGL-only версия; в 26.2 экспериментальный **Vulkan**, обратный depth buffer; прямые GL-вызовы запрещены — только **Blaze3D API**. `ChunkSectionLayer` заменил RenderType/RenderLayer для террейна; слои блоков назначаются автоматически по свойствам спрайтов. ⇒ рендер ракеты писать строго поверх Blaze3D-абстракций, без сырого GL.
- **26.2 реестры**: block/item id разнесены в `BlockIds`, `BlockItemIds`, `ItemIds` — «отделяй id от блоков».
- **ItemStack** требует загруженного мира — для шаблонов стеков использовать `ItemStackTemplate`.
- **Физ-атрибуты сущностей** (26.2): `minecraft:air_drag_modifier`, `minecraft:bounciness`, `minecraft:friction_modifier` — пригодно для низкой гравитации/среды. Fabric (26.1) добавил **API модификации атрибутов измерения** (dimension events) — гравитацию планет делать через него, не миксинами.
- **Экраны**: управление перенесено из `Minecraft` в `Gui`/`Hud` классы; Fabric `HudRenderCallback` → `HudElementRegistry`.
- **Chunk tickets**: изменений API в 26.x не найдено — классическая схема ticket'ов действует.
- Рецепты: MapCodec + StreamCodec; торговля/фичи всё более data-driven — подтверждает ставку на датапак-реестры (FR-013).

## R3. Прайор-арт → архитектурные решения

### Решение D1: собственная физика сущностей (НЕ Valkyrien Skies 2) — ПОДТВЕРЖДЕНО
VS2 заморожен на MC 1.20.1 (последний релиз 2.4.11, 10.04.2026), 1.21-порт не выпущен, 26.x-ветки нет, физическое ядро vs-core закрыто (только maven-бинарники). Зависимость невозможна. Все живые реализации «корабль из блоков» на современном MC используют путь Create.

### Паттерн Create (верифицирован по исходникам mc1.20.1/dev) — наш блюпринт для ракет
- **Сборка**: BFS от якоря (очередь-фронтир + visited), захват блока в `StructureBlockInfo` (state + NBT блок-сущности), перевод в локальные координаты, удаление оригиналов из мира.
- **Хранение**: `Map<BlockPos, StructureBlockInfo>` внутри Entity + консолидация инвентарей; сериализация — палитровый NBT.
- **Рендер**: тесселяция всех моделей блоков один раз в кэшированный буфер (SuperByteBuffer), воспроизведение с матрицей трансформации сущности каждый кадр.
- **Коллизия**: AABB сущности → локальное пространство контрапшена → OBB против форм блоков → вектор разделения обратно в мир.
- **Пассажиры**: сиденья + `positionRider()` через `toGlobalVector(local, partialTicks)`.

### Паттерн Ad Astra (верифицирован по исходникам)
- **Планеты**: обычные датапак-измерения + data-driven запись `Planet` через Codec (dimension key, oxygen, temperature, gravity, solar_power, orbit dimension, tier). Берём этот паттерн (FR-030).
- **Кислород**: FloodFill3D по **long-упакованным BlockPos** с fastutil `LongArrayFIFOQueue`/`LongOpenHashSet`, лимит блоков = «не герметично»; предикаты: теги PASSES/BLOCKS_FLOOD_FILL, полные коллизии, po-гранная прочность, двери. ⇒ применяем long-упаковку как оптимизацию нашего 26-направленного алгоритма (семантика остаётся нашей: 26 направлений, чебышёвский радиус).
- Ракеты Ad Astra — готовые модельки-сущности (НЕ наш путь).

### Паттерн Advanced Rocketry (верифицирован по исходникам)
- Скан объёма над платформой, суммирование тяги (IRocketEngine), расхода, ёмкости баков, веса → `StatsRocket` → `EntityRocket`. Мы делаем то же, но BFS вместо объёма и честная физика вместо `requiredFuel = 2·rate·sqrt(2·h/a)`.
- **Станции**: ОДНО общее космическое измерение; станции на квадратно-спиральной сетке с ячейкой 2×stationSize, обратное разрешение станции по координатам округлением. ⇒ берём для орбитального измерения.

### Конкурентное поле
Ни одного космического мода под 26.x не существует (Modrinth-поиск по версиям 26.1/26.2 пуст). Galacticraft 5 — пре-альфа Fabric 1.21.x; Stellaris — 1.21.1, лицензия CC-BY-NC-SA (код заимствовать нельзя); Ad Astra мёртв с 03.2025. **Ниша свободна.**

## R4. spec-kit — актуальный процесс
CLI: `specify init --here --force --integration claude --script sh` (флаг `--ai` умер, теперь `--integration`). Claude-интеграция ставит скиллы `.claude/skills/speckit-*`; порядок: constitution → specify → (clarify) → plan → (checklist) → tasks → (analyze) → implement → converge. Git-ветвление вынесено в расширение `specify extension add git` (не ставили — ветками управляем сами).

## R5. Открытые вопросы, перенесённые в план

| Вопрос | Куда | План по умолчанию |
|---|---|---|
| Стандарт энергии: жив ли Team Reborn Energy API для 26.2? | M2 (US2) | Проверить на старте M2; если нет — собственный интерфейс энергии + адаптер под TR Energy при появлении |
| Точные официальные имена ticket-API (TicketType и метод add) | M3/M7 | Проверить по jar 26.2 при реализации |
| Fabric gametest-фреймворк в 26.2 | M1 | JUnit для ядра гарантирован; gametest — бонус |
| Формат `PalettedContainer.copy()` для снапшотов секций | M2 | Есть в ваниле; если сигнатура иная — копия через итерацию секции |
