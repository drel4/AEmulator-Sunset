#!/bin/sh
set -eu
NDK=${NDK:-$ANDROID_NDK_HOME}
BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
TEST_DIR=$(mktemp -d)
TEST_SRC=$(cd "$(dirname "$0")" && pwd)
for variant in aosp directtrack; do
    D=
    [ "$variant" != directtrack ] || D=-DAEMU_DIRECTTRACK
    "$BIN/clang" --target=armv7a-linux-androideabi21 -march=armv7-a -mthumb -mfloat-abi=softfp -Os \
        -nostdlib -static -ffreestanding -fno-builtin -fno-stack-protector $D \
        -Wl,-e,_start -o "$TEST_DIR/audio-$variant" "$TEST_SRC/abi-smoke.c"
    qemu-arm "$TEST_DIR/audio-$variant"
done
