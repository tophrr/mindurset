package com.mindurset.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mindurset.app.domain.model.Emotion
import com.mindurset.app.ui.theme.CardShadow
import com.mindurset.app.ui.theme.MoodAnxious
import com.mindurset.app.ui.theme.MoodCalm
import com.mindurset.app.ui.theme.MoodHappy
import com.mindurset.app.ui.theme.MoodLow
import com.mindurset.app.ui.theme.MoodStressed
import com.mindurset.app.ui.theme.SurfaceWhite

/** Height of the floating bottom pill plus its margin; screens pad their content by this. */
val FloatingNavSpace = 108.dp

/** The surface every card in the app uses: white, softly shadowed, 20 dp corners. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    // A soft but visible lift, as in the mockups: the elevation carries the shadow,
    // and clip = false keeps it from being cut off at the card's own bounds.
    val base = modifier
        .fillMaxWidth()
        .shadow(elevation = 10.dp, shape = shape, clip = false)
    if (onClick == null) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            shape = shape,
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = base,
        ) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Card(
            onClick = onClick,
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            shape = shape,
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = base,
        ) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

/** White circular icon button used in the top bars. */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(21.dp)
    Box(
        modifier = modifier
            .size(42.dp)
            .shadow(4.dp, shape, ambientColor = CardShadow, spotColor = CardShadow),
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = shape, color = SurfaceWhite, onClick = onClick) {
            Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier, label: String? = null) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        if (label != null) {
            Text(
                text = label,
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            OutlinedButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun ErrorState(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            OutlinedButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
fun emotionLabel(emotion: Emotion): String = when (emotion) {
    Emotion.HAPPY -> "Happy"
    Emotion.CALM -> "Calm"
    Emotion.LOW -> "Low"
    Emotion.ANXIOUS -> "Anxious"
    Emotion.STRESSED -> "Stressed"
}

fun Emotion.tint(): Color = when (this) {
    Emotion.HAPPY -> MoodHappy
    Emotion.CALM -> MoodCalm
    Emotion.LOW -> MoodLow
    Emotion.ANXIOUS -> MoodAnxious
    Emotion.STRESSED -> MoodStressed
}

/** Two-option segmented control: Weekly/Monthly, Recent/Calendar. */
@Composable
fun SegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .shadow(3.dp, shape, ambientColor = CardShadow, spotColor = CardShadow),
    ) {
        Surface(shape = shape, color = SurfaceWhite) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                options.forEachIndexed { index, option ->
                    val selected = index == selectedIndex
                    Box(
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else SurfaceWhite,
                            onClick = { onSelect(index) },
                        ) {
                            Box(
                                Modifier.fillMaxSize().height(36.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
