package fi.jukkakot.rubikkisolveri

import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.assertEquals

class StringsTest {
    private fun keys(path: String, onlyTranslatable: Boolean): Set<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(path))
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).map { nodes.item(it) as org.w3c.dom.Element }
            .filter { !onlyTranslatable || it.getAttribute("translatable") != "false" }
            .map { it.getAttribute("name") }
            .toSet()
    }

    @Test
    fun shownLicenceMatchesTheVendoredOne() {
        assertEquals(
            File("../cube/src/commonMain/kotlin/fi/jukkakot/rubikkisolveri/cube/solve/min2phase/LICENSE").readText(),
            File("src/main/res/raw/min2phase_license.txt").readText(),
        )
    }

    @Test
    fun everyFinnishTextHasAnEnglishOne() {
        val finnish = keys("src/main/res/values/strings.xml", onlyTranslatable = true)
        val english = keys("src/main/res/values-en/strings.xml", onlyTranslatable = false)
        assertEquals(finnish, english)
    }
}
