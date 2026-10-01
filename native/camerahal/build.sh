#!/bin/sh
set -eu
NDK=${NDK:-$ANDROID_NDK_HOME}
BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
OUT=${1:-../../app/src/main/assets/engines/common/camera.aemu_host.so}
mkdir -p "$(dirname "$OUT")"
"$BIN/clang" --target=armv7a-linux-androideabi21 -march=armv7-a -mthumb -mfloat-abi=softfp -Os -fPIC -shared \
    -nostdlib -fvisibility=hidden -fno-stack-protector -ffreestanding -fno-builtin -DLONG_BIT=32 \
    -Wl,--hash-style=sysv -Wl,-z,norelro -Wl,--build-id=none -Wl,--no-undefined -Wl,-soname,camera.aemu_host.so \
    -o "$OUT" camera_hal.c -lc -ldl
"$BIN/llvm-strip" "$OUT"
