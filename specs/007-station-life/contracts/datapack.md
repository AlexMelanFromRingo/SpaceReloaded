# Contract: датапак 007

## Культуры `data/<ns>/spacereloaded/crops/<id>.json`
```json
{ "seed": "minecraft:wheat_seeds", "harvest": "minecraft:wheat", "byproduct": "spacereloaded:straw",
  "dli": 115, "cycle_days": 80, "edible_g_m2_day": 20.0, "harvest_index": 0.4,
  "o2_g_m2_day": 56.0, "co2_g_m2_day": 77.0, "water_kg_m2_day": 11.8, "photoperiod_h": 20 }
```
Урожай предметом: съедобная биомасса (г сухого) / масса предмета из таблицы масс.

## Грунт тела — поле `soil` профиля планеты
```json
"soil": { "n": 1.0, "kc": 0.14, "kphi": 0.82, "c": 0.017, "phi_deg": 35, "k_cm": 1.78 }
```
Единицы Беккера: Н/смⁿ⁺¹, Н/см², градусы, см. Нет поля — ровер не едет (скальный грунт — бэклог).

## Газы
Теги `spacereloaded:oxygen_sources`, `spacereloaded:nitrogen_sources` — блоки, отдающие газ контроллеру.
