# AVF interim environment specification

The owner requests a real OS workstation and genuine dual boot, with a clearly labeled interim two-environment option when safe dual boot cannot be established. Preserve stock Android and existing data. Existing installation approval is recorded; no new unlock or firmware flash is part of this fallback.

Actual onn hardware booted the stock Microdroid payload under GenieZone using a 256 MiB, one-vCPU non-protected VM. The log reached payload is ready and adbd started. The bounded test ended; vm list returned no running VMs. This is hardware-isolated Android-derived execution, not genuine dual boot or a full desktop.

Develop a lightweight interim Linux VM integration using the independent GPL-2.0 Podroid companion, only if its pinned publisher APK signature/hash and actual AVF guest operation pass. Preserve its attribution; do not relabel third-party software as a complete ForgeOS firmware build. Prefer 1 vCPU and 768 MiB over software CPU emulation. No silently accepted QEMU fallback.

The stock OS and SD workspace remain the native touch interface. Native controls must open real companion controls and report missing installation, permissions, low memory and unsupported hardware. No fake VM-running state. No bundled credentials, public remote command listeners or firmware writes.

Genuine dual boot, the earlier internal allocation stall and approximately 1.93 GiB storage difference remain separately unresolved. The system bugreport is private on the owner's laptop; it captured a BatteryStatsService watchdog stall and reported no zram backing device. Do not publish the raw report or claim a specific remaining allocation file.
