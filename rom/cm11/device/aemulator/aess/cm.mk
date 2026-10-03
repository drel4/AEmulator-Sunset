# AEmulator Sunset CM11 guest target, added 2026-10-04.
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/board/generic/device.mk)
$(call inherit-product, vendor/cm/config/common_full_phone.mk)

# Guest HAL modules only: no standalone desktop SDK emulator build.
PRODUCT_PACKAGES += \
    egl.cfg gralloc.goldfish libGLESv1_CM_emulation lib_renderControl_enc \
    libEGL_emulation libGLESv2_enc libOpenglSystemCommon libGLESv2_emulation \
    libGLESv1_enc qemu-props qemud camera.goldfish camera.goldfish.jpeg \
    lights.goldfish gps.goldfish sensors.goldfish

PRODUCT_COPY_FILES += \
    device/generic/goldfish/fstab.goldfish:root/fstab.goldfish \
    device/generic/goldfish/init.goldfish.rc:root/init.goldfish.rc \
    device/generic/goldfish/init.goldfish.sh:system/etc/init.goldfish.sh \
    device/generic/goldfish/ueventd.goldfish.rc:root/ueventd.goldfish.rc

DEVICE_PACKAGE_OVERLAYS := device/aemulator/aess/overlay
PRODUCT_NAME := cm_aess
PRODUCT_DEVICE := aess
PRODUCT_BRAND := AEmulator
PRODUCT_MANUFACTURER := AEmulator
PRODUCT_MODEL := AEmulator Sunset CM11
PRODUCT_CHARACTERISTICS := default
CM_BUILD := aess
TARGET_UNOFFICIAL_BUILD_ID := AESS
