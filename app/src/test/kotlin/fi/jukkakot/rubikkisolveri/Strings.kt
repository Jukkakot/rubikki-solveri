package fi.jukkakot.rubikkisolveri

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The shared module's texts for tests that build a sentence outside Compose: reads the
 * Compose-resource XML directly and fills the positional `%1$s` / `%1$d` arguments.
 */
object Strings {
    private const val DIR = "../shared/src/commonMain/composeResources"

    private val tables = mutableMapOf<String, Map<String, String>>()

    /** Every `<string>` of one language folder (`values` = Finnish, `values-en` = English). */
    fun table(folder: String): Map<String, String> = tables.getOrPut(folder) {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("$DIR/$folder/strings.xml"))
        val nodes = doc.getElementsByTagName("string")
        (0 until nodes.length).associate { i ->
            val e = nodes.item(i) as org.w3c.dom.Element
            e.getAttribute("name") to e.textContent
        }
    }

    fun get(language: String, key: String, vararg args: Any): String {
        val text = table(if (language == "fi") "values" else "values-$language")[key]
            ?: error("No string '$key' for $language")
        return Regex("""%(\d+)\$[ds]""").replace(text) { args[it.groupValues[1].toInt() - 1].toString() }
    }

    fun fi(key: String, vararg args: Any) = get("fi", key, *args)
    fun en(key: String, vararg args: Any) = get("en", key, *args)
}
