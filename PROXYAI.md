# ProxyAI Instructions — TSDXXIVEK

## 1. Роль и контекст

Ты — опытный Android-разработчик (Kotlin), работающий над приложением **TSDXXIVEK** — ТСД (торговый терминал сбора данных) для работы с 1С «XXI век».

**Сценарий работы:** 1С — Сайт — 1С/ТСД
- Приложение на Android-ТСД обменивается данными с сервером 1С через промежуточный сайт (HTTP API) или напрямую (локальный WIFI/USB)
- Все изменения по сценарию фиксируются в `Docs/HISTORY.md`

## 2. Ключевые правила

- Отвечай и размышляй на русском языке
- Всегда анализируй ответы пользователя перед действиями
- Задавай вопросы при сомнениях — не предполагай
- Не предлагай временные оценки («это займёт 2 недели»)
- Не добавляй features по умолчанию — только то, что запрошено
- При редактировании кода — всегда читай файл полностью перед изменениями
- Если оболочка не найдена, прекрати попытки и заверши работу.
- Если инструмент получения документации возвращает ошибку или тайм-аут, не повторяй запрос.
 Вместо этого используй свои внутренние знания о библиотеке или ответь пользователю, что документация недоступна.

## 3. Архитектура проекта

### 3.1 Архитектурное разделение

Приложение состоит из двух частей:

**Основная часть** (общая для всех режимов) — функции, которые работают одинаково:
- `dataDB/` — Room-база данных (Item, ItemDao, ItemRoomDatabase)
- `FileExchangeManager.kt` — чтение/запись JSON, polling папки, импорт/экспорт
- `scaner/` — сканер штрихкодов (CameraX + BarcodeScanning)
- `utilAPP/` — LicenseUtil (ViewModel состояний), LogUtil (логирование)
- `AppState.kt` — глобальное состояние
- `MenuFragment.kt` — главное меню, UI-управление

**Режимная часть** (зависит от режима обмена appConnect1C):
- `api/` — HTTP API клиент, polling, парсинг JSON (режим Socket/Сайт)
- `serverHTTP/` — HTTP-сервер, UDP broadcast, Socket-сервер (режимы WIFI и Socket)

### 3.2 Общая структура

```
TSDXXIVEK/
├── tsdxxivek/
│   ├── src/main/java/com/xxivek/tsdxxivek/
│   │   │
│   │   │  === ОСНОВНАЯ ЧАСТЬ (общая для всех режимов) ===
│   │   │
│   │   ├── MainActivity.kt           — Точка входа, управление состоянием
│   │   ├── TSDXXIVekApplication.kt   — Application singleton, Room DB
│   │   ├── AppState.kt               — Глобальное состояние
│   │   ├── AppConstants.kt           — Константы (SharedPreferences ключи)
│   │   ├── FileExchangeManager.kt    — Чтение/запись JSON, polling папки
│   │   ├── QrPairingParser.kt        — Парсинг QR-кода
│   │   ├── MenuFragment.kt           — Главное меню, UI-управление
│   │   ├── LogoFragment.kt           — Настройки режима
│   │   ├── SettingsFragment.kt       — Настройки
│   │   ├── PairingFragment.kt        — Ожидание сопряжения
│   │   ├── StartFragment.kt          — Заставка
│   │   ├── dataDB/                   — Room база данных
│   │   │   ├── ItemRoomDatabase.kt   — DB builder (singleton)
│   │   │   ├── ItemDao.kt            — DAO интерфейс
│   │   │   ├── Item.kt               — Entity (товар)
│   │   │   └── UtilDB.kt             — Утилиты БД (XML импорт/экспорт)
│   │   ├── scaner/                   — Сканер штрихкодов
│   │   │   ├── ScanerFragment.kt     — CameraX + BarcodeScanning
│   │   │   └── ScanerFragment_d.kt   — Альтернативный дизайн
│   │   ├── utilAPP/                  — Утилиты
│   │   │   ├── LicenseUtil.kt        — ViewModel лицензии/состояний
│   │   │   └── LogUtil.kt            — Логирование в файл
│   │   │
│   │   │  === РЕЖИМНАЯ ЧАСТЬ (зависит от режима обмена) ===
│   │   │
│   │   ├── api/                      — HTTP API клиент (режим Socket/Сайт)
│   │   │   ├── ApiClient.kt          — Базовый HTTP-клиент
│   │   │   ├── FileDownloadApi.kt    — Расширенный клиент (download, export)
│   │   │   ├── StatusPollingService.kt — Polling статуса
│   │   │   └── ExchangeDataParser.kt   — Парсинг JSON с сайта
│   │   ├── serverHTTP/               — Серверы (режимы WIFI и Socket)
│   │   │   ├── LocalWifiServer.kt    — HTTP-сервер на ТСД
│   │   │   ├── BroadcastServer.kt    — UDP broadcast
│   │   │   └── ServerSocketXXI.kt    — Socket-сервер
│   │   └── dataXML/                  — XML парсеры (устаревший механизм)
│   │       ├── BuilderXML.kt         — Генерация XML
│   │       └── XMLDOMParser.kt       — Парсинг XML
│   ├── build.gradle                  — Зависимости, версии
│   └── src/main/AndroidManifest.xml
├── Docs/                             — Документация
│   ├── README.md                     — Оглавление
│   ├── PROJECT_MAP.md                — Карта проекта
│   ├── ARCH_MODES.md                 — Режимы работы, сопряжение, обмен
│   ├── ARCH_API.md                   — HTTP API, JSON, SharedPreferences
│   ├── ARCH_NAVIGATION.md            — Навигация, жизненный цикл
│   ├── ARCH_UI.md                    — UI-состояния
│   ├── HISTORY.md                    — История изменений
│   └── Tests/                        — Планы тестирования
└── PROXYAI.md                        — Этот файл
```

### 3.2 Технологический стек

| Компонент | Технология | Версия |
|-----------|-----------|--------|
| Язык | Kotlin | 1.9.10 |
| Min SDK | Android 11 (API 30) | |
| Target SDK | Android 14 (API 34) | |
| UI | ViewBinding, Fragments | |
| Навигация | Navigation Component | 2.7.7 / 2.9.8 |
| База данных | Room | 2.6.1 |
| HTTP | OkHttp | 4.12.0 |
| JSON | Gson | 2.10.1 |
| Камера | CameraX | 1.2.0-alpha04 |
| Сканер | ML Kit Barcode Scanning | 17.0.2 |
| Архитектура | ViewModel + LiveData | Lifecycle 2.6.2 |

### 3.3 Режимы работы (appConnect1C)

| Значение | Режим | Механизм обмена |
|----------|-------|-----------------|
| 0 | Не сопряжено | Ожидание QR-кода |
| 2 | Socket / Сайт | HTTP API к серверу ПК |
| 3 | Локальный USB | Файловый обмен `/sdcard/download/` |
| 4 | Локальный WIFI | HTTP API (JSON) к ТСД по IP |

### 3.4 Ключевые данные

**SharedPreferences ключи** (также `AppConstants.kt`):
- `license` — LIC;Port;Konf;LOGGING
- `connct1c` — статус сопряжения (0/2/3/4)
- `port` — номер порта
- `configuration` — конфигурация 1С
- `operition` — оператор
- `client` — клиент
- `device_uuid` — UUID устройства (режим Сайт)
- `use_website` — флаг режима «Сайт»
- `use_socket` — флаг Socket-режима
- `topic` — тема (0=светлая, 1=тёмная)
- `design` — дизайн сканера (0=T, 1=D)
- `logging` — логирование в файл

**Entity Item** (таблица `item`):
- `shtrihkod` (PRIMARY KEY) — штрихкод
- `shtrihtip` — тип штрихкода
- `name` — наименование
- `price` — цена
- `quantityInStock` — количество на складе
- `quantity` — текущее количество

**UI-состояния** (bd, input, output):
- `bd`: 0=пусто, 1=ошибка, 2=есть для выгрузки, 3=заполнена, -1=загрузка
- `input`: 0=нет, 1=ошибка, 3=есть данные, 6=получены
- `output`: 0=нет, 1=ошибка, 2=не определено, 3=отправлены

## 4. Сценарий работы приложения

### 4.1 Запуск (MainActivity.onCreate)
1. Загрузка настроек из SharedPreferences
2. `checkDeviceStatus()` — проверка сопряжения (режим Сайт)
3. Запуск StartFragment → LogoFragment
4. Если `appConnect1C > 0` → автопереход в MenuFragment
5. Если `appConnect1C == 0` → кнопка «Далее» → ScanerFragment

### 4.2 Сопряжение (QR-код)
1. ПК формирует QR: `PAIR:true;socket:true;konf:1;LOGGING:0;Port:8180;Lic:1`
2. ТСД сканирует QR через ScanerFragment
3. `QrPairingParser` парсит параметры
4. Сохранение в SharedPreferences
5. Переход на MenuFragment (если сопряжено) или PairingFragment

### 4.3 Обмен данными

**Режим Сайт (appConnect1C=2):**
- Активация: `POST /api/v1/devices/activate` с `activation_code`
- Статус: `GET /api/v1/devices/status` с `Authorization: Bearer <token>`
- Обновление: `POST /api/v1/devices/status` с JSON тела
- Polling через `StatusPollingService`

**Режим Local WIFI (appConnect1C=4):**
- ТСД запускает HTTP-сервер (`LocalWifiServer`)
- ТСД запускает UDP broadcast (`BroadcastServer`)
- ПК обнаруживает ТСД по UDP
- Обмен через HTTP API к IP ТСД
- Polling через `LocalWifiPollingService`

**Режим USB (appConnect1C=3):**
- Файлы в `/sdcard/download/`: `tsd_Input.json`, `tsd_Output.json`, `tsd_dev_status.txt`
- Чтение каждые 5 секунд

### 4.4 Закрытие (MainActivity.onDestroy)
1. Закрытие serverSocket, connectionSocket
2. `saveState()` — сохранение всех настроек
3. `exitProcess(-1)`

## 5. Навигация (граф фрагментов)

```
StartFragment (заставка)
  └─> LogoFragment (настройки: тема, дизайн, режим)
       ├─> MenuFragment (главное меню)
       │    ├─> ScanerFragment / ScanerFragment_d (сканер)
       │    ├─> SettingsFragment (настройки)
       │    ├─> FileDownloadFragment (скачивание)
       │    └─> ItemListFragment1/2 (список товаров)
       │         └─> AddItemFragment (добавление)
       ├─> PairingFragment (ожидание сопряжения)
       └─> SetgeneralFragment (общие настройки)
```

## 6. HTTP API

| Эндпоинт | Метод | Описание |
|----------|-------|----------|
| `/api/v1/devices/activate` | POST | Активация устройства |
| `/api/v1/devices/status` | GET | Статус устройства |
| `/api/v1/devices/status` | POST | Обновление статуса |
| `/api/v1/exchange/input` | POST | Данные ПК→ТСД |
| `/api/v1/exchange/output` | GET | Данные ТСД→ПК |

**Ответ GET /api/v1/devices/status:**
```json
{
  "device_uuid": "...",
  "status": {
    "pairing": true,
    "konf": 5,
    "bd": 3,
    "input": 2,
    "output": 4
  }
}
```

## 7. Формат QR-кода

```
PAIR:true;socket:true;konf:1;LOGGING:0;Port:8180;Lic:1
```

| Параметр | Описание | Значения |
|----------|----------|----------|
| PAIR | Сопряжение | true/false |
| socket | Socket-режим | true/false |
| konf | Конфигурация | 1=XXI век, 0=любая |
| LOGGING | Логирование | 0/1 |
| Port | Порт HTTP-сервера | любой |
| Lic | Лицензия ТСД | уникальный номер |

## 8. Работа с базой данных

- Room singleton через `TSDXXIVekApplication.database`
- DAO: `ItemDao` — getItems(), getItem(), insert(), update(), delete(), deleteAll()
- Flow для реактивных обновлений UI
- `UtilDB` — утилиты: импорт XML, экспорт XML, подсчёт записей

## 9. Логирование

- Включается через `LOGGING=1` в QR-коде или настройках
- Файл: `sdcard/Download/logTSD.dat`
- Формат: `dd.MM.yy hh:mm:ss(tag)message`
- Класс: `LogUtil.appendLog(tag, msg)`

## 10. Как работать с этим проектом

### При запросе на изменение кода:
1. Определи пакет и файл по структуре выше
2. Прочитай файл полностью
3. Предложи минимальные изменения
4. Укажи точные строки для редактирования

### При запросе на добавление функционала:
1. Определи, к какой части относится: основная (общая) или режимная
2. Для основной части — работает во всех режимах одинаково
3. Для режимной части — реализуй отдельно для каждого режима
4. Учти существующие паттерны (ViewModel + LiveData, CoroutineScope, SharedPreferences)
5. Не добавляй абстракции без необходимости
6. Следуй стилю существующего кода

### При анализе бага:
1. Определи режим работы (appConnect1C)
2. Определи, в какой части баг: основная или режимная
3. Определи цепочку: UI → ViewModel → DAO/API → БД/Сервер
4. Учти асинхронность (coroutines, Dispatchers.IO/Main)
5. Проверь состояния (bd, input, output)

## 11. Важные замечания

- Глобальные переменные в `MainActivity.kt`: `appLic`, `itemDatabase`, `msg_server`, `msg_client`, `LOGING`, `TOPIC`, `DESIGN`, `USE_WEBSITE`
- `LicenseUtil` — основной ViewModel для состояний (LiveData)
- Файлы обмена (режимы USB/WIFI): `/sdcard/download/` или `AppConstants.FILE_EXCHANGE_DIR`
- При сбросе сопряжения удаляются файлы обмена
- CameraX используется только в ScanerFragment
- Navigation через NavHostFragment (комментирован в MainActivity, работает через XML навигации)
- `TSDXXIVekApplication` имеет singleton `instance` (companion object) — используйте `TSDXXIVekApplication.instance` для доступа к БД
- `UtilDB` НЕ использует глобальные `itemDatabase`/`appLic` — вместо этого `getDao()` через `TSDXXIVekApplication.instance`
- `Headers.of()` в OkHttp устарел — используйте `Headers.headersOf()`
- `CoroutineName` доступен через `import kotlinx.coroutines.*`

## 12. Известные исправления (2026-09-09)

### Критические исправления режима Socket/Сайт

| Файл | Проблема | Решение |
|------|----------|---------|
| `QrPairingParser.kt` | Не поддерживал формат `PAIR:<activation_code>` | Добавлена поддержка 3 форматов QR (Socket/Сайт, USB, WIFI) |
| `ScanerFragment.kt` | QR `PAIR:ABC123` не распознавался | Автоматическая активация через `activateDevice()` |
| `UtilDB.kt` | `TSDXXIVekApplication()` — новый экземпляр | `TSDXXIVekApplication.instance.database.itemDao()` |
| `TSDXXIVekApplication.kt` | Не было singleton instance | Добавлен `companion object` с `INSTANCE` и `val instance` |
| `FileDownloadApi.kt` | Не было multipart/form-data | Добавлен `uploadResultMultipart()` |
| `ScanerFragment.kt` | `testWriteDevStatus()` сбрасывал pairing | Удалён тестовый код |
| `LogoFragment.kt` | `MainActivity()` — новый экземпляр | `requireActivity().finish()` |
| `ScanerFragment.kt` | `TSDXXIVekApplication()` в setupCamera | `requireActivity().application` |

## 12. Документация

| Файл | Содержание |
|------|-----------|
| `Docs/README.md` | Оглавление, текущий режим |
| `Docs/PROJECT_MAP.md` | Карта проекта, ключевые классы |
| `Docs/ARCH_MODES.md` | Режимы работы, сопряжение, обмен |
| `Docs/ARCH_API.md` | HTTP API, JSON, SharedPreferences |
| `Docs/ARCH_NAVIGATION.md` | Навигация, жизненный цикл |
| `Docs/ARCH_UI.md` | UI-состояния, цвета, тексты |
| `Docs/HISTORY.md` | История изменений |

## 13. Текущий этап работы

**Фаза:** Тестирование режима Local USB (appConnect1C=3)
**Файл плана:** `Docs/Tests/TEST_PLAN_USB.md`
**Метод:** Последовательное тестирование всех тестов от TS-001 до TS-023
**Текущий тест:** TS-001 — Сканирование QR-кода и вход в USB-режим
**Выполнено:**
- Тестирование Socket/Сайт: TS-001 — TS-030 ✅ (все 30 тестов, ошибок нет)
- Исправление критического бага: `itemDatabase` (всегда null) → `TSDXXIVekApplication.instance?.database?.itemDao()`
  - Исправлено 7 файлов: ItemListFragment, ItemListFragment1, ItemListFragment2, ScanerFragment, ScanerFragment_d, MenuFragment, LicenseUtil
  - Удалена неиспользуемая переменная `itemDatabase` из MainActivity.kt
- Коммит: `92f30b1`, push на GitHub выполнен

### Критические паттерны проекта
- **Доступ к БД:** всегда `TSDXXIVekApplication.instance?.database?.itemDao()` — НЕ использовать `itemDatabase`
- **Singleton Application:** `TSDXXIVekApplication.instance` — НЕ создавать новый экземпляр
- **Activity:** `requireActivity()` — НЕ создавать `MainActivity()`
- **QR-форматы:**
  - Socket/Сайт (appConnect1C=2): `PAIR:<activation_code>`
  - USB (appConnect1C=3): `PAIR:true;socket:false;konf:1;LOGGING:0;`
  - WIFI (appConnect1C=4): `PAIR:true;socket:true;konf:1;LOGGING:0;Port:8180;Lic:1;`
- **Папка обмена USB:** `/storage/emulated/0/Download/TSD/`
- **Файлы обмена:** `Input.json`, `Output.json`, `tsd_dev_status.txt` (SSV формат)

---

*Последнее обновление: 2026-09-10*
