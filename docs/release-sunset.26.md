# AEmulator Sunset 0.0.0.3-sunset.26

- **VM settings: Use host resolution** uses the host's physical pixel size and
  density on the next full VM start. Legacy Gingerbread engine is excluded.
  Higher resolutions can increase RAM usage and rendering cost.
- The VM window now extends behind display cutouts. With host resolution on,
  emulator controls overlay the guest rather than reducing its display area.
- Navigation controls stay on the phone's natural bottom edge when rotated;
  icons remain upright. Pressed buttons have a rounded-square highlight.
- **Rotate screen** is beside the volume/power buttons in the in-VM menu.
- **Motion sensors** is an experimental, opt-in setting for Android 2.3–7.1
  guests using the standard legacy sensor HAL interface. It forwards available
  host accelerometer, gyroscope and magnetic-field readings, and pauses host
  sampling while the VM screen is in the background. OEM-specific interfaces
  are not guaranteed. Restart fully after changing the setting.
- Samsung `samsungani` and `playsound` files were already retained during
  import. Their optional services now support firmware boot-animation control
  and whitelisted boot-animation property triggers, preserving service arguments.
- New settings and controls have English and Russian text.

Real-phone testing of sensors, Samsung boot media, rotation/touch mapping and
cutout layouts is still needed. Build and unit tests do not establish that every
ROM supports these features. Original firmware archives are not modified.

## Source / license

Verification: both signed release variants compiled; 104 JVM tests passed,
including the Xperia charging-boot fixture tests. The native sensor transport
and lifecycle smoke test passed under ASan/UBSan. No firmware is shipped.

[Exact tagged source](https://github.com/drel4/AEmulator-Sunset/tree/v0.0.0.3-sunset.26),
[build instructions](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.26/docs/build-source.md),
[modification notices](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.26/NOTICE.md),
[source-provenance audit](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.26/docs/license-audit.md).
GPL-3.0; inherited engine Corresponding Source gaps remain disclosed.

## APK SHA-256

- Standard (`app.aemu`, versionCode 28):
  `8a549001b19fde4724f4453877e7f902cd3a952688df9cf9ba42810041c2253d`
- Clone (`app.aemu.clone`, versionCode 28):
  `9f913b6b71085d029da630411b7e21a7098c5a04f5104619d3e34527a61b82f1`
