# AEmulator Sunset 0.0.0.3-sunset.8

Includes sunset.7's audio compatibility fix. The user confirms that Xperia ZR
reaches its launcher and vibration works.

- Forward legacy guest `vibrator_exists/on/off` calls to the host motor.
- Per-VM **Guest vibration** toggle, enabled by default.
- Timed pulses and cancellation; 60-second maximum per pulse.
- Cancel vibration on VM stop and boot failure; bounded socket waits.
- No stock firmware library patches for this feature. ROMs using private
  drivers or newer vibrator HALs are not automatically supported.

Eleven JVM tests and all guest-shim/audio ABI smoke tests passed. Both release
APK signatures, versions and embedded native assets were verified.

Setup wizard remains unresolved in this build; camera support is not implemented.
Russian translations and hidden-menu/Back-gesture mode are upcoming changes,
not included in these APKs. Galaxy S5 remains unverified.

Host: Android 8.0+ (API 26), arm64-v8a. Version code: 10.
Clone package `app.aemu.clone` can coexist with `app.aemu`.

SHA-256:
- Clone: `20259c0028a40adc2593f935fcd9117b817aefc529c741dd8d078a3c41a087f8`
- Standard: `ef4e79dbf668049e111631d7855b476c2be1a4e95175d1b815773235321a80fb`
