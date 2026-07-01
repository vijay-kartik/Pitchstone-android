package space.pitchstone.android.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import space.pitchstone.android.BuildConfig

@Singleton
class GatewayPreferencesImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GatewayPreferences {
    
    companion object {
        private const val PREFS_FILE_NAME = "gateway_secure_prefs"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_API_KEY = "api_key"
        private const val DEFAULT_BASE_URL = "https://kartiks-mac-mini-7.tailafb282.ts.net"
        private val DEFAULT_API_KEY: String = BuildConfig.GATEWAY_API_KEY
    }

    private val sharedPreferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val baseUrlState = MutableStateFlow(getBaseUrlSync())
    private val apiKeyState = MutableStateFlow(getApiKeySync())

    override fun getBaseUrl(): Flow<String> = baseUrlState.asStateFlow()

    override suspend fun setBaseUrl(url: String) {
        val formattedUrl = when {
            url.isBlank() -> DEFAULT_BASE_URL
            url.endsWith("/") -> url.removeSuffix("/")
            else -> url
        }
        sharedPreferences.edit().putString(KEY_BASE_URL, formattedUrl).apply()
        baseUrlState.value = formattedUrl
    }

    override fun getApiKey(): Flow<String> = apiKeyState.asStateFlow()

    override suspend fun setApiKey(key: String) {
        sharedPreferences.edit().putString(KEY_API_KEY, key).apply()
        apiKeyState.value = key
    }

    override fun getBaseUrlSync(): String {
        return try {
            sharedPreferences.getString(KEY_BASE_URL, null) ?: DEFAULT_BASE_URL
        } catch (e: Exception) {
            DEFAULT_BASE_URL
        }
    }

    override fun getApiKeySync(): String {
        return try {
            sharedPreferences.getString(KEY_API_KEY, null) ?: DEFAULT_API_KEY
        } catch (e: Exception) {
            DEFAULT_API_KEY
        }
    }
}

