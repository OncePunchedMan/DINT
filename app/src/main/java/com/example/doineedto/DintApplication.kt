package com.example.doineedto

import android.app.Application

class DintApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
