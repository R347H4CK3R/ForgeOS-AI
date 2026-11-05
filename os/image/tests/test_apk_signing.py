"""OpenSSL fixture tests; never access production signing keys."""
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import sign_apk as signing

OPENSSL = shutil.which("openssl") or r"C:\Program Files\Git\usr\bin\openssl.exe"

@unittest.skipUnless(Path(OPENSSL).exists() or shutil.which(OPENSSL), "OpenSSL required")
class SigningTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.key = self.root/"fixture.pem"
        self.cert = self.root/"fixture.crt"
        subprocess.run([OPENSSL,"req","-x509","-newkey","rsa:2048","-nodes",
                        "-keyout",str(self.key),"-out",str(self.cert),
                        "-subj","/CN=Throwaway test/","-days","1"],check=True,
                       stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    def tearDown(self):
        self.temp.cleanup()
    def test_der_pin_and_rejected_hash(self):
        der = subprocess.run([OPENSSL,"x509","-in",str(self.cert),"-outform","DER"],
                             check=True,capture_output=True).stdout
        self.assertEqual(signing.certificate_sha256(self.cert,OPENSSL),
                         hashlib.sha256(der).hexdigest())
        with self.assertRaisesRegex(ValueError,"certificate SHA256"):
            signing.check_certificate(self.cert,"0"*64,OPENSSL)
    def test_matching_and_mismatching_keys(self):
        signing.check_key_certificate(self.key,self.cert,OPENSSL)
        other = self.root/"other.pem"
        subprocess.run([OPENSSL,"genpkey","-algorithm","RSA","-pkeyopt",
                        "rsa_keygen_bits:2048","-out",str(other)],check=True,
                       stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
        with self.assertRaisesRegex(ValueError,"does not match"):
            signing.check_key_certificate(other,self.cert,OPENSSL)
    def test_existing_output_before_tools_or_key(self):
        apk=self.root/"in.apk"; apk.write_bytes(b"input")
        out=self.root/"out.apk"; out.write_bytes(b"preserve")
        with patch.dict(os.environ,{},clear=True):
            with self.assertRaisesRegex(ValueError,"overwrite"):
                signing.sign_apk(apk,out,self.cert,"0"*64,"missing",
                                 self.root/"evidence.json",openssl=OPENSSL)
        self.assertEqual(out.read_bytes(),b"preserve")
    def test_input_hash_and_size_rejected_before_tools(self):
        apk=self.root/"in.apk"; apk.write_bytes(b"input")
        for kwargs, message in (({"expected_input_sha256":"0"*64},"input SHA256"),
                                ({"expected_input_bytes":7},"input size")):
            with self.assertRaisesRegex(ValueError,message):
                signing.sign_apk(apk,self.root/"out.apk",self.cert,"0"*64,
                                 "missing",self.root/"evidence.json",
                                 openssl=OPENSSL,**kwargs)
    def test_failure_cleans_private_temporary_files(self):
        apk=self.root/"in.apk"; apk.write_bytes(b"input")
        cert_hash=signing.certificate_sha256(self.cert,OPENSSL)
        original_run=signing.run_private
        observed=[]
        def fake_run(args, **kwargs):
            if args[0]=="fixture-apksigner":
                if args[1]=="verify":
                    return ""
                key_path=Path(args[args.index("--key")+1])
                observed.append(key_path)
                self.assertTrue(key_path.is_file())
                if os.name!="nt":
                    self.assertEqual(key_path.stat().st_mode & 0o777,0o600)
                raise RuntimeError("fixture failure")
            return original_run(args,**kwargs)
        with patch.dict(os.environ,{"RUNNER_TEMP":str(self.root),
                                    "FORGEOS_APK_SIGNING_KEY":self.key.read_text()}):
            with patch.object(signing,"run_private",side_effect=fake_run):
                with self.assertRaisesRegex(RuntimeError,"fixture failure"):
                    signing.sign_apk(apk,self.root/"out.apk",self.cert,cert_hash,
                                     "fixture-apksigner",self.root/"evidence.json",
                                     openssl=OPENSSL)
        self.assertEqual(len(observed),1)
        self.assertFalse(observed[0].parent.exists())
        self.assertFalse((self.root/"out.apk").exists())
        self.assertFalse((self.root/"evidence.json").exists())
    def test_subprocess_environment_does_not_include_secret(self):
        with patch.dict(os.environ,{"FORGEOS_APK_SIGNING_KEY":"fixture sentinel"}):
            with patch.object(signing.subprocess,"run") as run:
                run.return_value.returncode=0
                run.return_value.stdout=b"ok"
                self.assertEqual(signing.run_private(["fixture"]),"ok")
                self.assertNotIn("FORGEOS_APK_SIGNING_KEY",run.call_args.kwargs["env"])
    def test_exact_single_signer(self):
        digest="a"*64
        line="Signer #1 certificate SHA-256 digest: "+digest
        signing.verify_signer_digest(line,digest)
        for output in ("",line.replace(digest,"b"*64),
                       line+"\nSigner #2 certificate SHA-256 digest: "+digest):
            with self.assertRaises(ValueError):
                signing.verify_signer_digest(output,digest)

if __name__ == "__main__":
    unittest.main()
