# CM11 Android 4.4.2 for AEmulator Sunset

**Experimental: original build boots after a restart; first-boot fix needs testing.**
The user reports AESS442-1 boots after a restart. Its first boot crashes in
CM11's default-keyboard selection with an empty enabled-IME list. AESS442-2 adds
a guarded fallback to the most applicable discovered keyboard and enables it.
This fix still requires a fresh-data boot test.
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
The container uses `C.UTF-8`, checked at startup, so JDK7's javadoc reads UTF-8
framework sources rather than failing on non-ASCII comments with `LC_ALL=C`.
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

## Packaging

Compile `guest-fs-config.c` against the pinned `system/core/include` headers into
`packaged-AESS442-2/guest-fs-config`, then run `python3 recipe/pack.py /path/to/cm11-aess`.
It packages system files using CM's filesystem ownership/mode rules and root
files using ramdisk CPIO metadata. Absolute guest links become relative links
inside the archive. Settings are included; data and runtime files are omitted.
The initial profile has version zero so Sunset analyzes init scripts before boot.

`boot.img` is an Android v0 ramdisk-only container with **no kernel**. It is for
AEmulator's host-kernel userspace model only, not for flashing a physical device.
Packaging verifies headers, archive contents, paths, required applications and
libraries, and reads every payload back. This does not establish a successful
on-device import or guest boot. Root access is CM's userdebug implementation;
its behavior in the guest still needs testing.

Distribution includes `Android442forAESS-source.tar.gz`: the actual patched
source tree and checked-in prebuilts, recipe, resolved manifest, and terminal
APK. Git/repo metadata and build outputs are excluded. Upstream source licenses
remain in their directories and Android's generated NOTICE is retained in the VM.

For AESS442-2, additionally apply `Android442forAESS-source-update-AESS442-2.tar.gz`
over the original source bundle: it supplies the updated recipe and full modified
InputMethodManagerService source. Rebuild using the documented container, then
package. The original complete source bundle is retained unchanged.
