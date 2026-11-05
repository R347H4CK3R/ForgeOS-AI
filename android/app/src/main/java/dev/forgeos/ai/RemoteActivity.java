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

/** Explicit authenticated host requests; write proposals require a second review. */
public final class RemoteActivity extends Activity {
 private EditText endpoint,token,model,prompt,project,task;
 private TextView output;
 private LinearLayout actions;
 private JSONObject lastPlan;
 private String planConnection="";
 private volatile HttpsURLConnection active;
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
  TextView title=new TextView(this);title.setText("ForgeOS Â· Remote AI & development");title.setTextSize(24);box.addView(title);
  TextView info=new TextView(this);info.setText("Runs on your configured host, not this tablet. HTTPS and a session token are required. No token is stored. The agent reads only the chosen project, proposes bounded edits and checks Python syntax without executing generated code. Applying edits requires a separate review.");box.addView(info);
  endpoint=field(box,"https://gaminglaptop.your-tailnet.ts.net",false);endpoint.setText(getPreferences(0).getString("endpoint",""));
  token=field(box,"Session bridge token",true);
  model=field(box,"Installed host model name",false);
  project=field(box,"Project folder name",false);
  task=field(box,"Project task to diagnose / repair",false);task.setSingleLine(false);task.setMinLines(2);task.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4096)});
  prompt=field(box,"Message / code for a direct AI response",false);prompt.setSingleLine(false);prompt.setMinLines(3);
  actions=new LinearLayout(this);actions.setOrientation(LinearLayout.VERTICAL);box.addView(actions);
  button("Check authenticated bridge",()->request("/health",new JSONObject()));
  button("List remote projects",()->request("/projects",new JSONObject()));
  button("Read Git status",()->sendProject("/git-status",false));
  button("Plan / diagnose project task (no writes)",()->sendProject("/agent/plan",true));
  button("Review and apply last edit plan",this::reviewPlan);
  button("Send to remote AI",()->{JSONObject b=new JSONObject();try{b.put("prompt",prompt.getText().toString());b.put("model",model.getText().toString());request("/inference",b);}catch(Exception e){show(e.toString());}});
  button("Save response privately",()->{try{byte[] bytes=output.getText().toString().getBytes(StandardCharsets.UTF_8);if(!WorkspacePolicy.editableSize(bytes.length))throw new IOException("History exceeds256KiB");try(FileOutputStream out=openFileOutput("remote-history.txt",MODE_PRIVATE)){out.write(bytes);}Toast.makeText(this,"Response saved in app-private history",Toast.LENGTH_SHORT).show();}catch(Exception e){show(e.toString());}});
  button("Load saved response",()->{lastPlan=null;try(InputStream in=openFileInput("remote-history.txt")){show(readBody(in,WorkspacePolicy.MAX_TEXT_BYTES));}catch(Exception e){show("No readable saved response: "+e.getClass().getSimpleName());}});
  output=new TextView(this);output.setTextIsSelectable(true);output.setText("Not connected");box.addView(output);ForgeStyle.install(this,scroll);
 }
 private void sendProject(String route,boolean agent){
  try{JSONObject body=new JSONObject();body.put("project",project.getText().toString());if(agent){body.put("task",task.getText().toString());body.put("model",model.getText().toString());}request(route,body);}catch(Exception e){show(e.getMessage());}
 }
 private String connectionKey()throws Exception{
  byte[] bytes=java.security.MessageDigest.getInstance("SHA-256").digest(token.getText().toString().getBytes(StandardCharsets.UTF_8));
  StringBuilder key=new StringBuilder(endpoint.getText().toString().trim()).append('|');for(byte b:bytes){key.append(Character.forDigit((b>>>4)&15,16));key.append(Character.forDigit(b&15,16));}return key.toString();
 }
 private void reviewPlan(){
  try{
   if(lastPlan==null||lastPlan.isNull("plan_id")){show("Generate a live edit plan first. Saved text is not an approval capability.");return;}
   if(!planConnection.equals(connectionKey())){lastPlan=null;show("Connection changed. Generate a new plan before applying edits.");return;}
   JSONObject body=new JSONObject();body.put("plan_id",lastPlan.getString("plan_id"));body.put("plan_sha256",lastPlan.getString("plan_sha256"));body.put("confirmed",true);
   ScrollView scroll=new ScrollView(this);TextView review=new TextView(this);review.setPadding(20,12,20,12);review.setTextIsSelectable(true);
   review.setText("Project: "+lastPlan.getString("project")+"\nReplaces only these files and keeps project backups. Python syntax checks are not unit tests. No generated code or shell command will run.\n\n"+lastPlan.getJSONArray("edits").toString(2));scroll.addView(review);
   new AlertDialog.Builder(this).setTitle("Approve these exact file edits?").setView(scroll).setNegativeButton("Cancel",null).setPositiveButton("Apply reviewed plan",(d,w)->request("/agent/approve",body)).show();
  }catch(Exception e){show(e.getMessage());}
 }
 private void show(String value){output.setText(value);}
 private static String readBody(InputStream stream,int limit)throws IOException{
  if(stream==null)throw new IOException("Empty response");
  try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if((long)out.size()+n>limit)throw new IOException("Response exceeds the size limit");out.write(buffer,0,n);}return out.toString("UTF-8");
  }
 }
 private void request(String route,JSONObject data){
  try{
   URI base=new URI(endpoint.getText().toString().trim());
   if(!"https".equals(base.getScheme())||base.getHost()==null||base.getUserInfo()!=null||base.getQuery()!=null||base.getFragment()!=null||!(base.getPath().isEmpty()||"/".equals(base.getPath())))throw new IOException("Use an HTTPS host URL without credentials, query or path");
   String secret=token.getText().toString();if(secret.length()<32)throw new IOException("Enter a session token of at least32 characters");
   String url=base.resolve(route).toString();String key=connectionKey();byte[] bytes=data.toString().getBytes(StandardCharsets.UTF_8);
   if(bytes.length>65536)throw new IOException("Request too large");
   getPreferences(0).edit().putString("endpoint",base.toString()).apply();
   busy=true;for(int i=0;i<actions.getChildCount();i++)actions.getChildAt(i).setEnabled(false);show("Running on remote hostâ€¦");
   worker.execute(()->{
    String result;JSONObject parsed=null;HttpsURLConnection conn=null;
    try{
     if(Thread.currentThread().isInterrupted())throw new IOException("Request cancelled");
     conn=(HttpsURLConnection)new URL(url).openConnection();active=conn;conn.setInstanceFollowRedirects(false);conn.setConnectTimeout(10000);conn.setReadTimeout(130000);conn.setRequestMethod("POST");conn.setDoOutput(true);conn.setRequestProperty("Authorization","Bearer "+secret);conn.setRequestProperty("Content-Type","application/json");conn.setFixedLengthStreamingMode(bytes.length);
     try(OutputStream out=conn.getOutputStream()){out.write(bytes);}
     int code=conn.getResponseCode();
     if(code!=200){String detail="";try{detail=new JSONObject(readBody(conn.getErrorStream(),8192)).optString("error","");}catch(Exception ignored){}throw new IOException("Bridge HTTP "+code+(detail.isEmpty()?"":" Â· "+detail));}
     parsed=new JSONObject(readBody(conn.getInputStream(),WorkspacePolicy.MAX_TEXT_BYTES));
     result=parsed.has("response")?parsed.getString("response"):parsed.toString(2);
    }catch(Exception e){result="Request failed: "+e.getClass().getSimpleName()+" Â· "+e.getMessage();}
    finally{if(conn!=null)conn.disconnect();active=null;}
    final String value=result;final JSONObject response=parsed;
    runOnUiThread(()->{if(isDestroyed())return;busy=false;for(int i=0;i<actions.getChildCount();i++)actions.getChildAt(i).setEnabled(true);lastPlan=route.equals("/agent/plan")&&response!=null&&response.optBoolean("approval_required",false)?response:null;planConnection=lastPlan==null?"":key;show(value);});
   });
  }catch(Exception e){show(e.getMessage());}
 }
 @Override protected void onDestroy(){worker.shutdownNow();HttpsURLConnection conn=active;if(conn!=null)conn.disconnect();token.setText("");lastPlan=null;super.onDestroy();}
}
