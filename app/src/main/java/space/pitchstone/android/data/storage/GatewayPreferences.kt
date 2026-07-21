package space.pitchstone.android.data.storage

import kotlinx.coroutines.flow.Flow

interface GatewayPreferences {
    fun getOpenAiApiKey(): Flow<String>
    suspend fun setOpenAiApiKey(key: String)
    fun getOpenAiApiKeySync(): String

    fun getOpenAiModel(): Flow<String>
    suspend fun setOpenAiModel(model: String)
    fun getOpenAiModelSync(): String
}
