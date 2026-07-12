package space.pitchstone.android.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.pitchstone.android.ui.theme.JetBrainsMono
import space.pitchstone.android.ui.theme.PitchstoneColors

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
        color = PitchstoneColors.TextMuted,
        modifier = modifier
    )
}

@Composable
fun MonoText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = PitchstoneColors.OnBackground,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp
) {
    Text(
        text = text,
        fontFamily = JetBrainsMono,
        fontSize = fontSize,
        color = color,
        modifier = modifier
    )
}

@Composable
fun HairlineDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 0.5.dp,
        color = PitchstoneColors.Outline
    )
}

@Composable
fun StatusDot(
    label: String,
    color: Color = PitchstoneColors.Accent,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = PitchstoneColors.OnSurfaceVariant
        )
    }
}

@Composable
fun PaceBar(
    ratio: Float,
    modifier: Modifier = Modifier,
    tickRatio: Float? = null
) {
    // Doc rule: green <80% · amber ≥80% · red over cap
    val color = when {
        ratio > 1f -> PitchstoneColors.Danger
        ratio >= 0.8f -> PitchstoneColors.Warn
        else -> PitchstoneColors.Accent
    }
    val barHeight = 5.dp
    val containerHeight = if (tickRatio != null) 11.dp else barHeight

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(containerHeight),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(100.dp))
                .background(PitchstoneColors.Outline)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio.coerceIn(0f, 1f))
                    .height(barHeight)
                    .clip(RoundedCornerShape(100.dp))
                    .background(color)
            )
        }
        if (tickRatio != null) {
            Canvas(modifier = Modifier.fillMaxWidth().height(containerHeight)) {
                val x = size.width * tickRatio.coerceIn(0f, 1f)
                drawLine(
                    color = Color.White.copy(alpha = 0.3f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }
    }
}

@Composable
fun AccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PitchstoneColors.Accent,
            contentColor = PitchstoneColors.Background,
            disabledContainerColor = PitchstoneColors.Outline,
            disabledContentColor = PitchstoneColors.OnSurfaceVariant
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (accent) PitchstoneColors.Accent else PitchstoneColors.OnBackground
        ),
        border = BorderStroke(
            1.dp,
            if (accent) PitchstoneColors.Accent else PitchstoneColors.Outline
        )
    ) {
        Text(text, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun MonoPill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = PitchstoneColors.Accent
) {
    Box(
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(100.dp))
            .padding(horizontal = 11.dp, vertical = 5.dp)
    ) {
        MonoText(text = text, color = color, fontSize = 10.sp)
    }
}

@Composable
fun ThinkingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "thinking")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1050, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dots"
    )
    val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(PitchstoneColors.Surface)
            .border(1.dp, Color.White.copy(alpha = 0.07f), shape)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val active = phase.toInt() % 3 == index
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(PitchstoneColors.OnSurfaceVariant.copy(alpha = if (active) 1f else 0.35f))
            )
        }
    }
}

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text("‹", color = PitchstoneColors.TextSecondary, fontSize = 24.sp)
            }
            Spacer(Modifier.width(4.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = PitchstoneColors.OnBackground
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = PitchstoneColors.OnSurfaceVariant,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
        trailingContent?.invoke()
    }
}

@Composable
fun HeaderChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) PitchstoneColors.Accent else PitchstoneColors.SurfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) PitchstoneColors.Background else PitchstoneColors.OnSurfaceVariant
        )
    }
}

@Composable
fun ChatBubbleUser(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp))
                .background(PitchstoneColors.SurfaceVariant)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = PitchstoneColors.OnBackground
            )
        }
    }
}

@Composable
fun ChatBubbleAgent(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(shape)
                .background(PitchstoneColors.Surface)
                .border(1.dp, Color.White.copy(alpha = 0.07f), shape)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = PitchstoneColors.OnSurface
            )
        }
    }
}

@Composable
fun IconInCircle(
    icon: ImageVector,
    contentDescription: String?,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    background: Color = PitchstoneColors.SurfaceVariant,
    tint: Color = PitchstoneColors.Accent,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun LedgerRow(
    recipient: String,
    meta: String,
    amount: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = recipient,
                style = MaterialTheme.typography.bodyMedium,
                color = PitchstoneColors.OnBackground,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = PitchstoneColors.OnSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        MonoText(
            text = amount,
            color = PitchstoneColors.OnBackground,
            fontSize = 14.sp
        )
    }
}
