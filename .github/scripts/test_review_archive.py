import hashlib,json,os,tempfile,unittest,zipfile
from pathlib import Path
try:
 from review_archive import build_archive
except ModuleNotFoundError:
 build_archive=None

class ReviewArchiveTests(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup)
  self.base=Path(self.tmp.name);self.root=self.base/"work";self.root.mkdir()
  self.output=self.base/"review.zip"
 def require(self):self.assertIsNotNone(build_archive,"Quota-independent review archive is not implemented")
 def test_only_requested_files_and_hash_manifest_are_published(self):
  self.require()
  (self.root/"reports").mkdir();(self.root/"reports"/"result.txt").write_bytes(b"verified")
  (self.root/"private.key").write_bytes(b"never publish")
  result=build_archive(self.root,["reports/"],self.output)
  with zipfile.ZipFile(self.output) as z:
   self.assertEqual(set(z.namelist()),{"reports/result.txt","MANIFEST.json"})
   self.assertEqual(z.read("reports/result.txt"),b"verified")
   manifest=json.loads(z.read("MANIFEST.json"))
   self.assertEqual(manifest["files"][0]["path"],"reports/result.txt")
   self.assertEqual(manifest["files"][0]["bytes"],8)
   self.assertEqual(manifest["files"][0]["sha256"],hashlib.sha256(b"verified").hexdigest())
  self.assertEqual(result["files"],1)
 def test_parent_path_cannot_escape_workspace(self):
  self.require();(self.base/"private.key").write_bytes(b"secret")
  with self.assertRaises(ValueError):build_archive(self.root,["../private.key"],self.output)
  self.assertFalse(self.output.exists())
 @unittest.skipIf(os.name=="nt","Windows link privilege differs; mandatory in Linux CI")
 def test_symlink_cannot_publish_external_secret(self):
  self.require();secret=self.base/"secret";secret.write_bytes(b"secret")
  (self.root/"result").symlink_to(secret)
  with self.assertRaises(ValueError):build_archive(self.root,["result"],self.output)
  self.assertFalse(self.output.exists())
 def test_missing_outputs_do_not_make_empty_success_artifact(self):
  self.require()
  with self.assertRaises(ValueError):build_archive(self.root,["missing/*.txt"],self.output)
  self.assertFalse(self.output.exists())
 def test_excessive_output_rejected_before_reading(self):
  self.require()
  with (self.root/"huge.log").open("wb") as f:f.truncate(512*1024**2+1)
  with self.assertRaises(ValueError):build_archive(self.root,["huge.log"],self.output)
  self.assertFalse(self.output.exists())
if __name__=="__main__":unittest.main()
