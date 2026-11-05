package dev.forgeos.ai;
import org.junit.Test;
import static org.junit.Assert.*;

public final class OsUpdatePublicationTest {
 private static final class FakeStore implements OsUpdatePublication.Store {
  OsUpdatePublication.Record memory,disk;int writes;boolean rollbackSucceeds;
  FakeStore(OsUpdatePublication.Record old,boolean rollback){memory=disk=old;rollbackSucceeds=rollback;}
  public OsUpdatePublication.Record read(){return memory;}
  public boolean write(OsUpdatePublication.Record value){
   memory=value;writes++; // Android mutates memory before disk commit returns.
   if(writes==1)return false;
   if(rollbackSucceeds){disk=value;return true;}return false;
  }
 }
 @Test public void failedCommitRestoresOldPointerAndReplayFloor(){
  OsUpdatePublication.Record old=new OsUpdatePublication.Record("old","old-json","old-sig",10),next=new OsUpdatePublication.Record("new","new-json","new-sig",11);
  FakeStore store=new FakeStore(old,true);
  assertFalse(OsUpdatePublication.replace(store,next));assertTrue(store.memory==old);assertTrue(store.disk==old);assertEquals(10,store.memory.highest);assertEquals(2,store.writes);
 }
 @Test public void failedRollbackStillRestoresMemoryEvenWhenDiskIsAmbiguous(){
  OsUpdatePublication.Record old=new OsUpdatePublication.Record("old","old-json","old-sig",10),next=new OsUpdatePublication.Record("new","new-json","new-sig",11);
  FakeStore store=new FakeStore(old,false);
  assertFalse(OsUpdatePublication.replace(store,next));assertTrue(store.memory==old);assertEquals(10,store.memory.highest);assertEquals(2,store.writes);
 }
}
