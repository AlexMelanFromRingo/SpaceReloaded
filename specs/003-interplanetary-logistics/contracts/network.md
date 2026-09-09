# Contract: сеть и синхронизация

Новых пакетов нет.

- `RocketEntity` synched data: `DATA_TRANSFER_DV` (float, м/с) — цена следующего хопа (перелёт + посадка при спуске), 0 если цели/стоимости нет; обновляется раз в 10 тиков вместе с `DATA_DELTA_V`.
- HUD: строка `hud.spacereloaded.rocket.transfer` («Перелёт: %s м/с · есть %s»), красная при нехватке.
- Jade (`sr_terminal_*`): `sr_terminal_state` (ключ локализации состояния), `sr_terminal_arg` (строка), `sr_terminal_departures`, `sr_terminal_arrivals`, `sr_terminal_mode`.
- Сообщения (чат/оверлей), ключи: `message.spacereloaded.mission.refused.<reason>` (аргументы: нужно, есть, нехватка), `message.spacereloaded.rocket.transfer_short` (нужно, есть), `message.spacereloaded.terminal.*`, `message.spacereloaded.workshop.*`.
