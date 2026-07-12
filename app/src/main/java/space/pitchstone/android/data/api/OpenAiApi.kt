package space.pitchstone.android.data.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import space.pitchstone.android.data.model.OpenAiChatRequest
import space.pitchstone.android.data.model.OpenAiChatResponse

interface OpenAiApi {
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: OpenAiChatRequest
    ): OpenAiChatResponse
}
