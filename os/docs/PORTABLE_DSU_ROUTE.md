# Alternate DSU backing-store route

Android15 AOSP DynamicSystemService.startInstallation selects storage in priority order: configured os.aot.path, qualifying public volume, then /data/gsi/dsu/<slot>. Actual reference source inspected:
https://android.googlesource.com/platform/frameworks/base/+/android-15.0.0_r1/services/core/java/com/android/server/DynamicSystemService.java

The public-volume branch requires TYPE_PUBLIC, mounted writable, fsType vfat, and physical disk size at least30GiB (MINIMUM_SD_MB=30L<<10, disk.size>>20). It does not explicitly filter DiskInfo.isSd. A marketed32GB disk may fall below30GiB; use64GB or larger. exFAT and adopted/private volumes do not meet this reference selector.

Actual tablet sm list-volumes all exposes only internal private/emulated storage and the existing adopted private card; no qualifying public volume is mounted. os.aot.path is empty. No property override, formatting or migration was attempted. The existing adopted card remains intact.

Next candidate: a separate portable FAT32 medium. A spare microSD can be considered after safely unmounting the existing adopted card; do not remove or format the current card casually. USB storage via an OTG adapter also meets the reference selector if Android exposes it as qualifying public/vfat media, but this tablet's USB early-boot visibility is NOT verified. A source-code allocation rule is not proof of target USB boot support.

Before another installation: verify actual mounted filesystem/type/disk size/free space; observe DynamicSystemService's selected installation path; retain stock image and named one-shot guest with automatic reboot disabled; monitor allocation without UIAutomator load; stop on renewed responsiveness kills. Test discard and reclaimed storage before any guest boot acceptance. This changes the backing store rather than repeating a RAM-only cleanup.

Status: candidate route identified, BLOCKED on separate physical media. It is not a verified fix, OS boot or permanent dual boot. The previous native allocation failures remain recorded in DSU_TARGET_ATTEMPT.md. Exact stock recovery firmware is still required before destructive bootloader/firmware modification. No existing-card erase or format is authorized by this proposal.
