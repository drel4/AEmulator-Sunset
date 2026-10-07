# Sunset.34 — 2026-10-07

## Changes

- Empty first-use libraries can download/import the Sunset CM11 ROM after
  consent. Declining is remembered; existing VM libraries skip the offer.
- Catalog download buttons request consent and then download/import directly.
  Section source links still open in a browser. Web-page links offer a browser
  fallback rather than trying to import HTML.
- Downloads/imports continue in a dedicated foreground service when leaving
  the screen. The notification shows percentage and downloaded/total size,
  switches to extraction progress, and includes Cancel and return-to-library
  actions. Unknown-length downloads/extraction use an indeterminate bar.
- Streaming downloads show progress, support cancellation, reject incomplete
  transfers and clean up temporary files. The starter CM11 file is SHA-256
  checked against the published AESS442-3 image. Update `StarterRom`'s digest
  deliberately whenever that served starter image changes.
- Compression signatures take precedence over misleading `.tar` suffixes.
  TAR/ZIP/7z wrappers can contain TGZ, compressed TAR or nested ZIP firmware;
  factory image ZIPs and Odin partition handling remain supported. Archive
  nesting and guest paths are checked; extraction can be cancelled.
- New dialogs/errors are available in English and Russian.

Both standard and clone APKs are supplied. Downloads import a **new** VM;
they do not replace existing VM data or start Android automatically. Downloads
are not resumable after process death or force-stop. Notification permission
is needed to see progress in the notification drawer. The service is typed
[`dataSync`](https://developer.android.com/develop/background-work/services/fgs/service-types#data-sync)
for user-started downloads and local imports; it is separate from the VM keeper.

The user confirms CM11 sound works after Sunset.33/AESS442-3. This release does
not modify the published CM11 image or firmware APKs. No new native assets.

## Verification

170 JVM tests passed with zero failures, errors or skips, including HTTP
transfer/cleanup, nested container policies and notification percentage/task-ID
guards. Both release APKs built successfully using JDK 21, SDK/build-tools 36,
Gradle 8.14.2, a 1024 MiB heap and one worker. APK v2 signatures use the existing
publisher certificate; both package IDs and version code 36 were verified.
Legal assets and the unchanged guest shim match their checked-in build inputs.
No native components were changed or rebuilt in this release.

SHA-256:

- Standard: `881053012769af82f7ec54754119a01f34383ff8dfde3831339e3865b4b9405f`
- Clone: `37a8be330f6189161e207e74b5e913bccfd5a9f7c9f27b0100f95449fa7a9a60`

Background-service/notification behavior, first-use dialogs and a complete
Nexus factory-ROM import still require
on-device confirmation; JVM fixtures do not establish guest boot compatibility.

Source: release tag `v0.0.0.3-sunset.34`. See [build instructions](build-source.md)
and [inherited engine-source limitations](license-audit.md).
