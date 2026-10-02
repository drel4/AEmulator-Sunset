/* AEmulator Sunset addition, 2026-10-02. GPL-3.0; see LICENSE. */
package app.aemu.core

internal object GuestWriterPolicy {
    fun matches(command: String, packageName: String, imagesPath: String): Boolean =
        (command.contains("$imagesPath/") && listOf("libqemu", "libbinderd", "libdhdrun", "libglserverd").any(command::contains)) ||
            command.startsWith("$packageName:binder") || command.startsWith("$packageName:gl")
}
