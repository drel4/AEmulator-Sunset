# sunset.8 — legacy guest vibration

Includes the sunset.7 direct-track audio compatibility fix that allowed the
Xperia ZR firmware to reach its launcher.

## Vibration

- The guest shim exports `vibrator_exists`, `vibrator_on`, and `vibrator_off`.
  Requests go through an emulator-owned Unix socket to Android's host vibrator.
  Stock firmware libraries and apps remain unchanged by this feature.
- Guest VibratorService remains responsible for pattern timing and cancellation;
  the bridge forwards individual on/off requests. Each pulse is capped at 60 seconds.
- VM settings have a **Guest vibration** toggle (enabled by default).
  A disabled setting or absent host motor reports no vibrator and never starts a pulse.
- VM stop and boot failure cancel host vibration. Nonblocking socket operations
  and a bounded acknowledgement wait avoid hanging system_server on bridge failure.
- This supports ROMs calling the legacy exported functions, not every vendor's
  private driver or newer vibrator HAL. Amplitude control is not implemented.

## Verification

- ARM/QEMU production-shim smoke test: existence, pulse duration, off, unsigned
  duration transport, split acknowledgements, host error, timeout, and disconnect.
- JVM controller tests: frame validation, 60-second cap, disabled/missing motor,
  zero-duration cancellation, on/off ordering, shutdown suppression, host exceptions.
- Existing guest shim and both audio HAL ABI smoke tests are retained.
- Linux-server build succeeded for clone and standard release variants. All 11
  JVM tests passed; both APK signatures, version code 10, and embedded shim/audio
  assets were verified. Downloaded APK SHA-256:
  - clone: `20259c0028a40adc2593f935fcd9117b817aefc529c741dd8d078a3c41a087f8`
  - standard: `ef4e79dbf668049e111631d7855b476c2be1a4e95175d1b815773235321a80fb`

The user confirmed that physical vibration works on the Xperia ROM. Pulse
cancellation on VM stop and other ROMs still need on-phone coverage.
Setup-wizard completion and telephony remain deferred.
