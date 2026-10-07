# Sunset.32 — property completion / first-boot investigation

Date: 2026-10-07. Version code: 34. Packages: `app.aemu`, `app.aemu.clone`.

Both supplied CM11 and Xperia ZR logs show an initial null dereference in
`InputMethodManagerService.resetDefaultImeLocked`, then a fatal system_server
exception and zygote exit. This is not evidence that the background memory
killer directly killed zygote.

The pinned Android 4.4.2 bionic property client returns success after 250ms even
without a completion ACK. SettingsProvider increments `sys.settings_*_version`
through that client; Settings caches null entries until the version changes.
An emulator-side processing delay can therefore preserve a stale keyboard
settings result through first-boot IME selection. This is a source-supported
failure mechanism, not yet a confirmed explanation of the supplied device logs.

Sunset's guest shim now sends the same legacy property frame but waits for
the host to finish and close the connection. A five-second timeout returns an
error, not false success. It handles interrupted calls, short sends, null values,
length validation and descriptor cleanup. The clock deadline wraps safely on
32-bit ARM. No firmware APK, settings database or default keyboard is rewritten.
This affects property writes generally, so it is not Sony- or CM-specific.

Regression testing includes real ARM Unix-socket transport with a service
delaying its ACK 700ms, plus null-value, invalid-length, unavailable-service and
five-second timeout cases. Existing shim and JVM tests are also run.

## Device verification still required

Use a newly imported VM or reset the data of a **disposable test copy**, then
boot once without restarting. Test both the unmodified Xperia firmware and CM11.
Existing data may hide first-boot failures. Preserve your working VM and data.
Check keyboard input, subsequent boots and property-driven service controls.
Do not mark the catalog build as verified or enable first-launch downloads
until actual fresh-VM testing succeeds.

The CM ROM package is unchanged by this release. AESS442-2 still includes its
separate ROM-side fallback; use unmodified Xperia to test the shared mitigation.

Build/source and inherited engine provenance limitations remain documented in
[build-source.md](build-source.md) and [license-audit.md](license-audit.md).

## Verified artifacts

Six ARM smoke tests passed (including the 700ms ACK / five-second timeout
property test). 137 JVM tests passed with zero failures, errors or skips.
Both APKs have version code 34, the expected package IDs, verified v2 signatures
and the existing publisher certificate. Packaged legal notices and guest shim
match the build checkout; the other rebuilt shim assets are unchanged.

SHA-256:

- Standard: `3c67f013bcd33d65c7692e9838107901383585e5560cdc6e42e43cb232ce9743`
- Clone: `6cf20e10770a6659afc658c2640308eea023cafa0a0ad3557240f48e75c6f5d3`
- Guest shim: `6962d85b5f8d7b8e934071d4a801cca3ab6015992f4a4744eea1eb8b14f95439`
