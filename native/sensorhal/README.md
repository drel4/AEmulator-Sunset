# Sunset host motion HAL

Independently implemented ARM32 legacy poll-device ABI (0.1), suitable for the
standard Android 2.3–7.1 sensorservice compatibility path. Optional VM setting;
missing host sensors are not advertised. Accelerometer (m/s²), magnetic field
(µT) and gyroscope (rad/s) are forwarded in natural-device axes with original
monotonic nanosecond timestamps and accuracy. No OEM drivers, fake missing
sensors, high-rate sampling, batching, wake-up sensors or direct channels.
Host registration is demand-driven, capped at 50 Hz and paused offscreen.

Build: `NDK=/path/to/ndk sh build.sh` from this directory. Run
`sh tests/run.sh` for a host ASan/UBSan transport/lifecycle smoke test. ARM32
sizes/offsets are checked at compile time. Device/ROM testing is still required:
vendor frameworks can impose nonstandard ABIs. Original OEM HALs are parked
in the guest compatibility tree, not erased from source firmware.

ABI reference:
[AOSP Gingerbread](https://android.googlesource.com/platform/hardware/libhardware/+/android-2.3.7_r1/include/hardware/sensors.h),
[AOSP KitKat](https://android.googlesource.com/platform/hardware/libhardware/+/android-4.4.4_r2/include/hardware/sensors.h),
[AOSP Nougat sensorservice](https://android.googlesource.com/platform/frameworks/native/+/android-7.1.2_r1/services/sensorservice/SensorDevice.cpp).
The implementation is GPL-3.0; ABI descriptions were consulted, not imported.
