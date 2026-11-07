import copy,hashlib,json,tempfile,unittest,zipfile
from pathlib import Path
from fetch_apk import verify_draft_asset,extract_apk_archive,verify_bundle_provenance

class DraftApkTests(unittest.TestCase):
 def setUp(self):
  self.pin={"release_id":77,"run_id":123,"run_attempt":1,"source_commit":"a"*40,"release_asset_id":55,"archive_bytes":100,"archive_sha256":"b"*64}
  self.release={"id":77,"tag_name":"forgeos-review-123-1-ForgeOS-native-review","target_commitish":"a"*40,"draft":True,"prerelease":True,"assets":[{"id":55,"name":"ForgeOS-native-review.zip","size":100,"digest":"sha256:"+"b"*64}]}
 def test_exact_unpublished_same_source_asset(self):
  self.assertEqual(verify_draft_asset(self.release,self.pin)["id"],55)
 def test_wrong_source_run_identity_or_hash_rejected(self):
  for key,value in (("id",78),("target_commitish","c"*40),("tag_name","other"),("draft",False)):
   bad=copy.deepcopy(self.release);bad[key]=value
   with self.subTest(key=key),self.assertRaises(ValueError):verify_draft_asset(bad,self.pin)
  for key,value in (("size",101),("digest","sha256:"+"c"*64),("id",56),("name","other.zip")):
   bad=copy.deepcopy(self.release);bad["assets"][0][key]=value
   with self.subTest(key=key),self.assertRaises(ValueError):verify_draft_asset(bad,self.pin)
 def test_exact_new_bundle_entry(self):
  with tempfile.TemporaryDirectory() as folder:
   root=Path(folder);archive=root/"review.zip"
   with zipfile.ZipFile(archive,"w") as z:
    z.writestr("android/app/build/outputs/apk/debug/app-debug.apk",b"apk")
    z.writestr("../../unwanted",b"no")
   target=extract_apk_archive(archive,root/"app.apk",hashlib.sha256(b"apk").hexdigest(),3,entry_name="android/app/build/outputs/apk/debug/app-debug.apk")
   self.assertEqual(target.read_bytes(),b"apk")
 def test_branch_target_cannot_hide_different_merge_checkout(self):
  with tempfile.TemporaryDirectory() as folder:
   path=Path(folder)/"bundle.zip"
   pin=dict(self.pin,sha256="d"*64,bytes=3)
   for source in ("a"*40,"c"*40):
    with zipfile.ZipFile(path,"w") as z:
     z.writestr("android/app/build/outputs/apk/debug/signing-evidence.json",json.dumps({"schema_version":1,"original_apk":{"source":source,"sha256":"d"*64,"bytes":3}}))
    if source==pin["source_commit"]:verify_bundle_provenance(path,pin)
    else:
     with self.assertRaisesRegex(ValueError,"checkout"):verify_bundle_provenance(path,pin)
 def test_unknown_entry_cannot_be_selected(self):
  with self.assertRaises(ValueError):extract_apk_archive("missing.zip","missing.apk","a"*64,3,entry_name="../bad")

if __name__=="__main__":unittest.main()
