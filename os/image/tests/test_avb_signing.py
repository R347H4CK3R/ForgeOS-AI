"""Offline AVB boundary tests; integration requires Linux host tools."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import struct
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import sign_avb as signing


class BoundaryTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory()
        self.root=Path(self.temp.name)
        self.image=self.root/"system.img"
        blob=bytearray(2048)
        blob[1080:1082]=b"\x53\xef"
        struct.pack_into("<I",blob,1028,2)
        self.image.write_bytes(blob)
        self.evidence=self.root/"evidence.json"
        self.public=self.root/"public.pem"; self.public.write_text("fixture")
        self.output=self.root/"signed.img"; self.report=self.root/"report.json"
        self.record={"fsck_clean":True,"trusted_avb":False,"output_bytes":2048,
                     "output_sha256":signing.sha256_file(self.image),
                     "security_patch":"2026-09-01"}
        self.save()
    def tearDown(self):
        self.temp.cleanup()
    def save(self):
        self.evidence.write_text(json.dumps(self.record))
    def invoke(self,**kwargs):
        return signing.sign_system_image(self.image,self.output,self.evidence,
            self.public,"0"*64,"2026-09-01","fixture/fingerprint",self.report,**kwargs)
    def test_reject_existing_output(self):
        self.output.write_bytes(b"preserve")
        with self.assertRaisesRegex(ValueError,"overwrite"): self.invoke()
        self.assertEqual(self.output.read_bytes(),b"preserve")
    def test_reject_hash_and_patch_evidence(self):
        for field,value in (("output_sha256","0"*64),("security_patch","2026-08-01")):
            before=self.record[field]; self.record[field]=value; self.save()
            with self.assertRaises(ValueError): self.invoke()
            self.record[field]=before
        self.assertFalse(self.output.exists())
    def test_reject_wrong_public_pin_cleanup(self):
        def fake_run(args):
            if "extract_public_key" in args:
                Path(args[args.index("--output")+1]).write_bytes(b"wrong public key")
                return ""
            self.fail("must reject public pin before image mutation")
        with patch.dict(os.environ,{"FORGEOS_AVB_DEV_KEY":"throwaway fixture",
                                    "RUNNER_TEMP":str(self.root)}):
            with patch.object(signing,"run",side_effect=fake_run):
                with self.assertRaisesRegex(ValueError,"public key SHA256"): self.invoke()
        self.assertFalse(self.output.exists())
        self.assertFalse(self.report.exists())
        self.assertEqual(list(self.root.glob("forgeos-avb-*")),[])
    def test_credentials_are_not_in_child_environment(self):
        with patch.dict(os.environ,{"FORGEOS_AVB_DEV_KEY":"secret","GH_TOKEN":"secret",
                                    "GITHUB_TOKEN":"secret","AWS_SECRET_ACCESS_KEY":"secret"}):
            env=signing.clean_environment()
            self.assertFalse(any(key in env for key in
                ("FORGEOS_AVB_DEV_KEY","GH_TOKEN","GITHUB_TOKEN","AWS_SECRET_ACCESS_KEY")))
    def test_verifier_alias_is_signed_inode_and_removed_on_success_and_failure(self):
        self.output.write_bytes(b"signed inode")
        original=self.image.read_bytes()
        for fail in (False,True):
            observed=[]
            def fake_run(args):
                alias=Path(args[args.index("--image")+1])
                observed.append(alias)
                self.assertEqual(alias.name,"system.img")
                self.assertNotEqual(alias.parent,self.root)
                self.assertFalse(alias.is_symlink())
                self.assertTrue(os.path.samefile(alias,self.output))
                self.assertEqual(alias.read_bytes(),b"signed inode")
                if fail: raise RuntimeError("fixture verifier failure")
                return ""
            with patch.object(signing,"run",side_effect=fake_run):
                if fail:
                    with self.assertRaisesRegex(RuntimeError,"fixture verifier failure"):
                        signing.verify_system_image(self.output,self.public)
                else:
                    signing.verify_system_image(self.output,self.public)
            self.assertEqual(len(observed),1)
            self.assertFalse(observed[0].parent.exists())
            self.assertEqual(self.image.read_bytes(),original)
            self.assertEqual(self.output.read_bytes(),b"signed inode")
    def test_modified_avbtool_rejected(self):
        tool=self.root/"avbtool.py"; tool.write_text("fixture")
        with self.assertRaisesRegex(ValueError,"checksum"): self.invoke(tool=tool)


LINUX_TOOLS = sys.platform.startswith("linux") and all(shutil.which(t) for t in ("openssl","mke2fs","e2fsck"))

@unittest.skipUnless(LINUX_TOOLS,"Linux OpenSSL and e2fsprogs required")
class IntegrationTests(unittest.TestCase):
    def test_real_ext4_rsa4096_sign_verify_and_prefix(self):
        with tempfile.TemporaryDirectory() as temp:
            root=Path(temp); image=root/"system.img"
            with image.open("wb") as stream: stream.truncate(2*1024*1024)
            subprocess.run(["mke2fs","-q","-t","ext4","-F",str(image)],
                           check=True,capture_output=True)
            key=root/"fixture.pem"; public=root/"public.pem"; avbkey=root/"public.avbpubkey"
            subprocess.run(["openssl","genpkey","-algorithm","RSA","-pkeyopt",
                            "rsa_keygen_bits:4096","-out",str(key)],check=True,capture_output=True)
            subprocess.run(["openssl","pkey","-in",str(key),"-pubout","-out",str(public)],
                           check=True,capture_output=True)
            signing.run([sys.executable,signing.DEFAULT_TOOL,"extract_public_key",
                         "--key",public,"--output",avbkey])
            public_sha=signing.sha256_file(avbkey); original=signing.sha256_file(image)
            evidence=root/"evidence.json"
            evidence.write_text(json.dumps({"fsck_clean":True,"trusted_avb":False,
                "output_bytes":image.stat().st_size,"output_sha256":original,
                "security_patch":"2026-09-01"}))
            with patch.dict(os.environ,{"FORGEOS_AVB_DEV_KEY":key.read_text(),
                                        "RUNNER_TEMP":str(root)}):
                report=signing.sign_system_image(image,root/"signed.img",evidence,
                    public,public_sha,"2026-09-01","fixture/fingerprint",root/"report.json")
            self.assertEqual(signing.sha256_file(image),original)
            self.assertEqual(signing.prefix_sha256(root/"signed.img",2*1024*1024),original)
            self.assertEqual(report["output_bytes"],report["partition_size"])
            self.assertEqual(report["rollback_index"],1788220800)
            self.assertFalse(report["trusted_avb"])
            self.assertFalse(report["boot_tested"])
            self.assertEqual(list(root.glob("forgeos-avb-*")),[])
            with tempfile.TemporaryDirectory(prefix="independent-verify-",dir=root) as verify_dir:
                alias=Path(verify_dir)/"system.img"
                os.link(root/"signed.img",alias)
                signing.run([sys.executable,signing.DEFAULT_TOOL,"verify_image",
                             "--image",alias,"--key",public])
            self.assertEqual(signing.sha256_file(image),original)

if __name__=="__main__": unittest.main()
