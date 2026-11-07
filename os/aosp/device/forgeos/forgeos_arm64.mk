# Experimental Android 15 GSI product. NOT a validated onn firmware target.
$(call inherit-product, $(SRC_TARGET_DIR)/product/aosp_arm64.mk)
PRODUCT_NAME := forgeos_arm64
PRODUCT_BRAND := ForgeOS
PRODUCT_MODEL := ForgeOS AI ARM64 GSI development image
PRODUCT_MANUFACTURER := ForgeOS
PRODUCT_PACKAGES += ForgeOSLauncher
PRODUCT_PACKAGE_OVERLAYS += device/forgeos/overlay
PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.software.freeform_window_management.xml:$(TARGET_COPY_OUT_SYSTEM)/etc/permissions/android.software.freeform_window_management.xml
# Keep upstream GSI kernel/vendor assumptions. No bootloader, super or userdata layout changes.
