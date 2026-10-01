# AEmulator Sunset 0.0.0.3-sunset.15

## ROM catalog

Open the new **ROM catalog** from the library list icon or app settings.
It loads the deployed Sunset `rom.list`, shows the leading MOTD, sections,
device/Android/skin fields, all four compatibility statuses and comments.
The pencil button edits and saves the list URL; refresh fetches it again.
ROM downloads, section sources and list-source links open in your default
browser. No automatic ROM download or import is performed.

Empty sections show **No ROMs at the moment.** Entries without a URL show
**ROM download URL isn't available right now.** They are not removed from the
catalog. Statuses are catalog reports, not app-verified compatibility claims.
Malformed rows are reported without hiding valid entries. Fetch size, redirects
and timeouts are bounded; HTTPS downgrades and non-web link schemes are rejected.

## Experimental features

Tap the **AEmulator Sunset** title 42 times in the library or app-settings about
card. Tap 42 permanently unlocks experimental settings, shows the activation
toast, and opens the requested YouTube video. Further taps do not reopen it.
Currently this reveals the host-camera setting, without automatically enabling
camera access. Already-enabled camera configurations remain available.

All new UI labels/messages are translated into Russian. Setup skipping from
sunset.14 is included; guest shim/camera/audio binaries are unchanged.

## Verification

43 JVM tests, including catalog format/empty entries/missing URLs/invalid links,
exact 42-tap activation, and setup opt-out regression tests. Both signed APKs
are built and checked for version/signatures and unchanged guest-native assets.
The compiled parser loaded the deployed catalog without warnings: four sections,
ten ROMs, two empty sections and two entries without URLs. End-to-end screen and
browser testing on a phone is still needed.

Host: Android 8.0+ (API 26), arm64-v8a. Version code: 17.

SHA-256:
- Clone (`app.aemu.clone`): `0410cf1b6cc6e14441e75523736e5a8d79de122561e1d7c4e6fa44423c4be7c2`
- Standard (`app.aemu`): `f4a5f0de9db324146afa7d319b6ec3deb43d725ee194f1decf3c180b2b4ab5c8`
