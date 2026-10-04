package fi.jukkakot.rubikkisolveri.ui.lessons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.beginner.Stage
import fi.jukkakot.rubikkisolveri.cube.beginner.StageGoals
import fi.jukkakot.rubikkisolveri.ui.common.stageName
import fi.jukkakot.rubikkisolveri.ui.cube3d.Cube3D
import fi.jukkakot.rubikkisolveri.ui.cube3d.StickerColors

/**
 * A still 3D cube for lessons: [colors] with null drawn grey (stickers not in place yet), [marked]
 * outlined. Only a [large] picture can be turned by dragging; small ones leave drags to the page.
 */
@Composable
fun GoalCube(
    colors: List<CubeColor?>,
    modifier: Modifier = Modifier,
    marked: Set<Int> = emptySet(),
    large: Boolean = false,
    description: String? = null,
) {
    // A mid grey in both themes: a light grey would read as white stickers.
    val grey = MaterialTheme.colorScheme.outline
    Cube3D(
        colors = colors.map { if (it == null) grey else StickerColors.of(it) },
        marked = marked,
        draggable = large,
        description = description,
        modifier = modifier.aspectRatio(1f),
    )
}

/** The goal picture of [stage]: done, held as the stage holds the cube, its new pieces outlined. */
@Composable
fun StageGoalCube(stage: Stage, modifier: Modifier = Modifier, large: Boolean = false) {
    val goal = StageGoals.of(stage)
    GoalCube(
        goal.colors, modifier, marked = goal.added, large = large,
        description = stringResource(Res.string.goal_description, stringResource(stageName(stage))),
    )
}

/** The goal picture large with its one-line note (twist), for goal pages, cards and dialogs. */
@Composable
fun StageGoalPicture(stage: Stage, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        StageGoalCube(stage, Modifier, large = true)
        if (StageGoals.of(stage).twistMayBeWrong) {
            Text(
                stringResource(Res.string.goal_twist_note), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
            )
        }
    }
}
