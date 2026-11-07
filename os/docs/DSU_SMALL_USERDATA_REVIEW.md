# Smaller guest userdata trial

This was a bounded resource experiment, not a supported workstation layout. The normal preflight planner still requires at least 2 GiB. The official Android 17 image and public-key pin were unchanged; the request specified 1 GiB userdata, one-shot mode and automatic enable=false. Source inspection shows the reference installer forwards the supplied size. The stock service selected /data/gsi/dsu/forgeos_small, but native allocation size/completion was not independently verified.

Only the portable SD volume was temporarily unmounted to select the internal fallback; the adopted remainder stayed mounted. No UIAutomator was run during allocation. MemAvailable was 1,701,280 KiB beforehand and 1,696,944 KiB at the first monitor sample. Device-not-responding lowmemorykiller events appeared after launch, including later stock-launcher kills. The test was stopped; there is no successful guest installation or boot. This still does not prove simple RAM exhaustion or isolate a particular kernel defect.

The native discard service was requested. The first shell reboot acknowledgement timed out; it was not counted as recovery. Subsequent fresh boot identity, original TTA.31 fingerprint, locked=1, AVB=green, slot_b and gsi_tool status=normal were independently checked. Both SD volumes mounted again. The supported gsi_tool wipe reported 'Live image install successfully removed.' Another normal stock reboot was checked.

## Unresolved cleanup measurement

Before: 6,151,444 KiB free internal storage. Final recorded sample: 4,130,764 KiB free. The roughly 1.93 GiB difference was not reclaimed by the observed discard/wipe/reboot sequence. Neither its attribution to a specific remaining file nor complete cleanup is verified: ADB cannot inspect the protected /data/gsi directory. A normal status or successful wipe message alone is insufficient evidence of full storage recovery. No unrelated files or app data were deleted to conceal the difference.

There were no new unlocks, factory resets, firmware writes, slot changes or guest enables. No further allocation retry is justified without explaining this failure and storage difference.

## Remaining route

The separate SD attempt already selected the right FAT32 volume but failed native block-device stat permission. The narrow offline vendor-context candidate remains unapplied and untested against combined policy. Normal ADB cannot safely apply it. An authenticated exact-device recovery/privileged maintenance route is still missing; existing unlock/flash/wipe approvals do not supply that technical recovery capability. Genuine dual boot remains blocked, and this report does not label the stock native launcher as a second OS.

See [sanitized evidence](evidence/small-userdata-20261107/trial.json), [fresh stock return](evidence/small-userdata-20261107/stock-return.json), [SD context candidate](SD_CONTEXT_FIX_REVIEW.md) and [separate app storage result](SD_APP_ACCESS_REVIEW.md).
