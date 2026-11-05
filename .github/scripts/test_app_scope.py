import subprocess,tempfile,unittest
from pathlib import Path
from app_scope import has_app_changes

class ScopeTests(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory();self.root=Path(self.tmp.name)
  self.git("init","-q");self.commit("android/main.java","initial")
  self.base=self.git("rev-parse","HEAD").strip()
 def tearDown(self):self.tmp.cleanup()
 def git(self,*args):
  return subprocess.check_output(["git","-c","user.name=ScopeTest","-c","user.email=scope@example.invalid",*args],cwd=self.root,text=True,stderr=subprocess.DEVNULL)
 def commit(self,path,text):
  file=self.root/path;file.parent.mkdir(parents=True,exist_ok=True);file.write_text(text)
  self.git("add",".");self.git("commit","-qm","fixture")
 def test_os_only_commits_skip_app(self):
  self.commit("os/image/tool.py","offline")
  self.assertFalse(has_app_changes(self.base,"HEAD",self.root))
 def test_app_change_followed_by_os_commit_is_not_missed(self):
  self.commit("android/main.java","changed");self.commit("os/image/tool.py","offline")
  self.assertTrue(has_app_changes(self.base,"HEAD",self.root))
 def test_workflow_or_bridge_change_requires_app_validation(self):
  self.commit("bridge/server.py","changed")
  self.assertTrue(has_app_changes(self.base,"HEAD",self.root))
 def test_unknown_baseline_cannot_silently_skip(self):
  with self.assertRaises(subprocess.CalledProcessError):has_app_changes("does-not-exist","HEAD",self.root)

if __name__=="__main__":unittest.main()
