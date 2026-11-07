# Efficiency decisions
## Tablet
- Native Android controls replace the WebView interface.
- CPU inference is compiled in Release mode even in the debuggable review APK.
- GGUF model file limit550MiB, parameter limit600M, two inference/prefill threads, context512, max output128 tokens.
- Reject inference when Android reports low memory or less than850MiB available.
- One generation at a time. Release model/context/sampler after each response instead of retaining hundreds of MiB while idle.
- Stop requests persist through model validation and loading; model load progress and CPU decode check cancellation.
- File/provider operations run on a single I/O worker with bounded text reads256KiB. UI is locked for conflicting document operations.
- Remote inference is opt-in; host Ollama adapter unloads after the response. No bridge or host model is started automatically.
## Build environment
- ARM64 and x86 emulator jobs build only the ABI they need.
- Native compiler cache capped512MiB per job; Gradle worker count2 and JVM heap1536MiB.
- No AOSP compilation, WSL startup or host inference on GamingLaptop.
- Feature work builds through PR events; branch push duplication removed.
- Binary model transfer uses ADB file sync with exact destination SHA256, not an unbounded raw stdin copy.
- Isolated emulator configured800x1280 at160dpi to match observed tablet geometry and reduce rendering overhead.
## Measured package size
Before native Release optimization:14,508,156 bytes APK (e858c21).
After optimization:6,704,780 bytes APK (3102180), approximately53.8% smaller.
These are observed APK file sizes, not inference-speed claims or target RAM measurements.
## Remaining limits
Actual tablet inference speed, peak app memory, thermal throttling, concurrent-window behavior and battery draw require the approved physical-device test. No claim of60FPS under local inference is made.

Final cf01a34 review artifact: 6,722,453 bytes (53.7% below 14,508,156). Remote planning uses a closed tool schema, temperature 0 and seed 42. The accepted remote-only 1.5B fixture completed read/propose/finish and approved apply in 33.181 seconds. The 0.5B model was unreliable and is not an accepted general repair backend.
