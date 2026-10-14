package id.lapor.lh.feature.report.ui

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import id.lapor.lh.R
import id.lapor.lh.feature.report.domain.Attachment
import java.io.File
import kotlin.math.max
import kotlin.math.min

private const val MAX_ZOOM = 4f
private const val DOUBLE_TAP_ZOOM = 2.5f
private val PreviewBackground = Color.Black
private val PreviewContent = Color.White

@Composable
internal fun ImagePreview(
    attachment: Attachment,
    onClose: () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        var size by remember { mutableStateOf(IntSize.Zero) }
        var imageSize by remember { mutableStateOf(Size.Unspecified) }
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        fun clamp(
            value: Offset,
            zoom: Float,
        ): Offset {
            val fit =
                if (imageSize.isSpecified && imageSize.width > 0f && imageSize.height > 0f) {
                    min(size.width / imageSize.width, size.height / imageSize.height)
                } else {
                    0f
                }
            val shown = if (fit > 0f) Size(imageSize.width * fit, imageSize.height * fit) else size.toSize()
            val maxX = max(0f, (shown.width * zoom - size.width) / 2f)
            val maxY = max(0f, (shown.height * zoom - size.height) / 2f)
            return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
        }

        val transformState =
            rememberTransformableState { zoomChange, panChange, _ ->
                val zoom = (scale * zoomChange).coerceIn(1f, MAX_ZOOM)
                offset = clamp(offset + panChange, zoom)
                scale = zoom
            }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(PreviewBackground)
                    .onSizeChanged { size = it },
        ) {
            AsyncImage(
                model = File(attachment.localPath),
                contentDescription = attachment.fileName,
                contentScale = ContentScale.Fit,
                onSuccess = { imageSize = it.painter.intrinsicSize },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { tap ->
                                    if (scale > 1f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        val center = Offset(size.width / 2f, size.height / 2f)
                                        offset = clamp((center - tap) * (DOUBLE_TAP_ZOOM - 1f), DOUBLE_TAP_ZOOM)
                                        scale = DOUBLE_TAP_ZOOM
                                    }
                                },
                            )
                        }.transformable(state = transformState, canPan = { scale > 1f })
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
            )
            PreviewTopBar(attachment = attachment, onClose = onClose)
        }
    }
}

@Composable
private fun PreviewTopBar(
    attachment: Attachment,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(PreviewBackground.copy(alpha = 0.7f), Color.Transparent)))
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
    ) {
        IconButton(onClick = onClose) {
            Icon(
                painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.preview_close),
                tint = PreviewContent,
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attachment.fileName,
                style = MaterialTheme.typography.titleMedium,
                color = PreviewContent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = Formatter.formatShortFileSize(context, attachment.sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = PreviewContent.copy(alpha = 0.8f),
            )
        }
    }
}
