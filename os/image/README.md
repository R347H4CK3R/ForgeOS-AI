# ForgeOS experimental offline OS image
This pipeline produces an actual Android system filesystem, adapted from a pinned vanilla TrebleDroid Android15 ARM64 GSI. It is not a full source-built ROM, not stock onn firmware and not an approved tablet installation.

## Inputs and provenance
base.lock.json records the exact upstream release, asset, byte count, observed SHA256, source release and verified APK artifact. The base checksum was computed in isolated inspection run42032559043; the publisher did not supply it. It is a reproducibility pin, not independent publisher authentication.
Upstream: https://github.com/TrebleDroid/treble_experimentations/releases/tag/ci-20250415
Sources/manifest: https://github.com/TrebleDroid/treble_experimentations/releases/download/ci-20250415/manifest.xml
The framework is API35, with patch2025-03-05, older than the tablet's2026-04-05 patch. The base identifies itself as a userdebug/test-keys build. Its publisher reports Internet failures on some devices. This baseline is unsuitable as a secure production release.
Retain upstream licenses/notices inside the filesystem. The build-script repository's Apache license is not a blanket license for every image component. Android/AOSP and included third-party components keep their original terms. This modified image is not an official TrebleDroid release.

## Build
Dispatch the ForgeOS offline system image workflow on the review branch with operation=inspect or operation=build. Compilation, downloads, ext4 processing and compression run on GitHub, not GamingLaptop. A successful verified APK workflow must remain available, and current Android source must match that APK's source.
The adapter copies only the regular input file's ext4 filesystem bytes, retains existing content/ownership/modes/xattrs, inserts the native APK in /system/app/ForgeOS, assigns root:root0644/system_file metadata, adds or preserves the freeform feature, and writes /system/etc/forgeos/build.json.
The base's real config_supportsMultiWindow resource is checked before building. Android15 uses the package-manager freeform feature; obsolete config_freeformWindowManagement is not injected. No unvalidated RRO is shipped.
The original input remains unchanged. No implicit filesystem/partition expansion is allowed. Existing framework/build.prop contents and metadata are checked after adaptation; the result must pass read-only e2fsck.
Outputs include a compressed raw ext4 image, hashes, input pins and inspection/build evidence. Original AVB footer/hash-tree/vbmeta bytes are omitted because modifications invalidate them. No trusted AVB signing is claimed. No boot/vendor/super/userdata image or flashing script is generated.

## Acceptance restrictions
EXPERIMENTAL-DO-NOT-FLASH.
Offline fsck/hash/metadata verification does not prove boot, HAL/VINTF/SELinux runtime, Wi-Fi, Bluetooth, graphics, audio, storage or window controls on onn100135925.
The exact stock restore package, recovery path, snapshot merge state, AVB trust/rollback and physical compatibility must be established first. Virtual A/B is not true dual boot.
Existing owner design/unlock/flash/conditional-erasure approval is recorded. Exact firmware, tested recovery and image acceptance gates remain unmet. This offline workflow performs no device unlock, wipe or flash.

## Signed repository delivery
The Settings / OS updates screen checks the latest successful explicit ForgeOS build. It verifies an exact-byte RSA3072/SHA256 catalog with the pinned public key, validates repository/API35/ARM64/sequence and signed byte/hash limits, then stages a candidate on validated unmetered Wi-Fi. Rotation, cancellation and foreground-service ownership are guarded. Export uses Android's document picker. There is no firmware install/apply command.
The private repository requires an Actions-read token entered for the session only. Tokens are not stored or sent to redirected artifact hosts. Signing private keys remain outside source and APKs, in owner-controlled files and encrypted Actions secrets. The public catalog key and separate app signing certificate are tracked.
The image's ordinary preinstalled APK is re-signed with the stable certificate SHA256 4e17359b376fc57b4d39c4479c23e8c786d93b621ac33d7eaed3114287914c9a. This grants no platform/AVB trust. Replacing an incompatible debug-signed package is not a reason to uninstall user data.
The image artifact contains only the XZ image. A separate signed catalog binds its artifact ID, actual GitHub ZIP digest/size, XZ digest/size and raw image digest/size. Evidence and the stable APK are separate artifacts. Artifacts expire after seven days; an expired latest build fails closed and requires a new verified build, rather than silently selecting an older candidate.
Actual integrated results are recorded in ../docs/OS_REVIEW.md. Seamless OTA application is not implemented: a device-specific trusted payload, update-engine integration, recovery/rollback evidence and separate installation authorization are still required.

## Quota-independent native APK inputs

Current APK pins may use immutable draft release_id and release_asset_id plus run_id/run_attempt, archive size/digest and exact APK source/hash/size. Drafts are retrieved by ID: GitHub release-by-tag lookup does not return unpublished drafts. The actual checkout recorded in bounded signing evidence must equal the pinned successful workflow head. Only the fixed APK entry is extracted; legacy Actions artifact pins remain supported. This does not change system-image artifact delivery or establish boot acceptance.

Dispatch operation=verify-apk for the lightweight authenticated Actions handoff check without downloading or adapting a GSI. Native source4f50024 / run42337427732 is pinned after all build/test jobs passed. The current installed stable APK matches its bytes. Production OTA remains unimplemented.
