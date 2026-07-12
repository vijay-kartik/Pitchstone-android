package space.pitchstone.android.data.storage

import kotlinx.coroutines.flow.Flow
import space.pitchstone.android.domain.model.AgentProvider

interface GatewayPreferences {
    fun getBaseUrl(): Flow<String>
    suspend fun setBaseUrl(url: String)

    fun getApiKey(): Flow<String>
    suspend fun setApiKey(key: String)

    fun getBaseUrlSync(): String
    fun getApiKeySync(): String

    fun getProvider(): Flow<AgentProvider>
    suspend fun setProvider(provider: AgentProvider)
    fun getProviderSync(): AgentProvider

    fun getOpenAiApiKey(): Flow<String>
    suspend fun setOpenAiApiKey(key: String)
    fun getOpenAiApiKeySync(): String

    fun getOpenAiModel(): Flow<String>
    suspend fun setOpenAiModel(model: String)
    fun getOpenAiModelSync(): String

    fun getAutoFallback(): Flow<Boolean>
    suspend fun setAutoFallback(enabled: Boolean)
}
