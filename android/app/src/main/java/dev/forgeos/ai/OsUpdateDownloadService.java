package dev.forgeos.ai;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.*;

/** User-started bounded staging transfer. Never restarts after process death. */
public final class OsUpdateDownloadService extends Service {
 private static final String CHANNEL="os-update-staging";
 private static final int NOTICE=3201;
 private String job;
 private boolean completed;
 private final OsUpdateServiceGeneration generation=new OsUpdateServiceGeneration();
 private final Handler main=new Handler(Looper.getMainLooper());
 @Override public IBinder onBind(Intent intent){return null;}
 private Notification notification(String text,long bytes,long total){
  Intent cancel=new Intent(this,OsUpdateDownloadService.class).setAction("cancel").setData(android.net.Uri.parse("forgeos-update:"+job)).putExtra("job",job);
  PendingIntent stop=PendingIntent.getService(this,0,cancel,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  PendingIntent open=PendingIntent.getActivity(this,1,new Intent(this,OsUpdateActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder b=new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle("ForgeOS OS staging · not installed").setContentText(text).setContentIntent(open).setOnlyAlertOnce(true).setOngoing(true).addAction(new Notification.Action.Builder(null,"Cancel download",stop).build());
  if(total>0)b.setProgress(1000,(int)Math.min(1000,bytes*1000/total),false);else b.setProgress(0,0,true);
  return b.build();
 }
 @Override public int onStartCommand(Intent intent,int flags,int startId){
  generation.noteStart(startId);
  String requested=intent==null?null:intent.getStringExtra("job");
  OsUpdateCoordinator coordinator=OsUpdateCoordinator.get(this);
  if(intent!=null&&"cancel".equals(intent.getAction())){
   coordinator.cancelDownload(requested);
   if(job==null||completed)stopSelf(startId);return START_NOT_STICKY;
  }
  if(requested!=null&&requested.equals(job)&&!completed)return START_NOT_STICKY;
  if(!coordinator.prepared(requested)){
   if(job==null||completed)stopSelf(startId);
   return START_NOT_STICKY;
  }
  // The previous completion may still be queued on the main looper. Rebind this
  // service to the new global owner and make old callbacks generation-specific.
  final String owner=requested;final int ownerStartId=startId;
  job=owner;completed=false;generation.accept(owner,ownerStartId);
  NotificationManager notifications=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
  notifications.createNotificationChannel(new NotificationChannel(CHANNEL,"OS image staging",NotificationManager.IMPORTANCE_LOW));
  if(Build.VERSION.SDK_INT>=29)startForeground(NOTICE,notification("Preparing bounded transfer",0,0),ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
  else startForeground(NOTICE,notification("Preparing bounded transfer",0,0));
  boolean accepted=coordinator.claimService(owner,new OsUpdateCoordinator.Reporter(){
   public void progress(String text,long bytes,long total){main.post(()->{if(!completed&&generation.owns(owner,ownerStartId))notifications.notify(NOTICE,notification(text,bytes,total));});}
   public void finished(){main.post(()->{
    if(!generation.retire(owner,ownerStartId))return;
    completed=true;stopForeground(STOP_FOREGROUND_REMOVE);stopSelf(generation.latestStartId());
   });}
  });
  if(!accepted){completed=true;stopForeground(STOP_FOREGROUND_REMOVE);stopSelf(generation.latestStartId());}
  return START_NOT_STICKY;
 }
 @Override public void onTimeout(int startId,int fgsType){OsUpdateCoordinator.get(this).cancelDownload(job);stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}
 @Override public void onDestroy(){if(!completed)OsUpdateCoordinator.get(this).cancelDownload(job);main.removeCallbacksAndMessages(null);super.onDestroy();}
}
