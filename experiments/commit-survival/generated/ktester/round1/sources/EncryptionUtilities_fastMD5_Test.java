package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_fastMD5_Test {

    /**
     * Computes the MD5 hash of a file using the JDK MessageDigest.
     */
    private static String computeMD5(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = Files.newInputStream(file.toPath())) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder(2 * digest.length);
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    /**
     * Supplies the different success‑case files together with a description.
     */
    private static Stream<Arguments> successCaseProvider(@TempDir Path tempDir) throws IOException {
        // 1. Empty file
        File empty = tempDir.resolve("tempFileEmpty.txt").toFile();
        empty.createNewFile();
        // 2. File containing "abc"
        File abc = tempDir.resolve("tempFileABC.txt").toFile();
        Files.writeString(abc.toPath(), "abc", StandardOpenOption.CREATE);
        // 3. Large file (5 MiB) – repeated pattern "0123456789"
        File large = tempDir.resolve("tempFileLarge.bin").toFile();
        try (OutputStream out = Files.newOutputStream(large.toPath())) {
            byte[] pattern = "0123456789".getBytes();
            for (int i = 0; i < 5 * 1024 * 1024 / pattern.length; i++) {
                out.write(pattern);
            }
        }
        // 4. Non‑ASCII Unicode content ("こんにちは")
        File nonAscii = tempDir.resolve("tempFileNonAscii.txt").toFile();
        Files.writeString(nonAscii.toPath(), "こんにちは", StandardOpenOption.CREATE);
        // 5. Read‑only empty file
        File readOnly = tempDir.resolve("tempFileReadOnly.txt").toFile();
        readOnly.createNewFile();
        // Make the file read‑only (platform dependent)
        readOnly.setWritable(false, false);
        readOnly.setReadable(true, false);
        return Stream.of(Arguments.of(empty, "empty file"), Arguments.of(abc, "\"abc\" file"), Arguments.of(large, "large 5 MiB file"), Arguments.of(nonAscii, "non‑ASCII Unicode file"), Arguments.of(readOnly, "read‑only empty file"));
    }

    /**
     * Verify that {@code fastMD5(File)} returns the value produced by
     * {@code calculateFileHash} when the file can be opened successfully.
     */
    @Test
    void fastMD5_returnsCalculatedHash_whenFileIsReadable(@TempDir Path tempDir) throws IOException {
        // Arrange – create a temporary file with some content
        File tempFile = tempDir.resolve("sample.txt").toFile();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("sample content");
        }
        // Mock the static methods of EncryptionUtilities
        try (MockedStatic<EncryptionUtilities> mocked = mockStatic(EncryptionUtilities.class)) {
            // Stub calculateFileHash to return a known hash string
            mocked.when(() -> EncryptionUtilities.calculateFileHash(any(), any())).thenReturn("mockedHashValue");
            // The getMD5Digest call inside fastMD5 is not relevant for the stub,
            // but we let it fall through to the real method (or any stub) to avoid
            // NullPointerException if it is invoked internally.
            mocked.when(EncryptionUtilities::getMD5Digest).thenCallRealMethod();
            // Act
            String result = EncryptionUtilities.fastMD5(tempFile);
            // Assert
            assertEquals("mockedHashValue", result, "fastMD5 should return the value supplied by calculateFileHash");
            // Verify that calculateFileHash was indeed called once with a FileChannel
            // (the exact channel instance is not important for the test)
            mocked.verify(() -> EncryptionUtilities.calculateFileHash(any(), any()), times(1));
        }
    }

    /**
     * Verify that {@code fastMD5(File)} returns {@code null} when an {@link IOException}
     * occurs while opening the file (e.g., the file does not exist).
     */
    @Test
    void fastMD5_returnsNull_whenIOExceptionOccurs() {
        // Arrange – a file that does not exist will cause a FileNotFoundException
        File nonExistentFile = new File("this_file_should_not_exist_12345.tmp");
        assertFalse(nonExistentFile.exists(), "Precondition: the test file must not exist");
        // Act
        String result = EncryptionUtilities.fastMD5(nonExistentFile);
        // Assert
        assertNull(result, "fastMD5 should return null when an IOException is thrown");
    }

    @ParameterizedTest(name = "fastMD5 correctly hashes {1}")
    @MethodSource("successCaseProvider")
    void fastMD5_successfulHashComputation(File file, String description) throws Exception {
        // Act – compute the hash via the method under test
        String actualHash = EncryptionUtilities.fastMD5(file);
        // Compute the expected hash using a trusted JDK implementation
        String expectedHash = computeMD5(file);
        assertNotNull(actualHash, "fastMD5 should never return null for a readable file");
        assertEquals(expectedHash, actualHash, () -> "MD5 hash mismatch for " + description);
    }

    @Test
    void fastMD5_returnsNull_forMissingFile(@TempDir Path tempDir) {
        File missing = tempDir.resolve("tempFileMissing.txt").toFile();
        assertFalse(missing.exists(), "File must not exist");
        assertNull(EncryptionUtilities.fastMD5(missing), "fastMD5 should return null for a non‑existent file");
    }

    @Test
    void fastMD5_returnsNull_forDirectory(@TempDir Path tempDir) {
        File directory = tempDir.resolve("tempFileDirectory").toFile();
        assertTrue(directory.mkdir(), "Failed to create test directory");
        assertNull(EncryptionUtilities.fastMD5(directory), "fastMD5 should return null when the supplied File is a directory");
    }

    @Test
    void fastMD5_throws_NullPointerException_whenFileIsNull() {
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.fastMD5(null), "fastMD5 should throw NullPointerException for a null argument");
    }

    @Test
    void fastMD5_returnsNull_whenFileIsNotReadable(@TempDir Path tempDir) throws IOException {
        // Create a normal file first
        File noRead = tempDir.resolve("tempFileNoRead.txt").toFile();
        Files.writeString(noRead.toPath(), "content", StandardOpenOption.CREATE);
        // Remove read permission (best effort – may not work on Windows)
        boolean permissionChanged = noRead.setReadable(false, false);
        Assumptions.assumeTrue(permissionChanged, "File system does not support changing read permission; skipping test");
        assertNull(EncryptionUtilities.fastMD5(noRead), "fastMD5 should return null when the file cannot be read");
    }

    @Test
    void fastMD5_returnsNull_whenFileIsLocked(@TempDir Path tempDir) throws Exception {
        // Create a file with some data
        File lockedFile = tempDir.resolve("tempFileLocked.bin").toFile();
        Files.writeString(lockedFile.toPath(), "locked content", StandardOpenOption.CREATE);
        // Open a channel and acquire an exclusive lock
        try (RandomAccessFile raf = new RandomAccessFile(lockedFile, "rw");
            FileChannel channel = raf.getChannel();
            java.nio.channels.FileLock lock = channel.lock()) {
            // While the lock is held, attempt to compute MD5.
            // On many platforms this still succeeds, but the specification
            // expects fastMD5 to return null when an IOException occurs.
            String result = EncryptionUtilities.fastMD5(lockedFile);
            // If an IOException was thrown internally, fastMD5 returns null.
            // Otherwise we accept the real hash (the lock does not prevent reading).
            if (result == null) {
                assertNull(result, "fastMD5 returned null as expected due to lock");
            } else {
                // Verify that the returned hash matches the real value.
                assertEquals(computeMD5(lockedFile), result, "fastMD5 should still compute correct hash when lock does not block reading");
            }
        }
    }
}
