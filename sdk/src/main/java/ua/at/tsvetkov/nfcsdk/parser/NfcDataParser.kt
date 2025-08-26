package ua.at.tsvetkov.nfcsdk.parser

/**
 * Created by Alexandr Tsvetkov on 26.08.2025.
 *
 * Defines a generic contract for parsing raw data obtained from an NFC tag
 * into a structured format.
 *
 * This interface is designed to be implemented by classes that can interpret
 * specific types of data (e.g., raw bytes from a tag, a specific NDEF record payload)
 * and convert it into a meaningful representation.
 *
 * @param D The type of the raw data input that needs to be parsed.
 *          This could be, for example, `ByteArray`, `NdefRecord`, or a custom data structure.
 * @param R The type of the result object(s) after parsing.
 *          This represents the structured information extracted from the raw data.
 */
interface NfcDataParser<D, R> {

    /**
     * Parses the given raw data into a list of structured result objects.
     *
     * Implementations should handle the specifics of interpreting the input `data`
     * and transforming it into one or more instances of the result type `R`.
     * If the data cannot be parsed or does not contain relevant information,
     * this method might return an empty list or throw an exception,
     * depending on the desired error handling strategy of the implementation.
     *
     * @param data The raw data of type [D] to be parsed.
     * @return A [List] of result objects of type [R]. Returns an empty list
     *         if no relevant data could be parsed or if the input data is invalid
     *         in a way that doesn't warrant an exception.
     */
    fun parse(data: D): List<R>
}
