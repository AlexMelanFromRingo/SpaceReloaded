# Quickstart: проверка фичи 003

## Предпосылки
- `JAVA_HOME=~/.sdkman/candidates/java/25.0.3-tem`; стенд — `DISPLAY=:0`, запуск в foreground с `timeout 570`.

## Тесты ядра
```sh
JAVA_HOME=~/.sdkman/candidates/java/25.0.3-tem ./gradlew :core:test
```
Ожидание: зелёные `TransferOrbitsTest`, `TransferBurnTest`, `LandingBudgetTest`, `MissionPlannerTest`, `WetWorkshopPlannerTest`, расширенные `AscentSimulatorTest`/`FlightPerformanceBudgetTest`.

## Сборка
```sh
JAVA_HOME=~/.sdkman/candidates/java/25.0.3-tem ./gradlew build
```

## E2E-стенд
```sh
JAVA_HOME=~/.sdkman/candidates/java/25.0.3-tem timeout 570 ./gradlew :mod:runClientGametest
```
Ожидание в логе `[SpaceReloaded Test]`: «грузовая линия: отказ … м/с → заправка → автозапуск ✓», «wet workshop: 27 воздух / 98 обшивка / люк ✓ · герметично ✓», «марсианский лёд найден ✓».

## Ручная проверка в игре
1. Площадка + маяк + терминал (ПКМ программой на орбитальный маяк, Sneak+ПКМ → AUTO), погрузчик с сундуком, колонка с баком; собрать беспилотный борт (двигатель, 3 бака, отсек, командный модуль).
2. Терминал: «отказ: нехватка Δv» пока бак пуст → колонка заправляет → через 10 с старт.
3. На орбите: маяк + терминал с обратной программой → борт возвращается.
4. Wet workshop: стек 5×5×5 баков на двигателях, вывести на орбиту, посадить у порта, стравить топливо колонкой (DRAIN), Sneak+ПКМ по порту → модуль; контроллер внутри → «герметично».
5. Марс: найти лёд киркой на глубине, Сабатье → метанокс, колонка → борт, взлёт → «Обратный билет».
