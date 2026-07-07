# Quickstart: сборка и запуск SpaceReloaded

## Требования

- **JDK 25** (Temurin). Через sdkman: `sdk install java 25.0.3-tem` (в WSL уже установлен: `~/.sdkman/candidates/java/25.0.3-tem`)
- Git; интернет для первого резолва зависимостей
- IntelliJ IDEA **2025.3+** (поддержка Java 25), опционально

## Сборка

```bash
cd ~/SpaceReloaded
JAVA_HOME=~/.sdkman/candidates/java/25.0.3-tem ./gradlew build
```

- `./gradlew :core:test` — юнит-тесты ядра (без Minecraft, быстрые)
- `./gradlew :mod:build` — сборка jar мода (в `mod/build/libs/`)

## Запуск дев-клиента/сервера

```bash
JAVA_HOME=~/.sdkman/candidates/java/25.0.3-tem ./gradlew :mod:runClient
JAVA_HOME=~/.sdkman/candidates/java/25.0.3-tem ./gradlew :mod:runServer
```

## Ручная проверка среза P1 (герметичность)

1. Создать мир, выполнить `/spacereloaded debug vacuum on` (режим вакуумной камеры для теста).
2. Построить куб 5×5×5 из герметичных блоков, внутрь — контроллер атмосферы, подать энергию (креативный источник `debug_power`).
3. Убедиться: статус SEALED, атмосфера растёт.
4. Убрать угловой блок, оставив диагональное касание — статус LEAK (26 направлений!).
5. `/spacereloaded debug zone here` — диагностика: объём, статус, точки утечки.

## Полезное

- Версии тулчейна — в `gradle.properties` (см. `specs/001-space-mod-core/research.md`, R1).
- Исходники Minecraft 26.2 деобфусцированы: `./gradlew :mod:genSources` больше не нужен в старом смысле — Loom подключает исходники напрямую; читать имена API по jar, не по памяти.
- Конфиг мода: `run/config/spacereloaded.json` (лимиты радиуса зон, блоков ракеты, параллельных пересчётов).
