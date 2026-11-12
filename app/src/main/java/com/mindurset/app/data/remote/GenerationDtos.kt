package com.mindurset.app.data.remote

import org.json.JSONArray
import org.json.JSONObject

data class GenerationResponse(
    val message: String,
    val distortion: String,
    val perspectives: List<String>,
    val microAction: MicroActionDto,
    val sourceIds: List<String>,
    val riskLevel: RiskLevelDto,
    val promptVersion: String,
    val modelVersion: String,
)

data class MicroActionDto(val title: String, val minutes: Int)

data class RiskLevelDto(val level: String, val confidence: Double)

/** Strict v1 parser: unknown, missing, or out-of-range fields are rejected. */
object GenerationResponseParser {
    private val sourceIdPattern = Regex("^[a-z0-9-]+#[a-z0-9-]+$")
    private val promptVersionPattern = Regex("^v[0-9]+\\.[0-9]+\\.[0-9]+$")

    fun parse(json: String): GenerationResponse {
        val root = JSONObject(json)
        root.requireKeys(
            "message",
            "distortion",
            "perspectives",
            "micro_action",
            "source_ids",
            "risk_level",
            "prompt_version",
            "model_version",
        )
        val message = root.requiredString("message", 1..600)
        val distortion = root.requiredString("distortion", 1..80)
        val perspectives = root.requiredArray("perspectives").let { array ->
            require(array.length() in 2..3) { "perspectives must contain 2 to 3 items" }
            (0 until array.length()).map { index -> array.requiredString(index, 1..300) }
        }
        val action = root.requiredObject("micro_action").apply { requireKeys("title", "minutes") }
        val microAction = MicroActionDto(
            title = action.requiredString("title", 1..120),
            minutes = action.requiredInteger("minutes", 1..15),
        )
        val sourceArray = root.requiredArray("source_ids")
        val sourceIds = (0 until sourceArray.length()).map { index ->
            val id = sourceArray.requiredString(index, 1..Int.MAX_VALUE)
            require(sourceIdPattern.matches(id)) { "source_ids contains an invalid id" }
            id
        }
        val risk = root.requiredObject("risk_level").apply { requireKeys("level", "confidence") }
        val riskLevel = RiskLevelDto(
            level = risk.requiredString("level", 2..2).also {
                require(it in setOf("L0", "L1", "L2", "L3")) { "risk level is outside v1" }
            },
            confidence = risk.requiredNumber("confidence").also {
                require(it in 0.0..1.0) { "risk confidence is outside v1" }
            },
        )
        val promptVersion = root.requiredString("prompt_version", 1..Int.MAX_VALUE).also {
            require(promptVersionPattern.matches(it)) { "prompt_version is outside v1" }
        }
        val modelVersion = root.requiredString("model_version", 1..Int.MAX_VALUE)
        return GenerationResponse(
            message = message,
            distortion = distortion,
            perspectives = perspectives,
            microAction = microAction,
            sourceIds = sourceIds,
            riskLevel = riskLevel,
            promptVersion = promptVersion,
            modelVersion = modelVersion,
        )
    }

    private fun JSONObject.requireKeys(vararg expected: String) {
        val actual = keys().asSequence().toSet()
        require(actual == expected.toSet()) { "object fields do not match generation v1" }
    }

    private fun JSONObject.requiredString(name: String, length: IntRange): String {
        val value = get(name)
        require(value is String && value.length in length) { "$name is outside generation v1" }
        return value
    }

    private fun JSONObject.requiredArray(name: String): JSONArray = get(name) as? JSONArray
        ?: throw IllegalArgumentException("$name must be an array")

    private fun JSONObject.requiredObject(name: String): JSONObject = get(name) as? JSONObject
        ?: throw IllegalArgumentException("$name must be an object")

    private fun JSONObject.requiredInteger(name: String, range: IntRange): Int {
        val value = get(name)
        require(value is Number && value.toDouble() % 1.0 == 0.0) { "$name must be an integer" }
        val integer = value.toInt()
        require(integer in range) { "$name is outside generation v1" }
        return integer
    }

    private fun JSONObject.requiredNumber(name: String): Double {
        val value = get(name)
        require(value is Number) { "$name must be a number" }
        return value.toDouble()
    }

    private fun JSONArray.requiredString(index: Int, length: IntRange): String {
        val value = get(index)
        require(value is String && value.length in length) { "array item is outside generation v1" }
        return value
    }
}
