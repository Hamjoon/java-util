package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_fastSHA1_Test {

    /* --------------------------------------------------------------------- */
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /* --------------------------------------------------------------------- */
    @Test
    void fastSHA1_validFile_returnsCorrectHash(@TempDir Path tempDir) throws IOException, NoSuchAlgorithmException {
        // Arrange – create a temporary file with known content.
        Path filePath = tempDir.resolve("sample.txt");
        byte[] content = "The quick brown fox jumps over the lazy dog".getBytes();
        Files.write(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        File file = filePath.toFile();
        // Expected SHA‑1 using the standard MessageDigest API.
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] expectedBytes = md.digest(content);
        String expectedHash = toHex(expectedBytes);
        // Act
        String actualHash = EncryptionUtilities.fastSHA1(file);
        // Assert
        assertNotNull(actualHash, "Hash should not be null for an existing file");
        assertEquals(expectedHash, actualHash, "SHA‑1 hash does not match the expected value");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void fastSHA1_nonExistingFile_returnsNull() {
        // Arrange – a File that points to a location that does not exist.
        File missingFile = new File("this_file_should_not_exist_12345.tmp");
        // Act
        String result = EncryptionUtilities.fastSHA1(missingFile);
        // Assert
        assertNull(result, "fastSHA1 should return null when the file cannot be found");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void fastSHA1_ioExceptionDuringOpen_returnsNull(@TempDir Path tempDir) throws IOException {
        // Arrange – a real file (so that the path is valid) but we will mock
        // Files.newInputStream to throw an IOException.
        Path filePath = tempDir.resolve("dummy.txt");
        Files.write(filePath, new byte[] { 0x01, 0x02 });
        File file = filePath.toFile();
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.newInputStream(any(Path.class))).thenThrow(new IOException("simulated I/O failure"));
            // Act
            String result = EncryptionUtilities.fastSHA1(file);
            // Assert
            assertNull(result, "fastSHA1 should return null when an IOException occurs");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void fastSHA1_directoryPath_returnsNull(@TempDir Path tempDir) throws IOException {
        // Arrange – an empty directory.
        Path dirPath = tempDir.resolve("emptyDir");
        Files.createDirectory(dirPath);
        File dir = dirPath.toFile();
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.newInputStream(dirPath)).thenThrow(new IOException("cannot open directory"));
            // Act
            String result = EncryptionUtilities.fastSHA1(dir);
            // Assert
            assertNull(result, "fastSHA1 should return null when a directory cannot be opened");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void fastSHA1_emptyFile_returnsEmptyHash(@TempDir Path tempDir) throws IOException, NoSuchAlgorithmException {
        // Arrange – create an empty file.
        Path emptyPath = tempDir.resolve("emptyFile.txt");
        Files.createFile(emptyPath);
        File emptyFile = emptyPath.toFile();
        // Expected SHA‑1 of zero‑length input.
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        String expected = toHex(md.digest(new byte[0]));
        // Act
        String actual = EncryptionUtilities.fastSHA1(emptyFile);
        // Assert
        assertEquals(expected, actual, "SHA‑1 of an empty file is incorrect");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void fastSHA1_largeFile_returnsCorrectHash(@TempDir Path tempDir) throws IOException, NoSuchAlgorithmException {
        // Arrange – 1 MiB file with repeating 'A' (0x41).
        Path largePath = tempDir.resolve("largeFile.bin");
        byte[] pattern = "A".getBytes();
        try (OutputStream out = Files.newOutputStream(largePath, StandardOpenOption.CREATE)) {
            for (int i = 0; i < 1024 * 1024; i++) {
                // 1 MiB
                out.write(pattern);
            }
        }
        File largeFile = largePath.toFile();
        // Expected SHA‑1 using standard MessageDigest.
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        try (InputStream in = Files.newInputStream(largePath)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
        }
        String expected = toHex(md.digest());
        // Act
        String actual = EncryptionUtilities.fastSHA1(largeFile);
        // Assert
        assertEquals(expected, actual, "SHA‑1 of the large file does not match expected value");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void fastSHA1_nullArgument_throwsNullPointerException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.fastSHA1(null), "Calling fastSHA1 with null should throw NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void fastSHA1_fallbackStreamHash_returnsCorrectHash(@TempDir Path tempDir) throws IOException, NoSuchAlgorithmException {
        // Arrange – file with known content, but we mock Files.newInputStream to return a ByteArrayInputStream.
        Path filePath = tempDir.resolve("customStreamFile.txt");
        // known content
        byte[] content = "OpenAI".getBytes();
        Files.write(filePath, content);
        File file = filePath.toFile();
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            // Return a ByteArrayInputStream (not a FileInputStream) to force the fallback branch.
            mockedFiles.when(() -> Files.newInputStream(filePath)).thenReturn(new ByteArrayInputStream(content));
            // Expected SHA‑1 using normal MessageDigest.
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            String expected = toHex(md.digest(content));
            // Act
            String actual = EncryptionUtilities.fastSHA1(file);
            // Assert
            assertEquals(expected, actual, "Fallback stream hash should match expected SHA‑1");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void fastSHA1_securityManagerDeniesRead_propagatesSecurityException(@TempDir Path tempDir) throws IOException {
        // Arrange – create a file that exists.
        Path filePath = tempDir.resolve("protected.txt");
        Files.write(filePath, "secret".getBytes());
        File file = filePath.toFile();
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.newInputStream(filePath)).thenThrow(new SecurityException("read denied"));
            // Act & Assert
            assertThrows(SecurityException.class, () -> EncryptionUtilities.fastSHA1(file), "SecurityException should propagate when read access is denied");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void fastSHA1_invalidPath_exceptionPropagates() {
        // Arrange – a File with an illegal path (contains a null character).
        File invalidFile = new File("\0invalidPath.txt");
        // Act & Assert
        assertThrows(InvalidPathException.class, () -> EncryptionUtilities.fastSHA1(invalidFile), "InvalidPathException should propagate from Files.newInputStream");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void fastSHA1_getSHA1DigestRuntimeFailure_propagates() {
        // Arrange – a normal temporary file.
        File dummy = new File("dummy.txt");
        try (MockedStatic<EncryptionUtilities> mockedUtil = mockStatic(EncryptionUtilities.class)) {
            // Let all other static methods behave normally, but make getSHA1Digest throw.
            mockedUtil.when(EncryptionUtilities::getSHA1Digest).thenThrow(new RuntimeException("simulated digest failure"));
            // For the rest of the call chain, we let the real method be used.
            mockedUtil.when(() -> EncryptionUtilities.calculateFileHash(any(), any())).thenCallRealMethod();
            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class, () -> EncryptionUtilities.fastSHA1(dummy), "RuntimeException from getSHA1Digest should propagate");
            assertTrue(ex.getMessage().contains("simulated digest failure"));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void fastSHA1_fallbackStreamReadIOException_returnsNull(@TempDir Path tempDir) throws IOException {
        // Arrange – a file that exists, but we mock the InputStream to throw on read.
        Path filePath = tempDir.resolve("faultyStream.txt");
        Files.write(filePath, "data".getBytes());
        File file = filePath.toFile();
        InputStream badStream = new InputStream() {

            @Override
            public int read() throws IOException {
                throw new IOException("simulated read failure");
            }
        };
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.newInputStream(filePath)).thenReturn(// not a FileInputStream → fallback path
            badStream);
            // Act
            String result = EncryptionUtilities.fastSHA1(file);
            // Assert
            assertNull(result, "fastSHA1 should return null when calculateStreamHash encounters an IOException");
        }
    }
}
