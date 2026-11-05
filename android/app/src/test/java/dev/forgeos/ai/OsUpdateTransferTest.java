package dev.forgeos.ai;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

public final class OsUpdateTransferTest {
 @Test public void cancellationCommandAdvancesServiceStopId(){OsUpdateServiceGeneration owner=new OsUpdateServiceGeneration();owner.noteStart(1);owner.accept("download",1);owner.noteStart(2);assertTrue(owner.retire("download",1));assertEquals(2,owner.latestStartId());}
 @Test public void delayedServiceCompletionCannotRetireNewDownload(){OsUpdateServiceGeneration owner=new OsUpdateServiceGeneration();owner.accept("old",1);owner.accept("new",2);assertFalse(owner.retire("old",1));assertTrue(owner.owns("new",2));assertFalse(owner.retire("new",1));assertTrue(owner.retire("new",2));assertFalse(owner.owns("new",2));}
 @Test public void operationGateRejectsOverlapAndStaleCompletion(){
  OsUpdateOperationGate g=new OsUpdateOperationGate();String first=g.acquire();assertNotNull(first);assertNull(g.acquire());
  assertFalse(g.release("stale"));assertTrue(g.owns(first));assertTrue(g.release(first));
  String next=g.acquire();assertNotEquals(first,next);assertFalse(g.release(first));assertTrue(g.owns(next));
 }
 @Test public void exactBoundRejectsLongAndShortArchivesAndDeletesPartial()throws Exception{
  Path dir=Files.createTempDirectory("os-transfer");File out=dir.resolve("new.zip").toFile();
  try{
   assertThrows(IOException.class,()->OsUpdateVerifier.copyExact(new ByteArrayInputStream(new byte[5]),out,4,b->{}));assertFalse(out.exists());
   assertThrows(IOException.class,()->OsUpdateVerifier.copyExact(new ByteArrayInputStream(new byte[3]),out,4,b->{}));assertFalse(out.exists());
   OsUpdateVerifier.copyExact(new ByteArrayInputStream(new byte[4]),out,4,b->{});assertEquals(4,out.length());
  }finally{out.delete();Files.delete(dir);}
 }
 @Test public void cancelDeletesOwnPartialButNeverReplacesExistingOutput()throws Exception{
  Path dir=Files.createTempDirectory("os-cancel");File out=dir.resolve("new.zip").toFile();
  try{
   assertThrows(InterruptedIOException.class,()->OsUpdateVerifier.copyExact(new ByteArrayInputStream(new byte[4]),out,4,b->{if(b>0)throw new InterruptedIOException();}));assertFalse(out.exists());
   Files.write(out.toPath(),new byte[]{9});
   assertThrows(IOException.class,()->OsUpdateVerifier.copyExact(new ByteArrayInputStream(new byte[4]),out,4,b->{}));assertArrayEquals(new byte[]{9},Files.readAllBytes(out.toPath()));
  }finally{out.delete();Files.delete(dir);}
 }
 @Test public void finalHashCancellationDeletesFullExtractedImage()throws Exception{
  Path dir=Files.createTempDirectory("os-hash-cancel");File archive=dir.resolve("a.zip").toFile(),out=dir.resolve("image.xz").toFile();
  byte[] image={1,2,3,4};
  try{
   try(ZipOutputStream z=new ZipOutputStream(new FileOutputStream(archive))){z.putNextEntry(new ZipEntry(OsUpdateVerifier.IMAGE));z.write(image);z.closeEntry();}
   OsUpdateVerifier.Metadata m=new OsUpdateVerifier.Metadata();m.name=OsUpdateVerifier.IMAGE;m.archiveBytes=archive.length();m.archiveSha=OsUpdateVerifier.hash(archive);m.compressedBytes=image.length;
   File expected=dir.resolve("expected").toFile();Files.write(expected.toPath(),image);m.compressedSha=OsUpdateVerifier.hash(expected);expected.delete();
   final int[] imagePasses={0};
   assertThrows(InterruptedIOException.class,()->OsUpdateVerifier.extract(archive,out,m,b->{if(b==image.length&&++imagePasses[0]==2)throw new InterruptedIOException("cancel final hash");}));
   assertFalse(out.exists());
  }finally{archive.delete();out.delete();Files.delete(dir);}
 }
 @Test public void corruptImageIsTerminalAndRemovesOutput()throws Exception{
  Path dir=Files.createTempDirectory("os-corrupt");File archive=dir.resolve("a.zip").toFile(),out=dir.resolve("image.xz").toFile();
  try{
   try(ZipOutputStream z=new ZipOutputStream(new FileOutputStream(archive))){z.putNextEntry(new ZipEntry(OsUpdateVerifier.IMAGE));z.write(new byte[]{1});z.closeEntry();}
   OsUpdateVerifier.Metadata m=new OsUpdateVerifier.Metadata();m.name=OsUpdateVerifier.IMAGE;m.archiveBytes=archive.length();m.archiveSha=OsUpdateVerifier.hash(archive);m.compressedBytes=1;m.compressedSha="0".repeat(64);
   assertThrows(SecurityException.class,()->OsUpdateVerifier.extract(archive,out,m));assertFalse(out.exists());
  }finally{archive.delete();out.delete();Files.delete(dir);}
 }
}
