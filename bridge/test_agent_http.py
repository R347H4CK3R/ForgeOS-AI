import json,tempfile,threading,unittest
from pathlib import Path
from http.server import ThreadingHTTPServer
from urllib.request import Request,urlopen
from urllib.error import HTTPError
from server import Workspace,handler
class AgentHTTPTests(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory();self.project=Path(self.tmp.name)/"demo";self.project.mkdir();(self.project/"main.py").write_text("print(1)")
  replies=iter([{"tool":"read_file","args":{"path":"main.py"}},{"tool":"propose_edit","args":{"path":"main.py","content":"print(2)"}},{"tool":"finish","args":{"summary":"ready"}}])
  self.token="fixture-only-session-secret-32-characters"
  self.server=ThreadingHTTPServer(("127.0.0.1",0),handler(Workspace(self.tmp.name),self.token,infer=lambda *args:next(replies)))
  self.thread=threading.Thread(target=self.server.serve_forever,daemon=True);self.thread.start()
 def tearDown(self):self.server.shutdown();self.server.server_close();self.thread.join();self.tmp.cleanup()
 def post(self,path,body,token=None):
  r=Request("http://127.0.0.1:%d%s"%(self.server.server_port,path),data=json.dumps(body).encode(),headers={"Authorization":"Bearer "+(token or self.token),"Content-Type":"application/json"})
  with urlopen(r,timeout=3) as response:return json.load(response)
 def plan(self):return self.post("/agent/plan",{"project":"demo","task":"fix it","model":"fixture"})
 def test_http_plan_then_exact_confirmed_apply(self):
  plan=self.plan();self.assertEqual((self.project/"main.py").read_text(),"print(1)")
  result=self.post("/agent/approve",{"plan_id":plan["plan_id"],"plan_sha256":plan["plan_sha256"],"confirmed":True})
  self.assertTrue(result["ok"]);self.assertEqual((self.project/"main.py").read_text(),"print(2)")
 def test_http_rejects_missing_confirmation(self):
  plan=self.plan()
  with self.assertRaises(HTTPError) as error:self.post("/agent/approve",{"plan_id":plan["plan_id"],"plan_sha256":plan["plan_sha256"]})
  self.assertEqual(error.exception.code,403);self.assertEqual((self.project/"main.py").read_text(),"print(1)")
 def test_http_rejects_bad_auth_before_apply(self):
  plan=self.plan()
  with self.assertRaises(HTTPError) as error:self.post("/agent/approve",{"plan_id":plan["plan_id"],"plan_sha256":plan["plan_sha256"],"confirmed":True},"wrong")
  self.assertEqual(error.exception.code,401);self.assertEqual((self.project/"main.py").read_text(),"print(1)")
if __name__=="__main__":unittest.main()
