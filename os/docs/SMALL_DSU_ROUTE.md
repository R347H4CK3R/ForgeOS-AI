# Small DSU evaluation route

This refines the earlier storage blocker; it does not authorize an unverified boot or establish dual boot. Current owner unlock/flash/conditional-wipe approval remains recorded in AUTHORIZATION.md.

## Storage route found

The older 8 GiB guest layout cannot fit. However, Android15 AOSP DynamicSystemInstallationService uses a 2 GiB default and accepts explicit KEY_USERDATA_SIZE. The tablet's own gsi_tool usage also exposes a configurable --userdata-size (that CLI separately defaults to8GiB). The AOSP source is a reference, not proof of the OEM service's exact implementation. Use an explicit size; do not infer the OEM default.

Source: https://android.googlesource.com/platform/frameworks/base/+/android-15.0.0_r1/packages/DynamicSystemInstallationService/src/com/android/dynsystem/DynamicSystemInstallationService.java

Actual USB df measurement and the already-verified candidate sizes are recorded in [capacity evidence](evidence/physical-usb-20261106/dsu-capacity.json):

| Component | Bytes |
|---|---:|
| Available internal space | 6,856,081,408 |
| Signed raw system | 2,955,452,416 |
| Explicit guest data | 2,147,483,648 |
| Extra allocation allowance | 134,217,728 |
| Minimum free-space reserve | 1,073,741,824 |
| Required for streamed source | 6,310,895,616 |
| Required if gzip is also staged on tablet | 7,747,445,802 |

Streaming fits this capacity estimate with 1,618,927,616bytes free after allocation including the extra allowance. Staging the gzip on the tablet does not preserve the reserve. This is not a successful allocation, installation or boot test. Actual vendor allocation and first-boot usage may differ. A 2 GiB guest is only an initial lightweight evaluation; models/projects/builds require a separate workspace strategy.

`os/image/dsu_preflight.py` is an executable read-only planner. It rejects invalid counts and guests smaller than2GiB, retains fixed reserve/allocation allowance, compares streamed and staged paths and never labels a capacity fit boot-ready. It invokes no ADB/fastboot, downloads nothing, formats nothing and provisions no server. Six CLI regression tests cover the actual tablet case, former8GiB layout, safety reserve, invalid data sizes and staged-transport accounting.

The proposed later transport is a verified gzip on GamingLaptop streamed through a loopback-only source and USB ADB reverse, avoiding a duplicate tablet download. That transport is not yet provisioned or tested. It must verify exact image hash, serve only the intended image, close after use and avoid reusable credentials. Do not expose a host file server on the LAN merely to overcome a capacity issue.

## Unlock route and trust boundary

Android's supported response to get_unlock_ability=0 is enabling OEM unlocking in Developer options when the device permits it. The tablet is presently at its secure lock screen. Developer options was opened successfully behind that screen; it cannot be inspected or changed until the owner unlocks normally. No credential bypass, direct protected-service write, unlock exploit or flash command is used.

Official reference: https://source.android.com/docs/core/architecture/bootloader/locking_unlocking

The alternative locked-bootloader DSU route requires a GSI key trusted by the OEM boot chain. Official developer GSIs may support compatibility testing on devices configured with those keys. Replacing their contents with ForgeOS invalidates that signature. Our own developer-signed image is not automatically OEM trusted. Normal Android exposes no /avb or /first_stage_ramdisk key directory on this tablet; absence of those runtime paths neither proves nor disproves support. An official testing GSI is not represented as a custom ForgeOS image or redistributed outside its license.

Official reference: https://source.android.com/docs/core/ota/dynamic-system-updates

Do not use a local-file URI as a workaround for signature/revocation errors: Android's installation code may be permissive for local files, but that does not establish boot-chain trust. Preserve verification and diagnose the actual key/compatibility result.

## Still required

Normal device unlock to inspect the OEM setting; authenticated exact100135925/TTA.31 recovery evidence; vendor/VINTF/AVB/rollback validation; actual source transport and allocation checks. Stock remains locked/AVBgreen on slot_b. No candidate installation/boot, unlock, wipe, slot switch or SD reformat occurred. The adopted card is excluded from this allocation calculation and is not treated as a portable DSU volume.
