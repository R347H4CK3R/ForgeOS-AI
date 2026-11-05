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
