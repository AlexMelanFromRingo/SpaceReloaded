# Quickstart: проверка 008

```bash
export JAVA_HOME=~/.sdkman/candidates/java/25.0.3-tem
./gradlew :core:test                       # 7 классов ядра против research-design
SR_ONLY=testEclssRack,testReactor,testCascade,testAirColumn,testArcFurnace,testDsnAntenna \
  DISPLAY=:0 ./gradlew :mod:runClientGametest
SR_ONLY=testReadmeShots DISPLAY=:0 ./gradlew :mod:runClientGametest && python3 tools/readme_shots.py
python3 tools/check_zfight.py && python3 tools/gen_site.py
```

Ожидания — SC-001…SC-009 спецификации: стойка 0.47 кг воды/сут/чел; реактор ~1 кВт(э) и период по
формуле, > 1 $ — расплав; каскад 29 → 93 %; колонна 11 тарелок → 99.5 %; плавка 1 т при 1 МВт ≈ 28 мин;
Марс 34 м ≈ 4.9 Мбит/с.
