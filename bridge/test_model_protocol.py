"""The backend must constrain tool selection before accepting model output."""
import unittest
from unittest.mock import patch
import server

class ModelProtocolTests(unittest.TestCase):
 def test_tool_turn_uses_closed_schema_and_deterministic_sampling(self):
  with patch.object(server,"model_request",return_value={"message":{"content":'{"tool":"finish","args":{"summary":"done"}}'}}) as request:
   server.ollama_turn([{"role":"user","content":"inspect project"}],"test-model",3)
  route,payload,timeout=request.call_args.args
  self.assertEqual(route,"/api/chat")
  self.assertEqual(timeout,3)
  schema=payload["format"]
  self.assertIsInstance(schema,dict)
  self.assertEqual(set(schema["properties"]["tool"]["enum"]),{"list_files","read_file","propose_edit","finish"})
  self.assertFalse(schema["additionalProperties"])
  self.assertFalse(schema["properties"]["args"]["additionalProperties"])
  self.assertEqual(set(schema["required"]),{"tool","args"})
  self.assertEqual(payload["options"]["temperature"],0)
  self.assertEqual(payload["options"]["seed"],42)

if __name__=="__main__":unittest.main()
