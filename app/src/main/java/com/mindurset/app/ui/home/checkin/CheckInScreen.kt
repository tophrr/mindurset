package com.mindurset.app.ui.home.checkin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mindurset.app.R
import com.mindurset.app.di.LocalAppContainer
import com.mindurset.app.domain.model.Emotion
import com.mindurset.app.ui.theme.MoodAnxious
import com.mindurset.app.ui.theme.MoodCalm
import com.mindurset.app.ui.theme.MoodHappy
import com.mindurset.app.ui.theme.MoodLow
import com.mindurset.app.ui.theme.MoodStressed
import kotlin.math.roundToInt

/**
 * Daily check-in. The five emotion chips use the mockup's selected-state colours
 * (pale tint fill, 2 dp accent border, accent label) driven by the shared mood
 * palette. Selection is held in [CheckInViewModel] so it survives rotation.
 */
@Composable
fun CheckInScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: CheckInViewModel = viewModel {
        CheckInViewModel(container.checkInRepository, container.sessionStore)
    }
    val emotion by vm.emotion.collectAsStateWithLifecycle()
    val intensity by vm.intensity.collectAsStateWithLifecycle()
    val note by vm.note.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        CheckInTopBar(onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = stringResource(R.string.checkin_prompt),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Emotion.entries.forEach { option ->
                    EmotionChip(
                        emotion = option,
                        selected = option == emotion,
                        onClick = { vm.selectEmotion(option) },
                        modifier = Modifier.width(92.dp),
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
                    valueRange = CheckInViewModel.MIN_INTENSITY.toFloat()..
                        CheckInViewModel.MAX_INTENSITY.toFloat(),
                    steps = CheckInViewModel.MAX_INTENSITY - CheckInViewModel.MIN_INTENSITY - 1,
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = vm::setNote,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.checkin_note_label)) },
                placeholder = { Text(stringResource(R.string.checkin_note_placeholder)) },
            )

            Spacer(modifier = Modifier.size(4.dp))

            Button(
                onClick = { vm.save(onDone) },
                enabled = emotion != null && !saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.checkin_continue))
            }
        }
    }
}

@Composable
private fun CheckInTopBar(onBack: () -> Unit) {
    val backLabel = stringResource(R.string.checkin_back_cd)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.semantics { contentDescription = backLabel },
        ) {
            Text(
                text = "\u2190",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = stringResource(R.string.checkin_title),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun EmotionChip(
    emotion: Emotion,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = moodColor(emotion)
    Surface(
        modifier = modifier.selectable(
            selected = selected,
            role = Role.RadioButton,
            onClick = onClick,
        ),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) accent else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = faceFor(emotion),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = emotionText(emotion),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = hintFor(emotion),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun emotionText(emotion: Emotion): String = stringResource(
    when (emotion) {
        Emotion.HAPPY -> R.string.emotion_happy
        Emotion.CALM -> R.string.emotion_calm
        Emotion.LOW -> R.string.emotion_low
        Emotion.ANXIOUS -> R.string.emotion_anxious
        Emotion.STRESSED -> R.string.emotion_stressed
    },
)

@Composable
private fun hintFor(emotion: Emotion): String = stringResource(
    when (emotion) {
        Emotion.HAPPY -> R.string.checkin_hint_happy
        Emotion.CALM -> R.string.checkin_hint_calm
        Emotion.LOW -> R.string.checkin_hint_low
        Emotion.ANXIOUS -> R.string.checkin_hint_anxious
        Emotion.STRESSED -> R.string.checkin_hint_stressed
    },
)

private fun faceFor(emotion: Emotion): String = when (emotion) {
    Emotion.HAPPY -> "\uD83D\uDE04"
    Emotion.CALM -> "\uD83D\uDE42"
    Emotion.LOW -> "\uD83D\uDE10"
    Emotion.ANXIOUS -> "\uD83D\uDE1F"
    Emotion.STRESSED -> "\uD83D\uDE23"
}

private fun moodColor(emotion: Emotion): Color = when (emotion) {
    Emotion.HAPPY -> MoodHappy
    Emotion.CALM -> MoodCalm
    Emotion.LOW -> MoodLow
    Emotion.ANXIOUS -> MoodAnxious
    Emotion.STRESSED -> MoodStressed
}
