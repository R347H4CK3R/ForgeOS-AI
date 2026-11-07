package dev.forgeos.ai;
import org.junit.Test;
import static org.junit.Assert.*;
public class WorkspacePolicyTest {
 @Test public void rejectsTraversalAndReservedNames() {
  for(String s:new String[]{"","..",".","../escape","a/b","a\\b"," a","a ","x\n"}) assertFalse(s,WorkspacePolicy.validName(s));
  assertTrue(WorkspacePolicy.validName("My project-1.py"));
 }
 @Test public void textLimitIsMeasuredInBytes() {
  assertTrue(WorkspacePolicy.editableSize(262144));
  assertFalse(WorkspacePolicy.editableSize(262145));
  assertFalse(WorkspacePolicy.editableSize(-1));
 }
 @Test public void requiredWorkspaceLayout() {
  assertArrayEquals(new String[]{"models","projects","source","datasets","downloads","logs","artifacts","backups"},WorkspacePolicy.DIRECTORIES);
 }
}
