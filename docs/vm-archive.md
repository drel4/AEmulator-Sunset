# VM archives (.aessvm), versions 1 and 2

Sunset.29 exports version 2 and still imports version 1. Choose **System and
boot** (or **System** if no boot files are present), **Settings**, and/or
**Data**. System and boot cannot be selected separately. Settings-only and
data-only archives cannot create a new VM. This release does not add a separate
data-only restore into an existing VM. Unselected settings restore to defaults.

Version 2 starts with `aessvm.version` (`2\n`), `image.json` and `aessvm.parts`
(three ASCII bits, system/config/data, followed by a newline). ROM metadata is
retained even without settings. Firmware payloads require system selection;
data payloads require data selection. New formats are not readable by old builds.

**Import settings** reads only the bounded leading metadata and changes the
settings sheet, not partitions. Press Save to apply. Device, Android release,
skin and ROM build fingerprint are compared with the current VM. Different
identities, missing fingerprints and old archives require confirmation.

**Import boot partition** is shown only when both the original boot image and
extracted `init.rc` are absent. Stop all VMs first. Supported inputs include
Android boot IMG, Samsung zImage, raw TWRP `boot.emmc.win`, ZIP/7z/TAR containers
and supported gzip/XZ/bzip2/LZ4 compression. Encrypted backups, arbitrary split
backups, bootloader-only images and modern boot header formats are not promised.
Only boot/ramdisk files are added; existing files and system/data are preserved.
The boot plan and property template are regenerated; failures roll back newly
created files. The guest kernel is retained for export, not booted as a host
kernel. Files are bounded and ramdisk paths/links are validated.

## Original version 1 behavior

Added in sunset.25. Export from VM settings, then choose a destination through
Android's document picker. Fully stop all VMs first; storage leases and orphan
writer checks prevent an inconsistent export. Data inclusion is off initially.
Current settings in the sheet are exported without changing the saved VM.

Restore with **Import firmware**. Keep the `.aessvm` extension. Restoration
creates a new independent VM with a new ID, preserving the exported settings
rather than applying application defaults. The existing VM is not replaced.

The file is a gzip-compressed POSIX tar stream. Its first entry is
`aessvm.version` containing `1\n`; `image.json` contains the GuestImage profile
and settings, plus the informational `aessvmIncludesData` flag. `root/system/`
is materialized even for older containers sharing another VM's system.
Other persistent guest root/ramdisk files are included. `root/data/` is included
only when requested. Optional `props.base` and `boot.img` are included if stored.
New firmware imports retain recognized boot images; older imports cannot
recover the original kernel image and export their extracted boot files instead.

Tar modes, relative guest symlinks and guest UID/GID values are preserved.
Ownership is remapped to newly created host inodes when restored; raw
`dhd.owners` keys are never reused. Runtime `dev`, `proc`, `sys`, `cache`, seeded
ownership stamps, host-engine links, working directories, snapshots, recovery
and the shared SD-card directory are excluded. Host engine links are rebuilt
by the normal VM startup path. Guest internal storage under `/data` is included
when data is selected. Exported archives are **not encrypted** and may contain
passwords, tokens, account information and other private files.

Import rejects traversal, duplicate paths, escaping symlinks, runtime entries
and special devices. Links are deferred until all file bytes are extracted;
failed imports remove only their newly created VM. Exports stream directly to
the document provider; failures attempt to remove the partial document. If the
provider cannot delete it, remove the partial file manually. Keep the export
screen open until completion. Phone/provider testing is still required.
