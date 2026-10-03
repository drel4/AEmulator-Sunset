#!/bin/sh
set -eu
NDK=${NDK:-$ANDROID_NDK_HOME}
BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
OUT=${1:-../../app/src/main/assets/engines/common/sensors.aemu_host.so}
mkdir -p "$(dirname "$OUT")"
"$BIN/clang" --target=armv7a-linux-androideabi21 -march=armv7-a -mthumb -mfloat-abi=softfp -Os -fPIC -shared \
    -nostdlib -ffreestanding -fno-builtin -DLONG_BIT=32 -fvisibility=hidden -fno-stack-protector -Wl,--hash-style=sysv -Wl,--build-id=none \
    -Wl,-z,norelro -Wl,--no-undefined -Wl,-soname,sensors.aemu_host.so -o "$OUT" sensors_hal.c -lc
"$BIN/llvm-strip" "$OUT"
