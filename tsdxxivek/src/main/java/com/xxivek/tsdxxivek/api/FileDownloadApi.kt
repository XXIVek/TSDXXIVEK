package com.xxivek.tsdxxivek.api

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.xxivek.tsdxxivek.AppConstants
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Модель: один файл из списка pending-файлов.
 * Соответствует ответу GET /api/v1/exchange/download.
 */
data class PendingFileItem(
    val id: String = "",
    val sender_uuid: String = "",
    val subject: String = "",
    val metadata: Metadata? = null,
    val file_url: String = "",
    val filename: String = "",
    val status: String = "",
    val created_at: String = ""
)

/**
 * Вложенный объект metadata (типизированный).
 */
data class Metadata(
    val type: String = "",
    val date: String = ""
)

/**
 * Модель: ответ сервера на GET /api/v1/exchange/download.
 */
data class PendingFilesResponse(
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0,
    val items: List<PendingFileItem> = emptyList()
)

/**
 * Модель: ответ на GET /api/v1/devices/status (поле status).
 */
data class DeviceStatusPayload(
    val pairing: Boolean = false,
    val konf: Int = 0,
    val bd: Int = 0,
    val input: Int = 0,
    val output: Int = 0
)

/**
 * Модель: тело POST-запроса на обновление статуса устройства.
 */
data class DeviceStatusUpdate(
    val pairing: Boolean,
    val konf: Int,
    val bd: Int,
    val input: Int,
    val output: Int
)

/**
 * Модель: ответ на обновление статуса.
 */
data class StatusUpdateResponse(
    val status: String = "",
    val device_uuid: String? = null,
    val paired: Boolean? = null
)

/**
 * Результат скачивания файла.
 */
data class DownloadResult(
    val success: Boolean = false,
    val file: File? = null,
    val message: String? = null,
    val httpCode: Int = 0
)

/**
 * Результат загрузки файла на сервер.
 */
data class UploadResult(
    val status: String = "",
    val message_id: String? = null,
    val backoffice_device_uuid: String? = null
)

/**
 * Результат удаления файла с сервера.
 */
data class DeleteResult(
    val status: String = "",
    val message_id: String? = null,
    val deleted: Boolean = false
)

/**
 * Результат выгрузки данных (экспорт + загрузка).
 */
data class ExportUploadResult(
    val success: Boolean = false,
    val fileId: String? = null,
    val message: String? = null
)

/**
 * HTTP-клиент для работы с exchange API (скачивание файлов, получение списка).
 *
 * Использует OkHttp с асинхронными запросами (enqueue).
 * Все запросы включают Authorization: Bearer {device_uuid}.
 */
class FileDownloadApi {

    private companion object {
        const val TAG = "FileDownloadApi"
        const val FILE_DOWNLOAD_DIR = "downloads"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    // ----------------------------------------------------------------
    // Вспомогательные методы
    // ----------------------------------------------------------------

    /**
     * Определяет базовый URL по режиму обмена (локальный сервер или веб-сервер).
     */
    private fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val useSite = prefs.getBoolean("use_website", false)
        return if (useSite) {
            ApiClient.SITE_URL
        } else {
            val port = prefs.getInt("port", 8080)
            "http://127.0.0.1:$port"
        }
    }

    /**
     * Получает device_uuid из AppState.
     * Возвращает null, если UUID не установлен — caller должен это обработать.
     */
    private fun getDeviceUuid(context: Context): String? {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return prefs.getString(AppConstants.APP_PREF_DEVICE_UUID, null)
    }

    /**
     * Создаёт GET-запрос с Authorization заголовком.
     */
    private fun buildAuthorizedGetRequest(
        url: String,
        token: String
    ): Request {
        return Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
    }

    // ----------------------------------------------------------------
    // 1. Получение списка pending файлов
    // ----------------------------------------------------------------

    /**
     * Асинхронный запрос GET /api/v1/exchange/download.
     *
     * @param context контекст приложения
     * @param token Bearer-токен (device_uuid)
     * @param callback результат запроса
     */
    fun getPendingFiles(
        context: Context,
        token: String,
        callback: (PendingFilesResponse) -> Unit
    ) {
        val baseUrl = getBaseUrl(context)
        val url = "$baseUrl/api/v1/exchange/download"

        Log.d(TAG, "getPendingFiles: baseUrl=$baseUrl, token=$token")

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "getPendingFiles failed", e)
                // Возвращаем пустой ответ при ошибке сети
                callback(PendingFilesResponse(items = emptyList()))
            }

            override fun onResponse(call: Call, response: Response) {
                val httpCode = response.code
                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "getPendingFiles: httpCode=$httpCode, body=$responseBody")

                val result = if (response.isSuccessful && responseBody.isNotBlank()) {
                    try {
                        gson.fromJson(responseBody, PendingFilesResponse::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "parse pending files failed", e)
                        PendingFilesResponse(items = emptyList())
                    }
                } else {
                    Log.w(TAG, "getPendingFiles: unsuccessful response $httpCode")
                    PendingFilesResponse(items = emptyList())
                }

                // Результат всегда возвращается в main-thread? Нет — caller сам обрабатывает.
                // Для UI-вызовов можно переключить через Handler/CoroutineDispatcher.
                callback(result)
            }
        })
    }

    // ----------------------------------------------------------------
    // 2. Скачивание файла
    // ----------------------------------------------------------------

    /**
     * Асинхронный запрос GET /api/v1/exchange/files/{message_id}.
     * Скачивает бинарные данные и сохраняет в external storage.
     *
     * ВАЖНО: После скачивания сообщение автоматически удаляется с сервера.
     *
     * @param context контекст приложения
     * @param token Bearer-токен
     * @param message_id ID сообщения для скачивания
     * @param callback результат скачивания
     */
    fun downloadFile(
        context: Context,
        token: String,
        message_id: String,
        callback: (DownloadResult) -> Unit
    ) {
        val baseUrl = getBaseUrl(context)
        val url = "$baseUrl/api/v1/exchange/files/$message_id"

        Log.d(TAG, "downloadFile: baseUrl=$baseUrl, message_id=$message_id")

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "downloadFile failed", e)
                callback(DownloadResult(success = false, message = e.message, httpCode = -1))
            }

            override fun onResponse(call: Call, response: Response) {
                val httpCode = response.code
                Log.d(TAG, "downloadFile: httpCode=$httpCode")

                if (!response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    Log.w(TAG, "downloadFile: unsuccessful $httpCode: $body")
                    callback(DownloadResult(success = false, message = "HTTP $httpCode: $body", httpCode = httpCode))
                    response.close()
                    return
                }

                // Извлекаем оригинальное имя файла из заголовков
                val originalFilename = extractOriginalFilename(response)
                    ?: "downloaded_file"

                Log.d(TAG, "downloadFile: filename=$originalFilename, size=${response.body?.contentLength() ?: -1}")

                callback(saveResponseBodyToFile(context, response))
            }
        })
    }

    // ----------------------------------------------------------------
    // 3. Обновление статуса устройства
    // ----------------------------------------------------------------

    /**
     * Асинхронный запрос POST /api/v1/devices/status.
     *
     * @param context контекст приложения
     * @param token Bearer-токен
     * @param payload данные статуса
     * @param callback результат
     */
    fun updateDeviceStatus(
        context: Context,
        token: String,
        payload: DeviceStatusUpdate,
        callback: (StatusUpdateResponse) -> Unit
    ) {
        val baseUrl = getBaseUrl(context)
        val url = "$baseUrl/api/v1/devices/status"

        Log.d(TAG, "updateDeviceStatus: baseUrl=$baseUrl, payload=$payload")

        val json = gson.toJson(payload)
        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "updateDeviceStatus failed", e)
                callback(StatusUpdateResponse())
            }

            override fun onResponse(call: Call, response: Response) {
                val httpCode = response.code
                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "updateDeviceStatus: httpCode=$httpCode, body=$responseBody")

                val result = if (response.isSuccessful && responseBody.isNotBlank()) {
                    try {
                        gson.fromJson(responseBody, StatusUpdateResponse::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "parse status update failed", e)
                        StatusUpdateResponse()
                    }
                } else {
                    Log.w(TAG, "updateDeviceStatus: unsuccessful $httpCode")
                    StatusUpdateResponse()
                }

                callback(result)
            }
        })
    }

    // ----------------------------------------------------------------
    // 4. Получение статуса устройства (синхронная версия из ApiClient
    //    дублируется здесь для асинхронного интерфейса)
    // ----------------------------------------------------------------

    /**
     * Асинхронный запрос GET /api/v1/devices/status.
     */
    fun getDeviceStatusAsync(
        context: Context,
        token: String,
        callback: (DeviceStatusResponse) -> Unit
    ) {
        val baseUrl = getBaseUrl(context)
        val url = "$baseUrl/api/v1/devices/status"

        Log.d(TAG, "getDeviceStatusAsync: baseUrl=$baseUrl, token=$token")

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "getDeviceStatusAsync failed", e)
                callback(DeviceStatusResponse(httpCode = -1, message = e.message))
            }

            override fun onResponse(call: Call, response: Response) {
                val httpCode = response.code
                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "getDeviceStatusAsync: httpCode=$httpCode, body=$responseBody")

                val result = if (response.isSuccessful && responseBody.isNotBlank()) {
                    try {
                        val jsonResp = gson.fromJson(responseBody, JsonObject::class.java)
                        val statusObj = jsonResp.get("status")?.asJsonObject
                        DeviceStatusResponse(
                            device_uuid = jsonResp.get("device_uuid")?.asString,
                            pairing = statusObj?.get("pairing")?.asBoolean ?: false,
                            konf = statusObj?.get("konf")?.asInt ?: 0,
                            bd = statusObj?.get("bd")?.asInt ?: 0,
                            input = statusObj?.get("input")?.asInt ?: 0,
                            output = statusObj?.get("output")?.asInt ?: 0,
                            httpCode = httpCode
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "parse device status failed", e)
                        DeviceStatusResponse(httpCode = httpCode, message = "Parse error")
                    }
                } else {
                    DeviceStatusResponse(
                        pairing = false,
                        httpCode = httpCode,
                        message = "HTTP $httpCode"
                    )
                }

                callback(result)
            }
        })
    }

    // ----------------------------------------------------------------
    // Синхронные методы для использования в StatusPollingService
    // ----------------------------------------------------------------

    /**
     * Синхронный запрос GET /api/v1/devices/status.
     * Используется в polling-цикле.
     */
    fun getDeviceStatusSync(context: Context, token: String): DeviceStatusResponse {
        val baseUrl = getBaseUrl(context)
        val url = "$baseUrl/api/v1/devices/status"

        Log.d(TAG, "getDeviceStatusSync: baseUrl=$baseUrl, token=$token")

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val httpCode = response.code
                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "getDeviceStatusSync: httpCode=$httpCode, body=$responseBody")

                if (response.isSuccessful && responseBody.isNotBlank()) {
                    try {
                        val jsonResp = gson.fromJson(responseBody, JsonObject::class.java)
                        val statusObj = jsonResp.get("status")?.asJsonObject
                        DeviceStatusResponse(
                            device_uuid = jsonResp.get("device_uuid")?.asString,
                            pairing = statusObj?.get("pairing")?.asBoolean ?: false,
                            konf = statusObj?.get("konf")?.asInt ?: 0,
                            bd = statusObj?.get("bd")?.asInt ?: 0,
                            input = statusObj?.get("input")?.asInt ?: 0,
                            output = statusObj?.get("output")?.asInt ?: 0,
                            httpCode = httpCode
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "parse device status failed", e)
                        DeviceStatusResponse(httpCode = httpCode, message = "Parse error")
                    }
                } else {
                    DeviceStatusResponse(
                        pairing = false,
                        httpCode = httpCode,
                        message = "HTTP $httpCode"
                    )
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "getDeviceStatusSync failed", e)
            DeviceStatusResponse(pairing = false, httpCode = -1, message = e.message)
        }
    }

    /**
     * Синхронный запрос GET /api/v1/exchange/download.
     */
    fun getPendingFilesSync(context: Context, token: String): PendingFilesResponse {
        val baseUrl = getBaseUrl(context)
        val url = "$baseUrl/api/v1/exchange/download"

        Log.d(TAG, "getPendingFilesSync: baseUrl=$baseUrl, token=$token")

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val httpCode = response.code
                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "getPendingFilesSync: httpCode=$httpCode, body=$responseBody")

                if (response.isSuccessful && responseBody.isNotBlank()) {
                    try {
                        gson.fromJson(responseBody, PendingFilesResponse::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "parse pending files failed", e)
                        PendingFilesResponse(items = emptyList())
                    }
                } else {
                    Log.w(TAG, "getPendingFilesSync: unsuccessful response $httpCode")
                    PendingFilesResponse(items = emptyList())
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "getPendingFilesSync failed", e)
            PendingFilesResponse(items = emptyList())
        }
    }

    /**
     * Синхронное скачивание всех pending файлов.
     * @return список успешно скачанных файлов
     */
    fun downloadAllFilesSync(
        context: Context,
        token: String,
        items: List<PendingFileItem>
    ): List<File> {
        val downloaded = mutableListOf<File>()

        for (item in items) {
            val result = downloadFileSync(context, token, item.id)
            Log.d(TAG, "downloadAllFilesSync: file ${item.filename}, success=${result.success}, httpCode=${result.httpCode}")
            if (result.success && result.file != null) {
                downloaded.add(result.file)
            } else {
                Log.w(TAG, "downloadAllFilesSync: failed to download ${item.filename}: ${result.message}")
            }
        }

        Log.d(TAG, "downloadAllFilesSync: downloaded ${downloaded.size}/${items.size} files")
        return downloaded
    }

    /**
     * Извлечь оригинальное имя файла из заголовков response.
     */
    private fun extractOriginalFilename(response: Response): String? {
        return response.header("X-Original-Filename")
            ?: response.header("Content-Disposition")
                ?.substringAfter("filename=")
                ?.trim('"')
    }

    /**
     * Сохранить тело HTTP-ответа в файл. Общий метод для async/sync версий.
     */
    private fun saveResponseBodyToFile(
        context: Context,
        response: Response
    ): DownloadResult {
        val originalFilename = extractOriginalFilename(response)
            ?: "downloaded_file"

        val downloadDir = context.getExternalFilesDir(FILE_DOWNLOAD_DIR)
            ?: context.filesDir.resolve(FILE_DOWNLOAD_DIR).also { it.mkdirs() }

        val targetFile = File(downloadDir, originalFilename)

        response.body?.byteStream()?.use { inputStream ->
            FileOutputStream(targetFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        Log.d(TAG, "saveResponseBodyToFile: saved to ${targetFile.absolutePath}")

        return DownloadResult(success = true, file = targetFile, httpCode = response.code)
    }

    /**
     * Синхронное скачивание одного файла.
     */
    fun downloadFileSync(
        context: Context,
        token: String,
        message_id: String
    ): DownloadResult {
        val baseUrl = getBaseUrl(context)
        val url = "$baseUrl/api/v1/exchange/files/$message_id"

        Log.d(TAG, "downloadFileSync: baseUrl=$baseUrl, message_id=$message_id")

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val httpCode = response.code

                if (!response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    Log.w(TAG, "downloadFileSync: unsuccessful $httpCode: $body")
                    DownloadResult(success = false, message = "HTTP $httpCode: $body", httpCode = httpCode)
                } else {
                    saveResponseBodyToFile(context, response)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "downloadFileSync failed", e)
            DownloadResult(success = false, message = e.message, httpCode = -1)
        }
    }

    /**
     * Синхронное обновление статуса устройства.
     */
    fun updateDeviceStatusSync(
        context: Context,
        token: String,
        payload: DeviceStatusUpdate
    ): StatusUpdateResponse {
        val baseUrl = getBaseUrl(context)
        val url = "$baseUrl/api/v1/devices/status"

        Log.d(TAG, "updateDeviceStatusSync: baseUrl=$baseUrl, payload=$payload")

        val json = gson.toJson(payload)
        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val httpCode = response.code
                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "updateDeviceStatusSync: httpCode=$httpCode, body=$responseBody")

                if (response.isSuccessful && responseBody.isNotBlank()) {
                    try {
                        gson.fromJson(responseBody, StatusUpdateResponse::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "parse status update failed", e)
                        StatusUpdateResponse()
                    }
                } else {
                    Log.w(TAG, "updateDeviceStatusSync: unsuccessful $httpCode")
                    StatusUpdateResponse()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "updateDeviceStatusSync failed", e)
            StatusUpdateResponse()
        }
    }

    // ----------------------------------------------------------------
    // 4. Загрузка результатов (JSON body)
    // ----------------------------------------------------------------

    /**
     * Синхронная загрузка результатов на сервер.
     * POST /api/v1/exchange/upload
     *
     * Отправляет JSON-тело с содержимым файла как строкой (без Base64).
     * Формат тела:
     * {
     *   "filename": "Output_12345.xml",
     *   "content": "<содержимое файла>",
     *   "metadata": {"type":"inventory_result", ...}
     * }
     *
     * @param context контекст приложения
     * @param token Bearer-токен
     * @param file файл с результатами
     * @param metadata JSON-строка с метаданными (type, task_id, device_uuid, sent_at)
     * @return результат загрузки
     */
    fun uploadResult(
        context: Context,
        token: String,
        file: File,
        metadata: String
    ): UploadResult {
        val baseUrl = getBaseUrl(context)
        val url = "$baseUrl/api/v1/exchange/upload"

        Log.d(TAG, "uploadResult: baseUrl=$baseUrl, file=${file.absolutePath}")

        // Читаем файл как строку (UTF-8) — "сырой" JSON, как в 1С
        val fileContent = file.readText()

        Log.d(TAG, "uploadResult: fileContent = $fileContent")

        // Тело запроса = содержимое файла напрямую (без обёртки)
        val requestBody = fileContent.toRequestBody("application/json; charset=UTF-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("X-Filename", file.name)
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val httpCode = response.code
                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "uploadResult: httpCode=$httpCode, body=$responseBody")

                if (response.isSuccessful && responseBody.isNotBlank()) {
                    try {
                        gson.fromJson(responseBody, UploadResult::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "parse upload result failed", e)
                        UploadResult()
                    }
                } else {
                    Log.w(TAG, "uploadResult: unsuccessful $httpCode: $responseBody")
                    UploadResult()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "uploadResult failed", e)
            UploadResult()
        }
    }

    // ----------------------------------------------------------------
    // 6. Выгрузка данных из БД ТСД на сервер
    // ----------------------------------------------------------------

    /**
     * Экспорт данных из БД и загрузка на сервер.
     * 1. Экспортирует записи с quantity > 0 в JSON-файл.
     * 2. Загружает файл на сервер через POST /api/v1/exchange/upload.
     *
     * @param context контекст приложения
     * @param token Bearer-токен
     * @param oper значение oper (из appLic.appOper)
     * @param client значение client (из appLic.appClient)
     * @return результат выгрузки
     */
    fun exportAndUpload(
        context: Context,
        token: String,
        oper: String,
        client: String
    ): ExportUploadResult {
        // Шаг 1: Экспорт в JSON
        val exportResult = JsonExport.exportToJSON(context, oper, client, token)

        if (!exportResult.success || exportResult.file == null) {
            return ExportUploadResult(
                success = false,
                message = "Экспорт не удался: ${exportResult.message}"
            )
        }

        Log.d(TAG, "exportAndUpload: JSON создан ${exportResult.file.absolutePath}")

        // Шаг 2: Подготовка message для загрузки
        val metadata = gson.toJson(
            mapOf(
                "type" to "inventory_result",
                "task_id" to "",
                "device_uuid" to token,
                "sent_at" to java.text.SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss",
                    java.util.Locale.getDefault()
                ).format(System.currentTimeMillis())
            )
        )

        // Шаг 3: Загрузка файла
        val uploadResult = uploadResult(context, token, exportResult.file, metadata)

        Log.d(TAG, "exportAndUpload: uploadResult status=${uploadResult.status}, fileId=${uploadResult.message_id}")

        if (uploadResult.status == "success") {
            return ExportUploadResult(
                success = true,
                fileId = uploadResult.message_id,
                message = "Выгрузка успешна"
            )
        }

        return ExportUploadResult(
            success = false,
            message = "Загрузка не удалась: статус=${uploadResult.status}"
        )
    }

    // ----------------------------------------------------------------
    // 5. Удаление файла задания
    // ----------------------------------------------------------------

    /**
     * Синхронное удаление файла задания с сервера.
     * DELETE /api/v1/exchange/files/{message_id}
     *
     * @param context контекст приложения
     * @param token Bearer-токен
     * @param message_id ID сообщения для удаления
     * @return результат удаления
     */
    fun deleteFile(
        context: Context,
        token: String,
        message_id: String
    ): DeleteResult {
        val baseUrl = getBaseUrl(context)
        val url = "$baseUrl/api/v1/exchange/files/$message_id"

        Log.d(TAG, "deleteFile: baseUrl=$baseUrl, message_id=$message_id")

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .delete()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val httpCode = response.code
                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "deleteFile: httpCode=$httpCode, body=$responseBody")

                if (response.isSuccessful && responseBody.isNotBlank()) {
                    try {
                        gson.fromJson(responseBody, DeleteResult::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "parse delete result failed", e)
                        DeleteResult()
                    }
                } else {
                    Log.w(TAG, "deleteFile: unsuccessful $httpCode")
                    DeleteResult()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "deleteFile failed", e)
            DeleteResult()
        }
    }

    // ----------------------------------------------------------------
    // 4b. Загрузка результатов (multipart/form-data)
    // ----------------------------------------------------------------

    /**
     * Синхронная загрузка результатов через multipart/form-data.
     * POST /api/v1/exchange/upload
     *
     * Формат: multipart/form-data с двумя частями:
     * - file: файл с результатами (бинарный)
     * - message: JSON-метаданные (type, task_id, sent_at)
     *
     * @param context контекст приложения
     * @param token Bearer-токен
     * @param file файл с результатами
     * @param metadata JSON-строка с метаданными
     * @return результат загрузки
     */
    fun uploadResultMultipart(
        context: Context,
        token: String,
        file: File,
        metadata: String
    ): UploadResult {
        val baseUrl = getBaseUrl(context)
        val url = "$baseUrl/api/v1/exchange/upload"

        Log.d(TAG, "uploadResultMultipart: baseUrl=$baseUrl, file=${file.absolutePath}")

        val mediaType = "application/json".toMediaType()

        val requestBody = okhttp3.MultipartBody.Builder()
            .setType(okhttp3.MultipartBody.FORM)
            .addPart(
                okhttp3.Headers.headersOf("Content-Disposition", "form-data; name=\"file\"; filename=\"${file.name}\""),
                RequestBody.create(mediaType, file.readText())
            )
            .addPart(
                okhttp3.Headers.headersOf("Content-Disposition", "form-data; name=\"message\""),
                RequestBody.create("text/plain".toMediaType(), metadata)
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val httpCode = response.code
                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "uploadResultMultipart: httpCode=$httpCode, body=$responseBody")

                if (response.isSuccessful && responseBody.isNotBlank()) {
                    try {
                        gson.fromJson(responseBody, UploadResult::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "parse upload result failed", e)
                        UploadResult()
                    }
                } else {
                    Log.w(TAG, "uploadResultMultipart: unsuccessful $httpCode: $responseBody")
                    UploadResult()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "uploadResultMultipart failed", e)
            UploadResult()
        }
    }
}
