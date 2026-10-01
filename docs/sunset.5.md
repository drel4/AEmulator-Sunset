# sunset.5: configurable Holo navigation and trackball

Version `0.0.0.3-sunset.5`, code 7. This change does not fix the Sony
netmgr/system_server abort or implement Qualcomm modem protocols.

## Controls

Open the image settings, then Controls. Choose navigation buttons and use
Earlier/Later to set their left-to-right order. Available buttons: Back,
Home, Recents, Menu, Search, Power, Volume down/up, and D-pad directions/center.
Long rows scroll horizontally so touch targets stay at least 48 dp.
Recents is omitted on guests older than Android 3. Settings persist in image
metadata; app settings provide defaults for newly imported images. Stop and
start the image after changing its controls. Existing images keep their
original default buttons unless configured, and trackball is off by default.

The icons are drawn as thin white Holo-style vector outlines, with a bent
Back arrow, pentagonal Home, overlapping Recents rectangles, and square Menu
dots. No generated bitmap assets are needed.

Enable Trackball to display a circle above the emulator navbar and host
navigation inset. Swipe to roll; tap to select. If all navbar buttons are
hidden, a navbar-height safety spacer remains below the circle. Guest-screen
fitting uses the actual measured control height rather than a fixed 52 dp.
Sensitivity is dp per navigation step; smaller numbers move faster.

Arrow-key mode sends D-pad key presses instead of trackball events. It never
sends both for one swipe. This is also the fallback option for recovery,
whose static input implementation does not load the Android guest shim.
Center and Home scan codes are read from the guest key layout when available.

## Real trackball transport

The host exposes `/dev/aemu_trackball` in the emulator's extracted guest root.
The guest shim opens the emulator-owned `/dev/input/aemu-trackball` placeholder
as a Unix-stream connection and supplies evdev identification/capability
ioctls. This device advertises EV_REL/REL_X/REL_Y and EV_KEY/BTN_MOUSE, with
no absolute touchscreen axes. Actual movement and button frames use the
guest's 32-bit input_event ABI, ending in SYN_REPORT.

Socket identity is verified by getpeername rather than an fd-number table;
native descriptor duplication, fork, close, polling and nonblocking flags
remain usable without stale-fd matches. Unrelated ioctl requests continue
through the original shim path. Unsupported evdev requests return ENOTTY.
Key-state snapshot queries currently report button-up; click presses are
delivered as ordered down/up events, not held buttons.

On Android 3+, an **additional emulator-owned**
`system/usr/idc/AEmulator_Trackball.idc` sets `cursor.mode = navigation`,
preventing this relative device from becoming a mouse. No original ROM
image, vendor binary, or existing stock configuration is patched.

The choice follows the stock input readers:
[Gingerbread EventHub](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-2.3.7_r1/libs/ui/EventHub.cpp)
identifies the relative axes/button as a trackball, while
[KitKat InputReader](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-4.4.2_r1/services/input/InputReader.cpp)
selects SOURCE_TRACKBALL in navigation mode. Fine relative movement is
preserved instead of reducing everything to arrows; guest apps can then
apply their own trackball acceleration/selection handling.

## Verification and runtime checklist

- JVM tests exercise stable button IDs/order, duplicate and unknown IDs,
  fractional positive/negative movement, and exact ARM32 relative/button frames.
- The ARM smoke test runs the production wrapper under qemu-arm and checks
  evdev capabilities, nonblocking/CLOEXEC flags, duplicate/close identity,
  poll/read transport, and unaffected non-trackball descriptors.
- Existing fopen and crash-map ARM tests are also retained.
- Full stock-browser behavior and host gesture avoidance still require an
  on-device test; a successful build is not proof of either.

For a running Android 2.x guest, test Browser link selection using real mode,
then compare with arrow-key mode. Look for `trackball: guest connected` in the
host log; the UI displays a notice while no guest connection exists. Where
supported, `dumpsys input` should identify AEmulator Trackball as a trackball,
not a mouse or touchscreen. Verify diagonal swipes, sensitivity, taps,
cancelled/multitouch gestures, portrait/landscape layout, and button ordering.

## Build result

Both signed variants built successfully on the Linux server. ARM smoke tests,
the local pure-Kotlin motion check, and the clone release JVM tests passed.
Package versions and signatures were verified, and both APKs contain the
same shim as the workspace asset (SHA-256
`59fcb2218d0ab843a81838d5e3ebb3e5e6969535e353805df3e4830f0d8ec0f6`).

- Clone APK SHA-256: `f2f62bf03ee715a51bcffa0cd647887b021a857d6c3cddf4c56758383200c2f6`
- Standard APK SHA-256: `b542651a1adf1c9061695868f7d9dd419ffa567546c64c24794e4c0fceae4ae3`

The server's shared Gradle cache was locked by an earlier AESS daemon. The
successful build used an isolated cache at
`/home/nyash/0drel/aess-gradle.9OFzA6`, a 1536 MB heap, one worker, and
`-Pkotlin.compiler.execution.strategy=in-process`. Existing dependency
cache data was copied without lock files; unrelated daemons were not stopped.
The isolated cache is retained for subsequent builds. No device was attached
for Android runtime/browser testing.
