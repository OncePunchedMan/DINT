package opb.myniceapp.dint

import android.app.Application

class DintApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
