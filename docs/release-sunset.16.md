# AEmulator Sunset 0.0.0.3-sunset.16

## Compact ROM catalog

- Removed the button that opens the raw `rom.list` file.
- Replaced the long ROM download buttons with download icons. They still open
  the default browser, with accessible labels naming the ROM.
- Sections now appear as a compact list with ROM counts and short comments.
  Tap a section to view only its ROMs; toolbar Back or the system back gesture
  returns to the section list. Official ROMs no longer fill the initial screen.
- Empty-section and missing-download messages are retained. Section source
  links and the editable catalog URL remain available.
- New labels and ROM-count plurals are translated into Russian.

No guest/native changes. Setup skipping and the 42-tap experimental unlock
remain as in sunset.15.

## Verification

Both signed APK variants built on the Linux server; 43 existing JVM tests
passed. APK versions/signatures and embedded native/helper assets verified.
On-device catalog navigation/layout still needs the user's test.

Host: Android 8.0+ (API 26), arm64-v8a. Version code: 18.

SHA-256:
- Clone (`app.aemu.clone`): `f72a37617817e83d54445057224ea73d4ae61d4a64c03f92ce608a6a495c3a4d`
- Standard (`app.aemu`): `53c0e1cb0965fb178b44698c34052d7a3e4efbc2118f2a38459a5c2f76fe6a61`
