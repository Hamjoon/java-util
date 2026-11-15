package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.channels.FileChannel;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_fastMD5_Test {

    /**
     * Verify that fastMD5 returns the correct MD5 hash string for a normal file.
     */
    @Test
    void fastMD5_returnsCorrectHash_forExistingFile(@TempDir Path tempDir) throws Exception {
        // GIVEN – a temporary file with known content
        Path filePath = tempDir.resolve("sample.txt");
        byte[] content = "The quick brown fox jumps over the lazy dog".getBytes();
        Files.write(filePath, content);
        File file = filePath.toFile();
        // AND – the expected MD5 hash calculated with the JDK MessageDigest
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] digest = md5.digest(content);
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        String expectedHash = sb.toString();
        // WHEN – the method under test is invoked
        String actualHash = EncryptionUtilities.fastMD5(file);
        // THEN – the returned hash matches the expected value
        assertEquals(expectedHash, actualHash);
    }

    /**
     * Verify that fastMD5 returns {@code null} when the supplied file does not exist.
     */
    @Test
    void fastMD5_returnsNull_whenFileDoesNotExist() {
        // GIVEN – a File instance that points to a non‑existent location
        File missingFile = new File("this_file_should_not_exist_12345.tmp");
        assertFalse(missingFile.exists(), "Test precondition: file must not exist");
        // WHEN
        String result = EncryptionUtilities.fastMD5(missingFile);
        // THEN
        assertNull(result, "fastMD5 should return null for a missing file");
    }

    /**
     * Verify that fastMD5 returns {@code null} when an {@link IOException}
     * is thrown while opening the file stream.
     */
    @Test
    void fastMD5_returnsNull_whenIOExceptionOccurs() throws Exception {
        // GIVEN – a File object (its existence is irrelevant because we will mock Files.newInputStream)
        File anyFile = new File("anyfile.tmp");
        // AND – mock static Files to throw an IOException
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.newInputStream(anyFile.toPath())).thenThrow(new IOException("Simulated I/O failure"));
            // WHEN
            String result = EncryptionUtilities.fastMD5(anyFile);
            // THEN
            assertNull(result, "fastMD5 should return null when an IOException is encountered");
        }
    }

    /**
     * Verify that when the {@link InputStream} returned by {@code Files.newInputStream}
     * is a {@link FileInputStream}, {@code fastMD5} delegates to {@code calculateFileHash}.
     */
    @Test
    void fastMD5_usesCalculateFileHash_whenFileInputStream(@TempDir Path tempDir) throws Exception {
        // GIVEN – a temporary file with known content
        Path filePath = tempDir.resolve("tempFile_hello.txt");
        Files.write(filePath, "hello".getBytes());
        File file = filePath.toFile();
        // Expected hash for the string "hello"
        String expectedHash = "5d41402abc4b2a76b9719d911017c592";
        // Mock static methods of EncryptionUtilities
        try (MockedStatic<EncryptionUtilities> mocked = mockStatic(EncryptionUtilities.class)) {
            // Stub getMD5Digest() to return a real MessageDigest (used only for argument matching)
            MessageDigest realDigest = MessageDigest.getInstance("MD5");
            mocked.when(EncryptionUtilities::getMD5Digest).thenReturn(realDigest);
            // Stub calculateFileHash to return the expected hash
            mocked.when(() -> EncryptionUtilities.calculateFileHash(any(FileChannel.class), eq(realDigest))).thenReturn(expectedHash);
            // WHEN
            String actual = EncryptionUtilities.fastMD5(file);
            // THEN
            assertEquals(expectedHash, actual, "fastMD5 should return the hash supplied by calculateFileHash");
            // Verify that calculateFileHash was indeed invoked
            mocked.verify(() -> EncryptionUtilities.calculateFileHash(any(FileChannel.class), eq(realDigest)), times(1));
        }
    }

    /**
     * Verify the fallback path: when {@code Files.newInputStream} returns an
     * {@link InputStream} that is NOT a {@link FileInputStream},
     * {@code fastMD5} should call {@code calculateStreamHash}.
     */
    @Test
    void fastMD5_fallbackToCalculateStreamHash_whenNonFileInputStream(@TempDir Path tempDir) throws Exception {
        // GIVEN – a temporary file (content is irrelevant because we will mock the stream)
        Path filePath = tempDir.resolve("dummy.txt");
        Files.write(filePath, "ignored".getBytes());
        File file = filePath.toFile();
        // Mock Files.newInputStream to return a ByteArrayInputStream (not a FileInputStream)
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class);
            MockedStatic<EncryptionUtilities> mockedEnc = mockStatic(EncryptionUtilities.class)) {
            InputStream fakeStream = new ByteArrayInputStream("fallback".getBytes());
            mockedFiles.when(() -> Files.newInputStream(file.toPath())).thenReturn(fakeStream);
            // Stub getMD5Digest()
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            mockedEnc.when(EncryptionUtilities::getMD5Digest).thenReturn(md5);
            // Expected hash for the fallback content
            String expectedHash = "c0e84e870874dd37c8b0c7e5c2e0b5b0";
            mockedEnc.when(() -> EncryptionUtilities.calculateStreamHash(fakeStream, md5)).thenReturn(expectedHash);
            // WHEN
            String actual = EncryptionUtilities.fastMD5(file);
            // THEN
            assertEquals(expectedHash, actual, "fastMD5 should return the hash supplied by calculateStreamHash");
            mockedEnc.verify(() -> EncryptionUtilities.calculateStreamHash(fakeStream, md5), times(1));
        }
    }

    /**
     * Verify that passing {@code null} as the file argument results in a
     * {@link NullPointerException}. The current implementation does not
     * catch this unchecked exception, so it propagates.
     */
    @Test
    void fastMD5_nullFileArgument_throwsNullPointerException() {
        // GIVEN
        File nullFile = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.fastMD5(nullFile), "fastMD5 should throw NullPointerException when the file argument is null");
    }

    /**
     * Verify that a {@link SecurityException} thrown by {@code Files.newInputStream}
     * propagates out of {@code fastMD5}.
     */
    @Test
    void fastMD5_propagatesSecurityException() throws Exception {
        // GIVEN – a File that exists (existence does not matter because we mock the call)
        File restrictedFile = new File("restricted_file.tmp");
        // Mock Files.newInputStream to throw SecurityException
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.newInputStream(restrictedFile.toPath())).thenThrow(new SecurityException("Access denied"));
            // WHEN / THEN
            assertThrows(SecurityException.class, () -> EncryptionUtilities.fastMD5(restrictedFile), "fastMD5 should let SecurityException propagate");
        }
    }
}
