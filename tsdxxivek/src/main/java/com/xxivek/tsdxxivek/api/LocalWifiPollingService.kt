package com.xxivek.tsdxxivek.api

import android.content.Context
import android.util.Log
import com.xxivek.tsdxxivek.AppState
import com.xxivek.tsdxxivek.utilAPP.appendLog
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Сервис периодического опроса статуса локального HTTP-сервера ТСД (режим "Локальный WIFI").
 * Опрашивает GET /api/v1/devices/status каждые 5 секунд.
 */
class LocalWifiPollingService {

    companion object {
        const val TAG = "LocalWifiPolling"
        const val POLL_INTERVAL_MS = 5000L // 5 секунд
    }

    private var coroutineScope: CoroutineScope? = null
    private var isRunning = false

    // Callback для уведомления о событиях
    interface Callback {
        fun onStatusUpdated(input: Int, output: Int)
        fun onPollError(message: String)
    }

    private var callback: Callback? = null

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    /**
     * Запустить опрос локального сервера.
     *
     * @param context контекст приложения
     * @param appState глобальное состояние приложения
     * @param appLic лицензия (для обновления LiveData)
     * @param ip адрес ТСД
     * @param port порт HTTP-сервера ТСД
     */
    fun startPolling(
        context: Context,
        appState: AppState,
        appLic: com.xxivek.tsdxxivek.utilAPP.LicenseUtil,
        ip: String,
        port: Int
    ) {
        if (isRunning) {
            Log.w(TAG, "startPolling: already running")
            return
        }

        isRunning = true
        coroutineScope = CoroutineScope(Dispatchers.IO + CoroutineName("LocalWifiPolling"))

        Log.d(TAG, "startPolling: polling local server $ip:$port")

        coroutineScope!!.launch {
            while (isRunning && coroutineScope!!.isActive) {
                try {
                    val url = "http://$ip:$port/api/v1/devices/status"
                    Log.d(TAG, "polling: GET $url")

                    val status = pollLocalServer(url)

                    if (status.httpCode == 200 && status.input != null && status.output != null) {
                        val input = status.input
                        val output = status.output
                        val bd = status.bd ?: 0

                        Log.d(TAG, "polling: status received - input=$input, output=$output, bd=$bd")

                        // Обновляем AppState
                        appState.updateServerStatus(input, output)
                        appState.appBd = bd

                        // Обновляем LiveData для UI
                        appLic.appInfoOUT.postValue(output)
                        appLic.appInfoBD.postValue(bd)

                        // Уведомляем callback
                        callback?.onStatusUpdated(input, output)
                    } else {
                        Log.w(TAG, "polling: unsuccessful response, httpCode=${status.httpCode}")
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "polling: error", e)
                    callback?.onPollError(e.message ?: "Unknown error")
                }

                delay(POLL_INTERVAL_MS)
            }
        }
    }

    /**
     * Опросить локальный HTTP-сервер.
     */
    private fun pollLocalServer(url: String): DeviceStatusResponse {
        var conn: HttpURLConnection? = null
        try {
            conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 3000
            conn.readTimeout = 3000

            val responseCode = conn.responseCode
            Log.d(TAG, "pollLocalServer: responseCode=$responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = StringBuilder()
                var line: String? = ""
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                Log.d(TAG, "pollLocalServer: response=${response.toString().take(200)}")

                // Парсим JSON вручную
                val json = response.toString()
                val pairing = json.contains("\"pairing\":true")
                val konf = Regex("\"konf\":\\s*(\\d+)").find(json)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val bd = Regex("\"bd\":\\s*(-?\\d+)").find(json)?.groupValues?.get(1)?.toIntOrNull()
                val input = Regex("\"input\":\\s*(-?\\d+)").find(json)?.groupValues?.get(1)?.toIntOrNull()
                val output = Regex("\"output\":\\s*(-?\\d+)").find(json)?.groupValues?.get(1)?.toIntOrNull()

                return DeviceStatusResponse(
                    httpCode = 200,
                    pairing = pairing,
                    konf = konf,
                    bd = bd ?: 0,
                    input = input ?: 0,
                    output = output ?: 0,
                    device_uuid = null
                )
            } else {
                return DeviceStatusResponse(
                    httpCode = responseCode,
                    pairing = false,
                    konf = 0,
                    bd = 0,
                    input = 0,
                    output = 0,
                    device_uuid = null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "pollLocalServer: error", e)
            return DeviceStatusResponse(
                httpCode = -1,
                pairing = false,
                konf = 0,
                bd = 0,
                input = 0,
                output = 0,
                device_uuid = null
            )
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Выполнить немедленный опрос сервера (для принудительного обновления статуса).
     */
    fun pollNow(context: Context, appState: AppState, appLic: com.xxivek.tsdxxivek.utilAPP.LicenseUtil, ip: String, port: Int) {
        try {
            val url = "http://$ip:$port/api/v1/devices/status"
            Log.d(TAG, "pollNow: GET $url")

            val status = pollLocalServer(url)

            if (status.httpCode == 200 && status.input != null && status.output != null) {
                val input = status.input
                val output = status.output
                val bd = status.bd ?: 0

                Log.d(TAG, "pollNow: status received - input=$input, output=$output, bd=$bd")

                appState.updateServerStatus(input, output)
                appState.appBd = bd

                appLic.appInfoOUT.postValue(output)
                appLic.appInfoBD.postValue(bd)

                callback?.onStatusUpdated(input, output)
            }
        } catch (e: Exception) {
            Log.e(TAG, "pollNow: error", e)
            callback?.onPollError(e.message ?: "Unknown error")
        }
    }

    /**
     * Остановить опрос.
     */
    fun stopPolling() {
        if (!isRunning) return
        isRunning = false
        coroutineScope?.cancel()
        coroutineScope = null
        Log.d(TAG, "stopPolling: polling stopped")
    }

    /**
     * Проверить, запущен ли опрос.
     */
    fun isPollingRunning(): Boolean = isRunning
}
