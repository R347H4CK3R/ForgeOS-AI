# Actual temporary DSU installation failure

Observed2026-11-06UTC, after owner completed native Android credential confirmation.

The native installer started allocating the official guest. gsid logged creation of userdata_gsi. Android lowmemorykiller then killed com.android.dynsystem:dynsystem (RSS112720kB, swap27688kB, oom_score_adj50), reason: device is not responding. No successful download/install completion or guest boot is inferred. This does not prove simple RAM exhaustion; memory-pressure and I/O stalls need further diagnosis.

Free internal space dropped to2776052KiB during partial allocation. The native discard action was requested, but cleanup did not promptly complete and the gsi status query stalled. Stock was rebooted without enabling any guest. After reboot, ADB verified boot_completed1, original Android15/TTA.31 fingerprint and AVBgreen. gsi_tool status returned normal and free space recovered to6922548KiB. A subsequent discard-service invocation before first device unlock returned service not found; it is not counted as cleanup success. Recovery is supported by the observed normal state and reclaimed space.

Result: target DSU installation FAILED; partial allocation recovered after normal stock reboot. No bootloader unlock, firmware flash, factory reset or guest boot occurred. Earlier reports describing a pending credential prompt are superseded by this actual attempt.

Next attempt must change the resource conditions, starting from the clean stock boot and measuring available memory/pressure before allocation. Do not repeatedly retry unchanged or weaken global memory safeguards. The previously verified official image remains unchanged; OEM key acceptance, cross-image vendor compatibility and guest-to-stock boot testing remain unverified.
