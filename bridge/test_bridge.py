import tempfile, unittest
from pathlib import Path
from server import Workspace
class WorkspaceTests(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory()
  self.root=Path(self.tmp.name)
  self.ws=Workspace(self.root)
 def tearDown(self): self.tmp.cleanup()
 def test_read_existing_utf8(self):
  (self.root/"main.py").write_text("print(42)",encoding="utf-8")
  self.assertEqual(self.ws.read("main.py"),"print(42)")
 def test_traversal_rejected(self):
  for name in ("../secret","/etc/passwd","C:\\Windows","a/../../secret"):
   with self.assertRaises(ValueError): self.ws.resolve(name)
 def test_symlink_escape_rejected(self):
  with tempfile.TemporaryDirectory() as outside:
   try: (self.root/"escape").symlink_to(outside,target_is_directory=True)
   except OSError: self.skipTest("symlink permission unavailable")
   with self.assertRaises(ValueError): self.ws.resolve("escape/file")
 def test_oversize_read_rejected(self):
  (self.root/"big").write_bytes(b"x"*262145)
  with self.assertRaises(ValueError): self.ws.read("big")
if __name__=="__main__": unittest.main()
