package com.mindurset.app.ui.home.checkin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mindurset.app.R
import com.mindurset.app.di.LocalAppContainer
import com.mindurset.app.domain.model.Emotion
import com.mindurset.app.ui.common.CircleIconButton
import com.mindurset.app.ui.common.FloatingNavSpace
import com.mindurset.app.ui.common.emotionLabel
import com.mindurset.app.ui.common.tint
import com.mindurset.app.ui.theme.AppIcons
import com.mindurset.app.ui.theme.MutedGray
import com.mindurset.app.ui.theme.NavyInk
import kotlin.math.roundToInt

/**
 * Daily check-in. Full-width mood cards use the mood palette's selected state
 * (tinted fill, accent border, accent label and a check glyph), followed by the
 * intensity slider with its value, the optional note and the Continue button.
 * Selection is held in [CheckInViewModel] so it survives rotation.
 */
@Composable
fun CheckInScreen(onDone: () -> Unit, onBack: () -> Unit, initialEmotion: Emotion? = null) {
    val container = LocalAppContainer.current
    val vm: CheckInViewModel = viewModel {
        CheckInViewModel(
            checkInRepository = container.checkInRepository,
            sessionStore = container.sessionStore,
            initialEmotion = initialEmotion,
        )
    }
    val emotion by vm.emotion.collectAsStateWithLifecycle()
    val intensity by vm.intensity.collectAsStateWithLifecycle()
    val note by vm.note.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()

    val noteFocus = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    // Reached by pressing Continue on the home card, the note is what the user came for, so
    // the field takes focus straight away and the column scrolls to the end: the input then
    // sits just above the keyboard instead of hiding behind it.
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    // The pill navigation is hidden behind the keyboard, so its reserved space is dropped
    // while typing: the note and Continue then sit right above the keyboard.
    val bottomSpace = if (imeBottom > 0) 16.dp else FloatingNavSpace
    LaunchedEffect(Unit) { noteFocus.requestFocus() }
    LaunchedEffect(imeBottom) {
        if (imeBottom > 0) scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
    ) {
        CheckInTopBar(onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 20.dp, end = 20.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = stringResource(R.string.checkin_prompt),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Emotion.entries.forEach { option ->
                    MoodCard(
                        emotion = option,
                        selected = option == emotion,
                        onClick = { vm.selectEmotion(option) },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.checkin_intensity_label),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.checkin_intensity_value, intensity),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Slider(
                    value = intensity.toFloat(),
                    onValueChange = { vm.setIntensity(it.roundToInt()) },
                    valueRange = CheckInViewModel.MIN_INTENSITY.toFloat()..CheckInViewModel.MAX_INTENSITY.toFloat(),
                    steps = CheckInViewModel.MAX_INTENSITY - CheckInViewModel.MIN_INTENSITY - 1,
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = vm::setNote,
                modifier = Modifier.fillMaxWidth().focusRequester(noteFocus),
                singleLine = true,
                label = { Text(stringResource(R.string.checkin_note_label)) },
                placeholder = { Text(stringResource(R.string.checkin_note_placeholder)) },
            )

            Button(
                onClick = { vm.save(onDone) },
                enabled = emotion != null && !saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.checkin_continue))
            }

            Spacer(modifier = Modifier.size(bottomSpace))
        }
    }
}

@Composable
private fun CheckInTopBar(onBack: () -> Unit) {
    val backLabel = stringResource(R.string.checkin_back_cd)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircleIconButton(
            icon = AppIcons.ArrowBack,
            contentDescription = backLabel,
            onClick = onBack,
        )
        Text(
            text = stringResource(R.string.checkin_title),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun MoodCard(emotion: Emotion, selected: Boolean, onClick: () -> Unit) {
    val accent = emotion.tint()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) accent else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = moodFaceIcon(emotion),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (selected) accent else NavyInk,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = emotionLabel(emotion),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) accent else MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = emotionHint(emotion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray,
                )
            }
            if (selected) {
                Icon(
                    imageVector = AppIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = accent,
                )
            }
        }
    }
}

private fun moodFaceIcon(emotion: Emotion): androidx.compose.ui.graphics.vector.ImageVector = when (emotion) {
    Emotion.HAPPY -> AppIcons.FaceHappy
    Emotion.CALM -> AppIcons.FaceCalm
    Emotion.LOW -> AppIcons.FaceLow
    Emotion.ANXIOUS -> AppIcons.FaceAnxious
    Emotion.STRESSED -> AppIcons.FaceStressed
}

@Composable
private fun emotionHint(emotion: Emotion): String = stringResource(
    when (emotion) {
        Emotion.HAPPY -> R.string.checkin_hint_happy
        Emotion.CALM -> R.string.checkin_hint_calm
        Emotion.LOW -> R.string.checkin_hint_low
        Emotion.ANXIOUS -> R.string.checkin_hint_anxious
        Emotion.STRESSED -> R.string.checkin_hint_stressed
    },
)
