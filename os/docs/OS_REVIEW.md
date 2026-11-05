# Experimental OS image and signed update review
Current integrated build: [successful workflow42056016871](https://github.com/R347H4CK3R/ForgeOS-AI/actions/runs/42056016871), source df21a82bcb80dd69736d2d4242b88ab174a9080c.
This actual Android15 ARM64 system filesystem now includes the verified Strata-style native workstation. It adapts a pinned vanilla TrebleDroid GSI offline; it is not a full-source AOSP build or accepted onn firmware.

## Artifacts
- [ForgeOS-Android15-ARM64-EXPERIMENTAL-DO-NOT-FLASH](https://github.com/R347H4CK3R/ForgeOS-AI/actions/runs/42056016871/artifacts/13430415448)
- [ForgeOS-update-metadata](https://github.com/R347H4CK3R/ForgeOS-AI/actions/runs/42056016871/artifacts/13429926771)
- [ForgeOS-image-evidence](https://github.com/R347H4CK3R/ForgeOS-AI/actions/runs/42056016871/artifacts/13429562636)
- [ForgeOS-stable-signed-workstation](https://github.com/R347H4CK3R/ForgeOS-AI/actions/runs/42056016871/artifacts/13428468535)
Artifacts expire 2026-11-12T15:37:47Z. Expired latest metadata/artifacts fail closed; a new verified build is required.

## Exact identity
| Component | Bytes | SHA256 |
|---|---:|---|
| Raw ext4 | 2113343488 | c327a91573677530f0c08e82126500114b1046ae5c83d1ee08bb7cff30b3ca9c |
| XZ image | 774763916 | 1acb51160ed57f1baa25949c340c463d76ddc69f18a15e5243098076b373b384 |
| GitHub ZIP | 774764142 | c4f8b48dd478a6f127b8b442f329466317a3de30b0a2c0f1078feecae2c3b711 |
| Stable APK | 6767509 | e728a652d30a6b9cbd7ea376a0b610c81b54c9b5ee15d1d64a51ddb05a2c51dc |
Catalog version: 0.3.0-os.df21a82bcb80.42056016871. Separate stable app certificate SHA256: 4e17359b376fc57b4d39c4479c23e8c786d93b621ac33d7eaed3114287914c9a.
Downloaded catalog signature independently verified with OpenSSL: Verified OK. Actual GitHub archive metadata matches signed identity/digest/size. Downloaded stable APK matches receipt and injected payload evidence. XZ-to-raw decompression agreement, clean e2fsck, ownership/modes/SELinux labels and original-base preservation passed on the isolated worker. The775MB image was not downloaded to GamingLaptop.

## UI and test evidence
[Actual Strata-style UI review](STRATA_UI_REVIEW.md): native source10e2d26, successful workflow42054065879;26 unit tests and10 emulator tests in72.067seconds; lint0errors/65warnings; ARM64 verification and real remote1.5B integration passed. Real local135M generation/model download verification, editor recreation, navigation, shared theme and pinned RSA signature/tamper checks ran.
The reviewed home has native app icons, real clock/workspace widgets, blue/violet ribbon wallpaper and a centered floating dock. Dark/light/landscape, editor, remote, model manager and updater captures were inspected. No bitmap wallpaper, blur or continuous animation loop was added.
OS pipeline25tests passed, including actual ext4, malformed artifacts, signing/tampering and APK source provenance. OS-only integration workflow42056033341 skipped all heavy app/model jobs after scope validation.
Small public evidence is preserved under evidence/os-42056016871/. Earlier run42045228207 remains historical evidence, not the current UI image.

## Repository updates
Settings / OS updates verifies exact-byte RSA3072/SHA256 metadata before trusting signed artifact identity/bounds. Validated unmetered Wi-Fi transfer checks archive size/hash and extracted XZ size/hash; cancellation and verified SAF export are implemented. Private-repository token is session-only. Signing private keys are outside app/source and in encrypted Actions secrets.
This is staging only. Seamless OTA application, full real-image transfer in the native client and physical tablet session/storage acceptance remain unverified. There is no install, flash, unlock or wipe control. App signing grants neither platform privileges nor AVB trust.

## Hardware, recovery and installation gates
No new APK, image boot, unlock, wipe, partition change, SD format or migration was performed.
The base remains userdebug/test-keys with patch2025-03-05, older than stock2026-04-05; upstream reports Internet failures on some devices. Modified upstream AVB footer/hash-tree/vbmeta are omitted. This is EXPERIMENTAL-DO-NOT-FLASH and unsuitable as a production-security baseline.
Freeform resource/feature checks do not prove target floating windows, snapping, touch, graphics, audio, power, Wi-Fi, Bluetooth or USB preservation. Exact authenticated100135925/t402aa/TTA.31 recovery firmware, tested restoration, AVB/rollback and vendor/VINTF/SELinux acceptance remain unresolved. Adopted SD app access and wired display capability are unverified. Virtual A/B is not dual boot.
The stock DynamicSystem binder/package are present, but trusted ForgeOS DSU boot is not established; see [fresh audit](2026-11-05-followup-audit.md). No arbitrary Android16 image was substituted for API35.

No executable firmware installation procedure is safe to provide yet. Obtain exact authenticated stock recovery firmware, prove rollback/restoration, back up and restore-test user data/adopted-key dependencies, validate partition/snapshot/vendor/signing compatibility, then present exact image hashes and a device-specific procedure for separate owner authorization.
Earlier UI approval is recorded; the requested redesign has a new review preview. A separately scoped APK-only test must verify package/signer compatibility, preserve original home/old apps, and use a new workspace. Never uninstall user data to bypass a signing mismatch.
See [recovery gates](recovery-and-installation-gates.md), [verification matrix](verification-matrix.md) and [deliverables](deliverables.md).
