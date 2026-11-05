import json,tempfile,threading,unittest
from pathlib import Path
from http.server import ThreadingHTTPServer
from urllib.request import Request,urlopen
from urllib.error import HTTPError
from server import Workspace,handler
class HTTPTests(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory();(Path(self.tmp.name)/"demo").mkdir()
  self.token="test-only-session-secret-32-characters"
  self.server=ThreadingHTTPServer(("127.0.0.1",0),handler(Workspace(self.tmp.name),self.token))
  self.thread=threading.Thread(target=self.server.serve_forever,daemon=True);self.thread.start()
 def tearDown(self):
  self.server.shutdown();self.server.server_close();self.thread.join();self.tmp.cleanup()
 def post(self,path,body,token=None):
  request=Request("http://127.0.0.1:%d%s"%(self.server.server_port,path),data=json.dumps(body).encode(),headers={"Authorization":"Bearer "+(token or self.token),"Content-Type":"application/json"})
  with urlopen(request,timeout=3) as response:return json.load(response)
 def test_authenticated_health(self):
  self.assertTrue(self.post("/health",{})["ok"])
  self.assertFalse(self.post("/health",{})["writes"])
 def test_bad_token_is_rejected(self):
  with self.assertRaises(HTTPError) as result:self.post("/health",{},"bad")
  self.assertEqual(result.exception.code,401)
 def test_project_listing(self):self.assertEqual(self.post("/projects",{})["projects"],["demo"])
 def test_no_arbitrary_command_endpoint(self):
  with self.assertRaises(HTTPError) as result:self.post("/exec",{"command":"whoami"})
  self.assertEqual(result.exception.code,404)
 def test_traversal_read_is_rejected(self):
  with self.assertRaises(HTTPError) as result:self.post("/read",{"path":"../secret"})
  self.assertEqual(result.exception.code,400)
if __name__=="__main__":unittest.main()
