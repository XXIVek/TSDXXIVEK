# TSDXXIVEK — HTTP API, JSON форматы, SharedPreferences

## 1. JSON форматы обмена

### tsd_Input.json (ПК → ТСД)

Формат единый для всех режимов (основная часть):

```json
{
  "oper": "0",
  "client": "",
  "data": [
    {
      "Shtrih": "4600068026514",
      "ShtrihTip": 1,
      "TovNaim": "Товар",
      "TovCena": 22.8,
      "TovKol": 0
    }
  ]
}
```

| Поле | Тип | Описание |
|------|-----|----------|
| `oper` | String | Оператор |
| `client` | String | Клиент |
| `data` | Array | Массив товаров |
| `Shtrih` | String | Штрихкод |
| `ShtrihTip` | Int | Тип штрихкода |
| `TovNaim` | String | Наименование товара |
| `TovCena` | Float | Цена товара |
| `TovKol` | Int | Количество товара |

### tsd_Output.json (ТСД → ПК)

Формат идентичен `tsd_Input.json`.

---

## 2. HTTP API (режим Socket / Сайт)

Эндпоинты доступны через промежуточный сайт (`http://192.168.160.99`).

### Эндпоинты

| Эндпоинт | Метод | Направление | Описание |
|----------|-------|-------------|----------|
| `/api/v1/devices/activate` | POST | ПК → Сайт | Активация устройства, получение token |
| `/api/v1/devices/status` | GET | Сайт → ТСД | Получение состояния ТСД |
| `/api/v1/devices/status` | POST | ТСД → Сайт | Обновление состояния (bd, input, output) |
| `/api/v1/exchange/input` | POST | ПК → Сайт | Данные для загрузки от ПК |
| `/api/v1/exchange/output` | GET | ПК ← Сайт | Данные для выгрузки от ТСД |

### Формат запроса POST /api/v1/devices/activate

**Request:**
```json
{"activation_code": "CODE"}
```

**Response:**
```json
{
  "device_uuid": "UUID",
  "message": "OK"
}
```

### Формат ответа GET /api/v1/devices/status

```json
{
  "device_uuid": "UUID",
  "status": {
    "pairing": true,
    "konf": 5,
    "bd": 3,
    "input": 2,
    "output": 4
  }
}
```

### Формат запроса POST /api/v1/devices/status

**Request:**
```json
{
  "pairing": true,
  "konf": 5,
  "bd": 3,
  "input": 0,
  "output": 0
}
```

**Response:**
```json
{
  "status": "ok",
  "paired": true,
  "message": "Updated"
}
```

### Авторизация

Все запросы к сайту используют Bearer-токен:
```
Authorization: Bearer <device_uuid>
```

---

## 3. HTTP API (режим Локальный WIFI)

ТСД запускает собственный HTTP-сервер (`LocalWifiServer`). Эндпоинты аналогичны режиму Сайт, но запросы идут напрямую к ТСД по IP.

| Эндпоинт | Метод | Описание |
|----------|-------|----------|
| `/api/v1/devices/status` | GET | Статус ТСД |
| `/api/v1/devices/status` | POST | Обновление статуса |
| `/api/v1/exchange/input` | POST | Данные для загрузки |
| `/api/v1/exchange/output` | GET | Данные для выгрузки |

**Отличие:** нет активации и токена — обмен по обнаруженному IP.

---

## 4. UDP Broadcast (режим Локальный WIFI)

ТСД объявляет себя в локальной сети:

| Параметр | Значение |
|----------|----------|
| Адрес | `255.255.255.255:1900` |
| Интервал | Каждые 2 секунды |
| Длительность | 20 секунд |
| Формат | `{"ip":"%s","port":%d,"lic":"%s"}` |

---

## 5. SSV формат (режимы USB и WIFI)

Файл `tsd_dev_status.txt`: `pairing;konf;bd;input;output`

Пример: `true;1;3;0;0`

| Позиция | Параметр | Значения |
|---------|----------|----------|
| 1 | Сопряжение | `true` / `false` |
| 2 | Конфигурация | `0` — любая, `1` — XXI век |
| 3 | БД | `0` — пуста, `1` — ошибка, `2` — с выбранными, `3` — без выбранных |
| 4 | input | `0` — нет, `1` — ошибка, `3` — есть данные |
| 5 | output | `0` — нет, `1` — ошибка, `3` — есть данные |

---

## 6. SharedPreferences (ключи)

| Ключ | Значение | Описание | Пакет |
|------|----------|----------|-------|
| `license` | `"Lic=-1;Port=0;Konf=XXI;"` | LIC, порт, конфигурация | Основная |
| `connct1c` | 0 / 2 / 3 / 4 | Статус сопряжения и режим | Основная |
| `port` | `"0"` | Порт для Socket-режима | Основная |
| `configuration` | `"XXI"` | Номер конфигурации 1С | Основная |
| `operition` | `""` | Оператор | Основная |
| `client` | `""` | Клиент | Основная |
| `device_uuid` | `""` | UUID устройства (режим Сайт) | Режимная (Сайт) |
| `use_website` | false / true | Флаг режима «Сайт» | Основная |
| `use_socket` | false / true | Флаг Socket-режима | Основная |
| `topic` | 0 / 1 | Тема (день/ночь) | Основная |
| `design` | 0 / 1 | Дизайн сканера (T/D) | Основная |
| `logging` | false / true | Логирование в файл | Основная |

**Константы:** `AppConstants.kt` (строки 8-25)

---

## 7. Классы-обёртки API

### Основная часть

| Класс | Файл | Назначение |
|-------|------|------------|
| `FileExchangeManager` | `FileExchangeManager.kt` | Чтение/запись JSON и SSV-файлов, polling папки |
| `DeviceStatus` | `FileExchangeManager.kt` | Модель состояния (pairing, konf, bd, input, output) |

### Режимная часть — Socket / Сайт

| Класс | Файл | Назначение |
|-------|------|------------|
| `ApiClient` | `api/ApiClient.kt` | activateDevice, getDeviceStatus, sendPairingStatus |
| `FileDownloadApi` | `api/FileDownloadApi.kt` | downloadFiles, exportAndUpload, updateDeviceStatusSync |
| `StatusPollingService` | `api/StatusPollingService.kt` | Polling статуса каждые 5 сек |
| `ExchangeDataParser` | `api/ExchangeDataParser.kt` | Парсинг JSON-файлов с сайта |

### Режимная часть — Локальный WIFI

| Класс | Файл | Назначение |
|-------|------|------------|
| `LocalWifiServer` | `serverHTTP/LocalWifiServer.kt` | HTTP-сервер на ТСД |
| `BroadcastServer` | `serverHTTP/BroadcastServer.kt` | UDP broadcast |

---

*Последнее обновление: 2026-09-09*
