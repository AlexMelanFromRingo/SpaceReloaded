# Расширение SpaceReloaded

Значительная часть мода вынесена в данные. Планеты, топлива, характеристики
деталей ракет, рецепты и поведение блоков в герметичности задаются JSON и
читаются из синхронизируемых реестров. Это значит, что **аддон-датапак может
добавить контент без единой строки Java** и работать поверх блоков из других
модов.

Ниже: что делается данными, что требует кода, каркас датапака и рабочие
примеры (кросс-модовые теги и новая планета).

## Что данными, что кодом

| Задача | Способ |
|---|---|
| Новая планета или орбита | датапак (профиль + измерение) |
| Новая топливная пара | датапак (реестр `fuels`) |
| Сделать блок деталью ракеты (двигатель, бак, гиродин, узел) | датапак (реестр `part_properties`), в т.ч. чужой блок |
| Рецепты дробилки, электропечи, сборочного стола | датапак (`recipe`) |
| Держит ли блок вакуум / пропускает ли газ | датапак (теги) |
| Что можно грузить в электролизёр / перегонный куб | датапак (теги) |
| Новый блок с собственной механикой, новый предмет, генерация измерения | Java (Fabric-мод) |

Ядро физики (`core/`) не зависит от `net.minecraft`: формулы Циолковского,
тяговооружённости, герметичности и баллистики можно читать и тестировать без
движка. Скрытой магии в характеристиках нет, всё в данных.

## Каркас датапака-аддона

Датапак кладётся в `saves/<мир>/datapacks/<ваш_аддон>/` (или в папку `datapacks`
сервера). Для распространения через модпак это обычный zip или папка.

```
my_spacereloaded_addon/
├── pack.mcmeta
└── data/
    ├── my_addon/                         ← ваш неймспейс
    │   └── spacereloaded/
    │       ├── planets/
    │       │   └── mars.json             ← профиль планеты
    │       └── fuels/
    │           └── methalox.json         ← топливная пара
    ├── spacereloaded/
    │   ├── part_properties/
    │   │   └── my_engine.json            ← характеристики детали
    │   ├── recipe/
    │   │   └── assembly_my_engine.json   ← рецепт
    │   └── tags/
    │       └── block/
    │           ├── airtight.json         ← дополнение тега мода
    │           └── passes_gas.json
    └── minecraft/
        └── ...                           ← при добавлении измерения
```

Важно про пути:

- **Реестры контента** (`planets`, `fuels`, `part_properties`) читаются из
  `data/<любой_неймспейс>/spacereloaded/<реестр>/`. Файлы можно класть в своём
  неймспейсе, id записи станет `<ваш_неймспейс>:<имя_файла>`.
- **Рецепты** мода задаются его типами (`spacereloaded:crushing`,
  `spacereloaded:electric_smelting`, `spacereloaded:assembly`) и лежат в
  `data/<неймспейс>/recipe/`.
- **Теги мода** дополняются по его пути: `data/spacereloaded/tags/block/<имя>.json`
  с `"replace": false`, чтобы не затирать, а добавлять.

### pack.mcmeta

```json
{
  "pack": {
    "description": "SpaceReloaded addon",
    "pack_format": 107
  }
}
```

`pack_format` для Minecraft 26.2 равен **107**. Для другой версии возьмите
формат из её `version.json`.

## Пример: кросс-модовая интеграция тегами

Самая частая задача модпакера: заставить блоки других модов вести себя в
герметичности как надо. Два тега решают почти всё.

- `#spacereloaded:airtight` — блок гарантированно держит давление (даже если по
  форме он не полный куб).
- `#spacereloaded:passes_gas` — блок пропускает газ (утечка), даже если выглядит
  сплошным. Полезно для декоративных решёток и вентиляции.

Правило по умолчанию: полный непрозрачный куб держит воздух, частичный блок
(забор, плита, решётка) газ пропускает. Теги нужны там, где это правило
ошибается на блоке из другого мода.

Сделать герметичным армированное стекло из Create:

`data/spacereloaded/tags/block/airtight.json`
```json
{
  "replace": false,
  "values": [
    { "id": "create:tinted_glass", "required": false },
    { "id": "create:framed_glass", "required": false }
  ]
}
```

Сделать решётки Tech Reborn проницаемыми для газа (чтобы строить вентшахты):

`data/spacereloaded/tags/block/passes_gas.json`
```json
{
  "replace": false,
  "values": [
    { "id": "techreborn:reinforced_glass", "required": false },
    { "id": "#c:glass_blocks", "required": false }
  ]
}
```

`"required": false` обязательно для чужих блоков: если мод не установлен,
запись пропускается, а не роняет весь тег. Можно ссылаться и на общие теги
через `#c:...`.

Полный список тегов мода:

| Тег | Тип | Смысл |
|---|---|---|
| `spacereloaded:airtight` | block | держит давление |
| `spacereloaded:passes_gas` | block | пропускает газ (утечка) |
| `spacereloaded:rocket_parts` | block | участвует в сборке ракеты |
| `spacereloaded:energy_conduit` | block | проводник энергосети |
| `spacereloaded:energy_connectable` | block | подключается к энергосети |
| `spacereloaded:space_suit` | item | часть скафандра (полный сет защищает) |
| `spacereloaded:electrolyzer_input` | item | сырьё электролизёра (лёд) |
| `spacereloaded:refinery_input` | item | сырьё перегонного куба |

## Пример: новая топливная пара

`data/<неймспейс>/spacereloaded/fuels/methalox.json`
```json
{
  "efficiency": 1.05
}
```

`efficiency` умножает эффективность топлива в расчётах. Id записи (`неймспейс:methalox`)
затем указывается в поле `fuel` двигателей и в рецептах, которые это топливо
производят.

## Пример: сделать блок деталью ракеты

Характеристики детали привязываются к блоку по его id, поэтому ролью двигателя
или бака можно наделить и блок из другого мода.

`data/spacereloaded/spacereloaded/part_properties/my_engine.json`
```json
{
  "block": "my_addon:fancy_engine",
  "mass_kg": 450.0,
  "role": "engine",
  "thrust_n": 90000.0,
  "isp_sec": 340.0,
  "fuel": "my_addon:methalox"
}
```

| Поле | Для чего | Роли |
|---|---|---|
| `block` | id блока (свой или чужой) | все |
| `mass_kg` | масса детали в расчёте центра масс и Δv | все |
| `role` | `engine`, `tank`, `command`, `seat`, `gyro`, `clamp`, `hull` | — |
| `thrust_n` | тяга в ньютонах | engine |
| `isp_sec` | удельный импульс в секундах | engine |
| `fuel` | id топлива из реестра `fuels` | engine, tank |
| `propellant_capacity_kg` | ёмкость бака в кг | tank |
| `gyro_torque_nm` | компенсирующий момент | gyro |

Двигатель обязан иметь `thrust_n`, `isp_sec` и `fuel`; бак — `propellant_capacity_kg`
и `fuel`. Смешивать типы топлива в одной ракете нельзя, сборка это проверяет.

## Пример: новая планета

Профиль планеты — один файл в реестре. Ниже он с комментариями для чтения;
**в реальном файле комментарии уберите** (строгий JSON их не допускает).

`data/<неймспейс>/spacereloaded/planets/mars.json`
```jsonc
{
  // Измерение, к которому относится профиль. Для новой планеты нужно
  // отдельное измерение (см. ниже); для орбиты/спутника можно переиспользовать.
  "dimension": "my_addon:mars",

  // Гравитация в м/с². Земля 9.81, Луна 1.62, Марс 3.72 (0.38g).
  // Влияет на Δv старта, скорость падения, дальность прыжка.
  "gravity": 3.72,

  // Дышится ли без скафандра. false → нужна маска и кислород,
  // база должна быть загерметизирована.
  "breathable": false,

  // КПД солнечных панелей. В вакууме и ближе к Солнцу выше; на Марсе
  // дальше от Солнца, поэтому меньше 1.
  "solar_efficiency": 0.6,

  // Масштаб координат к «родителю» при переходе. 8.0 значит, что орбита
  // и поверхность связаны как 1:8, как у Луны.
  "coordinate_scale": 8.0,

  // Высота, на которой ракета уходит в следующее измерение.
  "transition_altitude": 240,

  // Куда можно улететь отсюда (id измерений целей).
  "transition_targets": [ "spacereloaded:earth_orbit" ],

  // Как прибывает ракета: "descend" — спуск с торможением (поверхность),
  // "platform" — парковка на автоплатформе (орбита).
  "arrival": "descend"
}
```

Для настоящей поверхности планеты нужно ещё описать измерение обычными
ванильными файлами worldgen (это тоже данные, но их несколько):

- `data/<неймспейс>/dimension/mars.json` — измерение;
- `data/<неймспейс>/dimension_type/mars.json` — тип (высота, свет, эффект неба);
- `data/<неймспейс>/worldgen/noise_settings/mars.json` — рельеф;
- `data/<неймспейс>/worldgen/biome/mars.json` — биом (блоки поверхности, цвета).

Готовые образцы — измерения `moon` и `earth_orbit` в исходниках мода
(`mod/src/main/resources/data/spacereloaded/`). Скопируйте и поменяйте блоки
поверхности и цвета неба. Профиль в реестре `planets` связывает физику
(гравитация, атмосфера) с этим измерением.

## Рецепты станков

Три типа рецептов мода. Все лежат в `data/<неймспейс>/recipe/`.

Дробилка (выход обычно ×2):
```json
{ "type": "spacereloaded:crushing", "ingredient": "my_addon:ore",
  "result": { "id": "spacereloaded:iron_dust", "count": 2 } }
```

Электропечь (плюс любые ванильные печные рецепты работают в ней автоматически):
```json
{ "type": "spacereloaded:electric_smelting", "ingredient": "my_addon:dust",
  "result": { "id": "minecraft:iron_ingot" } }
```

Сборочный стол (список ингредиентов, порядок не важен, повторы = количество):
```json
{ "type": "spacereloaded:assembly",
  "ingredients": [ "spacereloaded:steel_ingot", "spacereloaded:steel_ingot",
                    "my_addon:methalox_valve" ],
  "result": { "id": "my_addon:fancy_engine", "count": 1 } }
```

## Что потребует Java

Датапаком нельзя добавить блок с новой логикой (свой станок, реактор, тип
двигателя-сущности), новый предмет-инструмент или генерацию измерения с нуля.
Это обычная разработка мода на Fabric. Порог низкий: регистрации идут по единому
шаблону, а физику можно дёргать из движка-независимого модуля `core/`. Исходники
и точки расширения — на [GitHub](https://github.com/AlexMelanFromRingo/SpaceReloaded).

## Fluids and the flight map

Propellants are real fluids. A datapack cannot register a `Fluid` (it is a code
registry), but an add-on mod can: register a source and a flowing fluid, a
`LiquidBlock` and a `BucketItem`, then map the fluid to a fuel id. Note that
`LiquidBlock`'s constructor asks the fluid for its source and flowing instances
immediately, so fill those references before you construct the block.

The flight map needs nothing from you. It lays bodies out by their hop distance
from Earth using the `transition_targets` graph, so a planet added by a datapack
appears on the map, with its gravity, atmosphere and transfer window, the moment
its profile loads.
