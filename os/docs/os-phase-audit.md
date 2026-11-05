# OS phase audit: 2026-11-05
Owner approved the native UI/review results and requested OS development. This does not authorize unlocking, wiping, repartitioning or flashing. No OS image is booted or installed by this phase.

## Read-only target observations
Stock fingerprint: onn/100135925/100135925:15/AP3A.240905.015.A2/TTA.31:user/release-keys. Security patch: 2026-04-05.
AVB green, flash locked1, vbmeta locked, verity enforcing. Active slot _b; dynamic partitions and Virtual A/B enabled.
Physical by-name entries include A/B boot, init_boot, vendor_boot, dtbo, vbmeta, vbmeta_system and vbmeta_vendor. No separate recovery partition name was present. super, metadata and userdata are present; modem/security/calibration partitions are not modified.
Mapper exposes system_b, system_ext_b, product_b, vendor_b, odm_dlkm_b, system_dlkm_b and vendor_dlkm_b, with verity and COW mappings. This is not proof of the current snapshot merge state.
Readable vendor VINTF manifest: version8.0, target-level202404, sepolicy202404, 31 fragment names. Vendor compatibility matrix requests system SDK35. Sample HALs: AIDL boot control and composer3 version3.
First API level34; vendor API property30. These properties are not a substitute for actual VINTF matching.
gsi_tool status reports normal; no image_running property; cmd gsi status reports no gsi service. Usable DSU support/signing is not established.

## Recovery result
No authenticated exact factory/OTA/recovery package for 100135925/t402aa/TTA.31 Android15 has been obtained or verified. Community lists contain other model numbers and misleading older-build claims. No partition-content dump, recovery reboot or restore attempt was made.
Source lead only: https://sourceforge.net/projects/onn-tablet-firmwares/files/

## OS implementation decision
Adapt a separately verified copy of a vanilla Android15 ARM64 GSI using offline ext4 tools on GitHub's worker. Preserve upstream hardware support, metadata and notices; add the native workstation as an ordinary preinstalled app, not system UID/privileged code.
The initial candidate is TrebleDroid ci-20250415, Android15_r26. Its publisher lists Internet failures on some devices and FOD/UDFPS failures. Its security patch is inspected before acceptance and must not be represented as current device security. It is an experimental baseline, not a safe release candidate.
Source: https://github.com/TrebleDroid/treble_experimentations/releases/tag/ci-20250415
Official Google GSI downloads have separate usage/distribution terms; do not assume those downloads are freely modifiable redistributable OS releases.
No upstream vbmeta, bootloader, vendor, super or userdata image is distributed with the modified filesystem. The modified image's trusted AVB chain, recovery and target compatibility remain unresolved.
