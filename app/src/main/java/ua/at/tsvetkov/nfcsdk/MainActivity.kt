package ua.at.tsvetkov.nfcsdk

import android.app.ComponentCaller
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import ua.at.tsvetkov.nfcsdk.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val TAG = this::class.java.simpleName

    private lateinit var binding: ActivityMainBinding

    private val nfcAdmin = NfcAdmin(this, createNfcStateListener())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_read, R.id.navigation_write
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onResume() {
        super.onResume()
        nfcAdmin.enableForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        nfcAdmin.disableForegroundDispatch()
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
        nfcAdmin.onNewIntentInActivity(intent)
    }

    private fun createNfcStateListener(): NfcStateListener? {
        return object : NfcStateListener {
            override fun onNfcStateChanged(state: Int) {
                when (state) {
                    NfcAdapter.STATE_ON -> {
                        // Here you can add logic for actions when NFC is enabled
                        // For example, automatically call nfcManager.enableForegroundDispatch()
                        // if this matches the logic of your application.
                        nfcAdmin.enableForegroundDispatch()
                    }

                    NfcAdapter.STATE_OFF -> {
                        // Here you can add logic for actions when NFC is turned off
                        // For example, inform the user or disable related functions.
                        nfcAdmin.onNewIntentInActivity(intent)
                    }

                    NfcAdapter.STATE_TURNING_ON -> {
                        // NFC is turned on, you can show the loading indicator
                    }

                    NfcAdapter.STATE_TURNING_OFF -> {
                        // NFC turns off
                    }
                }
            }

            override fun onError(error: NfcAdminError) {
                when(error) {
                    NfcAdminError.NFC_ADAPTER_IS_NOT_AVAILABLE -> {}
                    NfcAdminError.NFC_ADAPTER_IS_NOT_ENABLED -> {}
                    else -> Unit
                }
                Log.e(TAG, "NFC Error: ${error.message}")
            }
        }
    }

}