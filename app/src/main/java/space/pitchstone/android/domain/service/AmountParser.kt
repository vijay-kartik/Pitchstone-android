package space.pitchstone.android.domain.service

object AmountParser {
    fun parse(raw: String?): Int {
        if (raw.isNullOrBlank()) return 0
        return raw
            .replace("₹", "")
            .replace(",", "")
            .replace(" ", "")
            .split(".")[0]
            .toIntOrNull() ?: 0
    }
}
