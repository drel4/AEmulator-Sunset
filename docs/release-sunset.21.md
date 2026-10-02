# AEmulator Sunset 0.0.0.3-sunset.21

## YAFFS2 backup import

- Added an original bounded Kotlin reader for clean CWM/mkyaffs2image recovery
  snapshots: 2048+64 and 4096+128 page/spare geometry, both byte orders.
- Import standalone system images or `system.yaffs2.img` in ZIP, 7z or TAR,
  including wrapped backup folders and existing nested-archive workflows.
- Files/directories, symlink aliases and executable bits are retained; hard-link
  targets are resolved and copied. Special devices are not created on the host.
- Other YAFFS2 partitions (data/cache/.android_secure) are ignored in archives;
  firmware import does not restore personal backup data.
- Corrupt/truncated snapshots, unsafe paths, cycles, repeated/missing chunks and
  unsupported journal/shrink/shadow layouts fail explicitly. Cancellation is
  checked while indexing and copying.
- Import descriptions were updated in English and Russian.

The supplied CM7 CWM system snapshot's MD5 matches its nandroid.md5 entry.
The new reader successfully indexed 984 objects and copied all 677 regular/
hard-link files (126,701,977 bytes), verifying complete per-file chunk sequences
and locating build.prop/framework. The original backup was not modified.

Use a ZIP with **system.yaffs2.img + boot.img**, optionally recovery.img, to
include the guest ramdisk/init metadata. No folder-picker workflow is added.
[Detailed format limits and usage](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.21/docs/yaffs2-import.md).
Raw NAND journal replay, ECC repair, YAFFS1/in-band/extended tags and 64-bit file
sizes are not supported. Successful unpacking does not prove CM7 boots.

## Clone updater fix

The updater previously selected the first APK in a release, which could be the
standard APK even in the clone app. It now selects the exact application-ID
filename suffix (`-app.aemu.apk` or `-app.aemu.clone.apk`) without cross-variant
fallback. The downloaded APK package and any APK passed to installation must
also match the running app's package; a mismatched download is deleted/rejected.
No in-place switch between standard and clone is attempted.

**Older installed builds still have their old updater:** install this release's
matching clone APK manually once. Future checks then use the corrected selector.

## Verification

All 12 new parser/asset-selector tests passed locally, including the real CWM
fixture (not distributed). Both signed APK variants built on Linux and all 67
JVM tests passed. APK signature/version, packaged legal documents and unchanged
native/helper hashes were checked; local APK hashes match the server outputs.
No on-device import/boot or Android installer interaction was available to test.
Camera behavior is unchanged from sunset.20.

## Source and notices

[Exact application source](https://github.com/drel4/AEmulator-Sunset/tree/v0.0.0.3-sunset.21).
[Build instructions](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.21/docs/build-source.md).
[Source/licensing audit](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.21/docs/license-audit.md).

Dated modification notices are updated and packaged. The inherited engine
source/provenance gap remains unresolved; complete Corresponding Source is not
certified. No stock ROM, user backup data or vendor code is distributed.

Host: Android 8.0+ (API 26), arm64-v8a. Version code: 23.

APK SHA-256:
- Clone (`app.aemu.clone`): `cff1a3420020db2a297ee8fddafb326bb187ad5045caf7be938cdf6aa163dc57`
- Standard (`app.aemu`): `c7c9dbef23fe5d9962e069b0331d2df027ae4b611599967cfbbc20d82e4e2a75`
