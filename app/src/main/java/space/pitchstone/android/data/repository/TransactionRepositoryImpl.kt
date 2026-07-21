package space.pitchstone.android.data.repository

import android.util.Base64
import retrofit2.HttpException
import space.pitchstone.android.data.api.OpenAiApi
import space.pitchstone.android.data.model.OpenAiChatRequest
import space.pitchstone.android.data.model.OpenAiContentPart
import space.pitchstone.android.data.model.OpenAiImageUrl
import space.pitchstone.android.data.model.OpenAiMessage
import space.pitchstone.android.data.storage.GatewayPreferences
import space.pitchstone.android.domain.model.Attachment
import space.pitchstone.android.domain.model.GatewayException
import space.pitchstone.android.domain.model.Transaction
import space.pitchstone.android.domain.repository.TransactionRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import com.google.gson.JsonParser

private const val DEFAULT_OPENAI_MODEL = "gpt-5.4-mini"

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val openAiApi: OpenAiApi,
    private val preferences: GatewayPreferences
) : TransactionRepository {

    // Transaction persistence is served by a backend that is not yet wired up
    // (the previous gateway was removed). These operations fail explicitly until
    // the replacement backend is integrated, rather than silently returning empty data.
    override suspend fun getTransactions(limit: Int): Result<List<Transaction>> =
        Result.failure(noBackendConfigured())

    override suspend fun getTransactionById(id: String): Result<Transaction> =
        Result.failure(noBackendConfigured())

    override suspend fun saveTransaction(rawJson: Map<String, Any?>): Result<Transaction> =
        Result.failure(noBackendConfigured())

    override suspend fun extractTransaction(text: String?, attachments: List<Attachment>): Result<String> {
        val apiKey = preferences.getOpenAiApiKeySync()
        if (apiKey.isBlank()) {
            return Result.failure(GatewayException.Unauthorized())
        }

        val contentParts = buildList {
            if (!text.isNullOrBlank()) {
                add(OpenAiContentPart(type = "text", text = text))
            }
            attachments.forEach { attachment ->
                val base64 = Base64.encodeToString(attachment.bytes, Base64.NO_WRAP)
                add(
                    OpenAiContentPart(
                        type = "image_url",
                        imageUrl = OpenAiImageUrl("data:${attachment.mimeType};base64,$base64")
                    )
                )
            }
        }
        if (contentParts.isEmpty()) {
            return Result.failure(GatewayException.BadRequest("Nothing to send — add a note or a screenshot."))
        }

        return try {
            val model = preferences.getOpenAiModelSync().ifBlank { DEFAULT_OPENAI_MODEL }
            val request = OpenAiChatRequest(
                model = model,
                messages = listOf(OpenAiMessage(role = "user", content = contentParts))
            )
            val response = openAiApi.createChatCompletion("Bearer $apiKey", request)
            val reply = response.choices.firstOrNull()?.message?.content
            if (reply.isNullOrBlank()) {
                Result.failure(GatewayException.AgentError("OpenAI returned no reply."))
            } else {
                Result.success(reply)
            }
        } catch (e: Exception) {
            Result.failure(mapToGatewayException(e))
        }
    }

    private fun noBackendConfigured(): GatewayException =
        GatewayException.ServerError("No transaction backend is configured yet.")

    private fun mapToGatewayException(e: Exception): GatewayException {
        return when (e) {
            is IOException -> GatewayException.NetworkError("Network request failed. Please check your connection or VPN status.", e)
            is HttpException -> handleHttpException(e)
            else -> GatewayException.UnknownError(e.message ?: "An unknown error occurred.", e)
        }
    }

    private fun handleHttpException(e: HttpException): GatewayException {
        val errorBody = e.response()?.errorBody()?.string()
        if (errorBody.isNullOrBlank()) {
            return when (e.code()) {
                401 -> GatewayException.Unauthorized()
                else -> GatewayException.ServerError("HTTP error code: ${e.code()}")
            }
        }

        return try {
            val jsonObject = JsonParser.parseString(errorBody).asJsonObject
            val errorMessage = jsonObject.get("error")?.asString ?: "Unknown error"

            when (e.code()) {
                401 -> GatewayException.Unauthorized()
                400 -> GatewayException.BadRequest(errorMessage)
                502 -> {
                    val detail = jsonObject.get("detail")?.asString ?: ""
                    GatewayException.AgentError(detail)
                }
                else -> GatewayException.ServerError(errorMessage)
            }
        } catch (parseException: Exception) {
            GatewayException.ServerError("Server returned error ${e.code()}: $errorBody")
        }
    }
}
