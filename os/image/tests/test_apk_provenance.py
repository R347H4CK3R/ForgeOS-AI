"""A pinned APK is stale when any effective build input changes."""
import subprocess,tempfile,unittest
from pathlib import Path
from fetch_apk import verify_apk_source

class ProvenanceTest(unittest.TestCase):
 def test_real_git_rejects_build_inputs_but_allows_os_docs(self):
  with tempfile.TemporaryDirectory() as directory:
   root=Path(directory)
   def git(*args):
    return subprocess.check_output(["git","-C",str(root),*args],stderr=subprocess.STDOUT,text=True).strip()
   git("init","-q")
   for name in ("android/app/build.gradle",".github/workflows/build.yml",".github/scripts/ui-review.sh",".gitattributes",".github/actions/publish-review/action.yml","os/image/sign_apk.py","os/image/image_tools.py","os/image/forgeos-apk-signing-cert.pem","os/docs/review.md"):
    p=root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text("initial\n")
   git("add",".");git("-c","user.name=Test","-c","user.email=test@example.invalid","commit","-qm","baseline")
   baseline=git("rev-parse","HEAD")
   for name in ("android/app/build.gradle",".github/workflows/build.yml",".github/scripts/ui-review.sh",".gitattributes",".github/actions/publish-review/action.yml","os/image/sign_apk.py","os/image/image_tools.py","os/image/forgeos-apk-signing-cert.pem"):
    with self.subTest(path=name):
     (root/name).write_text("changed\n")
     git("add",name);git("-c","user.name=Test","-c","user.email=test@example.invalid","commit","-qm","change build input")
     with self.assertRaises(ValueError):verify_apk_source(baseline,cwd=root)
     git("checkout",baseline,"--",name)
     git("-c","user.name=Test","-c","user.email=test@example.invalid","commit","-qm","restore build input")
   (root/"os/docs/review.md").write_text("new review\n")
   verify_apk_source(baseline,cwd=root)
 def test_unknown_commit_fails_closed(self):
  with tempfile.TemporaryDirectory() as directory:
   subprocess.run(["git","init","-q",directory],check=True)
   with self.assertRaises(ValueError):verify_apk_source("0"*40,cwd=directory)

