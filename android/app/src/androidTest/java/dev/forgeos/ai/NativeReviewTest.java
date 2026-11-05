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
 private void shot(String name)throws Exception{
  InstrumentationRegistry.getInstrumentation().waitForIdleSync();
  File dir=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),"review");assertTrue(dir.isDirectory()||dir.mkdirs());
  Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(bitmap);
  try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}bitmap.recycle();
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
 @Test public void realSmallGgufGeneratesText()throws Exception{
  File file=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(),"test-model.gguf");assertTrue("Pinned test model missing",file.isFile());
  LocalEngine.prepare();
  try(android.os.ParcelFileDescriptor fd=android.os.ParcelFileDescriptor.open(file,android.os.ParcelFileDescriptor.MODE_READ_ONLY)){
   byte[] output=LocalEngine.generate(fd.getFd(),"Say hello in one short sentence.".getBytes("UTF-8"));assertNotNull(output);assertTrue("Model returned no text",output.length>0);assertTrue(output.length<16384);
   File report=new File(file.getParentFile(),"inference-result.txt");try(FileOutputStream out=new FileOutputStream(report)){out.write(output);}
  }
 }
 @Test public void nativeRuntimeRejectsEmptyPrompt()throws Exception{try{LocalEngine.generate(-1,new byte[0]);fail("Empty prompt accepted");}catch(IOException expected){assertTrue(expected.getMessage().contains("Prompt"));}}
 @Test public void nativeNavigationEditorRecreationAndThemes()throws Exception{
  try(ActivityScenario<MainActivity> s=ActivityScenario.launch(MainActivity.class)){
   s.onActivity(a->assertFalse(hasWebView(a.getWindow().getDecorView())));shot("home-portrait");
   click(s,"Code Studio");s.onActivity(a->{EditText e=edit(a.getWindow().getDecorView());assertNotNull(e);e.setText("print(42)");});
   click(s,"Diagnostics");click(s,"Code Studio");s.onActivity(a->assertEquals("print(42)",edit(a.getWindow().getDecorView()).getText().toString()));
   s.recreate();s.onActivity(a->assertEquals("print(42)",edit(a.getWindow().getDecorView()).getText().toString()));shot("editor-portrait");
   click(s,"Settings");click(s,"Use light theme");shot("settings-light");
   InstrumentationRegistry.getInstrumentation().getUiAutomation().setRotation(UiAutomation.ROTATION_FREEZE_90);
   InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   s.recreate();shot("landscape");
  } finally {InstrumentationRegistry.getInstrumentation().getUiAutomation().setRotation(UiAutomation.ROTATION_UNFREEZE);}
 }
}
