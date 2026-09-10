package com.xxivek.tsdxxivek.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.xxivek.tsdxxivek.dataDB.Item
import java.io.File

/**
 * Парсер данных, полученных с веб-сервера через exchange API.
 *
 * Распаршивает JSON/XML файлы, полученные из /api/v1/exchange/files/{id}.
 */
class ExchangeDataParser {

    companion object {
        const val TAG = "ExchangeDataParser"
    }

    private val gson = Gson()

    /**
     * Результат парсинга.
     */
    data class ParseResult(
        val success: Boolean = false,
        val fileName: String = "",
        val data: JsonObject? = null,
        val message: String? = null
    )

    /**
     * Распарсить скачанный файл.
     *
     * @param file файл, полученный из exchange API
     * @return результат парсинга
     */
    fun parseFile(file: File): ParseResult {
        Log.d(TAG, "parseFile: parsing ${file.absolutePath}, size=${file.length()}")

        return try {
            val content = file.readText()

            // Пробуем распарсить как JSON
            val json = gson.fromJson(content, JsonObject::class.java)

            Log.d(TAG, "parseFile: parsed as JSON, keys=${json.keySet()}")

            ParseResult(
                success = true,
                fileName = file.name,
                data = json
            )
        } catch (e: Exception) {
            Log.e(TAG, "parseFile: failed to parse ${file.name}", e)
            ParseResult(
                success = false,
                fileName = file.name,
                message = "Ошибка парсинга: ${e.message}"
            )
        }
    }

    /**
     * Извлечь данные из JSON в список Item.
     *
     * @param json данные из Exchange API
     * @return список Item для записи в БД
     */
    fun parseDataToList(json: JsonObject): List<Item> {
        val items = mutableListOf<Item>()

        val dataArray = json.get("data")?.asJsonArray ?: return emptyList()

        Log.d(TAG, "parseDataToList: data array size=${dataArray.size()}")

        for (i in 0 until dataArray.size()) {
            val obj = dataArray[i].asJsonObject

            val shtrih = obj.get("Shtrih")?.asString ?: ""
            val shTip = obj.get("ShtrihTip")?.asInt ?: 0
            val tovNaim = obj.get("TovNaim")?.asString ?: ""
            val tovCena = obj.get("TovCena")?.asDouble ?: 0.0
            val tovKol = obj.get("TovKol")?.asInt ?: 0

            val item = Item(
                itemSh = shtrih,
                itemShTip = shTip,
                itemName = tovNaim,
                itemPrice = tovCena,
                itemQuantityInStock = tovKol,
                itemQuantity = 0
            )

            items.add(item)
            Log.d(TAG, "parseDataToList: item ${items.size}: shtrih=$shtrih, name=$tovNaim, kol=$tovKol")
        }

        return items
    }

    /**
     * Распарсить все файлы из директории загрузок.
     *
     * @param context контекст приложения
     * @return список результатов парсинга
     */
    fun parseAllDownloadedFiles(context: android.content.Context): List<ParseResult> {
        val downloadDir = context.getExternalFilesDir("downloads")
            ?: return emptyList()

        val files = downloadDir.listFiles { _, name ->
            // Фильтруем только скачанные файлы (не поддиректории)
            File(downloadDir, name).isFile
        }?.filter { it.isFile } ?: emptyList()

        return files.map { parseFile(it) }
    }

    /**
     * Извлечь тип данных из metadata.
     */
    fun extractDataType(data: JsonObject): String {
        return data.get("metadata")?.asJsonObject
            ?.get("type")?.asString ?: "unknown"
    }

    /**
     * Извлечь дату из metadata.
     */
    fun extractDate(data: JsonObject): String {
        return data.get("metadata")?.asJsonObject
            ?.get("date")?.asString ?: ""
    }

    /**
     * Извлечь subject (тему) из данных.
     */
    fun extractSubject(data: JsonObject): String {
        return data.get("subject")?.asString ?: ""
    }

    /**
     * Извлечь oper и client из JSON.
     *
     * @param json данные из Exchange API
     * @param defaultOper значение по умолчанию
     * @param defaultClient значение по умолчанию
     * @return пара (oper, client)
     */
    fun parseOperAndClient(
        json: JsonObject,
        defaultOper: String = "0",
        defaultClient: String = ""
    ): Pair<String, String> {
        val oper = json.get("oper")?.asString ?: defaultOper
        val client = json.get("client")?.asString ?: defaultClient

        Log.d(TAG, "parseOperAndClient: oper=$oper, client=$client")

        return oper to client
    }
}
