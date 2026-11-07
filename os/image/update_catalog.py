"""Sign exact repository update bytes only after image/evidence verification."""
import argparse,hashlib,json,lzma,os,re,shutil,subprocess,tempfile
from pathlib import Path
from image_tools import require_regular,sha256_file
REPOSITORY="R347H4CK3R/ForgeOS-AI"

def make_manifest(evidence,image,artifact_id,archive_sha,archive_bytes,run_id,commit):
 if evidence.get("acceptance")!="EXPERIMENTAL-DO-NOT-FLASH" or evidence.get("fsck_clean") is not True or evidence.get("trusted_avb") is not False:
  raise ValueError("image has not passed offline acceptance")
 if any(type(n) is not int or n<=0 for n in (artifact_id,archive_bytes,run_id)) or not re.fullmatch("[0-9a-f]{64}",archive_sha) or not re.fullmatch("[0-9a-f]{40}",commit):
  raise ValueError("invalid artifact/run identity")
 patch=evidence.get("security_patch","")
 if not re.fullmatch(r"[0-9]{4}-[0-9]{2}-[0-9]{2}",patch):raise ValueError("missing image security patch evidence")
 image=require_regular(image);size=image.stat().st_size
 if not 1<=size<=2*1024**3 or not size<=archive_bytes<=min(size+1024**2,2*1024**3+64*1024**2):
  raise ValueError("unexpected image/archive size")
 raw_hash=hashlib.sha256();raw_bytes=0
 with lzma.open(image,"rb") as source:
  for chunk in iter(lambda:source.read(2*1024**2),b""):
   raw_bytes+=len(chunk)
   if raw_bytes>evidence["output_bytes"] or raw_bytes>8*1024**3:raise ValueError("raw image exceeds evidence")
   raw_hash.update(chunk)
 if raw_bytes!=evidence["output_bytes"] or raw_hash.hexdigest()!=evidence["output_sha256"]:
  raise ValueError("compressed raw image does not match evidence")
 return {"schema":1,"repository":REPOSITORY,"channel":"experimental","sequence":run_id,
  "version":"0.3.0-os."+commit[:12]+"."+str(run_id),"source_commit":commit,"android_api":35,"abi":"arm64-v8a",
  "kind":"raw-gsi","install_method":"stage-only","boot_verified":False,"security_patch":patch,
  "image":{"artifact_id":artifact_id,"archive_sha256":archive_sha,"archive_bytes":archive_bytes,
   "name":image.name,"compressed_bytes":size,"compressed_sha256":sha256_file(image),"raw_sha256":raw_hash.hexdigest()}}

def openssl(args,executable):
 env={k:v for k,v in os.environ.items() if k not in ("FORGEOS_UPDATE_SIGNING_KEY","FORGEOS_APK_SIGNING_KEY","GH_TOKEN","GITHUB_TOKEN")}
 result=subprocess.run([str(executable),*[str(a) for a in args]],stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=60,env=env)
 if result.returncode:raise RuntimeError("OpenSSL signing/verification failed")
 return result.stdout

def sign_manifest(manifest,output,private,public,executable="openssl"):
 output=Path(output)
 if output.exists():raise ValueError("signed metadata output must be new")
 with tempfile.TemporaryDirectory(prefix="forge-sign-check-") as temporary:
  derived=Path(temporary)/"derived.der";pinned=Path(temporary)/"pinned.der"
  openssl(["pkey","-in",require_regular(private),"-pubout","-outform","DER","-out",derived],executable)
  openssl(["pkey","-pubin","-in",require_regular(public),"-outform","DER","-out",pinned],executable)
  if derived.read_bytes()!=pinned.read_bytes():raise ValueError("Signing key does not match app's pinned public key")
 output.mkdir()
 try:
  document=output/"update.json";signature=output/"update.sig"
  document.write_bytes((json.dumps(manifest,sort_keys=True,separators=(",",":"))+"\n").encode("utf-8"))
  openssl(["dgst","-sha256","-sign",private,"-out",signature,document],executable)
  openssl(["dgst","-sha256","-verify",public,"-signature",signature,document],executable)
  if signature.stat().st_size!=384:raise ValueError("Expected RSA3072 signature")
 except Exception:
  shutil.rmtree(output);raise

if __name__=="__main__":
 parser=argparse.ArgumentParser();parser.add_argument("--evidence",required=True);parser.add_argument("--image",required=True)
 parser.add_argument("--artifact-id",required=True,type=int);parser.add_argument("--archive-sha",required=True);parser.add_argument("--archive-bytes",required=True,type=int)
 parser.add_argument("--run-id",required=True,type=int);parser.add_argument("--commit",required=True);parser.add_argument("--public-key",required=True);parser.add_argument("--output",required=True)
 args=parser.parse_args()
 manifest=make_manifest(json.loads(Path(args.evidence).read_text()),Path(args.image),args.artifact_id,args.archive_sha.removeprefix("sha256:"),args.archive_bytes,args.run_id,args.commit)
 secret=os.environ.get("FORGEOS_UPDATE_SIGNING_KEY")
 if not secret:raise SystemExit("Repository update signing key is not configured")
 with tempfile.TemporaryDirectory(prefix="forge-private-key-",dir=os.environ.get("RUNNER_TEMP")) as temporary:
  private=Path(temporary)/"signing.pem"
  fd=os.open(private,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
  with os.fdopen(fd,"w") as stream:stream.write(secret)
  sign_manifest(manifest,Path(args.output),private,Path(args.public_key))
 print(json.dumps({key:manifest[key] for key in ("version","sequence","install_method")}))
