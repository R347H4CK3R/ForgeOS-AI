package dev.forgeos.ai;

import android.app.*;
import android.content.*;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;

/** UI attaches to application-owned operations; rotation never cancels a copy. */
public final class OsUpdateActivity extends Activity {
 private final Handler handler=new Handler(Looper.getMainLooper());
 private TextView status;
 private EditText token;
 private OsUpdateCoordinator coordinator;
 private boolean active,pickerOpen;
 private final Runnable poll=new Runnable(){public void run(){if(!active)return;status.setText(coordinator.snapshot());handler.postDelayed(this,1000);}};
 @Override public void onCreate(Bundle state){
  super.onCreate(state);coordinator=OsUpdateCoordinator.get(this);pickerOpen=state!=null&&state.getBoolean("picker");
  LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(24,24,24,24);page.setFitsSystemWindows(true);page.setBackgroundColor(0xff101d29);
  ScrollView scroll=new ScrollView(this);scroll.addView(page);setContentView(scroll);
  TextView title=new TextView(this);title.setText("ForgeOS / OS updates");title.setTextSize(25);title.setTextColor(-1);page.addView(title);
  TextView warning=new TextView(this);warning.setText("EXPERIMENTAL - STAGING ONLY\nImages have no boot acceptance. Downloading does not install an OS. No flash, unlock or wipe is available here.");warning.setTextColor(0xffffc879);warning.setTextSize(17);page.addView(warning);
  token=new EditText(this);token.setHint("GitHub token (private repo; session only)");token.setTextColor(-1);token.setHintTextColor(0xffbbbbbb);token.setSingleLine();token.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);token.setSaveEnabled(false);token.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);page.addView(token);
  button(page,"Check latest repository build",()->{if(!coordinator.check(token.getText().toString().trim()))notice("Another OS update operation is active.");});
  button(page,"Download and stage verified candidate",()->{if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},90);if(!coordinator.stage(token.getText().toString().trim()))notice("Check a signed candidate first, and wait for the active operation.");});
  button(page,"Cancel own download",()->coordinator.cancelDownload(null));
  button(page,"Export verified image",this::export);
  status=new TextView(this);status.setTextColor(-1);status.setTextSize(16);page.addView(status);status.setText(coordinator.snapshot());
 }
 private void button(LinearLayout p,String s,Runnable r){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setOnClickListener(v->r.run());p.addView(b);}
 private void notice(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 private void export(){
  if(pickerOpen)return;
  if(!coordinator.hasVerified()){notice("No verified image is staged.");return;}
  try{pickerOpen=true;startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/x-xz").putExtra(Intent.EXTRA_TITLE,OsUpdateVerifier.IMAGE),81);}
  catch(Exception e){pickerOpen=false;notice("Document picker unavailable.");}
 }
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);if(request!=81)return;pickerOpen=false;
  if(result!=RESULT_OK||data==null||data.getData()==null)return;
  // Queue directly: marker initialization and other operation ownership cannot drop this result.
  coordinator.export(data.getData());
 }
 @Override protected void onSaveInstanceState(Bundle state){state.putBoolean("picker",pickerOpen);super.onSaveInstanceState(state);}
 @Override protected void onStart(){super.onStart();active=true;handler.post(poll);}
 @Override protected void onStop(){active=false;handler.removeCallbacks(poll);super.onStop();}
 @Override protected void onDestroy(){token.setText("");super.onDestroy();}
}
