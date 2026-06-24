package com.xxivek.tsdxxivek

import android.net.Uri
import java.net.ServerSocket
import java.net.Socket

/**
 * Класс для хранения глобального состояния приложения.
 * Заменяет глобальные переменные, ранее объявленные в MainActivity.kt.
 */
class AppState {
    
    // Лицензирование
    var appLIC: String = "-1"
        private set
    var appPORT: String = "0"
        private set
    var appKONF: String = "Не определена"
        private set
    var appConnect1C: Int = 0
        private set
    var appOper: String = ""
        private set
    var appClient: String = ""
        private set
    
    // Настройки интерфейса
    var TOPIC: Int = 0
        private set
    var DESIGN: Int = 0
        private set
    
    // Логирование
    var LOGGING: Boolean = false
        private set
    
    // HTTP сервер
    var serverSocket: ServerSocket? = null
        private set
    var SERVERPORT: Int? = null
        private set
    var client_Data: ArrayList<String>? = null
        private set
    var connectionSocket: Socket? = null
        private set
    
    // Сообщения HTTP
    var msg_server: String = ""
        private set
    var msg_client: String = ""
        private set
    var headingHTTP: String = ""
        private set
    
    // Лог
    var msg_log: String? = null
        private set
    
    // Статус устройства от сервера
    var appInputStatus: Int = 0
    var appOutputStatus: Int = 0
    var hasPendingData: Boolean = false
    var appStatusUpdatedAt: Long = 0

    // Методы для обновления настроек
    fun updateFromPrefs(prefs: android.content.SharedPreferences) {
        appLIC = prefs.getString(AppConstants.APP_PREF_LIC, "-1") ?: "-1"
        appPORT = prefs.getString(AppConstants.APP_PREF_PORT, "0") ?: "0"
        appKONF = prefs.getString(AppConstants.APP_PREF_KONF, "Не определена") ?: "Не определена"
        appConnect1C = prefs.getInt(AppConstants.APP_PREF_CONNECT1C, 0)
        appOper = prefs.getString(AppConstants.APP_PREF_ОPER, "") ?: ""
        appClient = prefs.getString(AppConstants.APP_PREF_CLIENT, "") ?: ""
        LOGGING = prefs.getBoolean(AppConstants.APP_PREF_LOGGING, false)
        TOPIC = prefs.getInt(AppConstants.APP_PREF_TOPIC, 0)
        DESIGN = prefs.getInt(AppConstants.APP_PREF_DESIGN, 0)
    }
    
    /**
     * Обновить статус устройства от сервера.
     * @param input текущее значение input с сервера
     * @param output текущее значение output с сервера
     */
    fun updateServerStatus(input: Int, output: Int) {
        appInputStatus = input
        appOutputStatus = output
        hasPendingData = (input == 6)
        appStatusUpdatedAt = System.currentTimeMillis()
    }
    
    /**
     * Сбросить статус устройства.
     */
    fun resetStatus() {
        appInputStatus = 0
        appOutputStatus = 0
        hasPendingData = false
        appStatusUpdatedAt = 0
    }
    
    fun resetMessages() {
        msg_server = ""
        msg_client = ""
        headingHTTP = ""
    }
    
    fun setAppOper(value: String) { appOper = value }
    fun setAppClient(value: String) { appClient = value }
    fun setTOPIC(value: Int) { TOPIC = value }
    fun setDESIGN(value: Int) { DESIGN = value }
}
