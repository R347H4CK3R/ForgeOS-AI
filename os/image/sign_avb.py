"""Offline developer AVB hashtree signing; never an OEM trust assertion."""
import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import shutil
import stat
import struct
import subprocess
import sys
import tempfile

from image_tools import require_regular, sha256_file

AVBTOOL_SHA256 = "e5a664a38db623da00f080219bc0ee60a640a9dc4a872803616fae4938ac749b"
AVBTOOL_COMMIT = "5ac0c3a071d811846a62412383dd6e259f341e6e"
DEFAULT_TOOL = Path(__file__).parent / "vendor" / "avbtool.py"


def safe_path(path):
    path = Path(path).absolute()
    if any(p.is_symlink() for p in (path, *path.parents)):
        raise ValueError("symlink paths are not accepted")
    return path


def new_path(path):
    path = safe_path(path)
    if path.exists():
        raise ValueError("refusing to overwrite existing output")
    if not path.parent.is_dir():
        raise ValueError("output parent directory missing")
    return path


def clean_environment():
    # Allowlist instead of enumerating current/future credential variable names.
    names = ("PATH", "SystemRoot", "SYSTEMROOT", "WINDIR", "TEMP", "TMP",
             "LANG", "LC_ALL")
    return {name: os.environ[name] for name in names if name in os.environ}


def run(args):
    try:
        completed = subprocess.run([str(a) for a in args], capture_output=True,
                                   stdin=subprocess.DEVNULL, env=clean_environment(),
                                   timeout=300)
    except (OSError, subprocess.TimeoutExpired):
        raise RuntimeError("offline signing tool could not complete") from None
    if completed.returncode:
        raise RuntimeError("offline signing or verification tool failed") from None
    return completed.stdout.decode("utf-8", errors="replace")


def prefix_sha256(path, count):
    digest = hashlib.sha256()
    with require_regular(path).open("rb") as stream:
        while count:
            chunk = stream.read(min(count, 2*1024*1024))
            if not chunk:
                raise ValueError("image shorter than filesystem")
            digest.update(chunk)
            count -= len(chunk)
    return digest.hexdigest()


def ext4_bytes(path):
    with require_regular(path).open("rb") as stream:
        stream.seek(1024)
        sb = stream.read(1024)
    if len(sb)!=1024 or sb[56:58]!=b"\x53\xef":
        raise ValueError("input must be a raw ext4 filesystem")
    blocks = struct.unpack_from("<I", sb, 4)[0]
    incompat = struct.unpack_from("<I", sb, 96)[0]
    if incompat & 0x80:
        blocks |= struct.unpack_from("<I", sb, 336)[0] << 32
    log = struct.unpack_from("<I", sb, 24)[0]
    if log > 6:
        raise ValueError("invalid ext4 block size")
    return blocks * (1024 << log)


def sign_system_image(image, output, evidence, public_pem, expected_public_sha256,
                      patch, fingerprint, report, tool=DEFAULT_TOOL):
    image = require_regular(safe_path(image))
    evidence = require_regular(safe_path(evidence))
    public_pem = require_regular(safe_path(public_pem))
    tool = require_regular(safe_path(tool))
    output = new_path(output)
    report = new_path(report)
    if output == report:
        raise ValueError("output and report must differ")
    if sha256_file(tool) != AVBTOOL_SHA256:
        raise ValueError("avbtool source checksum mismatch")
    if len(expected_public_sha256)!=64 or any(c not in "0123456789abcdef" for c in expected_public_sha256):
        raise ValueError("invalid public key SHA256 pin")
    date = datetime.date.fromisoformat(patch)
    if date.isoformat()!=patch:
        raise ValueError("security patch must be exact YYYY-MM-DD")
    if not fingerprint or any(c in fingerprint for c in "\r\n\0"):
        raise ValueError("invalid fingerprint")
    record = json.loads(evidence.read_text(encoding="utf-8"))
    original_hash = sha256_file(image)
    size = image.stat().st_size
    if record.get("fsck_clean") is not True or record.get("trusted_avb") is not False:
        raise ValueError("evidence must confirm clean untrusted filesystem")
    if record.get("output_sha256") != original_hash or record.get("output_bytes") != size:
        raise ValueError("evidence image hash/bytes mismatch")
    if record.get("security_patch") != patch:
        raise ValueError("security patch evidence mismatch")
    fsbytes = ext4_bytes(image)
    if fsbytes != size:
        raise ValueError("input must contain exactly filesystem bytes")
    secret = os.environ.get("FORGEOS_AVB_DEV_KEY")
    if not secret:
        raise ValueError("FORGEOS_AVB_DEV_KEY is required")
    temp_root = os.environ.get("RUNNER_TEMP")
    if not temp_root or not safe_path(temp_root).is_dir():
        raise ValueError("RUNNER_TEMP must be an existing directory")
    partition_size = ((fsbytes + 64*1024*1024 + 4095)//4096)*4096
    rollback = int(datetime.datetime.combine(date, datetime.time(), datetime.timezone.utc).timestamp())
    created_output = created_report = False
    try:
        with tempfile.TemporaryDirectory(prefix="forgeos-avb-", dir=temp_root) as private:
            private = Path(private)
            key = private/"key.pem"
            fd = os.open(key, os.O_WRONLY|os.O_CREAT|os.O_EXCL, 0o600)
            with os.fdopen(fd, "wb") as stream:
                stream.write(secret.encode("utf-8"))
            secret = None
            public = private/"public.avbpubkey"
            public_from_pem = private/"expected.avbpubkey"
            base = [sys.executable, tool]
            run(base+["extract_public_key","--key",key,"--output",public])
            run(base+["extract_public_key","--key",public_pem,"--output",public_from_pem])
            if sha256_file(public)!=expected_public_sha256 or sha256_file(public_from_pem)!=expected_public_sha256:
                raise ValueError("developer public key SHA256 mismatch")
            with output.open("xb") as target, image.open("rb") as source:
                created_output = True
                shutil.copyfileobj(source, target, length=2*1024*1024)
            run(base+["add_hashtree_footer","--image",output,"--partition_name","system",
                      "--partition_size",partition_size,"--algorithm","SHA256_RSA4096",
                      "--key",key,"--do_not_generate_fec","--rollback_index",rollback,
                      "--prop","com.android.build.system.os_version:16",
                      "--prop","com.android.build.system.security_patch:"+patch,
                      "--prop","com.android.build.system.fingerprint:"+fingerprint])
            run(base+["verify_image","--image",output,"--key",public_pem])
            info = run(base+["info_image","--image",output])
            if prefix_sha256(output,fsbytes)!=original_hash:
                raise ValueError("signed filesystem prefix changed")
            run(["e2fsck","-f","-n",output])
            if sha256_file(image)!=original_hash or image.stat().st_size!=size:
                raise ValueError("original image changed")
            result = {"input_sha256":original_hash,"filesystem_bytes":fsbytes,
                      "output_sha256":sha256_file(output),"output_bytes":output.stat().st_size,
                      "public_key_sha256":expected_public_sha256,"security_patch":patch,
                      "fingerprint":fingerprint,"partition_size":partition_size,
                      "rollback_index":rollback,"algorithm":"SHA256_RSA4096",
                      "avbtool_commit":AVBTOOL_COMMIT,"avbtool_sha256":AVBTOOL_SHA256,
                      "avb_info":info,"fsck_clean":True,"boot_tested":False,
                      "OEMtrust":False,"trusted_avb":False,"unlocked_bootloader_required":True,
                      "DSU_candidate_only":True}
            with report.open("x",encoding="utf-8") as stream:
                created_report=True
                json.dump(result,stream,indent=2)
                stream.write("\n")
            return result
    except BaseException:
        if created_output:
            output.unlink(missing_ok=True)
        if created_report:
            report.unlink(missing_ok=True)
        raise


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    for flag in ("image","evidence","output","public-key","public-sha256","patch","fingerprint","report"):
        parser.add_argument("--"+flag,required=True)
    parser.add_argument("--tool",default=str(DEFAULT_TOOL))
    args=vars(parser.parse_args())
    args["public_pem"]=args.pop("public_key")
    args["expected_public_sha256"]=args.pop("public_sha256")
    try:
        sign_system_image(**args)
    except Exception:
        parser.exit(1,"Offline developer AVB signing failed; signing material omitted from diagnostics.\n")


if __name__=="__main__":
    main()
