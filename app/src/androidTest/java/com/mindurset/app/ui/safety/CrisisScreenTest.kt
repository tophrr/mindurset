package com.mindurset.app.ui.safety

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mindurset.app.R
import com.mindurset.app.domain.model.RiskLevel
import com.mindurset.app.safety.CrisisResources
import com.mindurset.app.ui.theme.MindUrSetTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for the stateless crisis card.
 *
 * Every string is resolved from resources, so the assertions hold in `values-in/` too. The screen
 * takes its actions as lambdas, so each test can count calls without a Context, an Intent or a
 * ViewModel — which is the point of splitting the route from the content.
 */
@RunWith(AndroidJUnit4::class)
class CrisisScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun text(resourceId: Int): String = context.getString(resourceId)

    private fun setContent(
        level: RiskLevel,
        onCall: () -> Unit = {},
        onOpenContactSheet: () -> Unit = {},
        onAcknowledge: () -> Unit = {},
    ) {
        composeRule.setContent {
            MindUrSetTheme {
                CrisisContent(
                    level = level,
                    onCall = onCall,
                    onOpenContactSheet = onOpenContactSheet,
                    onAcknowledge = onAcknowledge,
                )
            }
        }
    }

    @Test
    fun levelThreeShowsTheCrisisTitleAndTheHotlineNumber() {
        setContent(RiskLevel.L3)

        composeRule.onNodeWithText(text(R.string.crisis_title)).assertIsDisplayed()
        composeRule.onNodeWithText(CrisisResources.display()).assertIsDisplayed()
    }

    @Test
    fun levelTwoShowsTheSofterTitleAndNotTheCrisisTitle() {
        setContent(RiskLevel.L2)

        composeRule.onNodeWithText(text(R.string.crisis_title_l2)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.crisis_title)).assertDoesNotExist()
    }

    @Test
    fun theAcknowledgeButtonCarriesTheLevelSpecificLabelAndCallsBackOnce() {
        var acknowledged = 0
        setContent(RiskLevel.L3, onAcknowledge = { acknowledged += 1 })

        composeRule
            .onNodeWithText(text(R.string.crisis_acknowledge))
            .performScrollTo()
            .performClick()

        assertEquals(1, acknowledged)
    }

    @Test
    fun theCallAndContactButtonsInvokeTheirCallbacksOnce() {
        var calls = 0
        var sheets = 0
        setContent(
            RiskLevel.L3,
            onCall = { calls += 1 },
            onOpenContactSheet = { sheets += 1 },
        )

        composeRule
            .onNodeWithText(text(R.string.crisis_call))
            .performScrollTo()
            .performClick()
        composeRule
            .onNodeWithText(text(R.string.crisis_contact_action))
            .performScrollTo()
            .performClick()

        assertEquals(1, calls)
        assertEquals(1, sheets)
    }
}
