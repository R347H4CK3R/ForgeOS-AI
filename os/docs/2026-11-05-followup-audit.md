# Firmware follow-up checkpoint
Read-only audit on 2026-11-05; no new APK install, DSU preparation/boot, reboot, unlock, wipe or flash.

The tablet remains on stock security patch2026-04-05 and AVB green. Android's dynamic_system binder service and /system/priv-app/DynamicSystemInstallationService/DynamicSystemInstallationService.apk are present. This corrects any inference that absence of a cmd gsi service proves absence of DSU. It does not prove that a locked device accepts the modified ForgeOS signer/image. No DSU install or boot was attempted.
No /avb, /system/etc/security/gsi or /product/etc/security/gsi directory was found. /product/etc/security/avb contains system_other.avbpubkey (520bytes); its presence is not proof that ForgeOS is trusted.

A fresh GitHub release inventory confirms the latest available TrebleDroid Android15 baseline remains ci-20250415. Its later ci-20250617 release is Android16. The recent Doze-off/crdroid_gsi_treble release2026-09-22 is crDroid12.12 / Android16 QPR2; the release's reference to Android15 VNDK does not make it an Android15 image.
The newer Android16 vanilla asset581368392 publishes size974151100 and SHA256 b0797c0c93b7fbfbae4cec0e7440fd6f0527abbbc891cc57ac186445d5ef5871. It was not substituted into the API35 pipeline. Publisher notes include DSU compatibility issues and manual init replacement; this is not accepted recovery or tablet compatibility evidence.
Sources:
- https://github.com/TrebleDroid/treble_experimentations/releases
- https://github.com/Doze-off/crdroid_gsi_treble/releases/tag/2026-09-22
- https://source.android.com/docs/core/architecture/bootloader/locking_unlocking
- https://source.android.com/docs/core/ota/dynamic-system-updates

Exact100135925/t402aa/TTA.31 authenticated full recovery firmware remains unavailable from the checked sources. Community requests for a bricked100135925 are not recovery downloads. The old base's patch date cannot be safely fixed by changing build properties; replacing it requires a verified newer source/base and renewed vendor, AVB, rollback and hardware acceptance.
Owner subsequently requested Strata iOS visual styling. That native interface work proceeds separately; it does not remove firmware gates.
