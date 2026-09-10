# История изменений

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

*Последнее обновление: 2026-09-09*
