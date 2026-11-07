# Actual temporary DSU installation failure

Observed2026-11-06UTC, after owner completed native Android credential confirmation.

The native installer started allocating the official guest. gsid logged creation of userdata_gsi. Android lowmemorykiller then killed com.android.dynsystem:dynsystem (RSS112720kB, swap27688kB, oom_score_adj50), reason: device is not responding. No successful download/install completion or guest boot is inferred. This does not prove simple RAM exhaustion; memory-pressure and I/O stalls need further diagnosis.

Free internal space dropped to2776052KiB during partial allocation. The native discard action was requested, but cleanup did not promptly complete and the gsi status query stalled. Stock was rebooted without enabling any guest. After reboot, ADB verified boot_completed1, original Android15/TTA.31 fingerprint and AVBgreen. gsi_tool status returned normal and free space recovered to6922548KiB. A subsequent discard-service invocation before first device unlock returned service not found; it is not counted as cleanup success. Recovery is supported by the observed normal state and reclaimed space.

Result: target DSU installation FAILED; partial allocation recovered after normal stock reboot. No bootloader unlock, firmware flash, factory reset or guest boot occurred. Earlier reports describing a pending credential prompt are superseded by this actual attempt.

Next attempt must change the resource conditions, starting from the clean stock boot and measuring available memory/pressure before allocation. Do not repeatedly retry unchanged or weaken global memory safeguards. The previously verified official image remains unchanged; OEM key acceptance, cross-image vendor compatibility and guest-to-stock boot testing remain unverified.

## Changed-resource retry and recovery

The owner removed screen-lock security; actual keyguard secure=false/showing=false was observed. After10.5hours of uptime, MemAvailable was1,417,268KiB. Android am kill-all closed cached background processes; measured available RAM rose to1,623,456KiB before a single changed retry. No global low-memory safeguards, system partitions or adopted-card formatting were changed.

Allocation again triggered device-not-responding lowmemorykiller activity, including foreground processes. The installer process disappeared; memory measured2,013,048KiB afterward. This is not proof of simple RAM exhaustion or a specifically identified kernel defect. /proc/pressure and /proc/diskstats reads are denied, so detailed I/O-pressure attribution remains blocked. The monitoring helper also hit a UIAutomator timeout; UI inspection was stopped rather than treated as an install result.

The retry was aborted with a normal stock reboot. A fresh-boot check (uptime under600seconds) independently verified originalTTA.31, boot_completed1, locked1, AVBgreen, slot_b, gsi_tool statusnormal and more than6GiB free internal space. See evidence/official-42227213753/retry-stock-return.json. An initial check raced the asynchronous reboot and returned incomplete status; it was rejected, not counted as successful recovery.

Actual DSU allocation remains FAILED after two attempts with different resource preconditions. No guest boot, unlock, factory reset or firmware flash occurred. Do not retry this stock allocation path unchanged or disable LMK to force it through. The stock installation and adopted storage are preserved; the native workstation on stock remains a separate deliverable. Permanent dual boot and recovery firmware remain unresolved.

## Smaller userdata follow-up

A separately labeled 1 GiB request also produced responsiveness kills; normal stock boot and both SD mounts were verified afterward. Full free-space recovery was not verified (about1.93 GiB difference). See DSU_SMALL_USERDATA_REVIEW.md. The approved card conversion supersedes earlier adopted-layout preservation statements.
