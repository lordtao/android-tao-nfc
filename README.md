# NFC SDK
=====================
The project is a work in progress. 

1. To work with NFC, your app will need permissions in AndroidManifest.xml. Make sure you have the following lines:

``
   <uses-permission android:name="android.permission.NFC" />
   <uses-feature android:name="android.hardware.nfc" android:required="true" />
``

2. For the Activity that will handle NFC, you will need to set up an _intent-filter_ and possibly set launchMode to _singleTop_ or _singleTask_ so that onNewIntent is called correctly.
3. Initial setup 