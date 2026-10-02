package fi.jukkakot.rubikkisolveri

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import fi.jukkakot.rubikkisolveri.log.AppLog
import fi.jukkakot.rubikkisolveri.ui.log.shareLogIntent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class AppTest {
    private val app: RubikkiApp = ApplicationProvider.getApplicationContext()

    @Test
    fun startIsLoggedWithTheVersion() {
        AppLog.logger.flush()
        val start = AppLog.logger.file.readLines().last { " app.start " in it }
        assertTrue("ver=${BuildConfig.VERSION_NAME}" in start, start)
    }

    @Test
    fun shareTheLog() {
        AppLog.logger.flush()
        val chooser = shareLogIntent(app, AppLog.logger.file.file)
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals(Intent.ACTION_SEND, send.action)
        @Suppress("DEPRECATION")
        val uri = send.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
        assertEquals("content", uri.scheme)
        assertTrue(uri.toString().endsWith("app.log"))
    }
}
