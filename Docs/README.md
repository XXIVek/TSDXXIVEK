# TSDXXIVEK — Сценарии

> **Полная документация в [PROJECT_MAP.md](PROJECT_MAP.md)**

## Текущий режим

**Локальный WIFI** (appConnect1C = 4) — обмен через HTTP API (JSON) к ТСД по обнаруженному IP.

## Документация

| Раздел | Файл |
|--------|------|
| Карта проекта | [PROJECT_MAP.md](PROJECT_MAP.md) |
| Навигация и жизненный цикл | [ARCH_NAVIGATION.md](ARCH_NAVIGATION.md) |
| Режимы работы, сопряжение, обмен | [ARCH_MODES.md](ARCH_MODES.md) |
| HTTP API, JSON, SharedPreferences | [ARCH_API.md](ARCH_API.md) |
| UI-состояния | [ARCH_UI.md](ARCH_UI.md) |
| История изменений | [HISTORY.md](HISTORY.md) |
| Результаты тестирования USB | [Tests/TEST_RESULTS_USB.md](Tests/TEST_RESULTS_USB.md) |
| План тестирования Local WIFI | [Tests/TEST_PLAN_LOCAL_WIFI.md](Tests/TEST_PLAN_LOCAL_WIFI.md) |

## История изменений

### 2026-07-21 — Исправления получения данных

- `MainActivity.kt` — восстановление input/output из файлов при перезагрузке
- `MenuFragment.kt` — проверка `hasInputFile()` для режима 4 (WIFI)

### 2026-07-16 — Исправление навигации

- `ScanerFragment.kt` — убран сброс состояния при открытии
- `LogoFragment.kt` — автоматический переход в MenuFragment
- `MainActivity.kt` — `resetPairingState()` удаляет файлы обмена

*Последнее обновление: 2026-07-21*
