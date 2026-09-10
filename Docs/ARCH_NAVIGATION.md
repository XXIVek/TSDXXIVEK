# TSDXXIVEK — Навигация и Жизненный цикл

## 1. Навигация (граф фрагментов)

```
StartFragment (заставка с видео)
  └─> LogoFragment (логотип + настройки: тема, дизайн, режим)
       ├─> MenuFragment (главное меню)
       │    ├─> ScanerFragment / ScanerFragment_d (сканер, по DESIGN)
       │    ├─> SettingsFragment (настройки)
       │    ├─> FileDownloadFragment (скачивание файлов)
       │    └─> ItemListFragment1 / ItemListFragment2 / ItemListFragment (список товаров)
       │         └─> AddItemFragment (добавление товара)
       ├─> PairingFragment (ожидание сопряжения)
       └─> SetgeneralFragment (общие настройки)
```

**startDestination:** `StartFragment` → `LogoFragment`

### Логика перехода из LogoFragment

| appConnect1C | Поведение |
|:---:|------|
| `> 0` | Автоматический переход в MenuFragment |
| `== 0` | Остаёмся на LogoFragment, кнопка "Далее" → ScanerFragment |

### Кнопка "Далее" (bNext)

| appConnect1C | Переход |
|:---:|------|
| `== 0` | ScanerFragment / ScanerFragment_d (по DESIGN) |
| `> 0` | MenuFragment |

---

## 2. Жизненный цикл

### Запуск (MainActivity.onCreate)
1. Загрузка всех настроек из SharedPreferences
2. Вызов `checkDeviceStatus()` — проверка сопряжения и статуса
3. Запуск LogoFragment (стартовый экран)

### Закрытие (MainActivity.appExit / onDestroy)
1. Закрытие serverSocket, connectionSocket
2. Вызов `saveState()` — сохранение LIC, PORT, KONF, appConnect1C, OPER, CLIENT, USE_WEBSITE, USE_SOCKET
3. exitProcess(-1)

### Сброс сопряжения
При сбросе сопряжения удаляются файлы обмена:
- `tsd_dev_status.txt`
- `tsd_Input.json`
- `tsd_Output.json`

**Места сброса:**
- `MainActivity.resetPairingState()` — при запуске (checkDeviceStatus)
- `LogoFragment.bLogoPairing` — кнопка сброса сопряжения
- `LogoFragment.radioGroupUseWebsite` — переключение режима

### Ключевой момент
- **ScanerFragment НЕ сбрасывает** состояние при открытии
- **saveState()** сохраняет все настройки включая USE_WEBSITE и USE_SOCKET

---

*Последнее обновление: 2026-09-07*
