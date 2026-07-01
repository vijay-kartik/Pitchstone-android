package space.pitchstone.android.presentation.util

import org.json.JSONObject

interface JsonExtractionStrategy {
    fun extract(reply: String): JSONObject?
}

class DirectParseStrategy : JsonExtractionStrategy {
    override fun extract(reply: String): JSONObject? {
        return try {
            JSONObject(reply.trim())
        } catch (e: Exception) {
            null
        }
    }
}

class FencedCodeBlockStrategy : JsonExtractionStrategy {
    private val regex = Regex("```(?:json)?\\s*\\n?([\\s\\S]*?)\\n?```", RegexOption.IGNORE_CASE)

    override fun extract(reply: String): JSONObject? {
        return try {
            val matchResult = regex.find(reply)
            val jsonContent = matchResult?.groups?.get(1)?.value?.trim()
            if (jsonContent != null) {
                JSONObject(jsonContent)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

class BraceMatchingStrategy : JsonExtractionStrategy {
    override fun extract(reply: String): JSONObject? {
        return try {
            val startIndex = reply.indexOf('{')
            val endIndex = reply.lastIndexOf('}')
            if (startIndex in 0..<endIndex) {
                val jsonContent = reply.substring(startIndex, endIndex + 1).trim()
                JSONObject(jsonContent)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

class JsonExtractor {
    private val strategies = listOf(
        DirectParseStrategy(),
        FencedCodeBlockStrategy(),
        BraceMatchingStrategy()
    )

    fun extractJson(reply: String): Map<String, Any?>? {
        for (strategy in strategies) {
            val jsonObject = strategy.extract(reply)
            if (jsonObject != null) {
                return toMap(jsonObject)
            }
        }
        return null
    }

    private fun toMap(jsonObject: JSONObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonObject.get(key)
            if (value == JSONObject.NULL) {
                map[key] = null
            } else if (value is JSONObject) {
                map[key] = toMap(value)
            } else {
                map[key] = value
            }
        }
        return map
    }
}
