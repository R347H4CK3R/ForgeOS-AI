"""Skip heavy APK jobs only when source matches a previous successful branch build."""
import json,os,re,subprocess
from pathlib import Path

def has_app_changes(base,head,repo="."):
 result=subprocess.run(["git","diff","--name-only","-z",base,head,"--"],cwd=repo,capture_output=True,check=True)
 paths=[value.decode("utf-8",errors="surrogateescape") for value in result.stdout.split(b"\0") if value]
 return any(path.startswith(("android/","bridge/",".github/scripts/",".github/actions/")) or path in (".github/workflows/build.yml",".gitattributes") for path in paths)

def main():
 needed=True
 if os.environ.get("GITHUB_EVENT_NAME")=="pull_request":
  try:
   repository=os.environ["GITHUB_REPOSITORY"];branch=os.environ["GITHUB_HEAD_REF"]
   route="repos/"+repository+"/actions/workflows/build.yml/runs?status=success&event=pull_request&per_page=30"
   result=subprocess.run(["gh","api",route,"--jq",".workflow_runs | map({head_branch,head_sha,head_repository})"],
                         capture_output=True,text=True,check=True,timeout=30)
   for run in json.loads(result.stdout):
    if run["head_branch"]==branch and (run.get("head_repository") or {}).get("full_name")==os.environ.get("HEAD_REPOSITORY"):
     sha=run["head_sha"]
     if not re.fullmatch(r"[0-9a-f]{40}",sha):raise ValueError("invalid baseline")
     needed=has_app_changes(sha,"HEAD");break
  except (OSError,KeyError,ValueError,subprocess.SubprocessError):
   print("Scope history unavailable; validate all app jobs")
 print("App source validation required: "+str(needed))
 with Path(os.environ["GITHUB_OUTPUT"]).open("a") as output:output.write("needed="+str(needed).lower()+"\n")

if __name__=="__main__":main()
