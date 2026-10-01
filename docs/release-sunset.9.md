# AEmulator Sunset 0.0.0.3-sunset.9

Includes the working audio compatibility and vibration changes from sunset.8.

- **Sony setup-flow compatibility trial:** restore the full wizard disabled by
  the old emulator policy; let stock firmware manage setup-completion flags.
  Existing unfinished setups migrate once with retained data backups.
- The synthetic SIM now supports its read-only ICCID identity file, allowing
  firmware to retrieve an emulated SIM serial number. No real host SIM is accessed.
- **Hide menu button:** hide … and use the phone's Back button/gesture to open
  the emulator menu. Disabled by default; otherwise Back still reaches the guest.
  The guest navbar Back button always reaches guest Android.
- Russian translations for vibration, customizable navbar, trackball and menu
  visibility controls.

Sixteen JVM tests and all existing ARM guest-shim/audio ABI tests passed. Both
APK signatures, versions and embedded native assets were verified.

**Setup completion/reboot persistence is not yet verified on-phone.** Camera
support is still being developed separately and is not included in these APKs.
No wallpaper changes were made. Galaxy S5 compatibility remains unverified.

Host: Android 8.0+ (API 26), arm64-v8a. Version code: 11.

SHA-256:
- Clone (`app.aemu.clone`): `cc874f7e5b84031e1f82b3fcc5973cf3fc52725fd9c5fa647eea18a157b8a2a5`
- Standard (`app.aemu`): `7cf9a8a0ce58904f431485e7db5f2598304873df282f1f130658ca4340a61ca1`
