"""Compile and exercise the actual Android-free launch policy on a JDK."""
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]


@unittest.skipUnless(shutil.which("javac") and shutil.which("java"), "JDK required; run in Actions")
class EnvironmentLaunchPolicy(unittest.TestCase):
    def test_untrusted_or_unprovisioned_companion_cannot_launch(self):
        # Dropping any trust/permission/API gate must break this real compiled probe.
        harness = """
package dev.forgeos.ai;
public final class EnvironmentPolicyProbe {
 private static void check(boolean value,String name) {
  if(!value) throw new AssertionError(name);
 }
 public static void main(String[] args) {
  check(EnvironmentPolicy.assess(33,true,true,true,true,true)==EnvironmentPolicy.State.UNSUPPORTED,"API floor");
  check(EnvironmentPolicy.assess(35,false,true,true,true,true)==EnvironmentPolicy.State.UNSUPPORTED,"feature missing");
  check(EnvironmentPolicy.assess(35,true,false,false,false,false)==EnvironmentPolicy.State.INSTALL_REQUIRED,"install needed");
  check(EnvironmentPolicy.assess(35,true,true,false,true,true)==EnvironmentPolicy.State.UNTRUSTED,"wrong signer");
  check(EnvironmentPolicy.assess(35,true,true,true,false,true)==EnvironmentPolicy.State.PERMISSION_REQUIRED,"manage permission");
  check(EnvironmentPolicy.assess(35,true,true,true,true,false)==EnvironmentPolicy.State.PERMISSION_REQUIRED,"custom permission");
  check(EnvironmentPolicy.assess(35,true,true,true,true,true)==EnvironmentPolicy.State.READY,"provisioned");
  for(EnvironmentPolicy.State state:EnvironmentPolicy.State.values())
   check(EnvironmentPolicy.mayOpenControls(state)==(state==EnvironmentPolicy.State.READY),"controls gate "+state);
  check(!EnvironmentPolicy.recommendStart(-1),"unknown memory");
  check(!EnvironmentPolicy.recommendStart(1023),"memory floor");
  check(EnvironmentPolicy.recommendStart(1024),"memory boundary");
  check(EnvironmentPolicy.trustedSigner("6ce250ce670f5548e436b75660f3c0a02daa574e1d69f6d9f38c6a2ddaf0fedf"),"verified publisher signer");
  check(!EnvironmentPolicy.trustedSigner(null),"missing signer");
  check(!EnvironmentPolicy.trustedSigner("0000000000000000000000000000000000000000000000000000000000000000"),"untrusted signer");
  System.out.println("PASS: actual Java VM launch policy");
 }
}
"""
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            probe = root / "EnvironmentPolicyProbe.java"
            probe.write_text(harness, encoding="utf-8")
            policy = REPO / "android/app/src/main/java/dev/forgeos/ai/EnvironmentPolicy.java"
            compiled = subprocess.run(["javac", "--release", "17", "-d", str(root), str(policy), str(probe)],
                                      capture_output=True, text=True, timeout=30)
            self.assertEqual(compiled.returncode, 0, compiled.stderr)
            result = subprocess.run(["java", "-cp", str(root), "dev.forgeos.ai.EnvironmentPolicyProbe"],
                                    capture_output=True, text=True, timeout=15)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertIn("PASS: actual Java VM launch policy", result.stdout)


if __name__ == "__main__":
    unittest.main()
