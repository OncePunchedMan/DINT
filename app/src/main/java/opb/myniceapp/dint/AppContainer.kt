package opb.myniceapp.dint

import android.content.Context
import opb.myniceapp.dint.data.AppPreferences
import opb.myniceapp.dint.data.AppUpdateManager
import opb.myniceapp.dint.data.UnlockLogRepository

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val preferences: AppPreferences by lazy { AppPreferences(appContext) }
    val unlockLogRepository: UnlockLogRepository by lazy { UnlockLogRepository(appContext) }
    val updateManager: AppUpdateManager by lazy { AppUpdateManager(appContext) }
}
