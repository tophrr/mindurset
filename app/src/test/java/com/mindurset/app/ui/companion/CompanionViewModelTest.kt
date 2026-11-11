package com.mindurset.app.ui.companion

import com.mindurset.app.data.remote.GenerationClient
import com.mindurset.app.data.remote.GenerationTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionViewModelTest {
    private val fixture =
        requireNotNull(javaClass.getResource("/fixtures/generation-ok.json"))
            .readText()

    @Test
    fun sendsTheSelectedModeAndRendersGroundedReply() {
        val transport = QueuedTransport(fixture)
        val (viewModel, scope) = viewModelWith(transport)

        assertEquals(CompanionReplyState.Idle, viewModel.uiState.value.replyState)
        viewModel.selectMode(CompanionSupportMode.NEGATIVE_THOUGHTS)
        viewModel.send("A busy day")

        assertEquals("/v1/chat", transport.lastPath)
        assertEquals("reflect", JSONObject(requireNotNull(transport.lastBody)).getString("mode"))
        assertTrue(viewModel.uiState.value.replyState is CompanionReplyState.Reply)
        assertEquals(
            listOf("who-stress-01#p3", "cbt-thought-record#s2"),
            viewModel.uiState.value.lines.last().sourceIds,
        )
        scope.cancel()
    }

    @Test
    fun ungroundedWorkerReplyUsesOfflineFallback() {
        val ungrounded = JSONObject(fixture).put("source_ids", JSONArray()).toString()
        val (viewModel, scope) = viewModelWith(QueuedTransport(ungrounded))

        viewModel.send("A busy day")

        assertEquals(CompanionReplyState.OfflineFallback, viewModel.uiState.value.replyState)
        assertTrue(viewModel.uiState.value.lines.last().offline)
        scope.cancel()
    }

    @Test
    fun invalidContractReplyUsesOfflineCopyAndRetryCanSucceed() {
        val invalid = JSONObject(fixture)
            .put("micro_action", JSONObject(fixture).getJSONObject("micro_action").put("minutes", 16))
            .toString()
        val transport = QueuedTransport(invalid, fixture)
        val (viewModel, scope) = viewModelWith(transport)

        viewModel.send("A busy day")
        assertEquals(CompanionReplyState.Error, viewModel.uiState.value.replyState)
        assertTrue(viewModel.uiState.value.lines.last().offline)

        viewModel.retry()

        assertTrue(viewModel.uiState.value.replyState is CompanionReplyState.Reply)
        scope.cancel()
    }

    private fun viewModelWith(transport: QueuedTransport): Pair<CompanionViewModel, CoroutineScope> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        return CompanionViewModel(
            generationClient = GenerationClient("http://localhost:8787", transport),
            externalScope = scope,
        ) to scope
    }

    private class QueuedTransport(vararg responses: String) : GenerationTransport {
        private val queued = responses.toMutableList()
        var lastPath: String? = null
            private set
        var lastBody: String? = null
            private set

        override suspend fun post(path: String, body: String): String {
            lastPath = path
            lastBody = body
            return queued.removeAt(0)
        }
    }
}
