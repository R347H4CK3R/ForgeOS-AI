import hashlib,io,json,sys,tempfile,unittest,zipfile
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
try:
 from verify_sidecar import validate_pin,verify_apk
except ModuleNotFoundError:
 validate_pin=verify_apk=None

class SidecarVerification(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory()
  self.addCleanup(self.tmp.cleanup)
  self.path=Path(self.tmp.name)/"test.apk"
  with zipfile.ZipFile(self.path,"w") as z:
   z.writestr("AndroidManifest.xml",b"test manifest")
   z.writestr("lib/arm64-v8a/libtest.so",b"native fixture")
  self.pin=json.loads((Path(__file__).resolve().parents[1]/"sidecar.lock.json").read_text())
  self.pin.update(bytes=self.path.stat().st_size,sha256=hashlib.sha256(self.path.read_bytes()).hexdigest())
 def require_verifier(self):
  self.assertIsNotNone(verify_apk,"Pinned APK verifier is not implemented")
 def test_hash_check_does_not_claim_android_signature_or_boot(self):
  self.require_verifier()
  result=verify_apk(self.path,self.pin)
  self.assertEqual(result["sha256"],self.pin["sha256"])
  self.assertIs(result["apk_signature_verified"],False)
  self.assertIs(result["device_boot_verified"],False)
 def test_equal_length_tampering_rejected(self):
  self.require_verifier()
  data=bytearray(self.path.read_bytes());data[40]^=1;self.path.write_bytes(data)
  with self.assertRaisesRegex(ValueError,"SHA256"):verify_apk(self.path,self.pin)
 def test_truncated_download_rejected(self):
  self.require_verifier()
  self.path.write_bytes(self.path.read_bytes()[:-1])
  with self.assertRaisesRegex(ValueError,"size"):verify_apk(self.path,self.pin)
 def test_wrong_download_origin_rejected(self):
  self.require_verifier()
  self.pin["url"]="https://github.com.attacker.test/ExTV/Podroid/releases/download/v1.2.9/Podroid-v1.2.9-release.apk"
  with self.assertRaises(ValueError):validate_pin(self.pin)
 def test_archive_without_arm64_rejected_even_with_matching_hash(self):
  self.require_verifier()
  with zipfile.ZipFile(self.path,"w") as z:z.writestr("AndroidManifest.xml",b"test")
  self.pin.update(bytes=self.path.stat().st_size,sha256=hashlib.sha256(self.path.read_bytes()).hexdigest())
  with self.assertRaisesRegex(ValueError,"ARM64"):verify_apk(self.path,self.pin)
if __name__=="__main__":unittest.main()
