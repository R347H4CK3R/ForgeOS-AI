package dev.forgeos.ai;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class OsUpdateSignatureTest {
 @Test public void pinnedProductionKeyAcceptsFixtureAndRejectsTampering()throws Exception{
  Context app=InstrumentationRegistry.getInstrumentation().getTargetContext();
  Context tests=InstrumentationRegistry.getInstrumentation().getContext();
  byte[] json,sig;String pem;
  try(InputStream in=app.getResources().openRawResource(R.raw.forgeos_update_public_key)){pem=new String(OsUpdateVerifier.bounded(in,4096),StandardCharsets.US_ASCII);}
  try(InputStream in=tests.getAssets().open("updates/valid-update.json")){json=OsUpdateVerifier.bounded(in,16384);}
  try(InputStream in=tests.getAssets().open("updates/valid-update.sig")){sig=OsUpdateVerifier.bounded(in,512);}
  PublicKey key=OsUpdateVerifier.publicKey(pem);
  OsUpdateVerifier.signature(json,sig,key);
  byte[] changed=json.clone();changed[changed.length/2]^=1;
  assertThrows(SecurityException.class,()->OsUpdateVerifier.signature(changed,sig,key));
  byte[] changedSig=sig.clone();changedSig[changedSig.length-1]^=1;
  assertThrows(SecurityException.class,()->OsUpdateVerifier.signature(json,changedSig,key));
 }
}
