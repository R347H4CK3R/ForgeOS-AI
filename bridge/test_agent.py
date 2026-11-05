import json,tempfile,unittest
from pathlib import Path
from agent import Agent,Project,syntax
class AgentTests(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory();self.root=Path(self.tmp.name);self.project=self.root/"demo";self.project.mkdir();(self.project/"main.py").write_text("print(1)\n")
 def tearDown(self):self.tmp.cleanup()
 def agent(self,responses):
  iterator=iter(responses)
  return Agent(self.root,lambda messages,model,timeout:next(iterator))
 def plan(self,agent):return agent.plan("demo","Change the message","installed-model")
 def calls(self,content="print(2)\n"):
  return [{"tool":"read_file","args":{"path":"main.py"}},{"tool":"propose_edit","args":{"path":"main.py","content":content}},{"tool":"finish","args":{"summary":"Ready for review"}}]
 def test_plan_does_not_change_files(self):
  plan=self.plan(self.agent(self.calls()));self.assertTrue(plan["approval_required"]);self.assertEqual((self.project/"main.py").read_text(),"print(1)\n")
 def test_approved_exact_plan_applies_and_backs_up(self):
  agent=self.agent(self.calls());plan=self.plan(agent);result=agent.approve(plan["plan_id"],plan["plan_sha256"],True)
  self.assertTrue(result["ok"]);self.assertFalse(result["executed_code"]);self.assertEqual((self.project/"main.py").read_text(),"print(2)\n");self.assertEqual((self.project/result["backup"]/"main.py").read_text(),"print(1)\n")
 def test_returned_plan_cannot_mutate_pending_approved_content(self):
  agent=self.agent(self.calls());plan=self.plan(agent);plan['edits'][0]['content']='print(99)'
  agent.approve(plan['plan_id'],plan['plan_sha256'],True);self.assertEqual((self.project/'main.py').read_text(),'print(2)\n')
 def test_missing_confirmation_cannot_write(self):
  agent=self.agent(self.calls());plan=self.plan(agent)
  with self.assertRaises(PermissionError):agent.approve(plan["plan_id"],plan["plan_sha256"],False)
  self.assertEqual((self.project/"main.py").read_text(),"print(1)\n")
 def test_changed_hash_cannot_write(self):
  agent=self.agent(self.calls());plan=self.plan(agent)
  with self.assertRaises(ValueError):agent.approve(plan["plan_id"],"0"*64,True)
 def test_changed_source_cannot_be_overwritten(self):
  agent=self.agent(self.calls());plan=self.plan(agent);(self.project/"main.py").write_text("print(99)\n")
  with self.assertRaises(ValueError):agent.approve(plan["plan_id"],plan["plan_sha256"],True)
  self.assertEqual((self.project/"main.py").read_text(),"print(99)\n")
 def test_syntax_failure_drives_a_repair_before_approval(self):
  calls=[{"tool":"read_file","args":{"path":"main.py"}},{"tool":"propose_edit","args":{"path":"main.py","content":"def broken("}},{"tool":"propose_edit","args":{"path":"main.py","content":"print(3)\n"}},{"tool":"finish","args":{"summary":"Repaired syntax"}}]
  plan=self.plan(self.agent(calls));self.assertEqual(plan["repair_attempts"],1);self.assertEqual(plan["edits"][0]["check"]["status"],"passed")
 def test_model_cannot_run_a_shell_tool(self):
  plan=self.plan(self.agent([{"tool":"exec","args":{"command":"whoami"}},{"tool":"finish","args":{}}]))
  self.assertIsNone(plan["plan_id"]);self.assertIn("unsupported",str(plan["steps"]))
 def test_traversal_hidden_files_and_drive_paths_rejected(self):
  project=Project(self.project)
  for name in ("../secret",".env","folder/.git/config","/etc/passwd","C:\\secret","C:secret"):
   with self.assertRaises(ValueError,msg=name):project.path(name)
 def test_non_python_does_not_claim_a_test_pass(self):self.assertEqual(syntax("main.java","invalid java")["status"],"not_checked")
 def test_completed_plan_cannot_be_replayed(self):
  agent=self.agent(self.calls());plan=self.plan(agent);agent.approve(plan["plan_id"],plan["plan_sha256"],True)
  with self.assertRaises(ValueError):agent.approve(plan["plan_id"],plan["plan_sha256"],True)
 def test_symlink_escape_rejected(self):
  try:(self.project/"escape").symlink_to(self.root,target_is_directory=True)
  except OSError:self.skipTest("symlink not permitted")
  with self.assertRaises(ValueError):Project(self.project).path("escape/secret")


 def test_repeated_identical_tool_call_stops_without_writes(self):
  calls=[]
  def repeat(messages,model,timeout):
   calls.append(1)
   return {"tool":"read_file","args":{"path":"main.py"}}
  plan=self.plan(Agent(self.root,repeat))
  self.assertLessEqual(len(calls),2)
  self.assertFalse(plan["approval_required"])
  self.assertEqual(plan["steps"][-1]["reason"],"model repeated an identical tool call")
  self.assertEqual((self.project/"main.py").read_text(),"print(1)\n")
