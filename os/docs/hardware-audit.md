## Current follow-up (2026-11-07 UTC)

The historical snapshot below is retained as history. The approved mixed SD layout now provides a verified native FAT32 workspace plus a mounted adopted f2fs volume. Native0.3 direct SD inference and real split launch/touch resizing pass. A real Alpine guest boots under AVF/GenieZone, with Python/Git tests and restart persistence verified. See [current native evidence](NATIVE_03_REVIEW.md) and [AVF acceptance](AVF_INTERIM_REVIEW.md). Stock fingerprint/HOME, locked bootloader and green AVB were rechecked. No alternate Android system-image boot or full restoration is verified.

# Hardware audit — live read-only ADB, November 4, 2026 (America/Chicago)
Target: onn. 100135925, board t402aa. Hardware property mt8786; ro.board.platform reports mt6768. These are different properties, not proof of a replacement chipset.
Fingerprint: onn/100135925/100135925:15/AP3A.240905.015.A2/TTA.31:user/release-keys.
RAM: MemTotal 2927416 kB; MemAvailable approximately 1240596 kB at one observation. Free RAM changes.
ABI: arm64-v8a, armeabi-v7a, armeabi. Android 15, system and vendor patch 2026-04-05.
Bootloader: flash.locked=1, vbmeta device_state=locked, verifiedbootstate=green, slot _b.
Treble=true; dynamic partitions=true; virtual_ab=true.
Partition names observed include boot_a/b, init_boot_a/b, vendor_boot_a/b, dtbo_a/b, vbmeta_a/b, vbmeta_system_a/b, vbmeta_vendor_a/b, super, metadata, userdata, preloader_a/b, nvram/nvdata, persist. Names alone do not establish sizes, backups or recoverability.
Adopted private card mounted f2fs: 119G total, 1.4G used, 118G available.
Primary storage UUID=null; primary shared path remains /data/media, approximately 6.1G available internally.
Card's emulated:179,2;0 volume is UNMOUNTED. Ordinary app SAF access to adopted card is not established; selecting a folder is not automatic migration to the SD card.
Android reports file-based encryption and encrypted state. This is not independent cryptographic verification of SD data; adopted keys were not read.
Wi-Fi and Bluetooth settings both report enabled; wireless ADB communication succeeded. Bluetooth pairing/input and Wi-Fi performance not tested.
Termux and Tailscale installed (Tailscale 1.104.4); laptop Tailscale BackendState=Running. This does not prove tablet tailnet login or inter-device reachability.
Features report multitouch, USB host, Bluetooth LE, secondary displays. Physical peripherals not exercised.
Display: built-in 800x1280 at 60 Hz plus RustDeskVD virtual display. No HDMI/DisplayPort display observed.
Reading /sys/class/typec was denied. No DP Alt Mode claim is justified.
Freeform support global setting=null; native multi-window support and actual operation still unverified.
No device settings, installed apps, bootloader state, partition contents or storage layout were modified by this audit.
