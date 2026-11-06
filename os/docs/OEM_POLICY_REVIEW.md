# OEM unlocking policy: actual post-PIN verification

Observed2026-11-06UTC on the same USB-identified onn.100135925 / TTA.31 tablet. Prior unlock/flash/conditional-wipe approval remains recorded in AUTHORIZATION.md; this check did not execute those operations.

| Check | Before | After owner completed Android PIN confirmation |
|---|---|---|
| OEM unlocking Settings switch | Enabled control, unchecked | Checked; actual UI hierarchy inspected |
| sys.oem_unlock_allowed | 0 | 1 |
| fastboot flashing get_unlock_ability | 0 | **16777216**, exact decimal vendor response |
| fastboot getvar unlocked | no | no |
| fastboot getvar secure | yes | yes |
| Active slot | b | b |

The OEM permission blocker has changed through Android's supported setting. The vendor fastboot result is nonzero, but it is not the standard literal1 and is not silently normalized to1. A successful unlock has not been tested, so this is not labeled a verified unlocked bootloader. No speculative endian explanation is needed for the actual observed result.

After read-only queries, fastboot reboot succeeded. Android independently returned sys.boot_completed=1, original onn/100135925/100135925:15/AP3A.240905.015.A2/TTA.31:user/release-keys, locked1, AVBgreen, slot_b, OEM permission1 and stock Home com.android.launcher3. This verifies the setting survived a normal bootloader round trip. It does not establish restoration after a failed firmware flash.

Full bounded evidence: [OEM policy and stock return](evidence/physical-usb-20261106/oem-policy-verified.json). No PIN, credential input or private key is recorded.

A diagnostic assertion initially used a substring search for `unlock_ability = 1`, which also matches16777216. The full output exposed the mismatch before any unlock decision. The private probe now parses the complete numeric line and records the exact value; fixtures0/1/16777216 distinguish all three. The misleading literal1 success label was corrected. No OS install or unlock was triggered by that check.

## Remaining technical gates

- Authenticated exact stock recovery firmware and a tested restoration procedure remain unavailable. A focused filename search in GamingLaptop Downloads/Desktop found no matching100135925/t402/TTA.31 firmware. This is not proof that no firmware exists elsewhere.
- Android16 developer AVB signature integrity is verified offline; OEM acceptance, actual vendor/VINTF/HAL/rollback behaviour and alternate-system boot remain unverified.
- The small2GiB streamed DSU layout fits a conservative read-only capacity estimate. Actual transport, allocation and first boot remain untested; the adopted card is not reformatted or counted as portable DSU storage.
- AOSP DSULoader passes KEY_PUBKEY to the installation service. Merely displaying a catalog entry or completing a download does not prove that the first-stage OEM boot chain trusts our custom developer key.

Stock is preserved. No unlock, wipe, flash, critical unlock, slot change, DSU install or alternate-OS boot was sent. There is no repeated approval request; these are technical acceptance gates.
