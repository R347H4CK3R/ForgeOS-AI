import unittest
from review_release import validate_release

class ReviewReleaseTests(unittest.TestCase):
 def setUp(self):
  self.tag='forgeos-review-1-1-ForgeOS-native-review';self.sha='a'*40
  self.data={'tagName':self.tag,'targetCommitish':self.sha,'isDraft':True,'isPrerelease':True}
 def test_exact_pending_container_is_accepted(self):validate_release(self.data,self.tag,self.sha)
 def test_existing_other_source_or_published_container_is_rejected(self):
  for field,value in (('tagName','other'),('targetCommitish','b'*40),('isDraft',False),('isPrerelease',False)):
   data=dict(self.data);data[field]=value
   with self.subTest(field=field),self.assertRaises(ValueError):validate_release(data,self.tag,self.sha)

if __name__=='__main__':unittest.main()
