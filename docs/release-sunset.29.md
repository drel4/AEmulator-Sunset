# AEmulator Sunset 0.0.0.3-sunset.29

- Rotate screen now simulates physical accelerometer orientation when the host
  motion bridge is disabled. Hidden with host sensors enabled. Guest auto-rotate
  policy applies; the host does not rotate or stretch guest framebuffer pixels.
- Tablets (smallest width >= 600dp) allow host window rotation to avoid forced
  portrait compatibility letterboxing. Phones keep a fixed natural window.
- Tablet-only app setting **Rotate tablet navbar**: enabled follows the window
  bottom; disabled retains the natural bottom edge with side/top layouts. Tablet
  navbar rotation depends on actual display rotation, never physical tilt with
  host auto-rotate disabled. Native trackball views remain untransformed.
- Host resolution is selected only via the **Host** preset; removed the duplicate
  toggle. Resolution does not change during a running VM's host rotation.
- Flexible `.aessvm` exports: firmware (system + available boot, paired), settings
  and data selections. Version 2 exports; version 1 restores still supported.
- **Import settings** loads only settings, confirms different/unverifiable ROM
  identity and applies after Save. **Import boot partition** is available only
  for VMs without boot files and refreshes the boot plan without replacing data.
- First-launch advisory for chipsets not recognized as Snapdragon, Dimensity or
  Google Tensor; uses public Android SoC fields and conservative hardware-name
  fallback. Suggests disabling JIT/GPU bridge before reporting boot failures.
  Detection is advisory, incomplete on some hosts, and never guarantees boot.
- English/Russian additions and dated source/license notices.

Phone/tablet testing of the new behavior remains required. Host motion sensors
were reported working by the user on sunset.28; that is not a test of every ROM
or host. The separate AOSP 4.4.2 image and first-run image download are not part
of this APK release.

Verification: **135 JVM tests passed**, with zero failures, errors or skipped
tests. Both signed APK variants compiled; package/version/signing identities
and packaged license/notices were verified.

[Exact tagged source](https://github.com/drel4/AEmulator-Sunset/tree/v0.0.0.3-sunset.29),
[build instructions](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.29/docs/build-source.md),
[modification notices](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.29/NOTICE.md),
[source-provenance audit](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.29/docs/license-audit.md).
GPL-3.0; inherited engine Corresponding Source gaps remain disclosed.

## APK SHA-256

- Standard (`app.aemu`, versionCode 31):
  `cd13b7c25dbf130ea72e600bfbdcc0acbc2b894a7e5267437b4b26b7e9ed8ccf`
- Clone (`app.aemu.clone`, versionCode 31):
  `a54914d56d22b32d08313527e37937da790b43f86d622e249a8560ec207c03f4`
