# CM11 Android 4.4.2 for AEmulator Sunset

**Work in progress: compilation started; no completed or boot-tested image yet.**
Added 2026-10-04.

This is an unofficial ARMv7 CM11 guest, not firmware for a physical phone.
It targets AEmulator's userspace execution model and uses the host kernel.
The build includes CM File Manager and the CM `userdebug` managed root support.
Terminal is CM's upstream Jackpal Android Terminal Emulator 1.0.70, with both
matching ARM native libraries. No Google Apps or proprietary device blobs.
Retired CM account and physical-device update services are omitted.

Sources use release `cm-11.0-XNPH25R-bacon-d22b777afa`, with CM project commits
resolved from that tag and AOSP projects pinned to `android-4.4.2_r2`.
The unavailable CM SVOX fork is replaced with public AOSP SVOX from that release
(commit `838228cc17b5798e51bc20d06e54dbd781e441db`). Commercial `vendor/cyngn`
extras, Mac binaries and non-default IDE projects are excluded. The exact
426-project checkout is recorded in `manifest-resolved.xml`.

Build project: `/home/nyash/0drel/cm11-aess`. Sources and downloads remain there,
separate from the Sunset app build and other ROM projects. Run `prepare.py`
against that directory before `build-container.sh` inside a build container.
The latter expects source at `/src`, tools at `/tools`, and this recipe mounted
at `/recipe`. Base container currently available on the server:
`sm-t285-cm11-build@sha256:0138c2e800e4631087b9f66bee5d2aa4935335c352182e06cb16408291236e37`.
It contains Ubuntu 20.04, Python 2 and multilib build dependencies; tools use
Zulu OpenJDK 7.0.352. A 4096-file descriptor limit avoids JDK7 initialization
failure under modern Docker defaults. Do not inject JVM option banner variables:
the legacy version checker reads only the first output line. OpenJDK and GNU
make 4.2.1 produce legacy compatibility warnings. Further old-build-system
adaptations may be required.
`prepare.py` also gates CM minui's generated kernel-header dependency when
`TARGET_NO_KERNEL=true`; generic userspace uses the platform Linux headers.
The framework overlay enables the navigation bar only. Rotation uses CM11's
existing sensor/policy defaults; this snapshot has no `config_supportAutoRotation`
resource (removed from our overlay on 2026-10-04).

Downloaded inputs:

| Input | SHA-256 |
| --- | --- |
| Zulu `zulu7.56.0.11-ca-jdk7.0.352-linux_x64.tar.gz` | `8a7387c1ed151474301b6553c6046f865dc6c1e1890bcf106acc2780c55727c8` |
| `https://jackpal.github.io/Android-Terminal-Emulator/downloads/Term.apk` | `4cbf6adb273a6afa01f7d5f4ea97ac22b5a97ce1a34c000f2ef308a6383e8821` |

Preserve upstream licenses and generated system notices, plus resolved source
manifest and every source patch, with distributed images. Terminal's upstream
source tag is `v1.0.70` (`f5ecc5a63145ddb2b6797922dc241a7fea6cd6da`), Apache 2.0.
Do not describe the heterogeneous Android/CM sources as uniformly GPLv3.
This obsolete, rooted system is intended for trusted testing, not a secure daily OS.

Do not publish the first-start ROM download prompt until an image has been tested
in AEmulator. Planned distribution name remains `Android442forAESS.aessvm`.
