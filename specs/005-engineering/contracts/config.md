# Contract: конфиг 005

| Поле | Умолч. | Смысл |
|---|---|---|
| kineticMaxBlocks | 512 | предел сети |
| gearMeshEfficiency / bevelEfficiency | 0.98 / 0.97 | КПД зацеплений |
| kineticFrictionTorqueNm | 2 | трение на блок |
| shaftDiameterM | 0.25 | диаметр вала |
| steelShaftShearPa / woodShaftShearPa | 230e6 / 8e6 | τ_y материалов |
| clutchSlipTorqueNm | 50 000 | предел муфты |
| motorPowerW / motorNoLoadRpm / motorEfficiency | 300 000 / 1500 / 0.92 | мотор-генератор |
| motorEnergyBuffer | 200 | буфер мотора, E |
| flywheelInertia / flywheelMaxOmega | 385 / 703 | маховик |
| flywheelBurstMaxRadius / flywheelBurstBreaksBlocks | 6 / true | разрыв |
| pressStrokeJ / pressStrokeTicks / pressCycleTicks | 50 000 / 4 / 40 | пресс |
| pressMinRpm / pressMaxRpm / pressNominalRpm | 60 / 600 / 300 | окно пресса (на валу пресса) |
| latheNominalPowerW / latheMinRpm / latheMaxRpm / latheNominalRpm | 30 000 / 300 / 1200 / 750 | токарный |
| toleranceLatheUm / tolerancePressUm / toleranceSpeedFactor / toleranceScrapUm | 10 / 20 / 2 / 100 | допуски |
| stackMaxCells / columnMinTrays / columnMaxTrays | 15 / 4 / 16 | мультиблоки |
| kineticSyncThreshold / kineticSyncMinTicks | 0.03 / 5 | синк клиенту |
| kineticVisualMaxOmega | 40 | визуальный предел рендера, рад/с |
| windSpeedEarth / windSpeedMars / windSpeedMarsStorm | 8 / 8 / 25 | ветер, м/с |
