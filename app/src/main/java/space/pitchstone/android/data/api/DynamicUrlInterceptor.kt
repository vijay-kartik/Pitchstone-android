package space.pitchstone.android.data.api

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import space.pitchstone.android.data.storage.GatewayPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicUrlInterceptor @Inject constructor(
    private val preferences: GatewayPreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        
        // Retrieve dynamic Base URL and API key from preferences
        val rawBaseUrl = preferences.getBaseUrlSync()
        val apiKey = preferences.getApiKeySync()

        // 1. Update request URL with the configured base URL scheme, host, and port
        val baseHttpUrl = rawBaseUrl.toHttpUrlOrNull()
        if (baseHttpUrl != null) {
            val newUrl = request.url.newBuilder()
                .scheme(baseHttpUrl.scheme)
                .host(baseHttpUrl.host)
                .port(baseHttpUrl.port)
                .build()
            request = request.newBuilder().url(newUrl).build()
        }

        // 2. Inject API key header, except for the unauthenticated /healthz endpoint
        val pathSegments = request.url.pathSegments
        val isHealthCheck = pathSegments.isNotEmpty() && pathSegments.last() == "healthz"

        if (!isHealthCheck && apiKey.isNotBlank()) {
            request = request.newBuilder()
                .header("Authorization", "Bearer $apiKey")
                .build()
        }

        return chain.proceed(request)
    }
}
