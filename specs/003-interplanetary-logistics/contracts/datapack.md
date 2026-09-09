# Contract: датапак

## Профиль планеты — `transfer_delta_v`

```json
{
  "dimension": "spacereloaded:earth_orbit",
  "transition_targets": ["spacereloaded:earth", "spacereloaded:moon", "spacereloaded:mars", "spacereloaded:asteroid_belt"],
  "transfer_delta_v": {
    "spacereloaded:earth": 100.0,
    "spacereloaded:moon": 3955.0,
    "spacereloaded:mars": 3613.0,
    "spacereloaded:asteroid_belt": 9650.0
  }
}
```

Ключ — id записи планеты-цели (как в `transition_targets`), значение — м/с. Отсутствие ключа = 0. Значения по умолчанию мода: Земля→орбита 0; орбита→Земля 100; орбита→Луна 3955 (TLI 3133 + LOI 822); Луна→орбита 822; орбита→Марс 3613 (TMI, прямой вход); Марс→орбита 2103 (TEI, аэрозахват); орбита→пояс 9650 (4850 + 4800); пояс→орбита 4800. Вывод — `TransferOrbitsTest`.

## Worldgen Марса

- `configured_feature/mars_ice.json`: `minecraft:ore`, size 9, target `minecraft:red_sandstone` → `spacereloaded:mars_ice`.
- `placed_feature/mars_ice.json`: count 6, in_square, height 4…90, biome.
- `biome/mars_plains.json`: шаг `UNDERGROUND_ORES` += `spacereloaded:mars_ice`.

## Теги

- `spacereloaded:airtight` (block) += `module_hull`, `docking_port` (закрытый).
- `spacereloaded:electrolyzer_input` (item) += `mars_ice`.
- `minecraft:mineable/pickaxe` += `cargo_terminal`, `docking_port`, `module_hull`, `mars_ice`; `needs_iron_tool` += `cargo_terminal`, `docking_port`.

## Рецепты (сборочный стол)

- `assembly_cargo_terminal`: сталь ×2, медь ×1, полётная программа ×1 → терминал.
- `assembly_docking_port`: сталь ×2, герметичный люк ×1, стыковочный узел ×1 → порт.
- `module_hull` — без рецепта (только конверсия).

## Достижения

`cargo_line` (терминал; родитель `docking`), `wet_workshop` (порт; родитель `sealed`), `mars` (changed_dimension → mars; родитель `orbit`), `mars_return` (from mars → earth_orbit; родитель `mars`).
