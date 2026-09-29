#!/bin/sh
# Сборка звукового HAL с раскладкой AOSP 4.2–4.4: ARMv7, только libc, классическая хэш-таблица
NDK=${NDK:-$ANDROID_NDK_HOME}
BIN="$NDK/toolchains/llvm/prebuilt/windows-x86_64/bin"
[ -x "$BIN/clang" ] || BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
OUT=${1:-../../app/src/main/assets/engines/kk/audio.primary.aosp.so}
mkdir -p "$(dirname "$OUT")"
"$BIN/clang" --target=armv7a-linux-androideabi21 -march=armv7-a -mthumb -mfloat-abi=softfp -Os -fPIC -shared \
  -nostdlib -ffreestanding -fno-builtin -fvisibility=hidden -fno-stack-protector \
  -Wl,--hash-style=sysv -Wl,-z,norelro -Wl,--build-id=none -Wl,-soname,audio.primary.default.so \
  -o "$OUT" audio_hal.c -lc
"$BIN/llvm-strip" "$OUT" 2>/dev/null || true
# Android 4.0 (ICS) variant: older audio_hw_device / open_output_stream layout
"$BIN/clang" --target=armv7a-linux-androideabi21 -march=armv7-a -mthumb -mfloat-abi=softfp -Os -fPIC -shared \
  -nostdlib -ffreestanding -fno-builtin -fvisibility=hidden -fno-stack-protector -DAEMU_ICS \
  -Wl,--hash-style=sysv -Wl,-z,norelro -Wl,--build-id=none -Wl,-soname,audio.primary.default.so \
  -o "$(dirname "$OUT")/audio.primary.ics.so" audio_hal.c -lc
"$BIN/llvm-strip" "$(dirname "$OUT")/audio.primary.ics.so" 2>/dev/null || true
# Qualcomm CAF variants: extra set_fm_volume / open_output_session slots in audio_hw_device
for v in ics; do
  [ $v = ics ] && D=-DAEMU_ICS || D=
  "$BIN/clang" --target=armv7a-linux-androideabi21 -march=armv7-a -mthumb -mfloat-abi=softfp -Os -fPIC -shared \
    -nostdlib -ffreestanding -fno-builtin -fvisibility=hidden -fno-stack-protector $D -DAEMU_QCOM \
    -Wl,--hash-style=sysv -Wl,-z,norelro -Wl,--build-id=none -Wl,-soname,audio.primary.default.so \
    -o "$(dirname "$OUT")/audio.primary.$v-qcom.so" audio_hal.c -lc
  "$BIN/llvm-strip" "$(dirname "$OUT")/audio.primary.$v-qcom.so" 2>/dev/null || true
done
# MediaTek: replaces /system/lib/libaudio.primary.default.so (HMI + DcRemove forwarders), own PCM channel
"$BIN/clang" --target=armv7a-linux-androideabi21 -march=armv7-a -mthumb -mfloat-abi=softfp -Os -fPIC -shared \
  -nostdlib -ffreestanding -fno-builtin -fvisibility=hidden -fno-stack-protector -DAEMU_MTK -DAEMU_PCM_DEV='"/dev/aemu_pcm"' \
  -Wl,--hash-style=sysv -Wl,-z,norelro -Wl,--build-id=none -Wl,-soname,libaudio.primary.default.so \
  -o "$(dirname "$OUT")/audio.primary.mtk.so" audio_hal.c -ldl -lc
"$BIN/llvm-strip" "$(dirname "$OUT")/audio.primary.mtk.so" 2>/dev/null || true
