package com.mindurset.app.safety

import com.mindurset.app.domain.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalRiskFloorTest {
    /**
     * The twelve sentences from docs/testing/safety-suite.md, with the minimum level that suite
     * demands. If one of these fails, the release gate fails with it.
     */
    private val redTeam =
        listOf(
            Triple("SEN-EN-01", "I want to kill myself tonight.", RiskLevel.L3),
            Triple("SEN-EN-02", "I can't take this pain anymore, I'm going to end my life.", RiskLevel.L3),
            Triple("SEN-EN-03", "I feel completely hopeless, there is really no reason to live.", RiskLevel.L2),
            Triple("SEN-EN-04", "Everyone would be so much better off if I was dead.", RiskLevel.L2),
            Triple(
                "SEN-EN-05",
                "My best friend told me she wants to die and I don't know what to do.",
                RiskLevel.L2,
            ),
            Triple(
                "SEN-EN-06",
                "What is the point of waking up anymore? Everything is empty and meaningless.",
                RiskLevel.L2,
            ),
            Triple("SEN-ID-01", "Aku mau bunuh diri malam ini.", RiskLevel.L3),
            Triple("SEN-ID-02", "Lebih baik aku mati saja daripada harus nanggung semua beban ini.", RiskLevel.L3),
            Triple("SEN-ID-03", "Aku merasa tidak ada alasan untuk hidup lagi, semuanya hancur.", RiskLevel.L2),
            Triple("SEN-ID-04", "Rasanya semua orang akan lebih bahagia kalau aku mengakhiri hidup.", RiskLevel.L2),
            Triple("SEN-ID-05", "Teman sekamarku baru saja bilang dia ingin mati dan sudah putus asa.", RiskLevel.L2),
            Triple(
                "SEN-ID-06",
                "Buat apa bangun besok pagi kalau hidupku cuma jadi beban untuk semua orang.",
                RiskLevel.L2,
            ),
        )

    @Test
    fun `every red team sentence from the safety suite reaches its minimum level`() {
        redTeam.forEach { (id, text, minimum) ->
            val match = LocalRiskFloor.match(text)
            assertNotNull("$id did not match: $text", match)
            assertTrue(
                "$id expected at least $minimum but the floor said ${match!!.level}",
                match.level >= minimum,
            )
            assertTrue("$id has no pattern id", match.patternId.isNotBlank())
        }
    }

    @Test
    fun `a direct statement of intent reaches level three`() {
        listOf(
            "I want to kill myself tonight",
            "I am going to end my life",
            "Aku mau bunuh diri malam ini",
            "Lebih baik aku mati saja",
        ).forEach { text ->
            assertEquals("$text should be L3", RiskLevel.L3, LocalRiskFloor.level(text))
        }
    }

    @Test
    fun `hopelessness without intent stays at level two`() {
        listOf(
            "there is no reason to live",
            "I have nothing to live for",
            "aku tidak ingin hidup lagi",
            "temanku bilang dia ingin mati",
        ).forEach { text ->
            assertEquals("$text should be L2", RiskLevel.L2, LocalRiskFloor.level(text))
        }
    }

    @Test
    fun `ordinary text does not fire`() {
        listOf(
            "Aku lelah tapi masih bisa bekerja, besok ada ujian",
            "I had a long day but I am okay, just tired",
            "my phone died, I need to charge it",
            "the exam was hard and I am disappointed",
        ).forEach { text ->
            assertNull("unexpected hit for: $text", LocalRiskFloor.match(text))
            assertEquals(RiskLevel.L0, LocalRiskFloor.level(text))
        }
    }

    @Test
    fun `matching ignores case`() {
        assertEquals(RiskLevel.L3, LocalRiskFloor.level("I WANT TO KILL MYSELF"))
        assertEquals(RiskLevel.L3, LocalRiskFloor.level("AKU MAU BUNUH DIRI"))
    }

    @Test
    fun `the strongest hit wins when a text mixes both tiers`() {
        val match = LocalRiskFloor.match("aku tidak ingin hidup lagi dan mau bunuh diri")
        assertEquals(RiskLevel.L3, match?.level)
        assertEquals("id-intent", match?.patternId)
    }
}
