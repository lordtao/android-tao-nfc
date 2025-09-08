package ua.at.tsvetkov.nfcsdk.extensions

import kotlin.collections.joinToString
import kotlin.text.format

/**
 * Created by Alexandr Tsvetkov on 08.09.2025.
 */
@Suppress("ImplicitDefaultLocale")
fun ByteArray.toHexStringWithSeparator(separator: String = ":"): String = this.joinToString(separator) { byte ->
    String.format("%02X", byte)
}
