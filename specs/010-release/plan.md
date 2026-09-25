# Implementation Plan: Полировка и релиз

**Branch**: `010-release` | **Date**: 2026-09-25 | **Spec**: [spec.md](spec.md)

## Summary
Экраны состояния вместо чата на общем механизме 008 (плюс `BlockStatusProvider` для блоков без
сущности), анимации пушки (BER с отдельной моделью ствола) и факела ракеты (аддитивные конусы в
`RocketRenderer`), релизные метаданные и workflow.

## Technical Context
Java 25, Fabric Loom 26.2; клиент — BlockEntityRenderer, EntityRenderer, Screen. Синхронизация —
update-пакет блок-сущности пушки (момент выстрела, энергия), `SynchedEntityData` ракеты (флаг тяги).
Стенд — `testAnimations010` и полный прогон.

## Constitution Check
| Принцип | Соответствие |
|---|---|
| I | Откат с двумя постоянными времени, раскрытие факела по давлению среды профиля планеты, цвет по химии пламени |
| III | Экран опрашивает раз в секунду; синхронизация пушки — по событию и раз в 2 с при изменении заряда > 2 % |
| VI | Сервер авторитетен; клиент только рисует |
Отступления: нет.

## Ключевые решения
- **D100 Экран блока.** `BlockStatusProvider` (ЦУП, ступица, химмашины); `ModNetworking` обновляет
  по блок-сущности или по блоку. `KineticBlockEntity` — `StatusProvider` (строки подклассов через
  `extraReport(List)`), шкала скорости до предела маховика.
- **D101 Перенос.** `MachineStatusScreen` переносит строки `font.split`, перестраивает кнопки при
  смене высоты.
- **D102 Пушка.** Модель делится на `orbital_cannon_base` (блокстейт) и `orbital_cannon_barrel`
  (блок-держатель для рендера); предмет — полная модель.
- **D103 Факел.** Нижние двигатели структуры; вложенные конусы ядро/оболочка; tan(4° + 26°(1 − ρ/ρ₀)).
- **D104 Релиз.** `mod_version=1.0.0`, `CHANGELOG.md`, `docs/MODRINTH.md`, `.github/workflows/release.yml`
  (черновик), описание en + `modmenu.descriptionTranslation`.

## Project Structure
`multiblock/BlockStatusProvider`, правки `network/ModNetworking`, `rocket/MissionControlBlock`,
`logistics/CargoTerminal*`, `sealing/TelemetryScreen*`, `kinetics/*`, `station/SpinHubBlock`,
`machine/ChemMachineBlock`, `cannon/OrbitalCannonBlockEntity`, клиент `render/OrbitalCannonRenderer`,
`render/RocketRenderer`, `gui/MachineStatusScreen`; `tools/lang_010.py`.
