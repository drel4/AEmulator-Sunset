# Sunset.37 — 2026-10-07

## Changes

- The translucent in-VM ROM-note panel now explicitly supplies the theme's
  `onSurface` content color instead of relying on inherited foreground defaults.
- The compact/minimized boot-loading indicator uses the same explicit foreground.
  Both labels and note bodies inherit it from their Surface.
- The library ROM-card note explicitly uses `onSecondaryContainer` against its
  `secondaryContainer` background, preserving the theme's paired colors.

These changes apply to fixed and dynamic theme palettes. They do not change ROM
notes, boot timing, guest rendering or VM data. No new strings or native assets.

## Verification

Version `0.0.0.3-sunset.37`, code 39. Linux/JDK 21, SDK/build-tools 36,
Gradle 8.14.2, one worker, 1024 MiB heap and in-process Kotlin compilation.
192 JVM tests passed with zero failures, errors or skips. Both release APKs built
successfully in 4m 41s. Package IDs, version code 39, existing publisher v2
signatures, legal assets and the unchanged common guest shim were verified.
All five changed application/build inputs match the remote build. No native
components were changed or rebuilt.

SHA-256:

- Standard: `ea9b00f8837a2530dea578a98a28c8b8d78123de8f425b370e398dde8326abac`
- Clone: `7e55b8f317f5cfabdb23c6f1a02b2804e3eb09de3cd4a110e5329fba6606955a`

Source checks verify explicit foreground roles on both translucent guest panels
and the library note. These checks and JVM tests are not rendered screenshot
tests; on-device readability confirmation remains pending.

Source: `v0.0.0.3-sunset.37`. See [build instructions](build-source.md) and
[inherited engine-source limitations](license-audit.md).
