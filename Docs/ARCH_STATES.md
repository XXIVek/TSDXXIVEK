# TSDXXIVEK — Система анализа состояний ТСД

## 1. Три основных статуса (bd, input, output)

### bd — состояние базы данных на ТСД

| Значение | Цвет на UI | Текст | Описание |
|----------|-----------|-------|----------|
| `0` | Серый | БД: В базе данных нет записей | База пуста |
| `1` | Красный | — | Ошибка работы с БД |
| `2` | Жёлтый | БД: Всего N зап., из них выб. M | База загружена, есть записи для выгрузки (quantityInStock > 0) |
| `3` | Зелёный | БД: Всего N зап. | База загружена, данных для выгрузки нет |
| `-1` | — | — | Идёт загрузка данных из JSON |

**Логика определения bd:**
- Считается **фактически** по количеству записей в Room БД
- `notEmpty > 0` → `bd = 2` (есть что выгружать)
- `total > 0 && notEmpty == 0` → `bd = 3` (база полная, выгружать нечего)
- `total == 0` → `bd = 0` (база пуста)

### input — состояние входящих данных (ПК → ТСД)

| Значение | Цвет на UI | Текст | Описание |
|----------|-----------|-------|----------|
| `0` | Серый | Загрузка: Нет данных для загрузки | Нет данных с сервера/ПК |
| `1` | Красный | — | Ошибка обработки входящих данных |
| `3` | Жёлтый | Загрузка: Данные доступны. Нажмите 'Загрузить' | Есть данные для импорта в БД |
| `6` | — | — | **Pending** — сервер/ПК уведомил о наличии файлов |

### output — состояние исходящих данных (ТСД → ПК)

| Значение | Цвет на UI | Текст | Описание |
|----------|-----------|-------|----------|
| `0` | Серый | Выгрузка: Отсутствуют записи для выгрузки | Нет данных для отправки |
| `1` | Красный | — | Ошибка выгрузки |
| `2` | — | — | Не определено (начальное) |
| `3` | Зелёный | Выгрузка: Данные отправлены | Успешно отправлено на сервер/ПК |

---

## 2. Где хранятся состояния

### AppState.kt — глобальное состояние (режим Socket/Сайт)

```kotlin
class AppState {
    var appInputStatus: Int = 0      // input
    var appOutputStatus: Int = 0     // output
    var appBd: Int = 0               // bd
    var hasPendingData: Boolean = false  // input == 6?
}
```

- Загружается из `SharedPreferences` при старте через `updateFromPrefs()`
- Обновляется сервером через `updateServerStatus(input, output)`
- Сбрасывается через `resetStatus()`

### LicenseUtil.kt — ViewModel (LiveData) для UI

```kotlin
class LicenseUtil : ViewModel() {
    var appInfoBD = MutableLiveData<Int>()      // bd
    var appInfoINPUT = MutableLiveData<Int>()   // input
    var appInfoOUT = MutableLiveData<Int>()     // output
    var appInfoCountBD = MutableLiveData<Int>()  // кол-во записей
    var appInfoCountNotEmptyBD = MutableLiveData<Int>()
}
```

- Используется через `observe()` в фрагментах для обновления UI
- Метод `conditionInfo()` пересчитывает `appInfoBD` по данным БД:
  ```kotlin
  if (count > 0) {
      appInfoBD.postValue(3)           // база есть
      if (countNotEmpty > 0) {
          appInfoBD.postValue(2)       // есть что выгружать
      }
  } else {
      appInfoBD.postValue(0)           // база пуста
  }
  ```

### FileExchangeManager.kt — статус для USB/WIFI режимов

```kotlin
data class DeviceStatus(
    val pairing: Boolean,
    val konf: Int,
    var bd: Int,
    var input: Int,
    var output: Int
)
```

- Хранится в `currentStatus` (private field)
- Записывается/читается через файл `tsd_dev_status.txt` в формате SSV:
  ```
  true;1;3;0;0
  pairing;konf;bd;input;output
  ```

---

## 3. Источники обновления статусов

Система имеет **два параллельных канала** получения статусов:

### Канал А — HTTP API (режим Socket/Сайт, appConnect1C=2)

```
StatusPollingService → каждые 5 сек → GET /api/v1/devices/status
                                              ↓
                                 Парсинг JSON ответа
                                              ↓
                    appState.updateServerStatus(input, output)
                                              ↓
                        Callback.onStatusUpdated() в MenuFragment
                                              ↓
                              Обновление UI на экране
```

**Цикл обработки input=6 (pending файлы):**

1. Polling получает `input == 6` → `hasPendingData = true`
2. Вызывает `callback.onPendingDataAvailable(pendingFiles)`
3. Скачивает все pending-файлы через `api.downloadAllFilesSync()`
4. Записывает в БД: `UtilDB().insertItemList(items)`
5. Удаляет файлы с сервера по UUID
6. Отправляет `input=0, bd=3` на сервер (данные загружены)
7. Обновляет UI: `updateInputStatusUI(0)`, `updateBDStatus(3)`

### Канал Б — Файловый обмен (режимы USB/WIFI, appConnect1C=3 или 4)

```
FileExchangeManager.startPolling() → каждые 2 сек
    ↓
hasInputFile() → tsd_Input.json? → onInputFileReady()
hasOutputFile() → tsd_Output.json? → onOutputFileReady()
    ↓
Чтение tsd_dev_status.txt (SSV формат)
    ↓
Пересчёт bd по факту из БД
    ↓
onStatusChanged(newStatus) → обновление UI
```

**Polling в FileExchangeManager:**
- Проверка наличия `tsd_Input.json` — если есть + сопряжено → `onInputFileReady()`
- Проверка наличия `tsd_Output.json` — если есть → `onOutputFileReady()`
- Чтение `tsd_dev_status.txt` → парсинг SSV → обновление `currentStatus`
- Пересчёт `bd` по факту из БД (getCount(), getCountNotEmpty())
- Сравнение с предыдущим статусом — если изменился → `onStatusChanged()`

---

## 4. Обновление bd по факту из БД

**Ключевой момент:** `bd` **всегда пересчитывается** по реальной базе данных, а не берётся из файла/сервера. Это предотвращает рассинхронизацию.

```kotlin
// В FileExchangeManager.polling (каждые 2 сек):
val dao = app?.database?.itemDao()
val total = dao?.getCount() ?: 0
val notEmpty = dao?.getCountNotEmpty() ?: 0
val calculatedBd = if (notEmpty > 0) 2 else if (total > 0) 3 else 0

// В StatusPollingService.polling (каждые 5 сек):
val bdFromServer = status.bd
appState.appBd = bdFromServer
```

**В LicenseUtil.conditionInfo():**
```kotlin
val mCount = getCount()
val mCountNotEmpty = getCountNotEmpty()
if (mCount > 0) {
    appInfoBD.postValue(3)            // база есть, записей > 0
    if (mCountNotEmpty > 0) {
        appInfoBD.postValue(2)        // есть что выгружать
    } else {
        appInfoBD.postValue(3)        // база полная
    }
} else {
    appInfoBD.postValue(0)            // база пуста
}
```

---

## 5. Циклы состояний (машины состояний)

### Цикл bd:

```
         ┌───────────────────────────────┐
         │                               ▼
   0 ──► (-1) ──► 3 ──► 2              │
 (пусто) (загрузка) (есть) (выгрузка)    │
         ▲                   │           │
         └───────────────────┘           │
      после импорта                     │
                                        ▼
                                 (после выгрузки → 3)
```

### Цикл input:

```
   0 (нет данных)
     │
     ▼  сервер/ПК отправил данные
   6 (pending, файлы готовы к скачиванию)
     │
     ▼  скачали и загрузили в БД
   3 (данные получены, ждёт подтверждения)
     │
     ▼  подтверждение отправлено на сервер
   0 (сброс)
```

### Цикл output:

```
   0 (нет для выгрузки)
     │
     ▼  есть данные для отправки (bd=2)
   2 (не определено, ждёт отправки)
     │
     ▼  отправили на сервер
   3 (данные отправлены)
```

---

## 6. UI-обновление в MenuFragment

В `MenuFragment` статусы отображаются через три TextView:

```kotlin
// Обновление bd
private fun updateBDStatus(bd: Int) {
    when (bd) {
        0 -> textViewBD.setTextColor(COLOR_GRAY)
            textViewBD.setText("БД: В базе данных нет записей")
        2 -> textViewBD.setTextColor(COLOR_YELLOW)
            textViewBD.setText("БД: Всего N зап., из них выб. M")
        3 -> textViewBD.setTextColor(COLOR_GREEN)
            textViewBD.setText("БД: Всего N зап.")
    }
}

// Обновление input
private fun updateInputStatusUI(input: Int) {
    when (input) {
        0 -> textViewInput.setTextColor(COLOR_GRAY)
            textViewInput.setText("Загрузка: Нет данных для загрузки")
        3 -> textViewInput.setTextColor(COLOR_YELLOW)
            textViewInput.setText("Загрузка: Данные доступны")
    }
}

// Обновление output
private fun updateOutputStatusUI(output: Int) {
    when (output) {
        0 -> textViewOutput.setTextColor(COLOR_GRAY)
            textViewOutput.setText("Выгрузка: Отсутствуют записи для выгрузки")
        3 -> textViewOutput.setTextColor(COLOR_GREEN)
            textViewOutput.setText("Выгрузка: Данные отправлены")
    }
}
```

**Метод `updateStatusesSync()`** собирает все статусы и обновляет UI за один вызов.

---

## 7. Инициализация при старте

В `MainActivity.onCreate()`:

1. Загрузка настроек из SharedPreferences
2. Проверка сопряжения через `checkDeviceStatus()`
3. Если `appConnect1C > 0` → переход в MenuFragment
4. В `MenuFragment.onResume()`:
   - `updateStatusesSync()` — первоначальная синхронизация
   - `startStatusPolling()` — запуск polling-сервиса

**Для Socket/Сайт режима:**
```kotlin
// Запуск StatusPollingService
pollingService = StatusPollingService()
pollingService?.setCallback(this)  // MenuFragment реализует Callback
pollingService?.startPolling(
    context, appState, appLic, apiClient
)
```

**Для USB/WIFI режима:**
```kotlin
// Запуск FileExchangeManager.polling
fileExchangeManager = FileExchangeManager(context, usbMode = true)
fileExchangeManager.onStatusChanged = { status ->
    // Обновление UI при изменении статуса из файла
}
fileExchangeManager.startPolling()
```

---

## 8. Сводная диаграмма потоков данных

```
+---------------------------------------------------------------------+
|                    ИСТОЧНИКИ ДАННЫХ                                 |
+---------------------------+-----------------------------------------+
|   Socket/Сайт (app=2)    |   USB/WIFI (app=3,4)            |
|                          |                                         |
|  GET /api/v1/devices/    |  tsd_dev_status.txt (SSV)       |
|          /status         |  tsd_Input.json                 |
|                          |  tsd_Output.json                |
+----------+--------------+-----------+-------------------------+
           |                             |
           v                             v
+-----------------------------------------------+
|              АНАЛИЗ И ПЕРЕЧИСЛЕНИЕ                    |
+---------------------------+-------------------------+
|  StatusPollingService    |  FileExchangeManager      |
|  (каждые 5 сек)          |  (каждые 2 сек)           |
|                          |                           |
|  parseJSON(status)       |  readStatus() -> SSV       |
|  -> DeviceStatusPayload   |  -> DeviceStatus           |
+----------+--------------+-----------+-----------------+
           |                             |
           v                             v
+-----------------------------------------------+
|              ПЕРЕСЧЁТ bd ПО ФАКТУ                    |
+-----------------------------------------------+
|  Room DB: getCount() + getCountNotEmpty()            |
|  if (notEmpty > 0) -> bd = 2                          |
|  else if (total > 0) -> bd = 3                        |
|  else -> bd = 0                                       |
+----------+--------------------------------------------+
           |
           v
+-----------------------------------------------+
|              ОБНОВЛЕНИЕ СОСТОЯНИЙ                    |
+---------------------------+-------------------------+
|  AppState                |  FileExchangeManager      |
|  appInputStatus          |  currentStatus            |
|  appOutputStatus         |                           |
|  hasPendingData          |                           |
+----------+--------------+-----------+-----------------+
           |                             |
           v                             v
+-----------------------------------------------+
|              CALLBACK /LiveData                     |
+---------------------------+-------------------------+
|  StatusPollingService    |  LicenseUtil              |
|  .Callback               |  (LiveData)               |
|  onStatusUpdated()       |  appInfoBD                |
|  onPendingDataAvailable()|  appInfoINPUT             |
|  onFilesDownloaded()     |  appInfoOUT               |
+----------+--------------+-----------+-----------------+
           |                             |
           v                             v
+-----------------------------------------------+
|              ОБНОВЛЕНИЕ UI (MenuFragment)            |
+---------------------------+-------------------------+
|  updateStatusesSync()    |  observe(LiveData)        |
|  updateBDStatus(bd)      |  appInfoBD.observe()      |
|  updateInputStatusUI()   |  appInfoINPUT.observe()   |
|  updateOutputStatusUI()  |  appInfoOUT.observe()     |
+---------------------------+-------------------------+
```

---

## 9. Ключевые особенности системы

1. **bd всегда пересчитывается по факту** — ни файл, ни сервер не являются единственным источником истины для bd. Room БД — авторитетный источник.

2. **Два параллельных polling-механизма** — StatusPollingService (Socket/Сайт) и FileExchangeManager.polling (USB/WIFI) работают по одному принципу, но с разными источниками данных.

3. **hasPendingData** — флаг для отслеживания цикла `input=6 -> скачивание -> input=0`. Не сбрасывается автоматически при перезапуске приложения.

4. **deduplication статусов** — в FileExchangeManager используется `lastStatusString` (SSV-строка), чтобы не вызывать callback при каждом опросе, если статус не изменился.

5. **LiveData для UI** — LicenseUtil передаёт статусы через MutableLiveData, что позволяет фрагментам подписаться и автоматически получать обновления.

6. **Безопасный доступ к БД** — всегда через `TSDXXIVekApplication.instance?.database?.itemDao()`, а не через локальные переменные.

---

*Последнее обновление: 2026-09-14*