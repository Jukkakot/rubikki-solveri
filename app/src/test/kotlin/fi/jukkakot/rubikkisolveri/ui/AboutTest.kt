package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import fi.jukkakot.rubikkisolveri.ui.settings.AboutScreen
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AboutTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun downloadOpensTheFixedAddress() {
        val opened = mutableListOf<String>()
        val handler = object : UriHandler {
            override fun openUri(uri: String) {
                opened += uri
            }
        }
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                CompositionLocalProvider(LocalUriHandler provides handler) {
                    AboutScreen("1.0.1-abc", onBack = {})
                }
            }
        }
        compose.onNodeWithText("Lataa uusin versio").performClick()
        assertEquals(
            listOf("https://github.com/Jukkakot/rubikki-solveri/releases/latest/download/rubikki-solveri.apk"),
            opened,
        )
    }
}
