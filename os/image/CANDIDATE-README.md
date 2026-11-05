# Android16 second-system candidate: DO NOT FLASH
This offline candidate uses a pinned vanilla community crDroid12.12 / Android16 QPR2 image with inspected API36, actual security patch2026-09-01 and user/release-keys properties. It contains the independently verified native ForgeOS Strata-style workstation APK, re-signed with its stable app certificate. The source is not an official Google image, despite retaining its upstream fingerprint.

The system filesystem is adapted without replacing the original input. Upstream AVB metadata is removed during adaptation; a new SHA256_RSA4096 developer hashtree/footer is generated and verified against a pinned owner public key. Filesystem-prefix hash and read-only fsck checks must pass. FEC is not generated. No AVB disabling flags are used.
This provides internal image integrity, not OEM trust. The private developer key is outside source/app and in encrypted Actions secrets. The candidate requires an appropriate unlocked boot path; actual target acceptance remains unverified.

The .raw.gz artifact is a gzip transport of a non-sparse raw filesystem with its developer AVB trailer. It is not an OEM OTA package, boot image, super image, stock restore package or permanent dual-boot bootloader. No flash script or device command is supplied. It is deliberately outside the native API35 update feed.
The proposed second-system route is Android DSU, preserving physical stock system/vendor/boot partitions. DSU is a temporary alternate-system mechanism, not verified permanent dual boot. Vendor/kernel/VINTF/SELinux/graphics/input/network acceptance and returning to stock have not been tested.

DSU cannot allocate from adopted SD media. Its documented default needs approximately10GB free internally or suitable non-adopted storage. Current adopted card data/encryption must not be reformatted as part of this build. Bootloader unlocking normally factory-resets internal data and can destroy adopted-storage keys.
The owner approved the design and separately authorized unlock/flash/wipe if necessary, while requiring stock Android to remain available. Those authorizations do not make this untested candidate safe to boot. Exact authenticated stock recovery firmware, verified recovery, an actual USB bootloader connection and all compatibility checks remain technical gates.

All compilation/image processing/signing happens on an isolated GitHub worker. No tablet reboot, unlock, wipe, flash, DSU installation, slot change or SD operation is performed by the workflow.
