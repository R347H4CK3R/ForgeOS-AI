"""Opt-in actual Ollama/tool-loop test, executed only on the isolated CI worker."""
import hashlib,json,os,tempfile,unittest
from pathlib import Path
from agent import Agent
from server import ollama_turn
@unittest.skipUnless(os.environ.get("FORGEOS_LIVE_MODEL"),"live model is not configured")
class LiveAgentTests(unittest.TestCase):
 def test_real_model_reads_proposes_and_applies_only_after_confirmation(self):
  model=os.environ["FORGEOS_LIVE_MODEL"];models=Path(os.environ["OLLAMA_MODELS"])
  manifest=json.loads((models/"manifests/registry.ollama.ai/library/qwen2.5-coder/0.5b").read_text())
  expected=["20693aeb02c63304e263a72453b6ab89e1c700a87c6948cac523ac1e6f7cade0","66b9ea09bd5b7099cbb4fc820f31b575c0366fa439b08245566692c6784e281e","1e65450c30670713aa47fe23e8b9662bdf4065e81cc8e3cbfaa98924fcc0d320","832dd9e00a68dd83b3c3fb9f5588dad7dcf337a0db50f7d9483f310cd292e92e"]
  self.assertEqual([layer["digest"].removeprefix("sha256:") for layer in manifest["layers"]],expected)
  for digest in expected:
   check=hashlib.sha256()
   with (models/"blobs"/("sha256-"+digest)).open("rb") as stream:
    for chunk in iter(lambda:stream.read(1048576),b""):check.update(chunk)
   self.assertEqual(check.hexdigest(),digest)
  with tempfile.TemporaryDirectory() as root:
   project=Path(root)/"demo";project.mkdir();source=project/"main.py";source.write_text("print(1)\n")
   agent=Agent(root,ollama_turn)
   plan=agent.plan("demo","Read main.py, then change its complete contents to print(2) followed by a newline. Use read_file, propose_edit, then finish. Do not change any other file.",model)
   self.assertTrue(plan["approval_required"],json.dumps(plan))
   self.assertEqual(source.read_text(),"print(1)\n")
   self.assertEqual([edit["path"] for edit in plan["edits"]],["main.py"])
   self.assertEqual(plan["edits"][0]["content"].strip(),"print(2)")
   result=agent.approve(plan["plan_id"],plan["plan_sha256"],True)
   self.assertTrue(result["ok"]);self.assertEqual(source.read_text().strip(),"print(2)")
   evidence={"model":model,"model_sha256":expected[0],"plan":plan,"result":result}
   Path(os.environ.get("FORGEOS_EVIDENCE_FILE","agent-evidence.json")).write_text(json.dumps(evidence,indent=2))
if __name__=="__main__":unittest.main()
