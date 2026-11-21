package dev.jt.orientationwallpaper.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jt.orientationwallpaper.BuildConfig
import dev.jt.orientationwallpaper.ImageFormats
import dev.jt.orientationwallpaper.R
import dev.jt.orientationwallpaper.wallpaper.WallpaperChooser

@Composable
fun WallpaperScreen(viewModel: WallpaperViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var chooserFailed by remember { mutableStateOf(false) }
    val message = state.message
    val messageText = message?.let { stringResource(it.stringRes()) }
    val chooserText = stringResource(R.string.error_chooser)

    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            viewModel.messageShown()
        }
    }
    LaunchedEffect(chooserFailed) {
        if (chooserFailed) {
            snackbar.showSnackbar(chooserText)
            chooserFailed = false
        }
    }

    val pickPortrait = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.import(ImportTarget.PORTRAIT, uri)
    }
    val pickLandscape = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.import(ImportTarget.LANDSCAPE, uri)
    }
    val pickBoth = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.import(ImportTarget.BOTH, uri)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 880.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.headline),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = stringResource(R.string.subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.busy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = stringResource(R.string.saving),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    if (maxWidth >= 720.dp) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            SlotCard(
                                title = stringResource(R.string.portrait),
                                chooseLabel = stringResource(R.string.choose_portrait),
                                previewDescription = stringResource(R.string.preview_portrait),
                                slot = state.portrait,
                                enabled = !state.busy,
                                aspect = 3f / 4f,
                                onChoose = { pickPortrait.launch(ImageFormats.pickerMimeTypes) },
                                modifier = Modifier.weight(1f),
                            )
                            SlotCard(
                                title = stringResource(R.string.landscape),
                                chooseLabel = stringResource(R.string.choose_landscape),
                                previewDescription = stringResource(R.string.preview_landscape),
                                slot = state.landscape,
                                enabled = !state.busy,
                                aspect = 4f / 3f,
                                onChoose = { pickLandscape.launch(ImageFormats.pickerMimeTypes) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            SlotCard(
                                title = stringResource(R.string.portrait),
                                chooseLabel = stringResource(R.string.choose_portrait),
                                previewDescription = stringResource(R.string.preview_portrait),
                                slot = state.portrait,
                                enabled = !state.busy,
                                aspect = 3f / 4f,
                                onChoose = { pickPortrait.launch(ImageFormats.pickerMimeTypes) },
                            )
                            SlotCard(
                                title = stringResource(R.string.landscape),
                                chooseLabel = stringResource(R.string.choose_landscape),
                                previewDescription = stringResource(R.string.preview_landscape),
                                slot = state.landscape,
                                enabled = !state.busy,
                                aspect = 4f / 3f,
                                onChoose = { pickLandscape.launch(ImageFormats.pickerMimeTypes) },
                            )
                        }
                    }
                }
                TextButton(
                    onClick = { pickBoth.launch(ImageFormats.pickerMimeTypes) },
                    enabled = !state.busy,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(stringResource(R.string.same_image))
                }
                Button(
                    onClick = {
                        if (!WallpaperChooser.launch(context)) chooserFailed = true
                    },
                    enabled = state.ready && !state.busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(stringResource(R.string.set_wallpaper))
                }
                if (!state.ready) {
                    Text(
                        text = stringResource(R.string.set_wallpaper_needs_both),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(
                    text = stringResource(R.string.footnote),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.version_footer, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SlotCard(
    title: String,
    chooseLabel: String,
    previewDescription: String,
    slot: SlotUi,
    enabled: Boolean,
    aspect: Float,
    onChoose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = slot.formatLabel ?: stringResource(R.string.no_image),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PreviewImage(
                slot = slot,
                description = previewDescription,
                aspect = aspect,
            )
            OutlinedButton(
                onClick = onChoose,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(chooseLabel)
            }
        }
    }
}

@Composable
private fun PreviewImage(slot: SlotUi, description: String, aspect: Float) {
    val preview = slot.preview
    val image = remember(preview) { preview?.asImageBitmap() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = stringResource(R.string.no_image),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun UserMessage.stringRes(): Int = when (this) {
    UserMessage.UNSUPPORTED -> R.string.error_unsupported
    UserMessage.COULD_NOT_OPEN -> R.string.error_open
    UserMessage.DECODE_FAILED -> R.string.error_decode
}
