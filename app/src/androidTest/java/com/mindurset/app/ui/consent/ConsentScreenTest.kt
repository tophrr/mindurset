package com.mindurset.app.ui.consent

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mindurset.app.R
import com.mindurset.app.ui.theme.MindUrSetTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConsentScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun showsTitleAndLead() {
        composeRule.setContent {
            MindUrSetTheme {
                ConsentContent(saving = false, onAgree = {}, onLater = {})
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.consent_gate_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.consent_gate_lead)).assertIsDisplayed()
    }

    @Test
    fun laterInvokesOnlyOnLater() {
        var agreeCalls = 0
        var laterCalls = 0
        composeRule.setContent {
            MindUrSetTheme {
                ConsentContent(
                    saving = false,
                    onAgree = { agreeCalls++ },
                    onLater = { laterCalls++ },
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.consent_gate_later)).performClick()

        composeRule.runOnIdle {
            assertEquals(1, laterCalls)
            assertEquals(0, agreeCalls)
        }
    }

    @Test
    fun savingDisablesPrimaryAndInvokesNeitherCallback() {
        var agreeCalls = 0
        var laterCalls = 0
        composeRule.setContent {
            MindUrSetTheme {
                ConsentContent(
                    saving = true,
                    onAgree = { agreeCalls++ },
                    onLater = { laterCalls++ },
                )
            }
        }

        val agreeButton = composeRule.onNodeWithText(context.getString(R.string.consent_gate_agree))
        agreeButton.assertIsNotEnabled()
        agreeButton.performTouchInput { click() }

        composeRule.runOnIdle {
            assertEquals(0, agreeCalls)
            assertEquals(0, laterCalls)
        }
    }

    @Test
    fun agreeInvokesOnAgree() {
        var agreeCalls = 0
        composeRule.setContent {
            MindUrSetTheme {
                ConsentContent(saving = false, onAgree = { agreeCalls++ }, onLater = {})
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.consent_gate_agree)).performClick()

        composeRule.runOnIdle { assertEquals(1, agreeCalls) }
    }
}
