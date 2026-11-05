package dev.forgeos.ai;
import android.app.UiAutomation;
import android.graphics.Bitmap;
import android.view.*;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.io.*;
@RunWith(AndroidJUnit4.class)
public class NativeReviewTest {
 private View find(View v,String text){if(v instanceof TextView&&text.equals(((TextView)v).getText().toString()))return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View r=find(g.getChildAt(i),text);if(r!=null)return r;}}return null;}
 private boolean hasWebView(View v){if(v instanceof android.webkit.WebView)return true;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(hasWebView(g.getChildAt(i)))return true;}return false;}
 private EditText edit(View v){if(v instanceof EditText)return (EditText)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){EditText r=edit(g.getChildAt(i));if(r!=null)return r;}}return null;}
 private void click(ActivityScenario<MainActivity> s,String label){s.onActivity(a->{View v=find(a.getWindow().getDecorView(),label);assertNotNull(label,v);assertTrue(v.performClick());});InstrumentationRegistry.getInstrumentation().waitForIdleSync();}
 private boolean systemError(android.view.accessibility.AccessibilityNodeInfo node){
  if(node==null)return false;
  CharSequence text=node.getText();
  if("android".contentEquals(node.getPackageName()==null?"":node.getPackageName())&&text!=null){String value=text.toString().toLowerCase(java.util.Locale.ROOT);if(value.contains("responding")||value.contains("keeps stopping"))return true;}
  for(int i=0;i<node.getChildCount();i++){android.view.accessibility.AccessibilityNodeInfo child=node.getChild(i);if(systemError(child))return true;}
  return false;
 }
 private void shot(String name)throws Exception{
  InstrumentationRegistry.getInstrumentation().waitForIdleSync();
  InstrumentationRegistry.getInstrumentation().getUiAutomation().waitForIdle(250,5000);
  android.os.SystemClock.sleep(1500); // Let SurfaceFlinger present the current Activity frame.
  assertFalse("Android system error dialog blocks UI preview",systemError(InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow()));
  File dir=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),"review");assertTrue(dir.isDirectory()||dir.mkdirs());
  Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(bitmap);
  try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}bitmap.recycle();
 }
 @Test public void repositoryUpdaterShowsStagingGateWithoutInstallControls()throws Exception {
  try(ActivityScenario<OsUpdateActivity> scenario=ActivityScenario.launch(OsUpdateActivity.class)){
   scenario.onActivity(activity->{View root=activity.getWindow().getDecorView();assertFalse(hasWebView(root));assertNotNull(find(root,"Check latest repository build"));assertNotNull(find(root,"Export verified image"));assertNull(find(root,"Install update"));});
   shot("os-updater");
  }
 }
 private Object field(Object object,String name)throws Exception{java.lang.reflect.Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
 private void call(Object object,String name,Class<?> type,Object value)throws Exception{java.lang.reflect.Method m=object.getClass().getDeclaredMethod(name,type);m.setAccessible(true);m.invoke(object,value);}
 @Test public void slowOpenLocksEditorAndCannotRebindSavedDocument()throws Exception {
  android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  File a=new File(context.getCacheDir(),"race-a.txt"),b=new File(context.getCacheDir(),"race-b.txt");
  try(FileOutputStream out=new FileOutputStream(a)){out.write("original A".getBytes("UTF-8"));}
  try(FileOutputStream out=new FileOutputStream(b)){out.write("document B".getBytes("UTF-8"));}
  java.util.concurrent.CountDownLatch gate=new java.util.concurrent.CountDownLatch(1);
  try(ActivityScenario<MainActivity> s=ActivityScenario.launch(MainActivity.class)){
   click(s,"Code Studio");
   s.onActivity(activity->{try{
    edit(activity.getWindow().getDecorView()).setText("draft A");
    ((java.util.concurrent.ExecutorService)field(activity,"io")).execute(()->{try{gate.await(5,java.util.concurrent.TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}});
    call(activity,"load",android.net.Uri.class,android.net.Uri.fromFile(b));
    assertFalse(edit(activity.getWindow().getDecorView()).isEnabled());
    call(activity,"write",android.net.Uri.class,android.net.Uri.fromFile(a));
   }catch(Exception e){throw new AssertionError(e);}});
   gate.countDown();
   java.util.concurrent.atomic.AtomicBoolean busy=new java.util.concurrent.atomic.AtomicBoolean(true);
   long end=android.os.SystemClock.elapsedRealtime()+5000;
   while(busy.get()&&android.os.SystemClock.elapsedRealtime()<end){s.onActivity(activity->{try{busy.set((Boolean)field(activity,"busy"));}catch(Exception e){throw new AssertionError(e);}});android.os.SystemClock.sleep(20);}
   assertFalse("Open did not finish",busy.get());
   s.onActivity(activity->{try{assertEquals(android.net.Uri.fromFile(b),field(activity,"document"));assertEquals("document B",edit(activity.getWindow().getDecorView()).getText().toString());}catch(Exception e){throw new AssertionError(e);}});
   try(FileInputStream in=new FileInputStream(a)){byte[] bytes=new byte[100];int n=in.read(bytes);assertEquals("original A",new String(bytes,0,n,"UTF-8"));}
  } finally{gate.countDown();a.delete();b.delete();}
 }
 @Test public void cancelledBeforeEntryDoesNotRestart()throws Exception{LocalEngine.prepare();LocalEngine.cancel();try{LocalEngine.generate(-1,new byte[]{65});fail("Cancelled run started");}catch(IOException expected){assertTrue(expected.getMessage().contains("Cancelled"));}finally{LocalEngine.prepare();}}
 @Test(timeout=180000) public void realSmallGgufGeneratesText()throws Exception{
  File file=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),"test-model.gguf");assertTrue("Pinned test model missing",file.isFile());
  LocalEngine.prepare();
  try(android.os.ParcelFileDescriptor fd=android.os.ParcelFileDescriptor.open(file,android.os.ParcelFileDescriptor.MODE_READ_ONLY)){
   byte[] output=LocalEngine.generate(fd.getFd(),"Say hello in one short sentence.".getBytes("UTF-8"));assertNotNull(output);assertTrue("Model returned no text",output.length>0);assertTrue(output.length<16384);
   File report=new File(file.getParentFile(),"inference-result.txt");try(FileOutputStream out=new FileOutputStream(report)){out.write(output);}
  }
 }
 @Test public void modelManagerIsNativeAndUnverifiedModelCannotBeSelected()throws Exception{
  try(ActivityScenario<ModelManagerActivity> scenario=ActivityScenario.launch(ModelManagerActivity.class)){
   scenario.onActivity(a->{assertFalse(hasWebView(a.getWindow().getDecorView()));View use=find(a.getWindow().getDecorView(),"Use verified model");assertNotNull(use);assertFalse(use.isEnabled());});
   shot("model-manager");
  }
 }
 @Test public void remoteAgentShowsReviewGateWithoutNetworkRequest()throws Exception{
  try(ActivityScenario<RemoteActivity> scenario=ActivityScenario.launch(RemoteActivity.class)){
   scenario.onActivity(a->{assertFalse(hasWebView(a.getWindow().getDecorView()));assertNotNull(find(a.getWindow().getDecorView(),"Plan / diagnose project task (no writes)"));assertNotNull(find(a.getWindow().getDecorView(),"Review and apply last edit plan"));});
   shot("remote-agent");
  }
 }
 @Test(timeout=150000) public void modelDownloadVerifiesAcrossActivityRecreation()throws Exception{
  try(ActivityScenario<ModelManagerActivity> scenario=ActivityScenario.launch(ModelManagerActivity.class)){
   try{
    scenario.onActivity(a->{View start=find(a.getWindow().getDecorView(),"Download starter model");assertNotNull(start);start.performClick();});
    android.os.SystemClock.sleep(500);scenario.recreate();
    java.util.concurrent.atomic.AtomicBoolean verified=new java.util.concurrent.atomic.AtomicBoolean(false);
    java.util.concurrent.atomic.AtomicReference<String> detail=new java.util.concurrent.atomic.AtomicReference<>("");
    long deadline=android.os.SystemClock.elapsedRealtime()+120000;
    while(!verified.get()&&android.os.SystemClock.elapsedRealtime()<deadline){
     scenario.onActivity(a->{try{verified.set(field(a,"verifiedFile")!=null);detail.set(((TextView)field(a,"status")).getText().toString());}catch(Exception e){throw new AssertionError(e);}});
     android.os.SystemClock.sleep(500);
    }
    assertTrue(detail.get(),verified.get());shot("model-manager-verified");
   }finally{
    scenario.onActivity(a->{try{java.lang.reflect.Method remove=a.getClass().getDeclaredMethod("removeCurrent");remove.setAccessible(true);remove.invoke(a);}catch(Exception e){throw new AssertionError(e);}});
   }
  }
 }
 @Test public void nativeRuntimeRejectsEmptyPrompt()throws Exception{try{LocalEngine.generate(-1,new byte[0]);fail("Empty prompt accepted");}catch(IOException expected){assertTrue(expected.getMessage().contains("Prompt"));}}
 @Test public void nativeNavigationEditorRecreationAndThemes()throws Exception{
  try(ActivityScenario<MainActivity> s=ActivityScenario.launch(MainActivity.class)){
   s.onActivity(a->assertFalse(hasWebView(a.getWindow().getDecorView())));shot("home-portrait");
   click(s,"Code Studio");s.onActivity(a->{EditText e=edit(a.getWindow().getDecorView());assertNotNull(e);e.setText("print(42)");});
   click(s,"Diagnostics");click(s,"Code Studio");s.onActivity(a->assertEquals("print(42)",edit(a.getWindow().getDecorView()).getText().toString()));
   s.recreate();s.onActivity(a->assertEquals("print(42)",edit(a.getWindow().getDecorView()).getText().toString()));shot("editor-portrait");
   click(s,"Settings");click(s,"Use light theme");s.onActivity(a->assertFalse("Tool theme must follow the launcher in review and release packages",ForgeStyle.dark(a)));shot("settings-light");click(s,"Home");shot("home-light");click(s,"Settings");
   InstrumentationRegistry.getInstrumentation().getUiAutomation().setRotation(UiAutomation.ROTATION_FREEZE_90);
   InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   s.recreate();android.os.SystemClock.sleep(1500);
   s.onActivity(a->assertEquals(android.content.res.Configuration.ORIENTATION_LANDSCAPE,a.getResources().getConfiguration().orientation));shot("landscape");click(s,"Home");shot("home-landscape");
  } finally {InstrumentationRegistry.getInstrumentation().getUiAutomation().setRotation(UiAutomation.ROTATION_UNFREEZE);}
 }
}
