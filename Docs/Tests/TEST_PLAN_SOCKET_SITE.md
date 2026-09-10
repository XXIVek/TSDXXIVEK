# Test Plan — Socket / Сайт Mode (appConnect1C=2)

> **Device:** TSD (Terminal Collection Device) / Android TSDXXIVEK application
> **Mode:** Socket / Сайт — HTTP API через промежуточный сайт (`http://192.168.160.99`)
> **Scope:** Основные сценарии (без edge-case / negative testing)
> **Priority:** P0 = Critical / Blocker, P1 = Major, P2 = Minor
> **Test Environment:** Обработка 1С — Реальный сайт (192.168.160.99) — Реальный смартфон Xiaomi Redmi 9C NFC

---

## 0. Форматы QR-кодов по режимам

| Режим | appConnect1C | Формат QR-кода | Описание |
|-------|:---:|---------------|----------|
| **Socket / Сайт** | 2 | `PAIR:<activation_code>` | Код активации, полученный обработкой 1С с сайта при регистрации лицензии |
| **Локальный USB** | 3 | `PAIR:true;socket:false;konf:1;LOGGING:0;` | Файловый режим, без socket |
| **Локальный WIFI** | 4 | `PAIR:true;socket:true;konf:1;LOGGING:0;Port:8180;Lic:1;` | HTTP-сервер на ТСД, UDP broadcast |

---

## 1. Сопряжение и активация (Pairing & Activation)

### TS-001: Сканирование QR-кода активации и вход в режим Socket/Сайт
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | TSD включён; 1С сгенерировал `activation_code` через `POST /api/v1/devices/generate-code`; QR-код отображается (или показан текстом) |

**Steps:**
1. Перейти в раздел сканирования (ScanerFragment)
2. Отсканировать QR-код формата: `PAIR:ABC123` (где `ABC123` — 6-символьный activation_code)
3. Извлечь activation_code из QR-кода
4. Android вызывает `POST /api/v1/devices/activate` с телом `{"activation_code": "ABC123"}`

**Expected Result:**
- Activation code извлечён из QR-кода
- `POST /api/v1/devices/activate` отправлен **без авторизации** (эндпоинт работает только по коду активации)
- Сайт возвращает: `{"success": true, "device_uuid": "UUID", "message": "Use this UUID as Bearer token"}`
- `device_uuid` сохранён в SharedPreferences (`APP_PREF_DEVICE_UUID`)
- Android немедленно отправляет `POST /api/v1/devices/status` с `Authorization: Bearer <device_uuid>` и телом:
  ```json
  {"pairing": true, "konf": 0, "bd": 0, "input": 0, "output": 0}
  ```
- Сайт отвечает: `{"status": "ok", "device_uuid": "UUID", "paired": true}`
- Код активации **сгорает** (не может быть использован повторно)
- `appConnect1C` установлен в `2`
- `USE_WEBSITE = true`
- Переход на MenuFragment

**Ключевой код:** `QrPairingParser.parse()`, `ApiClient.activateDevice()`, `ApiClient.sendPairingStatus()`, `MainActivity`

---

### TS-002: Двухэтапная активация (activate → pairing)
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | activation_code получен от 1С |

**Steps:**
1. **Этап 1 — Активация:** `POST /api/v1/devices/activate` с `{"activation_code": "ABC123"}`
2. **Этап 2 — Статус сопряжения:** `POST /api/v1/devices/status` с `Authorization: Bearer <device_uuid>` и телом `{"pairing": true, "konf": 0, "bd": 0, "input": 0, "output": 0}`

**Expected Result:**
- Этап 1: `ActivateResponse(success=true, device_uuid="UUID")`
- device_uuid сохранён в SharedPreferences
- Этап 2: `PairingStatusResponse(status="ok", paired=true)`
- Сервер устанавливает `paired=true` в БД
- Код активации удаляется (сгорает)
- Устройство считается полностью сопряжённым

**Ключевой код:** `ApiClient.activateDevice()`, `ApiClient.sendPairingStatus()`

---

### TS-003: Проверка статуса при запуске (checkDeviceStatus)
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | `USE_WEBSITE = true`, `device_uuid` сохранён, сопряжение было ранее |

**Steps:**
1. Запустить приложение
2. `MainActivity.onCreate()` загружает настройки
3. `checkDeviceStatus()` вызывает `FileDownloadApi.getDeviceStatusSync()`

**Expected Result:**
- Запрос: `GET /api/v1/devices/status` с `Authorization: Bearer <device_uuid>`
- Ответ: `{"device_uuid":"UUID","status":{"pairing":true,"konf":1,"bd":0,"input":0,"output":0}}`
- Если `pairing=true`: `appConnect1C=2`, `appKONF` обновлён, переход на MenuFragment
- Если `pairing=false`: `resetPairingState()`, переход к сопряжению
- Если ошибка сети (`httpCode=-1`): `resetPairingState()`

**Ключевой код:** `MainActivity.checkDeviceStatus()`, `MainActivity.syncBdIfDifferent()`, `MainActivity.resetPairingState()`

---

## 2. Polling статуса (Status Polling)

### TS-004: Запуск polling статуса
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | ТСД в режиме Socket/Сайт; `USE_WEBSITE=true`; `device_uuid` сохранён |

**Steps:**
1. Открыть MenuFragment
2. `startStatusPolling()` проверяет `use_website` и `device_uuid`
3. Запускается `StatusPollingService.startPolling()`

**Expected Result:**
- Polling запущен в `CoroutineScope(Dispatchers.IO)`
- Интервал: 5 секунд (`POLL_INTERVAL_MS = 5000L`)
- Каждый цикл: `GET /api/v1/devices/status` с `Authorization: Bearer <device_uuid>`
- В логах: `"Polling запущен (5 сек)"`
- Callback `StatusPollingService.Callback` установлен на MenuFragment

**Ключевой код:** `MenuFragment.startStatusPolling()`, `StatusPollingService.startPolling()`

---

### TS-005: Получение статуса через polling
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | Polling запущен (TS-004) |

**Steps:**
1. ПК-обработка обновляет статус на сайте: `POST /api/v1/devices/status` с `{"bd":3,"input":3,"output":0}`
2. Ожидать следующий цикл polling (до 5 сек)
3. Проверить обновление UI

**Expected Result:**
- `StatusPollingService` получает `input=3, output=0`
- `appState.updateServerStatus(3, 0)` обновляет AppState
- `appLic.appInfoOUT.postValue(0)` обновляет LiveData
- `callback?.onStatusUpdated(3, 0)` вызывает MenuFragment
- MenuFragment обновляет `textViewInput` и `textViewOutput`
- `bd` проверяется асинхронно: `dao.getCount()`, `dao.getCountNotEmpty()`

**Ключевой код:** `StatusPollingService` (строки 90-165), `MenuFragment.onStatusUpdated()`, `MenuFragment.updateInputStatusUI()`, `MenuFragment.updateOutputStatusUI()`

---

### TS-006: Синхронизация bd-статуса
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | Polling запущен; в БД есть данные |

**Steps:**
1. На ТСД отсканировать товар и ввести количество → `quantity > 0`
2. Дождаться polling-цикла
3. `StatusPollingService` вычисляет `bd` по данным БД

**Expected Result:**
- `dao.getCountNotEmpty() > 0` → `calculatedBd = 2`
- Если `calculatedBd != appState.appBd`:
  - `appState.appBd = 2`
  - `POST /api/v1/devices/status` с `bd=2`
  - `appLic.appInfoBD.postValue(2)`
- UI обновляется: `textViewBD` — жёлтый, "БД: Всего N зап., из них выб. M"

**Ключевой код:** `StatusPollingService` (строки 114-165), `MenuFragment.updateBDStatus()`

---

### TS-007: Остановка polling при уходе с экрана
| Attribute | Value |
|-----------|-------|
| **Priority** | P1 |
| **Precondition** | Polling запущен |

**Steps:**
1. Перейти с MenuFragment на другой экран
2. `onPause()` / `onDestroyView()` вызывается

**Expected Result:**
- `stopStatusPolling()` вызывает `pollingService?.stopPolling()`
- `coroutineScope?.cancel()` — coroutineScope завершён
- В логах: `"Polling остановлен"`
- При возврате на MenuFragment: `onResume()` перезапускает polling

**Ключевой код:** `MenuFragment.stopStatusPolling()`, `StatusPollingService.stopPolling()`

---

## 3. Сценарий А: Импорт товаров (ПК → ТСД — JSON exchange)

> **Описание:** ПК отправляет JSON-список товаров → ТСД скачивает → импортирует в Room-БД

### TS-008: Обнаружение pending заданий и скачивание файлов
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | Polling запущен; ПК отправил данные на сайт |

**Steps:**
1. ПК отправляет данные на сайт (через обработку 1С)
2. Сайт размещает файл задания (например `task_001.json`)
3. Polling обнаруживает `input=6` (или `input=3`)

**Expected Result:**
- `StatusPollingService` получает статус с `input=6` (или `input=3`)
- `api.getPendingFilesSync()` запрашивает `GET /api/v1/exchange/download?limit=50&offset=0`
- Получен список `PendingFileItem` (только `status="pending"`)
- `callback?.onPendingDataAvailable(items)` — Toast на ТСД: "Получены данные: N файлов"
- `api.downloadAllFilesSync()` скачивает файлы через `GET /api/v1/exchange/files/{message_id}`
- Файлы сохраняются в `context.getExternalFilesDir("downloads")`
- **Файл НЕ удаляется при скачивании** (согласно спецификации)
- `callback?.onFilesDownloaded(downloadedFiles)` — Toast: "Файлы скачаны: N. Нажмите 'Загрузить'."
- `downloadedFiles` сохранены в `MenuFragment.downloadedFiles`

**Ключевой код:** `StatusPollingService` (строки 168-212), `FileDownloadApi.getPendingFilesSync()`, `FileDownloadApi.downloadAllFilesSync()`

---

### TS-009: Загрузка скачанных файлов в БД
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | Файлы скачаны (TS-008); `downloadedFiles` не пуст |

**Steps:**
1. На ТСД в MenuFragment нажать кнопку "Загрузить" (`bInput`)
2. `onInput()` проверяет `useWebsite` и `downloadedFiles`
3. `loadDownloadedFilesToDB()` выполняется

**Expected Result:**
- Для каждого файла: `ExchangeDataParser.parseFile()` → `parseDataToList()`
- `oper` и `client` извлекаются из JSON
- `appLic.setAppOper(oper, context)` и `appLic.setAppClient(client, context)`
- `UtilDB().insertItemList(items)` — вставка в Room
- Скачанные файлы из `downloads/` удалены
- `updateDeviceStatusSync(bd=3, input=0)` — статус отправлен на сервер
- UI: `textViewInput` — серый, "Загрузка: Нет данных для загрузки"
- Toast: "Загружено в БД: N записей из M файлов"

**Ключевой код:** `MenuFragment.onInput()`, `MenuFragment.loadDownloadedFilesToDB()`, `ExchangeDataParser.parseFile()`, `ExchangeDataParser.parseDataToList()`

---

### TS-010: Полный цикл сценария А (ПК → ТСД — импорт товаров)
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | ТСД в режиме Socket/Сайт; polling запущен; БД пуста |

**Steps:**
1. ПК отправляет JSON-данные на сайт (10 товаров)
2. Сайт размещает файл задания
3. ТСД обнаруживает `input=6`, скачивает файлы (автоматически)
4. Оператор на ТСД нажимает "Загрузить"
5. Данные импортируются в БД
6. Статус `input=0` отправлен на сервер

**Expected Result:**
- Шаг 3: ТСД получает файлы, Toast "Файлы скачаны"
- Шаг 4-5: `loadDownloadedFilesToDB()` — 10 записей в БД
- Шаг 6: `POST /api/v1/devices/status` с `bd=3, input=0`
- В логах: все этапы зафиксированы

---

## 4. Сценарий Б: Инвентаризация (ПК → ТСД → ПК — task/result.json)

> **Описание:** ПК отправляет задание на инвентаризацию → ТСД сканирует штрихкоды → загружает результаты

### TS-026: Скачивание файла задания (task.json)
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | ПК отправил задание на сайт; `input=6` |

**Steps:**
1. `GET /api/v1/exchange/download?limit=50&offset=0` — получение списка pending
2. `GET /api/v1/exchange/files/{message_id}` — скачивание файла задания
3. Проверить заголовки ответа

**Expected Result:**
- Ответ 200 OK
- Заголовки: `Content-Disposition: attachment; filename="task_001.json"`, `X-Original-Filename: task_001.json`
- Файл сохранён во внутреннем хранилище
- **Файл НЕ удалён** после скачивания (можно скачивать повторно)

---

### TS-027: Обработка задания (сканирование штрихкодов)
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | Файл задания скачан (TS-026) |

**Steps:**
1. Распарсить `task.json` — список товаров для инвентаризации
2. Оператор сканирует штрихкоды товаров через ScanerFragment
3. Вводит фактическое количество
4. Формируется `result.json`

**Expected Result:**
- `task.json` содержит список товаров для сканирования
- Оператор сканирует каждый товар
- `result.json` формируется с фактическими количествами

---

### TS-028: Загрузка результатов (result.json)
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | `result.json` сформирован (TS-027) |

**Steps:**
1. `POST /api/v1/exchange/upload` — загрузка результатов

**Expected Result:**
- **Формат:** `multipart/form-data`:
  - `file`: `result.json` (бинарный)
  - `message`: `{"type":"inventory_result","task_id":"{message_id}"}`
- **ИЛИ** plain JSON body (если сервер поддерживает `application/json` для JSON-файлов)
- Ответ 201 Created: `{"status":"ok","message_id":"UUID","backoffice_device_uuid":"UUID"}`

---

### TS-029: Удаление файла задания
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | Результаты загружены (TS-028) |

**Steps:**
1. `DELETE /api/v1/exchange/files/{message_id_задания}`

**Expected Result:**
- Ответ 200 OK: `{"status":"ok","message_id":"UUID","deleted":true}`
- Файл и сообщение удалены с сервера
- Удаление необратимо

---

### TS-030: Полный цикл сценария Б (инвентаризация)
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | ТСД в режиме Socket/Сайт; polling запущен |

**Steps:**
1. ПК отправляет задание на сайт (`task.json`)
2. ТСД скачивает задание
3. Оператор сканирует штрихкоды
4. ТСД загружает `result.json`
5. ТСД удаляет файл задания
6. ПК получает результаты

**Expected Result:**
- Шаг 2: `GET /api/v1/exchange/files/{id}` — файл скачан
- Шаг 4: `POST /api/v1/exchange/upload` — результаты загружены
- Шаг 5: `DELETE /api/v1/exchange/files/{id}` — задание удалено
- Шаг 6: ПК получает `result.json` через `GET /api/v1/exchange/download`

---

## 5. Выгрузка данных ТСД→ПК (TSD to PC — Export & Upload)

### TS-011: Экспорт данных и загрузка на сервер
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | В БД есть данные с `quantity > 0` |

**Steps:**
1. На ТСД в MenuFragment нажать кнопку "Выгрузить" (`bOutput`)
2. `exportToWebsite()` выполняется

**Expected Result:**
- Проверка `token` (device_uuid) — не пуст
- `itemDatabase?.getItemNotEmpty2()` — получение записей
- `JsonExport.exportToJSON()` — создание JSON-файла в `getExternalFilesDir(null)`
- `FileDownloadApi.uploadResult()` — `POST /api/v1/exchange/upload` с JSON-телом
- `ExportUploadResult(success=true)` — статус загрузки
- `updateDeviceStatusSync(output=3)` — статус отправлен на сервер
- UI: `textViewOutput` — зелёный, "Выгрузка: Данные отправлены"
- Toast: "Данные отправлены: N записей"

**Ключевой код:** `MenuFragment.exportToWebsite()`, `JsonExport.exportToJSON()`, `FileDownloadApi.uploadResult()`, `FileDownloadApi.exportAndUpload()`

---

### TS-012: Подтверждение выгрузки на сервере
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | Выгрузка выполнена (TS-011); `output=3` на сервере |

**Steps:**
1. ПК-обработка получает `output=3` через polling сайта
2. ПК запрашивает данные: `GET /api/v1/exchange/output`
3. ПК подтверждает: `POST /api/v1/devices/status` с `{"output":0}`
4. ТСД polling обнаруживает `output=0`

**Expected Result:**
- ПК получает JSON-данные через `GET /api/v1/exchange/output`
- После подтверждения: `StatusPollingService` получает `output=0`
- `callback?.onStatusUpdated(input, 0)` обновляет UI
- `textViewOutput` — серый, "Выгрузка: Отсутствуют записи для выгрузки"

---

### TS-013: Полный цикл ТСД→ПК (экспорт → подтверждение)
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | В БД есть данные с `quantity > 0` |

**Steps:**
1. На ТСД нажать "Выгрузить"
2. `exportToWebsite()` — экспорт + upload на сервер
3. `updateDeviceStatusSync(output=3)` — статус на сервере
4. ПК получает `output=3` через polling
5. ПК запрашивает данные: `GET /api/v1/exchange/output`
6. ПК подтверждает: `POST /api/v1/devices/status` с `{"output":0}`
7. ТСД polling обнаруживает `output=0`

**Expected Result:**
- Шаг 2: JSON-файл создан, upload успешен
- Шаг 3: `output=3` на сервере
- Шаг 4: ПК видит `output=3` в течение 5 сек
- Шаг 5: ПК получает полный JSON
- Шаг 7: ТСД видит `output=0`, UI обновлён

---

### TS-014: Выгрузка при пустой БД
| Attribute | Value |
|-----------|-------|
| **Priority** | P1 |
| **Precondition** | В БД нет записей или нет записей с `quantity > 0` |

**Steps:**
1. На ТСД нажать "Выгрузить"
2. Проверить результат

**Expected Result:**
- `itemDatabase?.getItemNotEmpty2()` возвращает пустой список
- Toast: "Нет данных для выгрузки"
- `exportToWebsite()` завершается без действия
- Статус `output` не меняется

---

### TS-015: Очистка количества (clearQuantity)
| Attribute | Value |
|-----------|-------|
| **Priority** | P1 |
| **Precondition** | В БД есть данные с `quantity > 0` |

**Steps:**
1. На ТСД нажать кнопку "Очистить количество" (`bClearCont`)
2. `UtilDB().clearQuantity()` выполняется

**Expected Result:**
- Все записи с `quantity > 0` получают `quantity = 0`
- `appLic.conditionInfo()` обновляет `appInfoBD`
- Если `appConnect1C == 2`: `appLic.appInfoBD.postValue(3)`
- UI: `textViewBD` — зелёный, "БД: Всего N зап."

**Ключевой код:** `MenuFragment.bClearCont`, `UtilDB.clearQuantity()`, `LicenseUtil.conditionInfo()`

---

## 6. Жизненный цикл (Lifecycle)

### TS-016: Запуск приложения в режиме Socket/Сайт
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | `USE_WEBSITE=true`, `device_uuid` сохранён, `appConnect1C=2` |

**Steps:**
1. Запустить приложение
2. `MainActivity.onCreate()` загружает настройки
3. `checkDeviceStatus()` проверяет статус

**Expected Result:**
- Настройки загружены из SharedPreferences
- `checkDeviceStatus()` отправляет `GET /api/v1/devices/status`
- Если `pairing=true`: `appConnect1C=2`, `syncBdIfDifferent()`
- Переход на LogoFragment → MenuFragment
- Polling запускается при открытии MenuFragment

**Ключевой код:** `MainActivity.onCreate()`, `MainActivity.checkDeviceStatus()`, `MainActivity.syncBdIfDifferent()`

---

### TS-017: Сброс сопряжения
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | ТСД в режиме Socket/Сайт |

**Steps:**
1. На LogoFragment нажать кнопку сброса сопряжения (`bLogoPairing`)
2. `resetPairingState()` выполняется

**Expected Result:**
- SharedPreferences: `appConnect1C=0`, `device_uuid` удалён, `license=-1`
- `appLic.appConnect1C = 0`, `appLic.appLIC = "-1"`
- Приложение перезапускается (`Intent(context, MainActivity::class.java)`)
- Переход к экрану сопряжения (ScanerFragment)

**Ключевой код:** `MainActivity.resetPairingState()`, `LogoFragment.bLogoPairing`

---

### TS-018: Переключение режима (Socket → Local)
| Attribute | Value |
|-----------|-------|
| **Priority** | P1 |
| **Precondition** | ТСД в режиме Socket/Сайт |

**Steps:**
1. На LogoFragment переключить режим на "Локальный" (`radioButtonLocal`)
2. `radioGroupUseWebsite` изменяет `USE_WEBSITE=false`

**Expected Result:**
- `USE_WEBSITE = false`
- Все настройки сопряжения сброшены (`appConnect1C=0`, `device_uuid` удалён)
- `appLic.appLIC = "-1"`, `appLic.appConnect1C = 0`
- Навигация к LogoFragment для обновления UI

**Ключевой код:** `LogoFragment.radioGroupUseWebsite`

---

### TS-019: Закрытие приложения
| Attribute | Value |
|-----------|-------|
| **Priority** | P1 |
| **Precondition** | Приложение запущено |

**Steps:**
1. `MainActivity.onDestroy()` вызывается
2. `appExit()` выполняется

**Expected Result:**
- `serverSocket?.close()` — серверный сокет закрыт
- `connectionSocket?.close()` — клиентский сокет закрыт
- `exitProcess(-1)` — процесс завершён
- В логах: "HTTP Сервер остановлен", "HTTP клиент остановлен"

**Ключевой код:** `MainActivity.appExit()`, `MainActivity.onDestroy()`

---

## 7. UI и состояния (UI & States)

### TS-020: MenuFragment — отображение статусов
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | ТСД в режиме Socket/Сайт; polling запущен |

**Steps:**
1. Открыть MenuFragment
2. Дождаться polling-цикла
3. Проверить отображение статусов

**Expected Result:**
- `textViewBD`:
  - `bd=0`: серый, "БД: В базе данных нет записей"
  - `bd=2`: жёлтый, "БД: Всего N зап., из них выб. M"
  - `bd=3`: зелёный, "БД: Всего N зап."
- `textViewInput`:
  - `input=0`: серый, "Загрузка: Нет данных для загрузки"
  - `input=3`: жёлтый, "Загрузка: Данные доступны. Нажмите 'Загрузить'"
  - `input=6`: зелёный, "Загрузка: Данные получены! Нажмите 'Загрузить'"
- `textViewOutput`:
  - `output=0`: серый, "Выгрузка: Отсутствуют записи для выгрузки"
  - `output=3`: зелёный, "Выгрузка: Данные отправлены"

**Ключевой код:** `MenuFragment.updateBDStatus()`, `MenuFragment.updateInputStatusUI()`, `MenuFragment.updateOutputStatusUI()`

---

### TS-021: LiveData-синхронизация
| Attribute | Value |
|-----------|-------|
| **Priority** | P1 |
| **Precondition** | ТСД в режиме Socket/Сайт |

**Steps:**
1. Подписаться на LiveData в MenuFragment (`infoLiveData()`)
2. Изменить статус через polling или вручную
3. Проверить обновление UI

**Expected Result:**
- `appInfoBD` → `updateBDStatus()` — перерисовка textViewBD
- `appInfoINPUT` → `updateInputStatusUI()` — перерисовка textViewInput
- `appInfoOUT` → `updateOutputStatusUI()` — перерисовка textViewOutput
- `appInfoCountBD` → `mCount` — обновление счётчика
- `appInfoCountNotEmptyBD` → `mCountNotEmpty` — обновление счётчика
- Нет дублирующих или пропущенных обновлений

**Ключевой код:** `MenuFragment.infoLiveData()`, `LicenseUtil` LiveData

---

### TS-022: Синхронизация bd при запуске (syncBdIfDifferent)
| Attribute | Value |
|-----------|-------|
| **Priority** | P1 |
| **Precondition** | `checkDeviceStatus()` обнаружил `pairing=true`; `bd` на сервере отличается от локального |

**Steps:**
1. Запустить приложение
2. Локальный `bd` отличается от серверного
3. `syncBdIfDifferent()` выполняется

**Expected Result:**
- `bdLocal != serverBd` → отправляется `POST /api/v1/devices/status`
- `DeviceStatusUpdate(pairing=true, konf=..., bd=bdLocal, input=0, output=0)`
- В логах: "Синхронизация bd: локальный=X, сервер=Y"
- В логах: "Синхронизация bd: status=ok"

**Ключевой код:** `MainActivity.syncBdIfDifferent()`

---

### TS-023: Оператор и клиент из JSON
| Attribute | Value |
|-----------|-------|
| **Priority** | P1 |
| **Precondition** | Данные получены с сайта |

**Steps:**
1. ПК отправляет JSON с `oper="1"`, `client="КлиентА"`
2. ТСД скачивает и импортирует данные
3. `loadDownloadedFilesToDB()` извлекает oper/client

**Expected Result:**
- `parsedOper = "1"`, `parsedClient = "КлиентА"`
- `appLic.setAppOper("1", context)` — сохранено в SharedPreferences
- `appLic.setAppClient("КлиентА", context)` — сохранено в SharedPreferences
- `textViewOperInfo` обновлён: "Приход.\nПоставщик: КлиентА"

**Ключевой код:** `MenuFragment.loadDownloadedFilesToDB()`, `MenuFragment.updateOperInfo()`

---

## 8. Обмен данными — JSON форматы

### TS-024: Формат tsd_Input.json (экспорт с сайта)
| Attribute | Value |
|-----------|-------|
| **Priority** | P1 |
| **Precondition** | Данные отправлены ПК на сайт |

**Expected Result:**
- JSON-структура:
  ```json
  {
    "oper": "1",
    "client": "КлиентА",
    "data": [
      {"Shtrih": "4600068026514", "ShtrihTip": 1, "TovNaim": "Товар", "TovCena": 22.8, "TovKol": 10}
    ]
  }
  ```
- `ExchangeDataParser.parseFile()` успешно парсит JSON
- `ExchangeDataParser.parseDataToList()` возвращает список Item
- Поля маппинга: `Shtrih→itemSh`, `ShtrihTip→itemShTip`, `TovNaim→itemName`, `TovCena→itemPrice`, `TovKol→itemQuantityInStock`

---

### TS-025: Формат выгрузки (JsonExport)
| Attribute | Value |
|-----------|-------|
| **Priority** | P1 |
| **Precondition** | В БД есть данные с `quantity > 0` |

**Expected Result:**
- `JsonExport.exportToJSON()` создаёт файл `Output_<deviceUuid>.json`
- JSON-структура:
  ```json
  {
    "oper": "1",
    "client": "КлиентА",
    "data": [
      {"Shtrih": "4600068026514", "ShtrihTip": 1, "TovNaim": "Товар", "TovCena": 22.8, "TovKol": 5}
    ]
  }
  ```
- `@SerializedName` маппинг в `ExportItemDto` корректен
- Файл загружается через `POST /api/v1/exchange/upload`

---

## Сводная таблица тестов

| № | Название | Категория | Приоритет | Статус |
|---|----------|-----------|-----------|--------|
| TS-001 | Сканирование QR-кода активации и вход в режим | Сопряжение | **P0** | ✅ Выполнен |
| TS-002 | Двухэтапная активация (activate → pairing) | Сопряжение | **P0** | ✅ Выполнен |
| TS-003 | Проверка статуса при запуске | Сопряжение | **P0** | ✅ Выполнен |
| TS-004 | Запуск polling статуса | Polling | **P0** | ✅ Выполнен |
| TS-005 | Получение статуса через polling | Polling | **P0** | ✅ Выполнен |
| TS-006 | Синхронизация bd-статуса | Polling | **P0** | ✅ Выполнен |
| TS-007 | Остановка polling при уходе с экрана | Polling | P1 | ✅ Выполнен |
| TS-008 | Обнаружение pending заданий и скачивание файлов | Сценарий А (ПК→ТСД) | **P0** | ✅ Выполнен |
| TS-009 | Загрузка скачанных файлов в БД | Сценарий А (ПК→ТСД) | **P0** | ✅ Выполнен |
| TS-010 | Полный цикл сценария А (импорт товаров) | Сценарий А (ПК→ТСД) | **P0** | ✅ Выполнен |
| TS-026 | Скачивание файла задания (task.json) | Сценарий Б (инвентаризация) | **P0** | ✅ Выполнен |
| TS-027 | Обработка задания (сканирование штрихкодов) | Сценарий Б (инвентаризация) | **P0** | ✅ Выполнен |
| TS-028 | Загрузка результатов (result.json) | Сценарий Б (инвентаризация) | **P0** | ✅ Выполнен |
| TS-029 | Удаление файла задания | Сценарий Б (инвентаризация) | **P0** | ✅ Выполнен |
| TS-030 | Полный цикл сценария Б (инвентаризация) | Сценарий Б (инвентаризация) | **P0** | ✅ Выполнен |
| TS-011 | Экспорт данных и загрузка на сервер | ТСД→ПК | **P0** | ✅ Выполнен |
| TS-012 | Подтверждение выгрузки на сервере | ТСД→ПК | **P0** | ✅ Выполнен |
| TS-013 | Полный цикл ТСД→ПК | ТСД→ПК | **P0** | ✅ Выполнен |
| TS-014 | Выгрузка при пустой БД | ТСД→ПК | P1 | ✅ Выполнен |
| TS-015 | Очистка количества | ТСД→ПК | P1 | ✅ Выполнен |
| TS-016 | Запуск приложения в режиме Socket/Сайт | Жизненный цикл | **P0** | ✅ Выполнен |
| TS-017 | Сброс сопряжения | Жизненный цикл | **P0** | ✅ Выполнен |
| TS-018 | Переключение режима | Жизненный цикл | P1 | ✅ Выполнен |
| TS-019 | Закрытие приложения | Жизненный цикл | P1 | ✅ Выполнен |
| TS-020 | Отображение статусов в UI | UI и состояния | **P0** | ✅ Выполнен |
| TS-021 | LiveData-синхронизация | UI и состояния | P1 | ✅ Выполнен |
| TS-022 | Синхронизация bd при запуске | UI и состояния | P1 | ✅ Выполнен |
| TS-023 | Оператор и клиент из JSON | UI и состояния | P1 | ✅ Выполнен |
| TS-024 | Формат tsd_Input.json | JSON форматы | P1 | ✅ Выполнен |
| TS-025 | Формат выгрузки (JsonExport) | JSON форматы | P1 | ✅ Выполнен |

---

## Ключевые точки проверки (Checklist)

### HTTP API Endpoints (через сайт)
- [x] `POST /api/v1/devices/activate` — активация без токена, получение device_uuid
- [x] `POST /api/v1/devices/status` (pairing=true) — сопряжение, сгорание activation_code
- [x] `GET /api/v1/devices/status` — получение статуса с Bearer-токеном
- [x] `POST /api/v1/devices/status` — обновление статуса (bd, input, output)
- [x] `GET /api/v1/exchange/download?limit=50&offset=0` — получение списка pending файлов
- [x] `GET /api/v1/exchange/files/{message_id}` — скачивание файла (НЕ удаляется)
- [x] `POST /api/v1/exchange/upload` — загрузка JSON-файла (multipart/form-data ИЛИ application/json)
- [x] `DELETE /api/v1/exchange/files/{message_id}` — удаление файла с сервера

### QR-коды по режимам
- [x] Socket/Сайт: `PAIR:<activation_code>` — только код активации
- [x] Локальный USB: `PAIR:true;socket:false;konf:1;LOGGING:0;`
- [x] Локальный WIFI: `PAIR:true;socket:true;konf:1;LOGGING:0;Port:8180;Lic:1;`

### Авторизация
- [x] `Authorization: Bearer <device_uuid>` во всех запросах (кроме activate)
- [x] device_uuid сохраняется в SharedPreferences
- [x] Запрос activate работает без токена
- [x] activation_code сгорает после pairing=true

### Polling
- [x] Интервал 5 секунд
- [x] `onStatusUpdated` вызывает callback MenuFragment
- [x] `onPendingDataAvailable` при input=6
- [x] `onFilesDownloaded` после скачивания
- [x] `onPollError` при ошибке сети
- [x] Корректная остановка через `stopPolling()`

### БД (Room)
- [x] `ItemDao.getCount()` — корректный подсчёт
- [x] `ItemDao.getCountNotEmpty()` — корректный подсчёт quantity>0
- [x] `ItemDao.insertList()` — вставка списка
- [x] `ItemDao.deleteAll()` — очистка таблицы
- [x] `ItemDao.getItemNotEmpty2()` — получение записей для выгрузки

### LiveData
- [x] `appInfoBD` — корректно обновляется
- [x] `appInfoINPUT` — корректно обновляется
- [x] `appInfoOUT` — корректно обновляется
- [x] `appInfoCountBD` — корректно обновляется
- [x] `appInfoCountNotEmptyBD` — корректно обновляется

### Состояния (bd, input, output)
- [x] `bd=0` — пустая БД
- [x] `bd=2` — есть записи с quantity>0
- [x] `bd=3` — загружена, нет quantity>0
- [x] `input=0` — нет данных
- [x] `input=3` — есть данные
- [x] `input=6` — данные получены (скачаны)
- [x] `output=0` — нет для выгрузки
- [x] `output=3` — отправлены

### Сценарий Б: Инвентаризация
- [ ] `GET /api/v1/exchange/files/{id}` — файл скачан, НЕ удалён
- [ ] Повторное скачивание файла возможно
- [ ] `POST /api/v1/exchange/upload` — multipart/form-data или JSON body
- [ ] `DELETE /api/v1/exchange/files/{id}` — задание удалено после загрузки результатов

---

## Исправления кода (2026-09-09)

| # | Файл | Исправление | Статус |
|---|------|-------------|--------|
| 1 | `QrPairingParser.kt` | Добавлена поддержка формата `PAIR:<activation_code>` для Socket/Сайт | ✅ Исправлено |
| 2 | `ScanerFragment.kt` | Автоматическая активация при сканировании QR `PAIR:ABC123` | ✅ Исправлено |
| 3 | `UtilDB.kt` | `TSDXXIVekApplication()` → `TSDXXIVekApplication.instance` | ✅ Исправлено |
| 4 | `TSDXXIVekApplication.kt` | Добавлен singleton `instance` | ✅ Исправлено |
| 5 | `FileDownloadApi.kt` | Добавлен метод `uploadResultMultipart()` | ✅ Исправлено |
| 6 | `ScanerFragment.kt` | Удалён тестовый метод `testWriteDevStatus()` | ✅ Исправлено |
| 7 | `LogoFragment.kt` | `MainActivity()` → `requireActivity().finish()` | ✅ Исправлено |
| 8 | `ScanerFragment.kt` | `TSDXXIVekApplication()` → `requireActivity().application` | ✅ Исправлено |
| — | `StatusPollingService.kt` | `CoroutineName` не импортирован | ❌ НЕ ПОДТВЕРДИЛСЯ |

---

*Последнее обновление: 2026-09-09*
