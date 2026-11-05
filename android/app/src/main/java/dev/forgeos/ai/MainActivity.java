package dev.forgeos.ai;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.net.wifi.WifiManager;
import android.bluetooth.BluetoothAdapter;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import org.json.JSONObject;
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
        Display[] displays=dm.getDisplays();
        o.put("displays",displays.length);
        o.put("externalDisplay",displays.length>1);
        o.put("tailscale",installed("com.tailscale.ipn"));
        o.put("termux",installed("com.termux"));
        o.put("rustdesk",installed("com.carriez.flutter_hbb"));
        o.put("device",android.os.Build.MODEL);
        o.put("android",android.os.Build.VERSION.RELEASE);
      } catch(Exception e) { try{o.put("error",e.toString());}catch(Exception ignored){} }
      return o.toString();
    }
    private boolean installed(String p) {
      try {getPackageManager().getPackageInfo(p,0);return true;}
      catch(Exception e){return false;}
    }
    @JavascriptInterface public void openApp(String id) {
      final String pkg;
      switch(id) {
        case "tailscale":pkg="com.tailscale.ipn";break;
        case "termux":pkg="com.termux";break;
        case "rustdesk":pkg="com.carriez.flutter_hbb";break;
        default:return;
      }
      runOnUiThread(()->{Intent intent=getPackageManager().getLaunchIntentForPackage(pkg);if(intent!=null)startActivity(intent);});
    }
    @JavascriptInterface public void openSettings(String type) {
      String action;
      switch(type) {
        case "wifi":action="android.settings.WIFI_SETTINGS";break;
        case "bluetooth":action="android.settings.BLUETOOTH_SETTINGS";break;
        case "display":action="android.settings.DISPLAY_SETTINGS";break;
        default:action="android.settings.SETTINGS";
      }
      runOnUiThread(()->{try{startActivity(new Intent(action));}catch(ActivityNotFoundException e){}});
    }
  }
  @Override public void onCreate(Bundle state) {
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
  @Override public void onBackPressed() {if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
}
