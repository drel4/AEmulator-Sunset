# AEmulator Sunset 0.0.0.3-sunset.20

## Camera initialization compatibility

The supplied Xperia ZR logs show the host HAL loading successfully, followed
by a null dereference in Sony's `ParameterManager.updateVideoOption` (line 172).
Inspection of the stock ODEX identifies the missing current-mode settings:
the app launches `SCENE_RECOGNITION` by default, but only creates that mode's
settings when `sony-scene-detect-supported` is true.

- The generic host HAL now exposes that automatic-mode shell, using its existing
  automatic host preview/JPEG capture path. Firmware APKs/ODEX remain unchanged.
- Scene-apply types and the extension version stay empty. No proprietary Sony
  scene classification, enhancement, face detection or extension service is
  implemented. The app's caught version-parsing warning may remain.
- Preview remains 640x480 NV21; JPEG uses the current preview frame. Video
  recording, raw capture, flash and proprietary sensor commands remain unsupported.

**This targets the reported initialization crash, not a verified stock-camera
preview/capture fix.** No connected device was available for the stock app test.
Further vendor assumptions may appear after initialization.

Sony stores capabilities in its Camera app preferences. If the same crash
persists after updating and rebooting, clear **only the guest Camera app's data**
in guest Settings, then reopen Camera to rediscover capabilities. This resets
Camera preferences. Do not wipe the ROM; Sunset does not erase app data.

## Verification

Native ASan/UBSan smoke checks pass, including the Sony mode-map capability
precondition, parameter round trip, unchanged unsupported-recording behavior,
preview/JPEG callbacks and callback re-entry. The ARM HAL also passes the stock
Sony Bionic ABI/smoke tests and actual legacy `hw_get_module` fallback selection.
These fixtures do not exercise the Sony camera UI or physical host Camera2 device.

Both signed APK variants built on the Linux server; all 55 JVM tests passed.
APK signatures, version/package metadata, embedded native/helper and legal
document hashes were checked against build sources and server outputs.

## Source and notices

[Exact application source](https://github.com/drel4/AEmulator-Sunset/tree/v0.0.0.3-sunset.20).
[Build instructions](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.20/docs/build-source.md).
[Source/licensing audit](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.20/docs/license-audit.md).

Dated modification notices are updated and packaged. The inherited engine
source/provenance gap remains unresolved; complete Corresponding Source is not
certified. No stock firmware code or decompiled Sony app code is distributed.

Host: Android 8.0+ (API 26), arm64-v8a. Version code: 22.

Camera HAL SHA-256: `8145a0975b0815cd49984caaf28abbc14b94162fa93bebba991b327da8dba379`.

APK SHA-256:
- Clone (`app.aemu.clone`): `a7a7fa9c3299447977c0bf2b4263991387026d158baf3912d0001a48052542c7`
- Standard (`app.aemu`): `1a7c64f0dcc232a653c461e1adc7d0c36fe350dcbf3d985ea61174ad9ceee75d`
