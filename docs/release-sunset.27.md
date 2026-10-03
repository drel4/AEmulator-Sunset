# AEmulator Sunset 0.0.0.3-sunset.27

- Host VM window stays in the device's natural orientation. The guest framebuffer
  is no longer rotated or resized based on how the phone is held. Guest Android
  handles its own screen rotation; trackball input is unchanged and untransformed.
- Only navbar icon artwork follows physical orientation, even with host automatic
  rotation disabled. Navbar, trackball and draggable menu-ball positions stay put.
- **Rotate screen** now requests the next rotation from guest WindowManager,
  independently of the Motion sensors option. Uses named Gingerbread/ICS/Nougat
  APIs, not numeric Binder transactions. Disabled in recovery/charging mode.
- **App settings → Appearance → Cutout barrier for VM screen** keeps the guest
  clear of the cutout when on; off permits fullscreen behind it (default off).
- **App settings → Appearance → Navbar style: Original / Sunset**, default Sunset.
  Original uses upstream-style Material icons and a rounded navigation container;
  both styles retain configurable buttons, held keys and pressed highlights.
- App display settings refresh on reopening an already-running VM, without a
  reboot. English and Russian text is included.
- Short host haptic on navbar and trackball press-down; no trackball movement or
  release vibration. Respects the host's touch-feedback setting.
- **VM settings → Disable Google apps** disables detected Google packages,
  Play Store, Play services and account components on the next full VM start.
  Google launchers/keyboards and critical system plumbing are preserved; setup
  remains controlled separately. Turning it off restores only this option's
  recorded changes. APKs and app data are not deleted. Dependent apps may fail.

Manual Rotate screen locks guest rotation; enable automatic rotation again inside
Android if desired. Android's own fixed-orientation app policies still apply.
Real-phone testing is still required for landscape trackball interaction, guest
ROM rotation and display cutouts; host tests do not establish device compatibility.

## Source / license

Verification: 111 JVM tests passed with no failures, errors or skipped tests,
including the Xperia charging-boot fixture. Rotation API-selection and
Google-app discovery/protection/restoration policy smoke tests also passed.
Both APK variants compiled and were verified against the existing signing
certificate. No firmware is shipped in the APKs.

[Exact tagged source](https://github.com/drel4/AEmulator-Sunset/tree/v0.0.0.3-sunset.27),
[build instructions](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.27/docs/build-source.md),
[modification notices](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.27/NOTICE.md),
[source-provenance audit](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.27/docs/license-audit.md).
GPL-3.0; inherited engine Corresponding Source gaps remain disclosed.

## APK SHA-256

- Standard (`app.aemu`, versionCode 29):
  `372b0f8e6e960ad07e67404647a2eb756d58e6d750c6ed086a2b6c085a892f3e`
- Clone (`app.aemu.clone`, versionCode 29):
  `fb475150b5e989d8e817145476860f36469fd6cdd9c4607c8e1b1d07f9ac2dab`
