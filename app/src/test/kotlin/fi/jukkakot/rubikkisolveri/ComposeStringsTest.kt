package fi.jukkakot.rubikkisolveri

import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The shared texts must work with Compose resources, which only know positional arguments. */
class ComposeStringsTest {
    private val dir = "../shared/src/commonMain/composeResources"

    /** name → every text of that key (one for a string, one per quantity for plurals). */
    private fun texts(folder: String): Map<String, List<String>> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("$dir/$folder/strings.xml"))
        val strings = doc.getElementsByTagName("string").let { n -> (0 until n.length).map { n.item(it) as org.w3c.dom.Element } }
            .associate { it.getAttribute("name") to listOf(it.textContent) }
        val plurals = doc.getElementsByTagName("plurals").let { n -> (0 until n.length).map { n.item(it) as org.w3c.dom.Element } }
            .associate { p ->
                val items = p.getElementsByTagName("item")
                p.getAttribute("name") to (0 until items.length).map { items.item(it).textContent }
            }
        return strings + plurals
    }

    private fun placeholders(text: String) = Regex("""%\d+\$[ds]""").findAll(text).map { it.value }.toSet()

    @Test
    fun bothLanguagesHaveTheSameKeys() {
        assertEquals(texts("values").keys, texts("values-en").keys)
    }

    @Test
    fun noAndroidEscapesOrLoneFormatSigns() {
        for (folder in listOf("values", "values-en")) {
            for ((key, values) in texts(folder)) {
                for (text in values) {
                    assertTrue("\\'" !in text, "$folder/$key keeps an Android escape: $text")
                    val stray = Regex("""%(?!%|\d+\$[ds])""").find(text.replace("%%", ""))
                    assertTrue(stray == null, "$folder/$key has a non-positional %: $text")
                }
            }
        }
    }

    @Test
    fun placeholdersMatchBetweenLanguages() {
        val fi = texts("values")
        val en = texts("values-en")
        for ((key, values) in fi) {
            assertEquals(
                values.flatMap(::placeholders).toSet(),
                en.getValue(key).flatMap(::placeholders).toSet(),
                "placeholders of $key",
            )
        }
    }
}
