"""Prepare an offline vendor file-contexts candidate. Never modifies a device."""
import argparse,hashlib,json,re
from pathlib import Path
from image_tools import require_regular

def prepare(source,output):
 source=require_regular(source);output=Path(output)
 if output.exists() or output.is_symlink():raise ValueError('Output already exists')
 if source.stat().st_size>4*1024*1024:raise ValueError('Input too large')
 data=source.read_bytes()
 mappings=re.findall(rb'(?m)^/dev/block/mmcblk1p1[ \t]+[^\r\n]+',data)
 if len(mappings)!=1:raise ValueError('Expected exactly one SD partition mapping')
 old=b'u:object_r:mmcblk1p1_block_device:s0';new=b'u:object_r:sdcard_block_device:s0'
 line=mappings[0]
 if line.split()[1]!=old:raise ValueError('Unexpected SD partition label')
 start=data.index(line);fixed=data[:start]+line.replace(old,new,1)+data[start+len(line):]
 with output.open('xb') as stream:stream.write(fixed)
 return {'input_sha256':hashlib.sha256(data).hexdigest(),'output_sha256':hashlib.sha256(fixed).hexdigest(),'only_mapping_changed':'/dev/block/mmcblk1p1','from_type':'mmcblk1p1_block_device','to_type':'sdcard_block_device','device_modified':False,'compiled_policy_verified':False,'boot_tested':False}

if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--input',required=True);p.add_argument('--output',required=True);a=p.parse_args()
 print(json.dumps(prepare(a.input,a.output),indent=2))
