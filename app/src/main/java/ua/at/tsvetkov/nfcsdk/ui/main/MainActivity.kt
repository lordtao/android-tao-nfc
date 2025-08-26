package ua.at.tsvetkov.nfcsdk.ui.main

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import ua.at.tsvetkov.nfcsdk.NfcAdmin
import ua.at.tsvetkov.nfcsdk.R
import ua.at.tsvetkov.nfcsdk.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val nfcViewModel: NfcViewModel by viewModels()

    private lateinit var nfcAdmin: NfcAdmin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navHostFragment =
            supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        val appBarConfiguration =
            AppBarConfiguration(
                setOf(
                    R.id.navigation_read,
                    R.id.navigation_write
                )
            )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        setupNfc()
    }

    private fun setupNfc() {
        nfcAdmin = NfcAdmin(
            activity = this,
            isAdminLogEnabled = true,
            nfcStateListener = nfcViewModel.nfcAdminStateListener
        )
        nfcAdmin.addHandlers(
            nfcViewModel.nfcTextHandler,
            nfcViewModel.nfcUriHandler
        )
    }

    override fun onResume() {
        super.onResume()
        nfcAdmin.registerNfcStateReceiver()
        nfcAdmin.enableReaderModeWithDefaults()
    }

    override fun onPause() {
        super.onPause()
        nfcAdmin.disableReaderMode()
        nfcAdmin.unregisterNfcStateReceiver()
    }
}
