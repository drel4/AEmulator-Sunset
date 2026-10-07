# AEmulator Sunset modification notice

**AEmulator Sunset** is a modified fork of
[uxazu/AEmulator](https://github.com/uxazu/AEmulator). It remains licensed
under the GNU General Public License, version 3; see [LICENSE](LICENSE).

The upstream project grew from the HTC Desire HD and HTC One M7 emulators by
the [original engine author](https://t.me/istratiit_ech). Those credits and the
upstream Git history are intentionally preserved.

## Fork changes

**Modification notice updated 2026-10-07 for sunset.33.** This work has been
modified by drel4 and contributors to AEmulator Sunset. Original copyright,
license notices, credits and Git history are retained; no upstream authorship
is claimed for Sunset's additions.

Changes made by drel4 and contributors to this fork are recorded in Git.
sunset.33 adds optional export notes retained until a reported stable boot,
the add-firmware catalog shortcut, English/Russian wording changes and an
AGENTS.md contributor guide. The separate CM11 recipe now packages its pinned
software media-codec configuration and init-created /etc alias for Ogg UI
sounds. The emulator restores missing /etc aliases in existing VMs too.
Dated 2026-10-07.
sunset.32 waits for the host's legacy property-service completion ACK instead
of treating a 250ms timeout as success. It includes delayed-service and timeout
ARM smoke tests. This emulator-side first-boot crash mitigation is dated
2026-10-07; fresh-ROM boot verification remains pending.
sunset.31 library cards wrap badges with single-line labels, abbreviate
CyanogenMod labels for display only, and omit spacing for hidden header cards.
These UI changes are dated 2026-10-04.

The initial AEmulator Sunset release was prepared on 2026-09-30 and includes:

- AEmulator Sunset naming, repository links, versioning, and orange visual
  identity;
- removal of the upstream project's donation promotion;
- Android 4/Holo-inspired guest navigation controls;
- update checks redirected to the fork's own GitHub releases;
- an explicit focus on Sony Xperia ZR and international Samsung Galaxy S5 ROM
  compatibility.

Further modifications dated 2026-10-01 (sunset.6 through sunset.16):

- Sony/legacy Android service, network and setup-flow compatibility changes;
- configurable Holo navigation and native relative-motion trackball input;
- Sony KitKat audio ABI support and a guest-to-host vibration bridge;
- hidden-menu/back-gesture option and Russian translations;
- a Keyguard scheduling trial in sunset.10, rolled back in sunset.11;
- an experimental host camera HAL1 bridge and reversible legacy HAL discovery;
- opt-in, per-ROM setup skipping with reversible package enabled-state records;
- a configurable ROM catalog, compact section index and browser download links;
- persistent 42-tap experimental-feature unlocking.

Modifications dated 2026-10-02 (sunset.17):

- navbar key-down/key-up holding, including guest Home long-press actions;
- a Minimize menu action returning to the library without stopping the guest;
- updated modification notices, packaged license/notices and build/source audit.

Further modifications dated 2026-10-02 (sunset.18):

- Minimize grouped with reboot/shutdown controls at the bottom of the menu;
- host Back closes logs before any guest Back or hidden-menu action;
- live cross-process VM status changes active guests' Start button to Open,
  including preparing/booting guests, with stale-status protection on process exit.

Further modifications dated 2026-10-02 (sunset.19):

- replaced alarm-based VM relaunch with a foreground main-process restart
  activity that waits for the old VM process's Binder death before booting;
- retained normal/recovery boot targets and guarded duplicate stop/reboot requests.

Further modifications dated 2026-10-02 (sunset.20):

- exposed Sony's default automatic camera mode shell in the host HAL to avoid
  a missing-mode settings crash; proprietary scene detection remains unavailable;
- added native capability/round-trip regression checks and documented Sony's
  app-owned capability cache and device-testing limits.

Further modifications dated 2026-10-02 (sunset.21):

- added bounded YAFFS2 recovery-snapshot parsing and CWM system-image import
  from standalone IMG and ZIP/7z/TAR containers, preserving file/link metadata;
- fixed updater APK selection for standard/clone variants and validated the
  downloaded/installed APK package identity;
- added YAFFS2 corruption, path, link, cancellation and variant-selection tests.

Further modifications dated 2026-10-02 (sunset.22):

- added confirmed, recoverable guest-data reset with a process-lifetime storage
  lease preventing concurrent boot/reset, without deleting firmware or shared SD;
- added experimental long-press Start for a temporary low-power refresh/input
  preset, leaving saved settings unchanged; added English/Russian labels and tests.

Further modifications dated 2026-10-02 (sunset.23):

- replaced the misunderstood refresh-rate LPM preset with a separate native
  charging-only boot path; retained stock charger imports and added boot.img
  repair for charging executables discarded by older imports;
- removed the library LPM hint/toast and handled fresh intents to existing VMs;
- changed confirmed data reset to permanent no-backup deletion with link-safe
  traversal and stale inode ownership pruning; updated English/Russian messages.

Further modifications dated 2026-10-02 (sunset.24):

- removed the LPM-only charging boot-image repair button, picker, and its
  English/Russian UI messages; charging-only boot and normal recovery controls
  remain unchanged.

Implementation details and release-specific testing limits are recorded in
`docs/release-sunset.*.md`, `docs/sunset.*.md` and the tagged Git history.

This notice identifies a modified version. It does not imply endorsement by
the upstream maintainers, Sony, Samsung, Google, or any firmware vendor.

## Source and corresponding binaries

Further modifications dated 2026-10-03 (sunset.26): optional host accelerometer,
gyroscope and magnetic-field forwarding through a source-backed ARM32 sensor
HAL; natural-edge navbar layout with upright icons and rounded-square pressed
states; immersive cutout display and opt-in host resolution; Rotate screen in
the hardware-button menu section; optional Samsung boot media service handling.

Further modifications dated 2026-10-03 (sunset.27): fixed host VM orientation,
icon-only physical orientation tracking, untransformed guest display and trackball,
guest WindowManager rotation helper independent of sensor forwarding, app-wide
cutout-barrier and Original/Sunset navigation styles with cross-process refresh,
press-down host haptics and reversible opt-in Google-app disabling inside the guest.

Further modifications dated 2026-10-04 (sunset.29): manual sensor-based rotation
when the host sensor bridge is off; tablet-only host-window/navbar rotation
policy and app setting; removal of the duplicate host-resolution switch;
selectable firmware/settings/data archives, settings import with ROM identity
confirmation, safe boot import for boot-less VMs; and a first-launch advisory
for unrecognized SoC families. New UI strings include Russian translations.

Further modifications dated 2026-10-04 (sunset.30): shorter English/Russian VM
export description; removal of the library toolbar subtitle; simplified firmware
import format summary; simplified ROM catalog compatibility note and removal of
browser-opening descriptions. Export contents, layout and guest behavior are unchanged.

Further modifications dated 2026-10-03 (sunset.28): Host resolution preset,
mutually exclusive host/fixed preset selection and manual-size mode switching.

Further modifications dated 2026-10-03 (sunset.25): portable `.aessvm` export
from VM settings, optional private data inclusion, archive restore through
firmware import, portable file metadata, original boot-image retention for
new imports, and English/Russian export controls. Source is marked in Git.

Source code for AEmulator Sunset releases, including the exact tagged revision
used to build each APK, is published at:

<https://github.com/drel4/AEmulator-Sunset>

The Sunset application and fork-owned modifications are released under GPL-3.0.
This does not relicense independently licensed dependencies or user firmware.
See [build/source instructions](docs/build-source.md) and the
[source/licensing audit](docs/license-audit.md).

**Unresolved source-provenance gap:** this checkout contains inherited prebuilt
engine binaries for which matching source, patches and build instructions have
not been located or verified. The tagged checkout can rebuild the APK using
those prebuilts, but is not established as complete Corresponding Source for
every bundled component. This notice is not a claim of full GPL compliance;
publishing notices or hashes does not remedy missing required source.
