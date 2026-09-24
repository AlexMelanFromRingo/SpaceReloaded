# Contract: конфиг (`SpaceReloadedConfig`), фича 004

| Поле | По умолчанию | Валидация | Смысл |
|---|---|---|---|
| `coilTier1AccelG` | 1000 | 1…100 000 | предельное ускорение стальной катушки, g |
| `coilTier2AccelG` | 3000 | ≥ tier1 | сверхпроводящей катушки, g |
| `massDriverSectionLengthM` | 1.0 | 0.25…4 | длина секции (1 блок = 1 м) |
| `massDriverMaxSections` | 400 | 8…1024 | предел длины рельса |
| `massDriverEfficiency` | 0.85 | (0, 1] | КПД линейного двигателя η |
| `massDriverJoulesPerEnergy` | 15 000 | > 0 | масштаб Дж → E (как у пушки) |
| `massDriverClearance` | 8 | 1…64 | свободные клетки за срезом |
| `massDriverSledReturnTicks` | 200 | ≥ 20 | перезарядка = возврат салазок |
| `massDriverWaveTicks` | 10 | 2…100 | визуальная длительность волны |
| `capacitorCapacity` | 250 000 | ≥ 1000 | ёмкость конденсатора, E |
| `capacitorMaxInsert` | 2 000 | ≥ 1 | приём конденсатора, E/т |
| `capacitorMaxBlocks` | 32 | 1…256 | предел батареи |
| `podDryMassKg` | 100 | > 0 | масса пустой капсулы |
| `podKgPerItem` | 2.0 | ≥ 0 | игровые кг на предмет груза |
| `podMaxDynamicPressurePa` | 1 000 000 | > 0 | предел напора капсулы |
| `podMaxHeatFluxWm2` | 5 000 000 | > 0 | предел теплового потока капсулы |
| `podTransitTicks` | 600 | ≥ 60 | игровое время перелёта |
| `podSigmaCovered` / `podSigmaUncovered` | 0.5 / 10 | > 0 | рассеивание прибытия, блоки |
| `catcherBaseRadius` / `catcherRadiusPerSqrtNet` / `catcherMaxRadius` | 1.5 / 0.6 / 12 | ≥ 0 | радиус захвата |
| `catcherNetMaxBlocks` | 441 | 1…4096 | предел сетки |
| `massCatcherAnyDimension` | false | — | только для стенда: ловушка вне орбиты |
| `reactorCycleTicks` | 200 | ≥ 1 | цикл MRE |
| `reactorEnergyPerCycle` | 1 600 | ≥ 0 | энергия цикла |
| `reactorOxygenPerBlock` | 150 | ≥ 0 | O₂ на блок |
| `reactorTitaniumChance` | 0.2 | 0…1 | шанс Ti-пыли |
| `reactorOxygenBuffer` | 1 500 | ≥ 0 | буфер O₂ |
| `shelterMinRockBlocks` | 4 | 1…64 | толща над головой для «укрытия» |
