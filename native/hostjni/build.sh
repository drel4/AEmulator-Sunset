#!/bin/sh
# Сборка хост-помощника libaemuhost.so (arm64, API 29 — там появился fdsan)
NDK=${NDK:-$ANDROID_NDK_HOME}
BIN="$NDK/toolchains/llvm/prebuilt/windows-x86_64/bin"
[ -x "$BIN/clang" ] || BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
OUT=${1:-../../app/src/main/jniLibs/arm64-v8a/libaemuhost.so}
"$BIN/clang" --target=aarch64-linux-android29 -Os -fPIC -shared -Wl,-soname,libaemuhost.so -o "$OUT" hostjni.c
"$BIN/llvm-strip" "$OUT" 2>/dev/null || true
