# CM11 AESS442-1 — experimental

Built 2026-10-04: CM11, Android 4.4.2/API 19, ARMv7, userdebug.
Compilation completed with exit code 0. Packaging validated 1,548 archive entries,
including all payload reads, required apps/libraries, and boot ramdisk placement.
On-device import, boot, root access and hardware bridge behavior are **not tested**.

Package: `Android442forAESS.aessvm`, 202,570,326 bytes (~193 MiB).
SHA-256: `d44241e5bcb4bc2bf72184242fa75b4ce26839819134a3bbd1d5e4309815d6bb`.

Includes system, init ramdisk, settings, CM File Manager, Terminal and CM root
support. No user data or Google Apps. The boot container contains no kernel and
must not be flashed to physical devices. The first-launch download prompt is
not enabled while boot testing is pending.

Public directory: `https://dumpster.ralsei.tech/drel/AESSRomCatalog/`.
Matching sources: `Android442forAESS-source.tar.gz`; see accompanying checksum
file `Android442forAESS.sha256`. Upstream Android/CM licenses remain applicable;
the complete ROM is not uniformly GPLv3.

Catalog entry: status **-1 (Unknown)** in **AEmulator Sunset builds**.
Existing catalog entries and their statuses are unchanged.
