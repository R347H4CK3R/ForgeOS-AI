import subprocess,sys,tempfile,unittest
from pathlib import Path
SCRIPT=Path(__file__).resolve().parents[1]/'prepare_sd_context_fix.py'
class SdContextFixTests(unittest.TestCase):
 def run_fix(self,text,existing=False):
  with tempfile.TemporaryDirectory() as d:
   source=Path(d)/'vendor_file_contexts';out=Path(d)/'fixed';source.write_bytes(text)
   if existing:out.write_bytes(b'keep')
   r=subprocess.run([sys.executable,str(SCRIPT),'--input',str(source),'--output',str(out)],capture_output=True,text=True)
   self.assertEqual(source.read_bytes(),text)
   return r,out.read_bytes() if out.exists() else None
 def test_changes_only_exact_sd_partition_label(self):
  text=b'# stock\n/dev/block/mmcblk1p1    u:object_r:mmcblk1p1_block_device:s0\n/dev/block/by-name/userdata u:object_r:userdata_block_device:s0\n'
  r,out=self.run_fix(text);self.assertEqual(r.returncode,0,r.stderr)
  self.assertEqual(out,text.replace(b'u:object_r:mmcblk1p1_block_device:s0',b'u:object_r:sdcard_block_device:s0'))
 def test_rejects_wrong_label(self):
  r,out=self.run_fix(b'/dev/block/mmcblk1p1 u:object_r:boot_block_device:s0\n');self.assertNotEqual(r.returncode,0);self.assertIsNone(out)
 def test_rejects_duplicate_mapping(self):
  r,out=self.run_fix(b'/dev/block/mmcblk1p1 u:object_r:mmcblk1p1_block_device:s0\n'*2);self.assertNotEqual(r.returncode,0);self.assertIsNone(out)
 def test_never_overwrites_existing_output(self):
  r,out=self.run_fix(b'/dev/block/mmcblk1p1 u:object_r:mmcblk1p1_block_device:s0\n',True);self.assertNotEqual(r.returncode,0);self.assertEqual(out,b'keep')
