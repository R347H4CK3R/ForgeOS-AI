import unittest
from instrumentation_result import validate_result

class InstrumentationResultTests(unittest.TestCase):
 def report(self,count):
  return f'INSTRUMENTATION_STATUS: numtests={count}\nOK ({count} tests)\nINSTRUMENTATION_CODE: -1\n'
 def test_current_and_expanded_suite_pass(self):
  for count in (12,13,20):self.assertEqual(validate_result(self.report(count),12),count)
 def test_empty_smaller_and_truncated_suite_fail(self):
  for report in ('',self.report(10),self.report(12).replace('INSTRUMENTATION_CODE: -1','')):
   with self.assertRaises(ValueError):validate_result(report,12)
 def test_failure_cannot_be_hidden_by_success_text(self):
  for error in ('FAILURES!!!','INSTRUMENTATION_FAILED','Process crashed','INSTRUMENTATION_STATUS_CODE: -2'):
   with self.assertRaises(ValueError):validate_result(error+'\n'+self.report(12),12)
 def test_inconsistent_discovery_fails(self):
  with self.assertRaises(ValueError):validate_result(self.report(12)+'INSTRUMENTATION_STATUS: numtests=13\n',12)

if __name__=='__main__':unittest.main()
