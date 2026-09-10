package com.xxivek.tsdxxivek

/**
 * Данные статуса устройства из dev_status.txt.
 * Формат SSV: pairing;konf;bd;input;output;
 * Пример: True;1;3;3;3;
 */
data class DeviceStatus(
    val pairing: Boolean,
    val konf: Int,
    val bd: Int,
    val input: Int,
    val output: Int
) {
    companion object {
        private const val SEPARATOR = ";"

        /**
         * Преобразовать статус в SSV-строку.
         */
        fun toSsv(status: DeviceStatus): String {
            return "${status.pairing}${SEPARATOR}${status.konf}${SEPARATOR}${status.bd}${SEPARATOR}${status.input}${SEPARATOR}${status.output}${SEPARATOR}"
        }

        /**
         * Распарсить SSV-строку в DeviceStatus.
         * Если pairing=False — возвращает DeviceStatus(false, 0, 0, 0, 0).
         */
        fun fromSsv(ssv: String): DeviceStatus? {
            val parts = ssv.split(SEPARATOR)
            if (parts.size < 5) return null

            val pairingStr = parts[0].trim()
            if (pairingStr == "False") {
                return DeviceStatus(false, 0, 0, 0, 0)
            }

            val konf = parts[1].toIntOrNull() ?: 0
            val bd = parts[2].toIntOrNull() ?: 0
            val input = parts[3].toIntOrNull() ?: 0
            val output = parts[4].toIntOrNull() ?: 0

            return DeviceStatus(true, konf, bd, input, output)
        }

        /**
         * Статус по умолчанию (без сопряжения).
         */
        fun notPaired(): DeviceStatus {
            return DeviceStatus(false, 0, 0, 0, 0)
        }
    }
}

/**
 * Результат операции файлового обмена.
 */
data class FileExchangeResult(
    val success: Boolean,
    val message: String
)
