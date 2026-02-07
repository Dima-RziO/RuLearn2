package ru.dimarzio.rulearn2.compose

import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class SwipeToRevealBoxValue {
    Settled,
    EndToStart,
    StartToEnd
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeToRevealBox(
    hiddenContentEnd: @Composable () -> Unit = {},
    hiddenContentEndBackground: Color = MaterialTheme.colorScheme.errorContainer,
    onHiddenContentEndClick: () -> Unit = {},
    hiddenContentStart: @Composable () -> Unit = {},
    hiddenContentStartBackground: Color = MaterialTheme.colorScheme.primaryContainer,
    onHiddenContentStartClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    var endAnchor by remember { mutableFloatStateOf(0f) }
    var startAnchor by remember { mutableFloatStateOf(0f) }

    val state = remember {
        AnchoredDraggableState(
            initialValue = SwipeToRevealBoxValue.Settled,
            positionalThreshold = { distance -> distance * 0.2f },
            velocityThreshold = { 0.2f },
            snapAnimationSpec = tween(),
            decayAnimationSpec = decayAnimationSpec
        )
    }

    val coroutineScope = rememberCoroutineScope()

    SideEffect {
        state.updateAnchors(
            DraggableAnchors {
                SwipeToRevealBoxValue.Settled at 0f
                SwipeToRevealBoxValue.EndToStart at endAnchor
                SwipeToRevealBoxValue.StartToEnd at startAnchor
            }
        )
    }

    Box(modifier = Modifier.height(IntrinsicSize.Max)) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(hiddenContentEndBackground)
                .clickable {
                    coroutineScope.launch {
                        state.animateTo(SwipeToRevealBoxValue.Settled)
                    }

                    onHiddenContentEndClick()
                }
                .onSizeChanged { size ->
                    endAnchor = -size.width.toFloat()
                }
                .align(Alignment.CenterEnd),
            contentAlignment = Alignment.Center
        ) {
            hiddenContentEnd()
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(hiddenContentStartBackground)
                .clickable {
                    coroutineScope.launch {
                        state.animateTo(SwipeToRevealBoxValue.Settled)

                        onHiddenContentStartClick()
                    }
                }
                .onSizeChanged { size ->
                    startAnchor = size.width.toFloat()
                }
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center
        ) {
            hiddenContentStart()
        }

        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .offset {
                    IntOffset(
                        state
                            .requireOffset()
                            .roundToInt(), 0
                    )
                }
                .anchoredDraggable(state = state, orientation = Orientation.Horizontal)
        ) {
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                content()
            }
        }
    }
}