package space.pitchstone.android.data.model

import com.google.gson.annotations.SerializedName

data class AgentResponseDto(
    @SerializedName("reply") val reply: String,
    @SerializedName("filesReceived") val filesReceived: List<String>?
)
