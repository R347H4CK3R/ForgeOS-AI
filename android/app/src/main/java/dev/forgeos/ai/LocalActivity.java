package dev.forgeos.ai;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
public final class LocalActivity extends Activity {
 private Uri model;
 private EditText prompt;
 private TextView status;
 private Button run,choose;
 private static final java.util.concurrent.ExecutorService WORKER=java.util.concurrent.Executors.newSingleThreadExecutor();
 private static final java.util.concurrent.atomic.AtomicBoolean RUNNING=new java.util.concurrent.atomic.AtomicBoolean();
 @Override public void onCreate(Bundle state){
  super.onCreate(state);ScrollView scroll=new ScrollView(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(24,36,24,24);box.setFitsSystemWindows(true);scroll.addView(box);
  TextView title=new TextView(this);title.setText("ForgeOS · Local GGUF");title.setTextSize(24);box.addView(title);
  TextView info=new TextView(this);info.setText("CPU inference: 2 threads, 512-token context, at most 128 generated tokens. Use a small quantized model, ideally 135M–360M parameters. Runtime support is not proof of successful inference on your tablet. No model is bundled.");box.addView(info);
  String stored=getPreferences(0).getString("model",null);if(stored!=null)model=Uri.parse(stored);
  choose=new Button(this);choose.setText("Select GGUF model");choose.setOnClickListener(v->{if(!RUNNING.get())startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),20);});box.addView(choose);
  prompt=new EditText(this);prompt.setHint("Ask a short question");prompt.setMinLines(4);prompt.setGravity(android.view.Gravity.TOP);box.addView(prompt);
  run=new Button(this);run.setText("Run locally");run.setOnClickListener(v->generate());box.addView(run);
  Button stop=new Button(this);stop.setText("Stop generation");stop.setOnClickListener(v->{if(RUNNING.get())LocalEngine.cancel();});box.addView(stop);
  status=new TextView(this);status.setTextIsSelectable(true);status.setText(model==null?"Select a model using Android's file picker.":"Selected: "+model);box.addView(status);setContentView(scroll);
 }
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req!=20||result!=RESULT_OK||data==null||data.getData()==null)return;
  try{Uri uri=data.getData();getContentResolver().takePersistableUriPermission(uri,data.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION);model=uri;getPreferences(0).edit().putString("model",uri.toString()).apply();status.setText("Selected: "+uri);}catch(Exception e){status.setText(e.toString());}
 }
 private void generate(){
  if(model==null){status.setText("Select a GGUF model first.");return;}
  byte[] input=prompt.getText().toString().getBytes(StandardCharsets.UTF_8);
  if(input.length<1||input.length>8192){status.setText("Prompt must be 1..8192 UTF-8 bytes.");return;}
  ActivityManager.MemoryInfo memory=new ActivityManager.MemoryInfo();((ActivityManager)getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(memory);
  if(memory.lowMemory||memory.availMem<850L*1048576){status.setText("At least 850 MiB available RAM is required. Close other apps and retry.");return;}
  if(!RUNNING.compareAndSet(false,true)){status.setText("A local generation is already running.");return;}
  try{LocalEngine.prepare();}catch(LinkageError e){RUNNING.set(false);status.setText("Native runtime unavailable: "+e.getMessage());return;}
  run.setEnabled(false);choose.setEnabled(false);status.setText("Loading model and generating locally…");
  final Uri selected=model;
  WORKER.execute(()->{
   String result;
   try{
    try(InputStream in=getContentResolver().openInputStream(selected)){if(in==null||in.read()!='G'||in.read()!='G'||in.read()!='U'||in.read()!='F')throw new IOException("Not a GGUF file");}
    try(ParcelFileDescriptor fd=getContentResolver().openFileDescriptor(selected,"r")){
     if(fd==null)throw new IOException("Model provider unavailable");
     long size=fd.getStatSize();if(size<1048576||size>550L*1048576)throw new IOException("Model must have a known size between 1 and 550 MiB and be seekable");
     result=new String(LocalEngine.generate(fd.getFd(),input),StandardCharsets.UTF_8);
     if(result.isEmpty())result="Model returned no text.";
    }
   }catch(Exception|LinkageError e){result="Local inference failed: "+e.getMessage();}
   finally{RUNNING.set(false);}
   final String value=result;runOnUiThread(()->{if(isDestroyed())return;run.setEnabled(true);choose.setEnabled(true);status.setText(value);});
  });
 }
 @Override protected void onDestroy(){if(RUNNING.get())LocalEngine.cancel();super.onDestroy();}
}
