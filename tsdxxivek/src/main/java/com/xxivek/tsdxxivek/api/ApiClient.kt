package com.xxivek.tsdxxivek.api

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ActivateResponse(
    val success: Boolean = false,
    val device_uuid: String? = null,
    val message: String? = null,
    val httpCode: Int = 0
)

data class PairingStatusResponse(
    val status: String = "",
    val paired: Boolean? = null,
    val message: String? = null
)

data class DeviceStatusResponse(
    val device_uuid: String? = null,
    val pairing: Boolean = false,
    val konf: Int = 0,
    val bd: Int = 0,
    val input: Int = 0,
    val output: Int = 0,
    val httpCode: Int = 0,
    val message: String? = null
)

class ApiClient {
    companion object {
        const val TAG = "ApiClient"
        const val SITE_URL = "http://192.168.160.99"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    // Определяем URL по режиму обмена (use_website)
    private fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val useSite = prefs.getBoolean("use_website", false)
        return if (useSite) {
            SITE_URL
        } else {
            val port = prefs.getInt("port", 8080)
            "http://127.0.0.1:$port"
        }
    }

    fun activateDevice(context: Context, activationCode: String): ActivateResponse {
        val baseUrl = getBaseUrl(context)
        val url = "$baseUrl/api/v1/devices/activate"

        Log.d(TAG, "activateDevice: baseUrl=$baseUrl, code=$activationCode")

        val json = """
            {"activation_code": "$activationCode"}
        """.trimIndent()

        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val httpCode = response.code
                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "activateDevice: httpCode=$httpCode, body=$responseBody")
                if (response.isSuccessful && responseBody.isNotBlank()) {
                    val jsonResp = gson.fromJson(responseBody, JsonObject::class.java)
                    ActivateResponse(
                        success = true,
                        device_uuid = jsonResp.get("device_uuid")?.asString,
                        message = jsonResp.get("message")?.asString,
                        httpCode = httpCode
                    )
                } else {
                    ActivateResponse(
                        success = false,
                        message = "HTTP $httpCode: $responseBody",
                        httpCode = httpCode
                    )
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "activateDevice failed", e)
            ActivateResponse(success = false, message = e.message, httpCode = -1)
        }
    }

    fun sendPairingStatus(context: Context, token: String, pairing: Boolean, konf: Int, bd: Int, input: Int, output: Int): PairingStatusResponse {
        val baseUrl = getBaseUrl(context)
        val url = "$baseUrl/api/v1/devices/status"

        Log.d(TAG, "sendPairingStatus: baseUrl=$baseUrl, token=$token, pairing=$pairing")

        val json = """
            {
                "pairing": $pairing,
                "konf": $konf,
                "bd": $bd,
                "input": $input,
                "output": $output
            }
        """.trimIndent()

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
                Log.d(TAG, "sendPairingStatus: httpCode=$httpCode, body=$responseBody")
                if (response.isSuccessful && responseBody.isNotBlank()) {
                    val jsonResp = gson.fromJson(responseBody, JsonObject::class.java)
                    PairingStatusResponse(
                        status = jsonResp.get("status")?.asString ?: "",
                        paired = jsonResp.get("paired")?.asBoolean,
                        message = jsonResp.get("message")?.asString
                    )
                } else {
                    PairingStatusResponse(status = "", paired = false, message = "HTTP $httpCode: $responseBody")
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "sendPairingStatus failed", e)
            PairingStatusResponse(status = "", paired = false, message = e.message)
        }
    }

    fun getDeviceStatus(context: Context, token: String): DeviceStatusResponse {
        val baseUrl = getBaseUrl(context)
        val url = "$baseUrl/api/v1/devices/status"

        Log.d(TAG, "getDeviceStatus: baseUrl=$baseUrl, token=$token")

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val httpCode = response.code
                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "getDeviceStatus: httpCode=$httpCode, body=$responseBody")
                if (response.isSuccessful && responseBody.isNotBlank()) {
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
                } else {
                    DeviceStatusResponse(
                        pairing = false,
                        httpCode = httpCode,
                        message = "HTTP $httpCode: $responseBody"
                    )
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "getDeviceStatus failed", e)
            DeviceStatusResponse(pairing = false, httpCode = -1, message = e.message)
        }
    }
}
