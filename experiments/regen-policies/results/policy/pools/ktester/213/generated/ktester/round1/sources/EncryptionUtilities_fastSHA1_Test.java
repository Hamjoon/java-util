package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.channels.FileChannel;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_fastSHA1_Test {

    /**
     * Temporary directory provided by JUnit for file‑system tests.
     */
    @TempDir
    static Path tempDir;

    /**
     * A file that will contain known data for the happy‑path test.
     */
    private static File existingFile;

    /**
     * The exact SHA‑1 hash of the data written to {@link #existingFile}.
     */
    private static String expectedHash;

    @BeforeAll
    static void setupBeforeAll() throws IOException, NoSuchAlgorithmException {
        // Create a file with deterministic content (used by the original test case).
        existingFile = Files.createFile(tempDir.resolve("sample.txt")).toFile();
        byte[] content = "The quick brown fox jumps over the lazy dog".getBytes();
        Files.write(existingFile.toPath(), content);
        // Compute the expected SHA‑1 using the JDK directly.
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(content);
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        expectedHash = sb.toString();
    }

    /**
     * Verify that {@code fastSHA1(File)} returns the correct SHA‑1 hash
     * for a regular, readable file.
     */
    @Test
    void fastSHA1_returnsCorrectHashForExistingFile() {
        String actual = EncryptionUtilities.fastSHA1(existingFile);
        assertNotNull(actual, "Hash should not be null for an existing file");
        assertEquals(expectedHash, actual, "SHA‑1 hash does not match expected value");
    }

    /**
     * Verify that {@code fastSHA1(File)} returns {@code null}
     * when the supplied file does not exist (triggers {@code NoSuchFileException}).
     */
    @Test
    void fastSHA1_returnsNullWhenFileDoesNotExist() {
        File missing = tempDir.resolve("does-not-exist.txt").toFile();
        assertFalse(missing.exists(), "Test pre‑condition: file must not exist");
        String result = EncryptionUtilities.fastSHA1(missing);
        assertNull(result, "Result should be null when the file cannot be found");
    }

    /**
     * Verify that {@code fastSHA1(File)} returns {@code null}
     * when an {@code IOException} occurs while opening the stream.
     * This is simulated by creating a directory (which cannot be opened as a regular file).
     */
    @Test
    void fastSHA1_returnsNullOnIOException() throws IOException {
        // A directory cannot be opened as a regular InputStream, causing an IOException.
        File directory = Files.createDirectory(tempDir.resolve("subdir")).toFile();
        assertTrue(directory.isDirectory(), "Test pre‑condition: should be a directory");
        String result = EncryptionUtilities.fastSHA1(directory);
        assertNull(result, "Result should be null when an IOException occurs");
    }

    /**
     * Parameterized test covering:
     * <ul>
     *   <li>Normal file containing "Hello World"</li>
     *   <li>Empty file</li>
     * </ul>
     * Both cases use the FileInputStream branch.
     */
    @ParameterizedTest(name = "{index} => file={0}, expectedHash={1}")
    @CsvSource({ // file name,   expected SHA‑1 hash of the content
    "hello.txt, 2ef7bde608ce5404e97d5f042f95f89f1c232871", "empty.txt, da39a3ee5e6b4b0d3255bfef95601890afd80709" })
    void fastSHA1_fileInputStreamBranch(String fileName, String expected) throws IOException, NoSuchAlgorithmException {
        Path path = tempDir.resolve(fileName);
        Files.createFile(path);
        if (!fileName.startsWith("empty")) {
            Files.write(path, "Hello World".getBytes());
        }
        File file = path.toFile();
        // Compute expected hash using JDK (already supplied via CSV, but we double‑check)
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] data = file.length() > 0 ? Files.readAllBytes(path) : new byte[0];
        byte[] digest = md.digest(data);
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        assertEquals(expected, sb.toString(), "Pre‑condition: CSV expected hash must match JDK calculation");
        String actual = EncryptionUtilities.fastSHA1(file);
        assertNotNull(actual, "Hash should not be null for existing file");
        assertEquals(expected, actual, "SHA‑1 hash does not match expected value");
    }

    /**
     * Simulates the fallback branch where {@code Files.newInputStream} returns a
     * {@link ByteArrayInputStream} (i.e., not a {@link FileInputStream}).
     *
     * The method should still compute the correct SHA‑1 hash.
     */
    @Test
    void fastSHA1_fallbackBranchNonFileInputStream() throws IOException {
        // Prepare a real file with known content.
        Path path = tempDir.resolve("fallback.txt");
        byte[] content = "Hello World".getBytes();
        Files.write(path, content);
        File file = path.toFile();
        // Mock Files.newInputStream to return a ByteArrayInputStream instead of a FileInputStream.
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.newInputStream(eq(file.toPath()))).thenReturn(new ByteArrayInputStream(content));
            // Call the method under test.
            String actual = EncryptionUtilities.fastSHA1(file);
            // Expected SHA‑1 of "Hello World"
            String expected = "2ef7bde608ce5404e97d5f042f95f89f1c232871";
            assertEquals(expected, actual, "Fallback branch should compute correct SHA‑1 hash");
        }
    }

    /**
     * Passing {@code null} as the file argument should propagate a
     * {@link NullPointerException} (the method does not catch it).
     */
    @Test
    void fastSHA1_nullFileArgumentPropagatesNPE() {
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.fastSHA1(null), "fastSHA1 should propagate NullPointerException for null argument");
    }

    /**
     * Simulate a {@link SecurityException} thrown by {@code Files.newInputStream}.
     * The exception is unchecked and must propagate.
     */
    @Test
    void fastSHA1_securityExceptionPropagates() throws IOException {
        Path path = tempDir.resolve("secure.txt");
        Files.createFile(path);
        File file = path.toFile();
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.newInputStream(eq(file.toPath()))).thenThrow(new SecurityException("access denied"));
            assertThrows(SecurityException.class, () -> EncryptionUtilities.fastSHA1(file), "SecurityException should propagate from fastSHA1");
        }
    }

    /**
     * Mock {@link EncryptionUtilities#calculateFileHash(FileChannel, MessageDigest)} to throw an {@link IOException}.
     * The method should catch the exception and return {@code null}.
     */
    @Test
    void fastSHA1_calculateFileHashThrowsIOException_returnsNull() throws IOException {
        // Create a normal file so that Files.newInputStream returns a FileInputStream.
        Path path = tempDir.resolve("fileHashIOException.txt");
        Files.write(path, "Hello World".getBytes());
        File file = path.toFile();
        try (MockedStatic<EncryptionUtilities> euMock = mockStatic(EncryptionUtilities.class)) {
            // Let the real getSHA1Digest be used.
            euMock.when(EncryptionUtilities::getSHA1Digest).thenCallRealMethod();
            // Mock calculateFileHash to throw IOException.
            euMock.when(() -> EncryptionUtilities.calculateFileHash(any(FileChannel.class), any(MessageDigest.class))).thenThrow(new IOException("mocked I/O error"));
            // The rest of the static methods (e.g., calculateStreamHash) are not needed for this path.
            String result = EncryptionUtilities.fastSHA1(file);
            assertNull(result, "When calculateFileHash throws IOException, fastSHA1 should return null");
        }
    }

    /**
     * Mock {@link EncryptionUtilities#calculateStreamHash(InputStream, MessageDigest)} to throw an {@link IOException}.
     * The method should catch the exception and return {@code null}.
     */
    @Test
    void fastSHA1_calculateStreamHashThrowsIOException_returnsNull() throws IOException {
        // Prepare a file; we will force the fallback branch.
        Path path = tempDir.resolve("streamHashIOException.txt");
        Files.write(path, "Hello World".getBytes());
        File file = path.toFile();
        try (MockedStatic<Files> filesMock = mockStatic(Files.class);
            MockedStatic<EncryptionUtilities> euMock = mockStatic(EncryptionUtilities.class)) {
            // Force fallback: return a ByteArrayInputStream (not a FileInputStream).
            filesMock.when(() -> Files.newInputStream(eq(file.toPath()))).thenReturn(new ByteArrayInputStream("Hello World".getBytes()));
            // Let the real getSHA1Digest be used.
            euMock.when(EncryptionUtilities::getSHA1Digest).thenCallRealMethod();
            // Mock calculateStreamHash to throw IOException.
            euMock.when(() -> EncryptionUtilities.calculateStreamHash(any(InputStream.class), any(MessageDigest.class))).thenThrow(new IOException("mocked stream I/O error"));
            String result = EncryptionUtilities.fastSHA1(file);
            assertNull(result, "When calculateStreamHash throws IOException, fastSHA1 should return null");
        }
    }

    /**
     * Simulate an unchecked exception from {@code getSHA1Digest}.
     * The exception must propagate out of {@code fastSHA1}.
     */
    @Test
    void fastSHA1_getSHA1DigestThrowsRuntimeException_propagates() throws IOException {
        // Create a normal file so that the method reaches the digest acquisition step.
        Path path = tempDir.resolve("runtimeDigest.txt");
        Files.write(path, "Hello World".getBytes());
        File file = path.toFile();
        try (MockedStatic<EncryptionUtilities> euMock = mockStatic(EncryptionUtilities.class)) {
            // Mock getSHA1Digest to throw RuntimeException.
            euMock.when(EncryptionUtilities::getSHA1Digest).thenThrow(new RuntimeException("digest failure"));
            // The rest of the static methods are irrelevant because the exception occurs first.
            assertThrows(RuntimeException.class, () -> EncryptionUtilities.fastSHA1(file), "RuntimeException from getSHA1Digest should propagate");
        }
    }
}
