package cc.tomko.outify.data.metadata

import kotlinx.coroutines.delay
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

data class NativeError(
    val type: String,
    val message: String,
    val retryAfterSeconds: Long? = null
)

data class NativeResult(
    val metadata: JSONObject? = null,
    val error: NativeError? = null
)

@Singleton
class NativeMetadata @Inject constructor() {
    fun fetchMetadata(uri: String): JSONObject {
        val result = getNativeResult(uri)

        result.error?.let { err ->
            when (err.type) {
                "rate_limit" -> throw RateLimitException(err.message, err.retryAfterSeconds)
                else -> throw Exception(err.message)
            }
        }

        return result.metadata ?: throw Exception("Metadata is null")
    }

    private fun getNativeResult(uri: String): NativeResult {
        val result = getNativeMetadata(uri)
        val obj = JSONObject(result)

        return if (obj.has("error")) {
            val err = obj.getJSONObject("error")
            NativeResult(
                metadata = null,
                error = NativeError(
                    type = err.getString("type"),
                    message = err.getString("message"),
                    retryAfterSeconds = if (err.isNull("retry_after_seconds")) null else err.getLong("retry_after_seconds")
                )
            )
        } else {
            NativeResult(metadata = obj, error = null)
        }
    }

    suspend fun <T> retryOnRateLimit(
        maxAttempts: Int = 5,
        baseDelayMs: Long = 1000L,
        block: suspend () -> T
    ): T {
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (e: RateLimitException) {
                attempt++
                if (attempt >= maxAttempts) throw e

                val backoffMs = e.retryAfterSeconds?.times(1000L) ?: (baseDelayMs * (1L shl (attempt - 1)))
                val jitter = Random.nextLong(0, (backoffMs / 3).coerceAtLeast(1L))
                val delayMs = backoffMs + jitter

                delay(delayMs)
            }
        }
    }

    external fun getNativeMetadata(uri: String): String
}