package ua.at.tsvetkov.nfcsdk.demo.ui.main

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import ua.at.tsvetkov.nfcsdk.NfcAdmin
import ua.at.tsvetkov.nfcsdk.demo.R
import ua.at.tsvetkov.nfcsdk.demo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private val nfcViewModel: NfcViewModel by viewModels()

    private lateinit var nfcAdmin: NfcAdmin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewModel = nfcViewModel
        binding.lifecycleOwner = this

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
            nfcStateListener = nfcViewModel.stateListener
        )
        nfcAdmin.addHandlers(
            nfcViewModel.textHandler,
            nfcViewModel.uriHandler
//            nfcViewModel.nfcMifareUltralightHandler
        )
    }

    override fun onResume() {
        super.onResume()
        nfcAdmin.registerNfcStateReceiver()
        nfcAdmin.enableReaderMode()
    }

    override fun onPause() {
        super.onPause()
        nfcAdmin.disableReaderMode()
        nfcAdmin.unregisterNfcStateReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.lifecycleOwner = null
        _binding = null
    }
}
