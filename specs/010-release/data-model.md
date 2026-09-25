# Data Model: Полировка и релиз

- `OrbitalCannonBlockEntity`: NBT `last_fire` (тик выстрела); update-пакет — `saveCustomOnly` (энергия,
  ломы, цель, `last_fire`).
- `RocketEntity`: `DATA_THRUST` (Boolean) — двигатели работают.
- `MachineStatusPayload` (без изменений формата): заголовок, строки, шкалы, кнопки.
