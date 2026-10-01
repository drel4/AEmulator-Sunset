# Sunset.7 — Sony KitKat audio stream ABI

Version `0.0.0.3-sunset.7`, code 9.

## Diagnosis

The .6 user run reaches the Xperia ZR lock screen, then freezes around unlock.
The current guest system buffer records a fatal system_server exception from Sony's
AudioEffectService / PostAudioEffect.setXLoud (`AudioEffect: set/get parameter
error`). Mediaserver has already crashed in AudioOut_2: PC 0x2, LR in stock
libnbaio.so+0x3a9e. Subsequent policy fallback crashes have address 0x98.

Headless Ghidra analysis of the unmodified ZR libnbaio identifies:

| Stock routine (relative offset) | Stream callback offset |
| --- | --- |
| AudioStreamOutSink::write, 0x3a48 | 0x40 |
| getNextWriteTimestamp, 0x3a74 | 0x50 |
| getTimestamp, 0x3a88 | 0x68 |

The getTimestamp routine loads and calls the non-null word at 0x68. The old
emulator's AOSP stream puts the integer speaker-device value 2 there, explaining
the observed branch to PC=2. This is the QCOM_DIRECTTRACK layout: start/stop
slots are inserted after get_render_position, shifting the timestamp tail by
eight bytes. The PCM write slot and standard device ABI are unchanged.

The layout is consistent with the
[CM11 hardware interface](https://raw.githubusercontent.com/LineageOS/android_hardware_libhardware/cm-11.0/include/hardware/audio.h),
and differs from the
[AOSP KitKat stream](https://raw.githubusercontent.com/aosp-mirror/platform_hardware_libhardware/android-4.4.2_r1/include/hardware/audio.h).
Stock Sony libposteffectwrapper's xLOUD parameter handler was also decompiled:
valid Boolean parameter values return success. That does not establish that
every xLOUD operation works, but provides no reason to patch or bypass it.
Keeping mediaserver alive may resolve the Java error if that error resulted
from its death; this remains to be checked on-device.

## Implementation

- Separate emulator audio HAL asset `audio.primary.directtrack.so` with the
  correct extended stream structure. Start/stop explicitly return unsupported;
  unsupported timestamp/offload callbacks are null. No DSP results are faked.
- Selection is API 19–20 only, using the verified ARM ELF/Thumb getTimestamp
  signature and AudioStreamOutSink symbol text in the actual guest libnbaio,
  not a blanket Sony/Qualcomm brand check. Other audio HAL selections are unchanged.
- For matching images, restore the saved stock audio_policy.default.so from
  .aemu-parked rather than retaining the incompatible AOSP fallback. Keep the
  saved original. Fresh imports retain their stock policy.
- Stock audio libraries, APKs, xLOUD code, and source ROM images are not patched.

## Verification

Native ARM smoke tests use the production HAL and independent raw callback
offsets. Both AOSP and direct-track variants are checked: PCM/configuration,
write flags, frame counts, standby/close, byte-sized mic mute, and extended
timestamp/start/stop behavior. JVM tests check selector matches, AOSP callback
offset rejection, symbol/header/architecture mismatches, and truncation.

Verification completed: both new ARM HAL smoke variants and the four existing
guest-shim smoke programs passed. All seven JVM tests passed. The production
selector was separately run against the extracted, unmodified ZR libnbaio and
recognized it. Both signed release APKs report version code 9 / sunset.7;
the new HAL embedded in each matches the compiled asset. The HAL exports HMI
and imports only its existing libc IO/allocation functions. Local downloaded
artifact hashes match the Linux build outputs.

SHA-256:

- Clone APK: `d089f08d36d5b15ebb4f54898a2e3c4460818c96b3ef76b790755a8c59d4ffa3`
- Standard APK: `16d5cb87a164d2d9148ffa33a8e7a2b3dc258a7a79d69b69f9809f20da69274d`
- Direct-track HAL: `a1f2b8d1fad93d5f68894cccfb2a5e0def92bb5c18ccb4cd6942b4db41d47721`
- Stock ZR libnbaio (analysis input): `b31315e8ce36f0d6fe50d83e606c3838160243e53384faa2e0877d213cc213cd`

This is a targeted fix candidate, not a claim of a verified successful unlock.
Boot the existing Xperia image (no reset/reimport required) and export fresh
host and guest logs. Look for `verified CAF direct-track stream ABI selected`
and `CAF direct-track, restored firmware audio policy` (the latter only when
an earlier saved policy exists). Check mediaserver stability, unlock, setup
wizard/launcher, and actual audio playback.
