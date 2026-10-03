# AEmulator Sunset 0.0.0.3-sunset.28

- Added **Host** to VM screen resolution presets (Russian: **Хост**), using
  the existing host-resolution mode on the next full VM start.
- Selecting a fixed preset or editing width/height/density switches back to
  manual resolution. Host and fixed presets cannot appear selected together.
- Host preserves the saved manual resolution for later use. The legacy 2.x
  engine still has its fixed resolution and does not expose these presets.

Verification: 114 JVM tests passed, including three resolution-preset checks;
both signed APK variants compiled. Device testing remains needed for UI layout.

[Exact tagged source](https://github.com/drel4/AEmulator-Sunset/tree/v0.0.0.3-sunset.28),
[build instructions](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.28/docs/build-source.md),
[modification notices](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.28/NOTICE.md),
[source-provenance audit](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.28/docs/license-audit.md).
GPL-3.0; inherited engine Corresponding Source gaps remain disclosed.

## APK SHA-256

- Standard (`app.aemu`, versionCode 30):
  `ebdf2065225c6b200e822d70cda930bf5267d6be295981b1bdb5ec96abc20080`
- Clone (`app.aemu.clone`, versionCode 30):
  `d8f499ae12ddb10a8afddceecd3189d6b76d08e99145448b420b13f5043808c6`
