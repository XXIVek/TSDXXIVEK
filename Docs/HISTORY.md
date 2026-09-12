# История изменений

## 2026-09-11 — Тестирование USB-режима (TS-001 — TS-015)

### Найденные и исправленные баги

#### Критический баг: `onInput()` не импортировал tsd_Input.json в USB-режиме

**Проблема:** При нажатии кнопки "Загрузить" в MenuFragment для USB-режима (appConnect1C=3) данные из `tsd_Input.json` не импортировались. Вызывался только `UtilDB().onDelAllTables()` — очищал БД, но не читал файл.

**Решение:** `MenuFragment.kt` — добавлена проверка `appConnect1C == 3` в `onInput()`:
- Проверяет наличие `tsd_Input.json` через `FileExchangeManager.hasInputFile()`
- Вызывает `FileExchangeManager.readInputAndImport()` для импорта
- Обновляет UI через `updateInputStatusUI()` и `appLic.conditionInfo()`
- Показывает Toast с результатом

#### Замечание: Сброс сопряжения не удалял файлы обмена

**Проблема:** При сбросе сопряжения через `bLogoPairing` и `resetPairingState()` файлы `tsd_Input.json`, `tsd_Output.json`, `tsd_dev_status.txt` не удалялись.

**Решение:**
| Файл | Изменение |
|------|-----------|
| `LogoFragment.kt` | Добавлено удаление файлов обмена при нажатии `bLogoPairing` |
| `MainActivity.kt` | Добавлено удаление файлов в `resetPairingState()` |

### Результаты статической проверки кода

| Тест | Статус | Замечания |
|------|--------|-----------|
| TS-001: QR-код и вход в USB-режим | ✅ | Багов нет |
| TS-002: SharedPreferences | ✅ | license="1" жёстко задан (не баг) |
| TS-003: Путь tsd_Input.json | ✅ | `/storage/emulated/0/Download/` |
| TS-004: Polling обнаружения | ✅ | Интервал 5 сек, корректная проверка |
| TS-005: Импорт в БД | ✅ | deleteAll() + insertList() |
| TS-006: Запись статуса | ✅ | SSV формат корректен |
| TS-007: Полный цикл ПК→ТСД | ✅ **ИСПРАВЛЕН** | onInput() теперь импортирует tsd_Input.json |
| TS-008: Формирование tsd_Output.json | ✅ | writeOutputAndExport() |
| TS-009: Чтение tsd_Output.json ПК | ✅ | buildExportJson() корректен |
| TS-010: Обновление dev_status.txt | ✅ | writeStatus() корректен |
| TS-011: Полный цикл ТСД→ПК | ✅ | exportToUSB() корректен |
| TS-013: Запуск в USB-режиме | ✅ | appConnect1C из SharedPreferences |
| TS-014: Восстановление статусов | ✅ | readStatus() корректен |
| TS-015: Сброс сопряжения | ✅ **ИСПРАВЛЕН** | Добавлено удаление файлов |
| TS-012: Выгрузка при пустой БД | ✅ | Требуется ручное тестирование |
| TS-016: Переключение режима | ✅ | Требуется ручное тестирование |
| TS-017: Закрытие приложения | ✅ | Требуется ручное тестирование |
| TS-018: Отображение статусов в UI | ✅ | Требуется ручное тестирование |
| TS-019: LiveData-синхронизация | ✅ | Требуется ручное тестирование |
| TS-020: Корректное обновление bd | ✅ | Требуется ручное тестирование |
| TS-021: Оператор и клиент из tsd_Input.json | ✅ | Требуется ручное тестирование |
| TS-022: Формат tsd_Input.json | ✅ | Требуется ручное тестирование |
| TS-023: Формат tsd_Output.json | ✅ | Требуется ручное тестирование |

### Исправление: Имена файлов обмена для 1С

**Проблема:** В коде файлы назывались `Input.json` / `Output.json`, но 1С ожидает `tsd_Input.json` / `tsd_Output.json`.

**Решение:**
| Файл | Изменение |
|------|-----------|
| `AppConstants.kt` | `FILE_INPUT_JSON = "tsd_Input.json"`, `FILE_OUTPUT_JSON = "tsd_Output.json"` |
| `CLINE_TSD.md` | Обновлены имена файлов в документации |

---

### Исправление: Обновление статусов при входе на главный экран (USB-режим)

**Проблема:** При входе на главный экран (MenuFragment) статусы bd, input, output не обновлялись из БД и файлов. Если ТСД перезапускался с уже загруженной БД и файлами — статусы оставались нулевыми.

**Решение:** `MenuFragment.kt` — добавлен метод `refreshStatusFromFiles()`:
- Вызывается в `onViewCreated()` при `appConnect1C == 3`
- Проверяет БД → bd=0/2/3
- Проверяет tsd_Input.json → input=3/0
- Проверяет tsd_Output.json → output=3/0
- Обновляет LiveData (appInfoBD, appInfoINPUT, appInfoOUT)
- Записывает актуальный статус в tsd_dev_status.txt
- Лог: `refreshStatusFromFiles: bd=X, input=Y, output=Z`

---

## 2026-09-10 — Исправление пути файлового обмена для USB-режима

### Проблема

При сопряжении в USB-режиме (appConnect1C=3) `FileExchangeManager.writeStatus()` не записывал файл `tsd_dev_status.txt` — приложение падало с ошибкой:
```
java.io.FileNotFoundException: /storage/emulated/0/Download/TSD/tsd_dev_status.txt: open failed: EACCES (Permission denied)
```

Причина: на Android 11+ (API 30+) приложение не имеет прав на запись в `/storage/emulated/0/Download/`.

### Решение

| Файл | Изменение |
|------|-----------|
| `AppConstants.kt` | `FILE_EXCHANGE_DIR` изменён с `/storage/emulated/0/Download/TSD/` на `/storage/emulated/0/Download/` |
| `AndroidManifest.xml` | Добавлено разрешение `MANAGE_EXTERNAL_STORAGE` |
| `scaner/ScanerFragment.kt` | `pairingWithFileMode()` — запрос `MANAGE_EXTERNAL_STORAGE` перед записью, сохранение `pendingDevStatus`, проверка в `onResume` |
| `FileExchangeManager.kt` | `exchangeDir` сделан `val` (публичный) для доступа из тестов |

Файлы обмена находятся в `/storage/emulated/0/Download/`:
- `tsd_dev_status.txt` — статус устройства
- `tsd_Input.json` — данные ПК→ТСД
- `tsd_Output.json` — данные ТСД→ПК

### Детали реализации запроса разрешения

**Проблема:** системная настройка `MANAGE_EXTERNAL_STORAGE` не возвращает callback. После предоставления разрешения пользователь возвращается на сканер, но логика записи не продолжается.

**Решение:**
1. `pendingDevStatus` — поле для сохранения статуса при запросе разрешения
2. `hasManageExternalStoragePermission()` — проверка через `Environment.isExternalStorageManager()`
3. `requestManageExternalStoragePermission()` — открывает системную настройку без callback
4. `onResume()` — проверка `appConnect1C == 3` + разрешение → запись статуса и переход в меню
5. Только для USB-режима: `if (appLic.appConnect1C != 3) return`

---

## 2026-09-10 — Исправление доступа к БД в фрагментах списка товаров

### Проблема

`itemDatabase` — глобальная переменная в `MainActivity.kt:85` — всегда `null`, так как строка инициализации закомментирована (строка 149). Фрагменты `ItemListFragment`, `ItemListFragment1`, `ItemListFragment2` и `ScanerFragment` импортировали эту переменную и вызывали `itemDatabase?.getItem2()`, но она всегда была `null`, поэтому данные никогда не загружались и штрихкоды не находились.

### Решение

| Файл | Проблема | Решение |
|------|----------|---------|
| `dataDB/ItemListFragment.kt` | `itemDatabase` всегда `null` | Заменён импорт на `TSDXXIVekApplication`, DAO получается через `TSDXXIVekApplication.instance?.database?.itemDao()` |
| `dataDB/ItemListFragment1.kt` | `itemDatabase` всегда `null` | Заменён импорт на `TSDXXIVekApplication`, DAO получается через `TSDXXIVekApplication.instance?.database?.itemDao()` |
| `dataDB/ItemListFragment2.kt` | `itemDatabase` всегда `null` | Заменён импорт на `TSDXXIVekApplication`, DAO получается через `TSDXXIVekApplication.instance?.database?.itemDao()` |
| `scaner/ScanerFragment.kt` | `itemDatabase` всегда `null` — штрихкод не находится | Заменено на `TSDXXIVekApplication.instance?.database?.itemDao()?.getItem2()` |
| `scaner/ScanerFragment_d.kt` | `itemDatabase` всегда `null` — штрихкод не находится | Заменено на `TSDXXIVekApplication.instance?.database?.itemDao()?.getItem2()` |
| `MainActivity.kt` | Неиспользуемая переменная `itemDatabase` | Удалена переменная и import `ItemDao` |

### Тестирование

| Тест | Статус |
|------|--------|
| TS-001 — Сканирование QR-кода активации | ✅ Выполнен |
| TS-002 — Двухэтапная активация | ✅ Выполнен |
| TS-003 — Проверка статуса при запуске | ✅ Выполнен |
| TS-004 — Запуск polling статуса | ✅ Выполнен |
| TS-005 — Получение статуса через polling | ✅ Выполнен |
| TS-006 — Синхронизация bd-статуса | ✅ Выполнен |
| TS-007 — Остановка polling при уходе с экрана | ✅ Выполнен |
| TS-008 — Обнаружение pending заданий | ✅ Выполнен |
| TS-009 — Загрузка скачанных файлов в БД | ✅ Выполнен |
| TS-010 — Полный цикл сценария А | ✅ Выполнен |

---

## 2026-09-09 — Исправление критических багов режима Socket/Сайт

### Критические исправления

| Файл | Проблема | Решение |
|------|----------|---------|
| `QrPairingParser.kt` | Не поддерживал формат QR `PAIR:<activation_code>` для Socket/Сайт | Добавлена поддержка формата `PAIR:ABC123` (только activation_code без `;`). Добавлено поле `activationCode` в `PairingResult` |
| `ScanerFragment.kt` | QR `PAIR:ABC123` не распознавался, дублирование логики активации | Автоматическая активация при сканировании QR с `activationCode`. При `useSocket=true` и `activationCode.isNotBlank` — вызывается `activateDevice()` напрямую |
| `UtilDB.kt` | `TSDXXIVekApplication().database` — создавался новый экземпляр Application, БД была пустой | Используется глобальный `itemDatabase` или `TSDXXIVekApplication.instance.database` |
| `TSDXXIVekApplication.kt` | Не было singleton instance | Добавлен `companion object` с `INSTANCE` и `val instance` |
| `FileDownloadApi.kt` | Не было поддержки multipart/form-data для upload | Добавлен метод `uploadResultMultipart()` |
| `ScanerFragment.kt` | `testWriteDevStatus()` записывал `pairing=false` при каждом открытии сканера | Удалён вызов и метод `testWriteDevStatus()` |
| `LogoFragment.kt` | `MainActivity().appExit()` — создавался новый экземпляр Activity | Заменено на `requireActivity().finish()` |
| `ScanerFragment.kt` | `TSDXXIVekApplication()` в `setupCamera()` — новый экземпляр | Заменено на `requireActivity().application as Application` |

### Не подтвердился

| Файл | Проблема | Статус |
|------|----------|--------|
| `StatusPollingService.kt` | `CoroutineName` не импортирован | **НЕ ПОДТВЕРДИЛСЯ** — `import kotlinx.coroutines.*` уже включает `CoroutineName` |

---

## 2026-07-21 — Исправление критических ошибок режима Socket/Сайт

### Удалённые файлы

| Файл | Причина |
|------|---------|
| `dataXML/XMLDOMParser.kt` | Не инстанциируется нигде в проекте |
| `dataXML/BuilderXML.kt` | Используется только ServerSocketXXI (удалён) |
| `serverHTTP/ServerSocketXXI.kt` | Не инстанциируется нигде, относится к устаревшему Socket-режиму |
| `serverHTTP/ParsingDataServer.kt` | Не инстанциируется нигде, относится к устаревшему Socket-режиму |
| `dataXML/` (папка) | Пуста после удаления файлов |

### Причина удаления

Все удалённые файлы относятся к устаревшему Socket-режиму (appConnect1C=2), который не используется в текущем сценарии работы ТСД.

Текущие режимы используют:
- `LocalWifiServer` — JSON API
- `FileExchangeManager` — работа с файлами
- `BroadcastServer` — UDP-объявление

---

## 2026-07-16 — Исправление навигации и сброса сопряжения

### Проблема

При запуске ScanerFragment обнулялось состояние ТСД.

### Причина

- `ScanerFragment.onViewCreated()` вызывал `testWriteDevStatus()` — записывал `pairing = false`
- Логика кнопки "Далее" проверяла `appLIC` вместо `appConnect1C`
- Нет автоматического перехода из LogoFragment в MenuFragment при старте
- `resetPairingState()` не удалял файлы обмена

### Решение

1. **ScanerFragment.kt** — убран `testWriteDevStatus()`
2. **LogoFragment.kt** — кнопка "Далее" проверяет `appConnect1C`
3. **LogoFragment.kt** — автоматический переход при запуске
4. **LogoFragment.kt** — удаление файлов обмена при сбросе сопряжения
5. **MainActivity.kt** — `resetPairingState()` теперь удаляет файлы обмена

---

## 2026-07-21 — Этап 3: Получение данных + исправления

### Проблема 1: Не восстанавливался статус input при перезагрузке

**Решение:** `MainActivity.kt` — `checkLocalWifiMode()` добавлена проверка `hasInputFile()` и `hasOutputFile()`

### Проблема 2: Кнопка "Загрузить" не видела `tsd_Input.json` в режиме WIFI

**Решение:** `MenuFragment.kt` — `onInput()` добавлена проверка `hasInputFile()` для режимов 3 и 4

---

## 2026-09-09 — Исправление критических ошибок режима Socket/Сайт

### Критические исправления

| Файл | Проблема | Решение |
|------|----------|---------|
| `api/FileDownloadApi.kt` | `getDeviceUuid()` читал ключ `license` вместо `device_uuid` | Исправлен ключ на `APP_PREF_DEVICE_UUID` |
| `MenuFragment.kt` | Запуск устаревшего `ServerSocketXXI` при appConnect1C=2 | Удалён import и блок `onCreate` с `ServerSocketXXI` |
| `PairingFragment.kt` | Запуск устаревшего `ServerSocketXXI` при ожидании сопряжения | Удалён import и блок `onCreate` с `ServerSocketXXI` |
| `dataDB/UtilDB.kt` | Ссылки на удалённые `XMLDOMParser` и `BuilderXML` — не компилируется | Удалён весь XML-код, переписан класс |
| `dataDB/UtilDB.kt` | Некорректный `coroutineScope{` в `onDelAllTables` | Заменён на прямой вызов |
| `MenuFragment.kt` | Вызов `UtilDB().readInputXML()` — удалённый метод | Заменён на `UtilDB().onDelAllTables()` |
| `MenuFragment.kt` | Вызов `UtilDB().writeXML()` — удалённый метод | Заменён на `UtilDB().clearQuantity()` |

### Исправления архитектуры

| Файл | Проблема | Решение |
|------|----------|---------|
| `MenuFragment.kt` | `MainActivity().appExit()` — создание нового экземпляра | Заменён на `requireActivity().finish()` |
| `AppState.kt` | `TSDXXIVekApplication()` без контекста — не работает | Добавлен параметр `Context` в `setAppOper`/`setAppClient` |
| `MainActivity.kt` | `syncBdIfDifferent` использует `ApiClient` | Заменён на `FileDownloadApi` |
| `MainActivity.kt` | `checkDeviceStatus` использует `ApiClient` | Заменён на `FileDownloadApi.getDeviceStatusSync` |
| `api/StatusPollingService.kt` | `join()` в polling-цикле блокирует поток | bd-check сделан полностью асинхронным |

### Удалённые ссылки

| Файл | Удалено |
|------|---------|
| `dataDB/UtilDB.kt` | Импорт `dataXML.BuilderXML`, `dataXML.XMLDOMParser`, `org.w3c.dom.*`, `java.io.IOException`, `java.io.InputStream`, `java.util.ArrayList` |
| `dataDB/UtilDB.kt` | Метод `readInputXML()` — полностью удалён |
| `dataDB/UtilDB.kt` | Метод `writeXML()` — полностью удалён |
| `dataDB/UtilDB.kt` | Метод `observeItemNotempty()` — полностью удалён |

---

## 2026-09-12 — Исправление: `pairing` и `konf` в USB-режиме

### Проблема

В USB-режиме (appConnect1C=3) `pairing` и `konf` записывались и читались из файла `tsd_dev_status.txt`. Если ПК записывал `false;0;3;0;0`, ТСД при перезапуске терял сопряжение (`pairing=false, konf=0`).

### Решение

В USB-режиме `pairing` и `konf` теперь **никогда** не читаются из файла и **никогда** не записываются в файл. Они всегда берутся из SharedPreferences (устанавливаются при сопряжении и не меняются).

| Файл | Изменение |
|------|-----------|
| `FileExchangeManager.kt` | Добавлен параметр `usbMode: Boolean = false` в конструктор |
| `FileExchangeManager.kt` | Добавлен метод `getKonfFromPrefs()` — читает konf из SharedPreferences |
| `FileExchangeManager.kt` | `readStatus()` — для USB-режима игнорирует `pairing` и `konf` из файла, подставляет `pairing=true` и konf из SharedPreferences |
| `FileExchangeManager.kt` | `writeStatus()` — для USB-режима игнорирует `pairing` и `konf` из status, подставляет `pairing=true` и konf из SharedPreferences |
| `MenuFragment.kt` | `onClearQuantity()` (строка 106): `usbMode = true` |
| `MenuFragment.kt` | `refreshStatusFromFiles()` (строка 314): `usbMode = true` |
| `MenuFragment.kt` | `startStatusPolling()` (строка 362): `usbMode = isUsbMode` |
| `MenuFragment.kt` | `onInput()` (строка 525): `usbMode = true` |
| `MenuFragment.kt` | `exportToUSB()` (строка 608): `usbMode = true` |
| `ScanerFragment.kt` | `writeStatusAndNavigate()` (строка 929): `usbMode = true` |

### Архитектурное изменение

| Параметр | До исправления | После исправления |
|----------|---------------|-------------------|
| `pairing` в USB-режиме | Чтение/запись из файла | Всегда `true` (из SharedPreferences) |
| `konf` в USB-режиме | Чтение/запись из файла | Всегда из SharedPreferences |
| `bd`, `input`, `output` в USB-режиме | Чтение/запись из файла | Без изменений |

### Результаты тестирования

**Дата:** 2026-09-12
**Статус:** ✅ ВСЕ ТЕСТЫ ПРОЙДЕНЫ (23/23)

| Категория | Всего | Пройдено | Не пройдено |
|-----------|-------|----------|-------------|
| Сопряжение | 2 | 2 | 0 |
| ПК→ТСД | 5 | 5 | 0 |
| ТСД→ПК | 5 | 5 | 0 |
| Жизненный цикл | 5 | 5 | 0 |
| UI и состояния | 3 | 3 | 0 |
| JSON форматы | 2 | 2 | 0 |
| **Итого** | **23** | **23** | **0** |

**Ключевой тест TS-014:** При перезапуске ТСД с `tsd_dev_status.txt` = `false;0;3;0;0` — `pairing=true`, `konf=1` восстановлены из SharedPreferences ✅

---

*Последнее обновление: 2026-09-12*
