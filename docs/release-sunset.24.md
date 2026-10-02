# AEmulator Sunset sunset.24

- Removed the LPM-only **Repair charging files from boot.img** button and
  file picker from VM settings, including English/Russian messages.
- Charging-only long-press Start, normal recovery installation and permanent
  no-backup data reset remain unchanged.
- This does not add automatic repair for older imports which omitted `/charger`.
  A compatible import containing the firmware's charging executable is still
  needed. Native charging display compatibility still requires phone testing.

## Verification

Both standard and clone APKs are built and signature/package/version/legal
asset/native-helper checks performed before upload. No connected Android
device; no on-phone UI validation is claimed.
Both release variants built successfully on Linux. **81 JVM tests passed**
with zero failures/errors/skips, including the private Xperia boot-image fixture.
APK signatures, package/version identity, bundled legal documents and unchanged
native-helper hashes verified.

## Source and license

- [Exact application source](https://github.com/drel4/AEmulator-Sunset/tree/v0.0.0.3-sunset.24)
- [Build instructions](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.24/docs/build-source.md)
- [Modification notice](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.24/NOTICE.md)
- [Source/licensing audit](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.24/docs/license-audit.md)

GPL-3.0 fork changes and upstream attribution retained. Inherited engine
prebuilts still lack verified matching source/build provenance; full
Corresponding Source or GPL compliance is not claimed.

## APK SHA-256

- Standard (`app.aemu`): `f17a09fbfe587953f8429e91eedaef94e5314cf7a20c45d355e5a0798fd78f96`
- Clone (`app.aemu.clone`): `a933ed8779ce6d5e72dbdbbe6fc133172a9e153ebeb7e2370826b02ff6624940`
