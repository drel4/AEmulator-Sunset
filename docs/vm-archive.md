# VM archives (.aessvm), version 1

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
