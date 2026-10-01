# AEmulator Sunset 0.0.0.3-sunset.11

**Rollback of the sunset.10 Keyguard workaround.** The user reported Recents
now displays, but SystemUI and launcher freeze after some time. The logs show
invalid Parcel replies during permission checks and SystemUI input ANRs.

Restores the exact sunset.9 guest-shim source and binary, including its
original Binder behavior. Removes the added Keyguard callback queueing,
mixed-batch handling and pre-blocking replay. This is a stability rollback,
**not a new Recents fix**: the original invisible/black Recents issue may return.

Keeps the user-confirmed Sony setup-completion and vibration changes, Russian
controls, customizable navbar/trackball and hidden-menu option. No firmware
APKs/ODEX files or guest user data are intentionally changed by this rollback.

Camera development remains separate. Galaxy S5 behavior is unverified.
Host: Android 8.0+ (API 26), arm64-v8a. Version code: 13.

Install the same package variant over the current build, then fully stop and
restart the VM. Test normal launcher/navigation stability first; if freezing
continues, attach both host and logcat logs.

Validation: 16 JVM tests, 5 ARM guest-shim smoke tests and both audio ABI
smoke tests passed. Both release APK signatures, package versions and embedded
native-library hashes were checked. Long-running phone stability is not yet
confirmed.

SHA-256:
- Clone (`app.aemu.clone`): `fde5fffb4b5dc90677a3904674960de516aa60335a0f57f171082840c8ce66fa`
- Standard (`app.aemu`): `a75e51dac5f97729d5c1ce132baa7e313650ddc7a47505a142d15f8b5ee01279`
