# TAO NFC SDK (Android)

The NFC SDK library simplifies Near Field Communication (NFC) interactions in Android applications. It provides a higher-level abstraction over the native Android NFC API, making it easier to read from, write to, and manage NFC tags, with a focus on NDEF (NFC Data Exchange Format) data. The SDK is designed to be extensible, allowing developers to add support for various tag technologies and custom data formats.

## Features

*   **High-level API**: Simplifies common NFC operations.
*   **NDEF Support**: Robust reading and writing of NDEF messages.
*   **NDEF Formatting**: Automatically formats `NdefFormatable` tags before writing.
*   **Tag Cleaning**: Supports "cleaning" NDEF tags by writing an empty NDEF message.
*   **Extensibility**:
    *   Create custom `NfcHandler` implementations for different NFC tag technologies.
    *   Implement custom `NfcDataParser` and `NfcDataPreparer` to support various data encodings and application-specific data structures.
*   **Listeners**: Callbacks for NFC adapter state changes, tag read results (data or errors), and write operation outcomes.
*   **Error Handling**: Standardized `NfcAdminError` enum for detailed error reporting.

## Setup
To get started quickly, the demo project provides a comprehensive setup example.
https://github.com/lordtao/nfc-sdk/tree/master/app

### 1. Add SDK Dependency

There are a couple of ways to add the SDK dependency, depending on how it's structured in your project:

#### a. If the SDK is a Local Module

If this SDK is a local module (e.g., named `sdk`) within your Android Studio project, add it as a dependency in your app-level `build.gradle.kts` (or `build.gradle`) file:
```gradle
// build.gradle.kts (Kotlin DSL)
dependencies {
    implementation(project(":sdk"))
}
```

#### b. If the SDK is a Local `.aar` File

If you have the SDK as a precompiled `.aar` file (e.g., `nfc-sdk.aar`), follow these steps:

1.  **Create a `libs` directory**: In your app module's root directory (usually `app/`), create a directory named `libs` if it doesn't already exist.
2.  **Copy the `.aar` file**: Place your `nfc-sdk.aar` file into this `app/libs` directory.
3.  **Add the dependency**: Modify your app-level `build.gradle.kts` (or `build.gradle`) file to include this local `.aar` file.

    For `build.gradle.kts` (Kotlin DSL):
    ```gradle
    dependencies {
        // ... other dependencies
        implementation(files("libs/nfc-sdk.aar")) // Adjust filename if different
    }
    ```
    ```
    If you have multiple `.aar` files in the `libs` directory and want to include all of them, you can also do:

    For `build.gradle.kts` (Kotlin DSL):
    ```gradle
    dependencies {
        // ... other dependencies
        implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    }
    ```
4.  **Sync Project**: After making these changes, sync your Android Studio project with the Gradle files.

### 2. Configure Application's `AndroidManifest.xml`

Your application using this SDK needs the following configurations in its `AndroidManifest.xml`:

#### a. NFC Permission

Request the necessary permission to use NFC:

```xml
<uses-permission android:name="android.permission.NFC" />
```

#### b. NFC Feature Declaration

Declare that your app uses NFC hardware. `android:required="true"` means Google Play will only show your app on devices with NFC. Set to `false` if NFC is an optional feature.

```xml
<uses-feature android:name="android.hardware.nfc" android:required="true" />
```

#### c. Intent Filters for NFC Discovery

Add intent filters to the Activity that will handle discovered NFC tags. This allows your Activity to be launched or notified when an NFC tag is detected.

```xml
<activity
    android:name=".YourNfcHandlingActivity"
    android:launchMode="singleTop"> <!-- Or singleTask, important for onNewIntent -->

    <intent-filter>
        <action android:name="android.nfc.action.NDEF_DISCOVERED"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <!-- Specify MIME type if you only handle specific NDEF data -->
        <!-- Example: <data android:mimeType="text/plain" /> -->
        <!-- For generic NDEF handling: -->
        <data android:mimeType="*/*" />
    </intent-filter>

    <intent-filter>
        <action android:name="android.nfc.action.TECH_DISCOVERED"/>
        <category android:name="android.intent.category.DEFAULT"/>
    </intent-filter>
    <!-- NfcAdapter.ACTION_TAG_DISCOVERED is a fallback. -->
    <!-- TECH_DISCOVERED requires a tech-list XML resource file. -->
    <!-- For simplicity, NDEF_DISCOVERED and a generic TECH_DISCOVERED (without specific tech) can be a starting point. -->
    <!-- Add a meta-data for TECH_DISCOVERED if you want to specify technologies. -->
    <!--
    <meta-data
        android:name="android.nfc.action.TECH_DISCOVERED"
        android:resource="@xml/nfc_tech_filter" />
    -->

    <intent-filter>
        <action android:name="android.nfc.action.TAG_DISCOVERED"/>
        <category android:name="android.intent.category.DEFAULT"/>
    </intent-filter>

</activity>
```
*   **Note on `launchMode`**: Using `singleTop` or `singleTask` for your NFC handling Activity ensures that `onNewIntent()` is called when a tag is discovered while the Activity is already active, rather than creating a new instance of the Activity.
*   **Tech List File (Optional for `TECH_DISCOVERED`)**: If you want to handle specific tag technologies with `TECH_DISCOVERED`, you'd create an XML resource file (e.g., `res/xml/nfc_tech_filter.xml`) and reference it in the manifest. Example:
    ```xml
    <!-- res/xml/nfc_tech_filter.xml -->
    <resources xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2">
        <tech-list>
            <tech>android.nfc.tech.Ndef</tech>
            <tech>android.nfc.tech.NfcA</tech>
            <!-- Add other technologies your app supports -->
        </tech-list>
    </resources>
    ```

## Core SDK Components

*   **`NfcAdmin`**:
    The primary entry point for interacting with the SDK. It's responsible for:
    *   Enabling and disabling foreground NFC dispatch for an Activity.
    *   Handling incoming NFC intents from the Android system.
    *   Delegating tag interactions to a configured `NfcHandler`.
    *   Notifying `NfcStateListener` about NFC adapter state changes.
    *   Constructor: `NfcAdmin(activity: Activity, nfcStateListener: NfcStateListener? = null, isAdminLogEnabled: Boolean = false)`
    *   An `NfcHandler` is typically set via a dedicated method (e.g., `setNfcHandler(handler: NfcHandler<*, *>)`).

*   **`NfcHandler<D, R>`**:
    An abstract class that defines the contract for specific tag technology interactions.
    *   `D`: The raw data type read from or written to the tag (e.g., `NdefMessage`, `ByteArray`).
    *   `R`: The application-level data type that is parsed from `D` or prepared into `D`.
    *   Constructor: `NfcHandler(parser: NfcDataParser<D, R>, preparer: NfcDataPreparer<R, D>, nfcListener: NfcListener<R>? = null)`
    *   **`NdefHandler<R>`**: A concrete implementation of `NfcHandler<NdefMessage, R>` specialized for NDEF operations. It uses an `NfcDataParser<NdefMessage, R>` and `NfcDataPreparer<R, NdefMessage>`.

*   **`NfcDataParser<D, R>`**:
    An interface for parsing raw data read from a tag (`D`) into a more structured, application-friendly format (`R`).
    *   Key method: `parse(data: D): R`
    *   Example: `NfcTextDataParser` would implement `NfcDataParser<NdefMessage, String>`.

*   **`NfcDataPreparer<R, D>`**:
    An interface for converting application data (`R`) into a format (`D`) that can be written to a tag.
    *   Key method: `prepare(data: R): D`
    *   Additional method: `prepareCleaningData(): D` (to prepare data that effectively clears the tag).
    *   Example: `NfcTextDataPreparer` would implement `NfcDataPreparer<String, NdefMessage>`.

*   **Listeners**:
    *   **`NfcStateListener`**: Receives callbacks when the device's NFC adapter is enabled or disabled.
    *   **`NfcListener<R>`**: Called when an event/error occurs during the NFC tag scanning or processing in a handler.
        *   `onRead(result: List<R>)`: Called on successful read and parse.
        *   `onWriteSuccess()`: Called when data has been successfully written to the NFC tag.
        *   `onNfcError(error: NfcError, exception: Exception?)`: Called when an event/error occurs during the NFC tag scanning or processing.

*   **`NfcError`**:
    An enum representing various errors that can occur during NFC operations (e.g., `TAG_NOT_NDEF_FORMATTED`, `IO_EXCEPTION_WHILE_READING`, `TAG_NOT_WRITABLE`).

## How to Use the SDK

This section guides you on integrating and using the SDK in your Android Activity.

### 1. Define Listeners

Implement the necessary listeners to handle callbacks from the SDK.

```kotlin
// In YourNfcHandlingActivity.kt or a ViewModel

private val nfcStateListener = object : NfcStateListener {
    override fun onNfcStateChanged(state: NfcAdminState) {
        // Handle NFC adapter state (e.g., update UI)
        Log.d("NFC_SDK_App", "NFC State: $state")
    }
    override fun onError(error: NfcAdminError) {
        // Handle NFC error
        Log.d("NFC_SDK_App", "NFC Error: $error")
    }
}

// Assuming R (application-level data) is String
private val nfcListener = object : NfcListener<String> {
    override fun onRead(result: List<String>) {
        // Handle successfully read and parsed data
        Log.d("NFC_SDK_App", "Data: ${result.joinToString(separator = "\n")}")
        // Update UI with 'data'
    }

    override fun onWriteSuccess() {
        // Handle successful write operation
        Log.d("NFC_SDK_App", "Tag Write Success!")
    }

    override fun onError(message: NfcMessage, throwable: Throwable?) {
        // Handle events and errors
        Log.w("NFC_SDK_App", "Read message: ${message.name} ($message)")
    }
}
```

### 2. Initialize `NfcAdmin` and `NfcHandler`

In your Activity's `onCreate` method, initialize the `NfcAdmin` and an appropriate `NfcHandler`. Then, set the handler on the admin instance. For NDEF text data:

```kotlin
class YourNfcHandlingActivity : AppCompatActivity() {

    private lateinit var nfcAdmin: NfcAdmin
    private lateinit var ndefTextHandler: NfcNdefTextHandler // For NDEF text, R = String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ... your layout setup ...

        setupNfc()
    }

    private fun setupNfc() {
        // 1. Create NfcAdmin
        nfcAdmin = NfcAdmin(
            activity = this,
            isAdminLogEnabled = true,
            nfcStateListener = nfcViewModel.stateListener
        )
        
        // 2. Create a NfcHandler for handle your data types
        // You can also create and get a handlers from your ViewModel
        yourHandler = YourNfcHandler(
            parser = yourTextDataParser,
            preparer = yourTextDataPreparer,
            nfcReadListener = yourNfcReadListener,
            nfcListener = yourNfcWriteListener
        )
        // Create another handlers...
        
        // 3. Add a handlers
        nfcAdmin.addHandlers(
            nfcViewModel.textHandler,
            nfcViewModel.yourHandler
            // etc...
        )
    }
    
}
```

```kotlin
// Example concrete implementations (you'd need these or get them from SDK)
// D = NdefMessage, R = String
class YourTextDataParser : NfcDataParser<NdefMessage, String> {
    override fun parse(data: NdefMessage): String {
        return data.records.firstOrNull { it.tnf == NdefRecord.TNF_WELL_KNOWN && it.type.contentEquals(NdefRecord.RTD_TEXT) }
            ?.let { record ->
                val languageCodeLength = record.payload[0].toInt() and 0x3F
                String(record.payload, languageCodeLength + 1, record.payload.size - languageCodeLength - 1, Charsets.UTF_8)
            } ?: "No text record found"
    }
}

// R = String, D = NdefMessage
class YourTextDataPreparer : NfcDataPreparer<String, NdefMessage> {
    override fun prepare(data: String): NdefMessage {
        val textRecord = NdefRecord.createTextRecord("en", data) // "en" for English
        return NdefMessage(arrayOf(textRecord))
    }

    override fun prepareCleaningData(): NdefMessage {
        val emptyRecord = NdefRecord(NdefRecord.TNF_EMPTY, null, null, null)
        return NdefMessage(arrayOf(emptyRecord))
    }
}
```

### 3. Handle Activity Lifecycle and NFC Intents

Manage NFC dispatch in your Activity's lifecycle methods and pass NFC events to the `NfcAdmin`.

```kotlin
// In YourNfcHandlingActivity.kt

override fun onResume() {
    super.onResume()
    nfcAdmin.registerNfcStateReceiver() // register NFC state observer
    nfcAdmin.enableReaderMode() // Enable work of nfcAdmin
}

override fun onPause() {
    super.onPause()
    nfcAdmin.disableReaderMode() // unregister NFC state observer
    nfcAdmin.unregisterNfcStateReceiver()// Disable work of nfcAdmin
}
```

### 4. Reading Data from a Tag

When an NFC tag is detected, the configured `NfcHandler` will attempt to read the tag.
If successful, the `NfcReadListener.onRead(list)` callback will be invoked with the parsed data. 
Errors and events are reported via `onReadEvent`.

### 5. Writing Data to a Tag

To write data to an NFC tag:

1.  **Prepare the data**: Call the `prepareToWrite(data: R)` method on your `NfcHandler` instance (where `R` is your application-level data type). This uses the configured `NfcDataPreparer` to convert your application data into the format the handler can write (e.g., an `NdefMessage`).
2.  **Tap the tag**: When the user touches the NFC tag, the handler is instructed to perform a write operation with the prepared data.

```kotlin
// In YourNfcHandlingActivity.kt, e.g., in a button click listener

fun onWriteTextButtonClicked(textToWrite: String) {
    if (::ndefTextHandler.isInitialized) {
        ndefTextHandler.prepareToWrite(textToWrite) // R = String
        // Inform user to tap the tag
        Toast.makeText(this, "Ready to write. Tap an NFC tag.", Toast.LENGTH_SHORT).show()
    }
}
```
The result of the write operation will be reported via `NfcWriteListener`.

### 6. Cleaning/Erasing 

The ability to clear a tag depends on its specific technology and specifications. For NDEF tags, this is achieved by writing an empty record, but other tag types may require a different process.

1.  Call the `prepareCleaningData()` method on your `NdefHandler` instance.
2.  Tap the tag. The write operation will then write the empty message.
```kotlin
// In YourNfcHandlingActivity.kt, e.g., in a button click listener

fun onCleanTagButtonClicked() {
    if (::ndefTextHandler.isInitialized) {
        ndefTextHandler.prepareCleaningData()
        Toast.makeText(this, "Ready to clean. Tap an NFC tag.", Toast.LENGTH_SHORT).show()
    }
}
```

## Extending the SDK

You can extend the SDK to support custom data formats or different NFC tag technologies.

### 1. Creating a Custom `NfcDataParser<D, R>`

Implement the `NfcDataParser<D, R>` interface to convert raw tag data (`D`) into your desired application data structure (`R`).

```kotlin
// Example: Parsing a custom NDEF MIME record "application/vnd.mydata" containing "key:value"
// D = NdefMessage, R = MyCustomData?
data class MyCustomData(val key: String, val value: String)

class MyCustomNdefDataParser : NfcDataParser<NdefMessage, MyCustomData?> {
    override fun parse(data: NdefMessage): MyCustomData? {
        val customRecord = data.records.firstOrNull {
            it.tnf == NdefRecord.TNF_MIME_MEDIA && "application/vnd.mydata" == it.toMimeType()
        }
        return customRecord?.payload?.let { payloadBytes ->
            val payloadString = String(payloadBytes, Charsets.UTF_8)
            val parts = payloadString.split(":", limit = 2)
            if (parts.size == 2) MyCustomData(parts[0], parts[1]) else null
        }
    }
}
```

### 2. Creating a Custom `NfcDataPreparer<R, D>`

Implement the `NfcDataPreparer<R, D>` interface to convert your application data (`R`) into a format (`D`) suitable for writing to a tag.

```kotlin
// Example: Preparing MyCustomData for an NDEF MIME record "application/vnd.mydata"
// R = MyCustomData, D = NdefMessage
class MyCustomNdefDataPreparer : NfcDataPreparer<MyCustomData, NdefMessage> {
    override fun prepare(data: MyCustomData): NdefMessage {
        val payloadString = "${data.key}:${data.value}"
        val mimeRecord = NdefRecord.createMime(
            "application/vnd.mydata",
            payloadString.toByteArray(Charsets.UTF_8)
        )
        return NdefMessage(arrayOf(mimeRecord))
    }
}
```

### 3. Creating a Custom `NfcHandler<D, R>`

Extend the abstract `NfcHandler<D, R>` class.

```kotlin
// Example: Basic structure for a hypothetical MifareClassic handler
// D = ByteArray (raw data from blocks)
// R = MifareCardData (custom object)

class MifareCustomHandler(
    parser: NfcDataParser<ByteArray, MifareCardData>, // D_parser_in = ByteArray, R_parser_out = MifareCardData
    preparer: NfcDataPreparer<MifareCardData, ByteArray>, // R_preparer_in = MifareCardData, D_preparer_out = ByteArray
    readListener: NfcReadListener<MifareCardData>?,
    writeListener: NfcWriteListener?
) : NfcHandler<ByteArray, MifareCardData>(parser, preparer, readListener, writeListener) {
    // D = ByteArray, R = MifareCardData

    override fun readDataFromTag(tag: Tag) {
        val mifare = MifareClassic.get(tag)
        if (mifare == null) {
            onReadError(NfcError.UNSUPPORTED_TAG_TECHNOLOGY) // Use onReadError
            return
        }
        try {
            mifare.connect()
            // IMPORTANT: Authentication is required for most Mifare Classic operations!
            // boolean auth = mifare.authenticateSectorWithKeyA(sectorIndex, MifareClassic.KEY_DEFAULT);
            val blockData = mifare.readBlock(0) // Example: Read block 0
            mifare.close()

            val parsedData = this.parser.parse(blockData) // Use this.parser
            nfcListener?.onRead(parsedData)
        } catch (e: Exception) {
            onError(NfcError.READ_GENERAL_ERROR, e)
        } finally {
            try { mifare?.close() } catch (e: IOException) { /* Log quietly */ }
        }
    }

    override fun writeDataToTag(tag: Tag) {
        val mifare = MifareClassic.get(tag)
        if (mifare == null || preparedData == null) {
            onError(if (mifare == null) NfcError.WRITE_TAG_NOT_COMPLIANT_FOR_THIS_HANDLER else NfcError.WRITE_NO_DATA_TO_WRITE)
            return
        }
        val dataToWrite: ByteArray = preparedData ?: return

        try {
            mifare.connect()
            // IMPORTANT: Authentication is required!
            mifare.writeBlock(0, dataToWrite) // Example: Write to block 0
            mifare.close()
            nfcListener?.onNfcWriteSuccess()
        } catch (e: Exception) {
            onError(NfcError.WRITE_GENERAL_ERROR, e)
        } finally {
            try { mifare?.close() } catch (e: IOException) { /* Log quietly */ }
            preparedData = null
        }
    }

    // Application calls this to set up data for writing.
    // data is of type R (MifareCardData here)
    override fun prepareCleaningData() {
        val emptyRecord = EmptyRecord()
        preparedData = MifareCardData(arrayOf(emptyRecord))
    }
    
}
```
**Note**: Interacting with `MifareClassic` tags is complex due to sector/block structure and authentication requirements. The example above is highly simplified.

## Troubleshooting

*   **Android Studio**: As of now, Android Studio emulators lack support for NFC emulation. A physical device is necessary to test NFC features.
*   **NFC Not Working**: Make sure your device supports NFC. Ensure NFC is enabled in the device settings.
*   **App Not Detecting Tags**:
    *   Verify `AndroidManifest.xml` permissions and intent filters are correct.
    *   Ensure `NfcAdmin.enableNfcDispatch()` is called in `onResume` and `disableNfcDispatch()` in `onPause`.
    *   Check if your Activity's `launchMode` is suitable (`singleTop` or `singleTask`).
*   **Data Binding Issues**: If using LiveData from listeners with Data Binding, ensure `binding.lifecycleOwner` is set in your Fragment/Activity.
*   **Tag Specifics**: Different NFC tags have different capabilities. Test with various tags.
*   **Logs**: Check Logcat for messages (e.g., from `NFC_SDK_App` if `isAdminLogEnabled = true`) and any exceptions. 
*   **tao-log**: Recommend using the library  https://github.com/lordtao/android-tao-log for detailed and useful logs

---
This updated README should now accurately reflect the new class signatures.
