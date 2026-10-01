# AEmulator Sunset 0.0.0.3-sunset.7

Experimental stock-ROM compatibility prerelease. The user confirms that the
Sony Xperia ZR firmware now reaches its launcher.

- Corrected the emulator audio HAL's CAF direct-track callback layout used by
  Sony KitKat; restored the saved stock audio policy for matching firmware.
- Selection uses the verified library ABI signature, not a blanket brand check.
- Stock audio libraries and apps are not patched by this fix.
- Setup closes after language selection and reappears on reboot; still unresolved
  in this build. Camera support is not implemented. Galaxy S5 remains unverified.

Both release APKs built successfully; seven JVM tests and both ARM audio HAL
ABI tests passed, along with existing guest-shim smoke tests.

Host: Android 8.0+ (API 26), arm64-v8a. Version code: 9.
Clone package `app.aemu.clone` can coexist with `app.aemu`.

SHA-256:
- Clone: `d089f08d36d5b15ebb4f54898a2e3c4460818c96b3ef76b790755a8c59d4ffa3`
- Standard: `16d5cb87a164d2d9148ffa33a8e7a2b3dc258a7a79d69b69f9809f20da69274d`
