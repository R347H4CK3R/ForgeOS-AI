package dev.forgeos.ai;

/** Pure policy for opening the independently signed VM companion controls. */
public final class EnvironmentPolicy {
 public enum State { UNSUPPORTED, INSTALL_REQUIRED, UNTRUSTED, PERMISSION_REQUIRED, READY }
 private static final String SIGNER="6ce250ce670f5548e436b75660f3c0a02daa574e1d69f6d9f38c6a2ddaf0fedf";
 private EnvironmentPolicy() {}
 public static State assess(int api,boolean avf,boolean installed,boolean trusted,
                            boolean manage,boolean custom) {
  if(api<34||!avf)return State.UNSUPPORTED;
  if(!installed)return State.INSTALL_REQUIRED;
  if(!trusted)return State.UNTRUSTED;
  if(!manage||!custom)return State.PERMISSION_REQUIRED;
  return State.READY;
 }
 public static boolean trustedSigner(String digest){return digest!=null&&SIGNER.equalsIgnoreCase(digest);}
 public static boolean mayOpenControls(State state){return state==State.READY;}
 // Opening controls remains available at low memory so an existing VM can be stopped.
 public static boolean recommendStart(long availableMiB){return availableMiB>=1024;}
}
