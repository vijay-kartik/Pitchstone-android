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

@Singleton
class GatewayPreferencesImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GatewayPreferences {

    companion object {
        private const val PREFS_FILE_NAME = "gateway_secure_prefs"
        private const val KEY_OPENAI_API_KEY = "openai_api_key"
        private const val KEY_OPENAI_MODEL = "openai_model"
        private const val DEFAULT_OPENAI_MODEL = "gpt-5.4-mini"
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

    private val openAiApiKeyState = MutableStateFlow(readString(KEY_OPENAI_API_KEY, ""))
    private val openAiModelState = MutableStateFlow(readString(KEY_OPENAI_MODEL, DEFAULT_OPENAI_MODEL))

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

    private fun readString(key: String, default: String): String = try {
        sharedPreferences.getString(key, null) ?: default
    } catch (e: Exception) {
        default
    }
}
