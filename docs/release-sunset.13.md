# AEmulator Sunset 0.0.0.3-sunset.13

**Fixes camera HAL discovery on older ROMs, including Xperia ZR Android 4.4.2.**
Sunset.12's host bridge had permission and started successfully, but the Sony
camera service reported `Could not load camera HAL module`. The app then saw
zero cameras and failed in `Camera.getCameraInfo`.

The old ROM loader does not use `ro.hardware.camera`. This build additionally
installs the host-camera HAL in its `camera.default.so` fallback slot on API
14–20. Global hardware properties, stock camera APKs and ODEX files are not
patched. Existing default HAL files/symlinks are preserved and restored when
the camera option is disabled and the VM restarts. Externally modified fallback
files are not silently overwritten. The native HAL and guest shim are unchanged.

## Try it

Install the same APK variant over .12, keep **Host camera (experimental)**
enabled and fully stop/restart the VM. No ROM reimport or data wipe is needed.
Open the guest camera app and test preview, taking a photo and switching lenses.
The host log should report `camera: legacy default HAL fallback installed`
on the first enabled boot.

Camera access remains opt-in and foreground-only. All .12 limitations remain:
HAL1, 640×480 preview/JPEG, no video recording, no proprietary Sony/Samsung
extensions. This fixes discovery, **not a confirmation that every stock camera
app works**. Send both logs again if preview/capture still fails.

## Verification

Sony's unmodified libhardware/libc/linker reproduced .12's failed discovery,
then selected the host-camera HAL through the new default fallback under ARM
QEMU. ARM preview/JPEG smoke and ASan/UBSan C smoke passed. Existing shim/audio
checks passed. All 29 JVM tests passed, including fallback update, restoration
of files/symlinks, opt-out cleanup and refusal to overwrite external changes.

Host: Android 8.0+ (API 26), arm64-v8a. Version code: 15.

Both APK signatures, version codes and embedded native-library hashes were
verified. Real phone preview/capture still needs the user's test.

SHA-256:
- Clone (`app.aemu.clone`): `b3d02d63c9c4d7b68cb799461b148b69044e96cd0275cd739248efc0b6c46d2a`
- Standard (`app.aemu`): `cbf4846cdb93f6e7f34facb310a15caadde98bbae5dfb732e87523904f9e47bd`
