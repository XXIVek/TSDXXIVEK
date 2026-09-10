# TSDXXIVEK — Карта проекта

> **Полная документация:** [README.md](README.md)

---

## 1. Архитектурное разделение

Приложение разделено на две части:

| Часть | Описание | Пакеты |
|-------|----------|--------|
| **Основная** (общая для всех режимов) | БД, сканер, логирование, обмен JSON, управление состояниями | `dataDB/`, `scaner/`, `utilAPP/`, `FileExchangeManager.kt` |
| **Режимная** (зависит от режима обмена) | HTTP API, polling, UDP broadcast, socket-сервер | `api/`, `serverHTTP/` |

---

## 2. Структура пакетов

### Основная часть

| Пакет | Файлов | Описание |
|-------|--------|----------|
| **root** (`com/xxivek/tsdxxivek/`) | 10 | MainActivity, AppState, AppConstants, QrPairingParser, FileExchangeManager, MenuFragment, LogoFragment, SettingsFragment, PairingFragment, StartFragment |
| **dataDB** | 4 | Room-база данных (ItemRoomDatabase, ItemDao, Item), утилиты (UtilDB) |
| **scaner** | 2 | Сканер штрихкодов (ScanerFragment, ScanerFragment_d), CameraX |
| **utilAPP** | 2 | LicenseUtil (ViewModel), LogUtil |

### Режимная часть

| Пакет | Файлов | Описание |
|-------|--------|----------|
| **api** | 5 | HTTP-клиенты (ApiClient, FileDownloadApi), polling (StatusPollingService), парсинг JSON (ExchangeDataParser) — **режим Socket/Сайт** |
| **serverHTTP** | 3 | LocalWifiServer (HTTP-сервер ТСД), BroadcastServer (UDP), ServerSocketXXI (Socket) — **режимы WIFI и Socket** |

---

## 3. Ключевые классы

### Основная часть (общая)

| Класс | Пакет | Режимы | Роль |
|-------|-------|--------|------|
| `MainActivity` | root | Все | Точка входа, checkDeviceStatus(), resetPairingState(), saveState() |
| `MenuFragment` | root | Все | Главное меню, UI-состояния, вызов общей и режимной логики |
| `FileExchangeManager` | root | USB, WIFI | Чтение/запись JSON, polling папки, импорт/экспорт |
| `Item` | dataDB | Все | Entity товара |
| `ItemDao` | dataDB | Все | DAO интерфейс (CRUD) |
| `ItemRoomDatabase` | dataDB | Все | Singleton Room-базы |
| `LicenseUtil` | utilAPP | Все | ViewModel состояний (LiveData: bd, input, output) |
| `AppState` | root | Все | Глобальное состояние |
| `LogUtil` | utilAPP | Все | Логирование в файл |
| `ScanerFragment` | scaner | Все | Сканер QR и штрихкодов (CameraX + BarcodeScanning) |
| `QrPairingParser` | root | Все | Парсинг QR-кода |

### Режимная часть — Socket / Сайт (appConnect1C = 2)

| Класс | Пакет | Роль |
|-------|-------|------|
| `ApiClient` | api | Базовый HTTP-клиент (activate, getDeviceStatus, sendPairingStatus) |
| `FileDownloadApi` | api | Расширенный клиент (downloadFiles, exportAndUpload, updateDeviceStatusSync) |
| `StatusPollingService` | api | Polling статуса каждые 5 сек |
| `ExchangeDataParser` | api | Парсинг JSON-файлов с сайта |

### Режимная часть — Локальный WIFI (appConnect1C = 4)

| Класс | Пакет | Роль |
|-------|-------|------|
| `LocalWifiServer` | serverHTTP | HTTP-сервер на ТСД (JSON API) |
| `BroadcastServer` | serverHTTP | UDP broadcast объявления ТСД |

### Режимная часть — Socket (appConnect1C = 2, локальный)

| Класс | Пакет | Роль |
|-------|-------|------|
| `ServerSocketXXI` | serverHTTP | Socket-сервер для локального режима |

---

## 4. Режимы работы

| Режим | appConnect1C | Механизм | Режимные классы |
|-------|:---:|----------|-----------------|
| Не сопряжено | 0 | Ожидание QR-кода | — |
| Socket / Сайт | 2 | HTTP API через промежуточный сайт | ApiClient, FileDownloadApi, StatusPollingService |
| Локальный USB | 3 | Файловый обмен через папку | FileExchangeManager |
| Локальный WIFI | 4 | HTTP API напрямую к ТСД по IP | LocalWifiServer, BroadcastServer |

---

## 5. Документация

| Файл | Содержание |
|------|------------|
| [README.md](README.md) | Оглавление, текущий режим, ссылки |
| [ARCH_MODES.md](ARCH_MODES.md) | **Основное:** разделение на общую и режимную части, режимы работы, сопряжение, обмен |
| [ARCH_API.md](ARCH_API.md) | HTTP API, JSON форматы, SharedPreferences |
| [ARCH_NAVIGATION.md](ARCH_NAVIGATION.md) | Навигация (граф фрагментов), жизненный цикл |
| [ARCH_UI.md](ARCH_UI.md) | UI-состояния, цвета, тексты |
| [HISTORY.md](HISTORY.md) | История изменений |
| [TSD_SCENARIO.md](TSD_SCENARIO.md) | Фиксация изменений по сценарию 1С-Сайт-1С/ТСД |
| [Tests/TEST_PLAN_LOCAL_WIFI.md](Tests/TEST_PLAN_LOCAL_WIFI.md) | План тестирования Local WIFI |

---

*Последнее обновление: 2026-09-09*
