package fi.jukkakot.rubikkisolveri.ui.common

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Below this the big element is too small to use, and the screen scrolls instead. */
val FIT_FLOOR: Dp = 200.dp

/** Receiver of [FitColumn]'s content: marks the screen's big element. */
interface FitScope {
    /** This child is the big element: it gets the height the others leave, and is centred in it. */
    fun Modifier.fitSlot(): Modifier = this.then(FitSlotMarker)
}

private object FitSlotMarker : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = FitSlotMarker
}

private object DefaultFitScope : FitScope

/**
 * A screen that fits without scrolling: a column whose children keep their natural height, except
 * the one marked [FitScope.fitSlot] (the screen's big element), which gets the height that is left.
 * The big element should draw as large as fits in those bounds
 * (`aspectRatio(…, matchHeightConstraintsFirst = true)`). When less than [FIT_FLOOR] is left, it
 * gets the floor and the column scrolls. [keepSize] never lets the slot grow back (the solution's
 * cube keeps one size from move to move).
 */
@Composable
fun FitColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    spacing: Dp = 8.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    keepSize: Boolean = false,
    content: @Composable FitScope.() -> Unit,
) {
    BoxWithConstraints(modifier) {
        val direction = LocalLayoutDirection.current
        val viewport = maxHeight - contentPadding.calculateTopPadding() - contentPadding.calculateBottomPadding()
        // The smallest slot so far for this screen size, for keepSize.
        val smallest = remember(maxWidth, maxHeight) { IntArray(1) { Int.MAX_VALUE } }
        Layout(
            content = { DefaultFitScope.content() },
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(
                start = contentPadding.calculateStartPadding(direction),
                end = contentPadding.calculateEndPadding(direction),
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
            ),
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val gap = spacing.roundToPx()
            val loose = Constraints(maxWidth = width)
            val placeables = arrayOfNulls<androidx.compose.ui.layout.Placeable>(measurables.size)
            var used = 0
            measurables.forEachIndexed { i, m ->
                if (m.parentData != FitSlotMarker) placeables[i] = m.measure(loose).also { used += it.height }
            }
            val slotIndex = measurables.indexOfFirst { it.parentData == FitSlotMarker }
            val gaps = gap * (measurables.size - 1).coerceAtLeast(0)
            var slot = 0
            if (slotIndex >= 0) {
                slot = (viewport.roundToPx() - used - gaps).coerceAtLeast(FIT_FLOOR.roundToPx())
                if (keepSize) {
                    smallest[0] = minOf(smallest[0], slot)
                    slot = smallest[0]
                }
                placeables[slotIndex] = measurables[slotIndex].measure(Constraints(maxWidth = width, maxHeight = slot))
            }
            val height = used + slot + gaps
            layout(width, height) {
                var y = 0
                placeables.forEachIndexed { i, p ->
                    p!!
                    val x = if (i == slotIndex) (width - p.width) / 2 else horizontalAlignment.align(p.width, width, layoutDirection)
                    if (i == slotIndex) {
                        p.place(x, y + (slot - p.height) / 2)
                        y += slot + gap
                    } else {
                        p.place(x, y)
                        y += p.height + gap
                    }
                }
            }
        }
    }
}
