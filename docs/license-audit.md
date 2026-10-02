# Source and licensing audit — 2026-10-02

Scope: the Sunset checkout through sunset.23, its build scripts and release
packaging. This is a technical inventory, not a legal compliance certification.

## Checked and improved

- Root `LICENSE` contains GNU GPL version 3. Upstream credits/history remain.
- `NOTICE.md` now prominently identifies the modified work, dates and features.
- New held-key/live-status sources and modified navbar/activity files carry dated notices.
- APK builds package `LICENSE`, `NOTICE.md`, this audit and build instructions
  under `assets/legal/`; Settings exposes the license and modification notices.
- Exact application revisions are identified by release tags. Release notes
  provide adjacent source links and disclose the limitations below.
- Sources/build scripts exist for Sunset's preload/trackball/vibration changes,
  DIRECTTRACK audio, host camera bridge and setup helper. No vendor ROM APKs
  or firmware images are added to the application source distribution.

## Unresolved: inherited prebuilt engine provenance

These tracked `app/src/main/jniLibs/arm64-v8a/` binaries lack matching engine
source/build scripts in this checkout (only `libaemuhost.so` has the
`native/hostjni` implementation):

- `libqemu_gb.so`, `libqemu_kk.so` — patched QEMU engines;
- `libbinderd_gb.so`, `libbinderd_kk.so`;
- `libglserverd_gb.so`, `libglserverd_kk.so`;
- `libdhdrun_gb.so`, `libdhdrun_kk.so`;
- `libdhdslot.so`, `libglbridge.so`.

There are also inherited guest assets without an established exact source
mapping, including `libashmemshim.so`, `libGLES*.so` (other than the source-backed
split wrapper), gralloc, Gingerbread camera, original audio-policy/HAL binaries
and helper executables/JARs. Their individual origins and licenses still need
verification. Source-present is not proof of a binary/source match.

Inspection of the checkout and upstream README did not establish a pinned
repository/version/patch set for those binaries. A generic QEMU download,
upstream credits, a hash list or a tag containing only binaries is not a
substitute for matching modified source and build materials.

Before claiming complete Corresponding Source, obtain and publish/verify:

1. Exact source revisions, local patches, configurations and build scripts for
   each inherited engine binary, starting with the patched QEMU builds.
2. Component-specific copyright/license texts and required third-party notices.
3. A source-to-binary inventory and clean build verification for each component.
4. Notices/license coverage for resolved Gradle dependencies and copied code.
5. Availability of required source next to downloads and for historical releases
   already distributed. New notices do not retroactively complete old sources.

External coordination with upstream/original engine maintainers is needed if
these materials are not already available to the fork maintainer. No contact
has been sent as part of this audit. Existing release tags were not rewritten.

## License scope

Sunset's GPL notice does not assert ownership of upstream work, independently
licensed libraries, Android firmware, trademarks or vendor assets. Preserve
their original notices and verify their distribution terms individually.

GPLv3 §§5–6 address modified-work notices and object-code distribution;
Corresponding Source includes the source and scripts needed to build/install
the covered work, subject to the license's definitions/exceptions:
<https://www.gnu.org/licenses/gpl-3.0.en.html>.

**Status: notices and app build documentation improved; complete bundled-engine
source availability and third-party notice coverage remain unverified.**
