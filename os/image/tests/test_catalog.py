import hashlib,json,lzma,shutil,subprocess,tempfile,unittest
from pathlib import Path
from update_catalog import make_manifest,sign_manifest

class CatalogTests(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory(prefix="forge-catalog-");self.root=Path(self.tmp.name)
  self.raw=b"synthetic system filesystem fixture"*100
  self.image=self.root/"ForgeOS-Android15-ARM64-EXPERIMENTAL-DO-NOT-FLASH.img.xz";self.image.write_bytes(lzma.compress(self.raw))
  self.evidence={"acceptance":"EXPERIMENTAL-DO-NOT-FLASH","fsck_clean":True,"trusted_avb":False,
    "security_patch":"2026-01-05","output_bytes":len(self.raw),"output_sha256":hashlib.sha256(self.raw).hexdigest()}
 def tearDown(self):self.tmp.cleanup()
 def manifest(self):return make_manifest(self.evidence,self.image,123,"a"*64,987,456,"b"*40)
 def test_catalog_binds_actual_image_and_artifact(self):
  value=self.manifest()
  self.assertEqual(value["repository"],"R347H4CK3R/ForgeOS-AI");self.assertEqual(value["sequence"],456)
  self.assertEqual(value["image"]["artifact_id"],123);self.assertEqual(value["image"]["archive_sha256"],"a"*64)
  self.assertEqual(value["image"]["compressed_sha256"],hashlib.sha256(self.image.read_bytes()).hexdigest())
  self.assertEqual(value["security_patch"],"2026-01-05")
  self.assertEqual(value["install_method"],"stage-only");self.assertFalse(value["boot_verified"])
 def test_unclean_or_wrong_raw_image_is_not_signable(self):
  self.evidence["fsck_clean"]=False
  with self.assertRaises(ValueError):self.manifest()
  self.evidence["fsck_clean"]=True;self.evidence["output_sha256"]="0"*64
  with self.assertRaisesRegex(ValueError,"raw image"):self.manifest()
 def test_bad_artifact_identity_rejected(self):
  with self.assertRaises(ValueError):make_manifest(self.evidence,self.image,0,"a"*64,987,456,"b"*40)
  with self.assertRaises(ValueError):make_manifest(self.evidence,self.image,123,"bad",987,456,"b"*40)
 def test_real_signature_and_tamper_detection(self):
  ssl=shutil.which("openssl") or str(Path("C:/Program Files/Git/usr/bin/openssl.exe"))
  if not Path(ssl).exists():self.skipTest("OpenSSL unavailable")
  private=self.root/"private.pem";public=self.root/"public.pem"
  subprocess.run([ssl,"genpkey","-algorithm","RSA","-pkeyopt","rsa_keygen_bits:3072","-out",private],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
  subprocess.run([ssl,"pkey","-in",private,"-pubout","-out",public],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
  output=self.root/"signed";sign_manifest(self.manifest(),output,private,public,ssl)
  result=subprocess.run([ssl,"dgst","-sha256","-verify",public,"-signature",output/"update.sig",output/"update.json"],capture_output=True)
  self.assertEqual(result.returncode,0)
  (output/"update.json").write_bytes(b"tampered")
  result=subprocess.run([ssl,"dgst","-sha256","-verify",public,"-signature",output/"update.sig",output/"update.json"],capture_output=True)
  self.assertNotEqual(result.returncode,0)

if __name__=="__main__":unittest.main()
