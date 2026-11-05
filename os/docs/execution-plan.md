# ForgeOS AI execution plan
Spec: User mission, 2026-11-04.
Architecture: Native Java Android activities using system document providers, Android task management, and optional separately installed Termux/Tailscale. An OS product must be separate from the launcher and cannot be released as compatible without stock recovery and device tests.
1. Replace WebView with native dashboard, document editor, file access, settings and app launching. Verify APK/lint and emulator interaction. Never install on tablet before UI approval.
2. Persist user-selected workspace tree using SAF; create eight required folders with explicit errors. Test revoked grants, directory access and text size bounds.
3. Add authenticated optional remote inference and development integration; no embedded secrets, no arbitrary unauthenticated command server.
4. Inspect ADB hardware, storage, native window support and connectivity read-only. No flags counted as functional verification.
5. Evaluate GSI/vendor and stock recovery evidence before image work; locked bootloader and missing recovery are release blockers.
6. Produce evidence matrix, UI capture, artifacts and recovery report. No firmware changes or tablet installation authorized.
Status: Baseline b2569c0; latest Actions 41962463987 succeeded. Existing source is a WebView.
