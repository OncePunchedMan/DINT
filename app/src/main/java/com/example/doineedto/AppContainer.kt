package com.example.doineedto

import android.content.Context
import com.example.doineedto.data.AppPreferences
import com.example.doineedto.data.AppUpdateManager
import com.example.doineedto.data.UnlockLogRepository

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val preferences: AppPreferences by lazy { AppPreferences(appContext) }
    val unlockLogRepository: UnlockLogRepository by lazy { UnlockLogRepository(appContext) }
    val updateManager: AppUpdateManager by lazy { AppUpdateManager(appContext) }
}
