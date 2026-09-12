# Test Plan — Local WIFI Mode (appConnect1C=4)

> **Device:** TSD (Terminal Collection Device) / Android TSDXXIVEK application
> **Mode:** Local WIFI — direct HTTP JSON API + UDP broadcast discovery
> **Scope:** Main scenarios only (no edge-case / negative testing)
> **Priority:** P0 = Critical / Blocker, P1 = Major, P2 = Minor

---

## 1. Сопряжение и обнаружение (Pairing & Discovery)

### TS-001: Сканирование QR-кода и вход в Local WIFI режим
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | TSD включён, база данных содержит данные (любой объём) |

**Steps:**
1. Перейти в раздел сканирования (ScanerFragment)
2. Отсканировать QR-код формата: `PAIR:true;socket:true;konf:1;LOGGING:0;Port:8180;Lic:5`
3. Подтвердить сопряжение

**Expected Result:**
- `appConnect1C` установлен в `4`
- `LocalWifiServer` запущен на порту `8180`
- `BroadcastServer` запущен с лицензией `"5"`
- В логах: `"JSON API сервер запущен на порту 8180"`
- В логах: `"BroadcastServer запущен: 192.168.x.x:8180 -> x.x.x.255"`
- На ТСД отображается, что устройство сопряжено

**Результат теста:** [x] УСПЕШНО — TS-001 прошёл, TS-002 прошёл. BroadcastServer запускается стабильно после перехода на MenuFragment.

---

### TS-002: UDP Broadcast-объявления от ТСД
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | ТСД в Local WIFI режиме (после TS-001) |

**Steps:**
1. Убедиться, что TSD в Local WIFI режиме
2. С помощью UDP-сниффера (Wireshark / PC-клиент) прослушивать порт `1900` на подсети
3. Наблюдать в течение 20 секунд

**Expected Result:**
- ТСД отправляет UDP-пакеты на broadcast-адрес подсети (port 1900)
- Интервал между пакетами: ~2 секунды
- Формат JSON: `{"ip":"192.168.x.x","port":8180,"lic":"5"}`
- IP-адрес соответствует реальному IP ТСД в сети
- Broadcast-объявления прекращаются после 20 секунд или первого HTTP-запроса к порту ТСД
- Количество отправок: ~10 (20 сек / 2 сек)

---

### TS-003: Обнаружение ТСД PC-клиентом (C++)
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | ТСД в Local WIFI режиме; PC и ТСД в одной подсети |

**Steps:**
1. Запустить PC-клиент (C++), настроить прослушку UDP port 1900
2. Указать ожидаемую лицензию (например `"5"`)
3. Дождаться UDP-объявления от ТСД
4. Проверить, что PC-клиент извлек IP и порт из broadcast

**Expected Result:**
- PC-клиент получает UDP-пакет с JSON `{"ip":"192.168.x.x","port":8180,"lic":"5"}`
- PC-клиент сопоставляет лицензию `"5"` с ожидаемой
- PC-клиент извлекает IP-адрес и порт ТСД
- Готов к HTTP-подключению

**Результат теста:** [x] УСПЕШНО — PC-клиент обнаружил ТСД по UDP broadcast.

---

## 2. Обмен данными ПК→ТСД (PC to TSD Data Transfer)

### TS-005: PC отправляет данные для загрузки в ТСД
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | ТСД сопряжён (TS-001); PC подключён к ТСД по HTTP |

**Steps:**
1. Сформировать JSON-данные для импорта:
   ```json
   {
     "oper": "OPER1",
     "client": "CLIENT1",
     "data": [
       {"Shtrih": "4607027387654", "ShtrihTip": 1, "TovNaim": "Товар A", "TovCena": 150.00, "TovKol": 10},
       {"Shtrih": "4607027387655", "ShtrihTip": 1, "TovNaim": "Товар B", "TovCena": 200.00, "TovKol": 5}
     ]
   }
   ```
2. Отправить `POST /api/v1/exchange/input` с JSON-телом на `http://192.168.x.x:8180`
3. Проверить ответ

**Expected Result:**
- HTTP-ответ: `200 OK`, тело: `{"status":"ok","message":"Данные получены"}`
- Файл `tsd_Input.json` создан в `/sdcard/download/` (или `AppConstants.FILE_EXCHANGE_DIR`)
- Содержимое файла совпадает с отправленным JSON
- Статус `input` установлен в `3` (has data)
- В логах: `"tsd_Input.json записан, input=3"`

**Результат теста:** [x] УСПЕШНО — Данные отправлены на ТСД, отражено на основном экране. Состояние ТСД изменено и получено обработкой 1С.

---

### TS-006: ТСД получает уведомление о новых данных (input=3) и импортирует
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | Данные загружены (TS-005); `input=3` |

**Steps:**
1. На ТСД в MenuFragment увидеть уведомление о наличии данных для загрузки
2. Нажать кнопку подтверждения / импорта
3. Дождаться завершения импорта

**Expected Result:**
- `FileExchangeManager.readInputAndImport()` выполняет:
  - Читает `tsd_Input.json`
  - Парсит JSON (data массив)
  - Очищает Room-таблицу (`dao.deleteAll()`)
  - Вставляет записи (`dao.insertList(items)`)
- `tsd_Input.json` удалён после импорта
- Статус `input` сброшен в `0`
- Статус `bd` обновлён: `2` (has records)
- В логах: `"Импорт завершен: 2 записей из tsd_Input.json"`
- Количество записей в БД соответствует количеству в JSON

**Результат теста:** [x] УСПЕШНО — Импортировано 3990 записей в базу данных. Состояние ТСД изменено и получено обработкой 1С.

---

### TS-007: PC проверяет статус ТСД и видит input=0 после импорта
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | Импорт завершён (TS-006) |

**Steps:**
1. `LocalWifiPollingService` опрашивает `GET /api/v1/devices/status` каждые 5 секунд
2. Дождаться следующего цикла опроса
3. Проверить полученные данные

**Expected Result:**
- Ответ: `{"device_uuid":"","status":{"pairing":true,"konf":1,"bd":2,"input":0,"output":0}}`
- `input = 0` (данные загружены, файл удалён)
- `bd = 2` (в БД есть записи)
- `pairing = true`
- MenuFragment обновляет UI (скрывает уведомление о загрузке)

**Результат теста:** [x] УСПЕШНО — PC (обработка 1С) видит input=0 после импорта.

---

### TS-008: Полный цикл PC→ТСД (отправка → импорт → подтверждение)
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | ТСД сопряжён, база данных пуста |

**Steps:**
1. PC отправляет `POST /api/v1/exchange/input` с 5 товарами
2. ТСД показывает уведомление, оператор нажимает "Импорт"
3. PC через 5 сек делает `GET /api/v1/devices/status`
4. PC отправляет ещё 10 товаров (второй пакет)
5. ТСД импортирует второй пакет
6. PC делает `GET /api/v1/devices/status`

**Expected Result:**
- Шаг 2: `tsd_Input.json` создан, `input=3`
- Шаг 3: `input=0`, `bd=2`, 5 товаров в БД
- Шаг 5: `tsd_Input.json` создан повторно, `input=3`
- Шаг 6: `input=0`, `bd=2`, 10 товаров в БД (предыдущие 5 удалены)
- Каждый импорт заменяет предыдущие данные (`deleteAll()` перед `insertList()`)

---

## 3. Обмен данными ТСД→ПК (TSD to PC Data Transfer)

### TS-009: ТСД формирует данные для выгрузки и уведомляет PC
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | В БД ТСД есть данные (≥1 запись), хотя бы один товар отсканирован и введено количество (`bd=3`) |

**Steps:**
1. На ТСД отсканировать штрихкод товара (найденного в БД) и ввести количество → `bd` меняется с `2` на `3`
2. На ТСД в MenuFragment нажать кнопку "Выгрузить" (или аналог)
3. Дождаться формирования `tsd_Output.json`
4. PC делает `GET /api/v1/devices/status`

**Expected Result:**
- `FileExchangeManager.writeOutputAndExport()` формирует `tsd_Output.json`
- Файл содержит JSON с полями: `oper`, `client`, `data` (массив товаров)
- Формат данных: `Shtrih`, `ShtrihTip`, `TovNaim`, `TovCena`, `TovKol`
- Статус `output` установлен в `3` (data sent)
- PC получает: `{"device_uuid":"","status":{"pairing":true,"konf":1,"bd":3,"input":0,"output":3}}`
- В логах: `"Экспорт: N записей в tsd_Output.json"`

---

### TS-010: PC запрашивает и получает данные выгрузки
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | output=3 (TS-009) |

**Steps:**
1. PC видит `output=3` через опрос статуса
2. PC делает `GET /api/v1/exchange/output` на `http://192.168.x.x:8180`
3. PC получает JSON-ответ

**Expected Result:**
- HTTP-ответ: `200 OK`
- Тело ответа = содержимое `tsd_Output.json`
- JSON содержит все записи из БД ТСД
- Статус `output` остаётся `3` (ожидание подтверждения от PC)
- В логах: `"tsd_Output.json отправлен, output=3"`

---

### TS-011: PC подтверждает получение данных
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | PC получил данные (TS-010) |

**Steps:**
1. PC отправляет `POST /api/v1/devices/status` с телом: `{"output":0}`
2. Дождаться ответа

**Expected Result:**
- HTTP-ответ: `200 OK`, тело: `{"status":"ok","device_uuid":"5"}`
- На ТСД: `output` установлен в `0`
- Файл `tsd_Output.json` удалён (или помечен как обработанный)
- В логах: `"output установлен: 0"`

---

### TS-012: Полный цикл ТСД→ПК (формирование → отправка → подтверждение)
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | В БД ТСД есть данные; ТСД сопряжён |

**Steps:**
1. На ТСД нажать "Выгрузить"
2. PC опрашивает статус → видит `output=3`
3. PC делает `GET /api/v1/exchange/output`
4. PC подтверждает: `POST /api/v1/devices/status` с `{"output":0}`
5. PC опрашивает статус → видит `output=0`

**Expected Result:**
- Шаг 1: `tsd_Output.json` создан, `output=3`
- Шаг 2: PC получает `output=3` в течение 5 сек
- Шаг 3: PC получает полный JSON с данными
- Шаг 4: HTTP 200 OK, `output` сброшен на ТСД
- Шаг 5: PC получает `output=0`, цикл завершён

---

### TS-013: Выгрузка при пустой БД
| Attribute | Value |
|-----------|-------|
| **Priority** | P1 |
| **Precondition** | В БД ТСД нет записей |

**Steps:**
1. На ТСД нажать "Выгрузить"
2. Проверить результат

**Expected Result:**
- `FileExchangeManager.writeOutputAndExport()` возвращает ошибку: `"Нет данных для выгрузки"`
- `tsd_Output.json` не создаётся
- Статус `output` остаётся `0`
- На ТСД отображается сообщение об отсутствии данных

---

## 4. Жизненный цикл (Lifecycle)

### TS-014: Запуск Local WIFI режима (checkLocalWifiMode)
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | Сохранённые настройки: QR-код отсканирован ранее, `port` и `license` в SharedPreferences |

**Steps:**
1. Запустить приложение
2. Проверить, что `appConnect1C == 4` (из сохранённых настроек)
3. Наблюдать за инициализацией

**Expected Result:**
- `appConnect1C` установлен в `4`
- `LocalWifiServer.start(port)` вызван с сохранённым портом
- `BroadcastServer.start(subnetPrefix, port, license)` вызван
- Состояния `bd`, `input`, `output` восстановлены из хранилища
- В логах: `"JSON API сервер запущен на порту X"`

---

### TS-015: Остановка серверов при выходе из Local WIFI режима
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | ТСД в Local WIFI режиме |

**Steps:**
1. Выйти из Local WIFI режима (сменить режим на другой, например файловый)
2. Проверить состояние серверов

**Expected Result:**
- `LocalWifiServer.stop()` вызван: `isRunning=false`, `job.cancel()`, `serverSocket.close()`
- `BroadcastServer.stop()` вызван: `stopRequested=true`, UDP-сокет закрыт
- В логах: `"JSON API сервер остановлен"`, `"BroadcastServer остановлен"`
- Состояния `bd`, `input`, `output` сохранены в SharedPreferences
- Порт и лицензия сохранены

---

### TS-016: Повторный запуск после перезагрузки
| Attribute | Value |
|-----------|-------|
| **Priority** | P1 |
| **Precondition** | Local WIFI режим был активен, настройки сохранены |

**Steps:**
1. Перезагрузить ТСД
2. Дождаться запуска приложения
3. Проверить состояние

**Expected Result:**
- Приложение загружает сохранённые настройки
- `appConnect1C == 4`
- Серверы запускаются автоматически
- Состояния `bd`, `input`, `output` восстановлены
- Данные в Room-БД сохранены

---

### TS-017: Одновременная работа серверов
| Attribute | Value |
|-----------|-------|
| **Priority** | P1 |
| **Precondition** | ТСД в Local WIFI режиме |

**Steps:**
1. Убедиться, что `LocalWifiServer` и `BroadcastServer` запущены
2. Отправить `GET /api/v1/devices/status`
3. Параллельно отправить `POST /api/v1/exchange/input`
4. Проверить, что оба запроса обработаны

**Expected Result:**
- Оба запроса обработаны корректно
- Сервер обрабатывает запросы последовательно (через `accept()` в цикле)
- Нет блокировок или зависаний
- `BroadcastServer` продолжает работать во время HTTP-запросов

---

## 5. UI и состояния (UI & States)

### TS-018: MenuFragment — отображение статуса устройства
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | ТСД в Local WIFI режиме |

**Steps:**
1. Открыть MenuFragment
2. Дождаться первого опроса `LocalWifiPollingService` (5 сек)
3. Проверить отображение статусов

**Expected Result:**
- Отображается статус сопряжения: "Сопряжено" / pairing indicator
- Отображается статус БД (`bd`): пустая / ошибка / есть записи / загружена
- Отображается статус входящих данных (`input`): нет данных / есть данные
- Отображается статус исходящих данных (`output`): нет / отправлены
- Значения обновляются каждые 5 секунд

---

### TS-019: UI — уведомление о входящих данных (input=3)
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | Данные загружены через PC (TS-005) |

**Steps:**
1. PC отправляет данные (`POST /api/v1/exchange/input`)
2. Дождаться опроса (≤5 сек)
3. Проверить MenuFragment

**Expected Result:**
- Появляется уведомление / кнопка "Загрузить данные"
- Статус `input` отображается как "Есть данные для загрузки"
- При нажатии на кнопку запускается `FileExchangeManager.readInputAndImport()`
- После импорта уведомление исчезает

---

### TS-020: UI — уведомление о готовности выгрузки (output=3)
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | В БД есть данные; данные сформированы для выгрузки |

**Steps:**
1. На ТСД сформировать `tsd_Output.json`
2. Дождаться опроса (≤5 сек)
3. Проверить MenuFragment

**Expected Result:**
- Статус `output` отображается как "Данные готовы к выгрузке"
- Кнопка "Выгрузить" активна
- При нажатии (если нужна) — отправляется `GET /api/v1/exchange/output`
- После подтверждения PC (`output=0`) статус сбрасывается

---

### TS-021: Корректное обновление bd-статуса
| Attribute | Value |
|-----------|-------|
| **Priority** | P1 |
| **Precondition** | ТСД в Local WIFI режиме |

**Steps:**
1. Начальное состояние: БД пуста (`bd=0`)
2. PC загружает данные → импортирует в БД
3. Проверить `bd` после импорта
4. Сформировать выгрузку → удалить данные из БД
5. Проверить `bd` после выгрузки

**Expected Result:**
- Шаг 1: `bd=0` (пустая БД)
- Шаг 3: `bd=2` (есть записи для выгрузки)
- Шаг 5: `bd=0` или `bd=3` (загружена, нет записей для выгрузки)
- Значения `bd` синхронизированы между Room-БД и HTTP-ответом

---

### TS-022: UI — синхронизация состояния LiveData
| Attribute | Value |
|-----------|-------|
| **Priority** | P1 |
| **Precondition** | ТСД в Local WIFI режиме |

**Steps:**
1. Запустить опрос (`LocalWifiPollingService.startPolling`)
2. С PC изменить статус: `POST /api/v1/devices/status` с `{"output":3}`
3. Дождаться следующего опроса (≤5 сек)
4. Проверить LiveData в `LicenseUtil`

**Expected Result:**
- `appLic.appInfoOUT` обновляется через `postValue()`
- MenuFragment (observer) получает обновление
- UI перерисовывается с новым значением `output`
- Нет дублирующих или пропущенных обновлений

---

## Сводная табль приоритетов

| # | Название | Категория | Приоритет |
|---|----------|-----------|-----------|
| TS-001 | Сканирование QR и вход в режим | Сопряжение | **P0** |
| TS-002 | UDP Broadcast-объявления | Сопряжение | **P0** |
| TS-003 | Обнаружение ТСД PC-клиентом | Сопряжение | **P0** |
| TS-005 | PC отправляет данные для загрузки | ПК→ТСД | **P0** |
| TS-006 | ТСД импортирует данные из tsd_Input.json | ПК→ТСД | **P0** |
| TS-007 | PC видит input=0 после импорта | ПК→ТСД | **P0** |
| TS-008 | Полный цикл ПК→ТСД | ПК→ТСД | **P0** |
| TS-009 | ТСД формирует данные для выгрузки | ТСД→ПК | **P0** |
| TS-010 | PC запрашивает данные выгрузки | ТСД→ПК | **P0** |
| TS-011 | PC подтверждает получение | ТСД→ПК | **P0** |
| TS-012 | Полный цикл ТСД→ПК | ТСД→ПК | **P0** |
| TS-013 | Выгрузка при пустой БД | ТСД→ПК | P1 |
| TS-014 | Запуск Local WIFI режима | Жизненный цикл | **P0** |
| TS-015 | Остановка серверов при выходе | Жизненный цикл | **P0** |
| TS-016 | Повторный запуск после перезагрузки | Жизненный цикл | P1 |
| TS-017 | Одновременная работа серверов | Жизненный цикл | P1 |
| TS-018 | Отображение статуса в UI | UI и состояния | **P0** |
| TS-019 | Уведомление о входящих данных | UI и состояния | **P0** |
| TS-020 | Уведомление о готовности выгрузки | UI и состояния | **P0** |
| TS-021 | Корректное обновление bd-статуса | UI и состояния | P1 |
| TS-022 | Синхронизация состояния LiveData | UI и состояния | P1 |

---

## Ключевые точки проверки (Checklist)

### HTTP API Endpoints
- [ ] `GET /api/v1/devices/status` — возвращает корректный JSON
- [ ] `POST /api/v1/devices/status` — обновляет `input`, `output`, `pairing`
- [ ] `POST /api/v1/exchange/input` — записывает файл, ставит `input=3`
- [ ] `GET /api/v1/exchange/output` — возвращает JSON, ставит `output=3`
- [ ] 404 для неизвестных путей

### Файлы обмена
- [ ] `tsd_Input.json` создаётся при `POST /exchange/input`
- [ ] `tsd_Input.json` удаляется после импорта
- [ ] `tsd_Output.json` создаётся при экспорте
- [ ] `tsd_Output.json` удаляется после подтверждения PC

### Состояния (LicenseUtil LiveData)
- [ ] `appInfoBD` — корректно обновляется
- [ ] `appInfoINPUT` — корректно обновляется
- [ ] `appInfoOUT` — корректно обновляется
- [ ] `appConnect1C == 4` в Local WIFI режиме

### BroadcastServer
- [ ] Интервал 2 сек, длительность 20 сек
- [ ] Формат: `{"ip":"...","port":...,"lic":"..."}`
- [ ] Остановка при явном `stop()`

### Polling
- [ ] Интервал 5 секунд
- [ ] `onStatusUpdated` вызывает callback
- [ ] `onPollError` при ошибке сети
- [ ] Корректная остановка через `stopPolling()`
