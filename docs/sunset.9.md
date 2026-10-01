# sunset.9 — Sony setup-flow trial, menu access, Russian controls

## Sony setup diagnosis and changes

The latest log shows Initial-boot-setup's locale selector returning to the
launcher, without a fatal Java exception at that transition. Stock bytecode
shows a separate full setup wizard and an ACTION_FINISHED receiver that needs a
non-empty SIM serial number before unregistering the repeat-setup listener.

- Existing emulator code disabled the full Sony wizard and forced
  `device_provisioned`/`user_setup_complete` to 1. For the verified two-stage
  firmware layout, leave these values and the Sony wizard to the firmware.
- Before guest processes start, migrate the old emulator's Sony package disable
  once, if the real wizard's `setup_wizard_has_run` preference is not true.
  Restore its package's default enabled state and reset the two completion flags.
  Keep copies of package restrictions and the settings database/WAL/SHM next to
  the originals (`.aemu-before-sony-setup-v1`). Preserve component restrictions
  and already-completed setups. Other firmware's compatibility policy is unchanged.
- The existing fake RIL now serves the synthetic SIM's read-only EF_ICCID:
  transparent-file metadata and checked READ_BINARY offsets. No real host SIM,
  carrier credentials, SIM authentication, or modem access is used. Unsupported
  files/commands still fail. This is not a complete telephony implementation.

The transport/header follow the legacy
[RIL implementation](https://raw.githubusercontent.com/LineageOS/android_frameworks_opt_telephony/cm-11.0/src/java/com/android/internal/telephony/RIL.java)
and [IccFileHandler](https://raw.githubusercontent.com/LineageOS/android_frameworks_opt_telephony/cm-11.0/src/java/com/android/internal/telephony/uicc/IccFileHandler.java).
Stock APKs/ODEX are not patched. Completion and reboot persistence need an
on-phone test; this release is a compatibility trial, not a verified setup fix.

## Menu and localization

- **Hide menu button** is off by default. When enabled, hide the floating …
  button and route the host Back button/gesture to the emulator menu. When off,
  host Back reaches guest Android as before. The guest navbar's Back button
  always reaches the guest, including in hidden-menu mode.
- Russian translations cover vibration, customizable navbar, trackball,
  sensitivity, and hidden-menu settings.

## Pending

Wallpapers were observed working again; no wallpaper changes were made.
Camera support is being developed separately and is not advertised in this build.
Samsung Galaxy S5 and setup completion remain unverified.
