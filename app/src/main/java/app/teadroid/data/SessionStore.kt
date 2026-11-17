package app.teadroid.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Process-wide singletons, created in [app.teadroid.TeaDroidApp]. */
object Graph {
    lateinit var session: SessionStore
    lateinit var api: GiteaApi
}


/**
 * Persists the server URL, access token (AES-GCM encrypted with an Android Keystore key),
 * the signed-in user, and UI preferences. Fields are Compose state so the UI reacts to changes.
 */
class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("teadroid", Context.MODE_PRIVATE)

    var serverUrl by mutableStateOf(prefs.getString("server", "") ?: "")
        private set

    var token by mutableStateOf(prefs.getString("token", null)?.let(Vault::decrypt) ?: "")
        private set

    var username by mutableStateOf(prefs.getString("username", "") ?: "")
        private set

    var avatarUrl by mutableStateOf(prefs.getString("avatar", "") ?: "")
        private set

    var displayName by mutableStateOf(prefs.getString("display", "") ?: "")
        private set

    /** "system", "light" or "dark" */
    var themeMode by mutableStateOf(prefs.getString("theme", "system") ?: "system")
        private set

    var dynamicColor by mutableStateOf(prefs.getBoolean("dynamic", true))
        private set

    val isLoggedIn: Boolean get() = token.isNotBlank()

    fun signIn(server: String, token: String, user: User) {
        this.serverUrl = server
        this.token = token
        saveUser(user)
        prefs.edit()
            .putString("server", server)
            .putString("token", Vault.encrypt(token))
            .apply()
    }

    fun saveUser(user: User) {
        username = user.login
        avatarUrl = user.avatarUrl
        displayName = user.displayName
        prefs.edit()
            .putString("username", user.login)
            .putString("avatar", user.avatarUrl)
            .putString("display", user.displayName)
            .apply()
    }

    fun signOut() {
        token = ""
        username = ""
        avatarUrl = ""
        displayName = ""
        prefs.edit().remove("token").remove("username").remove("avatar").remove("display").apply()
    }

    fun updateThemeMode(mode: String) {
        themeMode = mode
        prefs.edit().putString("theme", mode).apply()
    }

    fun updateDynamicColor(enabled: Boolean) {
        dynamicColor = enabled
        prefs.edit().putBoolean("dynamic", enabled).apply()
    }
}

private object Vault {
    private const val ALIAS = "teadroid_token_key"
    private const val PROVIDER = "AndroidKeyStore"
    private const val TRANSFORM = "AES/GCM/NoPadding"
    private const val IV_LEN = 12

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        gen.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }

    fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val out = cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String? = runCatching {
        val all = Base64.decode(encoded, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, all, 0, IV_LEN))
        String(cipher.doFinal(all, IV_LEN, all.size - IV_LEN), Charsets.UTF_8)
    }.getOrNull()
}
