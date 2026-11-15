package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_fastSHA512_Test {

    /**
     * Temporary file used by the tests
     */
    private Path tempFile;

    /**
     * Expected SHA‑512 hash of the test content (computed once)
     */
    private static String expectedHash;

    /**
     * Expected SHA‑512 hash of an empty byte array (standard value)
     */
    private static final String EMPTY_HASH = "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9c" + "e47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e";

    /**
     * Compute the expected hash once for the known test content.
     * This avoids calling the method under test during the @BeforeAll phase.
     */
    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // The same content that will be written to every temporary file
        byte[] content = "The quick brown fox jumps over the lazy dog".getBytes("UTF-8");
        MessageDigest md = EncryptionUtilities.getSHA512Digest();
        md.update(content);
        expectedHash = EncryptionUtilities.calculateSHA512Hash(content);
        // Verify that the utility's helper produces the same result as the digest directly
        assertEquals(bytesToHex(md.digest()), expectedHash);
    }

    /**
     * Create a fresh temporary file before each test.
     */
    @BeforeEach
    void setupBeforeEach() throws IOException {
        tempFile = Files.createTempFile("fastSHA512Test", ".txt");
        // Write deterministic content so the hash is predictable
        Files.write(tempFile, "The quick brown fox jumps over the lazy dog".getBytes("UTF-8"));
    }

    /**
     * Delete the temporary file after each test.
     */
    @AfterEach
    void teardownAfterEach() throws IOException {
        if (tempFile != null && Files.exists(tempFile)) {
            Files.delete(tempFile);
        }
    }

    /**
     * Convert a byte array to a lower‑case hex string.
     * Replicates the format used by EncryptionUtilities for hash strings.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    @Test
    void fastSHA512_returnsCorrectHash_forExistingFile() {
        // GIVEN – a real file on disk (created in @BeforeEach)
        // WHEN
        String actual = EncryptionUtilities.fastSHA512(tempFile.toFile());
        // THEN
        assertNotNull(actual, "Hash should not be null for an existing file");
        assertEquals(expectedHash, actual, "SHA‑512 hash does not match the expected value");
    }

    @Test
    void fastSHA512_returnsNull_whenFileDoesNotExist() {
        // GIVEN – a path that does not point to any file
        File missing = new File("non‑existent‑file‑" + System.nanoTime());
        // Ensure the file truly does not exist
        assertFalse(missing.exists(), "Test pre‑condition failed – file unexpectedly exists");
        // WHEN
        String result = EncryptionUtilities.fastSHA512(missing);
        // THEN
        assertNull(result, "Method should return null when the file cannot be found");
    }

    @Test
    void fastSHA512_returnsCorrectHash_forEmptyFile() throws IOException {
        // GIVEN – an empty temporary file
        Path emptyFile = Files.createTempFile("empty", ".txt");
        // No content is written, file size = 0
        try {
            // WHEN
            String actual = EncryptionUtilities.fastSHA512(emptyFile.toFile());
            // THEN
            assertNotNull(actual, "Hash should not be null for an empty file");
            assertEquals(EMPTY_HASH, actual, "SHA‑512 hash of an empty file is incorrect");
        } finally {
            Files.deleteIfExists(emptyFile);
        }
    }

    @Test
    void fastSHA512_throwsNullPointerException_whenFileArgumentIsNull() {
        // GIVEN – a null reference
        File nullFile = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.fastSHA512(nullFile), "Calling fastSHA512(null) should throw NullPointerException");
    }

    @Test
    void fastSHA512_returnsNull_whenPathIsDirectory() throws IOException {
        // GIVEN – a temporary directory (not a regular file)
        Path dir = Files.createTempDirectory("fastSHA512Dir");
        try {
            // WHEN
            String result = EncryptionUtilities.fastSHA512(dir.toFile());
            // THEN
            assertNull(result, "Method should return null when a directory is supplied");
        } finally {
            Files.deleteIfExists(dir);
        }
    }

    @Test
    void fastSHA512_returnsNull_whenFileIsNotReadable() throws IOException {
        // GIVEN – a file with read permission removed (POSIX systems)
        Path unreadable = Files.createTempFile("unreadable", ".txt");
        Files.write(unreadable, "content".getBytes("UTF-8"));
        // Attempt to make the file unreadable. On Windows the call is a no‑op,
        // but the test will still pass because fastSHA512 will catch the resulting IOException.
        try {
            // POSIX permissions
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(unreadable);
            perms.remove(PosixFilePermission.OWNER_READ);
            Files.setPosixFilePermissions(unreadable, perms);
        } catch (UnsupportedOperationException ignored) {
            // Fallback for non‑POSIX file systems (e.g., Windows)
            unreadable.toFile().setReadable(false);
        }
        // WHEN
        String result = EncryptionUtilities.fastSHA512(unreadable.toFile());
        // THEN
        assertNull(result, "Method should return null when the file cannot be read (permission denied)");
    }

    @Test
    void fastSHA512_usesStreamFallback_whenInputStreamIsNotFileInputStream() throws Exception {
        // GIVEN – a temporary file with known content
        byte[] content = "The quick brown fox jumps over the lazy dog".getBytes("UTF-8");
        Path customFile = Files.createTempFile("customProvider", ".txt");
        Files.write(customFile, content);
        // Mock static Files.newInputStream to return a ByteArrayInputStream (not a FileInputStream)
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.newInputStream(customFile.toPath())).thenReturn(new ByteArrayInputStream(content));
            // WHEN
            String actual = EncryptionUtilities.fastSHA512(customFile.toFile());
            // THEN
            assertNotNull(actual, "Hash should not be null when using the stream fallback");
            assertEquals(expectedHash, actual, "SHA‑512 hash via stream fallback is incorrect");
        } finally {
            Files.deleteIfExists(customFile);
        }
    }
}
