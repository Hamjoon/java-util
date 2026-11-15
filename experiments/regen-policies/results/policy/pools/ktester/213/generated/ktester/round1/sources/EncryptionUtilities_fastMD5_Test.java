package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.security.MessageDigest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import java.io.*;
import java.nio.file.*;
import java.security.InvalidPathException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_fastMD5_Test {

    /**
     * Temporary file used by the normal‑path test.
     */
    private Path tempFile;

    /**
     * Temporary directory used for the directory‑input test.
     */
    private Path tempDir;

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Create a temporary file with known content for the normal‑path test.
        tempFile = Files.createTempFile("EncryptionUtilities_fastMD5_Test", ".txt");
        Files.write(tempFile, "hello world".getBytes());
        // Create a temporary directory for the directory‑input test.
        tempDir = Files.createTempDirectory("EncryptionUtilities_fastMD5_dir");
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Delete temporary file.
        if (tempFile != null && Files.exists(tempFile)) {
            Files.delete(tempFile);
        }
        // Delete temporary directory.
        if (tempDir != null && Files.exists(tempDir)) {
            Files.delete(tempDir);
        }
    }

    /**
     * Verifies that {@code fastMD5} returns the correct MD5 hash for an existing file.
     */
    @Test
    void fastMD5_ReturnsCorrectHash_ForExistingFile() {
        // Arrange
        File file = tempFile.toFile();
        byte[] content = "hello world".getBytes();
        // Expected MD5 using the public utility method.
        String expected = EncryptionUtilities.calculateMD5Hash(content);
        // Act
        String actual = EncryptionUtilities.fastMD5(file);
        // Assert
        assertNotNull(actual, "Hash should not be null for an existing file");
        assertEquals(expected, actual, "MD5 hash does not match the expected value");
    }

    /**
     * Verifies that {@code fastMD5} returns {@code null} when the file does not exist.
     */
    @Test
    void fastMD5_ReturnsNull_WhenFileNotFound() {
        // Arrange
        File nonExistent = new File("this_file_should_not_exist_1234567890.tmp");
        // Pre‑condition check – make absolutely sure the file is absent.
        assertFalse(nonExistent.exists(), "Test pre‑condition failed: file unexpectedly exists");
        // Act
        String result = EncryptionUtilities.fastMD5(nonExistent);
        // Assert
        assertNull(result, "fastMD5 should return null for a missing file");
    }

    /**
     * Forces the fallback path by mocking {@code Files.newInputStream} to return a
     * {@link ByteArrayInputStream} (which is not a {@code FileInputStream}).
     */
    @Test
    void fastMD5_UsesFallbackWhenInputStreamIsNotFileInputStream() throws Exception {
        // Arrange
        byte[] content = "fallback stream content".getBytes();
        String expected = EncryptionUtilities.calculateMD5Hash(content);
        // Mock static Files.newInputStream to return a ByteArrayInputStream.
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.newInputStream(any(Path.class))).thenAnswer(invocation -> new ByteArrayInputStream(content));
            // The file argument is irrelevant because the stream is mocked.
            File dummyFile = new File("dummy.txt");
            // Act
            String actual = EncryptionUtilities.fastMD5(dummyFile);
            // Assert
            assertNotNull(actual, "Hash should not be null when using the fallback stream");
            assertEquals(expected, actual, "Fallback MD5 hash does not match the expected value");
        }
    }

    /**
     * Group: fastMD5 - FileInputStream branch (happy path) with an empty file.
     * Expected hash is the MD5 of an empty file: d41d8cd98f00b204e9800998ecf8427e
     */
    @Test
    void fastMD5_EmptyFile_ReturnsKnownHash() throws IOException {
        // Create an empty temporary file.
        Path emptyFile = Files.createTempFile("empty", ".txt");
        try {
            String actual = EncryptionUtilities.fastMD5(emptyFile.toFile());
            assertEquals("d41d8cd98f00b204e9800998ecf8427e", actual, "MD5 hash of an empty file should match the known value");
        } finally {
            Files.deleteIfExists(emptyFile);
        }
    }

    /**
     * Group: fastMD5 - directory input (IOException).
     * Passing a directory should cause Files.newInputStream to throw an IOException,
     * which fastMD5 catches and returns {@code null}.
     */
    @Test
    void fastMD5_DirectoryInput_ReturnsNull() {
        File dir = tempDir.toFile();
        assertTrue(dir.isDirectory(), "Setup failure: tempDir is not a directory");
        String result = EncryptionUtilities.fastMD5(dir);
        assertNull(result, "fastMD5 should return null when given a directory");
    }

    /**
     * Group: fastMD5 - null file argument.
     * Expect a NullPointerException because {@code file.toPath()} is invoked.
     */
    @Test
    void fastMD5_NullFileArgument_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.fastMD5(null), "Calling fastMD5 with null should throw NullPointerException");
    }

    /**
     * Group: fastMD5 - large file (multiple channel reads).
     * Creates a 5 MiB file filled with 0xAB bytes and verifies the computed hash.
     */
    @Test
    void fastMD5_LargeFile_ComputesCorrectHash() throws IOException {
        // 5 MiB = 5 * 1024 * 1024 bytes
        int size = 5 * 1024 * 1024;
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) 0xAB;
        }
        Path largeFile = Files.createTempFile("large", ".bin");
        try {
            Files.write(largeFile, data);
            String expected = EncryptionUtilities.calculateMD5Hash(data);
            String actual = EncryptionUtilities.fastMD5(largeFile.toFile());
            assertEquals(expected, actual, "MD5 hash of the large file does not match expected value");
        } finally {
            Files.deleteIfExists(largeFile);
        }
    }

    /**
     * Group: fastMD5 - SecurityException on file access.
     * Mock {@code Files.newInputStream} to throw a SecurityException and verify
     * that the exception propagates (unchecked).
     */
    @Test
    void fastMD5_SecurityException_Propagates() {
        File mockedFile = new File("any");
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.newInputStream(any(Path.class))).thenThrow(new SecurityException("access denied"));
            assertThrows(SecurityException.class, () -> EncryptionUtilities.fastMD5(mockedFile), "SecurityException should propagate out of fastMD5");
        }
    }

    /**
     * Group: fastMD5 - InvalidPathException from Path conversion.
     * Mock the static call to throw InvalidPathException (unchecked) and verify propagation.
     */
    @Test
    void fastMD5_InvalidPathException_Propagates() {
        File mockedFile = new File("any");
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.newInputStream(any(Path.class))).thenThrow(new InvalidPathException("invalid", "reason"));
            assertThrows(InvalidPathException.class, () -> EncryptionUtilities.fastMD5(mockedFile), "InvalidPathException should propagate out of fastMD5");
        }
    }

    /**
     * Group: fastMD5 - calculateFileHash throws IOException (caught).
     * After obtaining a FileInputStream, the static helper throws IOException.
     * fastMD5 should catch it and return {@code null}.
     */
    @Test
    void fastMD5_CalculateFileHashThrowsIOException_ReturnsNull() throws Exception {
        // Use the real temporary file (non‑empty) to get a real FileInputStream.
        File file = tempFile.toFile();
        // Mock static methods of EncryptionUtilities.
        try (MockedStatic<EncryptionUtilities> euMock = mockStatic(EncryptionUtilities.class);
            MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            // Files.newInputStream returns a real FileInputStream (so the branch is taken).
            filesMock.when(() -> Files.newInputStream(any(Path.class))).thenAnswer(invocation -> Files.newInputStream(file.toPath()));
            // calculateFileHash throws IOException.
            euMock.when(() -> EncryptionUtilities.calculateFileHash(any(), any())).thenThrow(new IOException("simulated IO error"));
            // The MD5 digest method is also called; we can return a real MessageDigest.
            euMock.when(EncryptionUtilities::getMD5Digest).thenCallRealMethod();
            String result = EncryptionUtilities.fastMD5(file);
            assertNull(result, "fastMD5 should return null when calculateFileHash throws IOException");
        }
    }

    /**
     * Group: fastMD5 - calculateFileHash throws unchecked RuntimeException (propagated).
     */
    @Test
    void fastMD5_CalculateFileHashThrowsRuntimeException_Propagates() throws Exception {
        File file = tempFile.toFile();
        try (MockedStatic<EncryptionUtilities> euMock = mockStatic(EncryptionUtilities.class);
            MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.newInputStream(any(Path.class))).thenAnswer(invocation -> Files.newInputStream(file.toPath()));
            euMock.when(() -> EncryptionUtilities.calculateFileHash(any(), any())).thenThrow(new IllegalStateException("unchecked error"));
            euMock.when(EncryptionUtilities::getMD5Digest).thenCallRealMethod();
            assertThrows(IllegalStateException.class, () -> EncryptionUtilities.fastMD5(file), "Unchecked exception from calculateFileHash should propagate");
        }
    }

    /**
     * Group: fastMD5 - fallback path with IOException during read.
     * The InputStream returned by Files.newInputStream throws IOException on read().
     * fastMD5 should catch it and return {@code null}.
     */
    @Test
    void fastMD5_FallbackStreamThrowsIOException_ReturnsNull() {
        InputStream badStream = new InputStream() {

            @Override
            public int read() throws IOException {
                throw new IOException("forced read error");
            }
        };
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.newInputStream(any(Path.class))).thenReturn(badStream);
            File dummy = new File("dummy");
            String result = EncryptionUtilities.fastMD5(dummy);
            assertNull(result, "fastMD5 should return null when fallback stream read throws IOException");
        }
    }

    /**
     * Group: fastMD5 - calculateStreamHash throws unchecked IllegalArgumentException (propagated).
     */
    @Test
    void fastMD5_CalculateStreamHashThrowsUnchecked_Propagates() {
        byte[] data = "hello".getBytes();
        try (MockedStatic<Files> filesMock = mockStatic(Files.class);
            MockedStatic<EncryptionUtilities> euMock = mockStatic(EncryptionUtilities.class)) {
            filesMock.when(() -> Files.newInputStream(any(Path.class))).thenReturn(new ByteArrayInputStream(data));
            euMock.when(() -> EncryptionUtilities.calculateStreamHash(any(InputStream.class), any())).thenThrow(new IllegalArgumentException("bad argument"));
            File dummy = new File("dummy");
            assertThrows(IllegalArgumentException.class, () -> EncryptionUtilities.fastMD5(dummy), "Unchecked exception from calculateStreamHash should propagate");
        }
    }

    /**
     * Group: fastMD5 - fallback to calculateStreamHash (happy path) with known content.
     * This mirrors the JSON case using "hello world".
     */
    @Test
    void fastMD5_FallbackHappyPath_ReturnsCorrectHash() throws Exception {
        byte[] content = "hello world".getBytes();
        String expected = EncryptionUtilities.calculateMD5Hash(content);
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.newInputStream(any(Path.class))).thenReturn(new ByteArrayInputStream(content));
            File dummy = new File("dummy");
            String actual = EncryptionUtilities.fastMD5(dummy);
            assertEquals(expected, actual, "Fallback MD5 hash should match expected value");
        }
    }
}
