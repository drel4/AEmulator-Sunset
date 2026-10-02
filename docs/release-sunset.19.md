# AEmulator Sunset 0.0.0.3-sunset.19

## Reboot fix

The old restart path used an ordinary RTC alarm to relaunch the VM 400 ms later,
then removed its task and killed the VM process. Ordinary alarms can be delayed,
and a launch racing process exit can reach the old singleTask activity. This
could leave the guest stopped rather than restarting promptly.

- Replaced that alarm with a private restart activity in the main app process.
  The VM launches it while foreground, finishes only its own activity and exits.
- The restart activity waits for Binder death of the exact old VM process before
  opening the selected firmware in a fresh VM process. No PID guessing or
  exact-alarm permission is required.
- Relaunch happens while the restart activity is resumed. If the user backgrounds
  the app during the handoff, it continues when the app returns to the foreground.
- Ordinary Reboot preserves the current normal/recovery mode; explicit Reboot
  to recovery/system uses the requested target. Switching ROMs also keeps the
  requested target when a fresh VM process is needed.
- Repeated shutdown/reboot requests cannot start competing teardown workers.
- A timeout shows a translated error rather than silently claiming success.
  Restart/progress messages are supplied in English and Russian.

No guest/native binary changes. Firmware and guest data are not replaced.

## Verification

Both signed APK variants built on the Linux server. All 55 JVM tests passed,
including four new process-exit gate tests covering delayed death, already-dead
processes, registration/notification races and cancellation. APK signatures,
versions, packaged legal documents and unchanged native/helper hashes were
verified. Downloaded APK hashes match the server's build outputs.
On-device normal reboot, recovery transitions and ROM switching still need testing.

Platform references:
[AlarmManager](https://developer.android.com/reference/android/app/AlarmManager#set(int,%20long,%20android.app.PendingIntent)),
[Binder process death](https://developer.android.com/reference/android/os/IBinder#linkToDeath(android.os.IBinder.DeathRecipient,%20int)).

## Source and notices

[Exact application source](https://github.com/drel4/AEmulator-Sunset/tree/v0.0.0.3-sunset.19).
[Build instructions](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.19/docs/build-source.md).
[Source/licensing audit](https://github.com/drel4/AEmulator-Sunset/blob/v0.0.0.3-sunset.19/docs/license-audit.md).

Dated notices are updated and packaged. The inherited engine source/provenance
gap remains unresolved; complete Corresponding Source is not certified.

Host: Android 8.0+ (API 26), arm64-v8a. Version code: 21.

SHA-256:
- Clone (`app.aemu.clone`): `47284cab9fb5ed92facc20548cbbe03e527a51cdf8e35df84a59af24b408fdba`
- Standard (`app.aemu`): `a45877618ae26258b119f4915c428fe00ee495ced61a1bd72b0d1900194a5642`
