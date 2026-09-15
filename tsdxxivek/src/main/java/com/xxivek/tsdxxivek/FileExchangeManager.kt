package com.xxivek.tsdxxivek

import android.content.Context
import android.util.Log
import com.xxivek.tsdxxivek.dataDB.Item
import com.xxivek.tsdxxivek.dataDB.ItemRoomDatabase
import com.xxivek.tsdxxivek.utilAPP.appendLog
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import java.io.*
import java.util.concurrent.Executors

/**
 * Менеджер файлового обмена с 1С/ТСД.
 * Работает через папку /storage/emulated/0/Download/TSD/
 * Файлы: tsd_dev_status.txt, tsd_Input.json, tsd_Output.json
 */
class FileExchangeManager(
    private val context: Context,
    private val usbMode: Boolean = false
) {

    companion object {
        private const val TAG = "FileExchangeManager"
    }

    val exchangeDir = File(AppConstants.FILE_EXCHANGE_DIR)
    private val gson = Gson()
    private val executor = Executors.newSingleThreadExecutor()

    // Callback для уведомлений о изменениях
    var onStatusChanged: ((DeviceStatus) -> Unit)? = null
    var onInputFileReady: (() -> Unit)? = null
    var onOutputFileReady: (() -> Unit)? = null
    var onExportComplete: ((Boolean, String) -> Unit)? = null
    var onImportComplete: ((Boolean, String) -> Unit)? = null

    // Флаг: если true — не писать tsd_dev_status.txt (режим WIFI)
    var skipStatusFile = false

    // Текущий статус
    private var currentStatus = DeviceStatus.notPaired()

    // Текущие oper и client (из настроек)
    var appOper: String = ""
    var appClient: String = ""

    /**
     * Инициализация pairing/konf из SharedPreferences.
     * Вызывается при создании FileExchangeManager для восстановления состояния.
     */
    fun initFromPrefs() {
        try {
            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val konfStr = prefs.getString(AppConstants.APP_PREF_KONF, null)
            val konf = konfStr?.toIntOrNull() ?: currentStatus.konf

            // Проверяем сопряжение по лицензии
            val lic = prefs.getString(AppConstants.APP_PREF_LIC, "-1") ?: "-1"
            val paired = (lic != null && lic != "-1" && konf >= 0)

            if (paired) {
                currentStatus = DeviceStatus(true, konf, currentStatus.bd, currentStatus.input, currentStatus.output)
                Log.d(TAG, "initFromPrefs: сопряжение восстановлено — pairing=true, konf=$konf")
            } else {
                currentStatus = DeviceStatus(false, 0, currentStatus.bd, currentStatus.input, currentStatus.output)
                Log.d(TAG, "initFromPrefs: сопряжение отсутствует")
            }
        } catch (e: Exception) {
            Log.e(TAG, "initFromPrefs: ошибка", e)
        }
    }

    /**
     * Восстановить полный статус из tsd_dev_status.txt или SharedPreferences.
     * Вызывается при создании FileExchangeManager для полного восстановления состояния.
     */
    fun initFullStatus() {
        try {
            // Сначала пробуем прочитать из файла статуса
            val statusFile = File(exchangeDir, AppConstants.FILE_DEV_STATUS)
            if (statusFile.exists() && statusFile.canRead() && statusFile.length() > 0L) {
                val ssv = statusFile.readText().trim()
                val status = DeviceStatus.fromSsv(ssv)
                if (status != null) {
                    // Для USB-режима игнорируем pairing и konf из файла
                    val finalStatus = if (usbMode) {
                        status.copy(
                            pairing = currentStatus.pairing,
                            konf = currentStatus.konf
                        )
                    } else {
                        status
                    }
                    currentStatus = finalStatus
                    Log.d(TAG, "initFullStatus: статус восстановлен из файла — ${DeviceStatus.toSsv(finalStatus)}")
                    return
                }
            }

            // Если файл не найден (WIFI-режим), восстанавливаем только pairing/konf
            initFromPrefs()
        } catch (e: Exception) {
            Log.e(TAG, "initFullStatus: ошибка", e)
            initFromPrefs()
        }
    }


    /**
     * Получить konf из SharedPreferences (только для USB-режима).
     * Если не найден — возвращает текущий konf из currentStatus.
     */
    private fun getKonfFromPrefs(): Int {
        if (!usbMode) return currentStatus.konf
        return try {
            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            prefs.getString(AppConstants.APP_PREF_KONF, null)?.toIntOrNull() ?: currentStatus.konf
        } catch (e: Exception) {
            currentStatus.konf
        }
    }

    /**
     * Проверить и создать папку обмена.
     */
    fun ensureExchangeDir(): Boolean {
        Log.d(TAG, "ensureExchangeDir: exchangeDir=${exchangeDir.absolutePath}, exists=${exchangeDir.exists()}")
        if (!exchangeDir.exists()) {
            val created = exchangeDir.mkdirs()
            Log.d(TAG, "ensureExchangeDir: mkdirs result=$created, existsAfter=${exchangeDir.exists()}")
            if (created) {
                Log.d(TAG, "Папка обмена создана: ${exchangeDir.absolutePath}")
            } else {
                Log.e(TAG, "Не удалось создать папку обмена: ${exchangeDir.absolutePath}")
            }
            return created
        }

        return true
    }

    /**
     * Прочитать статус из dev_status.txt.
     * Формат SSV: pairing;konf;bd;input;output
     * Возвращает DeviceStatus или null при ошибке.
     */
    fun readStatus(): DeviceStatus? {
        val statusFile = File(exchangeDir, AppConstants.FILE_DEV_STATUS)
        if (!statusFile.exists()) {
            return null
        }

        // Файл создан ADB — права root, приложение не может читать
        if (!statusFile.canRead()) {
            Log.w(TAG, "readStatus: файл недоступен для чтения, удаляем ${statusFile.absolutePath}")
            statusFile.delete()
            return null
        }

        // Файл пустой (длина 0) — удаляем
        if (statusFile.length() == 0L) {
            Log.w(TAG, "readStatus: файл пустой (length=0), удаляем ${statusFile.absolutePath}")
            statusFile.delete()
            return null
        }

        return try {
            val ssv = statusFile.readText().trim()
            val status = DeviceStatus.fromSsv(ssv)
            if (status != null) {
                // Для USB-режима игнорируем pairing и konf из файла
                val finalStatus = if (usbMode) {
                    status.copy(
                        pairing = true,
                        konf = getKonfFromPrefs()
                    )
                } else {
                    status
                }
                currentStatus = finalStatus
                Log.d(TAG, "Статус прочитан: pairing=${finalStatus.pairing}, konf=${finalStatus.konf}, bd=${finalStatus.bd}, input=${finalStatus.input}, output=${finalStatus.output}")
                finalStatus
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка чтения статуса", e)
            null
        }
    }

    /**
     * Записать статус в dev_status.txt.
     * Формат SSV: pairing;konf;bd;input;output
     */
    fun writeStatus(status: DeviceStatus): FileExchangeResult {
        // В режиме WIFI статус передаётся через HTTP-эндпоинт, файл не нужен
        if (skipStatusFile) {
            Log.d(TAG, "writeStatus: пропуск записи (skipStatusFile=true)")
            currentStatus = status
            return FileExchangeResult(true, "Статус обновлён (память)")
        }

        val statusFile = File(exchangeDir, AppConstants.FILE_DEV_STATUS)
        Log.d(TAG, "writeStatus: dirExists=${exchangeDir.exists()}, dirCanWrite=${exchangeDir.canWrite()}, file=${statusFile.absolutePath}")
        if (!exchangeDir.exists()) {
            val created = exchangeDir.mkdirs()
            Log.d(TAG, "writeStatus: mkdirs result=$created, dirExistsNow=${exchangeDir.exists()}")
        }

        return try {
            // Для USB-режима игнорируем pairing и konf из status
            val finalStatus = if (usbMode) {
                status.copy(
                    pairing = true,
                    konf = getKonfFromPrefs()
                )
            } else {
                status
            }
            val ssv = DeviceStatus.toSsv(finalStatus)
            Log.d(TAG, "writeStatus: writing SSV=$ssv")
            
            // Проверяем содержимое папки
            val files = exchangeDir.listFiles()
            Log.d(TAG, "writeStatus: exchangeDir files count=${files?.size}, files=${files?.joinToString { "${it.name}(isDir=${it.isDirectory})" }}")
            
            // Проверяем tsd_dev_status.txt
            if (statusFile.exists()) {
                Log.w(TAG, "writeStatus: tsd_dev_status.txt exists! isDir=${statusFile.isDirectory}, isFile=${statusFile.isFile}")
                if (statusFile.isDirectory) {
                    Log.w(TAG, "writeStatus: tsd_dev_status.txt is a directory! Deleting recursively")
                    statusFile.deleteRecursively()
                } else {
                    val deleted = statusFile.delete()
                    Log.d(TAG, "writeStatus: delete result=$deleted")
                }
            }
            
            // Записываем файл через writeText — надёжнее, чем createNewFile + FileWriter
            // на Android с scoped storage
            Log.d(TAG, "writeStatus: writing SSV=$ssv to ${statusFile.absolutePath}")
            statusFile.writeText(ssv)
            
            Log.d(TAG, "writeStatus: writeText OK, fileExists=${statusFile.exists()}, length=${statusFile.length()}")
            currentStatus = finalStatus
            Log.d(TAG, "Статус записан: $ssv, bytes=${ssv.length}")
            appendLog("FileExchangeManager", "Статус записан: $ssv")
            FileExchangeResult(true, "Статус записан")
        } catch (e: Exception) {
            val msg = "Ошибка записи статуса: ${e.javaClass.simpleName} - ${e.message}"
            Log.e(TAG, "Ошибка записи статуса", e)
            e.printStackTrace()
            appendLog("FileExchangeManager", msg)
            FileExchangeResult(false, msg)
        }
    }

    /**
     * Проверить наличие Input.json (данные готовы для загрузки).
     */
    fun hasInputFile(): Boolean {
        val file = File(exchangeDir, AppConstants.FILE_INPUT_JSON)
        return file.exists() && file.length() > 0
    }

    /**
     * Проверить наличие Output.json (данные готовы для получения).
     */
    fun hasOutputFile(): Boolean {
        val file = File(exchangeDir, AppConstants.FILE_OUTPUT_JSON)
        return file.exists() && file.length() > 0
    }

    /**
     * Прочитать Input.json и загрузить данные в БД.
     */
    suspend fun readInputAndImport(): FileExchangeResult {
        val inputFile = File(exchangeDir, AppConstants.FILE_INPUT_JSON)
        if (!inputFile.exists()) {
            return FileExchangeResult(false, "Input.json не найден")
        }

        return try {
            val json = inputFile.readText()
            val jsonObject = JsonParser.parseString(json).asJsonObject

            // Читаем oper, client и konf из JSON
            val oper = jsonObject.get("oper")?.asString ?: ""
            val client = jsonObject.get("client")?.asString ?: ""

            // Сохраняем в поля менеджера для доступа из MenuFragment
            appOper = oper
            appClient = client

            // Парсим данные
            val items = parseJsonData(jsonObject)

            // Очищаем БД и загружаем
            val app = context.applicationContext as? com.xxivek.tsdxxivek.TSDXXIVekApplication
            val dao = app?.database?.itemDao()
            if (dao != null) {
                dao.deleteAll()
                dao.insertList(items)
            }

            // Определяем bd по факту после импорта
            val total = dao?.getCount() ?: 0
            val notEmpty = dao?.getCountNotEmpty() ?: 0
            val bd = if (notEmpty > 0) 2 else if (total > 0) 3 else 0

            // Обновляем статус: input=0, bd по факту
            val newStatus = DeviceStatus(
                pairing = currentStatus.pairing,
                konf = currentStatus.konf,
                bd = bd,
                input = 0,
                output = currentStatus.output
            )
            if (usbMode) {
                writeStatus(newStatus)
            } else {
                // Для WIFI-режима не пишем файл статуса — статус передаётся через HTTP
                currentStatus = newStatus
            }

            // Удаляем Input.json после загрузки
            inputFile.delete()

            Log.d(TAG, "Импорт завершен: ${items.size} записей из Input.json")
            appendLog("Файловый обмен", "Импорт: ${items.size} записей")

            // Вызываем callback onImportComplete
            onImportComplete?.invoke(true, "Импортировано ${items.size} записей")

            FileExchangeResult(true, "Импортировано ${items.size} записей")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка импорта", e)
            FileExchangeResult(false, "Ошибка импорта: ${e.message}")
        }
    }

    /**
     * Сформировать Output.json и записать в папку обмена.
     */
    fun writeOutputAndExport(): FileExchangeResult {
        return try {
            // Получаем данные из БД
            val app = context.applicationContext as? com.xxivek.tsdxxivek.TSDXXIVekApplication
            val dao = app?.database?.itemDao()
            val items = dao?.getItemNotEmpty2() ?: emptyList()

            if (items.isEmpty()) {
                return FileExchangeResult(false, "Нет данных для выгрузки")
            }

            // Формируем JSON вручную
            val jsonStr = buildExportJson(items)
            val outputFile = File(exchangeDir, AppConstants.FILE_OUTPUT_JSON)
            outputFile.writeText(jsonStr)

            // Определяем bd по факту из БД
            val total = runBlocking { dao?.getCount() ?: 0 }
            val notEmpty = runBlocking { dao?.getCountNotEmpty() ?: 0 }
            val bd = if (notEmpty > 0) 2 else if (total > 0) 3 else 0

            // Читаем текущий статус из файла, чтобы сохранить pairing и konf
            val existingStatus = readStatus()
            val pairing = existingStatus?.pairing ?: currentStatus.pairing
            val konf = existingStatus?.konf ?: currentStatus.konf
            val input = existingStatus?.input ?: currentStatus.input

            // Обновляем статус: output=3, bd по факту
            if (usbMode) {
                writeStatus(DeviceStatus(
                    pairing = pairing,
                    konf = konf,
                    bd = bd,
                    input = input,
                    output = 3
                ))
            } else {
                // Для WIFI-режима не пишем файл статуса — статус передаётся через HTTP
                currentStatus = currentStatus.copy(
                    bd = bd,
                    output = 3
                )
            }

            // Вызываем callback onExportComplete
            onExportComplete?.invoke(true, "Выгружено ${items.size} записей")

            Log.d(TAG, "Экспорт: ${items.size} записей в Output.json")
            appendLog("Файловый обмен", "Экспорт: ${items.size} записей")

            FileExchangeResult(true, "Выгружено ${items.size} записей")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка экспорта", e)
            FileExchangeResult(false, "Ошибка экспорта: ${e.message}")
        }
    }

    /**
     * Построить JSON для выгрузки вручную.
     * Формат идентичен Input.json: {oper, client, data: [{Shtrih, ShtrihTip, TovNaim, TovCena, TovKol}]}
     */
    private fun buildExportJson(items: List<Item>): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"oper\": \"${appOper}\",\n")
        sb.append("  \"client\": \"${appClient}\",\n")
        sb.append("  \"data\": [\n")

        items.forEachIndexed { index, item ->
            // ShtrihTip — число в JSON (не строка)
            sb.append("    {\n")
            sb.append("      \"Shtrih\": \"${item.itemSh}\",\n")
            sb.append("      \"ShtrihTip\": ${item.itemShTip},\n")
            sb.append("      \"TovNaim\": \"${escapeJson(item.itemName)}\",\n")
            sb.append("      \"TovCena\": ${item.itemPrice},\n")
            sb.append("      \"TovKol\": ${item.itemQuantity}\n")
            sb.append("    }")
            if (index < items.size - 1) sb.append(",")
            sb.append("\n")
        }

        sb.append("  ]\n")
        sb.append("}\n")
        return sb.toString()
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * Распарсить данные из JSON в список Item.
     * Формат Input.json: {Shtrih, ShtrihTip, TovNaim, TovCena, TovKol}
     */
    private fun parseJsonData(jsonObject: JsonObject): List<Item> {
        val items = mutableListOf<Item>()
        val data = jsonObject.getAsJsonArray("data")

        data?.forEach { jsonElement ->
            val obj = jsonElement.asJsonObject
            val shtrih = obj.get("Shtrih")?.asString ?: ""
            val shtrihTip = obj.get("ShtrihTip")?.asInt ?: 0
            val name = obj.get("TovNaim")?.asString ?: ""
            val price = obj.get("TovCena")?.asDouble ?: 0.0
            val kol = obj.get("TovKol")?.asInt ?: 0

            val item = Item(
                itemSh = shtrih,
                itemShTip = shtrihTip,
                itemName = name,
                itemPrice = price,
                itemQuantityInStock = 0,
                itemQuantity = kol
            )
            items.add(item)
        }
        return items
    }

    // ---------------------------------------------------------------
    // Опрос папки (coroutine-friendly)
    // ---------------------------------------------------------------

    /**
     * Запустить опрос папки в фоне.
     * Проверяет наличие Input.json и Output.json, обновляет статус.
     */
    private var pollingRunning = false
    private var lastStatusString: String? = null

    fun startPolling() {
        if (pollingRunning) return
        pollingRunning = true
        CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "Опрос папки запущен")
            while (pollingRunning) {
                try {
                    // Проверяем Input.json — если файл существует и сопряжение активно
                    if (hasInputFile() && currentStatus.pairing) {
                        // Устанавливаем input=3 фактически, а не из файла статуса
                        val updatedStatus = currentStatus.copy(input = 3)
                        if (updatedStatus != currentStatus) {
                            currentStatus = updatedStatus
                            Log.d(TAG, "startPolling: input установлен в 3 (файл tsd_Input.json найден)")
                        }
                        onInputFileReady?.invoke()
                    } else if (!hasInputFile() && currentStatus.input == 3) {
                        // Файл удалён — сбрасываем input на 0
                        val updatedStatus = currentStatus.copy(input = 0)
                        currentStatus = updatedStatus
                        Log.d(TAG, "startPolling: input сброшен в 0 (файл tsd_Input.json удалён)")
                    }
                    // Проверяем Output.json
                    if (hasOutputFile()) {
                        onOutputFileReady?.invoke()
                    }

                    // Определяем bd по факту из БД
                    val app = context.applicationContext as? com.xxivek.tsdxxivek.TSDXXIVekApplication
                    val dao = app?.database?.itemDao()
                    val total = dao?.getCount() ?: 0
                    val notEmpty = dao?.getCountNotEmpty() ?: 0
                    val calculatedBd = if (notEmpty > 0) 2 else if (total > 0) 3 else 0

                    // Обновляем статус только если он изменился
                    readStatus()?.let { status ->
                        val newStatus = status.copy(bd = calculatedBd, input = currentStatus.input)
                        val statusStr = DeviceStatus.toSsv(newStatus)
                        if (statusStr != lastStatusString) {
                            lastStatusString = statusStr
                            currentStatus = newStatus
                            onStatusChanged?.invoke(newStatus)
                        }
                    }
                    delay(2000) // 2 секунды
                } catch (e: InterruptedException) {
                    Log.d(TAG, "Опрос прерван")
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка опроса", e)
                }
            }
        }
    }

    /**
     * Остановить опрос.
     */
    fun stopPolling() {
        pollingRunning = false
        executor.shutdown()
        Log.d(TAG, "Опрос папки остановлен")
        appendLog("Файловый обмен", "Опрос остановлен")
    }

    // ---------------------------------------------------------------
    // Утилиты
    // ---------------------------------------------------------------

    /**
     * Получить текущий статус.
     */
    fun getStatus(): DeviceStatus = currentStatus

    /**
     * Установить статус сопряжения (когда appConnect1C == 3).
     */
    fun setPaired(konf: Int) {
        currentStatus = DeviceStatus(true, konf, 0, 0, 0)
    }

    /**
     * Проверить, сопряжено ли устройство.
     */
    fun isPaired(): Boolean = currentStatus.pairing

    /**
     * Получить конфигурацию.
     */
    fun getKonf(): Int = currentStatus.konf

    /**
     * Проверить наличие данных для загрузки (input=3).
     */
    fun hasPendingInput(): Boolean {
        return currentStatus.input == 3
    }

    /**
     * Установить текущий статус (для восстановления при запуске).
     */
    fun updateStatus(status: DeviceStatus) {
        currentStatus = status
        Log.d(TAG, "updateStatus: pairing=${status.pairing}, konf=${status.konf}, bd=${status.bd}")
    }

    /**
     * Проверить наличие данных для выгрузки (output=0).
     */
    fun hasPendingOutput(): Boolean {
        return currentStatus.output == 0
    }
}