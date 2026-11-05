# ForgeOS development ledger — 2026-11-04
Baseline b2569c0, WebView shell. Existing untracked build-output/build-v02 were preserved; isolated worktree ForgeOS-Native and branch forgeos/native-workstation.
- 35f33ef: workspace contract tests and validation pipeline.
- 3ac9ea2: native dashboard, document editor, workspace source.
- 6f19e74: removed unavailable legacy SDK tools package from setup.
- 2f0c37f: corrected document-picker syntax; bounded loopback host bridge.
- 4bd04e0: emulator navigation/recreation and screenshot tests.
- e28ca70: local JNI inference, remote HTTPS client, actual HTTP tests.
- f040da8: adapted pinned llama model-load API, cancellation and lifecycle guards; live hardware/recovery evidence.
- a72f235: background provider operations and slow-open/save regression.
- 249b78c: pinned real-model test, split ABI jobs, third-party license.
- bac9192: reviewed opt-in Termux command integration and persistable URI grant corrections.
- e858c21: emulator shell root-cause correction; experimental AOSP product source.
## Verification evidence
GamingLaptop: 9 bridge tests passed. Earlier Android policy suite: 3 passed. bac9192 ARM64 job in run41965275757 succeeded: tests/lint/APK build/signature/checksum. Its UI job failed because the emulator runner executed every script line in a separate shell; a standalone shell script corrected the working-directory dependency.
Current run41965711640 validates e858c21; await actual UI/inference results.
## Decisions and limits
Ruling: Keep the new native launcher as the reviewable interim environment, separately labeled from the OS product — locked bootloader, exact recovery absent — cost: no replacement operating system yet.
Ruling: Use persisted SAF grants instead of raw adopted-volume paths — ordinary app access to private adopted storage is not established — cost: SD workspace remains blocked until a valid exposed folder/application placement is tested.
Ruling: Native inference is CPU-only, 2 threads, 512 context, 128 output tokens, file-size/RAM guard — limited 3GB tablet memory — cost: short prompts and modest model capability.
Ruling: Bridge exposes no shell/write endpoint and is not auto-started — prevent unreviewed remote destructive operations or gaming load — cost: build automation and remote editing remain incomplete.
Ruling: Use AOSP product integration without fabricating vendor/kernel blobs — preserve hardware assumptions — cost: no built or verified onn system image.
Ruling: No AOSP build on GamingLaptop — observed about 128GiB free disk, 16GB total RAM and under1GB free at an observation, below AOSP400GB disk guidance — cost: a suitable isolated Linux worker is still needed.
## Review
Independent reviewer identified document-operation races, stale activity callbacks, UI-thread provider work and pre-entry cancellation reset. Corrections committed; tests include a real delayed-IO load/save sequence and cancelled-before-native-entry check.
Deferred minor: concurrent Termux results share one last-result slot without command correlation.
Deferred minor: original Termux stdout/stderr truncation lengths are not displayed.
No unlock, boot, image install, APK install, storage migration, format or flash was performed on the tablet.
