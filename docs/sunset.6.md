# Sunset.6 — Sony netmgr offline compatibility trial

Version: `0.0.0.3-sunset.6`, code 8. Firmware binaries are not patched.

## Evidence and scope

The Xperia ZR logs show `Loc_hal_worker` aborting in the vendor netmgr client:
`Error on netmgr_nl_init`, followed by the assertion at netmgr_client.c:435.
Headless Ghidra analysis of the supplied stock libnetmgr.so finds a listener
created with AF_NETLINK / SOCK_RAW / protocol 16, bound to the process PID and
multicast mask 0x40000000. Other services report netlink errno 93. The precise
failure stage in netmgr itself is not yet observed in a live .6 run.

## Implementation

When creating that socket fails with EPROTONOSUPPORT or EAFNOSUPPORT, the guest
shim supplies an emulator-owned, idle Unix datagram endpoint. It accepts only
the vendor group-31 bind contract and multicast destination. A ping send has
no modem peers and produces no reply. Poll/select and blocking receive remain
real kernel operations. No cellular connectivity, GPS fixes, link-up events,
generic-netlink families, or host network administration are fabricated.

Descriptors are identified by their kernel abstract socket name, preserving
dup/fork behavior without stale fd tables. A per-process atomic sequence avoids
name collisions when an original fd closes but a duplicate keeps it alive.
All unrelated socket/bind/sendmsg operations pass through. Diagnostic stderr
lines are prefixed `aemushim netmgr:` and identify fallback and bind outcomes.

This is an offline compatibility trial, not a telephony implementation or a
claim that the Sony firmware now boots. Missing QMI/qmux and other vendor
services may be subsequent blockers. The prior navbar/trackball work remains.

## Verification

The production ARM shim is exercised under qemu-arm. Tests cover the forced
unsupported-socket branch, CLOEXEC/NONBLOCK, wrong-group rejection, duplicate
bind rejection, getsockname truncation, dup/close/fd reuse, multicast ping
without an echo, idle poll, EAGAIN receive, invalid requests, and ordinary
Unix sockets, inherited sockets after fork, and normal sendmsg traffic.
Existing fopen, crash-map, and trackball tests remain enabled.

All four ARM smoke programs passed; all five JVM tests passed. Clone and
standard release APKs built successfully on the configured Linux server.
Both APK signatures verified, version code/name matched, and each embedded
shim hash matched the compiled asset. The shim still imports only `__errno`,
`environ`, and `fdopen`, with no added runtime dependencies. No adb devices
were attached, so guest ROM boot verification remains outstanding.

SHA-256:

- Clone APK: `2a238a54d52d17ac8a29b392a24112a7fb5102c28cd3647cae6bcac555a46e51`
- Standard APK: `5df6602c3db8ffb5f59b3f8d13680b9b8a76ae782b3238531b2cf69b7a19df30`
- Guest shim: `8780b3161b8ade7992757d41168cced7197b0a5a4fa97e49f03d5f49cf971c35`

ARM syscall numbers checked against the
[Linux ARM syscall table](https://raw.githubusercontent.com/torvalds/linux/v3.4/arch/arm/include/asm/unistd.h).

A live Xperia ROM boot must still be tested. Export both host and full guest
logs; check for `vendor group 31 bound offline` and whether the original
netmgr assertion disappears. Do not treat a boot-complete property as proof
that launcher/SystemUI is usable.

## First on-device report

The user reports that the stock Xperia ZR image reaches the lock screen with
.6, then appears frozen after unlocking. The new exported logs contain the
successful offline group-31 bind. Historical stderr includes earlier netmgr
assertions; these should not be mistaken for new .6 failures.

The current guest main log has a fatal exception on the system_server main
thread: Sony AudioEffectService calls PostAudioEffect.setXLoud and receives
`AudioEffect: set/get parameter error`. The host log also records mediaserver
SIGSEGV/restart loops: initial crashes have PC 0x2 in AudioOut_2 with LR in
libnbaio; after the existing policy fallback, crashes occur in libaemu_apaosp.
These audio faults are not fixed by .6. The evidence points to audio as the
next compatibility blocker, rather than a proven display/input deadlock.
