import unittest
from image_tools import validate_security_patch
class PatchPolicyTests(unittest.TestCase):
 def test_older_than_stock_is_rejected(self):
  with self.assertRaisesRegex(ValueError,"older"):validate_security_patch("2025-03-05","2026-04-05")
 def test_equal_and_newer_actual_dates_pass(self):
  self.assertEqual(validate_security_patch("2026-04-05","2026-04-05"),"2026-04-05")
  self.assertEqual(validate_security_patch("2026-09-05","2026-04-05"),"2026-09-05")
 def test_unknown_or_invalid_dates_fail_closed(self):
  for value in (None,"","2026-02-30","2026-4-5","2026-13-01"):
   with self.subTest(value=value):
    with self.assertRaises(ValueError):validate_security_patch(value,"2026-04-05")
 def test_invalid_floor_is_not_ignored(self):
  with self.assertRaises(ValueError):validate_security_patch("2026-09-05","bad")
