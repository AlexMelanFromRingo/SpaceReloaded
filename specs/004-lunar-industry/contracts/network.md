# Contract: синхронизация клиента, фича 004

Новых пакетов нет. Анимации — через update tag блок-сущностей
(`ClientboundBlockEntityDataPacket.create(this)` + `getUpdateTag` → `saveCustomOnly`,
`level.sendBlockUpdated(pos, state, state, UPDATE_CLIENTS)` с одинаковым состоянием,
чтобы не будить `ServerLevelMixin`).

| BE | Поля в update tag | Когда |
|---|---|---|
| Казённик | `facing` (из состояния), `rail` (число секций), `shotTick`, `sledUntil`, `charge` (0–255) | при выстреле, при изменении рельса, при изменении заряда на ≥ 1/32 (не чаще раза в 10 тиков) |
| Реактор | `lit` (из состояния), `formed` | при смене состояния |

Визуальная капсула `CargoPodEntity` — обычная сущность (`updateInterval 1`), без данных.
Экран реактора — `ContainerData` (прогресс, энергия, буфер O₂, formed, badCell).
