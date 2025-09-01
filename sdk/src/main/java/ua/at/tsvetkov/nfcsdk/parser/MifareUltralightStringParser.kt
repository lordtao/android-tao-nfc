package ua.at.tsvetkov.nfcsdk.parser

/**
 * Created by Alexandr Tsvetkov on 27.08.2025.
 */

import java.nio.charset.StandardCharsets

private const val MAX_LENGTH = 255

/**
 * Parses a raw ByteArray (read from Mifare Ultralight pages) into a List containing a single String.
 * Assumes the first byte of the array is the length of the UTF-8 string,
 * and the subsequent bytes are the string data itself.
 */
class MifareUltralightStringParser(private val expectedMaxDataBytes: Int) : NfcDataParser<ByteArray, String> {
    @Suppress("UseRequire")
    override fun parse(data: ByteArray): List<String> {
        return if (data.isEmpty()) {
            emptyList() // Возвращаем пустой список, если нет данных
        } else {
            val stringLength = data[0].toInt() and MAX_LENGTH // Длина строки (без знака)
            if (stringLength == 0) {
                // Если длина 0, значит, была записана пустая строка.
                // Возвращаем список с одной пустой строкой.
                listOf("")
            } else {
                // Проверяем, достаточно ли данных после байта длины  +1 из-за самого байта длины
                if (stringLength > data.size - 1) {
                    throw IllegalArgumentException(
                        "Reported string length ($stringLength) is greater than available data " +
                                "(${data.size - 1} bytes). Data might be corrupt or incomplete."
                    )
                }
                if (stringLength > expectedMaxDataBytes - 1) {
                    // Это больше для согласованности с preparer, если мы читаем больше, чем могли бы записать
                    throw IllegalArgumentException("Reported string length ($stringLength) exceeds maximum expected.")
                }

                // Извлекаем только байты строки, исключая первый байт (длину)
                // и обрезаем до фактической длины строки
                val resultString = String(data, 1, stringLength, StandardCharsets.UTF_8)
                listOf(resultString)
            }
        }
    }
}
