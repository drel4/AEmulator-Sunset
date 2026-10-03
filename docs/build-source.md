# Building AEmulator Sunset

Updated 2026-10-03 for `v0.0.0.3-sunset.28`.

## Application APKs (uses checked-in engine prebuilts)

The release tag identifies the application source, Gradle configuration and
bundled assets used for that release. This builds the app, **not every engine
component from source**; see [the outstanding audit](license-audit.md).

The tested Linux release environment uses JDK 21, Android SDK platform 36,
build-tools 36.0.0 and Gradle 8.14.2 (the checked-in wrapper). Kotlin/Java
bytecode targets Java 17. First-time builds need network access to resolve the
dependencies pinned in `build.gradle.kts` and `app/build.gradle.kts`.

```sh
git clone https://github.com/drel4/AEmulator-Sunset.git
cd AEmulator-Sunset
git checkout v0.0.0.3-sunset.28
export JAVA_HOME=/path/to/jdk-21
export ANDROID_HOME=/path/to/Android/Sdk
sh gradlew --no-daemon '-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8' \
  -Pkotlin.compiler.execution.strategy=in-process --max-workers=1 \
  testCloneReleaseUnitTest assembleCloneRelease assembleStandardRelease
```

Outputs: `app/build/outputs/apk/{standard,clone}/release/`.
Standard package: `app.aemu`; clone package: `app.aemu.clone`. Both require
an arm64-v8a Android host, API 26 or newer.

Release signing uses private, untracked `keystore.properties`. Without it,
release APKs are unsigned. To test/install without the publisher's key, build
`assembleStandardDebug` / `assembleCloneDebug` using your own debug key, or
sign an unsigned release APK with your own key using Android `apksigner`.
Your differently signed APK cannot update the publisher-signed installation.
No publisher credentials are needed to compile or run your own build.

## Fork native/helper source

These scripts overwrite the corresponding bundled binaries; run them only in
a build checkout. Sunset's tested guest-native toolchain is NDK
`22.1.7171670` on Linux. The current APK build does not rebuild native assets
automatically. Rebuilding may change binary hashes; byte-for-byte APK
reproducibility has not been established.

```sh
export NDK="$ANDROID_HOME/ndk/22.1.7171670"
(cd native/guestshim && sh build.sh)
(cd native/audiohal && sh build.sh)
(cd native/camerahal && sh build.sh)
(cd native/sensorhal && sh build.sh)
sh native/setupctl/build.sh
```

- `guestshim`: preload shim, recovery shim and `aemu_true.so`.
- `audiohal`: AOSP, Sony DIRECTTRACK, ICS/Qualcomm and MTK audio HAL variants.
- `camerahal`: `camera.aemu_host.so`, the experimental HAL1 bridge.
- `sensorhal`: `sensors.aemu_host.so`, the experimental legacy motion-sensor
  bridge. Its host transport/lifecycle smoke test is `sh native/sensorhal/tests/run.sh`
  and requires a C compiler with ASan/UBSan (no guest firmware needed).
- `setupctl`: `aemu-setup.jar`, setup and guest rotation helpers;
  requires SDK 36/d8 and JDK 17+.
- Additional upstream source-backed scripts: `native/hostjni`, `native/apwrap`,
  `native/glsplit`, `native/gueststubs`. Read their scripts and requirements;
  do not mistake these for a complete engine build.

JVM tests run via `testCloneReleaseUnitTest`. Guest shim/audio/camera smoke
tests have `tests/run.sh` scripts and require `qemu-arm` plus an ARM cross
compiler; camera Bionic tests also require a separately supplied stock guest
fixture and user/PID namespace support (see `native/camerahal/README.md`).
Setup policy tests are in `native/setupctl/tests/PolicyTest.java`.

## Source access beside downloads

Release notes link the exact tagged checkout, this document and the audit.
GitHub's tag source archives include the source tracked in that checkout, but
cannot supply components absent from Git. Preserve license notices and make
required source/build materials available when redistributing your own build.
