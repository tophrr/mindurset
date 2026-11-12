package com.mindurset.app.data.remote

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GenerationResponseParserTest {
    private val fixture =
        requireNotNull(javaClass.getResource("/fixtures/generation-ok.json"))
            .readText()

    @Test
    fun parsesTheSharedContractFixture() {
        val response = GenerationResponseParser.parse(fixture)

        assertEquals(
            "You wrote that you will fail no matter what. That reads like all-or-nothing thinking: one exam is being treated as the whole degree.",
            response.message,
        )
        assertEquals(10, response.microAction.minutes)
        assertEquals(listOf("who-stress-01#p3", "cbt-thought-record#s2"), response.sourceIds)
    }

    @Test
    fun rejectsMinutesAboveTheContractLimit() {
        val invalid = JSONObject(fixture)
            .put("micro_action", JSONObject(fixture).getJSONObject("micro_action").put("minutes", 16))
            .toString()

        assertThrows(IllegalArgumentException::class.java) {
            GenerationResponseParser.parse(invalid)
        }
    }

    @Test
    fun rejectsAResponseWithoutSourceIds() {
        val invalid = JSONObject(fixture).apply { remove("source_ids") }.toString()

        assertThrows(IllegalArgumentException::class.java) {
            GenerationResponseParser.parse(invalid)
        }
    }
}
