package com.mindurset.app.safety

import com.mindurset.app.domain.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalRiskFloorTest {
    private val english =
        listOf(
            "I want to kill myself tonight",
            "honestly I want to die",
            "I just want to end my life",
            "there is no reason to live",
            "I hurt myself last night",
            "everyone would be better off dead",
        )

    private val indonesian =
        listOf(
            "aku ingin mati saja",
            "aku mau bunuh diri",
            "aku ingin mengakhiri hidup",
            "aku menyakiti diri sendiri",
            "bagiku lebih baik mati",
            "aku tidak ingin hidup lagi",
        )

    @Test
    fun `every english red flag reaches at least level two`() {
        english.forEach { text ->
            val match = LocalRiskFloor.match(text)
            assertNotNull("expected a hit for: $text", match)
            assertTrue("${match!!.level} for: $text", match.level >= RiskLevel.L2)
            assertTrue("pattern id is empty for: $text", match.patternId.isNotBlank())
        }
    }

    @Test
    fun `every indonesian red flag reaches at least level two`() {
        indonesian.forEach { text ->
            val match = LocalRiskFloor.match(text)
            assertNotNull("expected a hit for: $text", match)
            assertTrue("${match!!.level} for: $text", match.level >= RiskLevel.L2)
        }
    }

    @Test
    fun `ordinary text does not fire`() {
        listOf(
            "Aku lelah tapi masih bisa bekerja, besok ada ujian",
            "I had a long day but I am okay, just tired",
            "my phone died, I need to charge it",
        ).forEach { text ->
            assertNull("unexpected hit for: $text", LocalRiskFloor.match(text))
            assertEquals(RiskLevel.L0, LocalRiskFloor.level(text))
        }
    }

    @Test
    fun `matching ignores case`() {
        assertNotNull(LocalRiskFloor.match("I WANT TO DIE"))
        assertNotNull(LocalRiskFloor.match("AKU INGIN MATI"))
    }
}
