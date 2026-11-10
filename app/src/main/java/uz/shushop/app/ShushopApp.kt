package uz.shushop.app

import android.app.Application
import android.content.Context
import uz.shushop.app.data.AppDatabase
import uz.shushop.app.data.SessionStore
import uz.shushop.app.data.ShopRepository
import uz.shushop.app.remote.ApiClient

/** Builds the pieces the app is made of, once: the database, the saved login, the server client and the repository. */
class AppContainer(context: Context) {
    private val session = SessionStore(context, BuildConfig.API_BASE_URL)
    private val api = ApiClient(baseUrl = { session.serverUrl }, token = { session.token })
    val repository = ShopRepository(AppDatabase.get(context), api, session)
}

class ShushopApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
