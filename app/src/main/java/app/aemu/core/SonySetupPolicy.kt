package app.aemu.core

/** Preserve Sony's two-stage setup flow; repair only the old emulator's package disable. */
object SonySetupPolicy {
    const val PACKAGE = "com.sonyericsson.setupwizard"
    fun completed(preferences: String): Boolean = Regex("<boolean\\b[^>]*>").findAll(preferences).any {
        attribute(it.value, "name") == "setup_wizard_has_run" && attribute(it.value, "value") == "true"
    }
    fun restorePackage(xml: String): String = Regex("<pkg\\b[^>]*>").replace(xml) { match ->
        if (attribute(match.value, "name") == PACKAGE && attribute(match.value, "enabled") == "2") {
            Regex("\\benabled\\s*=\\s*([\"'])2\\1").replace(match.value, "enabled=\"0\"")
        } else match.value
    }
    private fun attribute(tag: String, name: String): String? =
        Regex("\\b${Regex.escape(name)}\\s*=\\s*([\"'])(.*?)\\1", RegexOption.DOT_MATCHES_ALL)
            .find(tag)?.groupValues?.get(2)
}
