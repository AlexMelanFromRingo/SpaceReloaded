# Quickstart: проверка 010

```bash
export JAVA_HOME=~/.sdkman/candidates/java/25.0.3-tem
./gradlew build                                   # jar spacereloaded-1.0.0
SR_ONLY=testAnimations010 DISPLAY=:0 ./gradlew :mod:runClientGametest && python3 tools/readme_shots.py
DISPLAY=:0 ./gradlew :mod:runClientGametest       # полный стенд
```
