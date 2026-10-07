# AEmulator Sunset contributor / agent guide

Applies to this entire checkout. Read any more-specific `AGENTS.md` before
editing its directory. These instructions do not grant access to a maintainer's
accounts, signing keys, build server or publishing credentials.

## Project intent

- Sunset is a GPL-3.0 fork of AEmulator focused on classic ARM Android firmware,
  especially Sony Xperia ZR and Samsung Galaxy S5 compatibility.
- Prefer emulator-side bridges and compatibility fixes that work with
  **unmodified firmware**. Do not silently patch vendor APKs or replace a user's
  ROM. The separately built CM11 guest has its own explicitly documented patches.
- The host app runs on arm64 Android, API 26+. Guests are 32-bit ARM; this is
  userspace emulation using the host kernel, not a full hardware/kernel VM.
- Do not confuse a guest boot-completed property with verified working UI.
  First-boot tests require fresh guest data; preserve working VMs.

## Where to work

- `app/src/main/java/app/aemu/ui/`: Compose screens and host navigation.
- `app/src/main/java/app/aemu/core/`: VM lifecycle, settings, storage, archives,
  property service and hardware bridges.
- `app/src/main/java/app/aemu/importer/`: firmware detection/extraction and
  guest profile analysis. Preserve user metadata when refreshing a profile.
- `app/src/main/res/values/strings.xml`: English UI; `values-ru/strings.xml`:
  Russian. Add both for Sunset features. Keep public descriptions short and
  user-facing; use docs for implementation details.
- `native/`: source-backed guest shims, HALs and helpers. APK builds **do not
  automatically rebuild native assets**. Rebuild changed components before
  packaging, and include the resulting tracked asset with its source change.
- `rom/cm11/`: pinned CM11 guest recipe, preparation tests and packaging tools.
  This is independent of the APK build; see its README and build records.
- `docs/build-source.md`, `docs/release-sunset.*.md`, `NOTICE.md`: build,
  verification, release and dated modification records.

## Build and tests

Use `docs/build-source.md` as the authoritative toolchain/build guide. Tested
app environment: Linux, JDK 21, Android SDK/build-tools 36, Gradle wrapper 8.14.2;
Java/Kotlin target 17. Set environment paths for your own machine; do not copy
maintainer-specific paths into new tooling. If a private `linuxserver.txt` is
provided locally, inspect it before using that server, and never commit it.

```sh
sh gradlew --no-daemon '-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8' \
  -Pkotlin.compiler.execution.strategy=in-process --max-workers=1 \
  testCloneReleaseUnitTest assembleCloneRelease assembleStandardRelease
```

- First builds need dependency downloads. Use `--offline` only with a populated
  cache. Do not download whole ROM source trees onto a space-limited app workspace.
- Guest-native builds use NDK `22.1.7171670`; follow each component's build script.
  Guest shim tests: `(cd native/guestshim && sh tests/run.sh)` with `NDK` set
  and `qemu-arm` available. Use relevant audio/camera/sensor/helper tests too.
- Add regression tests for parser, archive, lifecycle and compatibility changes.
  Report exact results, and distinguish host/JVM tests from on-device validation.
- Before handing off, inspect the diff and run `git diff --check`. Do not claim
  a crash is fixed solely because compilation or a smoke test passed.

## Storage, lifecycle and diagnostics

- Preserve unrelated changes and user ROMs, backups, source trees and VM data.
  Data reset is explicitly destructive; export/import must preserve archive
  path, symlink, bounds and storage-lease checks.
- `.aessvm` contains portable VM metadata and selected partitions, never host
  paths, runtime sockets or inode-keyed ownership tables. Keep old archives
  readable and test optional metadata defaults and round trips.
- A failed boot, retry, recovery boot or charging-only boot must not consume a
  first-success-only notice. Use a monotonic clock for stability intervals.
- Inspect both host and guest logcat logs. Separate primary errors from service
  death cascades. Exit 137 means SIGKILL, **not proof of an OOM kill**.
- Guest property writes use the host property service; preserve completion
  ordering and read-after-write visibility. See the Sunset.32 investigation.
- Keep host VM windows fixed; guest Android handles its own display rotation.
  Respect host auto-rotate and existing tablet/navbar settings.

## Releases and licensing

- Package IDs: standard `app.aemu`, clone `app.aemu.clone`. Updates must select
  the installed flavor; publish both when a release is requested/authorized.
- Increase `versionCode` monotonically and set the Sunset version name in
  `app/build.gradle.kts`. Never rewrite existing public release tags.
- Maintainer workflow requests publishing new APK builds. This is not permission
  for contributors to publish through someone else's account. Use existing
  authorized credentials only; otherwise hand off artifacts and instructions.
- Verify JVM/native results, package/version IDs, APK signatures and signer
  continuity, packaged legal assets and changed native binaries. Record SHA-256
  hashes. Tag the exact source/assets used; upload both APKs, verify uploaded
  hashes, then publish. Do not distribute private keystores or credentials.
- Preserve upstream history, credits, copyright and license notices. Record
  dated fork modifications in `NOTICE.md`; keep build/source docs accurate.
- Read `docs/license-audit.md`. Inherited engine prebuilts still lack verified
  complete matching source. Do not claim full GPL compliance or complete
  Corresponding Source merely because a tagged APK checkout is available.
- Android/CM sources and user firmware have their own licenses. Do not describe
  the entire CM source tree as GPL-3.0 or redistribute vendor firmware without
  considering its terms.

## Catalog and current verification context

- Default catalog: `https://dumpster.ralsei.tech/drel/AESSRomCatalog/rom.list`;
  users can configure another URL. Preserve the semicolon format, existing
  entries and MOTD when changing a catalog. Back up the deployed list before
  editing it. Remote changes require explicit scope/authorization.
- Catalog status: `-1` unknown, `0` does not boot, `1` boots with issues,
  `2` works. Statuses are supplied by catalog authors, not checked by the app.
- As of 2026-10-07 the user reports CM11 boots on the first attempt. UI sounds
  were reported missing except volume-key feedback. The codec XML is present in
  `/system/etc`, but CM11 reads it through a missing init-created `/etc` alias.
  Sunset.33 and AESS442-3 restore that alias, with playback testing pending.
  Do not infer that Xperia's first-boot issue or every CM11 feature is verified.
- Build/source recipes and release records are authoritative for artifacts;
  check current Git state and logs instead of treating this dated context as
  timeless. Update this section when new evidence supersedes it.
