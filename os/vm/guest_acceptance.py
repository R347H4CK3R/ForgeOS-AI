"""Run INSIDE the Linux guest. Tests only a new temporary project; no host access."""
import json
from pathlib import Path
import platform
import subprocess
import sys
import tempfile


def run(*args, cwd=None):
    return subprocess.run(args, cwd=cwd, capture_output=True, text=True,
                          check=True, timeout=30).stdout.strip()


def main():
    report = {"kernel": platform.release(), "arch": platform.machine(),
              "python": sys.version.split()[0], "git": run("git", "--version")}
    ssh = subprocess.run(["ssh", "-V"], capture_output=True, text=True,
                         check=True, timeout=10)
    report["ssh_client"] = (ssh.stdout + ssh.stderr).strip()
    with tempfile.TemporaryDirectory(prefix="forgeos-check-") as folder:
        root = Path(folder)
        (root / "calc.py").write_text("def add(a, b):\n    return a + b\n")
        (root / "test_calc.py").write_text(
            "import unittest\nfrom calc import add\n"
            "class TestCalc(unittest.TestCase):\n"
            "    def test_add(self): self.assertEqual(add(19, 23), 42)\n")
        run(sys.executable, "-m", "unittest", "discover", "-v", cwd=root)
        run("git", "init", "-q", cwd=root)
        run("git", "add", "calc.py", "test_calc.py", cwd=root)
        run("git", "-c", "user.name=ForgeOS Verification", "-c",
            "user.email=verification@example.invalid", "commit", "-qm", "Verify project", cwd=root)
        assert run("git", "rev-list", "--count", "HEAD", cwd=root) == "1"
        assert not run("git", "status", "--porcelain", "--untracked-files=no", cwd=root)
        report.update(python_test=True, git_init_add_commit=True, tracked_tree_clean=True)
    report["remote_ssh_verified"] = False
    print("FORGEOS_GUEST_ACCEPTANCE " + json.dumps(report, sort_keys=True))


if __name__ == "__main__":
    main()
