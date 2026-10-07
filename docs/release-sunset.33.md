# Sunset.33

- Add firmware includes Visit ROM catalog.
- Firmware help includes YAFFS2 system images; data reset text no longer mentions backups.
- Optional plain-text export note (up to 2000 characters), independent of settings
  selection. Restoring an archive retains it; importing settings alone does not
  overwrite a target VM's note. Older archives default to no note.
- The note remains visible until Android reports boot completion and the VM
  stays running at least ten seconds. Crashes reset the stability timer;
  recovery/charging-only boots do not consume it. The watchdog checks zygote and
  SurfaceFlinger liveness before clearing metadata, not merely the boot property.
  This is supervised process stability, not proof of a responsive launcher.
- Profile refresh preserves notes. Atomic in-process metadata updates preserve
  newer settings/notes when background boot bookkeeping completes.
- English/Russian strings and a scrollable export dialog.
- Root AGENTS.md documents contributor/agent workflow without credentials.

Audio: CM11 reads `/etc/media_codecs.xml`, while its existing XML lives under
`/system/etc`. Both prior archives lack the `/etc` symlink normally created by
init.rc. The earlier missing-XML hypothesis was disproved by archive comparison.
Sunset.33 restores missing aliases before boot, preserving vendor directories
and custom links; it converts only the known absolute `/system/etc` link into
a relative guest-tree link. Existing VMs receive this fix without reimporting.
The AESS442-3 recipe explicitly includes the codec XML and portable root alias.
CM11's first-attempt boot is user-reported; playback and the note overlay still
require on-device verification.

Standard `app.aemu`, clone `app.aemu.clone`; version code 35.
Sources: tag `v0.0.0.3-sunset.33`. See build-source.md and license-audit.md for
toolchains and inherited source-provenance limitations.

Final verification: 150 JVM tests passed with zero failures/errors/skips,
including five filesystem-alias tests, plus all six ARM smoke tests and both
CM11 recipe fixture tests. Both APKs built successfully with version code 35,
expected package IDs and the existing publisher certificate; v2 signatures,
packaged legal assets and guest shim verified. On-device playback/note testing
remains pending.

SHA-256:

- Standard: `7e3692394276327270a4ebc169f521cd05c84a3fbec692972b4e80b0076d3008`
- Clone: `a154fdeff16245003877153a710bee351ee5630470c706d347f605433d20bc8a`

The public CM11 package and catalog were updated to AESS442-3. The entry is
under Official (Sunset), status 1 (boots with issues), with user-reported
first-attempt boot and playback verification pending. Previous ROM/catalog
files are preserved in the deployment history. See
`rom/cm11/BUILD-AESS442-3.md` for ROM/source hashes and the corrected diagnosis.
