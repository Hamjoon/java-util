package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.file.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_fastSHA512_Test {

    /**
     * Temporary directory provided by JUnit5 for file creation
     */
    @TempDir
    static Path tempDir;

    /**
     * Supplies the parameters for {@link #testFastSHA512EdgeCases}.
     */
    static Stream<Arguments> edgeCaseProvider() throws Exception {
        // 1. Empty file
        Path emptyPath = Files.createFile(tempDir.resolve("emptyFile.txt"));
        File emptyFile = emptyPath.toFile();
        String emptyHash = sha512Hex(new byte[0]);
        // 2. Large file (~12 MiB) – deterministic content
        Path largePath = tempDir.resolve("largeFile.bin");
        // 16 bytes
        byte[] chunk = "0123456789ABCDEF".getBytes();
        try (SeekableByteChannel channel = Files.newByteChannel(largePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            // Write 12 MiB = 12 * 1024 * 1024 bytes
            long bytesToWrite = 12L * 1024 * 1024;
            long written = 0;
            while (written < bytesToWrite) {
                channel.write(ByteBuffer.wrap(chunk));
                written += chunk.length;
            }
        }
        File largeFile = largePath.toFile();
        byte[] largeContent = Files.readAllBytes(largePath);
        String largeHash = sha512Hex(largeContent);
        // 3. Read‑protected file – make it unreadable
        Path protectedPath = Files.createFile(tempDir.resolve("readProtectedFile.txt"));
        Files.write(protectedPath, "secret".getBytes());
        File protectedFile = protectedPath.toFile();
        // Attempt to remove read permission; works on most platforms
        protectedFile.setReadable(false, false);
        // 4. Null argument – we expect a NullPointerException to be thrown
        File nullFile = null;
        return Stream.of(Arguments.of("Empty file", emptyFile, emptyHash, null), Arguments.of("Large file (~12 MiB)", largeFile, largeHash, null), Arguments.of("Read‑protected file", protectedFile, null, null), Arguments.of("Null file argument", nullFile, null, NullPointerException.class));
    }

    /**
     * Helper that computes the SHA‑512 hash of the supplied byte array
     * and returns it as a lower‑case hex string.
     */
    private static String sha512Hex(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] digest = md.digest(data);
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Verify that {@code fastSHA512(File)} returns the correct SHA‑512 hash
     * for a regular file.
     */
    @Test
    void testFastSHA512ReturnsCorrectHash() throws IOException, NoSuchAlgorithmException {
        // GIVEN: a temporary file with known content
        byte[] content = "The quick brown fox jumps over the lazy dog".getBytes();
        Path filePath = tempDir.resolve("testFile.txt");
        Files.write(filePath, content);
        File file = filePath.toFile();
        // AND: the expected hash computed directly via MessageDigest
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] digest = md.digest(content);
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        String expectedHash = sb.toString();
        // WHEN: the utility method is invoked
        String actualHash = EncryptionUtilities.fastSHA512(file);
        // THEN: the returned hash matches the expected value
        assertNotNull(actualHash, "Hash should not be null for a readable file");
        assertEquals(expectedHash, actualHash, "SHA‑512 hash does not match expected value");
    }

    /**
     * Verify that {@code fastSHA512(File)} returns {@code null}
     * when the file cannot be opened (e.g., does not exist).
     */
    @Test
    void testFastSHA512ReturnsNullOnIOException() {
        // GIVEN: a File reference that points to a non‑existent location
        File nonExistentFile = new File(tempDir.toFile(), "does_not_exist.bin");
        // WHEN: the utility method is invoked
        String result = EncryptionUtilities.fastSHA512(nonExistentFile);
        // THEN: the method should swallow the IOException and return null
        assertNull(result, "Result should be null when an IOException occurs");
    }

    /**
     * Parameterised tests covering edge‑case files:
     * <ul>
     *   <li>empty file – known constant SHA‑512 digest</li>
     *   <li>large file – verifies streaming via FileChannel</li>
     *   <li>read‑protected file – should result in {@code null}</li>
     * </ul>
     */
    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("edgeCaseProvider")
    void testFastSHA512EdgeCases(String description, File file, String expectedHash, Class<? extends Throwable> expectedException) throws Exception {
        if (expectedException != null) {
            // Expect an exception (e.g., NullPointerException)
            assertThrows(expectedException, () -> EncryptionUtilities.fastSHA512(file), "Expected exception for case: " + description);
        } else {
            // Normal execution – method should return a hash (or null)
            String actual = EncryptionUtilities.fastSHA512(file);
            if (expectedHash == null) {
                assertNull(actual, "Expected null for case: " + description);
            } else {
                assertNotNull(actual, "Hash should not be null for case: " + description);
                assertEquals(expectedHash, actual, "Hash mismatch for case: " + description);
            }
        }
    }
}
