# Contract: датапак 009

## Орбита в профиле планеты (`data/<ns>/spacereloaded/planets/<id>.json`)
```json
"orbit": { "a_au": 1.52371034, "e": 0.0933941, "i_deg": 1.84969142, "node_deg": 49.55953891,
           "peri_deg": -23.94362959, "mean_lon_deg": -4.55343205 },
"mu": 4.282837e13, "aerocapture": true
```
Орбитальному измерению (орбита Земли) вместо `body_radius` задаётся `"park_radius": 6571000` — радиус
парковки отлёта (радиус тела включил бы катапульту на платформе).
Без `orbit` — таблица `transfer_delta_v` и расписание окна (`synodic_period_ticks` …), как в 003.

## Сигнатуры минералов `data/<ns>/spacereloaded/spectral/<id>.json`
```json
{ "blocks": ["spacereloaded:oil_shale", "#c:ores/oil_shale"], "mineral": "kerogen",
  "map_color_id": 26, "band_um": 2.3 }
```

## Диэлектрики `data/<ns>/spacereloaded/dielectric/<id>.json`
```json
{ "blocks": ["spacereloaded:moon_regolith", "spacereloaded:moon_stone"], "epsilon": 3.0, "loss_tangent": 0.005 }
```

## Конфиг
`spectralDetectFraction` (0.25), `radarFrequencyMhz` (500), `radarDynamicRangeDb` (60),
`calendarEpochDayJ2000` (6647). `map_color_id` — индекс ванильной `MapColor` (1…63). Имя минерала —
ключ `mineral.<ns>.<mineral>` локализации.
