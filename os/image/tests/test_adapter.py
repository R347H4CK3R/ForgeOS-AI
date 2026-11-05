import hashlib,json,shutil,struct,subprocess,tempfile,unittest,zipfile
from pathlib import Path
from build_image import adapt_image
from image_tools import debugfs,extract,sha256_file

@unittest.skipUnless(shutil.which("mke2fs") and shutil.which("debugfs"),"Linux ext4 tools required")
class AdapterTests(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory(prefix="forge-image-");self.root=Path(self.tmp.name)
  seed=self.root/"seed";(seed/"system/framework").mkdir(parents=True);(seed/"system/app").mkdir()
  (seed/"system/etc/permissions").mkdir(parents=True)
  (seed/"system/build.prop").write_text("ro.system.build.version.sdk=35\nro.system.build.version.security_patch=2025-03-05\n")
  (seed/"system/framework/framework-res.apk").write_bytes(b"original framework bytes")
  self.base=self.root/"base.img"
  subprocess.run(["truncate","-s","64M",self.base],check=True)
  subprocess.run(["mke2fs","-q","-t","ext4","-F","-d",seed,self.base],check=True)
  self.context=self.root/"context";self.context.write_bytes(b"u:object_r:system_file:s0\0")
  debugfs(self.base,"ea_set -f "+str(self.context)+" /system/framework/framework-res.apk security.selinux",write=True)
  debugfs(self.base,"set_inode_field /system/framework/framework-res.apk uid 123",write=True)
  self.apk=self.root/"ForgeOS.apk";elf=bytearray(64);elf[:6]=b"\x7fELF\x02\x01";elf[18:20]=struct.pack("<H",183)
  with zipfile.ZipFile(self.apk,"w") as archive:
   archive.writestr("AndroidManifest.xml",b"synthetic fixture")
   archive.writestr("classes.dex",b"synthetic fixture")
   archive.writestr("lib/arm64-v8a/libforge_ai.so",elf)
  self.lock={"raw_sha256":sha256_file(self.base),"android_api":35,"apk":{"sha256":sha256_file(self.apk),"bytes":self.apk.stat().st_size}}
 def tearDown(self):self.tmp.cleanup()
 def test_real_ext4_integration_preserves_original_and_labels_new_files(self):
  original=sha256_file(self.base);before=debugfs(self.base,"stat /system/framework/framework-res.apk")
  output=self.root/"output.img";report=adapt_image(self.base,self.apk,output,self.lock)
  self.assertEqual(sha256_file(self.base),original)
  copied=extract(output,"/system/app/ForgeOS/ForgeOS.apk",self.root/"copied.apk")
  self.assertEqual(copied.read_bytes(),self.apk.read_bytes())
  stat=debugfs(output,"stat /system/app/ForgeOS/ForgeOS.apk")
  self.assertIn("Mode:  0644",stat);self.assertRegex(stat,r"User:\s+0\s+Group:\s+0")
  self.assertIn("u:object_r:system_file:s0",stat)
  after=debugfs(output,"stat /system/framework/framework-res.apk")
  self.assertIn("u:object_r:system_file:s0",after);self.assertRegex(after,r"User:\s+123")
  self.assertEqual(extract(output,"/system/framework/framework-res.apk",self.root/"framework").read_bytes(),b"original framework bytes")
  self.assertEqual(report["acceptance"],"EXPERIMENTAL-DO-NOT-FLASH")
  self.assertTrue(report["fsck_clean"]);self.assertFalse(report["trusted_avb"])
 def test_wrong_base_rejected_without_creating_output(self):
  self.lock["raw_sha256"]="0"*64;output=self.root/"output.img"
  with self.assertRaisesRegex(ValueError,"SHA256"):adapt_image(self.base,self.apk,output,self.lock)
  self.assertFalse(output.exists())
 def test_existing_output_never_overwritten(self):
  output=self.root/"output.img";output.write_bytes(b"keep")
  with self.assertRaises(ValueError):adapt_image(self.base,self.apk,output,self.lock)
  self.assertEqual(output.read_bytes(),b"keep")

if __name__=="__main__":unittest.main()
