package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.security.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_calculateFileHash_Test {

    /**
     * Temporary file used by the tests
     */
    private File tempFile;

    /**
     * Channel opened on {@link #tempFile}
     */
    private FileChannel channel;

    /**
     * Content written to {@link #tempFile}
     */
    private static final byte[] CONTENT = "The quick brown fox jumps over the lazy dog".getBytes();

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Create a temporary file and write known content to it
        tempFile = File.createTempFile("EncryptionUtilitiesTest", ".tmp");
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            out.write(CONTENT);
        }
        // Open a channel on the temporary file – this mimics the channel that
        // fastMD5/fastSHA* obtain from a FileInputStream.
        channel = new java.io.FileInputStream(tempFile).getChannel();
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Ensure the channel is closed and the temporary file is removed
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        if (tempFile != null && tempFile.exists()) {
            assertTrue(tempFile.delete(), "Failed to delete temporary test file");
        }
    }

    /**
     * Helper that converts a byte array to a lower‑case hex string.
     */
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    static Stream<Arguments> smallFileHashProvider() {
        return Stream.of(Arguments.of("MD5 hash of small file", "MD5", "900150983cd24fb0d6963f7d28e17f72"), Arguments.of("SHA-1 hash of small file", "SHA-1", "a9993e364706816aba3e25717850c26c9cd0d89d"), Arguments.of("SHA-512 hash of small file", "SHA-512", "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a" + "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f"));
    }

    @Test
    void calculateFileHash_matchesManualDigest_SHA256() throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        // Expected hash – compute it manually using the same digest instance
        byte[] expectedBytes = md.digest(CONTENT);
        String expectedHex = toHex(expectedBytes);
        // Reset the digest for the method under test
        md.reset();
        // Call the method under test
        String actualHex = EncryptionUtilities.calculateFileHash(channel, md);
        assertEquals(expectedHex, actualHex, "Hash calculated via calculateFileHash should match manual digest");
    }

    @Test
    void calculateFileHash_matchesManualDigest_MD5() throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] expectedBytes = md.digest(CONTENT);
        String expectedHex = toHex(expectedBytes);
        md.reset();
        String actualHex = EncryptionUtilities.calculateFileHash(channel, md);
        assertEquals(expectedHex, actualHex, "MD5 hash calculated via calculateFileHash should match manual digest");
    }

    @Test
    void calculateFileHash_handlesEmptyFile() throws Exception {
        // Replace the file with an empty one
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            // write nothing – file is now empty
        }
        // Re‑open the channel to reflect the new (empty) file size
        channel.close();
        channel = new java.io.FileInputStream(tempFile).getChannel();
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        // digest of zero‑length input
        byte[] expectedBytes = md.digest();
        String expectedHex = toHex(expectedBytes);
        md.reset();
        String actualHex = EncryptionUtilities.calculateFileHash(channel, md);
        assertEquals(expectedHex, actualHex, "Hash of an empty file should equal digest of zero‑length input");
    }

    @Test
    void calculateFileHash_throwsWhenChannelIsNull() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.calculateFileHash(null, md), "Passing a null FileChannel should result in NullPointerException");
    }

    @Test
    void calculateFileHash_throwsWhenDigestIsNull() {
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.calculateFileHash(channel, null), "Passing a null MessageDigest should result in NullPointerException");
    }

    /**
     * Parameterized test for small files (content "abc") with different algorithms.
     *
     * <p>Verifies that the method correctly processes files that are much smaller
     * than the internal 64 KB buffer and that the provided MessageDigest is used.</p>
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("smallFileHashProvider")
    void calculateFileHash_smallFile_correctHash(String description, String algorithm, String expectedHex) throws Exception {
        // Create a temporary file containing exactly "abc"
        File smallFile = File.createTempFile("small", ".tmp");
        try (FileOutputStream out = new FileOutputStream(smallFile)) {
            out.write("abc".getBytes());
        }
        // Open a read‑only channel on the file
        try (FileChannel ch = new FileInputStream(smallFile).getChannel()) {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            String actualHex = EncryptionUtilities.calculateFileHash(ch, md);
            assertEquals(expectedHex, actualHex, () -> "Hash mismatch for algorithm " + algorithm);
        } finally {
            assertTrue(smallFile.delete(), "Failed to delete temporary small file");
        }
    }

    /**
     * Verifies that a closed channel causes a {@link ClosedChannelException}
     * (a subclass of {@link IOException}) to be propagated.
     */
    @Test
    void calculateFileHash_closedChannel_throwsClosedChannelException() throws Exception {
        // Open a channel and close it immediately
        FileChannel closedChannel = new FileInputStream(tempFile).getChannel();
        closedChannel.close();
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        assertThrows(ClosedChannelException.class, () -> EncryptionUtilities.calculateFileHash(closedChannel, md), "Closed channel should cause ClosedChannelException");
    }

    /**
     * Verifies that a write‑only channel (obtained from {@link FileOutputStream})
     * throws {@link NonReadableChannelException} when read is attempted.
     */
    @Test
    void calculateFileHash_writeOnlyChannel_throwsNonReadableChannelException() throws Exception {
        // Obtain a write‑only channel
        FileOutputStream fos = new FileOutputStream(tempFile);
        FileChannel writeOnlyChannel = fos.getChannel();
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        assertThrows(NonReadableChannelException.class, () -> EncryptionUtilities.calculateFileHash(writeOnlyChannel, md), "Write‑only channel should cause NonReadableChannelException");
        // Clean‑up
        // also closes the channel
        fos.close();
    }

    /**
     * Simulates an asynchronous close while the method is reading a large file.
     *
     * <p>The test creates a file larger than the internal buffer (e.g., 2 MiB),
     * starts a background thread that closes the channel after a short delay,
     * and asserts that an {@link AsynchronousCloseException} is thrown.</p>
     */
    @Test
    void calculateFileHash_asynchronousClose_throwsAsynchronousCloseException() throws Exception {
        // Create a file larger than 64 KB (2 MiB)
        final int size = 2 * 1024 * 1024;
        byte[] largeContent = new byte[size];
        // Fill with deterministic data to avoid compression tricks
        for (int i = 0; i < size; i++) {
            largeContent[i] = (byte) (i & 0xFF);
        }
        File largeFile = File.createTempFile("large", ".tmp");
        try (FileOutputStream out = new FileOutputStream(largeFile)) {
            out.write(largeContent);
        }
        // Open a channel that will be closed asynchronously
        final FileChannel asyncChannel = new FileInputStream(largeFile).getChannel();
        // Thread that closes the channel after a tiny pause
        Thread closer = new Thread(() -> {
            try {
                // Give the main thread a chance to start reading
                Thread.sleep(10);
                asyncChannel.close();
            } catch (Exception ignored) {
            }
        });
        closer.start();
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        // The method should propagate AsynchronousCloseException (or its superclass IOException)
        assertThrows(AsynchronousCloseException.class, () -> EncryptionUtilities.calculateFileHash(asyncChannel, md), "Asynchronous close should result in AsynchronousCloseException");
        // Ensure the helper thread finishes
        closer.join();
        // Clean‑up
        assertTrue(largeFile.delete(), "Failed to delete temporary large file");
    }

    /**
     * Verifies that a {@link SecurityException} is propagated when the file
     * cannot be read due to OS‑level permission restrictions.
     *
     * <p>This test is only executed on POSIX‑compatible platforms where file
     * permissions can be manipulated.</p>
     */
    @Test
    void calculateFileHash_readPermissionDenied_throwsSecurityException() throws Exception {
        // Skip test on non‑POSIX platforms (e.g., Windows)
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"), "POSIX file permissions not supported on this platform");
        // Create a temporary file with some content
        File restrictedFile = File.createTempFile("restricted", ".tmp");
        try (FileOutputStream out = new FileOutputStream(restrictedFile)) {
            out.write(CONTENT);
        }
        // Remove read permission for the owner
        Path path = restrictedFile.toPath();
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("---rw----"));
        // Open a channel – opening succeeds because we have the file descriptor,
        // but reading should fail due to permission loss.
        try (FileChannel ch = new FileInputStream(restrictedFile).getChannel()) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            assertThrows(SecurityException.class, () -> EncryptionUtilities.calculateFileHash(ch, md), "Reading a file without read permission should throw SecurityException");
        } finally {
            // Restore permissions so the file can be deleted
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"));
            assertTrue(restrictedFile.delete(), "Failed to delete temporary restricted file");
        }
    }

    /**
     * Additional sanity check: an empty file that returns -1 on the first read
     * (covers the "Empty file – immediate EOF" JSON group). This is effectively
     * the same as {@link #calculateFileHash_handlesEmptyFile()}, but expressed
     * explicitly with the expected SHA‑256 hash.
     */
    @Test
    void calculateFileHash_emptyFileImmediateEof_returnsEmptyHash() throws Exception {
        // Ensure the temporary file is empty
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            // write nothing
        }
        channel.close();
        channel = new FileInputStream(tempFile).getChannel();
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        String actual = EncryptionUtilities.calculateFileHash(channel, md);
        String expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        assertEquals(expected, actual, "SHA‑256 hash of an empty file should match the well‑known empty‑hash value");
    }
}
