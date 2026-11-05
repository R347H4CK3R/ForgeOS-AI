import hashlib,tempfile,unittest,zipfile
from pathlib import Path
from fetch_apk import extract_apk_archive

class ArtifactTests(unittest.TestCase):
 def setUp(self):self.tmp=tempfile.TemporaryDirectory();self.root=Path(self.tmp.name);self.archive=self.root/"artifact.zip"
 def tearDown(self):self.tmp.cleanup()
 def test_only_exact_entry_extracted_and_traversal_ignored(self):
  with zipfile.ZipFile(self.archive,"w") as archive:
   archive.writestr("../../outside",b"never")
   archive.writestr("outputs/apk/debug/app-debug.apk",b"approved")
  output=self.root/"ForgeOS.apk"
  extract_apk_archive(self.archive,output,hashlib.sha256(b"approved").hexdigest(),8)
  self.assertEqual(output.read_bytes(),b"approved");self.assertFalse((self.root.parent/"outside").exists())
 def test_wrong_hash_removes_only_new_partial_output(self):
  with zipfile.ZipFile(self.archive,"w") as archive:archive.writestr("outputs/apk/debug/app-debug.apk",b"bad")
  output=self.root/"ForgeOS.apk"
  with self.assertRaisesRegex(ValueError,"SHA256"):extract_apk_archive(self.archive,output,"0"*64,3)
  self.assertFalse(output.exists())
 def test_existing_output_preserved(self):
  output=self.root/"ForgeOS.apk";output.write_bytes(b"keep")
  with self.assertRaises(ValueError):extract_apk_archive(self.archive,output,"0"*64,3)
  self.assertEqual(output.read_bytes(),b"keep")
 def test_unexpected_size_rejected_before_output(self):
  with zipfile.ZipFile(self.archive,"w") as archive:archive.writestr("outputs/apk/debug/app-debug.apk",b"too big")
  output=self.root/"ForgeOS.apk"
  with self.assertRaises(ValueError):extract_apk_archive(self.archive,output,"0"*64,1)
  self.assertFalse(output.exists())

if __name__=="__main__":unittest.main()
