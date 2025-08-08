package raf.console.pdfreader.helper

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch

@ExperimentalFoundationApi
@Composable
fun ZoomableImage(
    painter: Painter,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    imageAlign: Alignment = Alignment.Center,
    shape: Shape = RectangleShape,
    minScale: Float = 1f,
    maxScale: Float = 3f,
    contentScale: ContentScale = ContentScale.Fit,
    isRotation: Boolean = false,
    isZoomable: Boolean = true,
    scrollState: ScrollableState? = null
) {
    val scale = remember { mutableStateOf(1f) }
    val rotationState = remember { mutableStateOf(0f) }
    val offsetX = remember { mutableStateOf(0f) }
    val offsetY = remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .clip(shape)
            .background(backgroundColor)
            .pointerInput(Unit) {
                if (isZoomable) {
                    forEachGesture {
                        awaitPointerEventScope {
                            awaitFirstDown()
                            do {
                                val event = awaitPointerEvent()

                                val zoomChange = event.calculateZoom()
                                val newScale = (scale.value * zoomChange)
                                    .coerceIn(minScale, maxScale)

                                if (newScale > minScale) {
                                    // Блокируем прокрутку списка
                                    scrollState?.let {
                                        coroutineScope.launch { it.setScrolling(false) }
                                    }
                                }

                                val pan = event.calculatePan()
                                val rotationChange = event.calculateRotation()

                                scale.value = newScale
                                if (newScale > minScale) {
                                    offsetX.value += pan.x
                                    offsetY.value += pan.y
                                } else {
                                    offsetX.value = 0f
                                    offsetY.value = 0f
                                }

                                if (isRotation) {
                                    rotationState.value += rotationChange
                                }

                                if (newScale <= minScale) {
                                    // Возвращаем прокрутку
                                    scrollState?.let {
                                        coroutineScope.launch { it.setScrolling(true) }
                                    }
                                }

                            } while (event.changes.any { it.pressed })
                        }
                    }
                }
            }
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier
                .align(imageAlign)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    if (isRotation) rotationZ = rotationState.value
                    translationX = offsetX.value
                    translationY = offsetY.value
                }
        )
    }
}

suspend fun ScrollableState.setScrolling(enabled: Boolean) {
    scroll(scrollPriority = MutatePriority.PreventUserInput) {
        if (!enabled) {
            awaitCancellation()
        }
    }
}
