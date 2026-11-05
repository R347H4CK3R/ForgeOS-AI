package dev.forgeos.ai;

import java.io.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.zip.*;

/** Delivery verification only; does not establish AVB rollback protection or boot safety. */
public final class OsUpdateVerifier {
 public static final String REPOSITORY="R347H4CK3R/ForgeOS-AI";
 public static final String IMAGE="ForgeOS-Android15-ARM64-EXPERIMENTAL-DO-NOT-FLASH.img.xz";
 public static final long MAX_IMAGE=2L*1024*1024*1024;
 public static final long MAX_ARCHIVE=MAX_IMAGE+64L*1024*1024;
 private OsUpdateVerifier(){}
 public static final class Metadata {
  public long sequence,artifactId,compressedBytes,archiveBytes;
  public int schema,api;
  public boolean bootVerified;
  public String repository,channel,abi,kind,installMethod,version,commit,patch,name,archiveSha,compressedSha,rawSha;
 }
 public static PublicKey publicKey(String pem)throws Exception {
  String body=pem.replace("-----BEGIN PUBLIC KEY-----","").replace("-----END PUBLIC KEY-----","").replaceAll("\\s","");
  PublicKey key=KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(body)));
  if(! (key instanceof java.security.interfaces.RSAPublicKey) || ((java.security.interfaces.RSAPublicKey)key).getModulus().bitLength()!=3072)throw new SecurityException("Expected RSA3072 key");
  return key;
 }
 public static void signature(byte[] json,byte[] signature,PublicKey key)throws Exception {
  if(json.length==0||json.length>16384||signature.length!=384)throw new SecurityException("Invalid signed metadata size");
  Signature verifier=Signature.getInstance("SHA256withRSA");verifier.initVerify(key);verifier.update(json);
  if(!verifier.verify(signature))throw new SecurityException("Update signature failed");
 }
 public static void validate(Metadata m,long selectedRun,long floor,int deviceApi,String[] abis){
  boolean arm=false;for(String abi:abis)if("arm64-v8a".equals(abi))arm=true;
  if(m.schema!=1||!REPOSITORY.equals(m.repository)||!"experimental".equals(m.channel))fail("Wrong update repository/channel/schema");
  if(m.sequence<=floor||m.sequence!=selectedRun||m.sequence<=0)fail("Stale or replayed update");
  if(m.api!=35||deviceApi!=35||!"arm64-v8a".equals(m.abi)||!arm)fail("Update requires Android API35 / ARM64");
  if(!"raw-gsi".equals(m.kind)||!"stage-only".equals(m.installMethod)||m.bootVerified)fail("Unsupported update installation kind");
  if(m.artifactId<=0||m.compressedBytes<1||m.compressedBytes>MAX_IMAGE||m.archiveBytes<m.compressedBytes||m.archiveBytes>m.compressedBytes+1024L*1024||m.archiveBytes>MAX_ARCHIVE||!IMAGE.equals(m.name))fail("Invalid image/archive size/name");
  if(m.commit==null||!m.commit.matches("[0-9a-f]{40}")||m.version==null||!m.version.equals("0.3.0-os."+m.commit.substring(0,12)+"."+m.sequence))fail("Invalid version/commit");
  if(!hex(m.archiveSha)||!hex(m.compressedSha)||!hex(m.rawSha)||m.patch==null||!m.patch.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))fail("Invalid digest/patch metadata");
 }
 private static boolean hex(String s){return s!=null&&s.matches("[0-9a-f]{64}");}
 private static void fail(String s){throw new SecurityException(s);}
 public static byte[] bounded(InputStream in,int limit)throws IOException {
  ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;
  while((n=in.read(b))!=-1){if(out.size()+n>limit)throw new IOException("Response exceeds size limit");out.write(b,0,n);}return out.toByteArray();
 }
 public static byte[][] metadataZip(InputStream in)throws IOException {
  byte[] archive=bounded(in,65536),json=null,sig=null;
  try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(archive))){
   ZipEntry e;while((e=zip.getNextEntry())!=null){
    if(e.isDirectory())throw new IOException("Unexpected metadata entry");
    if("update.json".equals(e.getName())){if(json!=null)throw new IOException("Duplicate metadata");json=bounded(zip,16384);}
    else if("update.sig".equals(e.getName())){if(sig!=null)throw new IOException("Duplicate signature");sig=bounded(zip,512);}
    else throw new IOException("Unexpected metadata entry");
   }
  }
  if(json==null||sig==null)throw new IOException("Missing signed metadata");return new byte[][]{json,sig};
 }
 public static String hash(File file)throws Exception {
  MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();md.update(b,0,n);}}
  StringBuilder s=new StringBuilder();for(byte b:md.digest())s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();
 }
 public interface Monitor {void check(long bytes)throws IOException;}
 public static void copyExact(InputStream in,File output,long expected,Monitor monitor)throws IOException {
  if(expected<1||expected>MAX_ARCHIVE)throw new IOException("Invalid transfer bound");
  if(!output.createNewFile())throw new IOException("Output already exists");
  boolean success=false;
  try{
   try(OutputStream out=new FileOutputStream(output)){
    byte[] b=new byte[65536];long total=0;while(total<expected){
     monitor.check(total);int n=in.read(b,0,(int)Math.min(b.length,expected-total));
     if(n<0)throw new IOException("Short archive");if(n==0)continue;
     monitor.check(total+n);out.write(b,0,n);total+=n;
    }
    monitor.check(total);if(in.read()!=-1)throw new IOException("Archive exceeds signed byte bound");monitor.check(total);
   }
   success=true;
  }finally{if(!success)output.delete();}
 }
 public static void extract(File archive,File output,Metadata m)throws Exception {
  extract(archive,output,m,bytes->{if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();});
 }
 public static void extract(File archive,File output,Metadata m,Monitor monitor)throws Exception {
  boolean created=false,success=false;
  try{
   monitor.check(0);
   if(archive.length()!=m.archiveBytes||!hash(archive,monitor).equals(m.archiveSha))throw new SecurityException("Archive size/SHA256 mismatch");
   boolean found=false;
   try(ZipInputStream zip=new ZipInputStream(new FileInputStream(archive))){
    ZipEntry e;while((e=zip.getNextEntry())!=null){
     if(!m.name.equals(e.getName())||e.isDirectory()||found)throw new IOException("Unexpected or duplicate image entry");
     if(!output.createNewFile())throw new IOException("Output already exists");created=true;
     found=true;long size=0;try(OutputStream out=new FileOutputStream(output)){
      byte[] b=new byte[65536];int n;while((n=zip.read(b))!=-1){size+=n;monitor.check(size);if(size>m.compressedBytes)throw new IOException("Image size mismatch");out.write(b,0,n);}
     }
     if(size!=m.compressedBytes)throw new IOException("Image size mismatch");
    }
   }
   if(!found||!hash(output,monitor).equals(m.compressedSha))throw new SecurityException("Image SHA256 mismatch");
   monitor.check(m.compressedBytes);success=true;
  }finally{if(created&&!success)output.delete();}
 }
 public static String hash(File file,Monitor monitor)throws Exception {
  MessageDigest md=MessageDigest.getInstance("SHA-256");long total=0;
  try(InputStream in=new FileInputStream(file)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){total+=n;monitor.check(total);md.update(b,0,n);}}
  StringBuilder s=new StringBuilder();for(byte b:md.digest())s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();
 }
}
