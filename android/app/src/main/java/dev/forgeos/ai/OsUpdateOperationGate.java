package dev.forgeos.ai;

import java.util.UUID;

/** Process-wide operation ownership: a stale callback cannot release a newer job. */
final class OsUpdateOperationGate {
 private String owner;
 synchronized String acquire(){if(owner!=null)return null;owner=UUID.randomUUID().toString();return owner;}
 synchronized boolean owns(String id){return id!=null&&id.equals(owner);}
 synchronized boolean release(String id){if(!owns(id))return false;owner=null;return true;}
}
