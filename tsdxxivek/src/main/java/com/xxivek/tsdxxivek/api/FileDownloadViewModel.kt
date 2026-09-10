package com.xxivek.tsdxxivek.api

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.xxivek.tsdxxivek.AppConstants
import com.xxivek.tsdxxivek.TSDXXIVekApplication

/**
 * ViewModel для потока скачивания файлов с сервера.
 *
 * Поток работы:
 * 1. getPendingFiles() — получить список pending файлов
 * 2. Пользователь выбирает файл из списка
 * 3. downloadSelectedFile() — скачать файл и сохранить на устройство
 * 4. updateDeviceStatus() — обновить статус устройства
 */
class FileDownloadViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val TAG = "FileDownloadViewModel"
    }

    private val api = FileDownloadApi()

    // ---------- LiveData для UI ----------

    /** Список pending файлов */
    private val _pendingFiles = MutableLiveData<List<PendingFileItem>>()
    val pendingFiles: LiveData<List<PendingFileItem>> = _pendingFiles

    /** Текущий скачиваемый файл (имя) */
    private val _downloadingFile = MutableLiveData<String?>()
    val downloadingFile: LiveData<String?> = _downloadingFile

    /** Сообщение об успехе */
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    /** Сообщение об ошибке */
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /** Индикатор загрузки */
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    /** Текущий статус устройства */
    private val _deviceStatus = MutableLiveData<DeviceStatusResponse?>()
    val deviceStatus: LiveData<DeviceStatusResponse?> = _deviceStatus

    // ---------- Методы ----------

    /**
     * Получить список pending файлов с сервера.
     * Проверяет наличие device_uuid перед запросом.
     */
    fun getPendingFiles() {
        val context = getApplication<Application>()
        val token = getApplication<Application>()
            .getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString(AppConstants.APP_PREF_LIC, null)

        if (token.isNullOrEmpty()) {
            _errorMessage.value = "device_uuid не найден. Сначала выполните активацию."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        api.getPendingFiles(context, token) { response ->
            _pendingFiles.value = response.items
            _isLoading.value = false

            if (response.items.isEmpty()) {
                _successMessage.value = "Нет pending файлов"
            } else {
                _successMessage.value = "Получено файлов: ${response.items.size}"
            }
        }
    }

    /**
     * Скачать выбранный файл и сохранить на устройство.
     * После скачивания файл и сообщение удаляются с сервера.
     */
    fun downloadSelectedFile(messageId: String, filename: String) {
        val context = getApplication<Application>()
        val token = getApplication<Application>()
            .getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString(AppConstants.APP_PREF_LIC, null)

        if (token.isNullOrEmpty()) {
            _errorMessage.value = "device_uuid не найден."
            return
        }

        _downloadingFile.value = filename
        _errorMessage.value = null

        api.downloadFile(context, token, messageId) { result ->
            _downloadingFile.value = null

            if (result.success) {
                _successMessage.value = "Файл сохранён: ${result.file?.absolutePath}"
            } else {
                _errorMessage.value = "Ошибка скачивания: ${result.message}"
            }
        }
    }

    /**
     * Обновить статус устройства (input/output).
     */
    fun updateDeviceStatus(input: Int, output: Int) {
        val context = getApplication<Application>()
        val token = getApplication<Application>()
            .getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString(AppConstants.APP_PREF_LIC, null)

        if (token.isNullOrEmpty()) {
            _errorMessage.value = "device_uuid не найден."
            return
        }

        _isLoading.value = true

        // Читаем текущий статус из AppState
        val appState = getApplication<TSDXXIVekApplication>().appState
        val payload = DeviceStatusUpdate(
            pairing = appState.appLIC != "-1",
            konf = appState.appKONF.toIntOrNull() ?: 0,
            bd = 0,
            input = input,
            output = output
        )

        api.updateDeviceStatus(context, token, payload) { result ->
            _isLoading.value = false
            if (result.status == "ok") {
                _successMessage.value = "Статус устройства обновлён"
            } else {
                _errorMessage.value = "Ошибка обновления статуса"
            }
        }
    }

    /**
     * Получить текущий статус устройства с сервера.
     */
    fun fetchDeviceStatus() {
        val context = getApplication<Application>()
        val token = getApplication<Application>()
            .getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString(AppConstants.APP_PREF_LIC, null)

        if (token.isNullOrEmpty()) {
            _deviceStatus.value = null
            return
        }

        _isLoading.value = true

        api.getDeviceStatusAsync(context, token) { response ->
            _deviceStatus.value = response
            _isLoading.value = false
        }
    }

    /**
     * Очистить сообщения (вызывать из XML-файла в onAttachedToWindow).
     */
    fun clearMessages() {
        _successMessage.value = null
        _errorMessage.value = null
    }
}
