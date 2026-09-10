package com.xxivek.tsdxxivek.api

import android.content.Context
import com.google.gson.Gson
import com.xxivek.tsdxxivek.TSDXXIVekApplication
import com.xxivek.tsdxxivek.api.ExportItemDto
import com.xxivek.tsdxxivek.dataDB.Item
import com.xxivek.tsdxxivek.utilAPP.appendLog
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Результат экспорта.
 */
data class ExportResult(
    val success: Boolean = false,
    val file: File? = null,
    val message: String? = null
)

/**
 * Утилита для экспорта данных из БД ТСД в JSON-файл.
 * Экспортирует только записи с itemQuantity > 0.
 */
object JsonExport {

    private const val TAG = "JsonExport"

    /**
     * Экспортировать записи с quantity > 0 в JSON-файл.
     *
     * @param context контекст приложения
     * @param oper значение oper (из appLic.appOper)
     * @param client значение client (из appLic.appClient)
     * @return результат экспорта
     */
    fun exportToJSON(
        context: Context,
        oper: String,
        client: String,
        deviceUuid: String
    ): ExportResult {
        val items = (context.applicationContext as? TSDXXIVekApplication)
            ?.database?.itemDao()
            ?.getItemNotEmpty2() ?: emptyList()

        if (items.isEmpty()) {
            return ExportResult(success = false, message = "Нет записей для экспорта")
        }

        // Маппинг Item -> ExportItemDto
        val exportItems = items.map { it.toExportDto() }

        // Обёртка: {oper, client, data}
        val exportData = ExportData(
            oper = oper,
            client = client,
            data = exportItems
        )

        val gson = Gson()
        val json = gson.toJson(exportData)

        // Сохранение в файл Output_<timestamp>.json
        val exportDir = context.getExternalFilesDir(null)
            ?: context.filesDir.resolve("export").also { it.mkdirs() }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
        val fileName = "Output_$deviceUuid.json"
        val file = File(exportDir, fileName)

        try {
            FileWriter(file).use { writer ->
                writer.write(json)
            }
            appendLog("JsonExport", "Экспорт: ${items.size} записей -> $fileName")
            return ExportResult(success = true, file = file, message = "Экспортировано ${items.size} записей")
        } catch (e: Exception) {
            appendLog("JsonExport", "Ошибка экспорта: ${e.message}")
            return ExportResult(success = false, message = "Ошибка записи файла: ${e.message}")
        }
    }

    /**
     * Преобразовать Item в ExportItemDto.
     */
    private fun Item.toExportDto(): ExportItemDto {
        return ExportItemDto(
            shtrih = this.itemSh,
            shtrihTip = this.itemShTip,
            tovNaim = this.itemName,
            tovCena = this.itemPrice,
            tovKol = this.itemQuantity
        )
    }

    /**
     * Обёртка экспорта: {oper, client, data}.
     */
    data class ExportData(
        val oper: String,
        val client: String,
        val data: List<ExportItemDto>
    )
}
