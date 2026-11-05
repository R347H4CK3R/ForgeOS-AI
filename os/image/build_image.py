"""Offline ext4 adaptation, never flashing. Original input bytes remain untouched."""
import argparse,json,re,shutil,struct,tempfile,zipfile,xml.etree.ElementTree as ET
from pathlib import Path
from image_tools import *
CONTEXT=b"u:object_r:system_file:s0\0"
FEATURE=b'<permissions><feature name="android.software.freeform_window_management"/></permissions>\n'

def tool_path(path):
 value=str(Path(path).resolve())
 if re.search(r'[\s"\\\x00-\x1f]',value):raise ValueError("offline tool paths must not contain whitespace, quotes or backslashes")
 return value

def inode_metadata(image,path):
 text=debugfs(image,"stat "+path)
 mode=re.search(r"Mode:\s+([0-7]+)",text);owner=re.search(r"User:\s+(\d+)\s+Group:\s+(\d+)",text)
 if not mode or not owner:raise ValueError("missing inode: "+path)
 return {"mode":mode.group(1),"uid":int(owner.group(1)),"gid":int(owner.group(2)),
         "xattrs":debugfs(image,"ea_list "+path)}

def exists(image,path):return "Inode:" in debugfs(image,"stat "+path)

def filesystem_bytes(image):
 with require_regular(image).open("rb") as stream:stream.seek(1024);sb=stream.read(1024)
 size=1024<<struct.unpack_from("<I",sb,24)[0]
 count=struct.unpack_from("<I",sb,4)[0]+(struct.unpack_from("<I",sb,0x150)[0]<<32)
 free=struct.unpack_from("<I",sb,12)[0]+(struct.unpack_from("<I",sb,0x158)[0]<<32)
 inodes=struct.unpack_from("<I",sb,16)[0]
 total=count*size
 if not 2048<=total<=require_regular(image).stat().st_size:raise ValueError("invalid ext4 filesystem size")
 return total,free*size,inodes

def validate_apk(apk):
 with zipfile.ZipFile(require_regular(apk)) as archive:
  names=set(archive.namelist())
  if not {"AndroidManifest.xml","classes.dex","lib/arm64-v8a/libforge_ai.so"}<=names:
   raise ValueError("expected ARM64 ForgeOS APK")
  with archive.open("lib/arm64-v8a/libforge_ai.so") as stream:elf=stream.read(64)
  if len(elf)<20 or elf[:6]!=b"\x7fELF\x02\x01" or struct.unpack_from("<H",elf,18)[0]!=183:
   raise ValueError("native APK library is not ARM64 ELF")

def adapt_image(base,apk,output,lock):
 base=require_regular(base);apk=require_regular(apk);output=Path(output)
 tool_path(base);tool_path(apk);tool_path(output)
 if output.exists() or output.is_symlink():raise ValueError("output must be a new regular file")
 base_hash=verify_file(base,lock["raw_sha256"],base.stat().st_size)
 apk_hash=verify_file(apk,lock["apk"]["sha256"],lock["apk"]["bytes"])
 validate_apk(apk)
 if image_kind(base)!="ext4":raise ValueError("adapter accepts raw ext4 only")
 fs_bytes,free_bytes,free_inodes=filesystem_bytes(base)
 if free_bytes<apk.stat().st_size+2*1024*1024 or free_inodes<8:raise ValueError("base lacks safe free space; implicit expansion is forbidden")
 run(["e2fsck","-f","-n",base],timeout=300)
 root="/system" if exists(base,"/system/build.prop") else ""
 sentinels=[root+"/build.prop",root+"/framework/framework-res.apk"]
 before={path:inode_metadata(base,path) for path in sentinels}
 created=False
 with tempfile.TemporaryDirectory(prefix="forge-payload-",dir=output.parent) as temporary:
  work=Path(temporary);context=work/"context";context.write_bytes(CONTEXT)
  props=extract(base,root+"/build.prop",work/"base.prop").read_text()
  if not re.search(r"^ro\.(?:system\.)?build.version.sdk="+str(lock["android_api"])+r"$",props,re.M):
   raise ValueError("base Android API mismatch")
  originals={p:sha256_file(extract(base,p,work/("original"+str(i)))) for i,p in enumerate(sentinels)}
  if exists(base,root+"/app/ForgeOS"):raise ValueError("base already contains ForgeOS")
  try:
   with base.open("rb") as source,output.open("xb") as target:
    created=True;remaining=fs_bytes
    while remaining:
     chunk=source.read(min(2*1024*1024,remaining))
     if not chunk:raise ValueError("base truncated during copy")
     target.write(chunk);remaining-=len(chunk)
   def label(path,directory=False):
    debugfs(output,"set_inode_field "+path+" mode "+("040755" if directory else "0100644"),write=True)
    debugfs(output,"set_inode_field "+path+" uid 0",write=True)
    debugfs(output,"set_inode_field "+path+" gid 0",write=True)
    debugfs(output,"ea_set -f "+tool_path(context)+" "+path+" security.selinux",write=True)
   def mkdir(path):
    if exists(output,path):return
    parent=path.rsplit("/",1)[0]
    if parent:mkdir(parent)
    debugfs(output,"mkdir "+path,write=True);label(path,True)
   payload=[]
   def put(path,source):
    if exists(output,path):raise ValueError("refusing to replace upstream path: "+path)
    mkdir(path.rsplit("/",1)[0])
    debugfs(output,"write "+tool_path(source)+" "+path,write=True);label(path)
    copied=extract(output,path,work/("verify"+str(len(payload))))
    if sha256_file(copied)!=sha256_file(source):raise ValueError("injected payload mismatch")
    meta=inode_metadata(output,path)
    if meta["uid"]!=0 or meta["gid"]!=0 or meta["mode"]!="0644" or "u:object_r:system_file:s0" not in meta["xattrs"]:
     raise ValueError("injected metadata mismatch")
    payload.append({"path":path,"sha256":sha256_file(source),"metadata":meta})
   put(root+"/app/ForgeOS/ForgeOS.apk",apk)
   feature_path=root+"/etc/permissions/android.software.freeform_window_management.xml"
   if exists(output,feature_path):
    feature=extract(output,feature_path,work/"existing-feature.xml")
    if not any(node.get("name")=="android.software.freeform_window_management" for node in ET.parse(feature).getroot().iter("feature")):
     raise ValueError("unexpected existing freeform feature file")
    feature_action="existing upstream declaration retained"
   else:
    feature=work/"feature.xml";feature.write_bytes(FEATURE);put(feature_path,feature)
    feature_action="feature declaration added; platform support still requires runtime acceptance"
   marker=work/"build.json"
   marker.write_text(json.dumps({"acceptance":"EXPERIMENTAL-DO-NOT-FLASH","base_sha256":base_hash,
    "apk_sha256":apk_hash,"source":lock.get("source_release"),"security_patch":lock.get("security_patch"),
    "trusted_avb":False,"vendor_boot_tested":False},indent=2))
   put(root+"/etc/forgeos/build.json",marker)
   for i,path in enumerate(sentinels):
    if inode_metadata(output,path)!=before[path]:raise ValueError("upstream inode metadata changed: "+path)
    if sha256_file(extract(output,path,work/("after"+str(i))))!=originals[path]:
     raise ValueError("upstream content changed: "+path)
   fsck=run(["e2fsck","-f","-n",output],timeout=300)
   if sha256_file(base)!=base_hash:raise ValueError("original input changed")
   report={"acceptance":"EXPERIMENTAL-DO-NOT-FLASH","trusted_avb":False,"boot_tested":False,
    "base_sha256":base_hash,"apk_sha256":apk_hash,"output_sha256":sha256_file(output),
    "output_bytes":output.stat().st_size,"system_root":root,"payloads":payload,"feature":feature_action,
    "fsck_clean":True,"fsck_output":fsck,"upstream_sentinels":before,
    "avb_note":"Only ext4 filesystem bytes copied. Original AVB footer/hash tree/vbmeta omitted; no trusted signing claim.",
    "partition_note":"No partition expansion. No boot/vendor/super/userdata images or flash commands generated."}
   return report
  except Exception:
   if created:output.unlink(missing_ok=True)
   raise

if __name__=="__main__":
 parser=argparse.ArgumentParser();parser.add_argument("--base",required=True);parser.add_argument("--apk",required=True)
 parser.add_argument("--output",required=True);parser.add_argument("--lock",required=True);parser.add_argument("--report",required=True)
 args=parser.parse_args();report=adapt_image(Path(args.base),Path(args.apk),Path(args.output),json.loads(Path(args.lock).read_text()))
 Path(args.report).write_text(json.dumps(report,indent=2));print(json.dumps({k:report[k] for k in ("acceptance","output_bytes","output_sha256","trusted_avb")},indent=2))
