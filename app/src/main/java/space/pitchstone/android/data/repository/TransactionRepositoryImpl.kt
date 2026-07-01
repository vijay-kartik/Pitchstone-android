package space.pitchstone.android.data.repository

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import space.pitchstone.android.data.api.GatewayApi
import space.pitchstone.android.domain.model.Attachment
import space.pitchstone.android.domain.model.GatewayException
import space.pitchstone.android.domain.model.Transaction
import space.pitchstone.android.domain.repository.TransactionRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import com.google.gson.JsonParser

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val api: GatewayApi
) : TransactionRepository {

    override suspend fun getTransactions(limit: Int): Result<List<Transaction>> {
        return try {
            val response = api.getTransactions(limit)
            Result.success(response.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(mapToGatewayException(e))
        }
    }

    override suspend fun getTransactionById(id: String): Result<Transaction> {
        return try {
            val response = api.getTransactionById(id)
            Result.success(response.toDomain())
        } catch (e: Exception) {
            Result.failure(mapToGatewayException(e))
        }
    }

    override suspend fun saveTransaction(rawJson: Map<String, Any?>): Result<Transaction> {
        return try {
            val response = api.saveTransaction(rawJson)
            Result.success(response.toDomain())
        } catch (e: Exception) {
            Result.failure(mapToGatewayException(e))
        }
    }

    override suspend fun extractTransaction(text: String?, attachments: List<Attachment>): Result<String> {
        return try {
            val textBody = text?.toRequestBody("text/plain".toMediaTypeOrNull())
            
            val filesParts = attachments.map { attachment ->
                val requestBody = attachment.bytes.toRequestBody(attachment.mimeType.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("files", attachment.name, requestBody)
            }
            
            val response = api.extractTransaction(textBody, filesParts)
            Result.success(response.reply)
        } catch (e: Exception) {
            Result.failure(mapToGatewayException(e))
        }
    }

    override suspend fun pingGateway(): Result<Boolean> {
        return try {
            val response = api.getHealth()
            Result.success(response.status == "ok")
        } catch (e: Exception) {
            Result.failure(mapToGatewayException(e))
        }
    }

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
