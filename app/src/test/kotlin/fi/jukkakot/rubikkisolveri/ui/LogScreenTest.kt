package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import fi.jukkakot.rubikkisolveri.ui.log.LogScreen
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LogScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun errorsSayTheirLevel() {
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                LogScreen(
                    lines = listOf(
                        "2026-10-03T08:09:50Z INFO scan.done valid=true",
                        "2026-10-03T08:09:51Z ERROR scan.error msg=boom",
                    ),
                    onShare = {}, onClear = {}, onBack = {},
                )
            }
        }
        // The level word for the error; info lines without it.
        compose.onNodeWithText("ERROR scan.error msg=boom").assertIsDisplayed()
        compose.onNodeWithText("scan.done valid=true").assertIsDisplayed()
    }
}
