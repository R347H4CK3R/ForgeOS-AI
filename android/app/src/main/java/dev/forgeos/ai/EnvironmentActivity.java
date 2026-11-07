package dev.forgeos.ai;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.security.MessageDigest;

/** Owner-provisioned companion integration; never claims a VM state from package presence. */
public final class EnvironmentActivity extends Activity {
 private static final String COMPANION="com.excp.podroid";
 private static final String MANAGE="android.permission.MANAGE_VIRTUAL_MACHINE";
 private static final String CUSTOM="android.permission.USE_CUSTOM_VIRTUAL_MACHINE";
 private LinearLayout body;
 private TextView detail;
 private EnvironmentPolicy.State state;
 private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
 private void text(String value,int size){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setPadding(0,dp(8),0,dp(8));body.addView(v);}
 private Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setMinHeight(dp(48));b.setOnClickListener(v->action.run());body.addView(b);return b;}
 @Override protected void onCreate(Bundle saved){super.onCreate(saved);}
 @Override protected void onResume(){super.onResume();render();}
 @SuppressWarnings("deprecation")
 private EnvironmentPolicy.State inspect(){
  PackageManager pm=getPackageManager();boolean installed=false,trusted=false;
  try{
   int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;
   PackageInfo info=pm.getPackageInfo(COMPANION,flags);installed=true;
   Signature[] signers=Build.VERSION.SDK_INT>=28&&info.signingInfo!=null?info.signingInfo.getApkContentsSigners():info.signatures;
   if(signers!=null&&signers.length==1){
    byte[] digest=MessageDigest.getInstance("SHA-256").digest(signers[0].toByteArray());
    StringBuilder hex=new StringBuilder();for(byte b:digest)hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
    trusted=EnvironmentPolicy.trustedSigner(hex.toString());
   }
  }catch(Exception ignored){/* Missing or unreadable signing identity fails closed. */}
  return EnvironmentPolicy.assess(Build.VERSION.SDK_INT,pm.hasSystemFeature("android.software.virtualization_framework"),
    installed,trusted,pm.checkPermission(MANAGE,COMPANION)==PackageManager.PERMISSION_GRANTED,
    pm.checkPermission(CUSTOM,COMPANION)==PackageManager.PERMISSION_GRANTED);
 }
 private long availableMiB(){
  ActivityManager.MemoryInfo memory=new ActivityManager.MemoryInfo();
  ActivityManager manager=(ActivityManager)getSystemService(ACTIVITY_SERVICE);
  if(manager==null)return -1;manager.getMemoryInfo(memory);return memory.availMem/1048576;
 }
 private void render(){
  state=inspect();
  ScrollView scroll=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
  body.setPadding(dp(20),dp(24),dp(20),dp(24));body.setFitsSystemWindows(true);scroll.addView(body);
  text("OS environments",26);
  text("Use a Linux guest alongside stock Android. This is an interim two-environment setup; genuine dual boot remains unavailable.",16);
  String message;
  switch(state){
   case READY:message="Trusted VM companion is provisioned. Its controls show the current guest status.";break;
   case INSTALL_REQUIRED:message="Install the verified Podroid companion, then complete owner setup.";break;
   case UNTRUSTED:message="The installed companion has a different signer. Controls are blocked. Back up its guest data before replacing it.";break;
   case PERMISSION_REQUIRED:message="The trusted companion needs the two owner-granted VM permissions. Copy the setup commands below and run them from the laptop.";break;
   default:message="This hardware VM path requires Android 14 or newer and the Android virtualization feature.";
  }
  text(message,16);
  long available=availableMiB();
  text("Available device RAM: "+(available<0?"unknown":available+" MiB")+". Start with 1 CPU and 512 MiB guest RAM.",14);
  if(!EnvironmentPolicy.recommendStart(available))text("Memory is low. Open Linux controls to stop or reduce an existing workload before starting another.",14);
  boolean ready=EnvironmentPolicy.mayOpenControls(state);
  button("Open Linux controls",()->open(false)).setEnabled(ready);
  button("Open beside ForgeOS (request)",()->open(true)).setEnabled(ready);
  if(state==EnvironmentPolicy.State.INSTALL_REQUIRED||state==EnvironmentPolicy.State.UNTRUSTED)
   button("Get companion from publisher",()->external(new Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/ExTV/Podroid/releases/tag/v1.2.9"))));
  if(state==EnvironmentPolicy.State.PERMISSION_REQUIRED||state==EnvironmentPolicy.State.INSTALL_REQUIRED)
   button("Copy owner setup commands",()->{
    ClipboardManager clipboard=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
    if(clipboard!=null){clipboard.setPrimaryClip(ClipData.newPlainText("ForgeOS VM setup","adb shell pm grant "+COMPANION+" "+MANAGE+"\nadb shell pm grant "+COMPANION+" "+CUSTOM));detail.setText("Copied the two one-time owner setup commands.");}
   });
  button("Refresh environment checks",this::render);
  text("Guest settings: choose AVF, keep SSH off until key authentication is configured, and share only files you intend to expose. The native SD workspace remains available in ForgeOS.",14);
  detail=new TextView(this);detail.setTextIsSelectable(true);body.addView(detail);
  ForgeStyle.install(this,scroll);
 }
 private void open(boolean adjacent){
  state=inspect();
  if(!EnvironmentPolicy.mayOpenControls(state)){render();return;}
  Intent launch=getPackageManager().getLaunchIntentForPackage(COMPANION);
  if(launch==null){detail.setText("The companion cannot open. Check whether it is enabled in Android settings.");return;}
  launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  if(adjacent)launch.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
  external(launch);
 }
 private void external(Intent intent){
  try{startActivity(intent);}catch(RuntimeException failure){detail.setText("Could not open this action: "+failure.getMessage());}
 }
}
