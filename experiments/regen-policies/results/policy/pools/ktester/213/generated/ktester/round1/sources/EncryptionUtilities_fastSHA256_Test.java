package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.security.MessageDigest;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.Collections;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_fastSHA256_Test {

    /**
     * Temporary directory provided by JUnit – used for creating real files
     */
    @TempDir
    static Path tempDir;

    /**
     * A small file with deterministic content – used in the happy‑path test
     */
    private static File sampleFile;

    /**
     * Expected SHA‑256 hash of {@link #sampleFile} – computed once in @BeforeAll
     */
    private static String expectedHash;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // create a file with known bytes
        sampleFile = tempDir.resolve("sample.txt").toFile();
        byte[] data = "The quick brown fox jumps over the lazy dog".getBytes();
        Files.write(sampleFile.toPath(), data);
        // compute the expected hash using the public helper method
        expectedHash = EncryptionUtilities.calculateSHA256Hash(data);
    }

    /**
     * Verifies that {@code fastSHA256(File)} returns the correct SHA‑256 hash
     * for an existing file. The implementation should delegate to
     * {@code calculateFileHash} which ultimately uses the same algorithm as
     * {@code calculateSHA256Hash(byte[])}.
     */
    @Test
    void fastSHA256_returnsCorrectHashForExistingFile() {
        String actual = EncryptionUtilities.fastSHA256(sampleFile);
        assertNotNull(actual, "Hash should not be null for an existing file");
        assertEquals(expectedHash, actual, "The SHA‑256 hash does not match the expected value");
    }

    /**
     * Verifies that {@code fastSHA256(File)} returns {@code null} when the
     * supplied file does not exist (the method catches {@code NoSuchFileException}).
     */
    @Test
    void fastSHA256_returnsNullWhenFileDoesNotExist() {
        File nonExistent = tempDir.resolve("does-not-exist.txt").toFile();
        assertFalse(nonExistent.exists(), "File must not exist for this test");
        String result = EncryptionUtilities.fastSHA256(nonExistent);
        assertNull(result, "Method should return null when the file cannot be found");
    }

    /**
     * Simulates an {@code IOException} (different from {@code NoSuchFileException})
     * being thrown while opening the file. The method should swallow the exception
     * and also return {@code null}.
     */
    @Test
    void fastSHA256_returnsNullWhenIOExceptionIsThrown() throws Exception {
        // Mock the static Files class to throw a generic IOException
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.newInputStream(Mockito.any())).thenThrow(new IOException("simulated I/O failure"));
            // The file itself does not need to exist because the mock intercepts the call
            File dummy = new File("dummy.txt");
            String result = EncryptionUtilities.fastSHA256(dummy);
            assertNull(result, "Method should return null when an IOException occurs");
        }
    }

    /**
     * When {@link Files#newInputStream(Path)} returns an {@link InputStream}
     * that is *not* a {@code FileInputStream}, the fallback branch should be taken
     * and the hash must still be correct.
     */
    @Test
    void fastSHA256_usesFallbackStreamWhenNotFileInputStream() throws Exception {
        // Content for the temporary file
        Path filePath = tempDir.resolve("fallback.txt");
        byte[] data = "fallback test".getBytes();
        Files.write(filePath, data);
        File file = filePath.toFile();
        // Mock Files.newInputStream to return a ByteArrayInputStream (not a FileInputStream)
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.newInputStream(file.toPath())).thenReturn(new ByteArrayInputStream(data));
            // The method should still compute the correct hash using the fallback branch
            String actual = EncryptionUtilities.fastSHA256(file);
            String expected = EncryptionUtilities.calculateSHA256Hash(data);
            assertEquals(expected, actual, "Hash from fallback branch is incorrect");
        }
    }

    /**
     * An empty file should produce the well‑known SHA‑256 hash of an empty byte array.
     */
    @Test
    void fastSHA256_emptyFileReturnsEmptyHash() throws Exception {
        Path emptyPath = tempDir.resolve("empty.txt");
        // zero‑byte file
        Files.createFile(emptyPath);
        File emptyFile = emptyPath.toFile();
        String actual = EncryptionUtilities.fastSHA256(emptyFile);
        String expected = EncryptionUtilities.calculateSHA256Hash(new byte[0]);
        assertEquals(expected, actual, "Hash of empty file is incorrect");
    }

    /**
     * Supplying a directory should cause {@link Files#newInputStream(Path)} to throw
     * an {@code IsDirectoryException}. The method catches the {@code IOException}
     * and returns {@code null}.
     */
    @Test
    void fastSHA256_directorySuppliedReturnsNull() {
        // a directory, not a regular file
        File dir = tempDir.toFile();
        assertTrue(dir.isDirectory(), "TempDir must be a directory");
        String result = EncryptionUtilities.fastSHA256(dir);
        assertNull(result, "Method should return null when a directory is supplied");
    }

    /**
     * Passing {@code null} as the argument should result in a {@link NullPointerException}
     * because {@code file.toPath()} is dereferenced without a null‑check.
     */
    @Test
    void fastSHA256_nullArgumentThrowsNPE() {
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.fastSHA256(null), "fastSHA256 should propagate NullPointerException for null argument");
    }

    /**
     * Simulate an {@link AccessDeniedException} when trying to open the file.
     * The method should catch the {@code IOException} and return {@code null}.
     */
    @Test
    void fastSHA256_accessDeniedReturnsNull() throws Exception {
        File file = tempDir.resolve("restricted.txt").toFile();
        Files.write(file.toPath(), "secret".getBytes());
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.newInputStream(file.toPath())).thenThrow(new AccessDeniedException(file.getPath()));
            String result = EncryptionUtilities.fastSHA256(file);
            assertNull(result, "Method should return null on AccessDeniedException");
        }
    }

    /**
     * If {@code calculateFileHash} throws an {@link IOException}, the outer method
     * must catch it and return {@code null}.
     */
    @Test
    void fastSHA256_calculateFileHashThrowsIOExceptionReturnsNull() throws Exception {
        // Mock Files.newInputStream to return a real FileInputStream
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
            MockedStatic<EncryptionUtilities> mockedUtil = Mockito.mockStatic(EncryptionUtilities.class)) {
            FileInputStream fis = new FileInputStream(sampleFile);
            mockedFiles.when(() -> Files.newInputStream(sampleFile.toPath())).thenReturn(fis);
            // Mock the static calculateFileHash to throw IOException
            mockedUtil.when(() -> EncryptionUtilities.calculateFileHash(any(FileChannel.class), any(MessageDigest.class))).thenThrow(new IOException("forced from calculateFileHash"));
            String result = EncryptionUtilities.fastSHA256(sampleFile);
            assertNull(result, "Method should return null when calculateFileHash throws IOException");
        }
    }

    /**
     * If the fallback {@code calculateStreamHash} encounters an {@link IOException}
     * during read, the method should return {@code null}.
     */
    @Test
    void fastSHA256_fallbackCalculateStreamHashThrowsIOExceptionReturnsNull() throws Exception {
        Path filePath = tempDir.resolve("stream-failure.txt");
        Files.write(filePath, "data".getBytes());
        File file = filePath.toFile();
        // InputStream that throws IOException on read()
        InputStream badStream = new InputStream() {

            @Override
            public int read() throws IOException {
                throw new IOException("read failure");
            }
        };
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.newInputStream(file.toPath())).thenReturn(// not a FileInputStream → fallback branch
            badStream);
            String result = EncryptionUtilities.fastSHA256(file);
            assertNull(result, "Method should return null when calculateStreamHash throws IOException");
        }
    }

    /**
     * Verify that a symbolic link is resolved correctly and the hash of the
     * target file is returned.
     */
    @Test
    void fastSHA256_symbolicLinkReturnsTargetHash() throws Exception {
        // Create the real file
        Path target = tempDir.resolve("target.txt");
        byte[] data = "link content".getBytes();
        Files.write(target, data);
        // Create a symbolic link (skip on platforms that do not support it)
        Path link = tempDir.resolve("symlink.txt");
        try {
            Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | IOException e) {
            Assumptions.assumeTrue(false, "Symbolic links not supported on this platform");
        }
        String actual = EncryptionUtilities.fastSHA256(link.toFile());
        String expected = EncryptionUtilities.calculateSHA256Hash(data);
        assertEquals(expected, actual, "Hash of symbolic link target is incorrect");
    }

    /**
     * When {@link Files#newInputStream(Path)} throws a {@link SecurityException},
     * the method does not catch it (it's unchecked) and the exception should
     * propagate to the caller.
     */
    @Test
    void fastSHA256_securityExceptionPropagates() throws Exception {
        File file = tempDir.resolve("secure.txt").toFile();
        Files.write(file.toPath(), "data".getBytes());
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.newInputStream(file.toPath())).thenThrow(new SecurityException("access denied"));
            assertThrows(SecurityException.class, () -> EncryptionUtilities.fastSHA256(file), "SecurityException should propagate");
        }
    }

    /**
     * When {@link Files#newInputStream(Path)} throws an {@link IllegalArgumentException},
     * the method does not catch it and the exception should propagate.
     */
    @Test
    void fastSHA256_illegalArgumentExceptionPropagates() throws Exception {
        File file = new File("invalid<>path");
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.newInputStream(file.toPath())).thenThrow(new IllegalArgumentException("bad path"));
            assertThrows(IllegalArgumentException.class, () -> EncryptionUtilities.fastSHA256(file), "IllegalArgumentException should propagate");
        }
    }

    /**
     * If {@code close()} on the {@link InputStream} throws an {@link IOException},
     * the outer catch block will handle it and the method should return {@code null}.
     */
    @Test
    void fastSHA256_closeThrowsIOExceptionReturnsNull() throws Exception {
        Path filePath = tempDir.resolve("close-failure.txt");
        Files.write(filePath, "content".getBytes());
        File file = filePath.toFile();
        // InputStream that works for read but throws on close()
        InputStream stream = new ByteArrayInputStream(Files.readAllBytes(filePath)) {

            @Override
            public void close() throws IOException {
                throw new IOException("close failure");
            }
        };
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.newInputStream(file.toPath())).thenReturn(// not a FileInputStream → fallback branch
            stream);
            String result = EncryptionUtilities.fastSHA256(file);
            assertNull(result, "Method should return null when close() throws IOException");
        }
    }

    /**
     * Simulate a {@link FileInputStream#getChannel()} that throws an {@link IOException}.
     * The method should catch the exception and return {@code null}.
     */
    @Test
    void fastSHA256_fileInputStreamGetChannelThrowsIOExceptionReturnsNull() throws Exception {
        // Mock a FileInputStream whose getChannel() throws IOException
        FileInputStream mockedFis = Mockito.mock(FileInputStream.class);
        Mockito.when(mockedFis.getChannel()).thenThrow(new IOException("channel failure"));
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.newInputStream(sampleFile.toPath())).thenReturn(mockedFis);
            String result = EncryptionUtilities.fastSHA256(sampleFile);
            assertNull(result, "Method should return null when getChannel() throws IOException");
        }
    }
}
