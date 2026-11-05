package dev.forgeos.ai;
import android.content.*;
import android.os.Bundle;
public final class TerminalResultReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context,Intent intent){
  Bundle b=intent.getBundleExtra("result");if(b==null)return;
  String text="Exit code: "+b.getInt("exitCode",-999)+"\nTermux error: "+b.getInt("err",-999)+"\n"+b.getString("errmsg","")+"\n"+b.getString("stdout","")+"\n"+b.getString("stderr","");
  if(text.length()>100000)text=text.substring(0,100000)+"\n[truncated]";
  context.getSharedPreferences("terminal",0).edit().putString("result",text).apply();
 }
}
