package dev.forgeos.ai;
final class LocalEngine {
 static { System.loadLibrary("forge_ai"); }
 static native byte[] generate(int fd,byte[] prompt) throws java.io.IOException;
 static native void cancel();
 private LocalEngine() {}
}
