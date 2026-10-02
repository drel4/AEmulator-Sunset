# AEmulator Sunset 0.0.0.3-sunset.18

- Moved **Minimize** to the bottom menu group beside reboot and shutdown.
- System Back/back gesture closes the log panel without sending guest Back
  or opening the hidden-menu action. Normal guest/menu Back behavior is retained
  when logs are closed.
- Active VMs show **Open** instead of **Start** in the library, including
  preparing/booting guests. **Открыть** is supplied for Russian.
- A process-held file lock makes library status work across the separate app
  and VM processes. Stopped/failed guests and stale markers after process death
  do not count as active. Foreground library status refreshes once per second;
  background library polling is suspended.

No guest/native binary changes. Navbar holding remains as in sunset.17.

## Source and notices

[Exact tagged application source](https://github.com/drel4/AEmulator-Sunset/tree/v0.0.0.3-sunset.18).
[Build instructions](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.18/docs/build-source.md).
[Source/licensing audit](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.18/docs/license-audit.md).

Dated modification notices are updated and packaged in the APKs. The inherited
engine source/provenance gap remains unresolved: this tag rebuilds the APK with
prebuilt engines, not every engine from source. Full GPL compliance is not
certified; the source archives alone do not close the missing-source gap.

## Verification

Both signed APK variants built on the Linux server. All 51 JVM tests passed,
including five new Back/state/lease tests and a real separate-process death
test. APK signatures, versions and packaged legal documents were verified;
native/helper hashes are unchanged from sunset.17. Downloaded APK hashes match
the server's build outputs.
On-device menu ordering, log Back gestures and Open labels still need testing.

Host: Android 8.0+ (API 26), arm64-v8a. Version code: 20.

SHA-256:
- Clone (`app.aemu.clone`): `d360c6b3a62cef8bbdbe6946b60f3d280da46cae26718a1b4ceb16aaaa6f3725`
- Standard (`app.aemu`): `8eaf97645e94ea3f26dd02636f19fe7b3e53beca59956a65493717e208965165`
