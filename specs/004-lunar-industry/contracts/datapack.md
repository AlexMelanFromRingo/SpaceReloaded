# Contract: датапак и ресурсы, фича 004

## Профиль планеты (`data/<ns>/spacereloaded/planets/*.json`)
Новые необязательные поля: `body_radius` (м), `parking_altitude` (м), `shelter_temperature` (°C).
Значения мода: Луна 1 737 400 / 100 000 / 17; Земля 6 371 000 / 200 000; Марс 3 389 500 / 200 000; пояс 470 000 (Церера-подобное тело) / 10 000.

## Блоки (id `spacereloaded:`)
`mass_driver_breech`, `steel_coil`, `superconducting_coil`, `capacitor`, `mass_driver_sled` (без предмета), `mass_catcher`, `catcher_net`, `regolith_reactor`, `refractory_lining`, `sintered_regolith`, `lunar_bricks`. Предмет: `cargo_pod`, `slag`.

## Теги
- `spacereloaded:block/airtight` += `sintered_regolith`, `lunar_bricks`, `refractory_lining`.
- `spacereloaded:item/regolith_reactor_input` = `moon_regolith`, `moon_stone`.
- `spacereloaded:block/mass_driver_coils` = обе катушки (тир — по блоку).
- `minecraft:block/mineable/pickaxe` += все новые блоки.

## Рецепты (сборочный стол, если не указано)
| Результат | Входы |
|---|---|
| `mass_driver_breech` | 4 steel_ingot, 2 titanium_alloy, 1 comparator, 1 cargo_hold |
| `steel_coil` ×2 | 4 copper_ingot, 1 steel_ingot |
| `superconducting_coil` | 1 steel_coil, 2 tungsten_ingot, 2 titanium_alloy, 1 moon_ice |
| `capacitor` | 1 battery, 2 copper_ingot, 1 carbon_fiber |
| `cargo_pod` | 3 titanium_alloy, 1 heat_shield |
| `mass_catcher` | 4 steel_ingot, 1 cargo_hold, 1 gyroscope |
| `catcher_net` ×4 | 1 carbon_fiber, 2 steel_ingot |
| `regolith_reactor` | 4 tungsten_ingot, 1 electric_furnace, 1 hermetic_glass |
| `refractory_lining` ×2 | 2 lunar_bricks, 1 tungsten_ingot |
| `lunar_bricks` ×4 | 4 moon_stone (верстак 2×2) |
| `sintered_regolith` | slag (electric_smelting) |
| `titanium_dust` | asteroid_stone (crushing) |

## Лут
- `blocks/asteroid_stone` += `raw_titanium` шанс 0.15.
- `chests/crashed_probe`: 3–6 бросков: titanium_alloy 1–3, carbon_fiber 1–4, gyroscope 1, flight_program 1, tungsten_ingot 1–3, heat_shield 1, raw_titanium 2–5, canned_ration 1–3.

## Генерация (Луна, `moon_plains`)
`spacereloaded:lava_tube` (count 1 на чанк, feature сам решает по регионам), `spacereloaded:crashed_probe` (rarity 40, heightmap WORLD_SURFACE_WG).

## Достижения
`methalox`, `satellite`, `cannon_fire`, `mass_driver`, `mass_catch`, `lunar_air`, `underground` (impossible, выдаются кодом), `asteroid_belt` (changed_dimension).

## Звуки (`assets/spacereloaded/sounds.json`)
`mass_driver_charge`, `mass_driver_fire`, `mass_driver_sled`, `catcher_catch`, `regolith_reactor_hum`, `orbital_cannon_fire`.
