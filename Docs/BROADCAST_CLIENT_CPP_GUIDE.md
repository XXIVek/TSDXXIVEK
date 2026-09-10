# Рекомендации для AI: Замена SSDP на Broadcast-объявления

## Контекст

Проект: TSDXXIVEK — ТСД (терминал сбора данных) для торговли.
Режим: "Локальный WIFI" (сопряжение по QR-коду).
Проблема: SSDP не работает на Android из-за MulticastLock.
Решение: Заменён на UDP broadcast-объявления.

## Что изменилось на ТСД

### Новый класс: BroadcastServer.kt

Расположение: `tsdxxivek/src/main/java/com/xxivek/tsdxxivek/serverHTTP/BroadcastServer.kt`

**Поведение:**
- Запускается при сканировании QR-кода с `socket:true`
- Отправляет UDP-пакет на broadcast-адрес подсети каждые 2 секунды
- Работает в течение 20 секунд
- Прекращает при первом HTTP-запросе на порт ТСД (не реализовано, но планируется)
- **НЕ требует MulticastLock**

### Формат broadcast-адреса

ТСД автоматически определяет свой IP и формирует broadcast:
- IP ТСД: `192.168.160.160`
- Broadcast: `192.168.160.255`
- Порт: `1900`

### Формат JSON-сообщения

```json
{"ip":"192.168.160.160","port":8180,"lic":"5"}
```

Поля:
- `ip` — фактический IP ТСД (строка)
- `port` — порт HTTP-сервера (число)
- `lic` — лицензия из QR-кода (строка)

### Пример UDP-пакета

```
{"ip":"192.168.160.160","port":8180,"lic":"5"}
```

Отправляется на `192.168.160.255:1900` каждые 2 секунды в течение 20 секунд.

## Что нужно сделать в C++ клиенте

### 1. Заменить SSDP-код на UDP-listener

**Удалить:**
- Код отправки M-SEARCH на `239.255.255.250:1900`
- Код получения multicast-ответов
- Парсинг USN формата `uuid:xxx::ssdp:tsd-YYY`

**Добавить:**
- UDP-сокет для прослушивания порта 1900
- Таймаут ожидания (например, 25 секунд)
- Парсинг JSON-ответа

### 2. Алгоритм работы

```cpp
// 1. Создать UDP-сокет
SOCKET sock = socket(AF_INET, SOCK_DGRAM, 0);
int opt = 1;
setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
setsockopt(sock, SOL_SOCKET, SO_BROADCAST, &opt, sizeof(opt));

// 2. Привязать к порту 1900
sockaddr_in addr;
addr.sin_family = AF_INET;
addr.sin_port = htons(1900);
addr.sin_addr.s_addr = INADDR_ANY;
bind(sock, (sockaddr*)&addr, sizeof(addr));

// 3. Ожидать ответы в течение 25 секунд
char buffer[512];
sockaddr_in senderAddr;
int senderLen = sizeof(senderAddr);

auto startTime = std::chrono::steady_clock::now();
while (std::chrono::steady_clock::now() - startTime < std::chrono::seconds(25)) {
    // Проверить таймаут
    timeval tv;
    tv.tv_sec = 2;
    tv.tv_usec = 0;
    setsockopt(sock, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));
    
    int bytes = recvfrom(sock, buffer, sizeof(buffer), 0, (sockaddr*)&senderAddr, &senderLen);
    if (bytes > 0) {
        buffer[bytes] = '\0';
        
        // Парсить JSON
        // Формат: {"ip":"192.168.160.160","port":8180,"lic":"5"}
        
        // Извлечь lic, ip, port
        // Если lic совпадает с заданным — использовать ip:port
        // Иначе — продолжить ожидание
    }
}

// 4. Подключиться к найденному устройству
// HTTP GET http://<ip>:<port>/api/v1/devices/status
```

### 3. Парсинг JSON

**Простой парсинг без библиотек:**

```cpp
struct DeviceInfo {
    std::string ip;
    int port;
    std::string lic;
};

DeviceInfo parseBroadcastJson(const std::string& json) {
    DeviceInfo info;
    
    // Извлечь "ip":"..."
    auto ipPos = json.find("\"ip\":");
    if (ipPos != std::string::npos) {
        auto start = json.find('"', ipPos + 5);
        auto end = json.find('"', start + 1);
        if (start != std::string::npos && end != std::string::npos) {
            info.ip = json.substr(start + 1, end - start - 1);
        }
    }
    
    // Извлечь "port":<number>
    auto portPos = json.find("\"port\":");
    if (portPos != std::string::npos) {
        info.port = std::stoi(json.substr(portPos + 7));
    }
    
    // Извлечь "lic":"..."
    auto licPos = json.find("\"lic\":");
    if (licPos != std::string::npos) {
        auto start = json.find('"', licPos + 5);
        auto end = json.find('"', start + 1);
        if (start != std::string::npos && end != std::string::npos) {
            info.lic = json.substr(start + 1, end - start - 1);
        }
    }
    
    return info;
}
```

### 4. Фильтрация по лицензии

Если пользователь указал лицензию в настройках:
```cpp
if (!m_License.empty() && deviceInfo.lic != m_License) {
    continue; // Пропустить несоответствующее устройство
}
```

### 5. Подключение к устройству

После получения IP:
```cpp
// Проверить статус устройства
std::string url = "http://" + deviceInfo.ip + ":" + std::to_string(deviceInfo.port) + "/api/v1/devices/status";
// HTTP GET к url
// Если pairing == true — использовать устройство
```

## Ключевые отличия от SSDP

| Параметр | SSDP (старый) | Broadcast (новый) |
|----------|---------------|-------------------|
| Протокол | UDP multicast | UDP broadcast |
| Адрес | 239.255.255.250:1900 | 192.168.160.255:1900 |
| Формат | HTTP-заголовки | JSON |
| Формат USN | `uuid:xxx::ssdp:tsd-YYY` | `{"lic":"5"}` |
| MulticastLock | Требовался | Не нужен |
| Через роутеры | Нет | Да |
| Длительность | Пока работает сервер | 20 секунд |

## Пример полного цикла

1. Пользователь сканирует QR: `PAIR:true;socket:true;konf:1;Port:8180;Lic:5;`
2. ТСД запускает BroadcastServer
3. ТСД отправляет: `{"ip":"192.168.160.160","port":8180,"lic":"5"}` на `192.168.160.255:1900`
4. C++ клиент получает JSON
5. C++ клиент проверяет `lic == "5"`
6. C++ клиент делает `GET http://192.168.160.160:8180/api/v1/devices/status`
7. Получает: `{"lic":"5","konf":"1","pairing":true,"bd":0,"input":0,"output":0}`
8. Сопряжение завершено

## Примечания

- BroadcastServer запускается **только** при `socket:true` в QR
- Если порт не указан в QR — используется порт по умолчанию 80
- ТСД автоматически определяет подсеть из своего IP
- C++ клиент должен слушать порт 1900, а не отправлять запросы
- Если устройство не найдено за 25 секунд — показать ошибку
