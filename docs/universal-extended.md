# Universal Sunset Extended

Initial implementation, 2026-10-07 (Sunset.38).

Extended is an experimental **compatibility profile of the existing Universal
Sunset native backend**, not a separately rebuilt QEMU. No download is needed.
Universal Sunset remains the default. Enable experimental features to select
Extended in VM settings; already-selected profiles remain visible. Changes take
effect at the next full stop/start, not by reopening a minimized VM.

## Initial differences

- Preserve firmware search-path order; append common command/library directories
  only when they exist inside the guest tree (including `/system/vendor/lib`).
- Honor service-local `setenv` from parsed init files for planned services and
  boot-media services. Handle double quotes, backslash escapes and empty values.
  Translate values through the launcher's `DHD_ENV_` guest transport, never into
  the host dynamic loader. Explicit user `DHD_ENV_` overrides and host socket
  configuration retain precedence.
- Reject malformed names, unresolved property expansions, oversized/control-byte
  values and bridge/loader/property-workspace overrides from firmware `setenv`.
- Store service environment in portable profiles; analyzer version 14 refreshes
  older profiles while retaining VM settings, shared-system container IDs and notes.

The default profile ignores this new service environment and retains its previous
search paths. The Gingerbread fallback also ignores Extended. Existing
`legacyEngine` settings and old archives remain readable; unknown profile IDs
fall back to Sunset. Switching back does not delete ROM files or guest data.

## Boundaries

This is not a full Android init interpreter: imports, dynamic property expansion,
service credentials and hardware services are not newly emulated. These changes
do not establish that any additional firmware boots. Test on a separate VM copy
and compare both host logs and guest logcat. Native translator/syscall, Binder
and GPU behavior is unchanged. Inherited binary source-provenance limitations
in `license-audit.md` remain unresolved.

Universal Original needs a verified upstream component set. SENSES Sunset uses
a different protocol and requires a real adapter; neither is offered as a working
engine by this release. SENSES Original has been removed from the plan.

## Source references and hardware investigation

Checked Android init semantics against
[Android 4.0.4 init documentation](https://android.googlesource.com/platform/system/core/+/android-4.0.4_r2.1/init/readme.txt)
and [Android 7.1.2 service implementation](https://android.googlesource.com/platform/system/core/+/android-7.1.2_r39/init/service.cpp).
Service `setenv` is local to the launched process, not a global export. Quotes and
escapes tokenize values; this first pass does not implement init line-folding.

Device kernel references inspected at pinned revisions:

- [Sony APQ8064](https://github.com/LineageOS/android_kernel_sony_apq8064/tree/c9abb65b217287843a70158226ecf85a4171a324):
  `drivers/staging/android/binder.h`, `include/linux/input.h`,
  `include/linux/android_alarm.h`.
- [Samsung MSM8974](https://github.com/LineageOS/android_kernel_samsung_msm8974/tree/da48a34d7a3b9085819a716ce95f4b80e6f0a209):
  `drivers/staging/android/binder.h`, `include/linux/input.h`. The Sony alarm-header
  path is absent here; absence at that path is not proof of no alarm driver.
- [MediaTek Sprout](https://github.com/LineageOS/android_kernel_mediatek_sprout/tree/603890815aa034c946a95be6bbf1550f701ee093):
  `drivers/staging/android/binder.h`, `drivers/staging/android/ashmem.h`,
  `include/linux/input.h`. The inspected Binder/input definitions agree with the
  Qualcomm references; ashmem size/protection requests use guest-sized native
  types, while the pin range uses two 32-bit fields.

Both inspected Binder headers expose protocol 7 and the six-field native-long
`binder_write_read`; ioctl sizes must be interpreted for ARM32 guests, not copied
from the arm64 host. Both input headers use the same inspected `EVIOCGVERSION`
and `EVIOCGABS` definitions. Existing Sunset shim constants were compared to
these interfaces; no new kernel/ioctl emulation is shipped in this first pass.

Further generic work should deepen the AOSP userspace comparison with these kernel
families (including MediaTek), then add bounded ABI regression tests
for Binder/ashmem, alarm/time, framebuffer/input and sysfs contracts. Device
sources are references for guest-visible behavior, not guest kernels to boot.
Do not turn unrecognized driver requests into success: implement a supported
contract or preserve its error. Vendor-only behavior needs explicit isolation,
not a model-name hack applied to all firmware. Preserve kernel license terms;
this app's GPLv3 notice does not relicense kernel implementation code.
