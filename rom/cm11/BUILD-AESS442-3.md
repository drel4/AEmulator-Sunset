# AESS442-3 — 2026-10-07

CM11's first-attempt boot is user-reported. UI sound logs show MediaCodecList
unable to open its codec configuration and SoundPool failing to load Ogg files;
volume-key feedback works because it uses generated tones.

Archive comparison disproved the initial missing-XML hypothesis: AESS442-2 and
the private codec-only candidate both already contain the same 4744-byte
`root/system/etc/media_codecs.xml`. The actual missing path is `root/etc`.
The pinned MediaCodecList opens `/etc/media_codecs.xml`; pinned init.rc normally
creates `/etc -> /system/etc`, but Sunset launches services without running init.

The recipe now explicitly lists the codec XML and packs a portable
`root/etc -> system/etc` symlink. The Vorbis decoder and UI sound files are
verified present. Sunset.33 also creates missing aliases for existing VMs,
preserving vendor directories/custom links and converting only the known
absolute guest `/system/etc` link into a relative tree link. No vendor APK patch
or codec-configuration rewrite is needed for the emulator-side fix.

The original codec-only candidate was **not published**. It is retained privately
for comparison. The final package contains 1549 validated entries, with system,
ramdisk-only boot container and settings; no guest data/runtime files. It includes
an optional one-time informational note, understood by Sunset.33 and ignored by
older apps.

- ROM size: 202,571,307 bytes.
- ROM SHA-256: `7254606fea3d4ffe61aef80c0da20c64146ee2bee5f5084d9b9d0f9f8253ee10`.
- Cumulative source update SHA-256:
  `c13fa5faa38bb88347f694c1ca03a5a38b48dd4505853e3023cc5177ced3ad31`.

Sources remain pinned as documented in README.md and manifest-resolved.xml.
Apply `Android442forAESS-source-update-AESS442-3.tar.gz` over the original full
`Android442forAESS-source.tar.gz`; it includes the recipe, full modified IME
source and device target. The original full source archive is unchanged.
This post-packaging artifact record is tracked in Git, not inside the source
update whose checksum it records.

CM build container exited 0. Both preparation fixture tests pass. Packaging
checks boot headers, the XML's Vorbis declaration and exact pinned bytes,
required applications/libraries, safe paths and the root alias, and reads every
payload back. These checks do **not** confirm on-device sound playback. Test it
and send fresh logs if SoundPool still fails.
