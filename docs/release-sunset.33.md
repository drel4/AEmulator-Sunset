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

Separate ROM recipe: AESS442-3 adds the missing media-codec XML that logs identify
as preventing SoundPool from decoding Ogg UI sounds. Existing app updates cannot
add that file to an already imported CM11 system. Import the new ROM as a new VM
to test it; preserve working data. CM11's first-attempt boot is user-reported;
playback and note overlay still require on-device verification.

Standard `app.aemu`, clone `app.aemu.clone`; version code 35.
Sources: tag `v0.0.0.3-sunset.33`. See build-source.md and license-audit.md for
toolchains and inherited source-provenance limitations.

Verification: 145 JVM tests passed (zero failures/errors/skips), all six ARM
guest-shim smoke tests passed, and both CM11 recipe fixture tests passed.
Both APKs built successfully, have version code 35 and the existing publisher
certificate, with v2 signatures verified. Packaged legal assets and guest shim
match the build checkout. On-device overlay and sound playback testing remains
pending.

SHA-256:

- Standard: `ce9191a658c92269cb51ad71e1ccd6f27152c688de1eff33221c039e6e468553`
- Clone: `ac594c3ea38e2f8c356ca4a33136d8f605140e21efd0cf3e65af5a53e4642e85`
