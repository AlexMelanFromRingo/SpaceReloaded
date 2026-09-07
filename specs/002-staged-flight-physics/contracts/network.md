# Контракт: сеть и ввод

## C2S `spacereloaded:stage_separate` — `StageSeparatePayload`

- Поля: нет (действие относится к транспорту отправителя).
- Клиент: клавиша `key.spacereloaded.stage` (по умолчанию **X**, категория Gameplay); отправляется на каждый `consumeClick()` только если игрок сидит в `RocketEntity`.
- Сервер (обработчик в `ModNetworking`): `player.getVehicle() instanceof RocketEntity rocket` и `rocket.getFirstPassenger() == player` — иначе тихо игнорируется (попутчик не командует). Далее `rocket.requestStageSeparation(player)` → `Component` в оверлей:
  - `message.spacereloaded.stage.not_launched` — на стоянке;
  - `message.spacereloaded.stage.last` — нижней ступени нет;
  - `message.spacereloaded.stage.separated` (`%s` = номер отброшенной ступени, `%s` = осталось ступеней) — успех.
- Без побочных эффектов при отказе.

## Штатный ввод пилота (без нового пакета)

`ServerPlayer.getLastClientInput()`: `jump` — тяга (как раньше), `sprint` — цель на стоянке (как раньше), `forward/backward/left/right` — направление наклона относительно `getYHeadRot()` пилота. Сервер интегрирует командуемый угол сам (скорость и предел из конфига); клиент ничего не считает.

## S2C `spacereloaded:scan_report` — `ScanReportPayload` (расширение)

Добавляемые поля (в конец кодека, порядок фиксирован):

| Поле | Тип | Смысл |
|---|---|---|
| `stages` | список `StageLine(index:int, deltaV:double, twr:double, hasEngines:bool)` | ЛТХ ступеней снизу вверх |
| `ascentReached` | bool | симулятор достиг высоты перехода |
| `ascentDeltaV` | double | Δv, потраченный до высоты перехода (0, если не достигнута) |
| `ascentApexM` | double | апогей моделирования |
| `maxQPa` | double | пиковый скоростной напор |
| `maxQExceeded` | bool | превышен порог `maxDynamicPressurePa` |

Существующее поле `requiredDeltaV` теперь равно `ascentDeltaV` (честная стоимость подъёма) — экран продолжает рисовать «Δv X (нужно Y)».

## S2C `spacereloaded:cannon_state` — `CannonStatePayload` (расширение)

| Поле | Тип | Смысл |
|---|---|---|
| `guided` | bool | есть спутниковое покрытие целевого измерения |
| `spreadBlocks` | double | радиус рассеивания текущего режима |
| `impactSpeedMs` | double | прогноз скорости удара по атмосфере цели (0 без цели) |
| `impactEnergyMJ` | double | прогноз энергии удара, МДж |

## Synched-данные `RocketEntity` (S2C через `SynchedEntityData`)

`DATA_STAGE:int`, `DATA_STAGE_COUNT:int`, `DATA_STAGE_FUEL:float`, `DATA_DELTA_V:float` (обновляется раз в 10 тиков), `DATA_CMD_PITCH:float`, `DATA_CMD_ROLL:float` (град), `DATA_HAS_GYRO:bool`, `DATA_HEATING:bool`. Существующие `DATA_FUEL` (сумма по ступеням), `DATA_PITCH/ROLL`, `DATA_LAUNCHED`, `DATA_DESTINATION` — без изменений.

## Локализация (ключи, все три языка)

`block.spacereloaded.stage_separator`, `advancements.spacereloaded.staging.{title,description}`, `key.spacereloaded.stage`,
`hud.spacereloaded.rocket.stage` («Ступень %s/%s · топливо %s кг · Δv %s м/с»), `hud.spacereloaded.rocket.attitude` («Наклон: %s° (команда %s°)»), `hud.spacereloaded.rocket.no_gyro`, `hud.spacereloaded.rocket.heating`, `hud.spacereloaded.rocket.hint_flight` (обновить: «Прыжок — тяга · WASD — наклон · X — отделить ступень»),
`message.spacereloaded.stage.{separated,not_launched,last,not_pilot}`, `message.spacereloaded.assembly.{stage_above_command,stage_empty_below,stage_disconnected}`,
`message.spacereloaded.rocket.warning.STAGE_NO_ENGINE`, `screen.spacereloaded.scan.{stages,stage_line,ascent_ok,ascent_fail,max_q,max_q_warn}`,
`screen.spacereloaded.cannon.{guidance_sat,guidance_none,impact}`, `death.attack.spacereloaded.reentry_heat`, `message.spacereloaded.cannon.fired_mode` (режим в сообщении о выстреле).
