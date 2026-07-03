package space.pitchstone.android.presentation.capture

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import space.pitchstone.android.ui.components.AccentButton
import space.pitchstone.android.ui.components.HairlineDivider
import space.pitchstone.android.ui.components.OutlineButton
import space.pitchstone.android.ui.components.ScreenHeader
import space.pitchstone.android.ui.components.SectionLabel
import space.pitchstone.android.ui.components.ThinkingDots
import space.pitchstone.android.ui.theme.PitchstoneColors
import space.pitchstone.android.ui.theme.SpaceGrotesk

@Composable
fun CaptureScreen(
    viewModel: CaptureViewModel,
    onBack: () -> Unit,
    onExtractSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris -> if (uris.isNotEmpty()) viewModel.selectImages(uris) }

    LaunchedEffect(uiState) {
        if (uiState is CaptureUiState.Done) {
            val done = uiState as CaptureUiState.Done
            if (done.extractAndSave) {
                onExtractSuccess(done.replyText)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PitchstoneColors.Background)
    ) {
        when (val state = uiState) {
            is CaptureUiState.Input -> {
                InputContent(
                    state = state,
                    onBack = onBack,
                    onAddImages = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemoveImage = viewModel::removeImage,
                    onNoteChanged = viewModel::updateNote,
                    onModeChanged = viewModel::setMode,
                    onRunAgent = viewModel::runAgent
                )
            }

            is CaptureUiState.Processing -> {
                ProcessingContent(steps = state.steps)
            }

            is CaptureUiState.Done -> {
                DoneContent(state = state, onBack = onBack)
            }

            is CaptureUiState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetry = viewModel::retry,
                    onBack = onBack
                )
            }
        }
    }
}

@Composable
private fun InputContent(
    state: CaptureUiState.Input,
    onBack: () -> Unit,
    onAddImages: () -> Unit,
    onRemoveImage: (Uri) -> Unit,
    onNoteChanged: (String) -> Unit,
    onModeChanged: (Boolean) -> Unit,
    onRunAgent: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        ScreenHeader(title = "Capture", onBack = onBack)
        Spacer(Modifier.height(24.dp))

        SectionLabel("SCREENSHOTS")
        Spacer(Modifier.height(10.dp))

        if (state.images.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .border(1.dp, PitchstoneColors.Outline, RoundedCornerShape(12.dp))
                    .clickable(onClick = onAddImages),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ Add screenshots (max 10)",
                    color = PitchstoneColors.OnSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.images) { uri ->
                    ImageThumb(uri = uri, onRemove = { onRemoveImage(uri) })
                }
                item {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .border(1.dp, PitchstoneColors.Outline, RoundedCornerShape(10.dp))
                            .clickable(onClick = onAddImages),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+ Add", color = PitchstoneColors.OnSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        HairlineDivider()
        Spacer(Modifier.height(20.dp))

        SectionLabel("NOTE (OPTIONAL)")
        Spacer(Modifier.height(10.dp))
        BasicTextField(
            value = state.note,
            onValueChange = onNoteChanged,
            textStyle = TextStyle(
                fontFamily = SpaceGrotesk,
                fontSize = 15.sp,
                color = PitchstoneColors.OnBackground
            ),
            cursorBrush = SolidColor(PitchstoneColors.Accent),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PitchstoneColors.Outline, RoundedCornerShape(10.dp))
                        .padding(14.dp)
                ) {
                    if (state.note.isEmpty()) {
                        Text(
                            "e.g. This is from last night's dinner",
                            color = PitchstoneColors.OnSurfaceVariant,
                            fontSize = 15.sp
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
        )

        Spacer(Modifier.height(20.dp))

        SectionLabel("MODE")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModeChip(
                label = "Extract & save",
                selected = state.extractAndSave,
                onClick = { onModeChanged(true) },
                modifier = Modifier.weight(1f)
            )
            ModeChip(
                label = "Just ask",
                selected = !state.extractAndSave,
                onClick = { onModeChanged(false) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(28.dp))
        AccentButton(
            text = "Run agent →",
            onClick = onRunAgent,
            enabled = state.images.isNotEmpty() || state.note.isNotBlank()
        )
        Spacer(Modifier.height(16.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun ImageThumb(uri: Uri, onRemove: () -> Unit) {
    Box(modifier = Modifier.size(88.dp).clip(RoundedCornerShape(10.dp))) {
        AsyncImage(
            model = uri,
            contentDescription = "Screenshot",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) PitchstoneColors.Accent else PitchstoneColors.SurfaceVariant)
            .border(
                1.dp,
                if (selected) PitchstoneColors.Accent else PitchstoneColors.Outline,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) PitchstoneColors.Background else PitchstoneColors.OnSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ProcessingContent(steps: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        SectionLabel("AGENT WORKING")
        Spacer(Modifier.height(24.dp))
        steps.forEach { step ->
            Text(
                text = "→ $step",
                color = PitchstoneColors.OnSurfaceVariant,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(12.dp))
        ThinkingDots()
    }
}

@Composable
private fun DoneContent(state: CaptureUiState.Done, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        SectionLabel("AGENT REPLY")
        Spacer(Modifier.height(16.dp))
        Text(
            text = state.replyText,
            color = PitchstoneColors.OnBackground,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(24.dp))
        OutlineButton(text = "Done", onClick = onBack)
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = PitchstoneColors.Danger,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(20.dp))
        AccentButton(text = "Retry", onClick = onRetry)
        Spacer(Modifier.height(10.dp))
        OutlineButton(text = "Cancel", onClick = onBack)
    }
}
