# Opt-in ForgeOS development bridge
Python 3.11+, no third-party Python packages. This is a limited host service, not an Android Desktop Commander agent.
Run tests: `python -m unittest discover -s bridge -v`.
Set FORGEOS_WORKSPACE to an existing dedicated project directory and FORGEOS_BRIDGE_TOKEN to a newly generated random session secret (32+ characters), then run `python bridge/server.py`. Supply environment variables through your host's secure launch mechanism; do not put secrets in source, scripts, workflow inputs or logs.
Service binds only 127.0.0.1:8765. Use Tailscale Serve HTTPS with tailnet access controls to reach it; never port-forward it publicly. Tablet client rejects HTTP and redirects. Copy the session secret to the tablet manually; it is not persisted. Rotate on service restart. No service is started automatically by installing the APK.
Supported authenticated POST actions:
- /health: bridge capabilities (not model readiness).
- /projects: direct child folders.
- /read: UTF-8 file inside selected workspace, up to 256 KiB.
- /git-status: fixed git status command for one child project; no shell, fsmonitor disabled, 15-second timeout.
- /inference: requests your already installed Ollama service at loopback 11434 with a specified installed model; 512 output tokens, 2048 context, unload after use.
No arbitrary command, write, install, build-script execution, autonomous repair, or Desktop Commander credential access is exposed. This is a deliberate authorization boundary for the current review build.
Live tablet-to-host HTTPS and inference are unverified until setup and tablet UI approval. Unit and HTTP tests exercise auth and filesystem boundaries locally; they do not establish live Tailscale connectivity.
