# Data Model: Навигация

## Орбита тела (профиль планеты, `transfer` MapCodec)
| Поле | Тип | Смысл |
|---|---|---|
| `orbit.a_au`, `e`, `i_deg`, `node_deg`, `peri_deg`, `mean_lon_deg` | double | кеплеровы элементы J2000 |
| `mu` | double, м³/с² | гравитационный параметр (0 — малое тело) |
| `aerocapture` | bool | прибытие аэрозахватом (бесплатно) |
| `body_radius`, `parking_altitude` | double, м | парковочная орбита (есть с 003) |

Правило: цена по Ламберту — если орбиты есть у обоих концов и различаются; иначе таблица.

## Вариант перелёта (`InterplanetaryTransfer.Option`)
`totalMs`, `departureMs`, `arrivalMs`, `tofDays`. Не хранится; кэш `TransferCosts` (from, to, tick/92).

## Сигнатура минерала (`spacereloaded/spectral/<id>.json`)
`blocks` — id или `#тег`; `mineral` — ключ локализации; `map_color_id` — индекс MapColor; `band_um` — полоса.

## Диэлектрик (`spacereloaded/dielectric/<id>.json`)
`blocks`, `epsilon`, `loss_tangent`. По умолчанию: твёрдый блок 6 / 0.02, воздух 1 / 0, вода 80 / 0.5.

## Состояние
- `SpaceNetworkState.spectralSats: Map<ResourceKey<Level>, Integer>` (NBT `spectral_sats`).
- Заказ карты минералов — тот же `IMAGE_ORDER` с флагом `spectral` на предмете `orbital_image`
  (бланк `mineral_map` расходуется при заказе); легенда — `LORE` проявленной карты.
- Компонент `RADARGRAM`: `{step_m: 0.5, sample_ns: 3, traces: byte[n × 128], dimension}` на `radargram`.
- Ровер: NBT `radar` (bool), буфер трасс (≤ 256 × 128 байт), пройденный путь с последней трассы.

## Переходы
- Заказ карты минералов: `mineral_map` → (пролёт) → запертая карта `filled_map` с `MINERAL_LEGEND`.
- Радар: ровер с радаром едет → буфер → ПКМ бумагой → `radargram`, буфер пуст.
