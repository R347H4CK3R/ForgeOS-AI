# Approved SD conversion: actual results

The owner explicitly approved deleting everything on the existing card. Disk identity was checked before erasure: SanDisk, SD flag,127,934,660,608bytes, mmcblk1, disk:179,0. Internal storage was not repartitioned or wiped.

Roblox was successfully moved to internal storage and its APK path verified. Both ForgeOS packages could not be moved because MOVE_FAILED_LOCKED_USER(-10) was returned; the review package was also registered in locked Private space. No private-profile authentication was bypassed. Card erasure deleted its remaining app data as authorized. The verified stable review installer SHA256e728a652d30a6b9cbd7ea376a0b610c81b54c9b5ee15d1d64a51ddb05a2c51dc already on GamingLaptop was copied to the pre-format backup folder. Reading APKs directly from the old adopted path was denied; that failed copy is not counted as a backup. The review APK was reinstalled successfully to /data/app. The older ForgeOS package is not claimed restored.

The supported sm partition mixed25 operation erased the card and created:
- Portable PUBLIC/vfat volume2916-1A0F:31,225,376KiB filesystem capacity, mounted/storage/2916-1A0F.
- Adopted PRIVATE/f2fs volumeab41b09e-2da7-4d9b-8e69-a4185b74edc3:93,683,692KiB filesystem capacity.

AOSP's mixed-partition feature creates the public fraction first. A sub32GiB public partition receives FAT32 automatically, while the128GB physical disk exceeds DSU's30GiB disk-size criterion. This avoids the full-card exFAT default. Actual public vfat and private f2fs mounts were verified, not inferred from command exit.

ForgeOS/models, projects, source, datasets, downloads, logs, artifacts and backups were created on the portable volume and listed successfully. This establishes shell access, not an automatic SAF grant to every app or a successful guest boot. The adopted remainder is encrypted stock workspace capacity; it is not assumed accessible from an independent guest userdata environment.

Stock retains its original Android15/TTA.31 fingerprint, locked bootloader and AVBgreen. A new named DSU request, forgeos_sd, targets the unchanged official compatibility image with2GiB userdata, one-shot requested and automatic reboot disabled. Actual selected path, installation result and guest boot must be verified separately. [Exact conversion evidence](evidence/official-42227213753/card-conversion.json).

## Actual SD-backed installation outcome

The stock service explicitly selected /mnt/media_rw/2916-1A0F/forgeos_sd, proving the new public-volume routing on this target. Available memory stayed approximately1.7millionKiB during monitoring, without the earlier installer responsiveness-kill event. Userdata creation then failed because gsid could not stat /dev/block/mmcblk1p1: Permission denied. The later missing lp_metadata error is cleanup fallout, not the first failure.

The device node has vendor-specific labelu:object_r:mmcblk1p1_block_device:s0, ownerroot/root, mode0600; SELinux is Enforcing. A policy restriction is plausible, but no exact AVC denial was retrieved, so SELinux is not asserted as conclusively isolated from other access controls. The demonstrated fact is that the native privileged GSI service cannot inspect this backing block device on the current firmware.

Guest allocation failed cleanly with gsi_tool statusnormal, stock locked/AVBgreen and public space essentially unchanged. No guest enable/boot, internal wipe or firmware flash occurred. The card erase/repartition DID occur with explicit approval. Workspace shell access is verified; app SAF access and guest use remain unverified.

Full native log:evidence/official-42227213753/sd-install-failure.log. Reformatting again or changing the image does not address the demonstrated device-node access denial. Any firmware/policy correction requires a supported privileged route and validated recovery; global SELinux permissive mode is not used.
