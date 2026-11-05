# ForgeOS AI Operating System

Target: onn. 100135925 / MT8786 / ARM64 / Android 15 vendor baseline.

## Status
This directory contains **OS design and build scaffolding**, not a bootable image. Do not flash.

## Architecture
- AOSP-derived Android system image, retaining compatible vendor drivers and kernel.
- SystemUI tablet taskbar, split-screen and freeform window management.
- Dedicated AI workspace, local inference where RAM permits, optional remote compute.
- Native Wi-Fi, Bluetooth, touch and USB host via Android hardware abstraction layers.
- Tailscale and remote workstation integration.

## Release gates
1. Obtain legal stock firmware and verified recovery procedure for exact model.
2. Confirm bootloader unlock method, rollback strategy, AVB and partition layout.
3. Build matching AOSP/GSIs and verify image signatures, ABI, kernel/vendor compatibility.
4. Test on isolated hardware or reversible boot environment.
5. Validate touch, Wi-Fi, Bluetooth, audio, storage, USB, graphics, suspend, boot.
6. User approves UI and test results before flashing or booting.

## Multi-window
Android supports split-screen and freeform tasks, but device policy and SystemUI must support them. The current tablet has no confirmed enabled freeform setting. AOSP build-time overlays and SystemUI implementation must be validated.

## Dual boot
Virtual A/B is an OTA mechanism, not a verified dual-boot mechanism. Never repurpose boot slots without an independently verified recovery plan.
