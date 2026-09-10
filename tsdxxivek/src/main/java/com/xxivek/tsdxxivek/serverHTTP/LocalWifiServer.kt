package com.xxivek.tsdxxivek.serverHTTP

import com.xxivek.tsdxxivek.AppConstants
import com.xxivek.tsdxxivek.TSDXXIVekApplication
import com.xxivek.tsdxxivek.appLic
import com.xxivek.tsdxxivek.FileExchangeManager
import com.xxivek.tsdxxivek.utilAPP.appendLog
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket

/**
 * HTTP-сервер для режима "Локальный WIFI".
 * Заменяет ServerSocketXXI для JSON-обмена.
 * Работает параллельно с существующим HTTP-сервером.
 */
class LocalWifiServer(private val appContext: android.content.Context) {

    companion object {
        private const val TAG = "LocalWifiServer"
    }

    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    /**
     * Запустить JSON API сервер.
     * @param port порт для прослушивания
     */
    fun start(port: Int) {
        if (isRunning) {
            appendLog(TAG, "JSON API сервер уже запущен на порту $port")
            return
        }

        scope.launch {
            try {
                serverSocket = ServerSocket(port)
                isRunning = true
                appendLog(TAG, "JSON API сервер запущен на порту $port")

                while (isRunning) {
                    val clientSocket = serverSocket?.accept()
                    if (clientSocket != null && isRunning) {
                        handleClient(clientSocket)
                    }
                }
            } catch (e: IOException) {
                if (isRunning) {
                    appendLog(TAG, "Ошибка JSON API сервера: ${e.message}")
                }
            }
        }
    }

    /**
     * Обработать подключение клиента.
     */
    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 10000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val os = socket.getOutputStream()

            // Читаем HTTP-заголовок
            val requestLines = mutableListOf<String>()
            while (true) {
                val line = reader.readLine()
                if (line.isEmpty()) break
                requestLines.add(line)
            }

            if (requestLines.isEmpty()) return

            val requestLine = requestLines[0]
            val method = requestLine.split(" ")[0]
            val path = requestLine.split(" ")[1]

            // Читаем Content-Length для POST
            var contentLength = 0
            for (header in requestLines) {
                if (header.lowercase().startsWith("content-length:")) {
                    contentLength = header.substringAfter(":").trim().toIntOrNull() ?: 0
                    break
                }
            }

            // Читаем тело POST-запроса
            var body = ""
            if (method == "POST" && contentLength > 0) {
                val charBuffer = CharArray(contentLength)
                var totalRead = 0
                while (totalRead < contentLength) {
                    val read = reader.read(charBuffer, totalRead, contentLength - totalRead)
                    if (read == -1) break
                    totalRead += read
                }
                body = String(charBuffer, 0, totalRead)
                appendLog(TAG, "POST тело: ${totalRead} / $contentLength символов")
            }

            appendLog(TAG, "HTTP запрос: $method $path, bodyLen=$contentLength")

            // Обрабатываем запрос
            val response = when {
                method == "GET" && path == "/api/v1/devices/status" -> handleGetStatus()
                method == "POST" && path == "/api/v1/devices/status" -> handlePostStatus(body)
                method == "POST" && path == "/api/v1/exchange/input" -> handlePostInput(body)
                method == "GET" && path == "/api/v1/exchange/output" -> handleGetOutput()
                else -> buildErrorResponse(404, "Not Found")
            }

            // Отправляем ответ как байты (UTF-8)
            val responseBytes = response.toByteArray(Charsets.UTF_8)
            os.write(responseBytes)
            os.flush()
            appendLog(TAG, "HTTP ответ отправлен: ${responseBytes.size} байт")

        } catch (e: IOException) {
            appendLog(TAG, "Ошибка обработки клиента: ${e.message}")
        } finally {
            try {
                socket.close()
            } catch (e: IOException) {
                appendLog(TAG, "Ошибка закрытия сокета: ${e.message}")
            }
        }
    }

    /**
     * Обработать GET /api/v1/devices/status
     * Ответ: {"device_uuid":"","status":{"pairing":true,"konf":5,"bd":3,"input":2,"output":4}}
     */
    private fun handleGetStatus(): String {
        val json = buildString {
            append("{")
            append("\"device_uuid\":\"\",")
            append("\"status\":{")
            append("\"pairing\":${appLic.appConnect1C == 4},")
            append("\"konf\":${appLic.appKONF.toIntOrNull() ?: 0},")
            append("\"bd\":${appLic.appInfoBD.value ?: 0},")
            append("\"input\":${appLic.appInfoINPUT.value ?: 0},")
            append("\"output\":${appLic.appInfoOUT.value ?: 0}")
            append("}")
            append("}")
        }

        return buildHttpResponse(200, "application/json", json)
    }

    /**
     * Обработать POST /api/v1/devices/status
     * ПК отправляет статус (output=0, pairing=true и т.д.)
     */
    private fun handlePostStatus(body: String): String {
        appendLog(TAG, "POST /api/v1/devices/status, тело: $body")

        // Обновляем статус (парсинг JSON упрощён)
        val pairing = body.contains("\"pairing\":true")
        if (pairing) {
            appLic.appConnect1C = 4
        }

        // Парсим output из запроса (ПК может установить output=0)
        val outputMatch = Regex("\"output\":\\s*(\\d+)").find(body)
        outputMatch?.let { match ->
            val output = match.groupValues[1].toInt()
            appLic.appInfoOUT.postValue(output)
            appendLog(TAG, "output установлен: $output")
        }

        // Парсим input из запроса
        val inputMatch = Regex("\"input\":\\s*(\\d+)").find(body)
        inputMatch?.let { match ->
            val input = match.groupValues[1].toInt()
            appLic.appInfoINPUT.postValue(input)
            appendLog(TAG, "input установлен: $input")
        }

        val response = """{"status":"ok","device_uuid":"${appLic.appLIC}"}"""
        return buildHttpResponse(200, "application/json", response)
    }

    /**
     * Обработать POST /api/v1/exchange/input
     * ПК отправляет JSON с данными для загрузки в ТСД.
     * ТСД записывает файл tsd_Input.json и устанавливает input=3.
     */
    private fun handlePostInput(body: String): String {
        appendLog(TAG, "POST /api/v1/exchange/input, тело: $body")

        // Записываем файл tsd_Input.json напрямую
        val exchangeDir = java.io.File(AppConstants.FILE_EXCHANGE_DIR)
        if (!exchangeDir.exists()) {
            exchangeDir.mkdirs()
        }

        val inputFile = java.io.File(exchangeDir, AppConstants.FILE_INPUT_JSON)
        val writeOk = try {
            inputFile.writeText(body)
            true
        } catch (e: IOException) {
            appendLog(TAG, "Ошибка записи tsd_Input.json: ${e.message}")
            false
        }

        if (!writeOk) {
            return buildHttpResponse(500, "application/json", """{"error":"Ошибка записи файла"}""")
        }

        // Устанавливаем input=3 напрямую (не через writeStatus, чтобы не создавать tsd_dev_status.txt)
        appLic.appInfoINPUT.postValue(3)

        appendLog(TAG, "tsd_Input.json записан, input=3")

        val response = """{"status":"ok","message":"Данные получены"}"""
        return buildHttpResponse(200, "application/json", response)
    }

    /**
     * Обработать GET /api/v1/exchange/output
     * ПК запрашивает данные для выгрузки.
     * ТСД возвращает содержимое tsd_Output.json и устанавливает output=3.
     */
    private fun handleGetOutput(): String {
        val context = appContext
        val fileManager = FileExchangeManager(context)

        // Формируем Output.json если нет
        if (!fileManager.hasOutputFile()) {
            val result = fileManager.writeOutputAndExport()
            if (!result.success) {
                return buildHttpResponse(500, "application/json", """{"error":"${result.message}"}""")
            }
        }

        // Читаем и возвращаем содержимое
        val exchangeDir = java.io.File(AppConstants.FILE_EXCHANGE_DIR)
        val outputFile = java.io.File(exchangeDir, AppConstants.FILE_OUTPUT_JSON)

        val jsonContent = try {
            outputFile.readText()
        } catch (e: IOException) {
            return buildHttpResponse(500, "application/json", """{"error":"Ошибка чтения файла"}""")
        }

        // Устанавливаем output=3
        appLic.appInfoOUT.postValue(3)

        appendLog(TAG, "tsd_Output.json отправлен, output=3")

        return buildHttpResponse(200, "application/json", jsonContent)
    }

    /**
     * Построить HTTP-ответ.
     */
    private fun buildHttpResponse(statusCode: Int, contentType: String, body: String): String {
        val statusText = when (statusCode) {
            200 -> "OK"
            400 -> "Bad Request"
            404 -> "Not Found"
            else -> "Error"
        }

        return buildString {
            append("HTTP/1.1 $statusCode $statusText\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${body.toByteArray(Charsets.UTF_8).size}\r\n")
            append("Connection: close\r\n")
            append("\r\n")
            append(body)
        }
    }

    /**
     * Построить ответ с ошибкой.
     */
    private fun buildErrorResponse(statusCode: Int, message: String): String {
        val body = """{"error":"$message"}"""
        return buildHttpResponse(statusCode, "application/json", body)
    }

    /**
     * Остановить сервер.
     */
    fun stop() {
        isRunning = false
        job.cancel()

        try {
            serverSocket?.close()
        } catch (e: IOException) {
            appendLog(TAG, "Ошибка остановки JSON API сервера: ${e.message}")
        }

        serverSocket = null
        appendLog(TAG, "JSON API сервер остановлен")
    }
}
