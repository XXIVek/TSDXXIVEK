package com.xxivek.tsdxxivek.api

import android.content.Context
import android.util.Log
import com.xxivek.tsdxxivek.AppConstants
import com.xxivek.tsdxxivek.AppState
import kotlinx.coroutines.*
import java.io.File

/**
 * Сервис периодического опроса статуса устройства с веб-сервера.
 *
 * Запускается после активации устройства, опрашивает статус каждые 5 секунд.
 * При обнаружении input=6 автоматически скачивает pending файлы.
 */
class StatusPollingService {

    companion object {
        const val TAG = "StatusPolling"
        const val POLL_INTERVAL_MS = 5000L // 5 секунд
    }

    private var coroutineScope: CoroutineScope? = null
    private var isRunning = false
    private var hasPendingData = false

    // Callback для уведомления о событиях
    interface Callback {
        /** Статус устройства обновлён */
        fun onStatusUpdated(input: Int, output: Int)

        /** Получены pending файлы для скачивания (input=6) */
        fun onPendingDataAvailable(fileItems: List<PendingFileItem>)

        /** Файлы скачаны и готовы к загрузке в БД */
        fun onFilesDownloaded(downloadedFiles: List<File>)

        /** Ошибка опроса */
        fun onPollError(message: String)
    }

    private var callback: Callback? = null

    /**
     * Установить callback для получения событий.
     */
    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    /**
     * Запустить опрос статуса.
     *
     * @param context контекст приложения
     * @param appState глобальное состояние приложения
     * @param appLic лицензия (для обновления LiveData appInfoBD)
     * @param api клиент для API-запросов
     */
    fun startPolling(
        context: Context,
        appState: AppState,
        appLic: com.xxivek.tsdxxivek.utilAPP.LicenseUtil,
        api: FileDownloadApi
    ) {
        if (isRunning) {
            Log.w(TAG, "startPolling: already running")
            return
        }

        isRunning = true
        coroutineScope = CoroutineScope(Dispatchers.IO + CoroutineName("StatusPolling"))

        Log.d(TAG, "startPolling: polling started")

        // Запускаем бесконечный цикл опроса
        coroutineScope!!.launch {
            while (isRunning && coroutineScope!!.isActive) {
                try {
                    // Получаем device_uuid (токен)
                    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    val token = prefs.getString(AppConstants.APP_PREF_DEVICE_UUID, null)

                    if (token.isNullOrEmpty()) {
                        Log.w(TAG, "startPolling: no token, skipping")
                        delay(POLL_INTERVAL_MS)
                        continue
                    }

                    // Опрашиваем статус
                    Log.d(TAG, "polling: sending GET /api/v1/devices/status, token=${token.take(8)}...${token.takeLast(8)} (len=${token.length})")
                    val status = api.getDeviceStatusSync(context, token)

                    if (status.httpCode == 200 && status.device_uuid != null) {
                        val input = status.input
                        val output = status.output

                        Log.d(TAG, "polling: status received - input=$input, output=$output, httpCode=${status.httpCode}")
                        Log.d(TAG, "polling: hasPendingData=$hasPendingData, device_uuid=${status.device_uuid}")

                        // Обновляем AppState
                        appState.updateServerStatus(input, output)

                        // Обновляем LiveData для UI (статус выгрузки)
                        appLic.appInfoOUT.postValue(output)

                        // Уведомляем callback
                        callback?.onStatusUpdated(input, output)

                        Log.d(TAG, "polling: callback onStatusUpdated called with input=$input, output=$output")

                        // ----------------------------------------------------------------
                        // Проверка статуса bd по данным БД (асинхронно)
                        // ----------------------------------------------------------------
                        coroutineScope!!.launch(Dispatchers.IO) {
                            try {
                                val dao = (context.applicationContext as? com.xxivek.tsdxxivek.TSDXXIVekApplication)
                                    ?.database?.itemDao()

                                var calculatedBd = -1
                                var dbCountNotEmpty = 0
                                var dbTotalCount = 0

                                if (dao != null) {
                                    dbCountNotEmpty = dao.getCountNotEmpty()
                                    dbTotalCount = dao.getCount()

                                    // Определяем текущий bd по данным БД
                                    calculatedBd = if (dbTotalCount > 0) {
                                        if (dbCountNotEmpty > 0) 2 else 3
                                    } else {
                                        0
                                    }

                                    val currentBd = appState.appBd
                                    Log.d(TAG, "bd check: calculated=$calculatedBd, appState.appBd=$currentBd, countNotEmpty=$dbCountNotEmpty, totalCount=$dbTotalCount")

                                    // Обновляем appState.appBd если изменился
                                    if (calculatedBd != currentBd) {
                                        Log.d(TAG, "bd changed from $currentBd to $calculatedBd")
                                        appState.appBd = calculatedBd

                                        // Отправляем на сервер
                                        val updatePayload = DeviceStatusUpdate(
                                            pairing = true,
                                            konf = appState.appKONF.toIntOrNull() ?: 0,
                                            bd = calculatedBd,
                                            input = input,
                                            output = output
                                        )
                                        val updateResult = api.updateDeviceStatusSync(context, token, updatePayload)
                                        Log.d(TAG, "updateDeviceStatusSync bd=$calculatedBd, result=${updateResult.status}")
                                    }
                                }

                                // Обновляем LiveData в MainThread
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    Log.d(TAG, "LiveData update: bd=$calculatedBd, countNotEmpty=$dbCountNotEmpty, countBD=$dbTotalCount")
                                    appLic.appInfoBD.postValue(calculatedBd)
                                    appLic.appInfoCountNotEmptyBD.postValue(dbCountNotEmpty)
                                    appLic.appInfoCountBD.postValue(dbTotalCount)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "bd check failed", e)
                            }
                        }

                    // Реагируем на input=6 — есть данные для загрузки
                    if (input == 6) {
                        Log.d(TAG, "polling: input=6 detected, downloading files")

                        // Получаем список pending файлов
                        val pendingFiles = api.getPendingFilesSync(context, token)
                        Log.d(TAG, "polling: getPendingFiles returned total=${pendingFiles.total}, items=${pendingFiles.items.size}")

                        if (pendingFiles.items.isNotEmpty()) {
                            Log.d(TAG, "polling: downloading ${pendingFiles.items.size} files")
                            callback?.onPendingDataAvailable(pendingFiles.items)

                            // Скачиваем файлы
                            val downloaded = api.downloadAllFilesSync(
                                context, token, pendingFiles.items
                            )

                            Log.d(TAG, "polling: downloaded ${downloaded.size} files")
                            for (f in downloaded) {
                                Log.d(TAG, "polling:   file: ${f.absolutePath}, size=${f.length()}")
                            }
                            callback?.onFilesDownloaded(downloaded)

                            // Удаляем скачанные файлы с сервера по UUID
                            for (item in pendingFiles.items) {
                                Log.d(TAG, "polling: deleting server file ${item.id}")
                                val deleteResult = api.deleteFile(context, token, item.id)
                                Log.d(TAG, "polling: deleteFile(${item.id}): status=${deleteResult.status}, deleted=${deleteResult.deleted}")
                            }

                            // Отправляем input=3 на сервер (файлы приняты, удалены с сервера)
                            Log.d(TAG, "polling: sending input=3 to server")
                            val updatePayload = DeviceStatusUpdate(
                                pairing = true,
                                konf = appState.appKONF.toIntOrNull() ?: 0,
                                bd = 0,
                                input = 3,
                                output = output
                            )
                            val statusUpdateResult = api.updateDeviceStatusSync(context, token, updatePayload)
                            Log.d(TAG, "polling: updateDeviceStatusSync result: ${statusUpdateResult.status}, paired=${statusUpdateResult.paired}")
                            appState.updateServerStatus(3, output)
                            callback?.onStatusUpdated(3, output)
                        } else {
                            Log.w(TAG, "polling: no pending files but input=6")
                        }
                    } else if (input == 0) {
                        // Данные загружены в БД — сбрасываем hasPendingData и показываем UI
                        Log.d(TAG, "polling: input=0, resetting state")
                        hasPendingData = false
                        appState.hasPendingData = false
                        callback?.onStatusUpdated(input, output)
                    } else {
                        Log.d(TAG, "polling: input=$input (not 6, ignoring)")
                    }
                    } else {
                        Log.w(TAG, "polling: unsuccessful status response, httpCode=${status.httpCode}, uuid=${status.device_uuid}")
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "polling: error", e)
                    callback?.onPollError(e.message ?: "Unknown error")
                }

                // Ждем до следующего опроса
                delay(POLL_INTERVAL_MS)
            }
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
