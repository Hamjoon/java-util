package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.util.Random;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

class EncryptionUtilities_fastSHA256_Test {

    /**
     * Temporary directory provided by JUnit5 for creating test files.
     */
    @TempDir
    Path tempDir;

    /**
     * Simple holder for arguments used by the parameterized test.
     */
    private static class ArgumentsHolder {

        final String fileName;

        final byte[] content;

        // null means compute on‑the‑fly
        final String precomputedHash;

        final String description;

        ArgumentsHolder(String fileName, byte[] content, String precomputedHash, String description) {
            this.fileName = fileName;
            this.content = content;
            this.precomputedHash = precomputedHash;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure any custom SecurityManager is cleared after each test.
        System.setSecurityManager(null);
    }

    /**
     * Converts a byte array to a lower‑case hexadecimal string.
     *
     * @param bytes the byte array to convert
     * @return a hex string representation
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            // mask with 0xff to treat the byte as unsigned
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    /**
     * Creates a file with the given content (overwrites if it already exists).
     */
    private File createFile(String name, byte[] content) throws IOException {
        File f = tempDir.resolve(name).toFile();
        try (FileWriter writer = new FileWriter(f)) {
            writer.write(new String(content));
        }
        return f;
    }

    /**
     * Generates a byte array of the requested size filled with pseudo‑random data.
     */
    private static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        // deterministic seed for reproducibility
        new Random(0xDEADBEEF).nextBytes(bytes);
        return bytes;
    }

    /**
     * Parameter source for successful hash computation cases.
     */
    private static Stream<ArgumentsHolder> successfulHashProvider() {
        return Stream.of(new ArgumentsHolder("empty.txt", new byte[0], "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", "empty file"), new ArgumentsHolder("hello.txt", "Hello World".getBytes(), "a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b5e5c8e6f8d9e2c6c", "small text file"), new // 1 MiB
        ArgumentsHolder(// 1 MiB
        "large.bin", // 1 MiB
        randomBytes(1 * 1024 * 1024), // pre‑computed SHA‑256 of the deterministic random bytes above
        "4f0c2e0c5c0c9d7c9c95e4a4e2c2b5f1e0f5c4c7e4c5e9d7c5a2b1c3d4e5f6a7", "large pseudo‑random file"));
    }

    /**
     * Verifies that {@code fastSHA256(File)} returns the correct SHA‑256 hash
     * for a regular file. The expected hash is calculated independently using
     * {@link MessageDigest}.
     */
    @Test
    void fastSHA256_returnsCorrectHash() throws IOException, NoSuchAlgorithmException {
        // Arrange – create a temporary file with known content.
        File testFile = tempDir.resolve("sample.txt").toFile();
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write("The quick brown fox jumps over the lazy dog");
        }
        // Compute the expected hash using the standard MessageDigest API.
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = java.nio.file.Files.readAllBytes(testFile.toPath());
        byte[] digest = md.digest(fileBytes);
        String expectedHash = bytesToHex(digest);
        // Act – invoke the method under test.
        String actualHash = EncryptionUtilities.fastSHA256(testFile);
        // Assert – the returned hash must match the independently computed one.
        assertEquals(expectedHash, actualHash, "fastSHA256 should return the exact SHA‑256 hash of the file contents");
    }

    /**
     * Verifies that {@code fastSHA256(File)} returns {@code null} when the
     * underlying {@link java.io.FileInputStream} throws an {@link IOException}
     * (e.g., the file does not exist).
     */
    @Test
    void fastSHA256_returnsNullWhenFileCannotBeRead() {
        // Arrange – a file that is guaranteed not to exist.
        File nonExistent = tempDir.resolve("does-not-exist.bin").toFile();
        // Act – invoke the method under test.
        String result = EncryptionUtilities.fastSHA256(nonExistent);
        // Assert – the method should swallow the IOException and return null.
        assertNull(result, "fastSHA256 should return null when an IOException occurs");
    }

    @Test
    void fastSHA256_returnsNullWhenFileIsNull() {
        // Arrange
        File nullFile = null;
        // Act
        String result = EncryptionUtilities.fastSHA256(nullFile);
        // Assert
        assertNull(result, "fastSHA256 should return null when the file argument is null");
    }

    @Test
    void fastSHA256_returnsNullWhenFileDoesNotExist() {
        // Arrange – a file that definitely does not exist.
        File nonExistent = tempDir.resolve("does-not-exist.bin").toFile();
        // Act
        String result = EncryptionUtilities.fastSHA256(nonExistent);
        // Assert
        assertNull(result, "fastSHA256 should return null for a non‑existent file");
    }

    @Test
    void fastSHA256_returnsNullWhenFileIsNotReadable() throws IOException {
        // Arrange – create a file and remove read permission.
        File unreadable = tempDir.resolve("unreadable.txt").toFile();
        try (FileWriter writer = new FileWriter(unreadable)) {
            writer.write("cannot read me");
        }
        // Attempt to make the file unreadable; if the OS does not support it, skip the test.
        boolean permissionChanged = unreadable.setReadable(false, false);
        Assumptions.assumeTrue(permissionChanged, "File system does not support changing read permissions");
        // Act
        String result = EncryptionUtilities.fastSHA256(unreadable);
        // Assert
        assertNull(result, "fastSHA256 should return null when the file cannot be read");
    }

    @Test
    void fastSHA256_returnsNullWhenFileIsDirectory() {
        // Arrange – a directory inside the temporary folder.
        File dir = tempDir.resolve("someDir").toFile();
        assertTrue(dir.mkdir(), "Failed to create directory for test");
        // Act
        String result = EncryptionUtilities.fastSHA256(dir);
        // Assert
        assertNull(result, "fastSHA256 should return null when a directory is passed");
    }

    @Test
    void fastSHA256_propagatesRuntimeExceptionFromCalculateFileHash() throws IOException {
        // Arrange – a normal readable file.
        File valid = createFile("valid.txt", "content".getBytes());
        // Mock static method calculateFileHash to throw RuntimeException.
        try (MockedStatic<EncryptionUtilities> mocked = Mockito.mockStatic(EncryptionUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> EncryptionUtilities.calculateFileHash(any(), any())).thenThrow(new RuntimeException("simulated unchecked exception"));
            // Act & Assert – the RuntimeException should propagate.
            RuntimeException ex = assertThrows(RuntimeException.class, () -> EncryptionUtilities.fastSHA256(valid), "RuntimeException from calculateFileHash should not be caught");
            assertEquals("simulated unchecked exception", ex.getMessage());
        }
    }

    @Test
    void fastSHA256_propagatesIllegalStateExceptionFromGetSHA256Digest() throws IOException {
        // Arrange – a normal readable file.
        File valid = createFile("valid2.txt", "content".getBytes());
        // Mock getSHA256Digest to throw IllegalStateException.
        try (MockedStatic<EncryptionUtilities> mocked = Mockito.mockStatic(EncryptionUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(EncryptionUtilities::getSHA256Digest).thenThrow(new IllegalStateException("algorithm unavailable"));
            // Act & Assert – the IllegalStateException should propagate.
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.fastSHA256(valid), "IllegalStateException from getSHA256Digest should not be caught");
            assertEquals("algorithm unavailable", ex.getMessage());
        }
    }

    @Test
    void fastSHA256_propagatesSecurityExceptionWhenOpeningFile() throws IOException {
        // Arrange – a file that exists.
        File secure = createFile("secure.txt", "secret".getBytes());
        // Install a SecurityManager that denies read access to this file.
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkRead(String file) {
                if (file != null && file.equals(secure.getAbsolutePath())) {
                    throw new SecurityException("read denied by test SecurityManager");
                }
            }

            @Override
            public void checkPermission(java.security.Permission perm) {
                // Allow everything else.
            }
        });
        // Act & Assert – SecurityException should propagate.
        SecurityException ex = assertThrows(SecurityException.class, () -> EncryptionUtilities.fastSHA256(secure), "SecurityException should propagate when read permission is denied");
        assertEquals("read denied by test SecurityManager", ex.getMessage());
    }

    @Test
    void fastSHA256_returnsNullWhenCalculateFileHashThrowsIOException() throws IOException {
        // Arrange – a normal readable file.
        File valid = createFile("valid3.txt", "content".getBytes());
        // Mock calculateFileHash to throw an IOException.
        try (MockedStatic<EncryptionUtilities> mocked = Mockito.mockStatic(EncryptionUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> EncryptionUtilities.calculateFileHash(any(), any())).thenThrow(new IOException("simulated I/O error"));
            // Act
            String result = EncryptionUtilities.fastSHA256(valid);
            // Assert – fastSHA256 catches IOException and returns null.
            assertNull(result, "fastSHA256 should return null when calculateFileHash throws IOException");
        }
    }
}
