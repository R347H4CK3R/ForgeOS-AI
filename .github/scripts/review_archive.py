"""Bundle only explicitly selected, bounded build outputs for draft review."""
import argparse
import glob
import hashlib
import json
import os
from pathlib import Path
import zipfile

MAX_TOTAL = 512 * 1024**2


def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024**2), b""):
            digest.update(chunk)
    return digest.hexdigest()


def build_archive(workspace, patterns, destination):
    root = Path(workspace).resolve()
    destination = Path(destination)
    if destination.exists() or destination.is_symlink():
        raise ValueError("refusing existing review archive")
    files = {}
    for pattern in patterns:
        if not pattern.strip():
            continue
        relative = Path(pattern)
        if relative.is_absolute() or ".." in relative.parts:
            raise ValueError("review paths must remain inside workspace")
        for match in glob.glob(str(root / pattern), recursive=True):
            path = Path(match)
            candidates = list(path.rglob("*")) if path.is_dir() else [path]
            for item in candidates:
                if item.is_symlink() or not item.resolve().is_relative_to(root):
                    raise ValueError("review outputs cannot link outside workspace")
                if item.is_file():
                    name = item.relative_to(root).as_posix()
                    if ".git" in item.relative_to(root).parts:
                        raise ValueError("git internals are not review artifacts")
                    files[name] = item
    if not files:
        raise ValueError("no build outputs matched")
    if sum(p.stat().st_size for p in files.values()) > MAX_TOTAL:
        raise ValueError("review outputs exceed 512 MiB")
    manifest = {"schema": 1, "files": [
        {"path": n, "bytes": p.stat().st_size, "sha256": sha256(p)}
        for n, p in sorted(files.items())]}
    created = False
    try:
        with destination.open("xb") as stream:
            created = True
            with zipfile.ZipFile(stream, "w", compression=zipfile.ZIP_DEFLATED) as archive:
                for name, path in sorted(files.items()):
                    archive.write(path, name)
                archive.writestr("MANIFEST.json", json.dumps(manifest, indent=2))
    except BaseException:
        if created:
            destination.unlink(missing_ok=True)
        raise
    return {"files": len(files), "bytes": destination.stat().st_size,
            "sha256": sha256(destination)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    result = build_archive(Path(os.environ["GITHUB_WORKSPACE"]),
                           os.environ["REVIEW_PATHS"].splitlines(), args.output)
    print(json.dumps(result))


if __name__ == "__main__":
    main()
