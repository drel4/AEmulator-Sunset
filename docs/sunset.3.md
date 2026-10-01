# sunset.3 compatibility test build

## Evidence

The supplied logs are for Sony C5503 / Xperia UI, Android 4.4.2 (API 19).
They do not establish Galaxy S5 compatibility.

- `sunset.1` reaches `system_server`, then reports `Failed to parse network stats`.
- `sunset.2` adds an `fopen` interceptor but zygote and installd exit with code 139.
- The interceptor incorrectly defines `RTLD_NEXT` as `-1`. In ARM32 bionic,
  `-1` is `RTLD_DEFAULT` and `-2` is `RTLD_NEXT`. Consequently its lookup can
  return its own `fopen`, causing unbounded recursion.

Sources: [KitKat dlfcn.h](https://github.com/aosp-mirror/platform_bionic/blob/android-4.4.2_r1/libc/include/dlfcn.h),
[KitKat dlsym implementation](https://github.com/aosp-mirror/platform_bionic/blob/android-4.4.2_r1/linker/dlfcn.cpp).

## Changes

Keep the existing descriptor-based replacement: redirect qtaguid paths,
open through the ARM syscall helper, and create a guest-owned stream with
bionic `fdopen`. Export `fopen64` through the same wrapper. No `dlsym` lookup
or `libdl` dependency is needed. The build now fails on compilation/strip
errors and uses the selected host toolchain for stripping.

Version code: 5. Version name: `0.0.0.3-sunset.3`.

## Verification and next ROM run

`native/guestshim/tests/run.sh` runs the actual wrapper under `qemu-arm` with
a controlled `fdopen` stub. It checks path mapping, read/write/append modes,
truncation, exclusive creation, close-on-exec, errors, descriptor cleanup,
and the `fopen64` alias. It does not test bionic FILE internals or ROM boot.

Build on the configured Linux server, from the checkout root:

```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export NDK=/home/nyash/0flfx/Android/Sdk/ndk/22.1.7171670
sh native/guestshim/tests/run.sh
sh native/guestshim/build-sunset-linux.sh
```

Install the clone APK over `app.aemu.clone` and cold-start the existing Sony
image. The tree fixer compares asset contents and replaces the guest shim,
so reimporting the ROM is not required. Collect both host and guest logs:
check whether zygote/installd stay alive, whether network-stat parsing errors
disappear, and how far `system_server` proceeds. Device-specific missing
sensor/radio/secure-hardware messages alone do not identify the boot blocker.
