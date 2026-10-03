/* AEmulator Sunset addition, 2026-10-03. GPL-3.0. */
package app.aemu.core

internal enum class ResolutionPreset(val width: Int, val height: Int, val density: Int) {
    WVGA(480, 800, 240), QHD(540, 960, 240), HD(720, 1280, 320), HOST(0, 0, 0);

    fun selected(settings: VmSettings) = if (this == HOST) settings.hostResolution
        else !settings.hostResolution && settings.width == width && settings.height == height

    fun apply(settings: VmSettings) = if (this == HOST) settings.copy(hostResolution = true)
        else settings.copy(width = width, height = height, density = density, hostResolution = false)
}
