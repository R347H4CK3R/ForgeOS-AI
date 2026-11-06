# Locked-stock DSU compatibility route

## Actual cause and alternative

The physical tablet's built-in DSU Loader displays No DSU available for this device. Its own DSULOADER logs explain why: ARM64 catalog images with patches2025-04-05 and2026-01-05 are older than stock2026-04-05 and are rejected. This is not proof that the tablet lacks DSU. No patch-property spoofing, rollback bypass or verification-disabling flag is used.

A newer **unmodified official ARM64 Android17 GSI** is now verified for a separate native ForgeOS APK compatibility test. This is not a finished/custom ForgeOS image, permanent dual boot or permission to redistribute Google's image. The developer GSI is used for its stated application-testing purpose. Our separate Android16 developer-signed custom image remains intact and outside this route.

Official release:https://developer.android.com/topic/generic-system-image/releases

## Full offline verification

[Workflow42227213753](https://github.com/R347H4CK3R/ForgeOS-AI/actions/runs/42227213753) on4b02dca passed validation and actual full-image inspection. The existing legacy image build was correctly skipped. The worker downloaded/hashed/extracted/verified the image; GamingLaptop downloaded only a few kilobytes of ZIP range metadata and small evidence. No full GSI binary was uploaded as an artifact or modified.

| Check | Actual result |
|---|---|
| Official ZIP | 1,166,864,309bytes; SHA2564a869a6959a50fee15ff871ce378853ced4485f2aa456eb64dbdbf7a2c0408aa, matches Google's published digest |
| Raw system | 2,072,014,848bytes; SHA256e5db44e3365f9e542ee3f6d4caf88ece554a460e2987086b8bf21f6c440a25ec |
| Actual properties | API37 / Android17 / buildCP3A.260905.010 / patch2026-09-05 |
| Fingerprint | Android/generic_system/generic:17/CP3A.260905.010/16176746:user/release-keys |
| AVB | SHA256_RSA4096 signature and SHA256 hashtree verified; flags0; original image unchanged |
| Key | SHA1 1ffb1af796b18ebf40d18faed213f686720f73f2; absent from Google's fetched revocation list (empty object accepted as an empty list by AOSP's parser). This is not proof of OEM first-stage trust. |
| Filesystem | Read-only e2fsck clean |
| Storage | 7,062,597,632bytes available before request; explicit2GiB guest plus system fits with fixed1GiB reserve and128MiB allowance. Both streamed and staged estimates fit; HTTPS streaming avoids the extra archive write. |

Full evidence: [official-evidence.json](evidence/official-42227213753/official-evidence.json), [AVB verification](evidence/official-42227213753/avb-verification.txt), [capacity](evidence/official-42227213753/capacity.json).

Stock VINTF was also read using its own `vintf -v` tool: device target-level202404,54 merged HAL entries; device matrix requires framework sensorservice and system SDK35. These observations do not constitute a cross-image checkvintf pass. Actual Android17 vendor/HAL/SELinux/input/graphics/network behaviour is untested.

## Concrete next step already prepared

The documented native VerificationActivity was launched on the actual tablet with the verified official HTTPS ZIP URL, exact raw system size, explicit2GiB userdata, named slotforgeos_compat, exact public-key SHA1, KEY_ONE_SHOT=true and **KEY_ENABLE_WHEN_COMPLETED=false**. Android returned ConfirmDeviceCredentialActivity. The owner must authenticate directly on the device; no credential value is collected. Request details:[installer-request.json](evidence/official-42227213753/installer-request.json).

This is a request awaiting native authentication, not a completed guest installation. No automatic reboot is requested. The next verification is actual allocation/download/installation state, followed by cleanup controls and image-key results. Do not infer successful install from Activity launch.

The DSU mechanism creates separate temporary system/userdata rather than flashing stock boot/vendor/system or using Virtual A/B slots as a second OS. The bootloader remains locked and stock data is preserved. This route avoids unlocking/wiping for the compatibility evaluation **if the OEM boot chain accepts the official GSI key**; acceptance and actual boot are not yet established. The source service defaults to one-shot enablement, but target return-to-stock after a guest boot still requires testing. It is not a permanent dual-boot implementation.

Authenticated exact stock firmware remains a prerequisite for destructive bootloader/firmware changes. The temporary compatibility route does not waive that gate for the custom image. Existing owner authorization is retained; no repeated approval request is needed for already-approved operations.

## Pipeline repair

A new workflow could not be manually dispatched by filename because it was absent from the default branch (actual HTTP404). It was made reusable and connected to the existing registered os-image workflow's inspect-official operation; the corrected run succeeded. No unchanged failing run was repeatedly retried.
