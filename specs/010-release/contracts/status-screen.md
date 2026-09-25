# Contract: экран состояния (010)

Блок-сущность реализует `StatusProvider.status(ServerLevel)` или блок — `BlockStatusProvider.status(ServerLevel,
BlockPos, ServerPlayer)`; сервер отправляет `MachineStatusPayload`, клиент раз в секунду шлёт
`MachineActionPayload(pos, "refresh")`, кнопки — `MachineActionPayload(pos, id, value)` (радиус 8 блоков).
