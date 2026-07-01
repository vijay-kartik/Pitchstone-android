package space.pitchstone.android.data.model

import com.google.gson.annotations.SerializedName

data class HealthStatusDto(
    @SerializedName("status") val status: String
)
