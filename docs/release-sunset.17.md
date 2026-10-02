# AEmulator Sunset 0.0.0.3-sunset.17

## Navbar holds and Minimize

- Hold any Sunset navbar button to hold the actual guest key. Home long-press
  can open Recents on Android 2 if the ROM supports it; no setting is required.
- Key-up is sent on release, gesture cancellation, removal of the control and
  activity pause/destruction, avoiding stuck guest keys. Accessibility clicks
  remain single taps.
- **Minimize** in the emulator menu returns to Sunset's main library without
  stopping/rebooting the guest. Start the same ROM to return. Host camera
  capture closes while the guest screen is backgrounded, as before.
- New menu/license labels are translated into Russian.

## License notices and source access

GPLv3 text and upstream credits are preserved. Modification notices now cover
Sunset's changes through 2026-10-02. License, notices, audit and build
instructions are packaged in the APK; Settings offers an offline notice viewer.

Application source used for these APKs:
[tagged checkout](https://github.com/drel4/AEmulator-Sunset/tree/v0.0.0.3-sunset.17).
[Build instructions](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.17/docs/build-source.md).
[Source/licensing audit](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.17/docs/license-audit.md).

**Unresolved:** inherited prebuilt engine binaries do not yet have verified
matching source/patches/build instructions in this repository. The tag rebuilds
the application using those binaries, not the entire engine from source.
Complete Corresponding Source and third-party notice coverage are not certified;
notices or GitHub source archives alone do not close this gap.

## Verification

Both signed APK variants built successfully on the Linux server. All 46 JVM
tests passed, including three held-key ownership/lifecycle tests. APK versions
and signatures were verified; packaged license/notices match the checkout,
and native/helper hashes match sunset.16. Local APK hashes match the server.
Android 2 long-press, Minimize/resume and the notice viewer still require
on-device testing (no device was connected during this build).

Host: Android 8.0+ (API 26), arm64-v8a. Version code: 19.

SHA-256:
- Clone (`app.aemu.clone`): `b48db3d4e0ff4370014ac4c272189b3ef8d7b7cca5b01fa11a0cb54285f7cb60`
- Standard (`app.aemu`): `ba821c0bb89fc45f096da19f104db49ce2a85ab0b22f8fe20ce6341cad0e7e8e`
