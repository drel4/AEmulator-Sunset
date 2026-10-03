# AEmulator Sunset CM11 guest target, added 2026-10-04.
# Reuse the public ARM emulator ABI, not Sony/Samsung hardware blobs.
include build/target/board/generic/BoardConfig.mk
TARGET_NO_KERNEL := true
TARGET_NO_RECOVERY := true
TARGET_ARCH_VARIANT := armv7-a-neon
TARGET_CPU_VARIANT := cortex-a9
TARGET_CPU_SMP := true
WITH_DEXPREOPT := false
BOARD_SYSTEMIMAGE_PARTITION_SIZE := 1073741824
BOARD_USERDATAIMAGE_PARTITION_SIZE := 536870912
TARGET_USERIMAGES_SPARSE_EXT_DISABLED := false
