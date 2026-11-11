package com.mindurset.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

interface GenerationTransport {
    suspend fun post(path: String, body: String): String
}

class GenerationClient(
    private val baseUrl: String,
    private val transport: GenerationTransport = HttpGenerationTransport(baseUrl),
) {
    suspend fun reply(message: String, mode: String): GenerationResponse {
        require(message.isNotBlank()) { "message must not be blank" }
        require(mode in MODES) { "mode is outside the chat contract" }
        val request = JSONObject().put("message", message).put("mode", mode).toString()
        val response = GenerationResponseParser.parse(transport.post("/v1/chat", request))
        if (response.sourceIds.isEmpty()) throw UngroundedGenerationException()
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
