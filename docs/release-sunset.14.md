# AEmulator Sunset 0.0.0.3-sunset.14

## Skip setup wizard

New per-ROM **Skip setup wizard** option, with English and Russian labels.
Enable it in ROM settings, save, and fully stop/restart the VM. No ROM reimport
or data wipe is needed. The option defaults off.

Designed for guest Android 4.1–7.1 (API 16–25), including the Xperia ZR and
Galaxy S5 firmware families. It marks `device_provisioned` and
`user_setup_complete` through the guest SettingsProvider, verifies the values,
then disables recognized AOSP/Google/Sony/Samsung/HTC/MIUI/LG/Motorola setup
packages. It also handles Sony's initial-boot setup package.

This does not patch setup APKs, ODEX files or framework code. A bounded helper
waits for guest services on fresh data rather than waiting for boot completion.
It does not introduce host database writes for this option during a live boot.

Turning it off and fully rebooting restores enabled states of packages disabled
by this option. It **does not reset completed setup**. Previously disabled
packages are left alone. API 16+ no longer automatically skips non-Sony setup
when this option is off; already-completed data stays completed. Older-engine
behavior and the separate legacy Google login workaround are unchanged.

Unknown OEM package names/vendor-specific setup checks may need further work.
This is not an activation/account-lock bypass. Camera support and native
shim/audio/graphics libraries are unchanged from sunset.13.

## Verification

Guest helper compilation and DEX packaging; eight package-policy checks;
33 JVM tests, including the Sony opt-out regression; both signed APK builds. APK versions, signatures and
embedded helper/native assets are checked before publication.

End-to-end setup skipping/restoration on stock ROMs still needs a device test.
If it fails, send host and guest logs; host output includes `setup option` and
`SETUP_SKIP_OK` / `SETUP_RESTORE_OK` markers.

Host: Android 8.0+ (API 26), arm64-v8a. Version code: 16.

SHA-256:
- Clone (`app.aemu.clone`): `d6480b2e80833e4de0e05a10d45da5284e2941876c12c66b227a340eebdd104c`
- Standard (`app.aemu`): `df8e199e93612ac7dc236f4c86c1f75da2c0db071ad7338bc8cae6bb51dbd546`
