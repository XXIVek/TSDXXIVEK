# Test Plan — Local USB Mode (appConnect1C=3)

> **Device:** TSD (Terminal Collection Device) / Android TSDXXIVEK application
> **Mode:** Local USB / File Exchange — file-based JSON exchange via `/storage/emulated/0/Download/TSD/`
> **Scope:** Main scenarios only (no edge-case / negative testing)
> **Priority:** P0 = Critical / Blocker, P1 = Major, P2 = Minor
> **Test Environment:** TSD with Android 11+ (API 30+), file exchange directory: `/storage/emulated/0/Download/TSD/`

---

## 0. Форматы QR-кодов по режимам

| Режим | appConnect1C | Формат QR-кода | Описание |
|-------|:---:|---------------|----------|
| **Socket / Сайт** | 2 | `PAIR:<activation_code>` | Код активации, полученный через сайт |
| **Локальный USB** | 3 | `PAIR:true;socket:false;konf:1;LOGGING:0;` | Файловый режим, без socket |
| **Локальный WIFI** | 4 | `PAIR:true;socket:true;konf:1;LOGGING:0;Port:8180;Lic:1;` | HTTP-сервер на ТСД, UDP broadcast |

---

## 1. Сопряжение и вход (Pairing)

### TS-001: Сканирование QR-кода и вход в USB-режим
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | TSD включён; база данных содержит данные (любой объём) |

**Steps:**
1. Перейти в раздел сканирования (ScanerFragment)
2. Отсканировать QR-код формата: `PAIR:true;socket:false;konf:1;LOGGING:0;`
3. Подтвердить сопряжение

**Expected Result:**
- `appConnect1C` установлен в `3`
- `FileExchangeManager` инициализирован с путём `/storage/emulated/0/Download/TSD/`
- Папка обмена создана (если не существует)
- В логах: `"Папка обмена создана: /storage/emulated/0/Download/TSD/"`
- На ТСД отображается, что устройство сопряжено

**Ключевой код:** `QrPairingParser.parse()`, `QrPairingParser.savePairing()`, `ScanerFragment.pairingWithFileMode()`

---

### TS-002: Сохранение настроек USB-режима в SharedPreferences
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | QR-код отсканирован (TS-001) |

**Steps:**
1. Проверить SharedPreferences после сопряжения
2. Проверить значения ключей

**Expected Result:**
- `connct1c = 3`
- `configuration = "1"`
- `logging = false`
- `use_socket = false`
- `license` сохранён из QR-кода

**Ключевой код:** `QrPairingParser.savePairing()`, SharedPreferences

---

## 2. Обмен данными ПК→ТСД (PC to TSD Data Transfer)

### TS-003: ПК создаёт Input.json в папке обмена
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | ТСД в USB-режиме (appConnect1C=3); папка `/storage/emulated/0/Download/TSD/` доступна |

**Steps:**
1. Сформировать JSON-данные для импорта:
   ```json
   {
     "oper": "1",
     "client": "КлиентА",
     "data": [
       {"Shtrih": "4607027387654", "ShtrihTip": 1, "TovNaim": "Товар A", "TovCena": 150.00, "TovKol": 10},
       {"Shtrih": "4607027387655", "ShtrihTip": 1, "TovNaim": "Товар B", "TovCena": 200.00, "TovKol": 5}
     ]
   }
   ```
2. Записать файл как `Input.json` в `/storage/emulated/0/Download/TSD/`
3. Проверить наличие файла

**Expected Result:**
- Файл `Input.json` создан в папке обмена
- Содержимое файла совпадает с отправленным JSON
- Размер файла > 0

---

### TS-004: ТСД обнаруживает Input.json и показывает уведомление
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | Input.json создан (TS-003); polling запущен |

**Steps:**
1. Дождаться цикла polling (до 5 сек)
2. Проверить MenuFragment

**Expected Result:**
- `FileExchangeManager.hasInputFile()` возвращает `true`
- `onInputFileReady` callback вызван
- MenuFragment показывает: "Загрузка: Данные доступны. Нажмите 'Загрузить'"
- Статус `input` установлен в `3`

**Ключевой код:** `FileExchangeManager.startPolling()`, `FileExchangeManager.hasInputFile()`, `MenuFragment.updateInputStatusUI()`

---

### TS-005: Импорт данных из Input.json в БД
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | Input.json найден (TS-004) |

**Steps:**
1. На ТСД в MenuFragment нажать кнопку "Загрузить" (`bInput`)
2. Дождаться завершения импорта

**Expected Result:**
- `FileExchangeManager.readInputAndImport()` выполняет:
  - Читает `Input.json`
  - Парсит JSON (data массив)
  - Очищает Room-таблицу (`dao.deleteAll()`)
  - Вставляет записи (`dao.insertList(items)`)
  - Извлекает `oper` и `client` из JSON
- `Input.json` удалён после импорта
- Статус `input` сброшен в `0`
- Статус `bd` обновлён: `2` (есть записи с quantity>0)
- В логах: `"Импорт завершен: 2 записей из Input.json"`
- Количество записей в БД соответствует количеству в JSON

**Ключевой код:** `FileExchangeManager.readInputAndImport()`, `FileExchangeManager.parseJsonData()`, `MenuFragment.onInput()`

---

### TS-006: Запись статуса после импорта (dev_status.txt)
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | Импорт завершён (TS-005) |

**Steps:**
1. Проверить файл `tsd_dev_status.txt` в папке обмена
2. Проверить содержимое

**Expected Result:**
- Файл `tsd_dev_status.txt` создан/обновлён
- Формат SSV: `True;1;2;0;0` (pairing;konf;bd;input;output)
- `pairing = true`
- `konf = 1`
- `bd = 2` (есть записи с quantity>0)
- `input = 0` (данные загружены)
- `output = 0` (нет для выгрузки)

**Ключевой код:** `FileExchangeManager.writeStatus()`, `DeviceStatus.toSsv()`

---

### TS-007: Полный цикл ПК→ТСД (создание файла → обнаружение → импорт)
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | ТСД в USB-режиме; база данных пуста |

**Steps:**
1. ПК создаёт `Input.json` с 5 товарами
2. ТСД обнаруживает файл (polling)
3. Оператор на ТСД нажимает "Загрузить"
4. Проверить БД и статусы

**Expected Result:**
- Шаг 2: `input=3`, уведомление на ТСД
- Шаг 3: 5 записей в БД, `Input.json` удалён
- Шаг 4: `bd=2`, `input=0`, `tsd_dev_status.txt` обновлён

---

## 3. Обмен данными ТСД→ПК (TSD to PC Data Transfer)

### TS-008: ТСД формирует Output.json для выгрузки
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | В БД ТСД есть данные (≥1 запись с `quantity > 0`) |

**Steps:**
1. На ТСД отсканировать штрихкод товара (найденного в БД) и ввести количество → `quantity > 0`
2. На ТСД в MenuFragment нажать кнопку "Выгрузить" (`bOutput`)
3. Дождаться формирования `Output.json`

**Expected Result:**
- `FileExchangeManager.writeOutputAndExport()` формирует `Output.json`
- Файл содержит JSON с полями: `oper`, `client`, `data` (массив товаров)
- Формат данных: `Shtrih`, `ShtrihTip`, `TovNaim`, `TovCena`, `TovKol`
- `Output.json` записан в `/storage/emulated/0/Download/TSD/`
- Статус `output` установлен в `3`
- `tsd_dev_status.txt` обновлён: `output=3`

**Ключевой код:** `FileExchangeManager.writeOutputAndExport()`, `FileExchangeManager.buildExportJson()`

---

### TS-009: ПК читает Output.json
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | Output.json создан (TS-008) |

**Steps:**
1. ПК проверяет наличие `Output.json` в папке обмена
2. Читает и парсит JSON
3. Проверяет содержимое

**Expected Result:**
- Файл `Output.json` существует, размер > 0
- JSON содержит все записи из БД ТСД
- Формат соответствует спецификации (Shtrih, ShtrihTip, TovNaim, TovCena, TovKol)
- `oper` и `client` извлечены корректно

---

### TS-010: ПК обновляет dev_status.txt (output=0)
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | ПК получил данные (TS-009) |

**Steps:**
1. ПК записывает `tsd_dev_status.txt` с `output=0`
2. Формат SSV: `True;1;3;0;0`

**Expected Result:**
- Файл `tsd_dev_status.txt` обновлён
- `output = 0` (данные обработаны ПК)
- На ТСД: при следующем polling `output` установлен в `0`
- UI обновлён: "Выгрузка: Отсутствуют записи для выгрузки"

---

### TS-011: Полный цикл ТСД→ПК (формирование → чтение → подтверждение)
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | В БД ТСД есть данные; ТСД в USB-режиме |

**Steps:**
1. На ТСД нажать "Выгрузить"
2. ПК читает `Output.json`
3. ПК обновляет `tsd_dev_status.txt` с `output=0`
4. ТСД polling обнаруживает `output=0`

**Expected Result:**
- Шаг 1: `Output.json` создан, `output=3`
- Шаг 2: ПК получает полный JSON
- Шаг 3: `tsd_dev_status.txt` обновлён
- Шаг 4: ТСД видит `output=0`, UI обновлён

---

### TS-012: Выгрузка при пустой БД
| Attribute | Value |
|-----------|-------|
| **Priority** | P1 |
| **Precondition** | В БД ТСД нет записей или нет записей с `quantity > 0` |

**Steps:**
1. На ТСД нажать "Выгрузить"
2. Проверить результат

**Expected Result:**
- `FileExchangeManager.writeOutputAndExport()` возвращает ошибку: `"Нет данных для выгрузки"`
- `Output.json` не создаётся
- Статус `output` остаётся `0`
- На ТСД отображается Toast: "Нет данных для выгрузки"

---

## 4. Жизненный цикл (Lifecycle)

### TS-013: Запуск приложения в USB-режиме
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | Сохранённые настройки: `appConnect1C=3`, QR-код отсканирован ранее |

**Steps:**
1. Запустить приложение
2. Проверить, что `appConnect1C == 3` (из сохранённых настроек)
3. Наблюдать за инициализацией

**Expected Result:**
- `appConnect1C` установлен в `3`
- `FileExchangeManager` инициализирован
- Папка обмена проверена/создана
- Состояния `bd`, `input`, `output` восстановлены из `tsd_dev_status.txt`
- Polling папки запущен (5 сек)

**Ключевой код:** `MainActivity.onCreate()`, `FileExchangeManager.startPolling()`

---

### TS-014: Восстановление статусов из dev_status.txt
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | `tsd_dev_status.txt` существует с данными |

**Steps:**
1. Записать в `tsd_dev_status.txt`: `True;1;3;3;0`
2. Запустить приложение
3. Проверить восстановление

**Expected Result:**
- `FileExchangeManager.readStatus()` читает файл
- `pairing = true`, `konf = 1`, `bd = 3`, `input = 3`, `output = 0`
- UI отображает восстановленные значения

**Ключевой код:** `FileExchangeManager.readStatus()`, `DeviceStatus.fromSsv()`

---

### TS-015: Сброс сопряжения в USB-режиме
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | ТСД в USB-режиме |

**Steps:**
1. На LogoFragment нажать кнопку сброса сопряжения (`bLogoPairing`)
2. Проверить состояние

**Expected Result:**
- SharedPreferences: `appConnect1C=0`, `license=-1`
- `appLic.appConnect1C = 0`
- Приложение перезапускается
- Переход к экрану сопряжения (ScanerFragment)
- Файлы обмена (`Input.json`, `Output.json`, `tsd_dev_status.txt`) НЕ удаляются (остаются для обработки)

**Ключевой код:** `LogoFragment.bLogoPairing`, `MainActivity.resetPairingState()`

---

### TS-016: Переключение режима (USB → WIFI)
| Attribute | Value |
|-----------|-------|
| **Priority** | P1 |
| **Precondition** | ТСД в USB-режиме |

**Steps:**
1. На LogoFragment переключить режим на "Локальный WIFI"
2. Проверить состояние

**Expected Result:**
- `USE_WEBSITE = false` (локальный режим)
- Все настройки сопряжения сброшены (`appConnect1C=0`)
- `appLic.appLIC = "-1"`, `appLic.appConnect1C = 0`
- Навигация к LogoFragment для обновления UI

---

### TS-017: Закрытие приложения
| Attribute | Value |
|-----------|-------|
| **Priority** | P1 |
| **Precondition** | Приложение запущено |

**Steps:**
1. `MainActivity.onDestroy()` вызывается
2. `appExit()` выполняется

**Expected Result:**
- `serverSocket?.close()` — серверный сокет закрыт (если был)
- `connectionSocket?.close()` — клиентский сокет закрыт (если был)
- `exitProcess(-1)` — процесс завершён
- Файлы обмена (`Input.json`, `Output.json`, `tsd_dev_status.txt`) сохранены

**Ключевой код:** `MainActivity.appExit()`, `MainActivity.onDestroy()`

---

## 5. UI и состояния (UI & States)

### TS-018: MenuFragment — отображение статусов в USB-режиме
| Attribute | Value |
|-----------|-------|
| **Priority** | P0 |
| **Precondition** | ТСД в USB-режиме; polling запущен |

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
- `textViewOutput`:
  - `output=0`: серый, "Выгрузка: Отсутствуют записи для выгрузки"
  - `output=3`: зелёный, "Выгрузка: Данные отправлены"

**Ключевой код:** `MenuFragment.updateBDStatus()`, `MenuFragment.updateInputStatusUI()`, `MenuFragment.updateOutputStatusUI()`

---

### TS-019: LiveData-синхронизация
| Attribute | Value |
|-----------|-------|
| **Priority** | P1 |
| **Precondition** | ТСД в USB-режиме |

**Steps:**
1. Подписаться на LiveData в MenuFragment (`infoLiveData()`)
2. Изменить статус через polling или вручную
3. Проверить обновление UI

**Expected Result:**
- `appInfoBD` → `updateBDStatus()` — перерисовка textViewBD
- `appInfoINPUT` → `updateInputStatusUI()` — перерисовка textViewInput
- `appInfoOUT` → `updateOutputStatusUI()` — перерисовка textViewOutput
- Нет дублирующих или пропущенных обновлений

**Ключевой код:** `MenuFragment.infoLiveData()`, `LicenseUtil` LiveData

---

### TS-020: Корректное обновление bd-статуса
| Attribute | Value |
|-----------|-------|
| **Priority** | P1 |
| **Precondition** | ТСД в USB-режиме |

**Steps:**
1. Начальное состояние: БД пуста (`bd=0`)
2. ПК загружает данные → импортирует в БД
3. Проверить `bd` после импорта
4. Сформировать выгрузку → удалить данные из БД
5. Проверить `bd` после выгрузки

**Expected Result:**
- Шаг 1: `bd=0` (пустая БД)
- Шаг 3: `bd=2` (есть записи для выгрузки)
- Шаг 5: `bd=3` (загружена, нет quantity>0)
- Значения `bd` синхронизированы между Room-БД и `tsd_dev_status.txt`

---

### TS-021: Оператор и клиент из Input.json
| Attribute | Value |
|-----------|-------|
| **Priority** | P1 |
| **Precondition** | Данные получены с ПК |

**Steps:**
1. ПК создаёт `Input.json` с `oper="1"`, `client="КлиентА"`
2. ТСД импортирует данные
3. `FileExchangeManager.readInputAndImport()` извлекает oper/client

**Expected Result:**
- `oper = "1"`, `client = "КлиентА"`
- `textViewOperInfo` обновлён: "Приход.\nПоставщик: КлиентА"

**Ключевой код:** `FileExchangeManager.readInputAndImport()`, `MenuFragment.updateOperInfo()`

---

### TS-022: Формат Input.json (импорт с ПК)
| Attribute | Value |
|-----------|-------|
| **Priority** | P1 |
| **Precondition** | Данные отправлены ПК |

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
- `FileExchangeManager.parseJsonData()` успешно парсит JSON
- Поля маппинга: `Shtrih→itemSh`, `ShtrihTip→itemShTip`, `TovNaim→itemName`, `TovCena→itemPrice`, `TovKol→itemQuantity`

---

### TS-023: Формат Output.json (экспорт на ПК)
| Attribute | Value |
|-----------|-------|
| **Priority** | P1 |
| **Precondition** | В БД есть данные с `quantity > 0` |

**Expected Result:**
- `FileExchangeManager.buildExportJson()` создаёт JSON
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
- `escapeJson()` корректно экранирует спецсимволы

---

## Сводная таблица тестов

| № | Название | Категория | Приоритет |
|---|----------|-----------|-----------|
| TS-001 | Сканирование QR-кода и вход в USB-режим | Сопряжение | **P0** |
| TS-002 | Сохранение настроек USB-режима | Сопряжение | **P0** |
| TS-003 | ПК создаёт Input.json | ПК→ТСД | **P0** |
| TS-004 | ТСД обнаруживает Input.json | ПК→ТСД | **P0** |
| TS-005 | Импорт данных из Input.json в БД | ПК→ТСД | **P0** |
| TS-006 | Запись статуса после импорта | ПК→ТСД | **P0** |
| TS-007 | Полный цикл ПК→ТСД | ПК→ТСД | **P0** |
| TS-008 | ТСД формирует Output.json | ТСД→ПК | **P0** |
| TS-009 | ПК читает Output.json | ТСД→ПК | **P0** |
| TS-010 | ПК обновляет dev_status.txt | ТСД→ПК | **P0** |
| TS-011 | Полный цикл ТСД→ПК | ТСД→ПК | **P0** |
| TS-012 | Выгрузка при пустой БД | ТСД→ПК | P1 |
| TS-013 | Запуск приложения в USB-режиме | Жизненный цикл | **P0** |
| TS-014 | Восстановление статусов из dev_status.txt | Жизненный цикл | **P0** |
| TS-015 | Сброс сопряжения в USB-режиме | Жизненный цикл | **P0** |
| TS-016 | Переключение режима (USB → WIFI) | Жизненный цикл | P1 |
| TS-017 | Закрытие приложения | Жизненный цикл | P1 |
| TS-018 | Отображение статусов в UI | UI и состояния | **P0** |
| TS-019 | LiveData-синхронизация | UI и состояния | P1 |
| TS-020 | Корректное обновление bd-статуса | UI и состояния | P1 |
| TS-021 | Оператор и клиент из Input.json | UI и состояния | P1 |
| TS-022 | Формат Input.json | JSON форматы | P1 |
| TS-023 | Формат Output.json | JSON форматы | P1 |

---

## Ключевые точки проверки (Checklist)

### Файлы обмена
- [ ] Папка: `/storage/emulated/0/Download/TSD/`
- [ ] `Input.json` создаётся ПК
- [ ] `Input.json` удаляется после импорта ТСД
- [ ] `Output.json` создаётся ТСД
- [ ] `Output.json` читается ПК
- [ ] `tsd_dev_status.txt` обновляется при каждом изменении статуса

### Формат dev_status.txt (SSV)
- [ ] Формат: `pairing;konf;bd;input;output`
- [ ] `pairing = True/False`
- [ ] `konf` — номер конфигурации
- [ ] `bd` — 0/2/3
- [ ] `input` — 0/3
- [ ] `output` — 0/3

### Формат Input.json
- [ ] `oper` — строка
- [ ] `client` — строка
- [ ] `data[]` — массив объектов
- [ ] Поля: `Shtrih`, `ShtrihTip`, `TovNaim`, `TovCena`, `TovKol`

### Формат Output.json
- [ ] `oper` — строка
- [ ] `client` — строка
- [ ] `data[]` — массив объектов
- [ ] Поля: `Shtrih`, `ShtrihTip`, `TovNaim`, `TovCena`, `TovKol`
- [ ] Спецсимволы экранированы

### Состояния (LicenseUtil LiveData)
- [ ] `appInfoBD` — корректно обновляется
- [ ] `appInfoINPUT` — корректно обновляется
- [ ] `appInfoOUT` — корректно обновляется
- [ ] `appConnect1C == 3` в USB-режиме

### Polling папки
- [ ] Интервал 5 секунд
- [ ] `hasInputFile()` обнаруживает `Input.json`
- [ ] `hasOutputFile()` обнаруживает `Output.json`
- [ ] Корректная остановка через `stopPolling()`

### Состояния (bd, input, output)
- [ ] `bd=0` — пустая БД
- [ ] `bd=2` — есть записи с quantity>0
- [ ] `bd=3` — загружена, нет quantity>0
- [ ] `input=0` — нет данных
- [ ] `input=3` — есть данные
- [ ] `output=0` — нет для выгрузки
- [ ] `output=3` — отправлены

---

*Последнее обновление: 2026-09-10*
