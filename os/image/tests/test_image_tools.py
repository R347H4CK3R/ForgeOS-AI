import hashlib,struct,tempfile,unittest
from pathlib import Path
from image_tools import verify_file,image_kind,require_regular

class ImageToolsTests(unittest.TestCase):
 def setUp(self):
  self.temp=tempfile.TemporaryDirectory();self.path=Path(self.temp.name)/"image"
  self.path.write_bytes(b"owned image fixture")
 def tearDown(self):self.temp.cleanup()
 def test_exact_hash_and_size_required(self):
  data=self.path.read_bytes()
  self.assertEqual(verify_file(self.path,hashlib.sha256(data).hexdigest(),len(data)),hashlib.sha256(data).hexdigest())
  with self.assertRaisesRegex(ValueError,"SHA256"):verify_file(self.path,"0"*64,len(data))
  with self.assertRaisesRegex(ValueError,"size"):verify_file(self.path,hashlib.sha256(data).hexdigest(),len(data)+1)
 def test_bootstrap_pin_is_inspection_only(self):
  with self.assertRaisesRegex(ValueError,"unpinned"):verify_file(self.path,None,self.path.stat().st_size)
  self.assertEqual(len(verify_file(self.path,None,self.path.stat().st_size,allow_unpinned=True)),64)
 def test_nonregular_inputs_rejected(self):
  with self.assertRaises(ValueError):require_regular(Path(self.temp.name))
  if Path("/dev/null").exists():
   with self.assertRaises(ValueError):require_regular(Path("/dev/null"))
 def test_only_known_image_containers_classified(self):
  with self.assertRaisesRegex(ValueError,"format"):image_kind(self.path)
  self.path.write_bytes(struct.pack("<I",0xed26ff3a)+bytes(1100));self.assertEqual(image_kind(self.path),"sparse")
  image=bytearray(2048);image[1080:1082]=b"\x53\xef";self.path.write_bytes(image)
  self.assertEqual(image_kind(self.path),"ext4")
 def test_symlink_inputs_rejected(self):
  link=self.path.parent/"link"
  try:link.symlink_to(self.path)
  except OSError:self.skipTest("symlink privilege unavailable")
  with self.assertRaises(ValueError):require_regular(link)

if __name__=="__main__":unittest.main()
