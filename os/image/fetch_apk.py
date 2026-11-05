"""Fetch the exact verified same-source ARM64 APK; never extract arbitrary ZIP paths."""
import argparse,json,subprocess,zipfile
from pathlib import Path
from image_tools import verify_file,require_regular

def gh_json(route):
 result=subprocess.run(["gh","api",route],capture_output=True,text=True,timeout=60)
 if result.returncode:raise RuntimeError("GitHub artifact metadata request failed")
 return json.loads(result.stdout)

def extract_apk_archive(archive,destination,expected_sha,expected_bytes):
 destination=Path(destination)
 if destination.exists() or destination.is_symlink():raise ValueError("APK destination must be new")
 with zipfile.ZipFile(require_regular(archive)) as source:
  matches=[entry for entry in source.infolist() if entry.filename=="outputs/apk/debug/app-debug.apk"]
  if len(matches)!=1 or matches[0].file_size!=expected_bytes:raise ValueError("unexpected APK artifact entry or size")
  created=False
  try:
   with source.open(matches[0]) as stream,destination.open("xb") as target:
    created=True;total=0
    while chunk:=stream.read(2*1024*1024):
     total+=len(chunk)
     if total>expected_bytes:raise ValueError("APK exceeds expected size")
     target.write(chunk)
   verify_file(destination,expected_sha,expected_bytes)
  except Exception:
   if created:destination.unlink(missing_ok=True)
   raise
 return destination

def verify_apk_source(commit,cwd=None):
 paths=["android",".github/workflows/build.yml",".github/scripts",".gitattributes"]
 result=subprocess.run(["git","diff","--quiet",commit,"HEAD","--",*paths],
                       cwd=cwd,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,timeout=30)
 if result.returncode:
  raise ValueError("APK build inputs differ from verified source or Git comparison failed")

def fetch(lock,repository,output):
 apk=lock["apk"];output=Path(output);output.mkdir(parents=True,exist_ok=False)
 run=gh_json("repos/"+repository+"/actions/runs/"+str(apk["run_id"]))
 if run["head_sha"]!=apk["source_commit"] or run["conclusion"]!="success":
  raise ValueError("APK source workflow is not the pinned successful commit")
 verify_apk_source(apk["source_commit"])
 artifact=gh_json("repos/"+repository+"/actions/artifacts/"+str(apk["artifact_id"]))
 if artifact["workflow_run"]["id"]!=apk["run_id"] or artifact["expired"] or artifact["size_in_bytes"]>32*1024*1024:
  raise ValueError("APK artifact ownership, expiry or size mismatch")
 archive=output/"artifact.zip"
 with archive.open("xb") as target:
  result=subprocess.run(["gh","api","repos/"+repository+"/actions/artifacts/"+str(apk["artifact_id"])+"/zip"],
                        stdout=target,stderr=subprocess.PIPE,timeout=180)
 if result.returncode:raise RuntimeError("GitHub APK artifact download failed")
 target=extract_apk_archive(archive,output/"ForgeOS.apk",apk["sha256"],apk["bytes"])
 print("Pinned APK hash and successful same-source workflow verified")
 return target

if __name__=="__main__":
 parser=argparse.ArgumentParser();parser.add_argument("--lock",required=True);parser.add_argument("--repository",required=True);parser.add_argument("--output",required=True)
 args=parser.parse_args();fetch(json.loads(Path(args.lock).read_text()),args.repository,args.output)
