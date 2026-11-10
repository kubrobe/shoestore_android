package uz.shushop.app.data

import android.content.Context
import uz.shushop.app.util.TokenCipher

/**
 * What the phone remembers between launches besides the database: who is logged in (the token, encrypted)
 * and which server to talk to. This is the "session restore" - open the app and you are still logged in.
 */
class SessionStore(context: Context, private val defaultServerUrl: String) {
    private val prefs = context.applicationContext.getSharedPreferences("shushop_session", Context.MODE_PRIVATE)

    /** The login token, or null when logged out (or when it can no longer be decrypted). */
    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)?.let { TokenCipher.decrypt(it) }
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_TOKEN) else putString(KEY_TOKEN, TokenCipher.encrypt(value))
            }.apply()
        }

    /** The shop server. The build's default unless the customer typed another one in Profile > Server. */
    var serverUrl: String
        get() = prefs.getString(KEY_SERVER, null) ?: defaultServerUrl
        set(value) { prefs.edit().putString(KEY_SERVER, value).apply() }

    fun resetServerUrl() { prefs.edit().remove(KEY_SERVER).apply() }

    val hasCustomServer: Boolean get() = prefs.contains(KEY_SERVER)

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_SERVER = "server"
    }
}
