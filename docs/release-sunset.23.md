# AEmulator Sunset sunset.23

## Changes

- **Hold Start: charging-only firmware mode**, replacing sunset.22's mistaken
  reduced-refresh normal-Android preset. Still experimental and per boot.
  Selects the ROM's native `class charger` services (Sony/AOSP), Samsung
  `playlpm`/charging definitions, or a supported native charger fallback.
  No zygote, launcher, normal boot service plan, setup helper or normal boot
  completion/statistics are started. Unsupported ROMs fail visibly rather
  than silently booting normal Android. Fresh boot intents are handled even
  when Android reuses the existing VM activity.
- Removed the library LPM hint and activation toast (accessibility long-click
  label retained). Charging draws directly through the framebuffer, not the
  normal GPU surface; original charging executables remain unchanged when a
  private sysfs-compatible runtime copy is needed.
- **Old imports need repair:** previous importers deliberately discarded
  ramdisk `/charger`. Shut down all VMs, open VM settings with experimental
  features unlocked, and choose **Repair charging files from this ROM's
  boot.img**, selecting the original matching boot image. For Xperia ZR this
  is `boot.img` from the same firmware, not recovery.img. Only missing charging
  executables/assets/rules are added; existing firmware/data are not replaced.
  New ROM imports retain `/charger` automatically.
- **Reset VM data now permanently deletes `/data`, `/cache` and runtime setup
  state, without creating a backup.** Confirmation explicitly warns this is
  irreversible. Firmware, VM settings, recovery and shared SD remain intact.
  Traversal never follows symlinks; deleted inode ownership records are pruned
  to prevent recycled inodes inheriting stale application UIDs. Process-lifetime
  storage locking and orphan-writer checks remain. An I/O failure may leave
  partially erased data. Existing sunset.22 backup folders are not auto-deleted.
- English/Russian labels and regression tests updated.

## Verification and limits

Charging service selection was checked against the user's Xperia ZR boot
ramdisk (`/charger`, `healthd-charger`, and charger-class init actions).
JVM tests cover Sony/Samsung plans, omitted charger detection, healthd fallback,
private binary adaptation, repair-path validation, permanent reset, ownership
pruning, symlink/traversal rejection and lease exclusion.
Both release variants built successfully on Linux; **81 JVM tests passed**
with zero failures/errors/skips. The real Xperia boot.img repair fixture ran
successfully, preserving the original charger bytes and selecting `/charger`.
APK signatures, package/version identity, legal assets and unchanged native
compatibility-helper hashes verified.

No connected Android device: actual stock charging graphics, native static
syscalls, power-button behavior and OEM battery/PMIC requirements need phone
testing. This is **not yet guaranteed to work across all ROMs**. Guest battery
sysfs remains emulated; this mode does not control physical phone charging.
Native charger exit is reported with its log name rather than normal-boot fallback.
Legacy healthd charging fallback follows [AOSP's charging-mode entry point](https://android.git.googlesource.com/platform/system/core/+/refs/tags/android-7.1.2_r5/healthd/healthd.cpp).

## Source/license

- [Exact application source](https://github.com/drel4/AEmulator-Sunset/tree/v0.0.0.3-sunset.23)
- [Build instructions](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.23/docs/build-source.md)
- [Modification notice](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.23/NOTICE.md)
- [Source/licensing audit](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.23/docs/license-audit.md)

GPL-3.0 fork changes and upstream attribution retained. Inherited engine
prebuilts still lack verified matching source/build provenance; this release
does not claim complete Corresponding Source or full GPL compliance.

## APK SHA-256

- Standard (`app.aemu`): `42baa1579d4b6f924a62a73eee7a3b2bad58389c1481864f21a61fd680de1dc6`
- Clone (`app.aemu.clone`): `a2d30a7fbc4fb48e6dcfbe3a22cf5f66110dcb07464b17b092415f8f15c85d03`
