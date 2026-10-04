package fi.jukkakot.rubikkisolveri.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import fi.jukkakot.rubikkisolveri.log.Evt
import kotlinx.browser.document
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val services = WebServices()
    services.logger.info(
        Evt.APP_START, null,
        "ver" to BuildInfo.VERSION_NAME, "platform" to "web", "agent" to window.navigator.userAgent,
        "crashedLastTime" to services.crashedLastTime,
    )
    if (queryFlag("selftest")) {
        runSelfTest(services)
        return
    }
    ComposeViewport(document.getElementById("app")!!) {
        WebApp(services)
    }
}
