# AEmulator Sunset sunset.22

## Changes

- VM settings: confirmed **Reset VM data**. Fully shut down all VMs first.
  Moves guest `/data`, `/cache`, the ownership seed stamp and runtime state into
  a unique `data-reset-*` folder inside that VM's image directory. Apps, accounts,
  settings and internal guest files reset; firmware, emulator settings, recovery,
  snapshots and the separately shared SD-card folder remain untouched.
  **Backups still use disk space**; this is not a storage-reclamation feature.
  No restore UI yet: with all VMs shut down, backup contents can be moved back
  to their original paths (data/cache/stamp under `root`, `run` beside `root`).
  Do not merge old and new data trees. Keep a copy before manual restoration.
- Experimental **hold Start for low-power boot**. Unlock experimental features
  by tapping the Sunset title 42 times. Guest framebuffer refresh and input are
  capped at 30 Hz (lower saved caps are preserved); host keep-screen-on is off.
  GPU/JIT, RAM, resolution and saved settings stay unchanged. Normal Start and
  reboot use normal saved settings: hold Start again for each new LPM boot.
  Opening an already running VM does not change its current mode.
- English/Russian labels, long-click accessibility action, and storage locking
  shared by VM preparation/boot and reset. Failed/stopping VM processes retain
  the storage lease until they exit, so stale UI status cannot permit reset.
  Same-UID native orphan helpers are also checked before moving data.

## Verification and limits

JVM regressions cover temporary preset values, lower caps, recoverable reset,
firmware/settings retention, traversal and symlink safety, and competing leases.
Release APKs are built and signature/identity/assets checked before upload.
Both release variants built successfully on Linux; **77 JVM tests passed**
(zero failures/errors/skips). APK signatures, package/version identity,
bundled legal documents and compatibility-helper hashes verified.
No connected Android device: gesture behavior, fresh ROM boot after reset and
actual battery savings require device testing. Not Android's guest battery-saver
API or a CPU frequency governor; native GL paths may not honor the framebuffer
cap uniformly. Existing ROM compatibility and camera limitations remain.

## Source and license

- [Exact application source](https://github.com/drel4/AEmulator-Sunset/tree/v0.0.0.3-sunset.22)
- [Build instructions](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.22/docs/build-source.md)
- [Modification notice](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.22/NOTICE.md)
- [Source/licensing audit](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.22/docs/license-audit.md)

GPL-3.0 fork changes and upstream attribution retained. Inherited engine
prebuilts still have unresolved matching-source/build-script provenance; this
release does not claim complete Corresponding Source or full GPL compliance.

## APK SHA-256

- Standard (`app.aemu`): `264492b263d38b6dd976aa705e912ea3100d76e9cb7f35567bc88975ced11efd`
- Clone (`app.aemu.clone`): `65e914553bedc86bf951325faecc214d75eed716e9b40a4058f65a595ee58db2`
