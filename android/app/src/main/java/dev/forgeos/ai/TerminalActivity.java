package dev.forgeos.ai;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.widget.*;
/** Supported opt-in Termux integration. Every submitted command requires confirmation. */
public final class TerminalActivity extends Activity implements android.content.SharedPreferences.OnSharedPreferenceChangeListener {
 private TextView result;
 private EditText command;
 @Override public void onCreate(Bundle state){
  super.onCreate(state);ScrollView scroll=new ScrollView(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(24,36,24,24);box.setFitsSystemWindows(true);scroll.addView(box);
  TextView title=new TextView(this);title.setText("ForgeOS · Terminal");title.setTextSize(24);box.addView(title);
  TextView note=new TextView(this);note.setText("Commands execute in Termux's home as the Termux user. They can modify its accessible files. Grant Run commands permission and enable allow-external-apps=true in Termux properties first. Python, Git and SSH must be installed in Termux. SAF folders are not automatically shell paths.");box.addView(note);
  command=new EditText(this);command.setHint("Command to review and execute");command.setMinLines(3);command.setText("python --version; git --version; ssh -V");box.addView(command);
  Button run=new Button(this);run.setText("Review command");run.setOnClickListener(v->review());box.addView(run);
  Button open=new Button(this);open.setText("Open interactive Termux");open.setOnClickListener(v->{Intent i=getPackageManager().getLaunchIntentForPackage("com.termux");if(i!=null)startActivity(i);else result.setText("Termux is not installed.");});box.addView(open);
  result=new TextView(this);result.setTextIsSelectable(true);box.addView(result);setContentView(scroll);refresh();
 }
 private void review(){
  if(checkSelfPermission("com.termux.permission.RUN_COMMAND")!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{"com.termux.permission.RUN_COMMAND"},30);result.setText("Grant Termux command permission, then review again.");return;}
  final String script=command.getText().toString();if(script.trim().isEmpty()||script.length()>8192){result.setText("Use a command of 1..8192 characters.");return;}
  new AlertDialog.Builder(this).setTitle("Execute this exact command?").setMessage("Environment: Termux home\n\n"+script+"\n\nOnly approve commands you trust.").setNegativeButton("Cancel",null).setPositiveButton("Execute",(d,w)->execute(script)).show();
 }
 private void execute(String script){
  try{
   int id=(int)(System.nanoTime()&0x7fffffff);
   Intent callback=new Intent(this,TerminalResultReceiver.class).setAction("dev.forgeos.ai.TERMUX_RESULT."+id);
   int flags=PendingIntent.FLAG_ONE_SHOT;if(Build.VERSION.SDK_INT>=31)flags|=PendingIntent.FLAG_MUTABLE;
   PendingIntent pending=PendingIntent.getBroadcast(this,id,callback,flags);
   Intent i=new Intent("com.termux.RUN_COMMAND").setClassName("com.termux","com.termux.app.RunCommandService");
   i.putExtra("com.termux.RUN_COMMAND_PATH","/data/data/com.termux/files/usr/bin/bash");
   i.putExtra("com.termux.RUN_COMMAND_ARGUMENTS",new String[]{"-lc",script});
   i.putExtra("com.termux.RUN_COMMAND_WORKDIR","/data/data/com.termux/files/home");
   i.putExtra("com.termux.RUN_COMMAND_BACKGROUND",true);
   i.putExtra("com.termux.RUN_COMMAND_PENDING_INTENT",pending);
   i.putExtra("com.termux.RUN_COMMAND_COMMAND_LABEL","ForgeOS approved command");
   startService(i);result.setText("Submitted to Termux. Waiting for result. If none arrives, check Termux configuration and Android background restrictions.");
  }catch(Exception e){result.setText("Could not start Termux command: "+e.getMessage());}
 }
 private void refresh(){result.setText(getSharedPreferences("terminal",0).getString("result","No command result received."));}
 @Override public void onSharedPreferenceChanged(android.content.SharedPreferences p,String key){runOnUiThread(()->{if(!isDestroyed())refresh();});}
 @Override protected void onResume(){super.onResume();getSharedPreferences("terminal",0).registerOnSharedPreferenceChangeListener(this);refresh();}
 @Override protected void onPause(){getSharedPreferences("terminal",0).unregisterOnSharedPreferenceChangeListener(this);super.onPause();}
}
