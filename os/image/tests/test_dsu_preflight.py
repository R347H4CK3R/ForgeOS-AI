import json
from pathlib import Path
import subprocess
import sys
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / 'dsu_preflight.py'

class DsuPreflightTests(unittest.TestCase):
    def invoke(self, *extra):
        return subprocess.run([sys.executable, str(SCRIPT), '--available-bytes', '6856118272', '--system-bytes', '2955452416', '--transport-bytes', '1436550186', *extra], capture_output=True, text=True)

    def test_actual_tablet_fits_streaming_but_not_staging(self):
        result = self.invoke()
        self.assertEqual(result.returncode, 0, result.stderr)
        plan = json.loads(result.stdout)
        self.assertTrue(plan['streaming']['fits'])
        self.assertFalse(plan['staged']['fits'])
        self.assertEqual(plan['userdata_bytes'], 2147483648)
        self.assertGreaterEqual(plan['streaming']['free_after_allocation_bytes'], 1073741824)
        self.assertFalse(plan['boot_ready'])

    def test_old_eight_gib_userdata_does_not_fit(self):
        result = self.invoke('--userdata-bytes', '8589934592')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertFalse(json.loads(result.stdout)['streaming']['fits'])

    def test_reserve_cannot_be_removed_to_force_a_fit(self):
        result = self.invoke('--available-bytes', '5102936064')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertFalse(json.loads(result.stdout)['streaming']['fits'])

    def test_small_guest_is_rejected(self):
        result = self.invoke('--userdata-bytes', '536870912')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('at least 2 GiB', result.stderr)

    def test_negative_capacity_is_rejected(self):
        result = self.invoke('--available-bytes', '-1')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('nonnegative', result.stderr)

    def test_staged_transport_is_counted_in_full(self):
        result = self.invoke('--available-bytes', '20000000000')
        self.assertEqual(result.returncode, 0, result.stderr)
        plan = json.loads(result.stdout)
        self.assertEqual(plan['staged']['required_bytes'] - plan['streaming']['required_bytes'], 1436550186)

if __name__ == '__main__':
    unittest.main()
