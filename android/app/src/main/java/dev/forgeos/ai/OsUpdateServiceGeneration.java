package dev.forgeos.ai;

/** A delayed service completion cannot retire a newer accepted operation. */
final class OsUpdateServiceGeneration {
 private String job;private int startId,latestStart;
 synchronized void noteStart(int start){latestStart=start;}
 synchronized int latestStartId(){return latestStart;}
 synchronized void accept(String id,int start){job=id;startId=start;}
 synchronized boolean owns(String id,int start){return id!=null&&id.equals(job)&&start==startId;}
 synchronized boolean retire(String id,int start){if(!owns(id,start))return false;job=null;return true;}
}
