#!/bin/sh
set -eu
NDK=${NDK:-$ANDROID_NDK_HOME}
BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
TEST_DIR=$(mktemp -d)
TEST_SRC=$(cd "$(dirname "$0")" && pwd)
"$BIN/clang" --target=armv7a-linux-androideabi21 -march=armv7-a -mthumb -Os \
    -nostdlib -static -ffreestanding -fno-builtin -fno-stack-protector \
    -Wl,-e,_start -o "$TEST_DIR/fopen-smoke" "$TEST_SRC/fopen-smoke.c"
"$BIN/clang" --target=armv7a-linux-androideabi21 -march=armv7-a -mthumb -Os \
    -nostdlib -static -ffreestanding -fno-builtin -fno-stack-protector \
    -Wl,-e,_start -o "$TEST_DIR/crash-maps-smoke" "$TEST_SRC/crash-maps-smoke.c"
cd "$TEST_DIR"
"$BIN/clang" --target=armv7a-linux-androideabi21 -march=armv7-a -mthumb -Os \
    -nostdlib -static -ffreestanding -fno-builtin -fno-stack-protector \
    -Wl,-e,_start -o "$TEST_DIR/trackball-smoke" "$TEST_SRC/trackball-smoke.c"
qemu-arm ./fopen-smoke
qemu-arm ./crash-maps-smoke
qemu-arm ./trackball-smoke
"$BIN/clang" --target=armv7a-linux-androideabi21 -march=armv7-a -mthumb -Os \
    -nostdlib -static -ffreestanding -fno-builtin -fno-stack-protector \
    -Wl,-e,_start -o "$TEST_DIR/netmgr-smoke" "$TEST_SRC/netmgr-smoke.c"
qemu-arm ./netmgr-smoke
"$BIN/clang" --target=armv7a-linux-androideabi21 -march=armv7-a -mthumb -Os \
    -nostdlib -static -ffreestanding -fno-builtin -fno-stack-protector \
    -Wl,-e,_start -o "$TEST_DIR/vibration-smoke" "$TEST_SRC/vibration-smoke.c"
qemu-arm ./vibration-smoke
