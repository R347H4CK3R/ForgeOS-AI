# ForgeOS AI
Native Android workstation development and experimental AOSP GSI product integration for onn 100135925 / t402aa.

**This repository does not yet provide a verified onn operating-system image or true dual boot.** The native review APK runs on stock Android. The owner approved the Strata-style design and authorized unlock/flash/wipe if necessary while requiring stock Android preservation. The separate native review APK is installed on stock Android; no alternate Android system image has been booted or flashed. A real Alpine Linux guest boots through AVF/GenieZone as the clearly labeled interim environment; this is not true dual boot. Recovery, USB/bootloader and compatibility remain technical gates.

## Implemented source
- Native Strata-style HOME dashboard, clock/workspace widgets, responsive app icons, floating glass dock and matching tool panels; no WebView runtime, bitmap wallpaper or blur pass.
- Native UTF-8 editor with open, save, save-as, draft recreation, bounded reads, overwrite confirmation and load/save exclusion.
- Android document-provider workspace access, directory browsing, project-folder creation and eight workspace directories.
- Light/dark launcher themes and responsive scrollable portrait/landscape views.
- On-device GGUF runtime through pinned llama.cpp JNI: CPU only, two threads, 512-token context, 128 output tokens, 550 MiB file limit, 600M parameter limit and available-RAM checks. Model selection uses a persisted document grant. Real 135M generation passes in the isolated emulator and directly from the SD card on the tablet. One short prompt produced an off-topic reply; general instruction/coding quality is not established. A native Wi-Fi-only download manager verifies the pinned starter model SHA256 before selection.
- Trusted native environment controls for the independently verified Podroid companion. Actual AVF Linux boot, Python unittest, Git init/add/commit, restart persistence, native split-screen and touch divider resizing pass on the tablet. Use 512 MiB and one CPU; explicit QEMU maintenance supplies networking, then AVF runs the installed tools offline. AVF Internet and seamless restart reliability remain limited; see the recovery notes.
- Opt-in Termux terminal integration: review every command, then execute via the supported RUN_COMMAND service and receive results privately. Python/Git/SSH packages and Termux configuration are separate prerequisites.
- HTTPS remote AI/development client with memory-only session token; authenticated, loopback-only Python host bridge for workspace reads, project listing, fixed Git status and an already configured Ollama service.
- Android resizable activity support and adjacent launch requests. A real ForgeOS/Podroid split pair and touch resizing are verified; floating-window behavior, snapping and desktop window controls remain unverified.
- Actual experimental Android15 ARM64 system filesystem adapted offline from a pinned vanilla TrebleDroid GSI; verified native workstation integration, freeform feature declaration and filesystem checks. See [image pipeline](os/image/README.md). The full-source AOSP product remains experimental.
- Signed repository OS update catalog, native check/download/hash verification and export. Staging only: seamless OTA application is not implemented. Private repository access uses a memory-only session token; signing keys are never embedded.

## Build
JDK17, Android SDK35, NDK27.2.12479018, CMake3.22.1 and Gradle8.11.1.
Before building, check out ggml-org/llama.cpp revision 8bed7cfb999cebc03d2cc5b78911026a8d57549a into android/app/src/main/cpp/llama.cpp. The GitHub workflow does this reproducibly.
From android: `gradle --no-daemon --max-workers=2 testDebugUnitTest lintDebug assembleDebug`.
Default ABI is arm64-v8a. Emulator build uses `-PforgeAbi=x86_64`.
Host tests: `python -m unittest discover -s bridge -v`.
Actions produces APK, signature check, SHA256, unit/lint reports, isolated emulator interaction results and UI PNGs in bounded unpublished draft review bundles, avoiding Actions artifact quota. Source identity and actual workflow results remain mandatory; draft presence is not success. A pinned SHA256-checked 135M GGUF is used for actual emulator generation; it is not bundled in the APK.

## Verification and installation gates
- [Native0.3: direct SD inference, exact deployment and live Linux split-screen screenshots](os/docs/NATIVE_03_REVIEW.md)
- [Actual AVF/Linux guest, network maintenance fallback and recovery limits](os/docs/AVF_INTERIM_REVIEW.md)
- [Android16 second-system candidate, actual backup and physical preflight](os/docs/SECOND_SYSTEM_REVIEW.md)
- [Recorded authorization and stock-preservation requirements](os/docs/AUTHORIZATION.md)
- [Strata-style UI: actual dark/light/landscape previews and tests](os/docs/STRATA_UI_REVIEW.md)
- [Experimental OS image, signed updates and exact artifact hashes](os/docs/OS_REVIEW.md)
- [UI review and proposed tablet test](os/docs/UI_REVIEW.md)
- [Deliverables and evidence](os/docs/deliverables.md)
- [Hardware audit](os/docs/hardware-audit.md)
- [Verification matrix](os/docs/verification-matrix.md)
- [Recovery and installation gates](os/docs/recovery-and-installation-gates.md)
- [Experimental AOSP integration](os/aosp/README.md)
- [Remote bridge](bridge/README.md)
The approved mixed SD layout exposes a public FAT32 ForgeOS workspace and a separate adopted f2fs volume. Native folder creation, read/edit/save, persistent access across app restart/update and direct SD GGUF inference are verified. The FAT32 workspace is unencrypted; the Linux guest disk and Android primary shared storage remain internal. Full tablet-reboot access persistence and Linux-to-SD interoperability are unverified.
Exact stock recovery firmware, AVB trust, a tested restore procedure and target hardware acceptance remain prerequisites for an OS release. Offline GSI adaptation runs successfully on GitHub; a full-source AOSP build still needs a much larger worker. The experimental base has an older 2025-03-05 security patch and userdebug/test keys; it is not a production-safe release.
The bounded repair agent and model download manager are implemented. Complete window controls, full file operations, target tailnet inference and physical-peripheral acceptance remain incomplete. See [actual UI and software review](os/docs/UI_REVIEW.md).
