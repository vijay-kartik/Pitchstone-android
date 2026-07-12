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
import space.pitchstone.android.domain.model.AgentProvider

@Singleton
class GatewayPreferencesImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GatewayPreferences {
    
    companion object {
        private const val PREFS_FILE_NAME = "gateway_secure_prefs"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_PROVIDER = "agent_provider"
        private const val KEY_OPENAI_API_KEY = "openai_api_key"
        private const val KEY_OPENAI_MODEL = "openai_model"
        private const val KEY_AUTO_FALLBACK = "auto_fallback"
        private const val DEFAULT_BASE_URL = "https://kartiks-mac-mini-7.tailafb282.ts.net"
        private const val DEFAULT_OPENAI_MODEL = "gpt-5.4-mini"
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

    private val providerState = MutableStateFlow(readProvider())
    private val openAiApiKeyState = MutableStateFlow(readString(KEY_OPENAI_API_KEY, ""))
    private val openAiModelState = MutableStateFlow(readString(KEY_OPENAI_MODEL, DEFAULT_OPENAI_MODEL))
    private val autoFallbackState = MutableStateFlow(readBoolean(KEY_AUTO_FALLBACK, false))

    override fun getProvider(): Flow<AgentProvider> = providerState.asStateFlow()
    override fun getProviderSync(): AgentProvider = providerState.value

    override suspend fun setProvider(provider: AgentProvider) {
        sharedPreferences.edit().putString(KEY_PROVIDER, provider.name).apply()
        providerState.value = provider
    }

    override fun getOpenAiApiKey(): Flow<String> = openAiApiKeyState.asStateFlow()
    override fun getOpenAiApiKeySync(): String = openAiApiKeyState.value

    override suspend fun setOpenAiApiKey(key: String) {
        sharedPreferences.edit().putString(KEY_OPENAI_API_KEY, key.trim()).apply()
        openAiApiKeyState.value = key.trim()
    }

    override fun getOpenAiModel(): Flow<String> = openAiModelState.asStateFlow()
    override fun getOpenAiModelSync(): String = openAiModelState.value

    override suspend fun setOpenAiModel(model: String) {
        sharedPreferences.edit().putString(KEY_OPENAI_MODEL, model).apply()
        openAiModelState.value = model
    }

    override fun getAutoFallback(): Flow<Boolean> = autoFallbackState.asStateFlow()

    override suspend fun setAutoFallback(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_FALLBACK, enabled).apply()
        autoFallbackState.value = enabled
    }

    private fun readProvider(): AgentProvider = try {
        sharedPreferences.getString(KEY_PROVIDER, null)
            ?.let { AgentProvider.valueOf(it) }
            ?: AgentProvider.GATEWAY
    } catch (e: Exception) {
        AgentProvider.GATEWAY
    }

    private fun readString(key: String, default: String): String = try {
        sharedPreferences.getString(key, null) ?: default
    } catch (e: Exception) {
        default
    }

    private fun readBoolean(key: String, default: Boolean): Boolean = try {
        sharedPreferences.getBoolean(key, default)
    } catch (e: Exception) {
        default
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

