package ua.at.tsvetkov.nfcsdk.preparer

/**
 * Created by Alexandr Tsvetkov on 27.08.2025.
 */
import java.nio.charset.StandardCharsets

private const val MAX_LENGTH = 255

/**
 * Prepares a String (expected as the first element of a list) into a ByteArray
 * suitable for writing to Mifare Ultralight pages.
 * The first byte of the array will be the length of the UTF-8 string,
 * followed by the string data itself.
 *
 * This class implements NfcDataPreparer<String, ByteArray> according to the
 * provided interface `fun prepare(data: List<String>): ByteArray`.
 * It expects the list to contain at least one String element.
 *
 * @param startDataPage The starting page where this data is intended to be written (for log/debug).
 * @param maxDataBytes The maximum number of bytes available for storing length + string data.
 */
class MifareUltralightStringPreparer(private val startDataPage: Int, private val maxDataBytes: Int) :
    NfcDataPreparer<String, ByteArray> {

    companion object {
        const val PAGE_SIZE = 4
    }

    /**
     * @param data A list of Strings. This method will use the first String in the list.
     *             The list should not be empty.
     * @return ByteArray formatted with length prefix.
     * @throws IllegalArgumentException if the list is empty, or if the string is too long,
     *                                  or if data exceeds maxDataBytes.
     */
    override fun prepare(data: List<String>): ByteArray {
        require(data.isNotEmpty()) { "Input list cannot be empty for MifareUltralightStringPreparer." }

        val stringToPrepare = data[0] // Берем первую (и ожидаемо единственную) строку из списка
        val stringBytes = stringToPrepare.toByteArray(StandardCharsets.UTF_8)

        require(stringBytes.size <= MAX_LENGTH) {
            "String is too long (UTF-8 size ${stringBytes.size} bytes). " +
                "Max length byte is 255."
        }

        val totalLengthNeeded = 1 + stringBytes.size // 1 байт для длины + байты строки

        require(totalLengthNeeded <= maxDataBytes) {
            "String data (length byte + string = $totalLengthNeeded bytes) " +
                "exceeds maximum available space ($maxDataBytes bytes) " +
                "configured for pages starting from $startDataPage."
        }

        val preparedData = ByteArray(totalLengthNeeded)
        preparedData[0] = stringBytes.size.toByte() // Записываем длину строки
        System.arraycopy(stringBytes, 0, preparedData, 1, stringBytes.size)

        return preparedData
    }
}
