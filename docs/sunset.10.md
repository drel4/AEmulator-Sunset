# Sunset.10: Binder/Keyguard scheduling compatibility trial

The Xperia ZR user confirmed sunset.9 completes setup and remains completed
after reboot. Its stock setup APKs remain unchanged.

## Recents investigation

Recents was invisible/black but clickable. Turning Phone GPU off did not fix
it; the second log confirmed Android PixelFlinger, not the host GL bridge.
Both runs contain repeated Keyguard `CONTROL_KEYGUARD` denials during
`IWindowSession.relayout`. The logged caller PID belongs to `system_server`.
Software mode additionally lacks screenshot framebuffer APIs; it is not a
validated workaround for this problem.

Read-only Ghidra analysis of the emulator showed that binderd caches the UID
from each process's initial hello, and QEMU separately maintains live guest
credentials. A stale UID was considered, but the logs do not establish one.
This release does not rewrite UIDs or bypass permission checks.

The existing guest shim already deferred certain media callbacks delivered to
a thread awaiting a synchronous Binder reply. KitKat's Keyguard interface
also uses one-way calls, including setHidden, whose implementation performs a
synchronous permission check. The logs show that callback executing on a UI
thread inside a synchronous relayout call.

## Changes

- Extend the existing async callback queue to the exact standard
  `com.android.internal.policy.IKeyguardService` interface when the receiving
  thread awaits a synchronous reply. Synchronous Keyguard queries are unchanged.
- Defer matching callbacks even when the same read batch contains a reply;
  do not replay them on the thread before libbinder unwinds that reply.
- Idle read-only Binder requests drain queued callbacks before blocking in the
  engine. Writes, transaction buffers, caller PID/UID, Binder object offsets and
  permission decisions are preserved. Missing queue space leaves the original
  packet untouched; it never drops a callback.
- Validate full String16 tokens and media callback payloads before reading
  them. Preserve the existing media-prepared/seek-complete exemptions.
- Log the first 16 deferred Keyguard calls per process, including transaction
  code and original caller PID/UID, to make the phone trial diagnosable.

No firmware APK/ODEX modifications, permission grants, credential rewriting,
wallpaper changes or camera integration are included. Camera WIP is separate.

## Verification and device test

The ARM32 regression test covers FIFO replay, unchanged identities/pointers,
mixed callback/reply batches, pre-blocking replay, synchronous/unknown interface
pass-through, malformed tokens/payloads, queue pressure and audio exemptions.
Run it with `sh native/guestshim/tests/run.sh` using NDK 22.1 and qemu-arm.

Release verification passed: six ARM shim tests, both audio ABI variants and
all 16 JVM tests. Both APK signatures and version code 12/name sunset.10 were
verified. Embedded shim SHA-256:
`aa7f71efe0eb457a010c415213195385bc49e6b5758263ed0b8ef3ef6782418c`.
The unchanged direct-track audio HAL hash was checked in both variants too.

**The scheduling change is a compatibility trial, not a confirmed Recents
fix.** On the Xperia ROM, enable Phone GPU, boot, unlock and repeatedly open
Recents, reopen tasks, use Small Apps, and lock/unlock again. If still invisible,
collect both host and logcat logs; check the new deferred-Keyguard lines and
whether permission denials remain. Galaxy S5 behavior is unverified.
