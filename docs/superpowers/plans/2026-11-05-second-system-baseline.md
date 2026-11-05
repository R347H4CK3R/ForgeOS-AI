# New baseline and second-system preflight
Owner approved the Strata-style design and separately authorized unlock, flash and wipe if necessary. Preserve stock Android as the other OS. Authorization does not establish technical compatibility, recovery or dual boot.

- [ ] Add a strict actual security-patch floor validator. Test older/equal/newer/malformed calendar dates; never rewrite a build property to claim fixes.
- [ ] Pin an Android16 vanilla candidate by GitHub asset ID, size and SHA256 in a separate inspection-only lock. Do not replace the Android15 artifact or publish it through the native API35 staging feed.
- [ ] Add explicit inspect-android16 workflow selection with fixed paths, retaining read-only filesystem/resource evidence and actual AVB descriptor inspection.
- [ ] Validate tools, inspect candidate on GitHub, read actual logs/artifacts and record API/security date/AVB/inode/filesystem results. Promotion and target boot remain separate gates.
- [ ] Record current authorization, partial backup coverage, physical USB need and DSU constraints. No fastboot, reboot, wipe, flash, slot switching or DSU preparation occurs in this preflight.

Current evidence: DSU binder/package exist; stock remains locked/green. Internal free6.1GiB is below recommended10GiB. DSU cannot use adopted SD media. Exact firmware/restore route remains unverified; wireless ADB is the only observed transport.
