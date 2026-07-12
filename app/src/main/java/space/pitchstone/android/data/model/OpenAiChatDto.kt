package space.pitchstone.android.data.model

import com.google.gson.annotations.SerializedName

data class OpenAiChatRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<OpenAiMessage>,
    @SerializedName("max_tokens") val maxTokens: Int = 1024
)

data class OpenAiMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: List<OpenAiContentPart>
)

/** Either a text part (`text` set) or an image part (`imageUrl` set) — mirrors OpenAI's tagged union. */
data class OpenAiContentPart(
    @SerializedName("type") val type: String,
    @SerializedName("text") val text: String? = null,
    @SerializedName("image_url") val imageUrl: OpenAiImageUrl? = null
)

data class OpenAiImageUrl(
    @SerializedName("url") val url: String
)

data class OpenAiChatResponse(
    @SerializedName("choices") val choices: List<OpenAiChoice>
)

data class OpenAiChoice(
    @SerializedName("message") val message: OpenAiResponseMessage
)

data class OpenAiResponseMessage(
    @SerializedName("content") val content: String?
)
