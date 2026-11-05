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
 private boolean dirty=false,dark=true,busy=false;
 private void enabled(View view,boolean value){view.setEnabled(value);if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++)enabled(group.getChildAt(i),value);}}
 private void busy(boolean value){busy=value;if(page!=null)enabled(page,!value);}
 private void complete(Runnable action){runOnUiThread(()->{if(isDestroyed()||isFinishing())return;busy(false);action.run();});}
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
 private android.graphics.drawable.GradientDrawable surface(int color,int radius) {
  android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;
 }
 private void button(LinearLayout target,String title,Runnable action){
  Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextColor(ink);b.setMinHeight(dp(48));b.setPadding(dp(14),dp(8),dp(14),dp(8));b.setBackground(ForgeStyle.glass(this,dark,18));
  b.setOnClickListener(v->action.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(target.getOrientation()==LinearLayout.VERTICAL?-1:-2,-2);p.setMargins(dp(3),dp(4),dp(3),dp(4));target.addView(b,p);
 }
 private String glyph(String title){
  switch(title){case "Home":return "F";case "Projects":case "Files":return "[]";case "Code Studio":return "{}";case "Local AI":return "AI";case "Terminal":return ">_";case "Remote AI & agent":return "NET";case "Devices":return "LINK";case "Models":return "M";case "Settings":return "*";default:return "+";}
 }
 private int iconColor(String title){int[] colors={0xff447ff2,0xff9b62e4,0xff279cbb,0xffe99b39,0xff4d9e6b,0xffc65782};return colors[(title.hashCode()&0x7fffffff)%colors.length];}
 private void tile(LinearLayout row,String title,String subtitle,int icon,Runnable action){
  Button b=new Button(this);b.setText(title);b.setTextSize(12);b.setAllCaps(false);b.setTextColor(ink);b.setGravity(Gravity.CENTER);b.setPadding(dp(2),dp(6),dp(2),dp(6));b.setMinWidth(0);b.setMinimumWidth(0);b.setMaxLines(2);b.setBackgroundColor(Color.TRANSPARENT);
  android.graphics.drawable.Drawable image=ForgeStyle.glyph(glyph(title),iconColor(title));image.setBounds(0,0,dp(58),dp(58));b.setCompoundDrawables(null,image,null,null);b.setCompoundDrawablePadding(dp(9));b.setContentDescription(title+": "+subtitle);
  b.setOnClickListener(v->action.run());LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,dp(116),1);params.setMargins(dp(2),dp(4),dp(2),dp(4));row.addView(b,params);
 }
 private void note(String s){body.addView(text(s,16));}
 private void render(String name) {
  if(busy)return;capture();editor=null;screen=name;
  ink=ForgeStyle.ink(dark);
  bg=dark?Color.rgb(13,22,32):Color.rgb(241,245,249);
  getWindow().setBackgroundDrawable(ForgeStyle.wallpaper(dark));
  if(android.os.Build.VERSION.SDK_INT<35)getWindow().setStatusBarColor(bg);
  panel=dark?Color.rgb(23,39,53):Color.WHITE;
  page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setBackground(ForgeStyle.wallpaper(dark));
  page.setPadding(dp(14),dp(12),dp(14),dp(8));page.setFitsSystemWindows(true);
  TextView heading=text(name.equals("Home")?"ForgeOS":name,28);heading.setTypeface(android.graphics.Typeface.create("sans-serif-medium",0));page.addView(heading);
  ScrollView scroll=new ScrollView(this);
  body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(8),dp(8),dp(8),dp(8));body.setBackground(name.equals("Home")?new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT):ForgeStyle.glass(this,dark,26));
  scroll.addView(body);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  HorizontalScrollView dock=new HorizontalScrollView(this);dock.setFillViewport(true);LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);
  nav.setPadding(dp(7),dp(4),dp(7),dp(4));nav.setBackground(ForgeStyle.glass(this,dark,28));dock.setHorizontalScrollBarEnabled(false);
  for(String destination:new String[]{"Home","Files","Code Studio","Devices","Diagnostics","Settings"}){
   Button item=new Button(this);item.setText(destination);item.setAllCaps(false);item.setTextSize(10);item.setTextColor(ink);item.setMinWidth(0);item.setMinimumWidth(0);item.setPadding(dp(3),dp(7),dp(3),dp(6));
   android.graphics.drawable.Drawable image=ForgeStyle.glyph(glyph(destination),destination.equals(name)?0xff557eec:0xff465777);image.setBounds(0,0,dp(25),dp(25));item.setCompoundDrawables(null,image,null,null);item.setCompoundDrawablePadding(dp(5));item.setBackground(destination.equals(name)?ForgeStyle.glass(this,dark,20):new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));item.setSelected(destination.equals(name));item.setOnClickListener(v->render(destination));nav.addView(item,new LinearLayout.LayoutParams(dp(Math.max(48,Math.min(72,(getResources().getConfiguration().screenWidthDp-56)/6))),dp(68)));
  }
  dock.addView(nav);page.addView(dock);setContentView(page);
  if(android.os.Build.VERSION.SDK_INT>=30){android.view.WindowInsetsController bars=getWindow().getInsetsController();if(bars!=null)bars.setSystemBarsAppearance(dark?0:android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);}
  else {View decor=getWindow().getDecorView();int flags=decor.getSystemUiVisibility();decor.setSystemUiVisibility(dark?flags&~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR:flags|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);}
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
  LinearLayout widgets=new LinearLayout(this);widgets.setBaselineAligned(false);
  LinearLayout clock=new LinearLayout(this);clock.setOrientation(LinearLayout.VERTICAL);clock.setPadding(dp(16),dp(12),dp(16),dp(12));clock.setBackground(ForgeStyle.glass(this,dark,26));
  TextClock time=new TextClock(this);time.setFormat12Hour("h:mm");time.setFormat24Hour("HH:mm");time.setTextSize(38);time.setTextColor(ink);time.setTypeface(android.graphics.Typeface.create("sans-serif-light",0));clock.addView(time);
  TextClock date=new TextClock(this);date.setFormat12Hour("EEEE, MMM d");date.setFormat24Hour("EEEE, MMM d");date.setTextSize(12);date.setTextColor(ink);clock.addView(date);
  LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,dp(122),1);left.setMargins(0,0,dp(6),0);widgets.addView(clock,left);
  LinearLayout workspace=new LinearLayout(this);workspace.setOrientation(LinearLayout.VERTICAL);workspace.setPadding(dp(10),dp(6),dp(10),dp(6));workspace.setBackground(ForgeStyle.glass(this,dark,26));
  TextView workspaceTitle=text("WORKSPACE",11);workspaceTitle.setLetterSpacing(.14f);workspace.addView(workspaceTitle);
  button(workspace,"Choose workspace folder",()->safe(()->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION),10)));
  LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,-2,1);right.setMargins(dp(6),0,0,0);widgets.addView(workspace,right);body.addView(widgets);
  TextView section=text("Your workstation",20);section.setTypeface(android.graphics.Typeface.create("sans-serif-medium",0));body.addView(section);
  int columns=getResources().getConfiguration().screenWidthDp>=600?6:4;
  String[] titles={"Projects","Code Studio","Local AI","Terminal","Remote AI & agent","Devices","Models","Diagnostics","Settings"};
  String[] descriptions={"Files and project folders","Edit and save source","Offline model inference","Python, Git and SSH","Diagnose and review repairs","Tailscale and connections","Downloads and verification","Device and memory details","Appearance and OS updates"};
  Runnable[] actions={()->render("Files"),()->render("Code Studio"),()->startActivity(new Intent(this,LocalActivity.class)),()->startActivity(new Intent(this,TerminalActivity.class)),()->startActivity(new Intent(this,RemoteActivity.class)),()->render("Devices"),()->startActivity(new Intent(this,ModelManagerActivity.class)),()->render("Diagnostics"),()->render("Settings")};
  LinearLayout row=null;
  for(int i=0;i<titles.length;i++){if(i%columns==0){row=new LinearLayout(this);body.addView(row);}tile(row,titles[i],descriptions[i],0,actions[i]);if(i==titles.length-1){for(int j=(i%columns)+1;j<columns;j++)row.addView(new View(this),new LinearLayout.LayoutParams(0,dp(116),1));}}
  TextView badge=text((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM)!=0?"OS preview / hardware unverified":"Native preview / stock Android",11);badge.setAlpha(.8f);body.addView(badge);
  TextView hint=text(tree==null?"Choose a workspace above to connect your files.":"Workspace connected. Open Projects to browse files.",13);hint.setAlpha(.8f);body.addView(hint);
 }
 private void settings() {
  button(body,"OS updates · stage and export",()->startActivity(new Intent(this,OsUpdateActivity.class)));
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
  if(busy)return;busy(true);
  io.execute(()->{try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("Provider did not open document");out.write(bytes);out.flush();complete(()->{document=uri;capture();dirty=!draft.equals(content);Toast.makeText(this,"Saved",Toast.LENGTH_SHORT).show();if(screen.equals("Code Studio"))render(screen);});}catch(Exception e){complete(()->error(e.toString()));}});
 }
 private void load(Uri uri) {
  if(busy)return;busy(true);
  io.execute(()->{try(InputStream in=getContentResolver().openInputStream(uri)){
   if(in==null)throw new IOException("Provider did not open file");
   ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
   while((n=in.read(buffer))!=-1){if(!WorkspacePolicy.editableSize((long)out.size()+n))throw new IOException("File exceeds 256 KiB editor limit");out.write(buffer,0,n);}
   byte[] bytes=out.toByteArray();for(byte b:bytes)if(b==0)throw new IOException("Binary files cannot be edited");
   String content=StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString();
   complete(()->{editor=null;draft=content;dirty=false;document=uri;render("Code Studio");});
  }catch(Exception e){complete(()->error(e.toString()));}});
 }
 private Uri currentDir;
 private void files() {
  if(tree==null){note("Choose a workspace folder first. Select a folder on the adopted SD card in Android's picker.");button(body,"Choose workspace",()->render("Home"));return;}
  currentDir=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
  note("Selected workspace: "+tree);
  button(body,"Create / verify workspace folders",this::prepare);
  list(currentDir);
 }
 private void background(Checked task,Runnable done) {
  if(busy)return;busy(true);
  io.execute(()->{try{task.run();complete(done);}catch(Exception e){complete(()->error(e.toString()));}});
 }
 private static final class Entry {
  final Uri uri;final String name;final boolean folder;
  Entry(Uri u,String n,boolean f){uri=u;name=n;folder=f;}
 }
 private void list(Uri dir) {
  final java.util.List<Entry> entries=new java.util.ArrayList<>();
  background(()->{
   Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getDocumentId(dir));
   try(Cursor c=getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){
    if(c==null)throw new IOException("Folder provider unavailable");
    while(c.moveToNext()&&entries.size()<500)entries.add(new Entry(DocumentsContract.buildDocumentUriUsingTree(tree,c.getString(0)),c.getString(1),DocumentsContract.Document.MIME_TYPE_DIR.equals(c.getString(2))));
   }
  },()->{
   currentDir=dir;while(body.getChildCount()>2)body.removeViewAt(2);
   note(dir.toString());
   button(body,"Workspace root",()->render("Files"));
   button(body,"New project folder here",()->newFolder(dir));
   for(Entry item:entries)button(body,(item.folder?"Folder · ":"File · ")+item.name,()->{if(item.folder)list(item.uri);else guardDraft(()->load(item.uri));});
   if(entries.size()==500)note("Up to 500 entries shown.");
  });
 }
 private void newFolder(Uri dir) {
  EditText input=new EditText(this);input.setSingleLine(true);
  new AlertDialog.Builder(this).setTitle("Folder name").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Create",(d,w)->{
   String name=input.getText().toString();if(!WorkspacePolicy.validName(name)){error("Use a plain name without slash or control characters");return;}
   background(()->{if(DocumentsContract.createDocument(getContentResolver(),dir,DocumentsContract.Document.MIME_TYPE_DIR,name)==null)throw new IOException("Provider did not create folder");},()->list(dir));
  }).show();
 }
 private void prepare() {
  background(()->{
   Uri root=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
   java.util.Map<String,String> existing=new java.util.HashMap<>();
   Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
   try(Cursor c=getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){if(c==null)throw new IOException("Workspace inaccessible");while(c.moveToNext())existing.put(c.getString(0),c.getString(1));}
   for(String name:WorkspacePolicy.DIRECTORIES){if(existing.containsKey(name)){if(!DocumentsContract.Document.MIME_TYPE_DIR.equals(existing.get(name)))throw new IOException("A file blocks folder "+name);}else if(DocumentsContract.createDocument(getContentResolver(),root,DocumentsContract.Document.MIME_TYPE_DIR,name)==null)throw new IOException("Cannot create "+name);}
  },()->{Toast.makeText(this,"Eight workspace folders verified",Toast.LENGTH_LONG).show();render("Files");});
 }
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();
  if(req==10)safe(()->{int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);if(flags!=(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION))throw new IOException("Workspace needs read and write permission");getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);tree=uri;getPreferences(0).edit().putString("workspace",uri.toString()).apply();render("Files");});
  else if(req==11)load(uri);else if(req==12)write(uri);
 }
 private void launch(String pkg,boolean adjacent){safe(()->{Intent i=getPackageManager().getLaunchIntentForPackage(pkg);if(i==null)throw new IOException("Install "+pkg+" from its official distribution first");i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);if(adjacent)i.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);startActivity(i);});}
 private void openSettings(String action){safe(()->startActivity(new Intent(action)));}
 private interface Checked {void run() throws Exception;}
 private void safe(Checked r){try{r.run();}catch(Exception e){error(e.toString());}}
 private void error(String s){if(!isFinishing()&&!isDestroyed())new AlertDialog.Builder(this).setTitle("Action could not complete").setMessage(s).setPositiveButton("OK",null).show();}
 @Override public void onBackPressed(){if(busy)return;guardDraft(()->{dirty=false;super.onBackPressed();});}
 @Override protected void onDestroy(){io.shutdown();super.onDestroy();}
}
