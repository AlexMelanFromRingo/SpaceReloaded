# Контракт: датапак и ассеты

## Профиль небесного тела `data/<ns>/spacereloaded/planets/<body>.json` (расширение)

| Поле | Тип | По умолчанию | Валидация | Смысл |
|---|---|---|---|---|
| `atmosphere_density` | double | 0.0 | [0, 50] | Плотность у поверхности, кг/м³; 0 — вакуум (сопротивления и нагрева нет) |
| `scale_height` | double | 110.0 | [1, 100000] | Высота шкалы в игровых метрах: ρ(h) = ρ₀·exp(−h/H) |
| `datum_y` | double | 63.0 | — | Уровень отсчёта высоты h (поверхность/уровень моря тела) |

Поставляемые модом значения: `earth` — 1.225 / 110 / 63; `mars` — 0.020 / 150 / 64; `moon`, `earth_orbit`, `asteroid_belt` — поля отсутствуют (вакуум). Существующие поля (`gravity`, `atmosphere: air|co2|none`, окна, температура) не меняются; `atmosphere: "none"` и `atmosphere_density > 0` одновременно — допустимо для датапака, но плотность решает физику, а состав — дыхание/химию.

## Деталь ракеты `data/<ns>/spacereloaded/part_properties/<part>.json` (новая роль)

```json
{ "block": "spacereloaded:stage_separator", "mass_kg": 120.0, "role": "separator" }
```

Роль `separator`: только масса; задаёт горизонтальную плоскость разделения ступеней на своём ярусе. Блок обязан входить в `#spacereloaded:rocket_parts`. Несколько разделителей на одном ярусе — одна плоскость (кольцо).

## Блок `spacereloaded:stage_separator`

- Регистрация: обычный `Block` (без BE), прочность 3.0/10.0, металл, кирка (`#minecraft:mineable/pickaxe`).
- Модель `cube_column`: `stage_separator_side` (тёмное кольцо с оранжевыми пироболтами), `stage_separator_end` (крестовина); текстуры генерирует `tools/gen_textures.py` (`stage_separator()`).
- Лут: сам блок (`survives_explosion`).
- Рецепт (сборочный стол): `steel_ingot ×2`, `titanium_alloy_ingot ×1`, `minecraft:gunpowder ×1` → 1.
- Достижение `spacereloaded:staging` (родитель `rocketry`): получение разделителя.
- Книга рецептов: `OUR_BLOCK_TEX["stage_separator"] = "stage_separator_side"`, `PHASE = 5`, описание в `DESC`.

## Тип урона `data/spacereloaded/damage_type/reentry_heat.json`

```json
{ "message_id": "spacereloaded.reentry_heat", "scaling": "never", "exhaustion": 0.1, "effects": "burning" }
```

Тег `#minecraft:bypasses_armor` += `spacereloaded:reentry_heat` (как у `vacuum`/`exposure`: броня от плазмы не спасает — спасает только капсула).

## Сохранения

- `RocketEntity` NBT: `stage_active`, `stage_propellant` (список double), `debris`, `debris_ticks` — опциональны; без них ракета получает раскладку по разделителям и топливо по ёмкости ступеней.
- `KineticProjectileEntity` NBT: `guided` (bool, default false), `cd`, `area` (default из конфига).
- `MeteorEntity` NBT: без изменений (C_d/площадь — из конфига при тике).
