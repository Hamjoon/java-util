package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.io.*;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_fastSHA512_Test {

    /**
     * Temporary file used for the happy‑path test.
     */
    private Path tempFile;

    /**
     * Content written to {@link #tempFile}.
     */
    private static final byte[] CONTENT = "The quick brown fox jumps over the lazy dog".getBytes();

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Create a fresh temporary file before each test and write known content.
        tempFile = Files.createTempFile("EncryptionUtilities_fastSHA512_Test", ".tmp");
        Files.write(tempFile, CONTENT, StandardOpenOption.WRITE);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Clean up the temporary file after each test.
        if (tempFile != null && Files.exists(tempFile)) {
            Files.delete(tempFile);
        }
    }

    /**
     * Verifies that {@code fastSHA512(File)} returns the correct SHA‑512 hash
     * for an existing file.
     */
    @Test
    void fastSHA512_returnsCorrectHash_forExistingFile() throws NoSuchAlgorithmException {
        // GIVEN – a file with known content (created in @BeforeEach)
        // WHEN – the method under test is invoked
        String actualHash = EncryptionUtilities.fastSHA512(tempFile.toFile());
        // THEN – the hash matches the value produced by MessageDigest directly
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] expectedBytes = md.digest(CONTENT);
        String expectedHash = HexFormat.of().formatHex(expectedBytes);
        assertNotNull(actualHash, "Hash should not be null for an existing file");
        assertEquals(expectedHash, actualHash, "SHA‑512 hash does not match expected value");
    }

    /**
     * Verifies that {@code fastSHA512(File)} returns {@code null}
     * when the supplied file does not exist.
     */
    @Test
    void fastSHA512_returnsNull_forMissingFile() {
        // GIVEN – a Path that points to a non‑existent file
        Path missing = Paths.get("nonexistent_" + System.nanoTime());
        // WHEN – the method under test is invoked
        String result = EncryptionUtilities.fastSHA512(missing.toFile());
        // THEN – the result is null (method swallows NoSuchFileException)
        assertNull(result, "Result should be null when the file does not exist");
    }

    /**
     * Verifies that {@code fastSHA512(File)} returns {@code null}
     * when an I/O error occurs while opening the file.
     *
     * This test simulates an I/O error by creating a file and then
     * revoking read permissions before invoking the method.
     */
    @Test
    void fastSHA512_returnsNull_onIOException() throws IOException {
        // GIVEN – a file that cannot be read
        Path unreadable = Files.createTempFile("unreadable", ".tmp");
        Files.write(unreadable, CONTENT);
        // Remove read permission (platform‑dependent; best‑effort)
        unreadable.toFile().setReadable(false, false);
        try {
            // WHEN – the method is called
            String result = EncryptionUtilities.fastSHA512(unreadable.toFile());
            // THEN – it should return null because an IOException was caught
            assertNull(result, "Result should be null when an IOException occurs");
        } finally {
            // Restore permission so the file can be deleted
            unreadable.toFile().setReadable(true, false);
            Files.deleteIfExists(unreadable);
        }
    }

    /**
     * Fallback path test – {@code Files.newInputStream} returns a non‑FileInputStream.
     * The method should fall back to {@code calculateStreamHash} using SHA‑1 and
     * therefore return the SHA‑1 hash of the file’s bytes.
     */
    @Test
    void fastSHA512_fallbackToSHA1_whenNotFileInputStream() throws Exception {
        // GIVEN – a dummy file (its path is irrelevant because we mock the stream)
        Path dummyPath = Paths.get("dummy.txt");
        byte[] data = "fallback‑test".getBytes();
        // Mock static Files.newInputStream to return a ByteArrayInputStream
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
            filesMock.when(() -> Files.newInputStream(Mockito.eq(dummyPath))).thenReturn(new ByteArrayInputStream(data));
            // WHEN – fastSHA512 is invoked
            String actual = EncryptionUtilities.fastSHA512(dummyPath.toFile());
            // THEN – SHA‑1 hash of the data is returned
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            String expected = HexFormat.of().formatHex(sha1.digest(data));
            assertEquals(expected, actual, "Fallback SHA‑1 hash does not match expected value");
        }
    }

    /**
     * Simulates {@code Files.newInputStream} throwing {@link NoSuchFileException}.
     * The method should catch the exception and return {@code null}.
     */
    @Test
    void fastSHA512_returnsNull_whenNoSuchFileException() throws Exception {
        Path missing = Paths.get("nonexistent_" + System.nanoTime());
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
            filesMock.when(() -> Files.newInputStream(Mockito.eq(missing))).thenThrow(new NoSuchFileException("boom"));
            String result = EncryptionUtilities.fastSHA512(missing.toFile());
            assertNull(result, "Result should be null when NoSuchFileException is thrown");
        }
    }

    /**
     * Simulates {@code Files.newInputStream} throwing a generic {@link IOException}.
     * The method should catch the exception and return {@code null}.
     */
    @Test
    void fastSHA512_returnsNull_whenGenericIOException() throws Exception {
        Path somePath = Paths.get("anyfile.tmp");
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
            filesMock.when(() -> Files.newInputStream(Mockito.eq(somePath))).thenThrow(new IOException("boom"));
            String result = EncryptionUtilities.fastSHA512(somePath.toFile());
            assertNull(result, "Result should be null when generic IOException is thrown");
        }
    }

    /**
     * Passing a {@code null} argument should result in a {@link NullPointerException}
     * bubbling up from {@code Files.newInputStream}.
     */
    @Test
    void fastSHA512_propagatesNullPointerException_forNullArgument() {
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.fastSHA512(null), "NullPointerException should be thrown for a null File argument");
    }

    /**
     * An empty file (zero bytes) should produce the well‑known SHA‑512 hash
     * of an empty input.
     */
    @Test
    void fastSHA512_correctHash_forEmptyFile() throws Exception {
        // GIVEN – an empty temporary file
        Path emptyFile = Files.createTempFile("empty", ".tmp");
        try {
            // WHEN
            String actual = EncryptionUtilities.fastSHA512(emptyFile.toFile());
            // THEN – compare with the known SHA‑512 of empty input
            String expected = "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce" + "47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e";
            assertEquals(expected, actual, "SHA‑512 hash of empty file does not match expected value");
        } finally {
            Files.deleteIfExists(emptyFile);
        }
    }

    /**
     * When the supplied {@code File} is a directory, {@code Files.newInputStream}
     * throws an {@link IOException}.  The method should return {@code null}.
     */
    @Test
    void fastSHA512_returnsNull_whenFileIsDirectory() throws Exception {
        // GIVEN – a directory path (use the system temporary directory)
        Path dir = Files.createTempDirectory("dirTest");
        try {
            String result = EncryptionUtilities.fastSHA512(dir.toFile());
            assertNull(result, "Result should be null when the path points to a directory");
        } finally {
            Files.deleteIfExists(dir);
        }
    }

    /**
     * Simulates {@code calculateFileHash} throwing an {@link IOException}
     * after the {@code FileChannel} has been obtained.  The method should
     * catch the exception and return {@code null}.
     */
    @Test
    void fastSHA512_returnsNull_whenCalculateFileHashThrows() throws Exception {
        // GIVEN – a regular temporary file with content
        Path file = Files.createTempFile("calcHashFail", ".tmp");
        Files.write(file, CONTENT);
        // Mock static methods:
        // 1. Files.newInputStream returns a real FileInputStream (so channel is obtained)
        // 2. EncryptionUtilities.calculateFileHash throws IOException
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class);
            MockedStatic<EncryptionUtilities> euMock = Mockito.mockStatic(EncryptionUtilities.class)) {
            FileInputStream fis = new FileInputStream(file.toFile());
            filesMock.when(() -> Files.newInputStream(Mockito.eq(file))).thenReturn(fis);
            // When calculateFileHash is called, throw IOException
            euMock.when(() -> EncryptionUtilities.calculateFileHash(Mockito.any(), Mockito.any())).thenThrow(new IOException("simulated failure"));
            // The fallback digest (SHA‑512) is not used because the exception occurs first
            String result = EncryptionUtilities.fastSHA512(file.toFile());
            assertNull(result, "Result should be null when calculateFileHash throws IOException");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    /**
     * Stress test with a relatively large file (≈1 MiB).  Verifies that the
     * implementation can process data larger than typical buffer sizes.
     */
    @Test
    void fastSHA512_correctHash_forLargeFile() throws Exception {
        // GIVEN – a temporary file filled with pseudo‑random data (1 MiB)
        Path largeFile = Files.createTempFile("large", ".bin");
        byte[] buffer = new byte[1024];
        try (OutputStream out = Files.newOutputStream(largeFile, StandardOpenOption.WRITE)) {
            for (int i = 0; i < 1024; i++) {
                // 1024 * 1024 = 1 MiB
                ThreadLocalRandom.current().nextBytes(buffer);
                out.write(buffer);
            }
        }
        // Compute expected SHA‑512 using MessageDigest directly
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        try (InputStream in = Files.newInputStream(largeFile)) {
            byte[] tmp = new byte[8192];
            int read;
            while ((read = in.read(tmp)) != -1) {
                md.update(tmp, 0, read);
            }
        }
        String expected = HexFormat.of().formatHex(md.digest());
        // WHEN – invoke the method under test
        String actual = EncryptionUtilities.fastSHA512(largeFile.toFile());
        // THEN – hashes must match
        assertEquals(expected, actual, "SHA‑512 hash of large file does not match expected value");
        Files.deleteIfExists(largeFile);
    }
}
