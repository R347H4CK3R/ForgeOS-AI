package dev.forgeos.ai;
public final class WorkspacePolicy {
 private WorkspacePolicy() {}
 public static final int MAX_TEXT_BYTES=262144;
 public static final String[] DIRECTORIES={"models","projects","source","datasets","downloads","logs","artifacts","backups"};
 public static boolean validName(String name) {
  return name!=null&&!name.isEmpty()&&name.equals(name.trim())&&!name.equals(".")&&!name.equals("..")&&name.length()<=120&&!name.matches(".*[\\\\/\\p{Cntrl}].*");
 }
 public static boolean editableSize(long bytes) { return bytes>=0&&bytes<=MAX_TEXT_BYTES; }
}
