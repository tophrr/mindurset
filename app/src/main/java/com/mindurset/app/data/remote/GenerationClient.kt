package com.mindurset.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

interface GenerationTransport {
    suspend fun post(path: String, body: String): String
}

/** The Worker's own label for a response it produced without reaching the model. */
const val OFFLINE_MODEL_VERSION = "offline-fallback"

class GenerationClient(
    private val baseUrl: String,
    private val transport: GenerationTransport = HttpGenerationTransport(baseUrl),
) {
    suspend fun reply(message: String, mode: String, context: String? = null): GenerationResponse {
        require(message.isNotBlank()) { "message must not be blank" }
        require(mode in MODES) { "mode is outside the chat contract" }
        val request = JSONObject()
            .put("message", message)
            .put("mode", mode)
            .apply {
                if (!context.isNullOrBlank()) put("context", context.trim())
            }
            .toString()
        return grounded("/v1/chat", request)
    }

    /**
     * Step one of the reframe: the Worker names the thinking pattern and offers readings to choose
     * between. The request field is `thought`, which is what the Worker reads for this route.
     */
    suspend fun reframe(thought: String, context: String? = null): GenerationResponse {
        require(thought.isNotBlank()) { "thought must not be blank" }
        val request = JSONObject()
            .put("thought", thought)
            .apply {
                if (!context.isNullOrBlank()) put("context", context.trim())
            }
            .toString()
        return grounded("/v1/reframe", request)
    }

    /**
     * Summarises a finished reframe or companion transcript via `/v1/summarise`. Offline or
     * ungrounded responses throw so the client never stores canned copy as session memory.
     */
    suspend fun summarise(transcript: List<String>, notes: String? = null): GenerationResponse {
        val lines = transcript.map { it.trim() }.filter { it.isNotEmpty() }
        val cleanNotes = notes?.trim()?.takeIf { it.isNotEmpty() }
        require(lines.isNotEmpty() || cleanNotes != null) { "transcript or notes must not be empty" }
        val request = JSONObject()
            .apply {
                if (lines.isNotEmpty()) put("transcript", JSONArray(lines))
                if (cleanNotes != null) put("notes", cleanNotes)
            }
            .toString()
        return grounded("/v1/summarise", request)
    }

    private suspend fun grounded(path: String, request: String): GenerationResponse {
        // A build without an endpoint never sends the line: the screen shows the offline exercise
        // instead. This is what keeps a debug-only address out of a shipped APK.
        if (baseUrl.isBlank()) throw IOException("no generation endpoint is configured in this build")
        val response = GenerationResponseParser.parse(transport.post(path, request))
        // An answer that cites nothing, or that the Worker says it produced offline, is not grounded.
        if (response.sourceIds.isEmpty() || response.modelVersion == OFFLINE_MODEL_VERSION) {
            throw UngroundedGenerationException()
        }
        return response
    }

    private companion object {
        val MODES = setOf("listen", "reflect", "plan")
    }
}

class UngroundedGenerationException : IOException("generation response has no sources")

private class HttpGenerationTransport(private val baseUrl: String) : GenerationTransport {
    override suspend fun post(path: String, body: String): String = withContext(Dispatchers.IO) {
        val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
            val status = connection.responseCode
            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
                    ?: throw IOException("Worker returned HTTP $status")
            }
            val bytes = stream.use { it.readBounded(MAX_RESPONSE_BYTES) }
            if (status !in 200..299) throw IOException("Worker returned HTTP $status")
            bytes.toString(Charsets.UTF_8)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000
        const val MAX_RESPONSE_BYTES = 32 * 1024
    }
}

private fun InputStream.readBounded(limit: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    var total = 0
    while (true) {
        val count = read(buffer)
        if (count < 0) return output.toByteArray()
        total += count
        if (total > limit) throw IOException("Worker response is too large")
        output.write(buffer, 0, count)
    }
}
