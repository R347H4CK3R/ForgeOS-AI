package dev.forgeos.ai;

import android.content.*;
import android.net.*;
import android.os.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Base64;
import java.util.concurrent.*;

/**
 * One application-owned operation at a time. No Activity, token, or signed asset URL
 * is persisted. Process death requires an explicit new check/download.
 */
final class OsUpdateCoordinator {
 private static final String API="https://api.github.com/repos/"+OsUpdateVerifier.REPOSITORY;
 private static OsUpdateCoordinator instance;
 static synchronized OsUpdateCoordinator get(Context c){if(instance==null)instance=new OsUpdateCoordinator(c.getApplicationContext());return instance;}
 private final Context app;
 private final SharedPreferences prefs;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final OsUpdateOperationGate gate=new OsUpdateOperationGate();
 private volatile String message="Reading installed marker…",marker="Installed OS marker is being read.";
 private long installed;
 private Job current;
 private Candidate candidate;
 private final java.util.ArrayDeque<Uri> pendingExports=new java.util.ArrayDeque<>();
 interface Reporter {void progress(String text,long bytes,long total);void finished();}
 private static final class Candidate {
  final byte[] json,sig;final OsUpdateVerifier.Metadata metadata;
  Candidate(byte[] j,byte[] s,OsUpdateVerifier.Metadata m){json=j;sig=s;metadata=m;}
 }
 private static final class Job {
  final String id,kind;volatile boolean canceled;volatile HttpURLConnection connection;
  volatile String asset;Candidate candidate;Reporter reporter;boolean serviceClaimed;
  Job(String i,String k){id=i;kind=k;}
 }
 private OsUpdateCoordinator(Context c){
  app=c;prefs=app.getSharedPreferences("os-delivery",0);
  final String recoveredPhase=prefs.getString("phase","IDLE"),recoveredExport=prefs.getString("export_uri","");
  final java.util.Set<String> recoveredQueued=new java.util.HashSet<>(prefs.getStringSet("pending_exports",java.util.Collections.emptySet()));
  worker.execute(()->{
   try{
    File markerFile=new File("/system/etc/forgeos/build.json");
    marker="Stock / preview-only APK; no installed ForgeOS marker.";
    if(markerFile.isFile())try(InputStream in=new FileInputStream(markerFile)){
     JSONObject j=json(OsUpdateVerifier.bounded(in,16384));
     if(OsUpdateVerifier.REPOSITORY.equals(j.optString("repository"))&&j.optLong("sequence",0)>0){
      installed=j.getLong("sequence");marker="Installed marker: "+j.optString("version")+" / sequence "+installed;
     }else marker="Preview marker lacks delivery sequence; installed OS version unconfirmed.";
    }
   }catch(Exception ignored){marker="Installed marker unreadable; preview-only / installed version unconfirmed.";}
   cleanupOrphans();
   String phase=recoveredPhase;
   java.util.Set<String> queued=recoveredQueued;
   if(isUnfinished(phase)||!queued.isEmpty()){
    cleanupPersisted();
    String uri=recoveredExport;
    String result="Interrupted "+phase.toLowerCase(java.util.Locale.ROOT)+". No automatic retry. Check again to start a new download.";
    if(!uri.isEmpty())result+="\nA partial export may remain at: "+uri+"\nReview/delete that document before retrying.";
    if(!queued.isEmpty())result+="\nQueued export documents were not copied and may be empty: "+queued+"\nReview/delete those documents before retrying.";
    synchronized(this){java.util.Set<String> remaining=new java.util.HashSet<>(prefs.getStringSet("pending_exports",java.util.Collections.emptySet()));remaining.removeAll(queued);prefs.edit().putStringSet("pending_exports",remaining).apply();}
    terminal("INTERRUPTED",result);
   }else message=prefs.getString("message","Check the latest repository build to discover a signed candidate.");
  });
 }
 private static boolean isUnfinished(String p){return "CHECKING".equals(p)||"PREPARING".equals(p)||"DOWNLOADING".equals(p)||"VERIFYING".equals(p)||"EXPORTING".equals(p);}
 String snapshot(){return marker+"\nHighest staged sequence: "+prefs.getLong("highest",0)+"\n\n"+message+"\n\nStaging only · no OS installation.";}
 boolean hasVerified(){return prefs.contains("verified");}
 synchronized boolean busy(){return current!=null;}
 private synchronized Job reserve(String kind){
  String id=gate.acquire();if(id==null)return null;current=new Job(id,kind);return current;
 }
 private synchronized boolean owns(Job j){return current==j&&gate.owns(j.id);}
 private void persist(Job j,String phase,String text)throws IOException{
  if(!owns(j))throw new InterruptedIOException();
  message=text;
  if(!prefs.edit().putString("phase",phase).putString("operation",j.id).putString("message",text).commit())throw new IOException("Cannot persist operation state");
 }
 private void terminal(String phase,String text){
  message=text;prefs.edit().putString("phase",phase).putString("message",text).remove("operation").remove("job_zip").remove("job_xz").apply();
 }
 private void finish(Job j,String phase,String text){
  Reporter reporter;
  synchronized(this){
   if(!owns(j))return;reporter=j.reporter;j.asset=null;j.candidate=null;j.connection=null;
   terminal(phase,text);current=null;gate.release(j.id);
  }
  if(reporter!=null)reporter.finished();
  Uri queued; synchronized(this){queued=pendingExports.pollFirst();}
  if(queued!=null)export(queued);
 }
 private void fail(Job j,Exception e){
  cleanupPersisted();
  String text=j.canceled?"Download canceled; own partial files removed.":"Operation failed: "+safe(e)+"\nNo automatic retry. Check again before another download.";
  finish(j,j.canceled?"CANCELED":"FAILED",text);
 }
 private static String safe(Exception e){
  if(e instanceof HttpFailure||e instanceof SecurityException||e instanceof UserFailure)return e.getMessage();
  return e instanceof InterruptedIOException?"Interrupted.":e.getClass().getSimpleName()+"; check connectivity and storage.";
 }
 private static final class UserFailure extends IOException{UserFailure(String s){super(s);}}
 private static final class HttpFailure extends IOException{
  HttpFailure(int code){super(code==401||code==403?"GitHub authentication/access denied. Enter a token with Actions read access for this private repository.":"HTTP "+code);}
 }
 private void checkCanceled(Job j)throws InterruptedIOException{if(j.canceled||!owns(j)||Thread.currentThread().isInterrupted())throw new InterruptedIOException();}
 synchronized void cancelDownload(String id){
  if(current==null||!"DOWNLOAD".equals(current.kind)||(id!=null&&!current.id.equals(id)))return;
  current.canceled=true;HttpURLConnection c=current.connection;if(c!=null)c.disconnect();
 }
 private File dir()throws IOException{File d=new File(app.getFilesDir(),"os-updates");if(!d.isDirectory()&&!d.mkdirs())throw new IOException();return d;}
 private File own(String name)throws IOException{
  if(name==null||!name.matches("os-[0-9a-f-]+\\.(zip\\.part|xz\\.part|xz)"))throw new IOException("Invalid owned filename");
  return new File(dir(),name);
 }
 private void cleanupPersisted(){
  for(String key:new String[]{"job_zip","job_xz"})try{
   String name=prefs.getString(key,null);if(name!=null&&!name.equals(prefs.getString("verified",null)))own(name).delete();
  }catch(Exception ignored){}
 }
 private void cleanupOrphans(){
  try{
   String keep=prefs.getString("verified",null);File[] files=dir().listFiles();if(files==null)return;
   for(File file:files)if(file.getName().matches("os-[0-9a-f-]+\\.(zip\\.part|xz\\.part|xz)")&&!file.getName().equals(keep))file.delete();
  }catch(Exception ignored){}
 }
 private PublicKey key()throws Exception{
  try(InputStream in=app.getResources().openRawResource(R.raw.forgeos_update_public_key)){
   return OsUpdateVerifier.publicKey(new String(OsUpdateVerifier.bounded(in,4096),StandardCharsets.US_ASCII));
  }
 }
 private void validate(Candidate c,long floor)throws Exception{
  OsUpdateVerifier.signature(c.json,c.sig,key());
  OsUpdateVerifier.validate(c.metadata,c.metadata.sequence,floor,Build.VERSION.SDK_INT,Build.SUPPORTED_ABIS);
 }
 private static URL checked(String value)throws Exception{
  URL u=new URL(value);if(!"https".equals(u.getProtocol())||u.getUserInfo()!=null||u.getHost().isEmpty()||(u.getPort()!=-1&&u.getPort()!=443))throw new SecurityException("Unsafe artifact URL");return u;
 }
 private HttpURLConnection apiConnect(URL u,String credential,Job j)throws Exception{
  checkCanceled(j);HttpURLConnection c=(HttpURLConnection)u.openConnection();
  c.setInstanceFollowRedirects(false);c.setConnectTimeout(20000);c.setReadTimeout(30000);
  c.setRequestProperty("Accept","application/vnd.github+json");c.setRequestProperty("X-GitHub-Api-Version","2022-11-28");
  if("api.github.com".equalsIgnoreCase(u.getHost())&&!credential.isEmpty())c.setRequestProperty("Authorization","Bearer "+credential);
  j.connection=c;checkCanceled(j);return c;
 }
 private byte[] get(String endpoint,String credential,int limit,Job j)throws Exception{
  URL u=checked(endpoint);
  for(int hops=0;hops<=5;hops++){
   HttpURLConnection c=apiConnect(u,credential,j);
   try{
    int code=c.getResponseCode();
    if(code>=300&&code<400){if(hops==5)throw new SecurityException("Too many redirects");String loc=c.getHeaderField("Location");if(loc==null)throw new IOException();u=checked(new URL(u,loc).toString());continue;}
    if(code!=200)throw new HttpFailure(code);
    try(InputStream in=c.getInputStream()){return OsUpdateVerifier.bounded(in,limit);}
   }finally{c.disconnect();j.connection=null;}
  }throw new SecurityException("Too many redirects");
 }
 private String resolveAsset(String endpoint,String credential,Job j)throws Exception{
  URL u=checked(endpoint);
  for(int hops=0;hops<=5;hops++){
   if(!"api.github.com".equalsIgnoreCase(u.getHost()))return u.toString();
   HttpURLConnection c=apiConnect(u,credential,j);
   try{
    int code=c.getResponseCode();if(code<300||code>=400)throw new HttpFailure(code);
    if(hops==5)throw new SecurityException("Too many redirects");
    String loc=c.getHeaderField("Location");if(loc==null)throw new IOException();u=checked(new URL(u,loc).toString());
   }finally{c.disconnect();j.connection=null;}
  }throw new SecurityException("Too many redirects");
 }
 private static JSONObject json(byte[] b)throws Exception{return new JSONObject(new String(b,StandardCharsets.UTF_8));}
 static OsUpdateVerifier.Metadata parse(String raw)throws Exception{
  JSONObject j=new JSONObject(raw),im=j.getJSONObject("image");OsUpdateVerifier.Metadata m=new OsUpdateVerifier.Metadata();
  m.schema=j.getInt("schema");m.repository=j.getString("repository");m.channel=j.getString("channel");m.sequence=j.getLong("sequence");m.version=j.getString("version");m.commit=j.getString("source_commit");m.api=j.getInt("android_api");m.abi=j.getString("abi");m.kind=j.getString("kind");m.installMethod=j.getString("install_method");m.bootVerified=j.getBoolean("boot_verified");m.patch=j.getString("security_patch");
  m.artifactId=im.getLong("artifact_id");m.archiveBytes=im.getLong("archive_bytes");m.archiveSha=im.getString("archive_sha256");m.name=im.getString("name");m.compressedBytes=im.getLong("compressed_bytes");m.compressedSha=im.getString("compressed_sha256");m.rawSha=im.getString("raw_sha256");return m;
 }
 boolean check(String credential){
  final Job j=reserve("CHECK");if(j==null)return false;
  synchronized(this){candidate=null;}
  worker.execute(()->{
   try{
    persist(j,"CHECKING","Checking latest successful ForgeOS build…");
    JSONArray runs=json(get(API+"/actions/workflows/os-image.yml/runs?event=workflow_dispatch&status=success&per_page=10",credential,524288,j)).getJSONArray("workflow_runs");
    JSONObject latest=null;
    for(int i=0;i<runs.length();i++){
     JSONObject run=runs.getJSONObject(i);
     if("workflow_dispatch".equals(run.optString("event"))&&"success".equals(run.optString("conclusion"))&&"ForgeOS build".equals(run.optString("display_title"))&&(latest==null||run.getLong("id")>latest.getLong("id")))latest=run;
    }
    if(latest==null)throw new UserFailure("No successful ForgeOS build in the latest 10 successful manual runs.");
    long id=latest.getLong("id");
    JSONArray artifacts=json(get(API+"/actions/runs/"+id+"/artifacts?per_page=100",credential,262144,j)).getJSONArray("artifacts");
    JSONObject metadata=null;
    for(int a=0;a<artifacts.length();a++){
     JSONObject artifact=artifacts.getJSONObject(a);
     if("ForgeOS-update-metadata".equals(artifact.getString("name"))){
      if(metadata!=null)throw new SecurityException("Ambiguous metadata artifacts in latest build");
      metadata=artifact;
     }
    }
    if(metadata==null||metadata.getBoolean("expired"))throw new UserFailure("Latest ForgeOS build has no available signed metadata. Older builds will not be used.");
    byte[][] signed=OsUpdateVerifier.metadataZip(new ByteArrayInputStream(get(API+"/actions/artifacts/"+metadata.getLong("id")+"/zip",credential,65536,j)));
    OsUpdateVerifier.signature(signed[0],signed[1],key());
    OsUpdateVerifier.Metadata m=parse(new String(signed[0],StandardCharsets.UTF_8));
    OsUpdateVerifier.validate(m,id,Math.max(installed,prefs.getLong("highest",0)),Build.VERSION.SDK_INT,Build.SUPPORTED_ABIS);
    boolean imageFound=false;
    for(int a=0;a<artifacts.length();a++){JSONObject image=artifacts.getJSONObject(a);if(image.getLong("id")==m.artifactId&&!image.getBoolean("expired"))imageFound=true;}
    if(!imageFound)throw new SecurityException("Signed image is not available in the selected latest build");
    checkCanceled(j);synchronized(this){candidate=new Candidate(signed[0],signed[1],m);}
    finish(j,"READY","Signed candidate: "+m.version+"\nSecurity patch: "+m.patch+" (old; review before separate installation)\nArchive: "+m.archiveBytes+" bytes; image: "+m.compressedBytes+" bytes.\nRaw GSI / boot unverified. Ready for unmetered Wi-Fi staging.");
   }catch(Exception e){fail(j,e);}
  });return true;
 }
 boolean stage(String credential){
  final Job j;
  synchronized(this){if(candidate==null)return false;j=reserve("DOWNLOAD");if(j==null)return false;j.candidate=candidate;}
  worker.execute(()->{
   try{
    persist(j,"PREPARING","Preparing bounded download; requiring unmetered Wi-Fi…");
    validate(j.candidate,Math.max(installed,prefs.getLong("highest",0)));
    Network n=wifi();requireWifi(n);
    OsUpdateVerifier.Metadata m=j.candidate.metadata;
    long needed=m.archiveBytes+m.compressedBytes+64L*1024*1024;
    if(new StatFs(dir().getAbsolutePath()).getAvailableBytes()<needed)throw new UserFailure("Not enough free space for archive, verified image, and safety margin.");
    j.asset=resolveAsset(API+"/actions/artifacts/"+m.artifactId+"/zip",credential,j);checkCanceled(j);
    app.startForegroundService(new Intent(app,OsUpdateDownloadService.class).putExtra("job",j.id));
   }catch(Exception e){fail(j,e);}
  });return true;
 }
 synchronized boolean prepared(String id){return current!=null&&"DOWNLOAD".equals(current.kind)&&gate.owns(id)&&current.asset!=null&&!current.serviceClaimed;}
 synchronized boolean claimService(String id,Reporter reporter){
  if(current==null||!"DOWNLOAD".equals(current.kind)||!gate.owns(id)||current.asset==null||current.serviceClaimed)return false;
  Job j=current;j.serviceClaimed=true;j.reporter=reporter;
  worker.execute(()->download(j));return true;
 }
 private Network wifi()throws IOException{
  ConnectivityManager cm=(ConnectivityManager)app.getSystemService(Context.CONNECTIVITY_SERVICE);
  Network n=cm.getActiveNetwork();requireWifi(n);return n;
 }
 private void requireWifi(Network n)throws IOException{
  ConnectivityManager cm=(ConnectivityManager)app.getSystemService(Context.CONNECTIVITY_SERVICE);
  NetworkCapabilities c=n==null?null:cm.getNetworkCapabilities(n);
  if(c==null||!c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)||!c.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)||!c.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)||!c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))throw new UserFailure("Unmetered, validated Wi-Fi is required. Download stopped; retry explicitly on Wi-Fi.");
 }
 private void download(Job j){
  File zip=null,out=null;boolean committed=false;
  try{
   checkCanceled(j);Candidate selected=j.candidate;OsUpdateVerifier.Metadata m=selected.metadata;
   String zipName="os-"+j.id+".zip.part",outName="os-"+j.id+".xz.part";
   zip=own(zipName);out=own(outName);
   if(!prefs.edit().putString("job_zip",zipName).putString("job_xz",outName).commit())throw new IOException();
   persist(j,"DOWNLOADING","Downloading bounded archive over unmetered Wi-Fi…");
   final Network network=wifi();URL url=checked(j.asset);j.asset=null;
   for(int hops=0;;hops++){
    checkCanceled(j);requireWifi(network);
    HttpURLConnection c=(HttpURLConnection)network.openConnection(url);j.connection=c;checkCanceled(j);
    c.setInstanceFollowRedirects(false);c.setConnectTimeout(20000);c.setReadTimeout(30000);c.setRequestProperty("Accept-Encoding","identity");
    try{
     int code=c.getResponseCode();
     if(code>=300&&code<400){if(hops==5)throw new SecurityException("Too many asset redirects");String loc=c.getHeaderField("Location");if(loc==null)throw new IOException();url=checked(new URL(url,loc).toString());continue;}
     if(code!=200)throw new HttpFailure(code);
     long length=c.getContentLengthLong();if(length!=-1&&length!=m.archiveBytes)throw new SecurityException("Archive Content-Length differs from signed byte count");
     final long[] last={0};
     try(InputStream in=c.getInputStream()){
      OsUpdateVerifier.copyExact(in,zip,m.archiveBytes,bytes->{
       checkCanceled(j);requireWifi(network);
       long now=SystemClock.elapsedRealtime();
       if(now-last[0]>750||bytes==m.archiveBytes){last[0]=now;message="Downloading: "+bytes+" / "+m.archiveBytes+" bytes";j.reporter.progress("Staging OS image",bytes,m.archiveBytes);}
      });
     }break;
    }finally{c.disconnect();j.connection=null;}
   }
   checkCanceled(j);persist(j,"VERIFYING","Verifying archive and image. No automatic retry on failure.");
   j.reporter.progress("Verifying staged image",0,0);
   OsUpdateVerifier.extract(zip,out,m,bytes->checkCanceled(j));checkCanceled(j);
   File finalImage=own("os-"+j.id+".xz");
   if(finalImage.exists()||!out.renameTo(finalImage))throw new IOException();
   out=finalImage;
   String old=prefs.getString("verified",null);
   // Preserve both valid files if the durable record is ambiguous after failure.
   committed=true;
   OsUpdatePublication.Record replacement=new OsUpdatePublication.Record(finalImage.getName(),Base64.getEncoder().encodeToString(selected.json),Base64.getEncoder().encodeToString(selected.sig),m.sequence);
   if(!OsUpdatePublication.replace(new OsUpdatePublication.Store(){
    public OsUpdatePublication.Record read(){return new OsUpdatePublication.Record(prefs.getString("verified",null),prefs.getString("verified_json",null),prefs.getString("verified_sig",null),prefs.getLong("highest",0));}
    public boolean write(OsUpdatePublication.Record record){
     SharedPreferences.Editor editor=prefs.edit().putLong("highest",record.highest);
     restore(editor,"verified",record.file);restore(editor,"verified_json",record.json);restore(editor,"verified_sig",record.signature);
     if(record==replacement)editor.putString("phase","VERIFIED").putString("message","Verified image staged: "+m.version+"; export is available.").remove("job_zip").remove("job_xz");
     return editor.commit();
    }
   },replacement))throw new UserFailure("Could not persist verified image state. Prior staging record restored; valid files retained for recovery.");
   if(old!=null&&!old.equals(finalImage.getName()))try{own(old).delete();}catch(Exception ignored){}
   finish(j,"VERIFIED","Verified image staged: "+m.version+"\nArchive and compressed image SHA256 match signed metadata. Not decompressed; export is available.");
  }catch(Exception e){fail(j,e);}
  finally{if(zip!=null)zip.delete();if(!committed&&out!=null)out.delete();}
 }
 private static void restore(SharedPreferences.Editor editor,String key,String value){if(value==null)editor.remove(key);else editor.putString(key,value);}
 void export(Uri uri){
  final Job j;
  synchronized(this){
   java.util.Set<String> destinations=new java.util.HashSet<>(prefs.getStringSet("pending_exports",java.util.Collections.emptySet()));
   destinations.add(uri.toString());prefs.edit().putStringSet("pending_exports",destinations).apply();
   if(current!=null){pendingExports.addLast(uri);message+="\nExport destination queued until the current operation finishes: "+uri;return;}
   j=reserve("EXPORT");
  }
  worker.execute(()->{
   try{
    java.util.Set<String> destinations;
    synchronized(this){
     destinations=new java.util.HashSet<>(prefs.getStringSet("pending_exports",java.util.Collections.emptySet()));destinations.remove(uri.toString());
     message="Verifying staged image before export…";
     if(!prefs.edit().putString("export_uri",uri.toString()).putString("phase","EXPORTING").putString("operation",j.id).putString("message",message).putStringSet("pending_exports",destinations).commit())throw new IOException();
    }
    byte[] raw=Base64.getDecoder().decode(prefs.getString("verified_json","")),sig=Base64.getDecoder().decode(prefs.getString("verified_sig",""));
    OsUpdateVerifier.signature(raw,sig,key());OsUpdateVerifier.Metadata m=parse(new String(raw,StandardCharsets.UTF_8));
    OsUpdateVerifier.validate(m,m.sequence,0,Build.VERSION.SDK_INT,Build.SUPPORTED_ABIS);
    File file=own(prefs.getString("verified",null));
    if(file.length()!=m.compressedBytes||!OsUpdateVerifier.hash(file).equals(m.compressedSha))throw new SecurityException("Staged file changed");
    message="Exporting verified image. Rotation will not interrupt this copy.";
    try(InputStream in=new FileInputStream(file);OutputStream out=app.getContentResolver().openOutputStream(uri,"wt")){
     if(out==null)throw new IOException();byte[] b=new byte[65536];long total=0;int n;
     while((n=in.read(b))!=-1){total+=n;if(total>m.compressedBytes)throw new SecurityException("Staged file size changed");out.write(b,0,n);}
     if(total!=m.compressedBytes)throw new SecurityException("Staged file size changed");out.flush();
    }
    prefs.edit().remove("export_uri").apply();
    finish(j,"EXPORTED","Verified image exported. No OS installation was performed.");
   }catch(Exception e){
    finish(j,"EXPORT_FAILED","Export failed: "+safe(e)+"\nA partial document may remain at: "+uri+"\nReview/delete that document before retrying.");
   }
  });
 }
}
