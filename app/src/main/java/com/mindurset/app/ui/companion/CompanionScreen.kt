package com.mindurset.app.ui.companion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindurset.app.R
import com.mindurset.app.ui.common.AppCard
import com.mindurset.app.ui.common.CircleIconButton
import com.mindurset.app.ui.common.FloatingNavSpace
import com.mindurset.app.ui.profile.MindTopBar
import com.mindurset.app.ui.theme.AppIcons
import com.mindurset.app.ui.theme.CanvasBlue
import com.mindurset.app.ui.theme.OutlineSoft
import com.mindurset.app.ui.theme.SurfaceWhite
import com.mindurset.app.ui.theme.MutedGray
import com.mindurset.app.ui.theme.NavyDeep
import com.mindurset.app.ui.theme.NavyInk
import com.mindurset.app.ui.theme.NavyDeep
import com.mindurset.app.ui.theme.NavyInk
import com.mindurset.app.ui.theme.PaleBlue
import com.mindurset.app.ui.theme.SurfaceWhite

/** A selectable companion mode: its chip label plus the canned preview conversation. */
private data class CompanionMode(
    val chipRes: Int,
    val botRes: List<Int>,
    val userRes: List<Int>,
)

private val CompanionModes = listOf(
    CompanionMode(
        chipRes = R.string.companion_chip_personal,
        botRes = listOf(
            R.string.companion_personal_bot,
            R.string.companion_personal_bot_2,
            R.string.companion_personal_bot_3,
            R.string.companion_personal_bot_4,
        ),
        userRes = listOf(
            R.string.companion_personal_user,
            R.string.companion_personal_user_2,
            R.string.companion_personal_user_3,
            R.string.companion_personal_user_4,
        ),
    ),
    CompanionMode(
        chipRes = R.string.companion_chip_negative_thoughts,
        botRes = listOf(
            R.string.companion_negative_bot,
            R.string.companion_negative_bot_2,
            R.string.companion_negative_bot_3,
            R.string.companion_negative_bot_4,
        ),
        userRes = listOf(
            R.string.companion_negative_user,
            R.string.companion_negative_user_2,
            R.string.companion_negative_user_3,
            R.string.companion_negative_user_4,
        ),
    ),
    CompanionMode(
        chipRes = R.string.companion_chip_emotion_regulation,
        botRes = listOf(
            R.string.companion_emotion_bot,
            R.string.companion_emotion_bot_2,
            R.string.companion_emotion_bot_3,
            R.string.companion_emotion_bot_4,
        ),
        userRes = listOf(
            R.string.companion_emotion_user,
            R.string.companion_emotion_user_2,
            R.string.companion_emotion_user_3,
            R.string.companion_emotion_user_4,
        ),
    ),
)

/**
 * Static, honest preview of the companion chat matching uidesign/AI Chat.jpg. The mode
 * chips really switch selection and swap the canned bubbles; the send button is visibly
 * disabled and nothing typed is ever sent, because no model is wired to this screen.
 *
 * The header, chips, bubbles and preview note live in a scrollable column that takes the
 * remaining height; the message input sits AFTER it, pinned to the bottom just above the
 * floating pill navigation bar, so it stays put while the chat scrolls underneath.
 */
@Composable
fun CompanionScreen(
    onOpenDrawer: () -> Unit,
) {
    var selectedMode by remember { mutableIntStateOf(0) }
    var draft by remember { mutableStateOf("") }
    // Messages the user types are kept in local state only: nothing is sent anywhere.
    var typedMessages by remember { mutableStateOf(listOf<String>()) }
    val mode = CompanionModes[selectedMode]
    val scrollState = rememberScrollState()

    // Jump to the newest message so a sent line is never left off-screen.
    LaunchedEffect(typedMessages.size) {
        if (typedMessages.isNotEmpty()) scrollState.animateScrollTo(scrollState.maxValue)
    }

    // The shell already applies the top inset; taking the system insets here as well pushed
    // the input row far above the pill navigation. WindowInsets(0) keeps the geometry exact.
    Scaffold(
        containerColor = CanvasBlue,
        contentWindowInsets = WindowInsets(0),
        // The top bar lives in the Scaffold slot so it never scrolls away.
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
                .padding(top = 10.dp, bottom = FloatingNavSpace + 26.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CompanionModes.forEachIndexed { index, item ->
                        ModeChip(
                            label = stringResource(item.chipRes),
                            selected = index == selectedMode,
                            onClick = { selectedMode = index },
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    mode.botRes.forEachIndexed { index, botRes ->
                        if (index > 0) Spacer(Modifier.height(12.dp))
                        ChatBubble(
                            text = stringResource(botRes),
                            fromUser = false,
                        )
                        Spacer(Modifier.height(12.dp))
                        ChatBubble(
                            text = stringResource(mode.userRes[index]),
                            fromUser = true,
                        )
                    }
                }

                typedMessages.forEach { message ->
                    ChatBubble(text = message, fromUser = true)
                    Spacer(Modifier.height(12.dp))
                }

                Text(
                    text = stringResource(R.string.companion_note),
                    color = MutedGray,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )

                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(12.dp))

            MessageInputBar(
                value = draft,
                onValueChange = { draft = it },
                onSend = {
                    if (draft.isNotBlank()) {
                        typedMessages = typedMessages + draft.trim()
                        draft = ""
                    }
                },
                placeholder = stringResource(R.string.companion_input_placeholder),
                sendDescription = stringResource(R.string.companion_send),
            )
        }
    }
}


@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val background = if (selected) PaleBlue else SurfaceWhite
    val foreground = if (selected) NavyDeep else MutedGray
    Surface(
        shape = shape,
        color = background,
        border = if (selected) null else BorderStroke(1.dp, OutlineSoft),
        onClick = onClick,
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
            Text(
                text = label,
                color = foreground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ChatBubble(
    text: String,
    fromUser: Boolean,
) {
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
            // The companion's own replies are white on the pale canvas, with a hairline
            // border so they stay readable without a filled tint.
            color = if (fromUser) NavyDeep else SurfaceWhite,
            border = if (fromUser) null else BorderStroke(1.dp, OutlineSoft),
            shadowElevation = 0.dp,
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

/** Floating white input capsule that stays pinned above the navigation pill. */
@Composable
private fun MessageInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    placeholder: String,
    sendDescription: String,
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = SurfaceWhite,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
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
                textStyle = TextStyle(color = NavyInk, fontSize = 15.sp),
                cursorBrush = SolidColor(NavyDeep),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(text = placeholder, color = MutedGray, fontSize = 15.sp)
                    }
                    innerTextField()
                },
            )
            SendButton(enabled = value.isNotBlank(), onClick = onSend, contentDescription = sendDescription)
        }
    }
}

@Composable
private fun SendButton(enabled: Boolean, onClick: () -> Unit, contentDescription: String) {
    val shape = CircleShape
    Surface(
        shape = shape,
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

