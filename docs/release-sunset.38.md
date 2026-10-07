# Sunset.38 verification record

Date: 2026-10-08 (Europe/Moscow). Version code 40, version name
`0.0.0.3-sunset.38`. Standard `app.aemu`, clone `app.aemu.clone`.

## Changes

- Experimental Universal Sunset Extended profile with common guest-contained
  search-path fallbacks and safe service-local init `setenv` transport.
- VM engine picker: default Universal Sunset, experimental Extended, and the
  existing Gingerbread fallback on Android 2.x. No additional engine download.
- Portable profile field, analyzer v14 environment refresh, preservation of
  shared-system container IDs and English/Russian controls.
- [Implementation scope and pinned Android/kernel references](universal-extended.md).
  No SENSES integration or new kernel-driver emulation is claimed.

## Build and verification

JDK 21, SDK/build-tools 36, Gradle 8.14.2. Offline/no-daemon, in-process Kotlin,
one worker, `-Xmx1024m`. Tasks: `testCloneReleaseUnitTest assembleCloneRelease
assembleStandardRelease`. Final build: successful in 4m 2s.

205 JVM tests, zero failures/errors/skips, including 13 new Extended tests.
Source hashes for all 16 changed/new implementation and build-document files
match the remote final inputs. `git diff --check` passes.

Both APKs verified with `apksigner`; signer SHA-256:
`b59066f04ec7633ff85234910b1173acd725afb99fc15801ecc82ce157aeb311`.
Package IDs, version code/name, compiled Extended markers, packaged notices,
all tracked host engine binaries and guest engine assets verified. Native
components are unchanged; no native rebuild was needed. Common shim SHA-256:
`6962d85b5f8d7b8e934071d4a801cca3ab6015992f4a4744eea1eb8b14f95439`.

APK SHA-256:

- Standard: `26fc4ce22718f2a44dadad338be371a8f7a31e4bf702ef4fadb43e61679adb59`
- Clone: `d6c33d92f3437d13f8b163d412f21d609a8a245786a2ad17f94b287f6b71fdc5`

No on-device boot, graphics or hardware compatibility validation was performed
for this release. JVM tests and compilation do not establish that additional
ROMs now boot. Test Extended in a separate VM copy. Full corresponding-source
availability for inherited engine binaries remains unverified; see
[license-audit.md](license-audit.md).
