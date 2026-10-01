# Host camera HAL1

ARM32 standard HAL1 module, module/device version 1.0. Independent of Sony or
Samsung sensor drivers. `TreeFixer` installs it only with the per-ROM opt-in;
`ro.hardware.camera=aemu_host` selects it. Existing stock modules and firmware
APKs/ODEX remain untouched by this camera implementation.

Transport: `/dev/aemu_camera` Unix socket, 16-byte little-endian `CAM1` request
(operation, lens index, JPEG quality). Operations: 0 metadata, 1 preview stream,
2 current-preview-frame JPEG. Replies: `INF1` count + facing/orientation pairs;
`FRM1` or `JPG1` + byte count + 64-bit timestamp + payload; `ERR1` + negative
errno. Preview is 640×480 NV21. HAL renders RGBA8888 through guest gralloc,
delivers preview/JPEG callbacks and writes EXIF orientation. No video/raw/flash.

Host access requires explicit setting, runtime permission and a resumed VM
activity. Enumeration does not open the camera and stays stable in background;
capture sessions and client sockets close on pause, VM stop or boot failure.
Guest apps must reopen the camera after host backgrounding.

Build on the Linux server: `cd native/camerahal; NDK=/path/to/ndk sh build.sh`.
Native memory/protocol smoke: `sh native/camerahal/tests/run.sh` (cc, ASan/UBSan).
Optional real-ROM Bionic check:

```sh
NDK=/path/to/ndk AEMU_TEST_SYSTEM_DIR=/path/to/extracted/system \
  sh native/camerahal/tests/bionic-run.sh
```

Requires qemu-arm, unshare user/PID namespaces, bash, host cc and the supplied
ROM's bin/linker, lib/libc.so, lib/libdl.so. Files are copied into a temporary
fixture; no ROM binaries are distributed. Checks the actual HAL's dlopen and
ARM ABI, then preview/JPEG callbacks with a fake host transport. This does not
exercise Camera2 hardware, the AEmulator QEMU/Binder engine or stock camera UI.

The fixture needs a root-mapped read-only property area before Bionic startup.
The isolated PID namespace also avoids old recursive-mutex thread-ID overflow
on servers with high tids. These are harness requirements, not firmware fixes.

ABI references:
[AOSP HAL1 camera.h](https://raw.githubusercontent.com/aosp-mirror/platform_hardware_libhardware/android-4.4.2_r1/include/hardware/camera.h),
[camera_common.h](https://raw.githubusercontent.com/aosp-mirror/platform_hardware_libhardware/android-4.4.2_r1/include/hardware/camera_common.h).
Device/module/gralloc offsets are checked at ARM compilation. Xperia ZR's
decompiled CameraService uses count/info at 0x80/0x84 and device ops at 0x40.
Sony's separate extension service and proprietary commands are not implemented;
passing the core ABI tests does not prove the Sony/Samsung stock apps work.
