package space.pitchstone.android.domain.model

sealed class GatewayException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NetworkError(message: String, cause: Throwable) : GatewayException(message, cause)
    class Unauthorized : GatewayException("Invalid or missing API key. Please check your settings.")
    class BadRequest(message: String) : GatewayException(message)
    class AgentError(val detail: String) : GatewayException("Agent call failed: $detail")
    class ServerError(message: String = "Unexpected server error occurred.") : GatewayException(message)
    class UnknownError(message: String, cause: Throwable? = null) : GatewayException(message, cause)
}
