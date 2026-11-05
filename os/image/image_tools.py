"""Offline regular-file helpers. Never open a block device or invoke device tools."""
import hashlib,stat,struct,subprocess
from pathlib import Path

def require_regular(path):
 path=Path(path)
 if path.is_symlink() or not stat.S_ISREG(path.stat().st_mode):
  raise ValueError("only regular image files are accepted")
 return path

def sha256_file(path):
 path=require_regular(path);before=path.stat();check=hashlib.sha256()
 with path.open("rb") as stream:
  for chunk in iter(lambda:stream.read(2*1024*1024),b""):check.update(chunk)
 after=path.stat()
 if (before.st_size,before.st_mtime_ns)!=(after.st_size,after.st_mtime_ns):
  raise ValueError("input changed while hashing")
 return check.hexdigest()

def verify_file(path,expected_sha,expected_bytes,allow_unpinned=False):
 path=require_regular(path)
 if path.stat().st_size!=expected_bytes:raise ValueError("input size mismatch")
 if expected_sha is None and not allow_unpinned:raise ValueError("unpinned image cannot be adapted")
 actual=sha256_file(path)
 if expected_sha is not None and actual!=expected_sha:raise ValueError("input SHA256 mismatch")
 return actual

def image_kind(path):
 with require_regular(path).open("rb") as stream:header=stream.read(2048)
 if len(header)>=4 and struct.unpack_from("<I",header)[0]==0xed26ff3a:return "sparse"
 if len(header)>=1082 and header[1080:1082]==b"\x53\xef":return "ext4"
 raise ValueError("unsupported image format")

def run(args,accepted=(0,),timeout=120):
 result=subprocess.run([str(a) for a in args],capture_output=True,text=True,timeout=timeout)
 if result.returncode not in accepted:
  raise RuntimeError("command failed: "+str(args[0])+": "+(result.stdout+result.stderr)[-4000:])
 return result.stdout+result.stderr

def debugfs(image,command,write=False):
 args=["debugfs"]
 if write:args.append("-w")
 return run(args+["-R",command,require_regular(image)])

def extract(image,inside,destination):
 destination=Path(destination)
 if destination.exists():raise ValueError("refusing to replace extracted file")
 debugfs(image,'dump -p '+inside+' '+str(destination))
 if not destination.is_file():raise ValueError("missing image path: "+inside)
 return destination
