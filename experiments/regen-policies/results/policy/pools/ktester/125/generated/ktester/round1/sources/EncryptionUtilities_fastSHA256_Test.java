package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.channels.FileChannel;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_fastSHA256_Test {

    /**
     * Temporary file used for the happy‑path test.
     */
    private Path tempFile;

    /**
     * Expected SHA‑256 hash of the temporary file's content (hex string).
     */
    private String expectedHash;

    /**
     * Temporary directory used for creating test files.
     */
    private Path tempDir;

    /**
     * Prepare the per‑test fixture: a temporary file with known bytes and the expected hash.
     */
    @BeforeEach
    void setupBeforeEach() throws IOException, NoSuchAlgorithmException {
        // deterministic content – the same for every run
        byte[] content = "EncryptionUtilities fastSHA256 test data".getBytes();
        // create the temp file and write the content
        tempFile = Files.createTempFile("fastSHA256Test", ".dat");
        Files.write(tempFile, content, StandardOpenOption.WRITE);
        // compute expected SHA‑256 hash using the JDK directly
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(content);
        expectedHash = bytesToHex(digest);
        tempDir = Files.createTempDirectory("fastSHA256Tests");
    }

    /**
     * Delete the temporary file after each test to avoid littering the file‑system.
     */
    @AfterEach
    void teardownAfterEach() throws IOException {
        if (tempFile != null) {
            Files.deleteIfExists(tempFile);
        }
        if (tempDir != null) {
            Files.walk(tempDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    /**
     * Helper to convert a byte array to a lower‑case hex string (the format used by EncryptionUtilities).
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Compute SHA‑256 hex string using JDK directly (used for expected values).
     */
    private static String sha256Hex(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return bytesToHex(md.digest(data));
    }

    /**
     * Compute SHA‑1 hex string using JDK directly (used for fallback expectations).
     */
    private static String sha1Hex(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        return bytesToHex(md.digest(data));
    }

    /**
     * Happy‑path test: a real file should yield the correct SHA‑256 hash.
     */
    @Test
    void fastSHA256_returnsCorrectHash_forExistingFile() {
        // given – the temporary file is already created in @BeforeEach
        // when
        String actual = EncryptionUtilities.fastSHA256(tempFile.toFile());
        // then
        assertNotNull(actual, "Hash should not be null for an existing file");
        assertEquals(expectedHash, actual, "Computed SHA‑256 hash does not match the expected value");
    }

    /**
     * Edge‑case test: a non‑existent file should cause the method to return {@code null}.
     */
    @Test
    void fastSHA256_returnsNull_forMissingFile() {
        // given
        File missing = new File("this_file_does_not_exist_12345.tmp");
        // when
        String result = EncryptionUtilities.fastSHA256(missing);
        // then
        assertNull(result, "Method should return null when the file cannot be found");
    }

    /* --------------------------------------------------------------
     *  1. successful hash using FileInputStream (SHA‑256)
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("fastSHA256 - successful hash using FileInputStream")
    void fastSHA256_successfulHashUsingFileInputStream() throws Exception {
        // ----- arrange -------------------------------------------------
        byte[] content = "hello".getBytes();
        Path file = Files.write(tempDir.resolve("hello.txt"), content);
        String expectedHash = sha256Hex(content);
        // Mock Files.newInputStream to return a real FileInputStream
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class);
            MockedStatic<EncryptionUtilities> euMock = Mockito.mockStatic(EncryptionUtilities.class)) {
            filesMock.when(() -> Files.newInputStream(eq(file), any(OpenOption[].class))).thenAnswer(invocation -> new FileInputStream(file.toFile()));
            // Mock calculateFileHash to simply return the pre‑computed hash
            euMock.when(() -> EncryptionUtilities.calculateFileHash(any(FileChannel.class), any(MessageDigest.class))).thenAnswer(invocation -> {
                MessageDigest md = invocation.getArgument(1);
                assertEquals("SHA-256", md.getAlgorithm(), "fastSHA256 must use a SHA‑256 MessageDigest");
                return expectedHash;
            });
            // ----- act ----------------------------------------------------
            String actual = EncryptionUtilities.fastSHA256(file.toFile());
            // ----- assert -------------------------------------------------
            assertNotNull(actual);
            assertEquals(expectedHash, actual);
        }
    }

    /* --------------------------------------------------------------
     *  2. file not found (NoSuchFileException)
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("fastSHA256 - file not found (NoSuchFileException)")
    void fastSHA256_fileNotFound() throws Exception {
        File missing = tempDir.resolve("nonexistent.txt").toFile();
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
            filesMock.when(() -> Files.newInputStream(eq(missing.toPath()), any(OpenOption[].class))).thenThrow(new NoSuchFileException(missing.getPath()));
            String result = EncryptionUtilities.fastSHA256(missing);
            assertNull(result, "Method should return null when the file cannot be found");
        }
    }

    /* --------------------------------------------------------------
     *  3. IOException while opening stream
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("fastSHA256 - IOException while opening stream")
    void fastSHA256_ioExceptionOpeningStream() throws Exception {
        File file = tempDir.resolve("ioerror.txt").toFile();
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
            filesMock.when(() -> Files.newInputStream(eq(file.toPath()), any(OpenOption[].class))).thenThrow(new IOException("simulated I/O error"));
            String result = EncryptionUtilities.fastSHA256(file);
            assertNull(result, "Method should return null when an IOException occurs while opening the stream");
        }
    }

    /* --------------------------------------------------------------
     *  4. fallback to calculateStreamHash (non‑FileInputStream)
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("fastSHA256 - fallback to calculateStreamHash (non‑FileInputStream)")
    void fastSHA256_fallbackToStreamHash() throws Exception {
        byte[] content = "hello".getBytes();
        Path file = Files.write(tempDir.resolve("hello-fallback.txt"), content);
        // fallback uses SHA‑1 per implementation
        String expectedSha1 = sha1Hex(content);
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class);
            MockedStatic<EncryptionUtilities> euMock = Mockito.mockStatic(EncryptionUtilities.class)) {
            // Return a ByteArrayInputStream (not a FileInputStream)
            filesMock.when(() -> Files.newInputStream(eq(file), any(OpenOption[].class))).thenReturn(new ByteArrayInputStream(content));
            // Mock calculateStreamHash to verify SHA‑1 digest usage and return expected hash
            euMock.when(() -> EncryptionUtilities.calculateStreamHash(any(InputStream.class), any(MessageDigest.class))).thenAnswer(invocation -> {
                MessageDigest md = invocation.getArgument(1);
                assertEquals("SHA-1", md.getAlgorithm(), "Fallback should use a SHA‑1 MessageDigest");
                return expectedSha1;
            });
            String actual = EncryptionUtilities.fastSHA256(file.toFile());
            assertEquals(expectedSha1, actual);
        }
    }

    /* --------------------------------------------------------------
     *  5. empty file (SHA‑256 of zero bytes)
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("fastSHA256 - empty file")
    void fastSHA256_emptyFile() throws Exception {
        Path emptyFile = Files.createFile(tempDir.resolve("empty.txt"));
        // SHA‑256 of empty array
        String expectedHash = sha256Hex(new byte[0]);
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class);
            MockedStatic<EncryptionUtilities> euMock = Mockito.mockStatic(EncryptionUtilities.class)) {
            filesMock.when(() -> Files.newInputStream(eq(emptyFile), any(OpenOption[].class))).thenAnswer(invocation -> new FileInputStream(emptyFile.toFile()));
            euMock.when(() -> EncryptionUtilities.calculateFileHash(any(FileChannel.class), any(MessageDigest.class))).thenAnswer(invocation -> {
                MessageDigest md = invocation.getArgument(1);
                assertEquals("SHA-256", md.getAlgorithm());
                return expectedHash;
            });
            String actual = EncryptionUtilities.fastSHA256(emptyFile.toFile());
            assertEquals(expectedHash, actual);
        }
    }

    /* --------------------------------------------------------------
     *  6. SecurityException while opening stream (unchecked)
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("fastSHA256 - SecurityException while opening stream")
    void fastSHA256_securityException() throws Exception {
        File file = tempDir.resolve("secure.txt").toFile();
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
            filesMock.when(() -> Files.newInputStream(eq(file.toPath()), any(OpenOption[].class))).thenThrow(new SecurityException("access denied"));
            assertThrows(SecurityException.class, () -> EncryptionUtilities.fastSHA256(file), "SecurityException should propagate out of fastSHA256");
        }
    }

    /* --------------------------------------------------------------
     *  7. null file parameter
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("fastSHA256 - null file parameter")
    void fastSHA256_nullFile() {
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.fastSHA256(null), "Calling fastSHA256 with null should throw NullPointerException");
    }

    /* --------------------------------------------------------------
     *  8. IOException during calculateFileHash
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("fastSHA256 - IOException during calculateFileHash")
    void fastSHA256_ioExceptionDuringCalculateFileHash() throws Exception {
        Path file = Files.write(tempDir.resolve("io-during-hash.txt"), "data".getBytes());
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class);
            MockedStatic<EncryptionUtilities> euMock = Mockito.mockStatic(EncryptionUtilities.class)) {
            filesMock.when(() -> Files.newInputStream(eq(file), any(OpenOption[].class))).thenAnswer(invocation -> new FileInputStream(file.toFile()));
            euMock.when(() -> EncryptionUtilities.calculateFileHash(any(FileChannel.class), any(MessageDigest.class))).thenThrow(new IOException("simulated hash error"));
            String result = EncryptionUtilities.fastSHA256(file.toFile());
            assertNull(result, "When calculateFileHash throws IOException, fastSHA256 should return null");
        }
    }

    /* --------------------------------------------------------------
     *  9. IOException during calculateStreamHash (fallback)
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("fastSHA256 - IOException during calculateStreamHash (fallback)")
    void fastSHA256_ioExceptionDuringCalculateStreamHash() throws Exception {
        Path file = Files.write(tempDir.resolve("io-during-fallback.txt"), "data".getBytes());
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class);
            MockedStatic<EncryptionUtilities> euMock = Mockito.mockStatic(EncryptionUtilities.class)) {
            // Return a non‑FileInputStream to trigger fallback
            filesMock.when(() -> Files.newInputStream(eq(file), any(OpenOption[].class))).thenReturn(new ByteArrayInputStream("data".getBytes()));
            euMock.when(() -> EncryptionUtilities.calculateStreamHash(any(InputStream.class), any(MessageDigest.class))).thenThrow(new IOException("fallback read error"));
            String result = EncryptionUtilities.fastSHA256(file.toFile());
            assertNull(result, "When calculateStreamHash throws IOException, fastSHA256 should return null");
        }
    }

    /* --------------------------------------------------------------
     * 10. file is a directory (IOException)
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("fastSHA256 - file is a directory (IOException)")
    void fastSHA256_directoryPath() throws Exception {
        Path dir = Files.createDirectory(tempDir.resolve("directory"));
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
            filesMock.when(() -> Files.newInputStream(eq(dir), any(OpenOption[].class))).thenThrow(new IOException("cannot open directory as stream"));
            String result = EncryptionUtilities.fastSHA256(dir.toFile());
            assertNull(result, "When the path is a directory, fastSHA256 should return null");
        }
    }
}
