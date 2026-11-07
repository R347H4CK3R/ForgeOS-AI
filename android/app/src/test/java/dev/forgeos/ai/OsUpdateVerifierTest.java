package dev.forgeos.ai;
import org.junit.Test;
import static org.junit.Assert.*;
import java.security.*;
import java.io.*;
import java.util.zip.*;

public final class OsUpdateVerifierTest {
 private OsUpdateVerifier.Metadata valid(){
  OsUpdateVerifier.Metadata m=new OsUpdateVerifier.Metadata();m.schema=1;m.repository=OsUpdateVerifier.REPOSITORY;m.channel="experimental";m.sequence=123;m.version="0.3.0-os.aaaaaaaaaaaa.123";m.commit="a".repeat(40);m.api=35;m.abi="arm64-v8a";m.kind="raw-gsi";m.installMethod="stage-only";m.patch="2025-03-05";m.artifactId=456;m.name=OsUpdateVerifier.IMAGE;m.compressedBytes=100;m.archiveBytes=200;m.archiveSha="b".repeat(64);m.compressedSha="c".repeat(64);m.rawSha="d".repeat(64);return m;
 }
 private void accept(OsUpdateVerifier.Metadata m,long floor,int api,String...abis){OsUpdateVerifier.validate(m,123,floor,api,abis);}
 @Test public void acceptsOnlyCompatibleNewStagingImage(){accept(valid(),122,35,"arm64-v8a");}
 @Test public void rejectsInvalidArchiveBounds(){OsUpdateVerifier.Metadata m=valid();m.archiveBytes=0;assertThrows(SecurityException.class,()->accept(m,0,35,"arm64-v8a"));m.archiveBytes=m.compressedBytes-1;assertThrows(SecurityException.class,()->accept(m,0,35,"arm64-v8a"));m.archiveBytes=m.compressedBytes+1024L*1024+1;assertThrows(SecurityException.class,()->accept(m,0,35,"arm64-v8a"));m.archiveBytes=OsUpdateVerifier.MAX_ARCHIVE+1;assertThrows(SecurityException.class,()->accept(m,0,35,"arm64-v8a"));}
 @Test public void rejectsWrongRepository(){OsUpdateVerifier.Metadata m=valid();m.repository="attacker/repo";assertThrows(SecurityException.class,()->accept(m,0,35,"arm64-v8a"));}
 @Test public void rejectsWrongAbiAndApi(){assertThrows(SecurityException.class,()->accept(valid(),0,35,"x86_64"));assertThrows(SecurityException.class,()->accept(valid(),0,34,"arm64-v8a"));OsUpdateVerifier.Metadata m=valid();m.abi="x86_64";assertThrows(SecurityException.class,()->accept(m,0,35,"arm64-v8a"));}
 @Test public void rejectsReplayAndRollback(){assertThrows(SecurityException.class,()->accept(valid(),123,35,"arm64-v8a"));assertThrows(SecurityException.class,()->accept(valid(),124,35,"arm64-v8a"));assertThrows(SecurityException.class,()->OsUpdateVerifier.validate(valid(),124,0,35,new String[]{"arm64-v8a"}));}
 @Test public void rejectsUnsupportedInstallKind(){OsUpdateVerifier.Metadata m=valid();m.installMethod="ota";assertThrows(SecurityException.class,()->accept(m,0,35,"arm64-v8a"));m.installMethod="stage-only";m.kind="payload";assertThrows(SecurityException.class,()->accept(m,0,35,"arm64-v8a"));}
 @Test public void rejectsTamperedSignature()throws Exception{
  KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");generator.initialize(3072);KeyPair pair=generator.generateKeyPair();
  byte[] json="{\"schema\":1}".getBytes(java.nio.charset.StandardCharsets.UTF_8);Signature signer=Signature.getInstance("SHA256withRSA");signer.initSign(pair.getPrivate());signer.update(json);byte[] sig=signer.sign();
  OsUpdateVerifier.signature(json,sig,pair.getPublic());json[2]^=1;assertThrows(SignatureException.class,()->{try{OsUpdateVerifier.signature(json,sig,pair.getPublic());}catch(SecurityException e){throw new SignatureException(e);}});
 }
 @Test public void rejectsUnknownZipEntry()throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(out)){z.putNextEntry(new ZipEntry("../update.json"));z.write(1);z.closeEntry();}assertThrows(IOException.class,()->OsUpdateVerifier.metadataZip(new ByteArrayInputStream(out.toByteArray())));}
 @Test public void rejectsOversizedMetadata() {assertThrows(IOException.class,()->OsUpdateVerifier.bounded(new ByteArrayInputStream(new byte[16385]),16384));}
}
