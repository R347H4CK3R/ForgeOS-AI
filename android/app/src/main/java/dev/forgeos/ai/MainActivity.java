package dev.forgeos.ai;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Native workstation. No privileged firmware or window-management claims. */
public final class MainActivity extends Activity {
 private LinearLayout page,body;
 private EditText editor;
 private TextView documentLabel;
 private Uri document,tree;
 private String draft="",screen="Home";
 private boolean dirty=false,dark=true;
 private final java.util.concurrent.ExecutorService io=java.util.concurrent.Executors.newSingleThreadExecutor();
 private int ink,bg,panel;
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  dark=getPreferences(0).getBoolean("dark",true);
  String saved=getPreferences(0).getString("workspace",null);
  if(saved!=null)tree=Uri.parse(saved);
  if(state!=null){draft=state.getString("draft","");dirty=state.getBoolean("dirty");screen=state.getString("screen","Home");String d=state.getString("document");if(d!=null)document=Uri.parse(d);}
  render(screen);
 }
 @Override protected void onSaveInstanceState(Bundle out) {
  capture();out.putString("draft",draft);out.putBoolean("dirty",dirty);out.putString("screen",screen);
  if(document!=null)out.putString("document",document.toString());super.onSaveInstanceState(out);
 }
 private void capture(){if(editor!=null){String next=editor.getText().toString();if(!next.equals(draft))dirty=true;draft=next;}}
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
 private TextView text(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(ink);t.setPadding(dp(8),dp(6),dp(8),dp(6));return t;}
 private void button(LinearLayout target,String title,Runnable action){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setOnClickListener(v->action.run());target.addView(b);}
 private void note(String s){body.addView(text(s,16));}
 private void render(String name) {
  capture();editor=null;screen=name;
  ink=dark?Color.rgb(231,239,247):Color.rgb(20,36,50);
  bg=dark?Color.rgb(13,22,32):Color.rgb(241,245,249);
  panel=dark?Color.rgb(23,39,53):Color.WHITE;
  page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setBackgroundColor(bg);
  page.setPadding(dp(14),dp(12),dp(14),dp(8));page.setFitsSystemWindows(true);
  page.addView(text("ForgeOS AI  /  "+name,25));
  ScrollView scroll=new ScrollView(this);
  body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(8),dp(8),dp(8),dp(8));body.setBackgroundColor(panel);
  scroll.addView(body);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  HorizontalScrollView dock=new HorizontalScrollView(this);LinearLayout nav=new LinearLayout(this);
  for(String s:new String[]{"Home","Files","Code Studio","Devices","Diagnostics","Settings"})button(nav,s,()->render(s));
  dock.addView(nav);page.addView(dock);setContentView(page);
  switch(name) {
   case "Files": files();break;
   case "Code Studio": code();break;
   case "Devices": devices();break;
   case "Diagnostics": diagnostics();break;
   case "Settings": settings();break;
   default: home();
  }
 }
 private void home() {
  note("Your tablet workstation");
  note("Native Android review build · stock Android remains active");
  button(body,"Choose workspace on SD card",()->safe(()->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION),10)));
  button(body,"Browse workspace",()->render("Files"));
  button(body,"Edit source file",()->render("Code Studio"));
  button(body,"Terminal / Python / Git (Termux)",()->launch("com.termux",false));
  button(body,"Remote AI and development",()->startActivity(new Intent(this,RemoteActivity.class)));
  button(body,"Local AI / GGUF model",()->startActivity(new Intent(this,LocalActivity.class)));
  note("Local inference requires a small GGUF model and sufficient free RAM. No model or terminal is bundled. Autonomous repair is not yet implemented.");
  note("Floating windows and split screen are controlled by Android. Resizable manifest support does not verify hardware support.");
 }
 private void settings() {
  button(body,dark?"Use light theme":"Use dark theme",()->{dark=!dark;getPreferences(0).edit().putBoolean("dark",dark).apply();render(screen);});
  button(body,"Wi-Fi",()->openSettings(Settings.ACTION_WIFI_SETTINGS));
  button(body,"Bluetooth",()->openSettings(Settings.ACTION_BLUETOOTH_SETTINGS));
  button(body,"Display and rotation",()->openSettings(Settings.ACTION_DISPLAY_SETTINGS));
  button(body,"Android settings",()->openSettings(Settings.ACTION_SETTINGS));
  button(body,"Choose workspace",()->{render("Home");});
  note("Workspace selection grants only access to your chosen folder. No storage wipe, bootloader unlock or firmware change is performed.");
 }
 private void devices() {
  button(body,"Open Tailscale",()->launch("com.tailscale.ipn",false));
  button(body,"Open terminal",()->launch("com.termux",false));
  button(body,"Open terminal beside ForgeOS (request)",()->launch("com.termux",true));
  note("Tailscale login and Termux configuration are managed by those apps. Installed apps are not proof of an active connection.");
  button(body,"Configure remote AI / project bridge",()->startActivity(new Intent(this,RemoteActivity.class)));
  note("The opt-in bridge supports project listing, reads, Git status and host AI. It does not impersonate a native Desktop Commander Android agent. No credentials are included.");
 }
 private void diagnostics() {
  android.app.ActivityManager.MemoryInfo m=new android.app.ActivityManager.MemoryInfo();((android.app.ActivityManager)getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(m);
  android.hardware.display.DisplayManager d=(android.hardware.display.DisplayManager)getSystemService(DISPLAY_SERVICE);
  note("Device: "+Build.MANUFACTURER+" "+Build.MODEL+"\nAndroid: "+Build.VERSION.RELEASE+"\nABI: "+java.util.Arrays.toString(Build.SUPPORTED_ABIS)+"\nRAM total: "+m.totalMem/1048576+" MiB\nRAM available: "+m.availMem/1048576+" MiB\nDisplays reported: "+d.getDisplays().length+"\nIn multi-window: "+isInMultiWindowMode()+"\nWorkspace: "+(tree==null?"not selected":tree));
  button(body,"Refresh diagnostics",()->render("Diagnostics"));
  note("Display count does not establish HDMI compatibility. SD encryption, recovery, peripherals and OS boot remain unverified by this app.");
 }
 private void code() {
  documentLabel=text(document==null?"Untitled":document.toString(),13);body.addView(documentLabel);
  LinearLayout controls=new LinearLayout(this);
  button(controls,"Open",()->guardDraft(()->safe(()->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION),11))));
  button(controls,"Save",()->{capture();if(document==null)saveAs();else confirmSave();});
  button(controls,"Save as",this::saveAs);body.addView(controls);
  editor=new EditText(this);editor.setTextColor(ink);editor.setGravity(Gravity.TOP);editor.setTypeface(android.graphics.Typeface.MONOSPACE);editor.setTextSize(14);editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
  editor.setMinLines(15);editor.setText(draft);body.addView(editor,new LinearLayout.LayoutParams(-1,-2));
  note("UTF-8 text editor · 256 KiB limit · use Termux for execution");
 }
 private void guardDraft(Runnable next){capture();if(!dirty){next.run();return;}new AlertDialog.Builder(this).setTitle("Discard unsaved changes?").setMessage("Save the current document before opening another to keep your edits.").setNegativeButton("Cancel",null).setPositiveButton("Discard", (d,w)->next.run()).show();}
 private void saveAs(){capture();safe(()->startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"untitled.txt"),12));}
 private void confirmSave(){new AlertDialog.Builder(this).setTitle("Replace this document?").setMessage(document.toString()).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->write(document)).show();}
 private void write(Uri uri) {
  capture();final String content=draft;final byte[] bytes=content.getBytes(StandardCharsets.UTF_8);
  if(!WorkspacePolicy.editableSize(bytes.length)){error("Document exceeds 256 KiB limit");return;}
  io.execute(()->{try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("Provider did not open document");out.write(bytes);out.flush();runOnUiThread(()->{document=uri;capture();dirty=!draft.equals(content);Toast.makeText(this,"Saved",Toast.LENGTH_SHORT).show();if(screen.equals("Code Studio"))render(screen);});}catch(Exception e){runOnUiThread(()->error(e.toString()));}});
 }
 private void load(Uri uri) {
  io.execute(()->{try(InputStream in=getContentResolver().openInputStream(uri)){
   if(in==null)throw new IOException("Provider did not open file");
   ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
   while((n=in.read(buffer))!=-1){if(!WorkspacePolicy.editableSize((long)out.size()+n))throw new IOException("File exceeds 256 KiB editor limit");out.write(buffer,0,n);}
   byte[] bytes=out.toByteArray();for(byte b:bytes)if(b==0)throw new IOException("Binary files cannot be edited");
   String content=StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString();
   runOnUiThread(()->{editor=null;draft=content;dirty=false;document=uri;render("Code Studio");});
  }catch(Exception e){runOnUiThread(()->error(e.toString()));}});
 }
 private Uri currentDir;
 private void files() {
  if(tree==null){note("Choose a workspace folder first. Select a folder on the adopted SD card in Android's picker.");button(body,"Choose workspace",()->render("Home"));return;}
  currentDir=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
  note("Selected workspace: "+tree);
  button(body,"Create / verify workspace folders",this::prepare);
  list(currentDir);
 }
 private void list(Uri dir) {
  currentDir=dir;
  while(body.getChildCount()>2)body.removeViewAt(2);
  note(dir.toString());
  button(body,"Workspace root",()->render("Files"));
  button(body,"New project folder here",()->{EditText input=new EditText(this);input.setSingleLine(true);new AlertDialog.Builder(this).setTitle("Folder name").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Create",(d,w)->{String n=input.getText().toString();if(!WorkspacePolicy.validName(n)){error("Use a plain name without slash or control characters");return;}safe(()->{DocumentsContract.createDocument(getContentResolver(),dir,DocumentsContract.Document.MIME_TYPE_DIR,n);list(dir);});}).show();});
  safe(()->{Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getDocumentId(dir));
   try(Cursor c=getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)) {
    if(c==null)throw new IOException("Folder provider unavailable");int count=0;
    while(c.moveToNext()){if(++count>500){note("First 500 entries shown.");break;}Uri item=DocumentsContract.buildDocumentUriUsingTree(tree,c.getString(0));String name=c.getString(1);boolean folder=DocumentsContract.Document.MIME_TYPE_DIR.equals(c.getString(2));button(body,(folder?"Folder · ":"File · ")+name,()->{if(folder)list(item);else guardDraft(()->load(item));});}
   }
  });
 }
 private void prepare() {safe(()->{
  Uri root=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
  java.util.Map<String,String> existing=new java.util.HashMap<>();
  Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
  try(Cursor c=getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){if(c==null)throw new IOException("Workspace inaccessible");while(c.moveToNext())existing.put(c.getString(0),c.getString(1));}
  for(String name:WorkspacePolicy.DIRECTORIES){if(existing.containsKey(name)){if(!DocumentsContract.Document.MIME_TYPE_DIR.equals(existing.get(name)))throw new IOException("A file blocks folder "+name);}else if(DocumentsContract.createDocument(getContentResolver(),root,DocumentsContract.Document.MIME_TYPE_DIR,name)==null)throw new IOException("Cannot create "+name);}
  Toast.makeText(this,"Eight workspace folders verified",Toast.LENGTH_LONG).show();render("Files");
 });}
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();
  if(req==10)safe(()->{int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);if((flags&Intent.FLAG_GRANT_WRITE_URI_PERMISSION)==0)throw new IOException("Workspace needs write permission");getContentResolver().takePersistableUriPermission(uri,flags);tree=uri;getPreferences(0).edit().putString("workspace",uri.toString()).apply();render("Files");});
  else if(req==11)load(uri);else if(req==12)write(uri);
 }
 private void launch(String pkg,boolean adjacent){safe(()->{Intent i=getPackageManager().getLaunchIntentForPackage(pkg);if(i==null)throw new IOException("Install "+pkg+" from its official distribution first");i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);if(adjacent)i.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);startActivity(i);});}
 private void openSettings(String action){safe(()->startActivity(new Intent(action)));}
 private interface Checked {void run() throws Exception;}
 private void safe(Checked r){try{r.run();}catch(Exception e){error(e.toString());}}
 private void error(String s){if(!isFinishing())new AlertDialog.Builder(this).setTitle("Action could not complete").setMessage(s).setPositiveButton("OK",null).show();}
 @Override protected void onDestroy(){io.shutdown();super.onDestroy();}
}
