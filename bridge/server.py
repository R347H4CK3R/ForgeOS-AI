"""ForgeOS opt-in bridge. Bind loopback; expose only behind authenticated Tailscale HTTPS.
No shell endpoint. Tokens supplied at runtime, never committed.
"""
import hmac, json, os, subprocess, threading
from agent import Agent
from pathlib import Path, PureWindowsPath
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.request import Request, urlopen

LIMIT=262144
class Workspace:
 def __init__(self,root): self.root=Path(root).resolve(strict=True)
 def resolve(self,name):
  if not isinstance(name,str) or not name or "\\" in name or PureWindowsPath(name).is_absolute():
   raise ValueError("invalid workspace path")
  path=(self.root/name).resolve()
  if path==self.root or not path.is_relative_to(self.root): raise ValueError("path outside workspace")
  return path
 def read(self,name):
  path=self.resolve(name)
  with path.open("rb") as f: data=f.read(LIMIT+1)
  if len(data)>LIMIT: raise ValueError("file exceeds 256 KiB")
  return data.decode("utf-8")
 def projects(self): return sorted(p.name for p in self.root.iterdir() if p.is_dir() and not p.is_symlink())[:500]
MODEL_GATE=threading.Lock()
def model_request(route,payload,timeout):
 if not MODEL_GATE.acquire(blocking=False):raise RuntimeError("host inference is busy")
 try:
  request=Request("http://127.0.0.1:11434"+route,data=json.dumps(payload).encode(),headers={"Content-Type":"application/json"})
  with urlopen(request,timeout=timeout) as response:raw=response.read(LIMIT+1)
  if len(raw)>LIMIT:raise ValueError("inference response too large")
  return json.loads(raw)
 finally:MODEL_GATE.release()
TOOL_SCHEMA={"type":"object","properties":{"tool":{"type":"string","enum":["list_files","read_file","propose_edit","finish"]},"args":{"type":"object","properties":{"path":{"type":"string"},"content":{"type":"string"},"summary":{"type":"string"}},"additionalProperties":False}},"required":["tool","args"],"additionalProperties":False}
def ollama_turn(messages,model,timeout):
 result=model_request("/api/chat",{"model":model,"messages":messages,"format":TOOL_SCHEMA,"stream":False,"keep_alive":0,"options":{"num_predict":1536,"num_ctx":4096,"num_thread":2,"temperature":0,"seed":42}},timeout)
 return result["message"]["content"]
def handler(workspace,token,infer=ollama_turn):
 planner=Agent(workspace.root,infer)
 class Handler(BaseHTTPRequestHandler):
  def log_message(self,*args): pass  # never log prompts, tokens, filenames or content
  def reply(self,status,data):
   raw=json.dumps(data).encode()
   self.send_response(status);self.send_header("Content-Type","application/json")
   self.send_header("Content-Length",str(len(raw)));self.send_header("Cache-Control","no-store")
   self.end_headers();self.wfile.write(raw)
  def do_POST(self):
   if not hmac.compare_digest(self.headers.get("Authorization",""),"Bearer "+token):
    self.reply(401,{"error":"unauthorized"});return
   try:
    size=int(self.headers.get("Content-Length","0"))
    if not 0<size<=LIMIT: raise ValueError("invalid body length")
    self.connection.settimeout(15)
    body=json.loads(self.rfile.read(size))
    if self.path=="/health": result={"ok":True,"capabilities":["projects","read","git-status","inference","agent-plan","agent-approve"],"writes":"confirmation_required","arbitraryShell":False}
    elif self.path=="/projects": result={"projects":workspace.projects()}
    elif self.path=="/read": result={"text":workspace.read(body["path"])}
    elif self.path=="/git-status":
     folder=workspace.resolve(body["project"])
     if not folder.is_dir(): raise ValueError("not a project directory")
     proc=subprocess.run(["git","-c","core.fsmonitor=false","--no-optional-locks","status","--short"],cwd=folder,capture_output=True,text=True,timeout=15,shell=False)
     result={"exitCode":proc.returncode,"output":(proc.stdout+proc.stderr)[:32768]}
    elif self.path=="/agent/plan":result=planner.plan(body["project"],body["task"],body["model"])
    elif self.path=="/agent/approve":result=planner.approve(body["plan_id"],body["plan_sha256"],body.get("confirmed",False))
    elif self.path=="/inference":
     prompt=body.get("prompt","");model=body.get("model","")
     if not isinstance(prompt,str) or not 0<len(prompt)<=16384: raise ValueError("prompt length must be 1..16384")
     if not isinstance(model,str) or not 0<len(model)<=120: raise ValueError("model required")
     response=model_request("/api/generate",{"model":model,"prompt":prompt,"stream":False,"keep_alive":0,"options":{"num_predict":512,"num_ctx":2048,"num_thread":2}},120)
     result={"response":response["response"],"execution":"GamingLaptop"}
    else: self.reply(404,{"error":"unknown action"});return
    self.reply(200,result)
   except PermissionError:self.reply(403,{"error":"explicit review confirmation required"})
   except (ValueError,KeyError,UnicodeError): self.reply(400,{"error":"invalid request or workspace path"})
   except Exception: self.reply(502,{"error":"operation failed; check local service and workspace permissions"})
 return Handler
def main():
 token=os.environ.get("FORGEOS_BRIDGE_TOKEN","")
 if len(token)<32: raise SystemExit("Set FORGEOS_BRIDGE_TOKEN to a random session secret of at least 32 characters")
 root=os.environ.get("FORGEOS_WORKSPACE","")
 if not root: raise SystemExit("Set FORGEOS_WORKSPACE to an existing dedicated project folder")
 service=ThreadingHTTPServer(("127.0.0.1",8765),handler(Workspace(root),token))
 service.daemon_threads=True
 print("ForgeOS bridge listening on loopback:8765; requires Tailscale HTTPS to reach from tablet")
 try: service.serve_forever()
 finally: service.server_close()
if __name__=="__main__": main()
