# Sunset.31

- Compact `CM11`/`CM7.2` card labels without changing stored ROM identity.
- Card badges wrap onto another row instead of crushing the size badge.
- Single-line badge text for narrow screens and large fonts.
- No library-header gaps from hidden storage, firmware-folder or import cards.

App sources: tag `v0.0.0.3-sunset.31`. Dated modifications in `NOTICE.md`.
The APK does not include a rebuilt CM11 ROM or a zygote crash fix.
On-device layout verification remains necessary.

Verification: 137 JVM tests passed, zero failures/errors/skips. Standard and
clone APKs use version code 33 and the existing publisher signing certificate;
APK v2 signatures and packaged legal assets verified.

SHA-256:

- Standard: `3a4ff99d8d8ab5886b1a873d52f198c45d662054ac146191ef5ea94b76a8594f`
- Clone: `9e15049a51faed0ce49d5c5ff657f7ff43379dfbcc2aaf0ac657abb070fb2b4f`
