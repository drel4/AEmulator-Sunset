# CM11 AESS442-2 — first-boot IME fix

Original AESS442-1 reportedly boots after restarting the VM. Its failed first
boot logs show InputMethodManagerService.resetDefaultImeLocked dereferencing a
null default IME; the service fails to initialize. A subsequent focus event
crashes system_server, and zygote exits because system_server died.

The updated recipe falls back to the most applicable discovered keyboard when
no enabled default can be selected, enables it, and guards the log against a
null result. Existing valid default selection is preserved. No blanket zygote
restart or unrelated firmware modification is used.

Compilation: exit 0. Package: 1,548 validated entries, 202,570,669 bytes.
The archived services.jar was compared with build output and its classes.dex
confirmed to contain the new IME guard. **Fresh-data first boot still needs an
on-device test.** No user data is included or changed on existing installations.

ROM: `https://dumpster.ralsei.tech/drel/AESSRomCatalog/Android442forAESS.aessvm`

SHA-256: `08152f593e4db15da7150928aaeb893cb0fa869ece273a26b159c059ea91a486`.

Corresponding sources are the retained full `Android442forAESS-source.tar.gz`
plus `Android442forAESS-source-update-AESS442-2.tar.gz` in the same directory.
Extract the source update over the original bundle, then follow README.md.
The original ROM and its checksums are retained in the catalog's `.hist`.

Sunset.31 APKs fix library badge wrapping/labels and header spacing only. To test
this ROM fix, re-download the aessvm and import it as a **new VM**; an APK update
alone does not replace the existing guest framework. Keep the current working
VM as a backup. Kernel-less boot container: never flash to a physical device.
