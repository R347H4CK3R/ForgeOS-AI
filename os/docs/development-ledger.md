# Development ledger
Updated 2026-11-05 UTC. Baseline b2569c0 was a WebView shell. Original working tree, existing build outputs and existing tablet app were preserved. Worktree: ForgeOS-Native; branch: forgeos/native-workstation; draft PR1.

## Development milestones
Native HOME dashboard, provider-backed editor/workspace, lifecycle/race guards, JNI GGUF inference, supported Termux handoff, HTTPS host bridge, bounded tool agent and native verified downloader are implemented.
61de690 integrated the manager, agent UI and dashboard.
c90ffdd pinned an independently verified extra Ollama manifest layer after the integrity test rejected it.
b18e1a4 captured bounded synthetic backend errors and waited for landscape composition.
5b13bd6 constrained backend tool output with a closed schema and deterministic sampling.
757e8a0 added next-step guidance and a regression-proven repeated-call stop after the 0.5B model looped on reads. Acceptance moved to pinned remote-only 1.5B weights.
6bc8fd7 introduced a window-controller startup NPE. Actual emulator logs identified the premature call; 74dd5ef moved it after setContentView and passed all jobs.
bf5ac33 corrected snapshot timing after visual inspection found stale/blank frames despite passing UI interactions.
cf01a34 matched the window backing to light/dark themes. Final screenshots were visually inspected.

## Final evidence
[Actions 41978243888](https://github.com/R347H4CK3R/ForgeOS-AI/actions/runs/41978243888), source cf01a34edddb5466643e09229f33554f7662e8d4: all three jobs successful.
8 Android unit tests; 8 native integration tests in 37.863 seconds; real 1.5B read/propose/confirmed apply in 33.181 seconds. Lint: 0 errors / 62 warnings.
Windows bridge: 29 discovered, 28 passed, 1 live skip. Linux normal suite: 27 passed / 2 expected skips; live job separately passed.
APK: 6,722,453 bytes; SHA256 c6ac7a200f9fc45f2f18d5e141c01b71345026a4588d6130909db015e5b36dea.
See UI_REVIEW.md, verification-matrix.md and evidence/.

## Decisions and limits
The review APK dev.forgeos.ai.review is separate from the older app. Stock home and data remain intact.
SAF is the supported workspace interface. Adopted-card app access is not established; no path bypass/migration.
Local inference is CPU-only, two threads, context512/output128, with model/RAM guards. No target performance claim.
The host agent has no arbitrary shell/project-test execution. Python diagnostics parse syntax. Writes require a separate exact-plan approval; conflict detection is optimistic.
The 1.5B live fixture is a small edit, not proof of general repair. Repeated-call stopping may return an incomplete plan for review.
Experimental AOSP files are not a verified OS image. The laptop's observed disk/RAM conditions were inadequate for AOSP; no heavy host build or WSL session started.
Exact firmware/recovery and DSU trust remain unverified; Virtual A/B is not dual boot.
Termux overlapping results share a last-result slot; output-truncation usability remains limited.
Stable app signing and catalog signing are now provisioned separately for the OS pipeline; do not erase existing app data to replace mismatched debug keys.

## Review
Independent reviews identified and helped resolve provider races, stale callbacks, UI-thread work, cancellation reset, Windows aliases/junctions and mutable pending plans. Final source review found no important correctness/security blockers in the schema, loop guard and window-controller fixes. UI captures were additionally inspected manually.
No new APK install, image boot, unlock, wipe, partition change, SD format/migration or flash was performed on the tablet.

## OS phase:2026-11-05
Pinned TrebleDroid Android15 vanilla ARM64 base after actual isolated inspection. Built a real ext4 system image, retaining source bytes and metadata, omitting invalid upstream AVB data, and integrating the ordinary native app plus validated freeform feature. It remains an old-patch userdebug experiment, not accepted target firmware.
Signed native updater adds exact catalog verification, bounded unmetered download/export and process/lifecycle ownership. Independent review drove fixes for stale workers, failed publication, service generations and older-artifact fallback.
Workflow42040373607 passed10instrumentedtests but failed lint on a numeric orientation constant. b4edfcf used the named constant; workflow42041556381 passed. Visual review found a separate Quickstep ANR obscuring an earlier screenshot despite test assertions. 5d52b06 raised emulator RAM to3GB and made preview capture reject visible system error dialogs; workflow42043033635 passed26unit/10integrationtests with clean inspected updater preview.
353560b extends APK same-source validation to build scripts/workflow/attributes; actual Git fixtures and25Linuxpipeline tests pass in42045844737. OS-only changes skip unnecessary app/model rebuilds. Stable app/certificate and separate catalog keys are owner controlled outside source/app and provisioned as encrypted Actions secrets. No target writes, installs or bootloader operations occurred.
See OS_REVIEW.md for the final integrated image run and immutable hashes.

## Strata-style revision
User requested Strata iOS visual styling after the firmware follow-up. Original native Canvas wallpaper, app glyphs, clock/workspace widgets, a centered glass dock and shared tool surfaces were implemented. Source review caught a missing pre30 system-bar guard and suffixed-package theme preferences; both corrected. Actual lint rejected three raw Typeface constants; named constants corrected. The first emulator preview passed10tests, and visual review narrowed the initially full-width dock.
Final native source10e2d26 passed all jobs in42054065879:26unit tests,10emulator tests in72.067seconds, lint0errors/65warnings, real local model/download and remote-model regression. Final screenshots were inspected.
Image source df21a82 pins that exact APK. Integrated image/signing/catalog run42056016871 passed; independent downloaded catalog, artifact and signed APK cross-checks passed. Raw image SHA256 c327a91573677530f0c08e82126500114b1046ae5c83d1ee08bb7cff30b3ca9c. Stable APK SHA256 e728a652d30a6b9cbd7ea376a0b610c81b54c9b5ee15d1d64a51ddb05a2c51dc.
No new installation or tablet boot was performed. UI styling does not resolve the older base patch, exact recovery firmware, AVB or physical acceptance.
