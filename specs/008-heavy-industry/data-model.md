# Data Model: 008 Тяжёлая индустрия

## EclssController (BE)
| Поле | Тип | Смысл |
|---|---|---|
| modules | bitset {OGS, SABATIER, CDRA, WRS} | модули в гнёздах после сборки |
| waterKg | double | запас воды (лёд/вёдра) |
| h2Kg | double | водород между электролизом и Сабатье |
| ch4Kg | double | метан до отдачи в бак |
| dayBalance | record | O₂, CO₂, H₂O, CH₄ за игровые сутки (экран) |
| anim: fanPhase0, bubbleOn, pumpPhase0 | sync | анимация |

## ReactorController (BE, ключ — привод стержня)
| Поле | Тип | Смысл |
|---|---|---|
| rodTarget, rodPos | double 0…1 | цель и позиция стержня (сервер ведёт позицию скоростью привода) |
| precursors C | double | одна группа запаздывающих |
| power | double, Вт(т) | мощность деления + остаточная |
| temperature | double, K | температура зоны |
| burnup | double, МВт·сут | выгорание |
| damage | 0/1/2 | цело / повреждено / расплав |
| stirlings, radiatorSegments | int | из сборки |
| scramTick | long | для остаточного тепла и анимации падения |

## FuelBasket (предмет)
компоненты: `u235_kg`, `enrichment`, `burnup_mwd`.

## UraniumHexafluoride (предмет) — компонент `enrichment` (доля).

## CascadeController (BE): centrifuges N, feedKg, productKg, tailsKg, spinUp0 (анимация).

## AsuController (BE): trays N, airKgPerS (от вала), purityO2, purityN2, argon, frost 0…1 (анимация).

## EafController (BE)
| Поле | Тип | Смысл |
|---|---|---|
| phase | OPEN/CHARGED/MELT/REFINE/TAP | фаза плавки |
| chargeKg, meltedJ | double | шихта и введённая энергия |
| electrodeKg[3] | double | остаток электродов |
| roofAngle, electrodeY, tilt | цели анимации | sync |

## DsnController (BE): radius r (D = 2r), target body, rateBps, az/el цели, sessionActive.

## SpaceNetworkState (+) `ground_links`: измерение цели → лучшая скорость линии, бит/с.
