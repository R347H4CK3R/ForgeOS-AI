"""Fetch the exact verified same-source ARM64 APK; never extract arbitrary ZIP paths."""
import argparse,json,subprocess,zipfile
from pathlib import Path
from image_tools import verify_file,require_regular

def gh_json(route):
 result=subprocess.run(["gh","api",route],capture_output=True,text=True,encoding="utf-8",timeout=60)
 if result.returncode:raise RuntimeError("GitHub artifact metadata request failed")
 return json.loads(result.stdout)

def extract_apk_archive(archive,destination,expected_sha,expected_bytes,*,entry_name="outputs/apk/debug/app-debug.apk"):
 if entry_name not in ("outputs/apk/debug/app-debug.apk","android/app/build/outputs/apk/debug/app-debug.apk"):
  raise ValueError("unrecognized APK bundle entry")
 destination=Path(destination)
 if destination.exists() or destination.is_symlink():raise ValueError("APK destination must be new")
 with zipfile.ZipFile(require_regular(archive)) as source:
  matches=[entry for entry in source.infolist() if entry.filename==entry_name]
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
 paths=["android",".github/workflows/build.yml",".github/scripts",".github/actions",".gitattributes","os/image/sign_apk.py","os/image/image_tools.py","os/image/forgeos-apk-signing-cert.pem"]
 result=subprocess.run(["git","diff","--quiet",commit,"HEAD","--",*paths],
                       cwd=cwd,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,timeout=30)
 if result.returncode:
  raise ValueError("APK build inputs differ from verified source or Git comparison failed")

def verify_draft_asset(release,pin):
 tag="forgeos-review-"+str(pin["run_id"])+"-"+str(pin["run_attempt"])+"-ForgeOS-native-review"
 if release.get("id")!=pin["release_id"] or release.get("tag_name")!=tag or release.get("target_commitish")!=pin["source_commit"] or release.get("draft") is not True or release.get("prerelease") is not True:
  raise ValueError("review release source, run or publication state mismatch")
 if type(pin["archive_bytes"]) is not int or not 0<pin["archive_bytes"]<=32*1024*1024:
  raise ValueError("review archive size is not bounded")
 matches=[a for a in release.get("assets",[]) if a.get("id")==pin["release_asset_id"]]
 if len(matches)!=1:raise ValueError("review asset ownership mismatch")
 asset=matches[0]
 if asset.get("name")!="ForgeOS-native-review.zip" or asset.get("size")!=pin["archive_bytes"] or asset.get("digest")!="sha256:"+pin["archive_sha256"]:
  raise ValueError("review asset identity, size or digest mismatch")
 return asset


def verify_bundle_provenance(archive,pin):
 with zipfile.ZipFile(require_regular(archive)) as bundle:
  matches=[e for e in bundle.infolist() if e.filename=="android/app/build/outputs/apk/debug/signing-evidence.json"]
  if len(matches)!=1 or matches[0].file_size>16384:raise ValueError("missing or excessive checkout evidence")
  evidence=json.loads(bundle.read(matches[0]))
 original=evidence.get("original_apk",{})
 if evidence.get("schema_version")!=1 or original.get("source")!=pin["source_commit"] or original.get("sha256")!=pin["sha256"] or original.get("bytes")!=pin["bytes"]:
  raise ValueError("actual APK checkout or byte provenance differs from pin")


def fetch(lock,repository,output):
 apk=lock["apk"];output=Path(output);output.mkdir(parents=True,exist_ok=False)
 run=gh_json("repos/"+repository+"/actions/runs/"+str(apk["run_id"]))
 if run["head_sha"]!=apk["source_commit"] or run["conclusion"]!="success":
  raise ValueError("APK source workflow is not the pinned successful commit")
 verify_apk_source(apk["source_commit"])
 if "release_asset_id" in apk:
  if run.get("run_attempt")!=apk["run_attempt"]:raise ValueError("review run attempt mismatch")
  tag="forgeos-review-"+str(apk["run_id"])+"-"+str(apk["run_attempt"])+"-ForgeOS-native-review"
  release=gh_json("repos/"+repository+"/releases/"+str(apk["release_id"]))
  asset=verify_draft_asset(release,apk)
  archive=output/"review.zip"
  route="repos/"+repository+"/releases/assets/"+str(asset["id"])
  with archive.open("xb") as target:
   result=subprocess.run(["gh","api",route,"-H","Accept: application/octet-stream"],stdout=target,stderr=subprocess.PIPE,timeout=180)
  if result.returncode:raise RuntimeError("GitHub review asset download failed")
  verify_file(archive,apk["archive_sha256"],apk["archive_bytes"])
  verify_bundle_provenance(archive,apk)
  entry="android/app/build/outputs/apk/debug/app-debug.apk"
 else:
  artifact=gh_json("repos/"+repository+"/actions/artifacts/"+str(apk["artifact_id"]))
  if artifact["workflow_run"]["id"]!=apk["run_id"] or artifact["expired"] or artifact["size_in_bytes"]>32*1024*1024:
   raise ValueError("APK artifact ownership, expiry or size mismatch")
  archive=output/"artifact.zip"
  with archive.open("xb") as target:
   result=subprocess.run(["gh","api","repos/"+repository+"/actions/artifacts/"+str(apk["artifact_id"])+"/zip"],
                         stdout=target,stderr=subprocess.PIPE,timeout=180)
  if result.returncode:raise RuntimeError("GitHub APK artifact download failed")
  entry="outputs/apk/debug/app-debug.apk"
 target=extract_apk_archive(archive,output/"ForgeOS.apk",apk["sha256"],apk["bytes"],entry_name=entry)
 print("Pinned APK hash and successful same-source workflow verified")
 return target

if __name__=="__main__":
 parser=argparse.ArgumentParser();parser.add_argument("--lock",required=True);parser.add_argument("--repository",required=True);parser.add_argument("--output",required=True)
 args=parser.parse_args();fetch(json.loads(Path(args.lock).read_text()),args.repository,args.output)
