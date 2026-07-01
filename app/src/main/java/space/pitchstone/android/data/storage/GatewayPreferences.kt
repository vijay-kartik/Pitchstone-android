package space.pitchstone.android.data.storage

import kotlinx.coroutines.flow.Flow

interface GatewayPreferences {
    fun getBaseUrl(): Flow<String>
    suspend fun setBaseUrl(url: String)
    
    fun getApiKey(): Flow<String>
    suspend fun setApiKey(key: String)
    
    fun getBaseUrlSync(): String
    fun getApiKeySync(): String
}
