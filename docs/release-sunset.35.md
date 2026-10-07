# Sunset.35 — 2026-10-07

## Changes

- Removed the old-Android/daily-use sentence from the English and Russian CM11
  welcome popup. Its download estimate is now about 203 MB (193 MiB).
- New and existing VM configurations without the new `hostBattery` field follow
  host battery percentage and charging state by default. An explicit saved
  `false` is preserved, including settings restored from an `.aessvm` archive.
  VM settings include an opt-out; saved changes apply at the next boot.
- Battery readings come from the protected host battery broadcast. A scaled,
  bounded reading seeds virtual power-supply files before guest services start.
  An independent worker polls for changes every 15 seconds, even when minimized.
  On Android 4.2+ it also uses the guest BatteryService override after reported
  boot completion, and reapplies after supervised zygote restarts. Older guests
  use virtual sysfs; their runtime refresh behavior needs device verification.
  [AOSP 4.2.2 BatteryService](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-4.2.2_r1/services/java/com/android/server/BatteryService.java)
  exposes the override; [Gingerbread's implementation](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-2.3.7_r1/services/java/com/android/server/BatteryService.java)
  does not. Battery forwarding uses no new host runtime permission.
- Manual battery Apply pauses host following for this session. Reset restores
  the configured mode; the next full boot also restores the saved mode.
  Recovery/charging-only boots receive sysfs readings without framework commands.
  Failure/stop unregister the receiver and stop its worker. Commands are bounded,
  and battery-node writes reject symlinks rather than following them onto the host.
- Catalog Google Drive, MEGA and other recognized sharing-page links open in the
  browser immediately. Other links get a bounded small streaming content probe:
  HTML/JSON/XML pages, failed probes and unsafe redirects use the browser; binary
  direct downloads keep Sunset's consent-based background download/import flow.
  Section source links remain browser links.
- Both starter and direct-link download confirmations include **Open in browser**
  alongside download/import and decline/cancel. English and Russian labels remain
  available. Existing VMs and user data are not replaced.

The published CM11 image and its checksum are unchanged. No firmware APKs or
native engine assets were changed or rebuilt. Previously exported one-time notes
belong to their archives and are not rewritten by this app update.

## Verification

Release version `0.0.0.3-sunset.35`, version code 37. JDK 21, SDK/build-tools 36,
Gradle 8.14.2, 1024 MiB Gradle heap, one worker and in-process Kotlin compilation.

183 JVM tests passed with zero failures, errors or skips. Both final APKs built
successfully (final build: 3m 31s), have the existing publisher v2 signature,
and report their correct package IDs and version code 37. Packaged license,
notices, source instructions and audit match the checkout; the common guest shim
is unchanged. All 16 changed application/build inputs match the remote build.

SHA-256:

- Standard: `6dc8455b029c6b73839449644cc003c16239f374864a41847dcdb2172bc59fde`
- Clone: `fb38dd23aeb9aae342e2400216b87a0f28765374d5afc3939e2d448f05941fe8`

Physical-device validation remains pending for host battery forwarding,
manual overrides, browser launches and dialogs. JVM tests do not establish
compatibility with every guest ROM.

Source: tag `v0.0.0.3-sunset.35`. See [build instructions](build-source.md) and
[inherited engine-source limitations](license-audit.md).
