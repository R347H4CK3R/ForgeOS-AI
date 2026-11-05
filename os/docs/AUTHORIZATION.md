# Owner authorization and stock preservation
Updated2026-11-05 America/Chicago, from explicit instructions in this session.
- Strata-style UI design: approved.
- Bootloader unlocking and flashing: authorized.
- Wiping the tablet if necessary: authorized after the normal unlock-reset and adopted-key risks were explained.
- Required result: retain stock Android as one environment and ForgeOS as another. Do not overwrite stock system/vendor/boot partitions or repurpose Virtual A/B slots as if they were independent dual-boot installations.
- Existing adopted SD card: no reformat or migration performed. Any destructive card conversion requires a concrete backup/migration plan; tablet wipe authorization is not treated as an instruction to format this card.

Do not ask again for already-authorized actions. Authorization does not establish that an image, recovery route, rollback or bootloader method is technically safe.
No bootloader reboot/unlock, factory reset, firmware flash, DSU install/boot, slot switch or SD operation has been performed. The separate stable-signed native review APK was installed on stock Android for non-destructive hardware validation. It does not replace the stock launcher or constitute OS installation.
Next technical prerequisites are physical USB/bootloader identification, exact authenticated stock recovery/restore evidence, verified vendor/AVB/rollback compatibility and actual alternate-system/stock-return testing.
