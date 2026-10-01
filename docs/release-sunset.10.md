# AEmulator Sunset 0.0.0.3-sunset.10

**Xperia Recents / Keyguard compatibility trial.** Extends the emulator's
Binder callback scheduling workaround to one-way Keyguard calls arriving on
threads awaiting a synchronous reply. Handles mixed callback/reply batches,
drains queued callbacks before idle reads block, and adds bounded diagnostics.

Stock ROM APKs/ODEX files and permission checks are unchanged. Caller
credentials are preserved; no blanket permission bypass is used.

The sunset.9 setup fix is now user-confirmed: setup completes and remains
completed after reboot on the Xperia ZR ROM. Includes previous vibration,
navigation, hidden-menu and Russian UI changes.

**Recents still needs the on-phone test.** Turn Phone GPU back on, restart the
VM, and check Recents, Small Apps and lock/unlock. If it remains invisible,
attach host + logcat logs so the new Keyguard diagnostics can be checked.

Camera support is not included. No wallpaper changes. Galaxy S5 unverified.
Host: Android 8.0+ (API 26), arm64-v8a. Version code: 12.

Verification: 16 JVM tests, six ARM guest-shim smoke tests and both audio ABI
smoke variants passed. Both APK signatures, versions and embedded shim/audio
hashes verified. Recents behavior is not yet verified on-phone.

SHA-256:
- Clone (`app.aemu.clone`): `e1f0401ba7509ccfbca3e0b5d65bcfe71f94cba6a71f15c7858e6d9157e70a69`
- Standard (`app.aemu`): `624c891d13f078a177cf3e4d3e3c370c5e438e527af27323369a60b61f4d627f`
