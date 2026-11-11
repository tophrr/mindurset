package com.mindurset.app.ui.companion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mindurset.app.R
import com.mindurset.app.di.LocalAppContainer
import com.mindurset.app.domain.model.RiskLevel
import com.mindurset.app.domain.model.SafetyEvent
import com.mindurset.app.safety.LocalRiskFloor
import com.mindurset.app.ui.common.AppCard
import com.mindurset.app.ui.common.CircleIconButton
import com.mindurset.app.ui.common.FloatingNavSpace
import com.mindurset.app.ui.profile.MindTopBar
import com.mindurset.app.ui.theme.AppIcons
import com.mindurset.app.ui.theme.CanvasBlue
import com.mindurset.app.ui.theme.MutedGray
import com.mindurset.app.ui.theme.NavyDeep
import com.mindurset.app.ui.theme.NavyInk
import com.mindurset.app.ui.theme.OutlineSoft
import com.mindurset.app.ui.theme.PaleBlue
import com.mindurset.app.ui.theme.SurfaceWhite
import kotlinx.coroutines.launch
import java.util.UUID

private const val SAFETY_EVENT_RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000

@Composable
fun CompanionScreen(onOpenDrawer: () -> Unit, onCrisis: (RiskLevel) -> Unit = {}) {
    val container = LocalAppContainer.current
    val vm: CompanionViewModel = viewModel { CompanionViewModel(container.generationClient) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val session by container.sessionStore.session.collectAsStateWithLifecycle(initialValue = null)
    var draft by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val keyboardUp = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    LaunchedEffect(state.lines.size, state.replyState) {
        if (state.lines.isNotEmpty() || state.replyState == CompanionReplyState.Sending) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    fun sendDraft() {
        val text = draft.trim()
        if (text.isBlank() || state.replyState == CompanionReplyState.Sending) return
        // Keep the local floor first: L2 and L3 messages never enter the chat or leave the device.
        val hit = LocalRiskFloor.match(text)
        if (hit != null && hit.level >= RiskLevel.L2) {
            val owner = session?.userId
            if (owner != null) {
                scope.launch {
                    val at = System.currentTimeMillis()
                    container.safetyRepository.record(
                        SafetyEvent(
                            id = UUID.randomUUID().toString(),
                            userId = owner,
                            level = hit.level,
                            trigger = "companion_message",
                            patternId = hit.patternId,
                            createdAt = at,
                            retentionUntil = at + SAFETY_EVENT_RETENTION_MILLIS,
                        ),
                    )
                }
            }
            onCrisis(hit.level)
        } else {
            vm.send(text)
            draft = ""
        }
    }

    Scaffold(
        containerColor = CanvasBlue,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            MindTopBar(
                title = stringResource(R.string.companion_title),
                onOpenDrawer = onOpenDrawer,
                navDescription = stringResource(R.string.companion_open_menu),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CanvasBlue)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .imePadding()
                .padding(top = 10.dp, bottom = if (keyboardUp) 16.dp else FloatingNavSpace + 26.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CompanionSupportMode.entries.forEach { mode ->
                        ModeChip(
                            label = stringResource(mode.labelRes),
                            selected = mode == state.supportMode,
                            enabled = state.replyState != CompanionReplyState.Sending,
                            onClick = { vm.selectMode(mode) },
                        )
                    }
                }

                if (state.lines.isEmpty()) {
                    AppCard {
                        Text(
                            text = stringResource(R.string.companion_empty_state),
                            color = NavyInk,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                        )
                    }
                }

                state.lines.forEach { line ->
                    if (line.offline) {
                        ChatBubble(
                            text = stringResource(R.string.companion_offline_reply),
                            fromUser = false,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(R.string.companion_offline_label),
                                color = NavyDeep,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(R.string.companion_offline_sentence),
                                color = MutedGray,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                            )
                        }
                    } else {
                        ChatBubble(
                            text = line.text.orEmpty(),
                            fromUser = line.fromUser,
                        )
                        if (line.sourceIds.isNotEmpty()) {
                            Text(
                                text = stringResource(
                                    R.string.companion_sources,
                                    line.sourceIds.joinToString(),
                                ),
                                color = MutedGray,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                            )
                        }
                    }
                }

                when (state.replyState) {
                    CompanionReplyState.Idle, is CompanionReplyState.Reply -> Unit

                    CompanionReplyState.Sending -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = stringResource(R.string.companion_sending),
                            color = MutedGray,
                            fontSize = 13.sp,
                        )
                    }

                    CompanionReplyState.OfflineFallback -> RetryReply(onRetry = vm::retry)

                    CompanionReplyState.Error -> RetryReply(onRetry = vm::retry, showError = true)
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(12.dp))
            MessageInputBar(
                value = draft,
                onValueChange = { draft = it },
                onSend = ::sendDraft,
                enabled = state.replyState != CompanionReplyState.Sending,
                placeholder = stringResource(R.string.companion_input_placeholder),
                sendDescription = stringResource(R.string.companion_send),
            )
        }
    }
}

@Composable
private fun RetryReply(onRetry: () -> Unit, showError: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (showError) {
            Text(
                text = stringResource(R.string.companion_error),
                color = MutedGray,
                fontSize = 12.sp,
            )
        }
        Button(onClick = onRetry) {
            Text(stringResource(R.string.companion_retry))
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val background = if (selected) PaleBlue else SurfaceWhite
    val foreground = if (selected) NavyDeep else MutedGray
    Surface(
        shape = shape,
        color = background,
        border = if (selected) null else BorderStroke(1.dp, OutlineSoft),
        onClick = onClick,
        enabled = enabled,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = AppIcons.Sparkles,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = foreground,
            )
            Text(text = label, color = foreground, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ChatBubble(text: String, fromUser: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (fromUser) 18.dp else 4.dp,
                bottomEnd = if (fromUser) 4.dp else 18.dp,
            ),
            color = if (fromUser) NavyDeep else SurfaceWhite,
            border = if (fromUser) null else BorderStroke(1.dp, OutlineSoft),
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = if (fromUser) SurfaceWhite else NavyInk,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun MessageInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
    placeholder: String,
    sendDescription: String,
) {
    Surface(shape = RoundedCornerShape(26.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                singleLine = true,
                enabled = enabled,
                textStyle = TextStyle(color = NavyInk, fontSize = 15.sp),
                cursorBrush = SolidColor(NavyDeep),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) Text(text = placeholder, color = MutedGray, fontSize = 15.sp)
                    innerTextField()
                },
            )
            SendButton(
                enabled = enabled && value.isNotBlank(),
                onClick = onSend,
                contentDescription = sendDescription,
            )
        }
    }
}

@Composable
private fun SendButton(enabled: Boolean, onClick: () -> Unit, contentDescription: String) {
    Surface(
        shape = CircleShape,
        color = if (enabled) NavyDeep else PaleBlue,
        onClick = onClick,
        modifier = Modifier.size(46.dp),
        enabled = enabled,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = AppIcons.Companion,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
                tint = if (enabled) SurfaceWhite else MutedGray,
            )
        }
    }
}
