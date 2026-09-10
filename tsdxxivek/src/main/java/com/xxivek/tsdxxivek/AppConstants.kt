package com.xxivek.tsdxxivek

/**
 * Константы для SharedPreferences ключей.
 * Централизованное хранение всех ключей настроек.
 */
object AppConstants {
    const val APP_PREF_LIC = "license"
    const val APP_PREF_DEVICE_UUID = "device_uuid"
    const val APP_PREF_PORT = "port"
    const val APP_PREF_KONF = "configuration"
    const val APP_PREF_CONNECT1C = "connct1c"
    const val APP_PREF_ОPER = "operition"
    const val APP_PREF_CLIENT = "client"
    const val APP_PREF_LOGGING = "logging"
    const val APP_PREF_USE_SOCKET = "use_socket"
    const val APP_PREF_TOPIC = "topic"
    const val APP_PREF_DESIGN = "design"

    // Файлообмен с 1С/ТСД
    const val FILE_EXCHANGE_DIR = "/storage/emulated/0/Download/TSD/"
    const val FILE_INPUT_JSON = "Input.json"
    const val FILE_OUTPUT_JSON = "Output.json"
    const val FILE_DEV_STATUS = "tsd_dev_status.txt"
}
