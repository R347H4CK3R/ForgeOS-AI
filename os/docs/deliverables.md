# Deliverables and evidence

Review branch: [forgeos/native-workstation / PR1](https://github.com/R347H4CK3R/ForgeOS-AI/pull/1). The operating-system mission remains incomplete: exact stock recovery firmware, alternate Android image boot and genuine dual boot are blocked. Native0.3 and the real Linux interim environment are verified on the tablet.

| Requested deliverable | Current result | Evidence / next gate |
|---|---|---|
| Complete source repository | Native workstation, constrained bridge, signed staging updater and GSI adapter delivered; full OS hardware acceptance incomplete | PR1; native artifact source4f50024; current branch contains image-input integration fixes |
| Native Android launcher | HOME-capable native0.3 APK installed and physically exercised; stock HOME retained | [Native target review](NATIVE_03_REVIEW.md), exact deployment hash and screenshots |
| Integrated workstation | Native SD editor/workspace/model management plus actual AVF Linux Python/Git; Termux and broader workflows remain partial | [Verification matrix](verification-matrix.md), [Linux acceptance](AVF_INTERIM_REVIEW.md) |
| Working multi-window | Actual native/Podroid split pair, touch divider resizing and live Linux terminal verified | Physical screenshots/activity bounds; arbitrary freeform/snapping/window controls incomplete |
| Connectivity | Stock Wi-Fi and tablet/laptop tailnet ping verified; QEMU guest package downloads work | Bluetooth/USB/HDMI and tablet HTTPS inference remain unverified; AVF guest Internet unavailable |
| Local AI runtime | Actual135M GGUF generation directly from SD, persisted grant and granted-FD regression pass | Greeting fixture passes; another prompt was off-topic. General coding/model quality incomplete |
| Remote development bridge | Constrained authenticated bridge and real1.5B worker planning/approved apply tests | [Bridge source](../../bridge/README.md); target HTTPS setup and arbitrary remote sessions incomplete |
| GitHub Actions | Android/native builds, unit/lint, real local/remote models, UI tests and quota-independent draft bundles pass | [Native run42337427732](https://github.com/R347H4CK3R/ForgeOS-AI/actions/runs/42337427732): all five jobs passed |
| Hardware report | Actual physical ADB, storage, native multitasking and AVF execution documented | [Hardware audit](hardware-audit.md), native/guest reviews; physical peripherals remain deferred |
| Recovery/rollback docs | Current stock integrity and tested companion restart workaround documented | Exact firmware/full restoration still blocked; [firmware gates](recovery-and-installation-gates.md) |
| Buildable OS image | Experimental ARM64 Android15 and Android16 system filesystems built and verified offline previously | [Android16 run42066524982](https://github.com/R347H4CK3R/ForgeOS-AI/actions/runs/42066524982), artifact13433059297; not OEM trusted or boot verified. New native input is pinned for future builds |
| Repository OS updates | Signed discovery/download/verification/export staging implemented | Seamless OTA and trusted device-specific application remain unimplemented |
| UI/verification review | Actual target SD inference, native home, environment controls and live Linux split screenshots delivered | [Native0.3 review](NATIVE_03_REVIEW.md), [AVF review](AVF_INTERIM_REVIEW.md), earlier [light/dark/landscape review](STRATA_UI_REVIEW.md) |

Native run42337427732 passed12 instrumented tests in83.051seconds. Lint reports0errors/69warnings. The stable-signed APK is byte-identical to the physically tested installation (ff412bbe95561b91a925c09f9d73c3f5bb7ca60ac5e66d34a3485f9351142600). Windows image-tool suite:51discovered,47passed,4Linux/platform skips; CI-script suite:20discovered,19passed,1Windows symlink skip. Separate Linux CI runs the platform cases.

The owner approved design and authorized unlocking/flashing/conditional erasure. Technical firmware/recovery gates remain unmet. The earlier explicitly approved SD erase produced the current mixed public/private card layout; no new erase, bootloader unlock, internal wipe or firmware flash occurred during this VM/native repair. The Linux guest is not true dual boot. Approximately1.93GiB unexplained internal free-space difference remains separate from the verified100.6MiB model-copy cleanup.

The exact image-builder APK handoff also passed with the Actions token in [run42339071655](https://github.com/R347H4CK3R/ForgeOS-AI/actions/runs/42339071655). Current source pins use immutable release and asset IDs; no GSI image was generated or booted in this handoff check.
