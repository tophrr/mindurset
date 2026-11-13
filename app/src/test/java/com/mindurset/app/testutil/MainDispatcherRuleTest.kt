package com.mindurset.app.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MainDispatcherRuleTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `a coroutine that switches to main runs on the test thread`() = runBlocking {
        val onMain = withContext(Dispatchers.Main) { Thread.currentThread().name }

        assertEquals(Thread.currentThread().name, onMain)
    }
}
