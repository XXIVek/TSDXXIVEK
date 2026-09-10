package com.xxivek.tsdxxivek.serverHTTP

import com.xxivek.tsdxxivek.appLic
import com.xxivek.tsdxxivek.utilAPP.appendLog
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import java.io.IOException
import java.net.*

/**
 * Broadcast-сервер для обнаружения ТСД.
 * Заменяет SSDP — отправляет UDP-объявления на broadcast-адрес подсети.
 * Работает каждые 2 секунды в течение 20 секунд после сопряжения.
 * Прекращает при первом HTTP-запросе на порт ТСД.
 */
class BroadcastServer {

    companion object {
        private const val TAG = "BroadcastServer"
        private const val BROADCAST_INTERVAL_MS = 2000L
        private const val BROADCAST_DURATION_MS = 20000L
        private const val JSON_FORMAT = """{"ip":"%s","port":%d,"lic":"%s"}"""
    }

    private var isRunning = false
    private var job: Job? = null
    private var scope: CoroutineScope? = null
    private var udpSocket: DatagramSocket? = null
    private var broadcastAddress: InetAddress? = null
    private var httpPort: Int = 0
    private var localIp: String = ""
    private var license: String = ""
    private var stopRequested = false

    /**
     * Запустить broadcast-объявления.
     * @param subnetPrefix префикс подсети (например "192.168.160")
     * @param port порт HTTP-сервера ТСД
     */
    fun start(subnetPrefix: String, port: Int, license: String) {
        if (isRunning) {
            appendLog(TAG, "BroadcastServer уже запущен")
            return
        }

        httpPort = port
        localIp = getLocalIpAddress()
        this.license = license

        // Формируем broadcast-адрес из подсети
        broadcastAddress = try {
            InetAddress.getByName("$subnetPrefix.255")
        } catch (e: Exception) {
            appendLog(TAG, "Ошибка создания broadcast-адреса: ${e.message}")
            return
        }

        isRunning = true
        stopRequested = false
        job = Job()
        scope = CoroutineScope(Dispatchers.IO + job!!)

        scope!!.launch {
            try {
                udpSocket = DatagramSocket()
                udpSocket?.broadcast = true
                udpSocket?.soTimeout = 1000

                appendLog(TAG, "BroadcastServer запущен: $localIp:$port -> $broadcastAddress")
                android.util.Log.d(TAG, "BroadcastServer ЗАПУЩЕН: $localIp:$port -> $broadcastAddress:1900")

                var sendCount = 0
                val startTime = System.currentTimeMillis()

                // Периодические объявления (20 секунд)
                while (isRunning && (System.currentTimeMillis() - startTime < BROADCAST_DURATION_MS) && !stopRequested) {
                    sendBroadcast()
                    sendCount++
                    val msg = "BroadcastServer ОТПРАВКА #$sendCount: $localIp:$port -> $broadcastAddress:1900 lic=$license"
                    appendLog(TAG, msg)
                    android.util.Log.d(TAG, msg)

                    delay(BROADCAST_INTERVAL_MS)
                }

                if (stopRequested) {
                    val stopMsg = "BroadcastServer ОСТАНОВЛЕН по запросу (объявлений: $sendCount)"
                    appendLog(TAG, stopMsg)
                    android.util.Log.d(TAG, stopMsg)
                } else {
                    val stopMsg = "BroadcastServer ЗАВЕРШИЛ РАБОТУ (объявлений: $sendCount)"
                    appendLog(TAG, stopMsg)
                    android.util.Log.d(TAG, stopMsg)
                }
            } catch (e: IOException) {
                if (isRunning) {
                    appendLog(TAG, "Ошибка BroadcastServer: ${e.message}")
                }
            } finally {
                isRunning = false
            }
        }
    }

    /**
     * Остановить broadcast-объявления.
     */
    fun stop() {
        appendLog(TAG, "BroadcastServer: запрос на остановку")
        stopRequested = true
        isRunning = false
        job?.cancel()

        try {
            udpSocket?.close()
        } catch (e: IOException) {
            appendLog(TAG, "Ошибка остановки BroadcastServer: ${e.message}")
        }
        udpSocket = null
        appendLog(TAG, "BroadcastServer остановлен")
    }

    /**
     * Отправить одно broadcast-объявление.
     */
    private fun sendBroadcast() {
        if (broadcastAddress == null) return

        val json = String.format(JSON_FORMAT, localIp, httpPort, license)
        try {
            val data = json.toByteArray()
            val packet = DatagramPacket(data, data.size, broadcastAddress, 1900)
            udpSocket?.send(packet)
        } catch (e: IOException) {
            appendLog(TAG, "Ошибка отправки broadcast: ${e.message}")
        }
    }

    /**
     * Получить локальный IP-адрес устройства.
     */
    private fun getLocalIpAddress(): String {
        try {
            for (iface in NetworkInterface.getNetworkInterfaces().asSequence()) {
                if (iface.name == "wlan0" || iface.name.startsWith("wlan")) {
                    if (iface.isUp && !iface.isLoopback) {
                        for (addr in iface.inetAddresses.asSequence()) {
                            val hostAddr = addr.hostAddress
                            if (hostAddr != null && !hostAddr.contains(":") && !hostAddr.startsWith("127.")) {
                                return hostAddr
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            appendLog(TAG, "Ошибка получения IP: ${e.message}")
        }
        return "127.0.0.1"
    }
}
