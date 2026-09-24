# Data Model: Инженерия

| Сущность | Где | Поля |
|---|---|---|
| KineticGraph (ядро) | `core.kinetics` | nodes[I, frictionTorque, shaftLimit, clutchLimit], edges[a, b, f, η]; результат analyze: r[], e[], parent[], order[], state (OK/JAMMED/TOO_LARGE/EMPTY) |
| Узловая динамика | ядро | a, b (линейная часть источника), c (Кулон), I |
| KineticSolver.Step | ядро | ω', α, generatedPower[], transmitted[] (момент через узел) |
| KineticBlockEntity | мод | omegaLocal (рад/с), angle0 (рад), tick0; сеть-id (runtime) |
| KineticNetwork (runtime) | мод | узлы (pos), граф, ω, sleep-счётчик, state |
| Motor/Flywheel/Press/Lathe/WindHub BE | мод | энергобуфер (мотор), вход/выход, прогресс, накопленное отклонение скорости |
| MultiblockTemplate | реестр `spacereloaded:multiblock` | key, cells[offset, block|tag], repeat{cells, step, min, max, display} |
| Formed state | Electrolyzer/Refinery BE | formedCount (N ячеек / K тарелок) |
| Полуфабрикат | компоненты предмета | `machining_step` (int), `machining_delta_sq` (float, мкм²) |
| Деталь | компонент | `part_quality` (float 0…1) |
| Двигатель | состояние блока | `quality` 0…10 (по умолчанию 8) |
