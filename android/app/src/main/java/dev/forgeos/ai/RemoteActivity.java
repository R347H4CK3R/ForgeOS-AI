package dev.forgeos.ai;
import android.app.*;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;

/** Explicit, authenticated requests to an opt-in host. Tokens live only in memory. */
public final class RemoteActivity extends Activity {
 private EditText endpoint,token,model,prompt,project;
 private TextView output;
 private LinearLayout actions;
 private final java.util.concurrent.ExecutorService worker=java.util.concurrent.Executors.newSingleThreadExecutor();
 private boolean busy;
 private EditText field(LinearLayout box,String hint,boolean secret) {
  EditText e=new EditText(this);e.setHint(hint);e.setSingleLine(true);
  if(secret){e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);e.setSaveEnabled(false);}
  box.addView(e);return e;
 }
 private void button(String label,Runnable run){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setOnClickListener(v->{if(!busy)run.run();});actions.addView(b);}
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  ScrollView scroll=new ScrollView(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(24,36,24,24);box.setFitsSystemWindows(true);scroll.addView(box);
  TextView title=new TextView(this);title.setText("ForgeOS · Remote AI & development");title.setTextSize(24);box.addView(title);
  TextView info=new TextView(this);info.setText("Runs on your configured host, not this tablet. HTTPS and a session token are required. No token is stored. Requests send only the fields you enter.");box.addView(info);
  endpoint=field(box,"https://gaminglaptop.your-tailnet.ts.net",false);endpoint.setText(getPreferences(0).getString("endpoint",""));
  token=field(box,"Session bridge token",true);
  model=field(box,"Installed host model name",false);
  project=field(box,"Project folder name for Git status",false);
  prompt=field(box,"Message / code to analyze",false);prompt.setSingleLine(false);prompt.setMinLines(4);
  actions=new LinearLayout(this);actions.setOrientation(LinearLayout.VERTICAL);box.addView(actions);
  button("Check authenticated bridge",()->request("/health",new JSONObject()));
  button("List remote projects",()->request("/projects",new JSONObject()));
  button("Read Git status",()->{JSONObject b=new JSONObject();try{b.put("project",project.getText().toString());request("/git-status",b);}catch(Exception e){show(e.toString());}});
  button("Send to remote AI",()->{JSONObject b=new JSONObject();try{b.put("prompt",prompt.getText().toString());b.put("model",model.getText().toString());request("/inference",b);}catch(Exception e){show(e.toString());}});
  button("Save response privately",()->{try{byte[] bytes=output.getText().toString().getBytes(StandardCharsets.UTF_8);if(!WorkspacePolicy.editableSize(bytes.length))throw new IOException("History exceeds 256 KiB");try(FileOutputStream out=openFileOutput("remote-history.txt",MODE_PRIVATE)){out.write(bytes);}Toast.makeText(this,"Response saved in app-private history",Toast.LENGTH_SHORT).show();}catch(Exception e){show(e.toString());}});
  button("Load saved response",()->{try(FileInputStream in=openFileInput("remote-history.txt")){ByteArrayOutputStream buffer=new ByteArrayOutputStream();byte[] chunk=new byte[8192];int count;while((count=in.read(chunk))!=-1){if(!WorkspacePolicy.editableSize((long)buffer.size()+count))throw new IOException("History too large");buffer.write(chunk,0,count);}byte[] bytes=buffer.toByteArray();if(!WorkspacePolicy.editableSize(bytes.length))throw new IOException("History too large");show(new String(bytes,StandardCharsets.UTF_8));}catch(Exception e){show("No readable saved response: "+e.getClass().getSimpleName());}});
  output=new TextView(this);output.setTextIsSelectable(true);output.setText("Not connected");box.addView(output);setContentView(scroll);
 }
 private void show(String value){output.setText(value);}
 private void request(String route,JSONObject data){
  try {
   URI base=new URI(endpoint.getText().toString().trim());
   if(!"https".equals(base.getScheme())||base.getHost()==null||base.getUserInfo()!=null||base.getQuery()!=null||base.getFragment()!=null||!(base.getPath().isEmpty()||"/".equals(base.getPath())))throw new IOException("Use an HTTPS host URL without credentials, query or path");
   String secret=token.getText().toString();if(secret.length()<32)throw new IOException("Enter a session token of at least 32 characters");
   String url=base.resolve(route).toString();byte[] bytes=data.toString().getBytes(StandardCharsets.UTF_8);
   if(bytes.length>65536)throw new IOException("Request too large");
   getPreferences(0).edit().putString("endpoint",base.toString()).apply();
   busy=true;for(int i=0;i<actions.getChildCount();i++)actions.getChildAt(i).setEnabled(false);show("Running on remote host…");
   worker.execute(()->{
    String result;HttpsURLConnection conn=null;
    try {
     conn=(HttpsURLConnection)new URL(url).openConnection();conn.setInstanceFollowRedirects(false);conn.setConnectTimeout(10000);conn.setReadTimeout(130000);conn.setRequestMethod("POST");conn.setDoOutput(true);conn.setRequestProperty("Authorization","Bearer "+secret);conn.setRequestProperty("Content-Type","application/json");conn.setFixedLengthStreamingMode(bytes.length);
     try(OutputStream out=conn.getOutputStream()){out.write(bytes);}
     int code=conn.getResponseCode();if(code!=200)throw new IOException("Bridge returned HTTP "+code);
     try(InputStream in=conn.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
      byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(!WorkspacePolicy.editableSize((long)out.size()+n))throw new IOException("Response too large");out.write(buffer,0,n);}
      JSONObject response=new JSONObject(out.toString("UTF-8"));result=response.has("response")?response.getString("response"):response.toString(2);
     }
    }catch(Exception e){result="Connection failed: "+e.getClass().getSimpleName()+" · "+e.getMessage();}
    finally{if(conn!=null)conn.disconnect();}
    final String value=result;runOnUiThread(()->{if(isDestroyed())return;busy=false;for(int i=0;i<actions.getChildCount();i++)actions.getChildAt(i).setEnabled(true);show(value);});
   });
  }catch(Exception e){show(e.getMessage());}
 }
 @Override protected void onDestroy(){worker.shutdownNow();token.setText("");super.onDestroy();}
}
