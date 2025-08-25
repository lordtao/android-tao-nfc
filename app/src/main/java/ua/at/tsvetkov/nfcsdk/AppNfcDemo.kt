package ua.at.tsvetkov.nfcsdk

import android.app.Application
import ua.at.tsvetkov.util.logger.Log
import ua.at.tsvetkov.util.logger.LogComponents

/**
 * Created by Alexandr Tsvetkov on 09.17.2020.
 */
class AppNfcDemo : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            LogComponents.enableComponentsChangesLogging(this)
        } else {
            Log.setDisabled()
        }
    }
}
