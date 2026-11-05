# Recovery and installation gates
## Current decision
NO firmware installation procedure is approved or safe to propose as executable steps. Exact stock TTA.31 Android15 firmware for onn 100135925/t402aa and a tested restore route have not been verified. Do not use similarly named onn model firmware.
A read-only ADB audit confirms locked bootloader, AVB green and virtual A/B. No unlocking/reboot/flashing commands were executed.
## Evidence and architecture
- AOSP GSI requirements: https://source.android.com/docs/core/tests/vts/gsi
- DSU architecture and trusted signing keys: https://source.android.com/docs/core/ota/dynamic-system-updates
- Virtual A/B snapshots: https://source.android.com/docs/core/ota/virtual_ab
- Bootloader unlock and mandatory reset: https://source.android.com/docs/core/architecture/bootloader/locking_unlocking
A custom GSI is not automatically trusted by the locked bootloader. Treble support alone does not verify vendor HAL compatibility.
DSU is a possible temporary second-system route only if this OEM implements it and trusts the image signer. This has not been established. No DSU image was installed or booted.
Virtual A/B dynamic partitions are snapshots that merge; slots are not independent durable OS installations. Do not switch slots to claim dual boot.
Bootloader unlocking generally wipes userdata. That would also jeopardize access to adopted SD encryption keys. Copying encrypted SD blocks alone is not a proven backup.
## Before any OS installation proposal
1. Obtain an exact authenticated OEM full firmware/recovery package and record SHA256, board, region, build, security patch and rollback indexes.
2. Establish supported bootloader and recovery/fastbootd access, tool versions, partition sizes and snapshot state without modifying them.
3. Back up user-accessible data to separate storage and verify restoration; document adopted-key limitations.
4. Establish and test a recovery route on equivalent hardware or otherwise obtain evidence sufficient for the owner to assess risk.
5. Build the exact candidate image, validate AVB chain/signing, kernel/vendor/VINTF/SELinux compatibility and offline image contents.
6. Present native UI screenshots, tests, limitations, expected data changes, exact image hashes and a device-specific install/rollback sequence.
7. Obtain separate explicit permission for UI acceptance, APK installation, image boot, unlocking/wipe, and firmware flashing as applicable.
## Current reversible interim environment
The review APK is a native HOME-capable launcher on stock Android, not an OS image and not dual boot. No installation authorized or performed.
After approval, a limited APK-only test could preserve stock firmware and keep the original launcher available through Android Home settings. Disabling the ForgeOS launcher or switching the default home restores the original home UI; uninstall may delete private histories. This is not firmware recovery.
## External display
DP Alt Mode remains unverified. No software flag can establish missing physical support.
A DisplayLink USB graphics dock is a possible hardware-dependent alternative, not verified on this tablet: https://www.synaptics.com/products/displaylink-graphics/downloads/android
It requires compatible DisplayLink hardware and USB host; adapter test is deferred until connected. Network mirroring is separate from native wired display output.

## Firmware search checkpoint (2026-11-05 UTC)
Exact model/build queries did not establish an authenticated OEM TTA.31 recovery download. A community archive at https://sourceforge.net/projects/onn-tablet-firmwares/files/ is only a research lead, not a verified recovery source; model-number requests in reviews are not firmware. Driver downloads and firmware for 100135923 are not recovery images for this100135925. No candidate archive was flashed or treated as rollback proof.
USB-C connector/adapter listings also do not prove this tablet supports DP Alt Mode. Preserve the unverified classification until hardware documentation or the actual adapter test establishes it.
