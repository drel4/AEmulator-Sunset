#!/bin/sh
# Optional integration check with user-supplied KitKat ROM binaries, never distributed.
# Optional loader test also uses stock libhardware.so, libcutils.so, liblog.so.
set -eu
NDK=${NDK:-$ANDROID_NDK_HOME}
BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
TEST_SRC=$(cd "$(dirname "$0")" && pwd)
TEST_DIR=$(mktemp -d)
HAL=$(readlink -f "${1:-$TEST_SRC/../../../app/src/main/assets/engines/common/camera.aemu_host.so}")
[ -f "$HAL" ]
mkdir -p "$TEST_DIR/system/bin" "$TEST_DIR/system/lib"
cp "$AEMU_TEST_SYSTEM_DIR/bin/linker" "$TEST_DIR/system/bin/linker"
cp "$AEMU_TEST_SYSTEM_DIR/lib/libc.so" "$AEMU_TEST_SYSTEM_DIR/lib/libdl.so" "$TEST_DIR/system/lib/"
cc "$TEST_SRC/property-fixture.c" -o "$TEST_DIR/property-fixture"
"$TEST_DIR/property-fixture" "$TEST_DIR/properties"
chmod 444 "$TEST_DIR/properties"
for test in bionic-probe hal-smoke; do
    "$BIN/clang" --target=armv7a-linux-androideabi21 -march=armv7-a -mthumb -DLONG_BIT=32 -fPIE -pie \
        -fno-stack-protector -Wl,--hash-style=sysv -o "$TEST_DIR/$test" "$TEST_SRC/$test.c" -ldl
done
cd "$TEST_DIR"
# Root-mapped fixture passes Bionic's uid/gid check; small tids avoid old recursive
# mutex overflow on busy Linux servers. Neither namespace changes the host's uid/PIDs.
unshare -Urpf bash -c 'exec 30<"$1/properties"; ANDROID_PROPERTY_WORKSPACE=30,65536 timeout 20s qemu-arm -L "$1" -E LD_LIBRARY_PATH="$1/system/lib" "$1/bionic-probe" "$2"' -- "$TEST_DIR" "$HAL"
unshare -Urpf bash -c 'exec 30<"$1/properties"; ANDROID_PROPERTY_WORKSPACE=30,65536 timeout 25s qemu-arm -L "$1" -E LD_LIBRARY_PATH="$1/system/lib" "$1/hal-smoke"' -- "$TEST_DIR"
if [ -f "$AEMU_TEST_SYSTEM_DIR/lib/libhardware.so" ]; then
    cp "$AEMU_TEST_SYSTEM_DIR/lib/libhardware.so" "$AEMU_TEST_SYSTEM_DIR/lib/libcutils.so" "$AEMU_TEST_SYSTEM_DIR/lib/liblog.so" "$TEST_DIR/system/lib/"
    for library in libstdc++.so libm.so; do
        if [ -f "$AEMU_TEST_SYSTEM_DIR/lib/$library" ]; then cp "$AEMU_TEST_SYSTEM_DIR/lib/$library" "$TEST_DIR/system/lib/"; fi
    done
    mkdir -p "$TEST_DIR/system/lib/hw"
    cp "$HAL" "$TEST_DIR/system/lib/hw/camera.aemu_host.so"
    unshare -Urpf bash -c 'exec 30<"$1/properties"; ANDROID_PROPERTY_WORKSPACE=30,65536 timeout 20s qemu-arm -L "$1" -E LD_LIBRARY_PATH="$1/system/lib" "$1/bionic-probe" "$1/system/lib/hw/camera.aemu_host.so" "$1/system/lib/libhardware.so" missing' -- "$TEST_DIR"
    ln -s camera.aemu_host.so "$TEST_DIR/system/lib/hw/camera.default.so"
    unshare -Urpf bash -c 'exec 30<"$1/properties"; ANDROID_PROPERTY_WORKSPACE=30,65536 timeout 20s qemu-arm -L "$1" -E LD_LIBRARY_PATH="$1/system/lib" "$1/bionic-probe" /system/lib/hw/camera.aemu_host.so "$1/system/lib/libhardware.so"' -- "$TEST_DIR"
fi
