import os,subprocess,tempfile,unittest
from pathlib import Path
from agent import Agent,Project
class WindowsBoundaries(unittest.TestCase):
 def test_windows_alias_and_reserved_components_rejected(self):
  with tempfile.TemporaryDirectory() as root:
   (Path(root)/"main.py").write_text("print(1)")
   for name in ("main.py.","main.py ","folder./main.py","NUL","CON.txt","COM1.py"):
    with self.assertRaises(ValueError,msg=name):Project(root).path(name)
 @unittest.skipUnless(os.name=="nt","Windows junction semantics")
 def test_junction_cannot_enumerate_or_redirect_backups(self):
  with tempfile.TemporaryDirectory() as root,tempfile.TemporaryDirectory() as outside:
   base=Path(root);project=base/"demo";project.mkdir();(project/"main.py").write_text("print(1)")
   (Path(outside)/"secret.txt").write_text("not in project")
   junction=project/".forgeos";alias=project/"alias"
   try:
    for link in (junction,alias):
     result=subprocess.run(["cmd","/c","mklink","/J",str(link),outside],capture_output=True)
     if result.returncode:self.skipTest("junction creation unavailable")
    self.assertNotIn("alias/secret.txt",Project(project).files())
    with self.assertRaises(ValueError):Project(project).path("alias/secret.txt")
    replies=iter([{"tool":"read_file","args":{"path":"main.py"}},{"tool":"propose_edit","args":{"path":"main.py","content":"print(2)"}},{"tool":"finish","args":{}}])
    agent=Agent(base,lambda *a:next(replies));plan=agent.plan("demo","fix it","local")
    with self.assertRaises(ValueError):agent.approve(plan["plan_id"],plan["plan_sha256"],True)
    self.assertFalse((Path(outside)/"backups").exists());self.assertEqual((project/"main.py").read_text(),"print(1)")
   finally:
    for link in (junction,alias):
     if link.exists():os.rmdir(link)
if __name__=="__main__":unittest.main()
