# AEmulator Sunset 0.0.0.3-sunset.12

**Known camera-discovery bug on older ROMs:** Xperia Android 4.4 ignores the
class-specific loader property used by this build and reports zero cameras.
Use [sunset.13](https://github.com/drel4/AEmulator-Sunset/releases/tag/v0.0.0.3-sunset.13)
for the reversible legacy HAL fallback. Install the same variant and fully
restart the VM; no ROM reimport or wipe is needed.

**First experimental host-camera bridge for standard ARM32 HAL1 ROMs.**
Opt-in, disabled by default. Adds rear/front host-camera enumeration (at most
one of each), 640×480 NV21 preview and JPEG photographs. Uses Camera2 on the
host and a standard camera HAL1 in the guest; stock APKs/ODEX are not patched.
The emulator selects its own `camera.aemu_host.so`, not a stock sensor driver.

## Try it

1. Install the same package variant over the existing build; do not uninstall.
2. Fully stop the VM. In the ROM's AEmulator settings, enable **Host camera
   (experimental)** / **Камера телефона (экспериментально)** and save.
3. Start the ROM and grant AEmulator's camera permission before boot.
4. Open the guest camera app; test preview, taking a photo and switching lenses.
   If Sony's stock camera needs proprietary extensions, try a standard Camera
   API app to distinguish the core bridge from vendor-app compatibility.

The camera is released when AEmulator leaves the foreground. After returning,
reopen the guest camera app. If permission is denied/revoked, grant it in the
host Android app settings and fully restart the VM. Disabling the option and
restarting returns the previous behavior; nothing opens a camera by default.

## Limits and verification

- HAL1 path only, offered for Android 4.0–7.1 ROMs (API 14–25). No Android 2.x
  camera interface, HAL2/HAL3, video recording, raw capture or guest-controlled
  flash/autofocus/zoom. Vendor-specific Sony/Samsung extensions are not implemented.
- Photos are preview-resolution JPEGs, not full sensor-resolution still capture.
- Real host-camera capture and Sony/Galaxy S5 camera apps are **not yet phone-tested**.
  This is a compatibility trial, not a promise that every stock camera app works.
- 25 JVM tests passed; camera C smoke passed with ASan/UBSan. Actual release HAL
  loaded with Xperia ZR's unmodified linker/libc under QEMU, and ARM preview/JPEG
  smoke passed using that Bionic runtime. Existing 5 ARM shim and 2 audio tests passed.
- Both signed APKs checked for version, permission and embedded-library hashes.
  Sunset.11's shim, setup/vibration fixes and Binder rollback remain unchanged.

On failure, attach both host and logcat logs and say whether preview, lens
switching or saving fails. Host bridge logs start with `camera:`.

Host: Android 8.0+ (API 26), arm64-v8a. Version code: 14.

SHA-256:
- Clone (`app.aemu.clone`): `b526f9abc24ff70116496c26a0fb46e5fd4f32fb18ea65ec2f727cd6ff3ea9b6`
- Standard (`app.aemu`): `c5778306a91e6a328219a1d61a124b55c340352d759e28ad0696929bdad28c79`
