# Tasks: Полировка и релиз (T1000+)

Пути: `M/` = `mod/src/main/java/org/alex_melan/spacereloaded/`, `C/` = `mod/src/client/java/org/alex_melan/spacereloaded/client/`.

## Phase 1: US1 Экраны вместо чата (P1) 🎯 MVP
- [x] T1000 [US1] `M/multiblock/BlockStatusProvider.java`; `M/network/ModNetworking.java` — обновление по блоку
- [x] T1001 [US1] ЦУП (`M/rocket/MissionControlBlock.java`): борта с Δv, линии, спутники, кнопка карты
- [x] T1002 [US1] Грузовой терминал и экран телеметрии — `StatusProvider`
- [x] T1003 [US1] Кинетика (`M/kinetics/KineticBlockEntity.java`, подклассы `extraReport(List)`), ступица кольца, химмашины
- [x] T1004 [US1] `C/gui/MachineStatusScreen.java`: перенос строк, перестройка кнопок; `tools/lang_010.py`

## Phase 2: US2 Анимации (P2)
- [x] T1005 [US2] Пушка: синхронизация выстрела и заряда, модели base/barrel, `C/render/OrbitalCannonRenderer.java`
- [x] T1006 [US2] Ракета: `DATA_THRUST`, факел в `C/render/RocketRenderer.java` (цвет, раскрытие, ромбы Маха)
- [x] T1007 [US2] Стенд `testAnimations010` и кадры README (пушка, факел у земли и на высоте)

## Phase 3: US3 Релиз (P3)
- [x] T1008 [US3] `mod_version=1.0.0`, описание en, `modmenu.descriptionTranslation` ru/uk, `CHANGELOG.md`
- [x] T1009 [US3] `docs/MODRINTH.md`, `.github/workflows/release.yml` (черновик по тегу v*), README «Скачать»
- [x] T1010 [US3] GUIDE (экраны и анимации), ADDONS (шкалы энергии), ROADMAP

## Phase 4: Polish
- [x] T1011 Полный прогон (build + стенд), коммит, слияние, push, тег v1.0.0
- [x] T1012 Аудит незакрытых пунктов во всех документах (поручение автора)
