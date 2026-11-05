package dev.forgeos.ai;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.net.wifi.WifiManager;
import android.bluetooth.BluetoothAdapter;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import org.json.JSONObject;
import java.io.File;
public final class MainActivity extends Activity {
 private WebView web;
 private final class Bridge {
  @JavascriptInterface public String status() {
   JSONObject o=new JSONObject();
   try {
    WifiManager wifi=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    o.put("wifi",wifi!=null&&wifi.isWifiEnabled());
    BluetoothAdapter bt=BluetoothAdapter.getDefaultAdapter();
    o.put("bluetooth",bt!=null&&bt.isEnabled());
    DisplayManager dm=(DisplayManager)getSystemService(Context.DISPLAY_SERVICE);
    o.put("displays",dm.getDisplays().length);
    o.put("tailscale",installed("com.tailscale.ipn"));
    o.put("termux",installed("com.termux"));
    o.put("rustdesk",installed("com.carriez.flutter_hbb"));
    o.put("device",android.os.Build.MODEL);
    o.put("android",android.os.Build.VERSION.RELEASE);
    File workspace=workspaceRoot(false);
    o.put("workspace",workspace==null?"unavailable":workspace.getAbsolutePath());
    o.put("workspaceAvailable",workspace!=null);
    o.put("workspaceFreeGb",workspace==null?0:Math.round(workspace.getUsableSpace()/107374182.4)/10.0);
   }catch(Exception e){try{o.put("error",e.toString());}catch(Exception ignored){}}
   return o.toString();
  }
  private boolean installed(String pkg) {
   try{getPackageManager().getPackageInfo(pkg,0);return true;}catch(Exception e){return false;}
  }
  private File workspaceRoot(boolean create) {
   File root=getFilesDir();
   if(root==null)return null;
   File candidate=new File(root,"ForgeWorkspace");
   if(create&&!candidate.exists()&&!candidate.mkdirs())return null;
   return candidate;
  }
  @JavascriptInterface public String prepareWorkspace() {
   JSONObject o=new JSONObject();
   try {
    File root=workspaceRoot(true);
    if(root==null)throw new IllegalStateException("Workspace unavailable");
    for(String name:new String[]{"models","projects","datasets","downloads","logs"}){
     File dir=new File(root,name);
     if(!dir.isDirectory()&&!dir.mkdirs())throw new IllegalStateException("Cannot create "+name);
    }
    o.put("ok",true);o.put("path",root.getAbsolutePath());
    o.put("freeGb",Math.round(root.getUsableSpace()/107374182.4)/10.0);
   }catch(Exception e){try{o.put("ok",false);o.put("error",e.toString());}catch(Exception ignored){}}
   return o.toString();
  }
  @JavascriptInterface public void openApp(String id){
   String pkg;
   switch(id){case "tailscale":pkg="com.tailscale.ipn";break;
    case "termux":pkg="com.termux";break;
    case "rustdesk":pkg="com.carriez.flutter_hbb";break;
    default:return;}
   runOnUiThread(()->{Intent intent=getPackageManager().getLaunchIntentForPackage(pkg);if(intent!=null)startActivity(intent);});
  }
  @JavascriptInterface public void openSettings(String type){
   String action;
   switch(type){case "wifi":action="android.settings.WIFI_SETTINGS";break;
    case "bluetooth":action="android.settings.BLUETOOTH_SETTINGS";break;
    case "display":action="android.settings.DISPLAY_SETTINGS";break;
    default:action="android.settings.SETTINGS";}
   runOnUiThread(()->{try{startActivity(new Intent(action));}catch(Exception ignored){}});
  }
 }
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  web=new WebView(this);
  web.setWebViewClient(new WebViewClient());
  WebSettings settings=web.getSettings();
  settings.setJavaScriptEnabled(true);
  settings.setDomStorageEnabled(true);
  settings.setAllowFileAccess(true);
  web.addJavascriptInterface(new Bridge(),"ForgeBridge");
  web.loadUrl("file:///android_asset/index.html");
  setContentView(web);
 }
 @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
}
