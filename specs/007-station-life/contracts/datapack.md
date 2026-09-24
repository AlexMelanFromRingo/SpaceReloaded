# Contract: датапак 007

## Культуры `data/<ns>/spacereloaded/crops/<id>.json`
```json
{ "seed": "minecraft:wheat_seeds", "harvest": "minecraft:wheat", "byproduct": "spacereloaded:straw",
  "dli": 115, "cycle_days": 80, "edible_g_m2_day": 20.0, "harvest_index": 0.4,
  "o2_g_m2_day": 56.0, "co2_g_m2_day": 77.0, "water_kg_m2_day": 11.8, "fresh_factor": 1.14 }
```
Урожай предметом: съедобная биомасса (г сухого) / масса предмета из таблицы масс.

## Грунт `data/<ns>/spacereloaded/soils/<id>.json` (реестр `spacereloaded:soils`)
```json
{ "dimension": "spacereloaded:moon", "n": 1.0, "kc": 0.14, "kphi": 0.82, "c": 0.017, "phi_deg": 35,
  "k_cm": 1.78, "surface": "spacereloaded:moon_regolith" }
```
Единицы Беккера: Н/смⁿ⁺¹, Н/смⁿ⁺², Н/см², градусы, см. Грунт действует под блоками тега
`spacereloaded:loose_soil`; прочие блоки и тела без записи — твёрдая поверхность. `surface` (необяз.) —
цвет рельефа генератора на орбитальном снимке.

## Газы
Газ зоны берётся только из блоков `spacereloaded:gas_tank` рядом с контроллером (компоненты
`gas_kind`, `gas_kg`). Машины-источники (электролизёр, реголитовый реактор, газоразделитель, сборщик
атмосферы) отдают газ в соседний баллон.
