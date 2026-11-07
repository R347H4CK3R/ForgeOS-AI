"""Offline APK release signing. Private material is never included in evidence."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile

from image_tools import require_regular, sha256_file


def run_private(args, *, binary=False):
    # Never include subprocess output/arguments in exceptions: tools can echo keys.
    env = os.environ.copy()
    for name in ("FORGEOS_APK_SIGNING_KEY","FORGEOS_UPDATE_SIGNING_KEY","GH_TOKEN","GITHUB_TOKEN"):env.pop(name,None)
    try:
        result = subprocess.run([str(a) for a in args], capture_output=True,
                                timeout=120, env=env)
    except (OSError, subprocess.TimeoutExpired):
        raise RuntimeError("signing tool could not complete") from None
    if result.returncode:
        raise RuntimeError("signing tool failed") from None
    return result.stdout if binary else result.stdout.decode("utf-8", errors="replace")


def certificate_sha256(certificate, openssl="openssl"):
    der = run_private([openssl, "x509", "-in", require_regular(certificate),
                       "-outform", "DER"], binary=True)
    return hashlib.sha256(der).hexdigest()


def check_certificate(certificate, expected, openssl="openssl"):
    if not re.fullmatch(r"[0-9a-fA-F]{64}", expected):
        raise ValueError("expected certificate SHA256 must contain 64 hex digits")
    actual = certificate_sha256(certificate, openssl)
    if actual != expected.lower():
        raise ValueError("certificate SHA256 mismatch")
    return actual


def check_key_certificate(key, certificate, openssl="openssl"):
    key_public = run_private([openssl, "pkey", "-in", key, "-pubout",
                              "-outform", "DER"], binary=True)
    cert_public = run_private([openssl, "x509", "-in", certificate, "-pubkey",
                               "-noout"], binary=True)
    # Normalize the certificate public key to DER without writing private data.
    cert_der = subprocess.run([str(openssl), "pkey", "-pubin", "-outform", "DER"],
                              input=cert_public, capture_output=True, timeout=120,
                              env={k:v for k,v in os.environ.items()
                                   if k not in ("FORGEOS_APK_SIGNING_KEY","FORGEOS_UPDATE_SIGNING_KEY","GH_TOKEN","GITHUB_TOKEN")})
    if cert_der.returncode:
        raise RuntimeError("certificate public key conversion failed")
    if key_public != cert_der.stdout:
        raise ValueError("private key does not match certificate")


def verify_signer_digest(output, expected):
    digests = re.findall(r"^Signer #[0-9]+ certificate SHA-256 digest: ([0-9a-fA-F]{64})\s*$",
                         output, re.MULTILINE)
    if len(digests) != 1 or digests[0].lower() != expected.lower():
        raise ValueError("signed APK certificate SHA256 mismatch or multiple signers")


def protected_destination(path):
    path = Path(path)
    if path.exists() or path.is_symlink():
        raise ValueError("refusing to overwrite existing output")
    if not path.parent.is_dir():
        raise ValueError("output parent directory is missing")
    return path


def sign_apk(input_apk, output, certificate, expected_cert_sha256, apksigner,
             evidence, *, source=None, expected_input_sha256=None,
             expected_input_bytes=None, openssl="openssl"):
    input_apk = require_regular(input_apk)
    output = protected_destination(output)
    evidence = protected_destination(evidence)
    if output.resolve() == evidence.resolve():
        raise ValueError("APK and evidence destinations must differ")
    original_sha = sha256_file(input_apk)
    original_bytes = input_apk.stat().st_size
    if expected_input_sha256 is not None and original_sha != expected_input_sha256.lower():
        raise ValueError("input SHA256 mismatch")
    if expected_input_bytes is not None and original_bytes != expected_input_bytes:
        raise ValueError("input size mismatch")
    cert_sha = check_certificate(certificate, expected_cert_sha256, openssl)
    run_private([apksigner, "verify", "--verbose", "--print-certs", input_apk])
    secret = os.environ.get("FORGEOS_APK_SIGNING_KEY")
    if not secret:
        raise ValueError("FORGEOS_APK_SIGNING_KEY is required")
    runner_temp = os.environ.get("RUNNER_TEMP")
    if not runner_temp or not Path(runner_temp).is_dir():
        raise ValueError("RUNNER_TEMP must be an existing directory")
    # TemporaryDirectory is 0700 on POSIX. Both key files are created 0600.
    with tempfile.TemporaryDirectory(prefix="forgeos-sign-", dir=runner_temp) as temp:
        temp = Path(temp)
        pem = temp / "key.pem"
        der = temp / "key.pk8"
        for path in (pem, der):
            fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
            with os.fdopen(fd, "wb") as stream:
                if path == pem:
                    stream.write(secret.encode("utf-8"))
        secret = None
        check_key_certificate(pem, certificate, openssl)
        run_private([openssl, "pkcs8", "-topk8", "-nocrypt", "-in", pem,
                     "-outform", "DER", "-out", der])
        candidate = temp / "signed.apk"
        run_private([apksigner, "sign", "--key", der, "--cert", certificate,
                     "--out", candidate, input_apk])
        verified = run_private([apksigner, "verify", "--verbose", "--print-certs",
                                require_regular(candidate)])
        verify_signer_digest(verified, cert_sha)
        if sha256_file(input_apk) != original_sha or input_apk.stat().st_size != original_bytes:
            raise ValueError("original APK changed during signing")
        result = {
            "schema_version": 1,
            "original_apk": {"sha256": original_sha, "bytes": original_bytes,
                             "source": source or input_apk.name},
            "signed_apk": {"sha256": sha256_file(candidate),
                           "bytes": candidate.stat().st_size,
                           "certificate_sha256": cert_sha},
            "scope": {"platform_signing": False, "avb_trust": False,
                      "native_execution_tested": False},
        }
        # Exclusive creation protects against replacement/races. Never overwrite.
        created_output = False
        created_evidence = False
        try:
            with output.open("xb") as target, candidate.open("rb") as stream:
                created_output = True
                import shutil
                shutil.copyfileobj(stream, target)
            with evidence.open("x", encoding="utf-8") as target:
                created_evidence = True
                json.dump(result, target, indent=2)
                target.write("\n")
        except BaseException:
            if created_output:
                output.unlink(missing_ok=True)
            if created_evidence:
                evidence.unlink(missing_ok=True)
            raise
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("input", "output", "certificate", "expected-cert-sha256",
                 "apksigner", "evidence"):
        parser.add_argument("--" + name, required=True)
    parser.add_argument("--source")
    parser.add_argument("--expected-input-sha256")
    parser.add_argument("--expected-input-bytes", type=int)
    parser.add_argument("--openssl", default="openssl")
    args = vars(parser.parse_args())
    args["input_apk"] = args.pop("input")
    try:
        sign_apk(**args)
    except Exception:
        # Do not print secret-bearing tool exceptions or environment contents.
        parser.exit(1, "APK signing failed; no signing secrets are included in diagnostics.\n")


if __name__ == "__main__":
    main()
