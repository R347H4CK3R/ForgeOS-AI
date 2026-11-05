package dev.forgeos.ai;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public final class ModelIntegrityTest {
    private static final String ABC_SHA =
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private File fixture(String text) throws Exception {
        File file = temporary.newFile();
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
        return file;
    }

    @Test public void acceptsKnownSha256AndExactLength() throws Exception {
        assertTrue(ModelIntegrity.verify(fixture("abc"), ABC_SHA, 3));
    }

    @Test public void rejectsSameLengthCorruption() throws Exception {
        assertFalse(ModelIntegrity.verify(fixture("abd"), ABC_SHA, 3));
    }

    @Test public void rejectsFileLongerThanExpected() throws Exception {
        assertFalse(ModelIntegrity.verify(fixture("abc"), ABC_SHA, 2));
    }

    @Test public void rejectsFileShorterThanExpected() throws Exception {
        assertFalse(ModelIntegrity.verify(fixture("abc"), ABC_SHA, 4));
    }

    @Test public void streamsAcrossManyBuffers() throws Exception {
        File file = temporary.newFile();
        byte[] block = new byte[1000];
        java.util.Arrays.fill(block, (byte) 'a');
        try (FileOutputStream out = new FileOutputStream(file)) {
            for (int i = 0; i < 1000; i++) out.write(block);
        }
        assertTrue(ModelIntegrity.verify(file,
                "cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0",
                1000000));
    }
}
