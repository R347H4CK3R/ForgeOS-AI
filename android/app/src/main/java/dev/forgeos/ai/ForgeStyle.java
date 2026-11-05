package dev.forgeos.ai;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Static native surfaces: no bitmap wallpaper, blur pass or animation loop. */
public final class ForgeStyle {
 private ForgeStyle(){}
 public static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
 public static boolean dark(Context c){return c.getSharedPreferences("MainActivity",0).getBoolean("dark",true);}
 public static int ink(boolean dark){return dark?0xfff5f6ff:0xff172338;}
 public static GradientDrawable glass(Context c,boolean dark,int radius){
  GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{dark?0xd52a3149:0xeefbfcff,dark?0xc318233b:0xe5e2eaff});
  g.setCornerRadius(dp(c,radius));g.setStroke(dp(c,1),dark?0x35ffffff:0x70ffffff);return g;
 }
 public static Drawable wallpaper(boolean dark){return new Wallpaper(dark);}
 public static void install(Activity activity,View content){
  boolean dark=dark(activity);int ink=ink(dark);
  activity.getWindow().setBackgroundDrawable(wallpaper(dark));
  LinearLayout outer=new LinearLayout(activity);outer.setOrientation(LinearLayout.VERTICAL);outer.setFitsSystemWindows(true);outer.setBackground(wallpaper(dark));
  Button back=new Button(activity);back.setText("\u2039  ForgeOS");back.setAllCaps(false);back.setTextColor(ink);back.setTextSize(16);back.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);back.setBackgroundColor(Color.TRANSPARENT);back.setMinHeight(dp(activity,48));back.setOnClickListener(v->activity.finish());outer.addView(back,new LinearLayout.LayoutParams(-1,dp(activity,52)));
  content.setFitsSystemWindows(false);style(activity,content,dark);
  outer.addView(content,new LinearLayout.LayoutParams(-1,0,1));activity.setContentView(outer);
  WindowInsetsController bars=activity.getWindow().getInsetsController();if(bars!=null)bars.setSystemBarsAppearance(dark?0:WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
 }
 private static void style(Context c,View view,boolean dark){
  if(view instanceof EditText){
   EditText e=(EditText)view;e.setTextColor(ink(dark));e.setHintTextColor(dark?0xffb6bfd4:0xff53637e);e.setBackground(glass(c,dark,16));e.setPadding(dp(c,16),dp(c,14),dp(c,16),dp(c,14));e.setMinHeight(dp(c,52));
  }else if(view instanceof Button){
   Button b=(Button)view;b.setAllCaps(false);b.setTextColor(ink(dark));b.setTextSize(15);b.setMinHeight(dp(c,52));b.setPadding(dp(c,16),dp(c,12),dp(c,16),dp(c,12));b.setBackground(glass(c,dark,18));
  }else if(view instanceof TextView){
   TextView t=(TextView)view;t.setTextColor(t.getText().toString().startsWith("EXPERIMENTAL")?(dark?0xffffd59a:0xff734000):ink(dark));t.setLineSpacing(dp(c,3),1);
   if(t.getTextSize()/c.getResources().getDisplayMetrics().scaledDensity>=23)t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
  }
  if(view.getLayoutParams() instanceof LinearLayout.LayoutParams){LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)view.getLayoutParams();p.topMargin=dp(c,5);p.bottomMargin=dp(c,5);view.setLayoutParams(p);}
  if(view instanceof ScrollView){view.setBackgroundColor(Color.TRANSPARENT);}
  if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;if(group instanceof LinearLayout&&group.getParent() instanceof ScrollView){group.setPadding(dp(c,20),dp(c,8),dp(c,20),dp(c,28));group.setBackgroundColor(Color.TRANSPARENT);}for(int i=0;i<group.getChildCount();i++)style(c,group.getChildAt(i),dark);}
 }
 public static Drawable glyph(String symbol,int color){return new Glyph(symbol,color);}
 private static final class Glyph extends Drawable{
  final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final String symbol;final int color;
  Glyph(String symbol,int color){this.symbol=symbol;this.color=color;}
  @Override public void draw(Canvas canvas){Rect b=getBounds();float w=b.width(),h=b.height();paint.setShader(new LinearGradient(b.left,b.top,b.right,b.bottom,new int[]{color,blend(color)},null,Shader.TileMode.CLAMP));canvas.drawRoundRect(new RectF(b),w*.24f,h*.24f,paint);paint.setShader(null);paint.setColor(Color.WHITE);paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(w*(symbol.length()>2?.28f:.38f));canvas.drawText(symbol,b.centerX(),b.centerY()-(paint.ascent()+paint.descent())/2,paint);}
  private int blend(int c){return Color.rgb(Color.red(c)*2/3,Color.green(c)*2/3,Math.min(255,Color.blue(c)+28));}
  @Override public void setAlpha(int value){paint.setAlpha(value);}
  @Override public void setColorFilter(ColorFilter f){paint.setColorFilter(f);}
  @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
 }
 private static final class Wallpaper extends Drawable{
  final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final boolean dark;
  Wallpaper(boolean dark){this.dark=dark;}
  @Override public void draw(Canvas canvas){
   Rect b=getBounds();float w=b.width(),h=b.height();if(w<=0||h<=0)return;
   paint.setShader(new LinearGradient(0,0,w,h,dark?new int[]{0xff080d20,0xff10234a,0xff16102d}:new int[]{0xffe1eaff,0xffedf2ff,0xffdcdff4},null,Shader.TileMode.CLAMP));canvas.drawRect(b,paint);
   int save=canvas.save();canvas.rotate(-28,w*.5f,h*.5f);
   for(int i=0;i<3;i++){RectF oval=new RectF(-w*.35f+i*w*.22f,h*.1f+i*h*.08f,w*1.2f+i*w*.12f,h*.8f+i*h*.08f);paint.setShader(new LinearGradient(0,oval.top,w,oval.bottom,dark?new int[]{0x053a80ff,0x603361b9,0x105c48a6}:new int[]{0x05ffffff,0x406c91dd,0x20ffffff},null,Shader.TileMode.CLAMP));paint.setStyle(Paint.Style.FILL);canvas.drawOval(oval,paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);canvas.drawOval(oval,paint);}
   canvas.restoreToCount(save);paint.setStyle(Paint.Style.FILL);paint.setShader(null);
  }
  @Override public void setAlpha(int value){}
  @Override public void setColorFilter(ColorFilter filter){}
  @Override public int getOpacity(){return PixelFormat.OPAQUE;}
 }
}
