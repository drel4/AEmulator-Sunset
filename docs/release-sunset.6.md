# AEmulator Sunset 0.0.0.3-sunset.6

Experimental compatibility prerelease for stock firmware. **Not yet a stable
Xperia ZR boot: the lock screen is reachable, but an audio-related system_server
crash remains after unlock.** Samsung Galaxy S5 compatibility is not verified.

## Changes since sunset.2

- Corrected guest fopen interposition and ARM32 bionic handling.
- Improved restart bookkeeping, full guest log exports, and native crash diagnostics.
- Added configurable Android 4-style navbar buttons and an optional trackball
  above the navbar, with real relative input events or optional arrow-key mode.
- Added a narrowly scoped offline Qualcomm netmgr multicast endpoint when QEMU
  cannot create the vendor listener. No firmware library patches, modem
  connectivity, GPS fixes, or link-up events are fabricated by this fix.

## Xperia ZR status

The user reports reaching the lock screen. The new logs show the offline
netmgr group-31 bind succeeding. Unlock is followed by a fatal system_server
exception from Sony's xLOUD AudioEffectService (`AudioEffect: set/get parameter
error`), alongside repeated mediaserver crashes. **These audio faults are still
unfixed in this release.** See docs/sunset.6.md in the tagged source.

## Installation

- Host requirements: **Android 8.0+ (API 26), 64-bit ARM Android (arm64-v8a)**.
  These are host requirements, not requirements for the ROM being emulated.
- Clone APK (`app.aemu.clone`): use to install alongside the standard package.
- Standard APK (`app.aemu`): updates require the same signing key.
- Version code: 8.
- If an installer reports `Failed to allocate ... only 0 allocatable`, the
  failure is storage allocation, not evidence of an APK parsing or ABI error.
  Check internal storage, free space, and retry using the system installer.
  If it persists, report phone model, Android version, installer, and exact error.

## Verification

Four ARM native smoke programs and five JVM tests passed. Both release APKs
built on the configured Linux server. Signature/version checks passed; embedded
guest shim hashes match the compiled asset. APKs below are the exact tested .6
artifacts, not a new audio-fix build. Corresponding source is in this release tag.

SHA-256:

- Clone: `2a238a54d52d17ac8a29b392a24112a7fb5102c28cd3647cae6bcac555a46e51`
- Standard: `5df6602c3db8ffb5f59b3f8d13680b9b8a76ae782b3238531b2cf69b7a19df30`
