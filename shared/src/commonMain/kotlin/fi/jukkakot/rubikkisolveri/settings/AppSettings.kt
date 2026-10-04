package fi.jukkakot.rubikkisolveri.settings

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** The UI languages; Finnish is the default (product.md). */
enum class AppLanguage(val tag: String) {
    FINNISH("fi"),
    ENGLISH("en"),
    ;

    companion object {
        val DEFAULT = FINNISH
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { tag?.startsWith(it.tag) == true } ?: DEFAULT
    }
}
