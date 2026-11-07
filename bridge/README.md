# Opt-in ForgeOS development bridge
Python 3.11+, no third-party packages. This is a constrained host service, not a native Android Desktop Commander agent.
Run tests: `python -m unittest discover -s bridge -v`.

Set FORGEOS_WORKSPACE to an existing dedicated root and FORGEOS_BRIDGE_TOKEN to a newly generated random session secret (32+ characters), then run `python bridge/server.py`. Supply secrets through the host environment; never commit them.
Binds only 127.0.0.1:8765. Configure Tailscale Serve HTTPS and tailnet access controls; never publicly port-forward. The Android client rejects HTTP and redirects, keeps its token in memory, and saves only the endpoint. Rotate tokens on restart. APK installation does not start a host service.

Authenticated POST actions:
- /health: capabilities, not proof of model readiness.
- /projects: direct child folders.
- /read: bounded UTF-8 workspace file.
- /git-status: fixed Git status, no shell, fsmonitor disabled, 15-second timeout.
- /inference: configured loopback Ollama, bounded context/output, unload after response.
- /agent/plan: bounded model tool loop to list/read/propose edits and diagnose Python syntax.
- /agent/approve: explicit confirmation plus exact pending plan ID and SHA256; apply reviewed edits only.

The UI displays proposed contents and diffs before confirmation. Plans are canonical immutable bytes, expire in ten minutes, and are capped at four. At most three edits per plan, 16 KiB each; existing editable files max 32 KiB. Source hashes are checked again before writes; backups precede atomic file replacements, with rollback on error. Conflict detection is optimistic and is not a cross-process atomic compare-and-swap.

Hidden/traversal/Windows alias/reserved paths and reparse points are rejected. No arbitrary shell, generated program execution, project test/build scripts, package installation or Desktop Commander credential access. Python diagnosis checks syntax and attempts at most two repairs; it is not test execution.

Closed JSON tool schema, deterministic sampling and next-step guidance constrain model responses. Consecutive identical calls stop early with a reported reason. Model summaries remain untrusted descriptions; the server's approval_required/plan hash and actual apply result determine whether writes occurred.

The CI acceptance model is qwen2.5-coder:1.5b (986,048,576-byte quantized weights). It is remote-only, not a tablet-local model, and is not installed automatically on GamingLaptop. The 0.5B model produced repeated reads during repeat testing and is not accepted as reliable. The actual 1.5B read/propose/confirmed-apply test passed on two isolated workers (33.722 and 33.783 seconds). This modest fixture does not establish competence on arbitrary projects.

Normal tests cover authentication, filesystem boundaries, Windows junctions, immutable plans, conflicts, approval, backups, rollback and loop stopping. The opt-in live-model test runs on GitHub's worker; it does not establish a tablet HTTPS session. Host model use requires suitable free memory; defer demanding remote work while gaming.
Final candidate cf01a34 also passed the actual 1.5B fixture in 33.181 seconds; see ../os/docs/evidence/live-agent-cf01a34.json and successful workflow 41978243888.
