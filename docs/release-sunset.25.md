# AEmulator Sunset 0.0.0.3-sunset.25

- **VM settings → Export VM (.aessvm)** exports current settings, system,
  available boot files, and optionally the data partition (off by default).
- Fully shut down all VMs before exporting. Choose the destination using the
  Android document picker. No temporary full copy of the VM is needed.
- Restore with **Import firmware** to create a new independent VM. Existing
  VMs are not overwritten, and exported settings are preserved.
- Guest ownership, permissions and safe relative links are carried in the
  archive; host runtime sockets and host-engine links are excluded.
- New firmware imports retain recognized original boot images. Older imports
  without them export the available extracted ramdisk files instead.
- English and Russian controls. See [archive details](vm-archive.md).

**Privacy:** data exports can contain accounts, tokens and private files.
Archives are not encrypted. Recovery and shared SD-card files are not included.
Keep the export screen open until completion. Real phone/document-provider
testing is still needed; JVM archive tests are not an Android-device test.

## Source / license

Verification: both release variants compiled; **93 JVM tests passed**, including
12 archive tests (round-trip, optional data, boot files, ownership, guest/host
links, shared system, traversal, runtime entries, duplicates, truncation and
format version). No failures, errors or skipped tests. Charging boot fixture
tests used the private Xperia boot image; no firmware is shipped in the APKs.

[Exact tagged source](https://github.com/drel4/AEmulator-Sunset/tree/v0.0.0.3-sunset.25),
[build instructions](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.25/docs/build-source.md),
[modification notices](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.25/NOTICE.md),
[source-provenance audit](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.25/docs/license-audit.md).
GPL-3.0; inherited engine Corresponding Source gaps remain disclosed.

## APK SHA-256

- Standard (`app.aemu`, versionCode 27):
  `3b074f4eb5c6aea532d871adc45b737cef622daac14d33bb4846e2745331be5f`
- Clone (`app.aemu.clone`, versionCode 27):
  `ab3b4f2c0164cde4b7307c7567a31ba91e65b00de430391d2b0882f9a066c794`
