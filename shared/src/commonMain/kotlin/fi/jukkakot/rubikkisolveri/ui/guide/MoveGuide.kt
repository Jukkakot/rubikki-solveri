package fi.jukkakot.rubikkisolveri.ui.guide

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.ui.common.RoundIconButton
import fi.jukkakot.rubikkisolveri.ui.common.moveDescription
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import fi.jukkakot.rubikkisolveri.ui.cube3d.Cube3D
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeScene
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeViewState
import fi.jukkakot.rubikkisolveri.ui.cube3d.Quat
import fi.jukkakot.rubikkisolveri.ui.cube3d.StickerColors
import fi.jukkakot.rubikkisolveri.ui.cube3d.V3

/**
 * The 3D cube presenting the stepper's current move: the turning layer highlighted and the
 * direction arrow when the cube is still. The view stays in the holding position for every move;
 * after the user drags, a reset button turns it back. [mirror] adds the mirror behind the cube.
 */
@Composable
fun GuideCube(
    state: StepperState,
    modifier: Modifier = Modifier,
    description: String? = null,
    mirror: Boolean = false,
    viewState: CubeViewState = remember { CubeViewState(CubeScene.DEFAULT_VIEW) },
) {
    val animator = state.animator
    val presented = state.current
    val turning = animator.move
    val colors = animator.cube.toList().map(StickerColors::of)
    val highlight = turning ?: presented
    val arrow = if (turning == null && animator.pending == 0 && animator.cube == state.cubeAt(state.index)) presented else null
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val tilt = remember { Animatable(0f) }
    val hop = remember { Animatable(0f) }
    LaunchedEffect(state.nods) {
        // The cube already shows the next step; a small nod says it moved on.
        if (state.nods == 0 || animator.isInstant) return@LaunchedEffect
        tilt.animateTo(-NOD_DEGREES, tween(120))
        tilt.animateTo(0f, tween(200))
    }
    LaunchedEffect(state.celebrations) {
        if (state.celebrations == 0) return@LaunchedEffect
        if (!animator.isInstant) {
            // Solved: the cube hops and spins once around its vertical axis while confetti bursts.
            val base = viewState.rotation
            val spin = Animatable(0f)
            coroutineScope {
                launch {
                    hop.animateTo(1f, tween(CELEBRATE_MS / 2, easing = FastOutSlowInEasing))
                    hop.animateTo(0f, tween(CELEBRATE_MS / 2, easing = LinearOutSlowInEasing))
                }
                spin.animateTo(1f, tween(CELEBRATE_MS, easing = FastOutSlowInEasing)) {
                    viewState.rotation = (base * Quat.axisAngle(V3(0f, 1f, 0f), value * 2 * PI.toFloat())).normalized()
                }
            }
            viewState.rotation = base
        }
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }
    // As large as its bounds allow: by height in a FitColumn slot, by width elsewhere.
    Box(
        modifier.aspectRatio(1.1f, matchHeightConstraintsFirst = true).graphicsLayer {
            rotationZ = tilt.value
            translationY = -hop.value * size.height * HOP_HEIGHT
        },
    ) {
        Cube3D(
            colors = colors,
            move = turning,
            progress = animator.progress,
            viewState = viewState,
            highlight = highlight,
            arrow = arrow,
            description = description,
            mirror = mirror,
            modifier = Modifier.fillMaxSize().testTag(GUIDE_CUBE_TAG),
        )
        if (!animator.isInstant) Confetti(state.celebrations, Modifier.matchParentSize())
        if (!viewState.isAt(CubeScene.DEFAULT_VIEW)) {
            RoundIconButton(
                onClick = { scope.launch { viewState.animateTo(CubeScene.DEFAULT_VIEW) } },
                size = 40.dp,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
            ) {
                Icon(painterResource(Res.drawable.ic_reset_view), contentDescription = stringResource(Res.string.reset_view))
            }
        }
    }
}

/** Test tag of the guide's main 3D cube. */
const val GUIDE_CUBE_TAG = "guide_cube"

private const val NOD_DEGREES = 4f
private const val CELEBRATE_MS = 1000
private const val HOP_HEIGHT = 0.12f

/** The current move in words, big, and optionally its notation, small. */
@Composable
fun MoveWordsText(state: StepperState, showNotation: Boolean) {
    val move = state.current ?: return
    Column {
        Text(
            moveDescription(move, state.cubeAt(state.index + 1)),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.heightIn(min = 72.dp),
        )
        if (showNotation) {
            Text(move.toString(), style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
        }
    }
}
