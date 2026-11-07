# AVF interim environment implementation plan

> For agentic workers: use superpowers:executing-plans inline. Steps use checkboxes.

**Goal:** Deliver a verified VM environment through the native workstation while preserving stock Android.
**Architecture:** Pin and inspect the independent Podroid companion; prove its AVF Linux guest on the actual tablet, then add an honest native environment entry.
**Tech stack:** Android Java, Python standard library, GitHub Actions, stock AVF/GenieZone.
**Spec:** os/docs/AVF_INTERIM_REVIEW.md

## Global constraints
- Genuine dual boot remains a separate blocked deliverable.
- No new firmware writes, unlocks, wipes or SD formatting.
- Heavy builds run in Actions; no driver changes or compilation on the gaming laptop.
- Companion APK SHA256 and Android signature verified before installation.
- Use 1 vCPU and 768 MiB; do not silently count QEMU as hardware AVF.
- Existing approvals and autonomous inline execution apply.

## Review focus
- Same-size APK tampering must be rejected.
- A malformed ZIP or missing ARM64 binary must be rejected.
- Companion absence or wrong signer must not become a dead launch control.
- Missing VM development permissions must be distinguishable from running state.
- A guest readiness message is not evidence of Python/Git/network/SD interoperability.

## Task 1: Pinned dependency verification
Files: os/vm/sidecar.lock.json; os/vm/verify_sidecar.py; os/vm/tests/test_sidecar.py; .github/workflows/vm-compat.yml; .github/workflows/os-image.yml.
Interface: validate_pin(dict) and verify_apk(Path, dict) return hash-only evidence; package and signature proof come from actual aapt/apksigner.
- [ ] Write tests for valid archive boundaries, same-size tamper, wrong size, untrusted URL and missing ARM64 content.
- [ ] Run tests; observe missing verifier failure.
- [ ] Implement bounded streaming verification, then run all VM tests.
- [ ] Add reusable CI verification called by registered os-image inspect-vm, excluding unrelated system image build.
- [ ] Commit, push, dispatch and read actual verification results.

## Task 2: Physical companion VM acceptance
Consumes the exact APK identity and signature from Task 1.
- [ ] Independently hash the laptop download; verify package absent/preserve any existing app.
- [ ] Install the verified APK and grant only its declared development VM permissions.
- [ ] Configure a bounded memory/vCPU allocation and actual AVF mode before starting.
- [ ] Verify guest Linux boot, terminal, Python/Git where feasible, shutdown and stock integrity.
- [ ] Record every actual result and resource limit; do not convert partial proof into completion.

## Task 3: Native environment controls
Files: android/app/src/main/java/dev/forgeos/ai/EnvironmentActivity.java; EnvironmentPolicy.java; MainActivity.java; AndroidManifest.xml; associated unit/instrumented tests.
Consumes verified signer, exact companion package and actual Task 2 behavior.
- [ ] Write failing policy tests for absent companion, wrong signer and missing permissions.
- [ ] Implement clear native controls that launch the companion and explain actual setup limits.
- [ ] Build/test in Actions, inspect UI preview, then verify on target after existing approval.
- [ ] Update verification matrix and record remaining dual-boot/storage blockers.

Review the final diff and evidence before reporting success. No task is completed by a proposed implementation or a compilation alone.
