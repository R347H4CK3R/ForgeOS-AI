"""Read-only identity check for the independently distributed VM companion.

A hash match is not an Android signature or hardware boot verification.
"""
import argparse
import hashlib
import json
import re
import zipfile
from pathlib import Path
from urllib.parse import urlsplit


def validate_pin(pin):
    if pin.get("schema") != 1 or pin.get("package") != "com.excp.podroid":
        raise ValueError("unsupported dependency identity")
    version = pin.get("version", "")
    if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", version):
        raise ValueError("invalid release version")
    parsed = urlsplit(pin.get("url", ""))
    expected = f"/ExTV/Podroid/releases/download/v{version}/Podroid-v{version}-release.apk"
    if (parsed.scheme != "https" or parsed.netloc != "github.com"
            or parsed.path != expected or parsed.query or parsed.fragment):
        raise ValueError("release URL is not the pinned publisher download")
    if type(pin.get("bytes")) is not int or not 0 < pin["bytes"] <= 500 * 1024**2:
        raise ValueError("invalid APK size")
    if not re.fullmatch(r"[0-9a-f]{64}", pin.get("sha256", "")):
        raise ValueError("invalid SHA256")
    if not re.fullmatch(r"[0-9a-f]{40}", pin.get("source_commit", "")):
        raise ValueError("invalid source commit")
    return pin


def verify_apk(path, pin):
    validate_pin(pin)
    path = Path(path)
    if path.is_symlink() or not path.is_file():
        raise ValueError("APK must be a regular file, not a symlink")
    if path.stat().st_size != pin["bytes"]:
        raise ValueError("APK size differs from publisher pin")
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024**2), b""):
            digest.update(block)
    if digest.hexdigest() != pin["sha256"]:
        raise ValueError("APK SHA256 differs from publisher pin")
    try:
        with zipfile.ZipFile(path) as archive:
            names = archive.namelist()
            if "AndroidManifest.xml" not in names:
                raise ValueError("APK manifest missing")
            if not any(n.startswith("lib/arm64-v8a/") and n.endswith(".so") for n in names):
                raise ValueError("ARM64 native payload missing")
    except zipfile.BadZipFile as error:
        raise ValueError("invalid APK ZIP container") from error
    return {
        "schema": 1, "bytes": pin["bytes"], "sha256": digest.hexdigest(),
        "package_expected": pin["package"], "version_expected": pin["version"],
        "apk_signature_verified": False, "device_boot_verified": False,
        "installation_performed": False,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--lock", type=Path, required=True)
    args = parser.parse_args()
    try:
        result = verify_apk(args.apk, json.loads(args.lock.read_text(encoding="utf-8")))
    except (ValueError, OSError) as error:
        parser.error(str(error))
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
