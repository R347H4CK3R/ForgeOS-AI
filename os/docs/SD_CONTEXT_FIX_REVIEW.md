# Narrow SD context correction candidate

Actual stock inspection established both sides of the mismatch:
- Vendor file-contexts maps /dev/block/mmcblk1p1 to u:object_r:mmcblk1p1_block_device:s0; the live node has that label.
- Platform policy explicitly grants gsid read/getattr/open and related read access to sdcard_block_device, with ioctl permissions. A native stat of the vendor-labeled node fails with Permission denied.
- gsid runs root in domainu:r:gsid:s0; ADB is uid2000/domainu:r:shell:s0. No su executable was found. Root UID alone does not imply SELinux permission.

Prepared [onn-sd-file-contexts.patch](../image/candidates/onn-sd-file-contexts.patch): change only the exact removable SD partition mapping to the existing canonical sdcard_block_device type. No internal partition mapping, broad allow rule, verification-disable flag or global SELinux mode is changed. This is a proposed firmware-context correction, not a live fix or compiled-policy acceptance claim. A full effective-policy query/AVC would strengthen causal attribution; neither has been claimed.

The executable os/image/prepare_sd_context_fix.py processed the actual pulled105,753byte stock file into a separate candidate while preserving the original. It rejects duplicate/wrong mappings and refuses existing output. Input SHA256fb0a37759223a47aaacd178999c013f1b5876d2d47ef20c089368898cce814a0; output SHAb17dff04b98e9e181ee5b5edab9832ada43c14cf08c701a7f132e7ebf63eb750. Unified diff independently contains one removed and one added mapping line. The full extracted vendor file remains on the owner's laptop; only the narrow diff is tracked.

Four new CLI regressions pass. Windows image-tool suite:46discovered,42passed,4Linux-only skips in4.949seconds. These establish offline transformation, not boot acceptance.

## Application boundary

This correction belongs to the device's trusted vendor contexts/policy integration. Injecting it into the guest GSI will not repair the host gsid permission that fails before guest creation. Normal ADB cannot rewrite the protected vendor configuration. Changing labels at runtime would also require an authorized privileged path. No root or protection bypass, live relabel, SELinux-permissive switch or firmware flash was attempted.

Before deployment: compile/check vendor file contexts against the combined policy; check factory/vold/storage-domain access after relabeling; validate AVB/hash/signature and rollback; establish exact stock recovery; then verify DSU allocation, discard, guest boot and return-to-stock. Relabeling may change access for other domains, including vendor factory services, so it is not declared universally safe from a text diff.

The actual tablet remains stock locked/AVBgreen; its approved mixed SD layout and restored internal review APK remain intact. The concrete next requirement is a verified privileged firmware/recovery route for this device. Prior unlock/flash/wipe approval remains recorded; no repeated consent request is needed. See SD_CONVERSION_REVIEW.md for the actual failure and card results.
