# Quickstart: проверка 009

```bash
export JAVA_HOME=~/.sdkman/candidates/java/25.0.3-tem
./gradlew :core:test                                  # Ephemeris, Lambert, перелёты, SpectralMapping, GroundRadar
SR_ONLY=testLambertTransfer,testDeltaVMap,testMineralMap,testGroundRadar DISPLAY=:0 ./gradlew :mod:runClientGametest
python3 tools/check_zfight.py && python3 tools/gen_site.py
```

Ожидания — SC-001…SC-007 спецификации: минимум Земля → Марс 3.55…3.95 км/с (InSight 3.59); между
окнами ≥ 1.5 × минимума; цена карты = цена перехода; карта минералов отмечает выход сланца и не
видит его под грунтом; отражение от пустоты на 8 м под реголитом — 92 нс.
