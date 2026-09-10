# TSDXXIVEK — Режимы работы, Сопряжение, Обмен данными

## 1. Архитектура приложения

Приложение состоит из двух частей:

### 1.1 Основная часть (общая для всех режимов)

Функции, которые работают одинаково независимо от режима обмена:

| Функция | Файлы | Описание |
|---------|-------|----------|
| Управление БД | `dataDB/Item.kt`, `ItemDao.kt`, `ItemRoomDatabase.kt` | Room-база данных товаров |
| Импорт/экспорт JSON | `FileExchangeManager.kt` | Чтение `tsd_Input.json`, запись `tsd_Output.json`, парсинг данных |
| Управление состояниями | `utilAPP/LicenseUtil.kt`, `AppState.kt` | LiveData: bd, input, output, countBD |
| Сканер штрихкодов | `scaner/ScanerFragment.kt` | CameraX + ML Kit BarcodeScanning |
| Логирование | `utilAPP/LogUtil.kt` | Запись в `sdcard/Download/logTSD.dat` |
| Сервисные команды | `MenuFragment.kt` | Очистка quantity, навигация, UI-управление |

### 1.2 Функции обмена под 1С (режимно-зависимые)

Реализуются в зависимости от выбранного режима (`appConnect1C`):

| Режим | appConnect1C | Механизм | Файлы |
|-------|:---:|----------|-------|
| Socket / Сайт | 2 | HTTP API к серверу через промежуточный сайт | `api/ApiClient.kt`, `api/FileDownloadApi.kt`, `api/StatusPollingService.kt` |
| Локальный USB | 3 | Файловый обмен через `/storage/emulated/0/Download/TSD/` | `FileExchangeManager.kt` |
| Локальный WIFI | 4 | HTTP API напрямую к ТСД по IP | `serverHTTP/LocalWifiServer.kt`, `serverHTTP/BroadcastServer.kt` |

---

## 2. Режимы работы

### 2.1 Socket / Сайт (appConnect1C = 2)

**Концепция:**
- Обмен через HTTP API к промежуточному сайту в интернете (`http://192.168.160.99`)
- Состояния ТСД (pairing, konf, bd, input, output) сохраняются на сайте
- Обработка под 1С на ПК взаимодействует со сайтом, сайт — с ТСД

**Механизм:**
1. Активация: `POST /api/v1/devices/activate` с `activation_code` → получение `device_uuid`
2. Polling статуса: `GET /api/v1/devices/status` каждые 5 сек через `StatusPollingService`
3. Выгрузка: `FileDownloadApi.exportAndUpload()` → JSON на сервер
4. Загрузка: `FileDownloadApi.downloadFiles()` → файлы в локальную директорию → импорт в БД через `ExchangeDataParser`
5. Обновление статуса: `POST /api/v1/devices/status` с JSON тела

**Ключевые классы:**
| Класс | Роль |
|-------|------|
| `ApiClient` | Базовый HTTP-клиент (activate, getDeviceStatus, sendPairingStatus) |
| `FileDownloadApi` | Расширенный клиент (downloadFiles, exportAndUpload, updateDeviceStatusSync) |
| `StatusPollingService` | Периодический опрос статуса (каждые 5 сек) |
| `ExchangeDataParser` | Парсинг JSON-файлов, полученных с сайта |

---

### 2.2 Локальный USB (appConnect1C = 3)

**Концепция:**
- Файловый обмен через общую папку `/storage/emulated/0/Download/TSD/`
- Состояния ТСД передаются через файл `tsd_dev_status.txt` (SSV формат)
- Обработка под 1С на ПК использует ADB сервер для чтения/записи файлов

**Файлы обмена:**
| Файл | Направление | Описание |
|------|-------------|----------|
| `tsd_Input.json` | ПК → ТСД | Данные для загрузки в БД |
| `tsd_Output.json` | ТСД → ПК | Данные для выгрузки из БД |
| `tsd_dev_status.txt` | Двусторонний | Состояние устройства (SSV) |

**SSV формат `tsd_dev_status.txt`:** `pairing;konf;bd;input;output`
Пример: `true;1;3;0;0`

**Механизм:**
1. `FileExchangeManager` опрашивает папку каждые 5 секунд (`startPolling()`)
2. При появлении `tsd_Input.json` → `onInputFileReady` → импорт в БД
3. При запросе выгрузки → `writeOutputAndExport()` → запись `tsd_Output.json`
4. Статус обновляется через `writeStatus()` / читается через `readStatus()`

**Ключевые классы:**
| Класс | Роль |
|-------|------|
| `FileExchangeManager` | Управление файлами обмена, polling папки, импорт/экспорт |
| `DeviceStatus` | Модель состояния (pairing, konf, bd, input, output) |

---

### 2.3 Локальный WIFI (appConnect1C = 4)

**Концепция:**
- HTTP API напрямую к ТСД по IP-адресу в локальной сети
- Обработка под 1С на ПК обменивается с ТСД по API, запрашивает состояния напрямую
- ТСД сам является HTTP-сервером

**Механизм:**
1. ТСД запускает HTTP-сервер на порту из QR-кода (`LocalWifiServer.start(port)`)
2. ТСД запускает UDP broadcast (`BroadcastServer`) каждые 2 сек в течение 20 сек
3. ПК обнаруживает ТСД по UDP broadcast (получает IP, порт, lic)
4. ПК отправляет HTTP-запросы напрямую к IP ТСД
5. При `pairing=true` — ПК запоминает IP для дальнейших запросов

**BroadcastServer:**
- UDP broadcast на `255.255.255.255:1900`
- Формат: `{"ip":"%s","port":%d,"lic":"%s"}`
- Работает 20 секунд после сопряжения

**Ключевые классы:**
| Класс | Роль |
|-------|------|
| `LocalWifiServer` | HTTP-сервер на ТСД (JSON API) |
| `BroadcastServer` | UDP broadcast объявления ТСД |
| `FileExchangeManager` | Файловый обмен (status file, input/output JSON) |

**Отличие от USB:**
- Статус передаётся через HTTP-эндпоинт (`skipStatusFile=true`), файл не пишется
- Данные обмена могут передаваться через файлы (как USB) или через HTTP API

---

## 3. Сценарий сопряжения

### 3.1 Общая логика (единая для всех режимов)

1. ПК формирует QR-код с параметрами сопряжения
2. ТСД сканирует QR через `ScanerFragment`
3. `QrPairingParser` парсит содержимое QR-кода
4. Сохранение настроек в SharedPreferences
5. Переход на соответствующий экран (MenuFragment или PairingFragment)

### 3.2 Формат QR-кода

```
PAIR:true;socket:true;konf:1;LOGGING:0;Port:8180;Lic:1
```

| Параметр | Описание | Значения |
|----------|----------|----------|
| `PAIR` | Сопряжение | `true` / `false` |
| `socket` | Socket-режим (HTTP-сервер) | `true` / `false` |
| `konf` | Конфигурация | `1` — XXI век, `0` — любая |
| `LOGGING` | Логирование | `0` / `1` |
| `Port` | Порт HTTP-сервера | любой (настраивается в 1С) |
| `Lic` | Лицензия ТСД | уникальный номер |

### 3.3 Различия по режимам

| Режим | appConnect1C | Особенности | Классы |
|-------|:---:|------|--------|
| **Socket / Сайт** | 2 | Активация через HTTP API к серверу ПК, polling статуса | `ApiClient`, `FileDownloadApi`, `StatusPollingService` |
| **Локальный USB** | 3 | Файловый обмен через папку, polling каждые 5 сек | `FileExchangeManager`, `QrPairingParser` |
| **Локальный WIFI** | 4 | HTTP API к ТСД по IP, UDP broadcast | `LocalWifiServer`, `BroadcastServer`, `FileExchangeManager` |

---

## 4. Сценарий передачи данных

### 4.1 Общая логика

- Основная часть: чтение JSON, импорт в БД, экспорт из БД, управление состояниями
- Режимная часть: механизм доставки данных (HTTP API / файлы / UDP)

### 4.2 Различия по режимам

| Режим | Механизм input | Механизм output | Polling статуса |
|-------|---------------|-----------------|-----------------|
| **Socket / Сайт** | `GET /api/v1/exchange/input` → скачивание файлов | `POST /api/v1/exchange/output` → JSON на сервер | `StatusPollingService` каждые 5 сек |
| **Локальный USB** | Чтение `tsd_Input.json` из папки | Запись `tsd_Output.json` в папку | Чтение `tsd_dev_status.txt` каждые 5 сек |
| **Локальный WIFI** | HTTP POST к ТСД → запись в файл | HTTP GET с ТСД → чтение файла | HTTP GET `/api/v1/devices/status` |

### 4.3 Файловая структура обмена (режимы USB и WIFI)

Данные обмена хранятся в папке `/storage/emulated/0/Download/TSD/`:
- `tsd_Input.json` — данные для загрузки в ТСД
- `tsd_Output.json` — данные для выгрузки из ТСД
- `tsd_dev_status.txt` — статус устройства (SSV формат)

**Формат `tsd_dev_status.txt`:** `false;0;0;0;0;` (параметры разделены `;`)

| Позиция | Параметр | Описание | Значения |
|---------|----------|----------|----------|
| 1 | Сопряжение | Состояние сопряжения | `false` — нет, `true` — есть |
| 2 | Конфигурация | Номер конфигурации | `0` — любая, `1` — XXI век и т.д. |
| 3 | БД | Состояние БД | `0` — пуста, `1` — ошибка, `2` — заполнена с выбранными, `3` — заполнена без выбранных |
| 4 | input | Входящие данные | `0` — отсутствуют, `1` — ошибка, `3` — есть данные |
| 5 | output | Исходящие данные | `0` — отсутствуют, `1` — ошибка, `3` — есть данные |

### 4.4 Режим "Локальный WIFI" — восстановление состояний

При перезагрузке приложения (`checkLocalWifiMode`):
1. **Проверка bd:** Считывается `tsd_dev_status.txt` → `bd=0` (пуста), `bd=2` (с выбранными), `bd=3` (без выбранных)
2. **Проверка input:** Если `tsd_Input.json` существует → `input=3`, иначе — значение из `tsd_dev_status.txt`
3. **Проверка output:** Если `tsd_Output.json` существует → `output=3`, иначе — значение из `tsd_dev_status.txt`
4. Если `pairing=false` или файл отсутствует — полный сброс через `resetPairingState()`

### 4.5 Режим "Socket / Сайт" — процедура обмена

**Передача ПК → ТСД (input):**
1. Обработка 1С отправляет данные на сайт
2. Сайт размещает файлы для скачивания
3. `StatusPollingService` обнаруживает `input=3`
4. `FileDownloadApi.downloadFiles()` скачивает файлы
5. `onFilesDownloaded()` → `loadDownloadedFilesToDB()` → импорт в БД
6. `updateDeviceStatusSync()` → `input=0`, `bd=3` на сервере

**Передача ТСД → ПК (output):**
1. Оператор на ТСД нажимает "Выгрузить"
2. `exportToWebsite()` → JSON на сервер через `FileDownloadApi.exportAndUpload()`
3. `updateDeviceStatusSync()` → `output=3` на сервере
4. Обработка 1С получает статус через сайт
5. Обработка 1С забирает данные через `GET /api/v1/exchange/output`

---

## 5. Ключевые классы

### Основная часть (общая)

| Класс | Файл | Роль |
|-------|------|------|
| `Item` | `dataDB/Item.kt` | Entity товара (штрихкод, название, цена, количество) |
| `ItemDao` | `dataDB/ItemDao.kt` | DAO интерфейс (CRUD операции) |
| `ItemRoomDatabase` | `dataDB/ItemRoomDatabase.kt` | Singleton Room-базы данных |
| `FileExchangeManager` | `FileExchangeManager.kt` | Чтение/запись JSON, polling папки, импорт/экспорт |
| `LicenseUtil` | `utilAPP/LicenseUtil.kt` | ViewModel лицензии и состояний (LiveData) |
| `AppState` | `AppState.kt` | Глобальное состояние приложения |
| `LogUtil` | `utilAPP/LogUtil.kt` | Логирование в файл |
| `ScanerFragment` | `scaner/ScanerFragment.kt` | Сканирование QR и штрихкодов |
| `MenuFragment` | `MenuFragment.kt` | Главное меню, UI-управление |

### Режимная часть — Socket / Сайт

| Класс | Файл | Роль |
|-------|------|------|
| `ApiClient` | `api/ApiClient.kt` | Базовый HTTP-клиент (activate, status) |
| `FileDownloadApi` | `api/FileDownloadApi.kt` | Расширенный клиент (download, export, update) |
| `StatusPollingService` | `api/StatusPollingService.kt` | Polling статуса каждые 5 сек |
| `ExchangeDataParser` | `api/ExchangeDataParser.kt` | Парсинг JSON-файлов с сайта |

### Режимная часть — Локальный USB

| Класс | Файл | Роль |
|-------|------|------|
| `FileExchangeManager` | `FileExchangeManager.kt` | Файловый обмен, polling папки |
| `DeviceStatus` | `FileExchangeManager.kt` | Модель состояния (SSV) |

### Режимная часть — Локальный WIFI

| Класс | Файл | Роль |
|-------|------|------|
| `LocalWifiServer` | `serverHTTP/LocalWifiServer.kt` | HTTP-сервер на ТСД |
| `BroadcastServer` | `serverHTTP/BroadcastServer.kt` | UDP broadcast объявления |

---

*Последнее обновление: 2026-09-09*
