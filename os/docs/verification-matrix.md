# ForgeOS verification matrix
Evidence: 2026-11-06 UTC, native software candidate10e2d26; legacy OS image source df21a82 / Android16 candidate source e077fbc. This is a development review, not OS acceptance.

| Feature | Classification | Evidence / missing proof |
|---|---|---|
| Custom OS boot | Blocked | Locked AVB; exact recovery firmware and tested restoration unverified; candidate not boot-tested; existing approval recorded |
| Touch input | Partially working | Actual UI responds to ADB-injected taps; finger/multitouch acceptance pending |
| Native multi-window | Partially working | Actual target split placement and divider resize observed; Termux pane black in captures, full acceptance unresolved |
| Floating-window controls/snapping | Not implemented | No verified privileged window manager/image |
| Wi-Fi | Verified working | Wireless ADB and 3/3 Internet ping; throughput/roaming not measured |
| Bluetooth | Partially working | Enabled and supported; pairing and input untested |
| USB peripherals | Blocked | Host feature present; actual peripheral acceptance not performed |
| External display | Blocked | No physical HDMI evidence; virtual display is not DP Alt Mode proof |
| Adopted SD mount | Verified working | Approved card erase created new public FAT32 and private f2fs volumes; both mounted. Prior119G observation is historical; see SD_CONVERSION_REVIEW.md. |
| ForgeOS SD access | Partially working | Installed APK and private data on adopted UUID; visible SAF workspace/model placement unverified; debug run-as does not resolve adopted path |
| Local AI | Partially working | Actual pinned 135M GGUF generates on ARM64 tablet; useful coding quality, peak memory and throughput unverified |
| Model downloads | Partially working | Actual tablet download, independent SHA256 and handoff pass; downloaded model on internal shared storage, SD model placement pending |
| Remote AI | Partially working | Real 1.5B worker test passes; tablet HTTPS session unverified |
| Tool-calling repair | Partially working | Real read/propose/approved apply; syntax repair and permission regressions; complex-project behavior unverified |
| Agent project test/build execution | Not implemented | Python syntax parsing only; no arbitrary generated-code execution |
| Code editor | Partially working | Native edit/recreation and real-file race tests pass; target untested |
| Terminal | Partially working | Supported Termux request/confirmation/result implementation; actual configuration pending |
| Python execution | Blocked | Host syntax checks verified; tablet Python package/execution unverified |
| Git operations | Partially working | Host Git used, bounded status endpoint implemented; tablet Git unverified |
| SSH / remote terminal builds | Not implemented | No integrated arbitrary host command/session endpoint; Termux setup unverified |
| Tailscale | Partially working | Tablet-to-laptop tailnet ping 3/3; app HTTPS setup pending |
| Desktop Commander | Partially working | Authorized laptop integration used; app bridge is separate, no Android agent invented |
| File/project management | Partially working | SAF browsing, folder creation and editor import/export; broader move/delete operations incomplete |
| Recovery / rollback | Blocked | Normal bootloader-to-stock return verified; partial shared-files backup verified; app-private data/keys and authenticated exact firmware/tested restoration absent |
| Temporary official DSU test | Failed | Internal allocation had responsiveness kills; SD route selected correctly but native gsid was denied block-device stat access. No guest boot. See [SD test](SD_CONVERSION_REVIEW.md). |
| Genuine dual boot | Blocked | Separate DSU candidate built, but no alternate-OS boot-and-stock-return cycle or permanent boot selector verified. Virtual A/B is not dual boot |
| Experimental GSI system image | Partially working | Actual offline ARM64 filesystem built and fsck/metadata checked; target boot/AVB/vendor acceptance blocked |
| Full-source AOSP image | Not implemented | Product source only; large build worker and vendor acceptance unresolved |
| Repository OS updates | Partially working | Pinned RSA-signed metadata, bounded Wi-Fi download, verification/export and lifecycle tests; full native artifact transfer and target session unverified |
| Seamless OTA application | Not implemented | No trusted device-specific payload, platform signing or tested recovery path |

## Current second-system and physical preflight
[Android16 candidate42066524982](https://github.com/R347H4CK3R/ForgeOS-AI/actions/runs/42066524982) passed36Linux tests plus actual developer AVB/hash-tree/signature/prefix/fsck/gzip verification. Patch2026-09-01 matches system/AVB descriptors. This is not OEM trust or target boot proof.
The native review APK installed on the actual adopted code/private-data volume; process runs and stock HOME remains com.android.launcher3. Actual native UI/download/handoff/local generation pass; native split placement/resize observed with black Termux pane. Initial Windows driver Code28 resolved with exact matching Microsoft-signed package; actual fastboot metadata read, then stock return verified. After OEM setting PIN confirmation, permission1 and exact vendor unlock_ability16777216 were observed; bootloader remains locked. See [post-PIN evidence](OEM_POLICY_REVIEW.md). See [physical evidence](PHYSICAL_USB_REVIEW.md). Shared-file backup:47files/335,610,967bytes verified; app-private data and adopted keys excluded. See SECOND_SYSTEM_REVIEW.md.

## Earlier visual and integrated image evidence
[Native workflow42054065879](https://github.com/R347H4CK3R/ForgeOS-AI/actions/runs/42054065879): all jobs passed;26 unit tests,10 emulator tests in72.067seconds, actual local135M and remote1.5B checks, lint0errors/65warnings. Dark/light/landscape, editor, model, remote and updater screenshots were inspected.
[OS workflow42056016871](https://github.com/R347H4CK3R/ForgeOS-AI/actions/runs/42056016871): integrated Strata-style APK, stable signer, clean ext4, exact payload metadata and signed catalog passed. Downloaded catalog signature/artifact identity/APK hashes independently cross-checked. Hardware classifications below are not upgraded by this build.
See [UI preview](STRATA_UI_REVIEW.md) and [exact image evidence](OS_REVIEW.md).

## Previous updater software evidence
[Workflow 42043033635](https://github.com/R347H4CK3R/ForgeOS-AI/actions/runs/42043033635) passed all jobs: 26 Android unit tests (0 failures), 10 instrumented emulator tests, ARM64 APK verification, actual remote-model integration and lint (0 errors / 64 warnings). The emulator uses 3 GB RAM; screenshots reject visible Android crash/ANR dialogs. 25 OS pipeline tests passed on Linux, including real ext4 adaptation, signing/catalog and malformed-artifact checks.
The OS-only pin commit proved build scoping: workflow42045236220 skipped all three heavy app jobs after its scope check.

## Earlier workstation evidence
[Workflow 41978243888](https://github.com/R347H4CK3R/ForgeOS-AI/actions/runs/41978243888) completed successfully on cf01a34: ARM64 build, signature/hash checks, 8 Android unit tests, lint (0 errors / 62 warnings), 8 emulator integration tests in 37.863 seconds and actual remote 1.5B integration in 33.181 seconds.
Windows suite: 29 discovered / 28 passed / 1 live skip. Linux suite: 29 discovered / 27 passed / 2 platform/live skips; the separate live job passed.
Actual screenshots, test output and synthetic model evidence are linked in [UI review](UI_REVIEW.md).

## Scope
A successful build is not target verification. The remote model fixture establishes one small edit, not universal repair. Native review APK installed on stock Android after design approval; no OS boot, unlock, wipe, partition change, SD reformat or migration performed.
