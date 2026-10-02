package app.aemu.update

/** Sunset modification, 2026-10-02: never cross-install distribution variants. */
object UpdateAssetPolicy {
    fun matches(name: String, applicationId: String): Boolean = when (applicationId) {
        "app.aemu", "app.aemu.clone" -> name.endsWith("-$applicationId.apk", ignoreCase = true)
        else -> false
    }
}
