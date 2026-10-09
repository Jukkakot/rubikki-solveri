package fi.jukkakot.rubikkisolveri.store

import fi.jukkakot.rubikkisolveri.progress.Penalty
import fi.jukkakot.rubikkisolveri.settings.AppLanguage
import fi.jukkakot.rubikkisolveri.settings.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The browser version's storage, run against a plain map instead of localStorage. */
class BrowserStoresTest {
    private val map = MapKeyValueStore()

    @Test
    fun solvesSurviveARestart() = runTest {
        val repo = StoredProgressRepository(map)
        repo.addTimed(12_340, "R U F", finishedAt = 1)
        val second = repo.addTimed(10_000, "L D B", finishedAt = 2)
        repo.setPenalty(repo.timedSolves.first().last(), Penalty.PLUS_TWO)
        repo.addGuided("LEARN", 150, 600_000, finishedAt = 3)
        repo.addPractice(3, 60_000, finishedAt = 4)
        repo.addPractice(3, 50_000, finishedAt = 5)

        val again = StoredProgressRepository(map)
        val timed = again.timedSolves.first()
        assertEquals(listOf(10_000L, 12_340L), timed.map { it.millis }, "newest first")
        assertEquals(Penalty.PLUS_TWO, timed.last().penalty)
        assertEquals(150, again.guidedSolves.first().single().moves)
        assertEquals(mapOf(3 to 2), again.practiceCounts.first())
        again.deleteTimed(second)
        assertEquals(1, StoredProgressRepository(map).timedSolves.first().size)
        assertTrue(again.addTimed(1, "x") > second, "ids keep growing after a restart")
    }

    @Test
    fun brokenDataStartsEmpty() = runTest {
        map.set(StoreKeys.PROGRESS, "{not json")
        assertEquals(emptyList(), StoredProgressRepository(map).timedSolves.first())
    }

    @Test
    fun settingsSurviveARestart() {
        val settings = StoredSettings(map)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode.value)
        assertEquals(AppLanguage.FINNISH, settings.language, "Finnish whatever the browser")
        settings.setThemeMode(ThemeMode.DARK)
        settings.setShowNotation(true)
        settings.setLanguage(AppLanguage.ENGLISH)
        val again = StoredSettings(map)
        assertEquals(ThemeMode.DARK, again.themeMode.value)
        assertEquals(true, again.showNotation.value)
        assertEquals(AppLanguage.ENGLISH, again.language)
        assertEquals("en", map.get(StoreKeys.LANGUAGE), "index.html reads this key before the app starts")
    }

    @Test
    fun settingsStoredWithTheEarlierScannerChoiceStillLoad() {
        // scan-rules-only: the scanner choice is gone; a stored one is ignored.
        map.set(StoreKeys.SETTINGS, """{"theme":"DARK","notation":true,"scanEngine":"look"}""")
        val settings = StoredSettings(map)
        assertEquals(ThemeMode.DARK, settings.themeMode.value)
        assertEquals(true, settings.showNotation.value)
    }

    @Test
    fun logStaysSmall() {
        val log = StoredLog(map, maxChars = 1_000)
        repeat(200) { log.append("line ${it.toString().padStart(3, '0')} " + "x".repeat(20)) }
        val lines = log.readLines()
        assertTrue(map.get(StoreKeys.LOG)!!.length <= 1_000)
        assertEquals("line 199 " + "x".repeat(20), lines.last())
        assertTrue(lines.none { it.startsWith("line 000") })
        log.clear()
        assertEquals(emptyList(), log.readLines())
    }

    @Test
    fun onlyTheNewestPicturesAreKept() {
        var now = 0L
        val pictures = StoredScanPictures(map, { argb, size -> "png${argb.size}x$size" }, { now++ })
        val names = (1..15).map { pictures.newName("F").also { name -> pictures.write(name, IntArray(4), 2) } }
        val kept = StoredScanPictures(map, { _, _ -> "" }, { 0 }).list()
        assertEquals(names.takeLast(12), kept.map { it.name })
        assertEquals("png4x2", kept.first().pngBase64)
    }

    @Test
    fun blockedStorageFallsBackToMemory() {
        val blocked = object : KeyValueStore {
            override fun get(key: String): String? = throw IllegalStateException("SecurityError")
            override fun set(key: String, value: String) = throw IllegalStateException("SecurityError")
            override fun remove(key: String) = throw IllegalStateException("SecurityError")
        }
        val warnings = mutableListOf<Throwable>()
        val store = FallbackKeyValueStore(blocked) { warnings += it }
        assertEquals(null, store.get("a"))
        store.set("a", "1")
        assertEquals("1", store.get("a"))
        assertEquals(1, warnings.size, "warned once")
    }
}
