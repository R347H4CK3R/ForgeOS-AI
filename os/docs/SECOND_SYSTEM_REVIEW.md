# Android16 second-system and hardware preflight
Owner design, unlock/flash and conditional wipe authorization is recorded in [AUTHORIZATION.md](AUTHORIZATION.md). Stock Android must remain available. This is not yet verified dual boot.

## Newer baseline: actual offline results
[Inspection42062908036](https://github.com/R347H4CK3R/ForgeOS-AI/actions/runs/42062908036) verified the pinned community vanilla Android16 QPR2 / crDroid12.12 image:
- API36; actual system and AVB security patch2026-09-01, newer than stock2026-04-05.
- user/release-keys properties; community image, not an official Google build despite its retained upstream fingerprint.
- Raw input SHA2565595851f2f6a1f341000d085276bbe28ca584f25ab04236270ece828c27005d0;2,934,730,752bytes.
- ext4 filesystem2,888,343,552bytes;32MiB free before injection. Multi-window resource true; runtime controls unverified.
This replaces the old security baseline for the new candidate evaluation. It is not a complete CVE audit or proof of vendor security.

[Candidate build42066524982](https://github.com/R347H4CK3R/ForgeOS-AI/actions/runs/42066524982) passed36Linux tests and actual image construction/signing/verification:
- [Android16 second-system candidate13433059297](https://github.com/R347H4CK3R/ForgeOS-AI/actions/runs/42066524982/artifacts/13433059297)
- [Public evidence13433154313](https://github.com/R347H4CK3R/ForgeOS-AI/actions/runs/42066524982/artifacts/13433154313)
- Stable-signed native workstation from verified source10e2d26 is integrated as an ordinary app.
- New developer SHA256_RSA4096 hashtree/footer verifies against pinned public key50d95733f9b562b115b6ddebbebc17ac809a1fe972351017f313630dbd5cb61b. No verification-disabling flags. The original input and exact filesystem prefix remain unchanged; read-only e2fsck passes.
- Raw candidate:2,955,452,416bytes; SHA256d20bf46729bb99447cda51b3c4e5ea5680e68ac6e4c60c7148f535e65f11b164.
- Gzip transport:1,436,550,186bytes; SHA256d7312eacbf461a50e2b331134c09bb41949854c1eada0a23f5006ecf035b27e4. Decompression was rehashed on the worker and matches the signed raw image.
- GitHub ZIP SHA256a388c0255ca57f7a913154dec16b4433eadba8d870d16ad992714dbdce77c76b.
No1.44GB image download or heavy build was run on GamingLaptop. Only small evidence was downloaded. Artifacts expire after7days.
Developer signing proves integrity, not OEM trust. Actual boot and hardware rollback behaviour remain unverified. No physical rollback index was changed by this offline build.
The older Android15 stage-only update feed is retained separately. This Android16 candidate is deliberately not published through that API35 feed; native API36 update integration is not completed.

## Dual-system route and limits
Android DSU is the candidate route for a separate system while leaving physical stock boot/system/vendor partitions intact. It is a temporary alternate-system facility, not established permanent dual boot or a tested boot menu. Virtual A/B is not used as a dual-boot mechanism.
Stock DynamicSystem binder and installation package exist; OEM-lock service and Developer Settings activity resolve. The device remains locked/AVB-green. Unlock availability is not established from a missing build property or a denied protected-service read.
Only wireless ADB is currently observed; there is no verified USB/fastboot connection. Rebooting to the bootloader would lose this wireless session.
Android DSU cannot use adopted SD media as backing storage. Current internal free space is6.1GiB. The default8GiB DSU data image plus this2.96GB system and transport requires substantially more space; reserve about14GiB for a conservative setup. The SD card was not reformatted. Its encrypted contents are not automatically shareable between independent OS data environments.
Official reference:https://source.android.com/docs/core/ota/dynamic-system-updates

## Actual stock-Android hardware preflight
The exact stable APK SHA256e728a652d30a6b9cbd7ea376a0b610c81b54c9b5ee15d1d64a51ddb05a2c51dc was independently rehashed and installed successfully as dev.forgeos.ai.review. It is separate from the original app.
Package code and private data resolve under adopted volume18f27593-ebdf-491b-a706-6ce6e031255e. The app process runs; one background snapshot was44,558KiB PSS /94,404KiB RSS. This is not an inference benchmark.
The default HOME role remains com.android.launcher3. Stock kernel is6.6.102-android15; system security patch remains2026-04-05. No firmware was changed.
The secure lock screen is still showing. Physical UI, local model generation, finger/multitouch and window testing await normal device unlock. A debug run-as probe cannot resolve this adopted app's data path; that is not proof the app lacks storage permission. SAF workspace/model placement and cross-environment SD access remain unverified.

## Backup coverage
A partial shared-files backup was created on GamingLaptop at C:\Users\Chris\ForgeOS-Backups\2026-11-05_10-13-57.
All47copied files,335,610,967bytes, match tablet SHA256 hashes. The initial3verification failures were command argument interpretation; corrected quoting and full rehash cleared all3. Original copies and initial manifest were retained.
This covers selected readable shared media/documents/download folders for the current user0. App-private data, accounts, adopted-storage encryption keys, other users' private data and firmware recovery partitions are NOT backed up. It is not a full-device restore image.

## Remaining gates
1. Unlock the normal tablet screen and connect a data-capable USB cable to GamingLaptop for safe on-device tests and read-only bootloader identification.
2. Obtain/authenticate exact100135925/t402aa/TTA.31 stock recovery firmware and establish a tested stock restore route.
3. Validate VINTF/HAL/SELinux/input/graphics/network and AVB/rollback behaviour, including returning to stock, before accepting any dual-OS result.
4. Resolve internal/portable storage allocation without silently formatting the adopted card or claiming its encryption keys are backed up.

No bootloader reboot/unlock, data wipe, firmware flash, DSU preparation/boot, slot switching or SD reformat/migration has occurred. Existing authorization will be used once the technical gates are satisfied; no repeated permission request is needed for already-approved actions.
