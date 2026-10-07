# Sunset.36 — 2026-10-07

## Changes

- Removed the exact built-in CM11 warning from library cards and import results,
  including already imported AESS442-1/2/3 profiles. The filter is limited to model
  `AEmulator Sunset CM11`, API 19, and two known built-in warnings. Other ROMs and
  genuine diagnostic warnings are unchanged. Future CM11 packages omit that
  warning; the currently served archive and pinned checksum remain unchanged.
- **ROM card note (optional)** in the export dialog lets an archive author supply
  boot requirements, required VM settings or other advice. It is portable profile
  metadata, not a diagnostic inferred by the analyzer. Blank means no author note.
  Editing it changes the exported snapshot, not the source VM.
- Imported card notes are shown separately from diagnostic warnings. Cards show
  up to four lines; tapping the note opens its complete text in a scrollable dialog.
  Notes are plain text, bounded to 2,000 characters; controls are English/Russian.
- Card notes are persistent, separate from one-time boot notes. Successful boot
  consumes only the existing one-time note. Profile refresh and boot-partition
  import preserve both note types; clones and archive exports retain card notes.
- Optional `romCardNote` defaults to blank in old archives. It is included even
  when VM settings are excluded from an export. Older app versions can ignore
  the unknown field but cannot display the new card note. Import settings remains
  a settings-only action and does not overwrite author notes in the target VM.

This corrects the previous interpretation: Sunset.35 changed the CM11 welcome
popup, whereas this release addresses the warning on the library ROM card.
The served CM11 image's existing one-time boot note is not rewritten here.
No guest partitions, native assets, data or remote catalog entries are changed.

## Verification

Version `0.0.0.3-sunset.36`, code 38. Linux/JDK 21, SDK/build-tools 36,
Gradle 8.14.2, one worker, 1024 MiB heap and in-process Kotlin compilation.
192 JVM tests passed with zero failures, errors or skips. Both final release
APKs built successfully (final build: 4m 16s). Package IDs, version code 38,
publisher v2 signatures, legal assets and the unchanged common guest shim were
verified. All 15 changed application/build/recipe inputs match the remote build.
No native binaries were changed or rebuilt.

SHA-256:

- Standard: `98e2492f4c2a754574a88deeb8b7699d16824545333d72abf219b77934a7c0f4`
- Clone: `1b5e66078ad6066050e8208796ec9ce112bae2f38a3ce60fccead311a871f316`

On-device dialog layout and a complete export/import display test still need
confirmation; JVM tests do not establish guest boot compatibility.

Source: `v0.0.0.3-sunset.36`. See [build instructions](build-source.md) and
[inherited engine-source limitations](license-audit.md).
