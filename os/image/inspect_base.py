"""Fetch and inspect a pinned upstream GSI on an isolated build worker, never a tablet."""
import argparse,json,lzma,shutil,struct,subprocess
from pathlib import Path
from urllib.request import urlopen
from image_tools import *

def inspect(lock,out):
 out=Path(out).resolve()
 if out.exists():raise ValueError("inspection output must be new")
 out.mkdir(parents=True);report=out/"inspection";report.mkdir()
 archive=out/"upstream.img.xz"
 with urlopen(lock["url"],timeout=60) as response,archive.open("xb") as target:
  total=0
  while chunk:=response.read(2*1024*1024):
   total+=len(chunk)
   if total>lock["compressed_bytes"]:raise ValueError("download exceeds pinned size")
   target.write(chunk)
 checksum=verify_file(archive,lock["sha256"],lock["compressed_bytes"],allow_unpinned=True)
 raw=out/"base.img"
 with lzma.open(archive,"rb") as source,raw.open("xb") as target:
  total=0
  while chunk:=source.read(2*1024*1024):
   total+=len(chunk)
   if total>lock["max_raw_bytes"]:raise ValueError("decompressed image too large")
   target.write(chunk)
 kind=image_kind(raw)
 if kind=="sparse":
  converted=out/"raw.img";run(["simg2img",raw,converted]);raw=converted
 if image_kind(raw)!="ext4":raise ValueError("base must contain an ext4 system filesystem")
 fsck=run(["e2fsck","-f","-n",raw],accepted=(0,1,2,4),timeout=300)
 (report/"base-fsck.txt").write_text(fsck)
 (report/"root-listing.txt").write_text(debugfs(raw,"ls -l /"))
 root="/system" if "Inode:" in debugfs(raw,"stat /system/build.prop") else ""
 prop=extract(raw,root+"/build.prop",report/"build.prop").read_text(errors="replace")
 props=dict(line.split("=",1) for line in prop.splitlines() if "=" in line and not line.lstrip().startswith("#"))
 api=props.get("ro.system.build.version.sdk",props.get("ro.build.version.sdk"))
 if api!=str(lock["android_api"]):raise ValueError("unexpected Android API: "+str(api))
 framework=extract(raw,root+"/framework/framework-res.apk",report/"framework-res.apk")
 for inside,name in [(root+"/overlay/config/config.xml","overlay-config.xml"),(root+"/etc/selinux/plat_file_contexts","plat_file_contexts")]:
  try:extract(raw,inside,report/name)
  except ValueError:pass
 for name,command in {"app-metadata":"stat "+root+"/app","app-xattrs":"ea_list "+root+"/app","framework-metadata":"stat "+root+"/framework/framework-res.apk","filesystem":"stats"}.items():
  (report/(name+".txt")).write_text(debugfs(raw,command))
 with raw.open("rb") as stream:
  stream.seek(-64,2);footer=stream.read(64)
 avb=None
 if footer[:4]==b"AVBf":
  magic,major,minor,original,vbmeta_offset,vbmeta_size=struct.unpack(">4sIIQQQ28x",footer)
  avb={"major":major,"minor":minor,"original_bytes":original,"vbmeta_offset":vbmeta_offset,"vbmeta_bytes":vbmeta_size}
 evidence={"archive_sha256":checksum,"publisher_checksum_supplied":False,"matches_pinned_sha":lock["sha256"] is not None,
  "upstream":lock,"raw_bytes":raw.stat().st_size,"raw_sha256":sha256_file(raw),"system_root":root,
  "api":api,"security_patch":props.get("ro.system.build.version.security_patch",props.get("ro.build.version.security_patch")),
  "fingerprint":props.get("ro.system.build.fingerprint",props.get("ro.build.fingerprint")),"avb_footer":avb,
  "fsck_exit_policy":"read-only inspection; findings retained, not repaired",
  "acceptance":"OFFLINE INSPECTION ONLY. Not target boot verification; no flashing permitted."}
 (report/"base-evidence.json").write_text(json.dumps(evidence,indent=2))
 print(json.dumps({k:evidence[k] for k in ("archive_sha256","raw_bytes","system_root","api","security_patch","avb_footer")},indent=2))
 return evidence

if __name__=="__main__":
 parser=argparse.ArgumentParser();parser.add_argument("--lock",required=True);parser.add_argument("--output",required=True);args=parser.parse_args()
 inspect(json.loads(Path(args.lock).read_text()),args.output)
