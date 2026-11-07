package dev.forgeos.ai;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Streaming verification; neither the filename nor download status establishes trust. */
public final class ModelIntegrity {
    private ModelIntegrity() {}

    public static boolean verify(File file, String expectedSha256, long expectedBytes)
            throws IOException {
        if (expectedBytes < 0 || expectedSha256 == null
                || !expectedSha256.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("Expected a SHA256 digest and nonnegative size");
        }
        if (!file.isFile() || file.length() != expectedBytes) return false;
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
        byte[] expected = new byte[32];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = (byte) Integer.parseInt(expectedSha256.substring(i * 2, i * 2 + 2), 16);
        }
        byte[] buffer = new byte[64 * 1024];
        long total = 0;
        try (FileInputStream input = new FileInputStream(file)) {
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedIOException("Verification cancelled");
                }
                if (count > expectedBytes - total) return false;
                total += count;
                digest.update(buffer, 0, count);
            }
        }
        return total == expectedBytes && MessageDigest.isEqual(expected, digest.digest());
    }
}
