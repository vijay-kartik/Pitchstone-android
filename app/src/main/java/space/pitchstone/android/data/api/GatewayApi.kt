package space.pitchstone.android.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import space.pitchstone.android.data.model.AgentResponseDto
import space.pitchstone.android.data.model.HealthStatusDto
import space.pitchstone.android.data.model.TransactionDto

interface GatewayApi {

    @GET("healthz")
    suspend fun getHealth(): HealthStatusDto

    @Multipart
    @POST("agent")
    suspend fun extractTransaction(
        @Part("text") text: RequestBody?,
        @Part files: List<MultipartBody.Part>
    ): AgentResponseDto

    @POST("transactions")
    suspend fun saveTransaction(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): TransactionDto

    @GET("transactions")
    suspend fun getTransactions(
        @Query("limit") limit: Int
    ): List<TransactionDto>

    @GET("transactions/{id}")
    suspend fun getTransactionById(
        @Path("id") id: String
    ): TransactionDto
}
