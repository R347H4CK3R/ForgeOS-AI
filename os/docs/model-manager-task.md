# Model manager implementation task
Goal: Complete the native review app's real model download/management flow before physical testing. Repo C:\Users\Chris\ForgeOS-Native, branch forgeos/native-workstation. Android Java, SDK35/min26. No tablet installation or interaction.
Own only new ModelManagerActivity.java, ModelIntegrity.java, ModelIntegrityTest.java and the necessary LocalActivity.java integration. Do not modify manifest/build/workflow/other files or commit/push; coordinator integrates and commits one batch. Do not spawn subagents.
Use Android DownloadManager, Wi-Fi by default (no metered/roaming), persisted download ID, visible actual progress, cancel/retry, and background SHA256 verification. Handle rotation and cancelled/failed jobs without false completion.
Starter model URL https://huggingface.co/bartowski/SmolLM2-135M-Instruct-GGUF/resolve/09816acd5d99df7be770d85ea30822623dab342c/SmolLM2-135M-Instruct-Q4_K_M.gguf
Expected bytes105454432; SHA256 2e8040ceae7815abe0dcb3540b9995eaa1fa0d2ca9e797d0a635ae4433c68c2d.
Store only in app-owned external files directory. Display actual location and disclose it does not prove adopted-card use. Never delete arbitrary files; retry/cancel may remove only this app-created starter download.
Do not bundle weights, embed secrets, execute models automatically or add third-party paid services.
Use verified model returns RESULT_OK with data URI to LocalActivity startActivityForResult request21; LocalActivity must handle its own-file URI without requesting a persisted external grant. Existing user-selected GGUF request20 remains.
ModelIntegrity.verify(File,String,long) must stream/check exact bytes and SHA256; meaningful unit tests cover valid data, corruption and wrong length. JUnit4 already available. Actual CI will be run by coordinator; report honestly what was/wasn't executed.
Write concise completion report to os/docs/model-manager-result.md with changed paths and risks. Finish implementation fully, without user questions.
