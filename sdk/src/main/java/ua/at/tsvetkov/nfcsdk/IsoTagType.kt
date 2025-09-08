package ua.at.tsvetkov.nfcsdk

import android.nfc.Tag

/**
 * Enumerates various ISO and NFC-related tag types, categorized based on their underlying technologies.
 * This enum helps in identifying and differentiating NFC tags encountered by the system.
 *
 * @property description A human-readable description of the tag type.
 */
enum class IsoTagType(val description: String) {
    /** Represents an ISO 14443-4 tag that is Type A based. Typically uses IsoDep and NfcA technologies. */
    ISO_14443_4_A("ISO 14443-4 (Type A based)"),

    /** Represents an ISO 14443-4 tag that is Type B based. Typically uses IsoDep and NfcB technologies. */
    ISO_14443_4_B("ISO 14443-4 (Type B based)"),

    /** Represents a generic ISO 14443-4 tag where the underlying Type (A or B) is not specified or relevant.
     * Typically uses IsoDep. */
    ISO_14443_4_GENERIC("ISO 14443-4"),

    /** Represents an ISO 14443-A tag that is not ISO 14443-4 compliant (e.g., MIFARE Classic, MIFARE Ultralight).
     * Typically uses NfcA. */
    ISO_14443_A("ISO 14443-A"),

    /** Represents a MIFARE Classic tag, which is based on ISO 14443-A.
     * Typically uses NfcA and MifareClassic technologies. */
    MIFARE_CLASSIC("MIFARE Classic (ISO 14443-A)"),

    /** Represents a MIFARE Ultralight tag, which is based on ISO 14443-A.
     * Typically uses NfcA and MifareUltralight technologies. */
    MIFARE_ULTRALIGHT("MIFARE Ultralight (ISO 14443-A)"),

    /** Represents an ISO 14443-B tag that is not ISO 14443-4 compliant. Typically uses NfcB. */
    ISO_14443_B("ISO 14443-B"),

    /** Represents a JIS X 6319-4 tag, commonly known as FeliCa. Typically uses NfcF. */
    JIS_X_6319_4("JIS X 6319-4 (FeliCa)"),

    /** Represents an ISO 15693 tag, often used for vicinity cards. Typically uses NfcV. */
    ISO_15693("ISO 15693"),

    /**
     * Represents cases where the tag might be proprietary, NDEF-only without clear underlying ISO type,
     * or its specific ISO classification is ambiguous based on the available technologies.
     */
    PROPRIETARY_OR_NDEF_ONLY("Proprietary or NDEF only"),

    /** Indicates that the tag type is unknown or could not be determined from its technologies. */
    UNKNOWN("Unknown");

    /**
     * Companion object for [IsoTagType], providing utility functions.
     */
    companion object {

        /**
         * Determines the [IsoTagType] of a given [Tag] based on its supported technologies.
         *
         * The method inspects the `tag.techList` to infer the most specific [IsoTagType].
         * It prioritizes IsoDep-based types, then specific NfcA types (MIFARE),
         * followed by other standard NFC technologies.
         *
         * @param tag The [android.nfc.Tag] object discovered by the NFC reader.
         * @return The determined [IsoTagType]. Returns [UNKNOWN] if the `techList` is empty
         * or if the type cannot be clearly determined from the known combinations.
         * Returns [PROPRIETARY_OR_NDEF_ONLY] if technologies are present but don't
         * match specific ISO types.
         */
        // @Suppress("ReturnCount") // May no longer be needed with the when expression
        fun determineIsoTagType(tag: Tag): IsoTagType {
            val techSet = tag.techList.map { it.substringAfterLast('.') }.toSet()

            if (techSet.isEmpty()) {
                return UNKNOWN
            }

            return when {
                "IsoDep" in techSet -> {
                    when {
                        "NfcA" in techSet -> ISO_14443_4_A
                        "NfcB" in techSet -> ISO_14443_4_B
                        else -> ISO_14443_4_GENERIC
                    }
                }

                "NfcA" in techSet -> {
                    when {
                        "MifareClassic" in techSet -> MIFARE_CLASSIC
                        "MifareUltralight" in techSet -> MIFARE_ULTRALIGHT
                        else -> ISO_14443_A
                    }
                }

                "NfcB" in techSet -> ISO_14443_B
                "NfcF" in techSet -> JIS_X_6319_4
                "NfcV" in techSet -> ISO_15693
                else -> PROPRIETARY_OR_NDEF_ONLY
            }
        }
    }
}
