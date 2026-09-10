package com.xxivek.tsdxxivek

import android.content.Context
import android.util.Log

/**
 * Парсер QR-кода для сопряжения в файловом режиме.
 * Формат: PAIR:true;socket:false;konf:1;LOGGING:0
 */
class QrPairingParser {

    companion object {
        private const val TAG = "QrPairingParser"
        private const val PREFIX = "PAIR:"
    }

    /**
     * Результат парсинга QR-кода.
     */
    data class PairingResult(
        val paired: Boolean,
        val useSocket: Boolean,
        val konf: Int,
        val logging: Boolean,
        val port: Int = 0,
        val license: String = "-1",
        val activationCode: String = ""
    ) {
        fun isValid(): Boolean = paired && konf >= 0
    }

    /**
     * Распарсить QR-код.
     * Поддерживаемые форматы:
     * 1. Socket/Сайт: PAIR:<activation_code> (например PAIR:ABC123)
     * 2. Локальный USB: PAIR:true;socket:false;konf:1;LOGGING:0;
     * 3. Локальный WIFI: PAIR:true;socket:true;konf:1;LOGGING:0;Port:8180;Lic:1;
     */
    fun parse(qrData: String): PairingResult {
        Log.d(TAG, "Парсинг QR: $qrData")

        if (!qrData.startsWith(PREFIX)) {
            Log.w(TAG, "Неверный формат QR-кода (нет PAIR:)")
            return PairingResult(false, false, -1, false)
        }

        val rest = qrData.substringAfter(PREFIX, "")

        // Формат 1: Socket/Сайт — PAIR:<activation_code>
        // activation_code = 6 символов A-Z, 0-9, без ";"
        if (!rest.contains(";")) {
            val activationCode = rest.trim().uppercase()
            Log.d(TAG, "Распознан формат Socket/Сайт: activation_code=$activationCode")
            return PairingResult(
                paired = true,
                useSocket = true,
                konf = 1,
                logging = false,
                activationCode = activationCode
            )
        }

        // Форматы 2 и 3: Локальный USB / WIFI — PAIR:true;socket:...;konf:...;...
        val parts = rest.split(";")

        var paired = false
        var useSocket = false
        var konf = -1
        var logging = false
        var port = 0
        var license = "-1"

        for (part in parts) {
            val trimmed = part.trim()
            when {
                trimmed.equals("true", ignoreCase = true) -> {
                    paired = true
                }
                trimmed.equals("false", ignoreCase = true) -> {
                    // socket:false означает файловый режим
                    useSocket = false
                }
                trimmed.startsWith("socket:", ignoreCase = true) -> {
                    useSocket = trimmed.substringAfter(":", "").trim().equals("true", ignoreCase = true)
                }
                trimmed.startsWith("konf:", ignoreCase = true) -> {
                    konf = trimmed.substringAfter(":", "").trim().toIntOrNull() ?: -1
                }
                trimmed.startsWith("LOGGING:", ignoreCase = true) -> {
                    logging = trimmed.substringAfter(":", "").trim().toIntOrNull() == 1
                }
                trimmed.startsWith("Port:", ignoreCase = true) -> {
                    port = trimmed.substringAfter(":", "").trim().toIntOrNull() ?: 0
                }
                trimmed.startsWith("Lic:", ignoreCase = true) -> {
                    license = trimmed.substringAfter(":", "").trim()
                }
            }
        }

        Log.d(TAG, "Результат: paired=$paired, useSocket=$useSocket, konf=$konf, logging=$logging, port=$port, license=$license")
        return PairingResult(paired, useSocket, konf, logging, port, license)
    }

    /**
     * Сохранить результат сопряжения в SharedPreferences.
     */
    fun savePairing(context: Context, result: PairingResult) {
        if (!result.isValid()) {
            Log.w(TAG, "Невалидный результат сопряжения, не сохраняем")
            return
        }

        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(AppConstants.APP_PREF_LIC, result.license)
            putString(AppConstants.APP_PREF_PORT, result.port.toString())
            putString(AppConstants.APP_PREF_KONF, result.konf.toString())
            putBoolean(AppConstants.APP_PREF_USE_SOCKET, result.useSocket)
            putBoolean(AppConstants.APP_PREF_LOGGING, result.logging)
            apply()
        }

        Log.d(TAG, "Сопряжение сохранено: konf=${result.konf}, socket=${result.useSocket}, logging=${result.logging}, port=${result.port}, license=${result.license}")
    }

    /**
     * Сгенерировать QR-код для сопряжения (для отображения на ТСД).
     */
    fun generatePairingQr(konf: Int, logging: Boolean, port: Int = 0, license: String = "-1"): String {
        return "PAIR:true;socket:true;konf:$konf;LOGGING:${if (logging) 1 else 0};Port:$port;Lic:$license"
    }
}
