# Contract: датапак 005

- Реестр `data/<ns>/spacereloaded/multiblock/<id>.json` (синхронизируется): `key` (id блока), `cells` [{`offset`:[x,y,z], `block`: "id" | "#tag"}], `repeat` {`cells`, `step`, `min`, `max`, `display`}; локальная система: +z — от лицевой грани ключа внутрь, +y — вверх; поворот по FACING ключа.
- Рецепт `spacereloaded:pressing` / `spacereloaded:machining`: `ingredient` (id), `step` (необязательно: шаг полуфабриката на входе), `result` {id, count}, `delta_um` (погрешность станка на операцию, по умолчанию из конфига), `energy_j` (токарный), `balance` (bool: делит Σδ² на 4).
- Результат-деталь получает `part_quality`; результат-полуфабрикат — `machining_step = step + 1` и накопленную `machining_delta_sq`.
- Двигатели мода (`rocket_engine`, `hydrolox_engine`, `methalox_engine`) — свойство состояния `quality` 0…10; аддон-двигатель может объявить то же свойство, чтобы получить модель качества.
- Теги: `spacereloaded:kinetic` (все вращающиеся блоки — для кабелей/подсказок не нужен), `minecraft:mineable/pickaxe`/`axe` для новых блоков.
