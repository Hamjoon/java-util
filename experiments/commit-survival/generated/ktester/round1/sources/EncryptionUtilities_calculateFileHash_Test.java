package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import java.io.*;
import java.nio.channels.*;
import java.util.Random;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_calculateFileHash_Test {

    /**
     * Temporary directory used for creating test files.
     */
    private static File tempDir;

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        // Create a temporary directory that will be cleaned up after all tests.
        tempDir = Files.createTempDirectory("EncryptionUtilitiesTest").toFile();
        tempDir.deleteOnExit();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Recursively delete the temporary directory and its contents.
        deleteRecursively(tempDir);
    }

    /**
     * Helper that deletes a file or directory recursively.
     */
    private static void deleteRecursively(File f) {
        if (f == null || !f.exists()) {
            return;
        }
        if (f.isDirectory()) {
            for (File child : f.listFiles()) {
                deleteRecursively(child);
            }
        }
        f.delete();
    }

    /**
     * Verifies that {@code calculateFileHash} correctly hashes a file whose
     * contents are known. The expected hash is obtained by using the same
     * {@link MessageDigest} directly and encoding the result with {@link ByteUtilities}.
     */
    @Test
    void testCalculateFileHash_WithKnownContent() throws IOException, NoSuchAlgorithmException {
        // Arrange – create a file with deterministic content.
        byte[] data = "The quick brown fox jumps over the lazy dog".getBytes();
        File testFile = new File(tempDir, "knownContent.txt");
        Files.write(testFile.toPath(), data);
        // Use a real MessageDigest (MD5 in this case) to compute the expected value.
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(data);
        String expected = ByteUtilities.encode(md.digest());
        // Act – compute the hash via the method under test.
        String actual;
        try (FileInputStream fis = new FileInputStream(testFile);
            FileChannel channel = fis.getChannel()) {
            // Need a fresh digest instance because the previous one has been used.
            MessageDigest freshDigest = MessageDigest.getInstance("MD5");
            actual = EncryptionUtilities.calculateFileHash(channel, freshDigest);
        }
        // Assert
        assertEquals(expected, actual, "Hash of the file should match the expected MD5 value");
    }

    /**
     * Verifies that an empty file yields the correct hash (the digest of zero bytes).
     */
    @Test
    void testCalculateFileHash_EmptyFile() throws IOException, NoSuchAlgorithmException {
        // Arrange – create an empty temporary file.
        File emptyFile = new File(tempDir, "empty.txt");
        assertTrue(emptyFile.createNewFile(), "Failed to create empty test file");
        // Expected hash for an empty input using SHA‑256.
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        String expected = ByteUtilities.encode(md.digest());
        // Act
        String actual;
        try (FileInputStream fis = new FileInputStream(emptyFile);
            FileChannel channel = fis.getChannel()) {
            MessageDigest freshDigest = MessageDigest.getInstance("SHA-256");
            actual = EncryptionUtilities.calculateFileHash(channel, freshDigest);
        }
        // Assert
        assertEquals(expected, actual, "Hash of an empty file should match the digest of zero bytes");
    }

    /**
     * Verifies that the method correctly handles a channel that returns 0 bytes
     * on a read call (simulated by a very small buffer size). The implementation
     * should simply continue reading until -1 is returned.
     */
    @Test
    void testCalculateFileHash_ReadZeroBytesLoop() throws IOException, NoSuchAlgorithmException {
        // Arrange – write a modest amount of data.
        byte[] data = "abc".getBytes();
        File testFile = new File(tempDir, "small.txt");
        Files.write(testFile.toPath(), data);
        // Expected hash using SHA‑1.
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(data);
        String expected = ByteUtilities.encode(md.digest());
        // Act – use a channel but wrap it so that the first read returns 0.
        String actual;
        try (FileInputStream fis = new FileInputStream(testFile);
            FileChannel originalChannel = fis.getChannel()) {
            // Create a proxy channel that forces a zero‑byte read on the first call.
            FileChannel proxy = new FileChannel() {

                private boolean firstCall = true;

                @Override
                public int read(ByteBuffer dst) throws IOException {
                    if (firstCall) {
                        firstCall = false;
                        // Simulate a non‑blocking read that returns 0 bytes.
                        return 0;
                    }
                    // Delegate to the real channel for subsequent reads.
                    return originalChannel.read(dst);
                }

                // All other abstract methods delegate to the original channel.
                @Override
                public int read(ByteBuffer dst, long position) throws IOException {
                    return originalChannel.read(dst, position);
                }

                @Override
                public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
                    return originalChannel.read(dsts, offset, length);
                }

                @Override
                public int write(ByteBuffer src) throws IOException {
                    return originalChannel.write(src);
                }

                @Override
                public int write(ByteBuffer src, long position) throws IOException {
                    return originalChannel.write(src, position);
                }

                @Override
                public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
                    return originalChannel.write(srcs, offset, length);
                }

                @Override
                public long position() throws IOException {
                    return originalChannel.position();
                }

                @Override
                public FileChannel position(long newPosition) throws IOException {
                    originalChannel.position(newPosition);
                    return this;
                }

                @Override
                public long size() throws IOException {
                    return originalChannel.size();
                }

                @Override
                public FileChannel truncate(long size) throws IOException {
                    originalChannel.truncate(size);
                    return this;
                }

                @Override
                public void force(boolean metaData) throws IOException {
                    originalChannel.force(metaData);
                }

                @Override
                public long transferTo(long position, long count, java.nio.channels.WritableByteChannel target) throws IOException {
                    return originalChannel.transferTo(position, count, target);
                }

                @Override
                public long transferFrom(java.nio.channels.ReadableByteChannel src, long position, long count) throws IOException {
                    return originalChannel.transferFrom(src, position, count);
                }

                @Override
                public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
                    return originalChannel.map(mode, position, size);
                }

                @Override
                public java.nio.channels.FileLock lock(long position, long size, boolean shared) throws IOException {
                    return originalChannel.lock(position, size, shared);
                }

                @Override
                public java.nio.channels.FileLock tryLock(long position, long size, boolean shared) throws IOException {
                    return originalChannel.tryLock(position, size, shared);
                }

                @Override
                protected void implCloseChannel() throws IOException {
                    originalChannel.close();
                }
            };
            MessageDigest freshDigest = MessageDigest.getInstance("SHA-1");
            actual = EncryptionUtilities.calculateFileHash(proxy, freshDigest);
        }
        // Assert
        assertEquals(expected, actual, "Hash should be correct even when a read returns 0 bytes initially");
    }

    /**
     * Normal operation: the channel returns data in two reads (8 bytes then 3 bytes)
     * for the string "Hello World". Verifies that the digest matches the direct
     * calculation using SHA‑256.
     */
    @Test
    void testCalculateFileHash_MultipleReads() throws Exception {
        // 11 bytes
        final byte[] content = "Hello World".getBytes();
        final MessageDigest expectedDigest = MessageDigest.getInstance("SHA-256");
        expectedDigest.update(content);
        final String expected = ByteUtilities.encode(expectedDigest.digest());
        // Mock a FileChannel that returns 8 bytes, then 3 bytes, then -1.
        FileChannel mockChannel = mock(FileChannel.class);
        Answer<Integer> answer = new Answer<Integer>() {

            private int call = 0;

            @Override
            public Integer answer(org.mockito.invocation.InvocationOnMock invocation) throws Throwable {
                ByteBuffer dst = invocation.getArgument(0);
                if (call == 0) {
                    // first 8 bytes
                    dst.put(content, 0, 8);
                    call++;
                    return 8;
                } else if (call == 1) {
                    // remaining 3 bytes
                    dst.put(content, 8, 3);
                    call++;
                    return 3;
                } else {
                    // EOF
                    return -1;
                }
            }
        };
        when(mockChannel.read(any(ByteBuffer.class))).thenAnswer(answer);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String actual = EncryptionUtilities.calculateFileHash(mockChannel, digest);
        assertEquals(expected, actual);
    }

    /**
     * Edge case: empty file with MD5 digest. The method should return the MD5 of zero bytes.
     */
    @Test
    void testCalculateFileHash_EmptyFile_MD5() throws Exception {
        File emptyFile = new File(tempDir, "emptyMD5.txt");
        assertTrue(emptyFile.createNewFile());
        MessageDigest md = MessageDigest.getInstance("MD5");
        String expected = ByteUtilities.encode(md.digest());
        try (FileInputStream fis = new FileInputStream(emptyFile);
            FileChannel channel = fis.getChannel()) {
            MessageDigest fresh = MessageDigest.getInstance("MD5");
            String actual = EncryptionUtilities.calculateFileHash(channel, fresh);
            assertEquals(expected, actual);
        }
    }

    /**
     * Channel that returns 0 repeatedly (three times) before EOF.
     * The digest should be of an empty input.
     */
    @Test
    void testCalculateFileHash_MultipleZeroReads() throws Exception {
        FileChannel mockChannel = mock(FileChannel.class);
        when(mockChannel.read(any(ByteBuffer.class))).thenReturn(0, 0, 0, -1);
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        String expected = ByteUtilities.encode(md.digest());
        String actual = EncryptionUtilities.calculateFileHash(mockChannel, md);
        assertEquals(expected, actual);
    }

    /**
     * Propagation of a checked IOException thrown by the underlying channel.
     */
    @Test
    void testCalculateFileHash_IOExceptionPropagation() throws Exception {
        FileChannel mockChannel = mock(FileChannel.class);
        when(mockChannel.read(any(ByteBuffer.class))).thenThrow(new IOException("forced IO error"));
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        IOException ex = assertThrows(IOException.class, () -> EncryptionUtilities.calculateFileHash(mockChannel, md));
        assertEquals("forced IO error", ex.getMessage());
    }

    /**
     * Propagation of an unchecked RuntimeException (e.g., ClosedChannelException) from the channel.
     */
    @Test
    void testCalculateFileHash_RuntimeExceptionPropagation() throws Exception {
        FileChannel mockChannel = mock(FileChannel.class);
        // First read returns some data, second read throws RuntimeException
        when(mockChannel.read(any(ByteBuffer.class))).thenAnswer(new Answer<Integer>() {

            private int count = 0;

            @Override
            public Integer answer(org.mockito.invocation.InvocationOnMock invocation) throws Throwable {
                ByteBuffer dst = invocation.getArgument(0);
                if (count == 0) {
                    byte[] part = "partial".getBytes();
                    dst.put(part);
                    count++;
                    return part.length;
                } else {
                    throw new IllegalStateException("forced runtime error");
                }
            }
        });
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.calculateFileHash(mockChannel, md));
        assertEquals("forced runtime error", ex.getMessage());
    }

    /**
     * If the channel returns a negative value other than -1, the method will try to
     * set the buffer limit to that negative value, causing IllegalArgumentException.
     */
    @Test
    void testCalculateFileHash_NegativeReadOtherThanMinusOne() throws Exception {
        FileChannel mockChannel = mock(FileChannel.class);
        when(mockChannel.read(any(ByteBuffer.class))).thenReturn(-2);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        assertThrows(IllegalArgumentException.class, () -> EncryptionUtilities.calculateFileHash(mockChannel, md));
    }

    /**
     * Edge case: file size exactly equals the internal buffer size (65536 bytes).
     * The method should read the file in a single iteration.
     */
    @Test
    void testCalculateFileHash_ExactBufferSize() throws Exception {
        int size = 65536;
        byte[] data = new byte[size];
        // deterministic random data
        new Random(0).nextBytes(data);
        File file = new File(tempDir, "exactBuffer.bin");
        Files.write(file.toPath(), data);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(data);
        String expected = ByteUtilities.encode(md.digest());
        try (FileInputStream fis = new FileInputStream(file);
            FileChannel channel = fis.getChannel()) {
            MessageDigest fresh = MessageDigest.getInstance("SHA-256");
            String actual = EncryptionUtilities.calculateFileHash(channel, fresh);
            assertEquals(expected, actual);
        }
    }

    /**
     * Edge case: file size is one byte larger than the buffer (65537 bytes).
     * The method should perform two reads: a full buffer and a trailing byte.
     */
    @Test
    void testCalculateFileHash_BufferPlusOne() throws Exception {
        int size = 65537;
        byte[] data = new byte[size];
        new Random(1).nextBytes(data);
        File file = new File(tempDir, "bufferPlusOne.bin");
        Files.write(file.toPath(), data);
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(data);
        String expected = ByteUtilities.encode(md.digest());
        try (FileInputStream fis = new FileInputStream(file);
            FileChannel channel = fis.getChannel()) {
            MessageDigest fresh = MessageDigest.getInstance("SHA-512");
            String actual = EncryptionUtilities.calculateFileHash(channel, fresh);
            assertEquals(expected, actual);
        }
    }

    /**
     * Channel returns -1 on the first read (empty file). Expected digest of zero bytes.
     */
    @Test
    void testCalculateFileHash_EOFImmediately() throws Exception {
        FileChannel mockChannel = mock(FileChannel.class);
        when(mockChannel.read(any(ByteBuffer.class))).thenReturn(-1);
        MessageDigest md = MessageDigest.getInstance("MD5");
        String expected = ByteUtilities.encode(md.digest());
        String actual = EncryptionUtilities.calculateFileHash(mockChannel, md);
        assertEquals(expected, actual);
    }

    /**
     * Null FileChannel argument should result in NullPointerException.
     */
    @Test
    void testCalculateFileHash_NullChannel() throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.calculateFileHash(null, md));
    }

    /**
     * Null MessageDigest argument should result in NullPointerException.
     */
    @Test
    void testCalculateFileHash_NullDigest() throws Exception {
        FileChannel mockChannel = mock(FileChannel.class);
        when(mockChannel.read(any(ByteBuffer.class))).thenReturn(-1);
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.calculateFileHash(mockChannel, null));
    }

    /**
     * Closed channel should cause a ClosedChannelException (subclass of IOException) to be propagated.
     */
    @Test
    void testCalculateFileHash_ClosedChannel() throws Exception {
        File tempFile = new File(tempDir, "closed.txt");
        Files.write(tempFile.toPath(), "data".getBytes());
        FileChannel channel;
        try (FileInputStream fis = new FileInputStream(tempFile);
            FileChannel ch = fis.getChannel()) {
            // capture reference
            channel = ch;
        }
        // channel is now closed
        MessageDigest md = MessageDigest.getInstance("MD5");
        assertThrows(ClosedChannelException.class, () -> EncryptionUtilities.calculateFileHash(channel, md));
    }

    /**
     * Verify that external manipulation of the buffer's position before the method call
     * does not affect the result. The method creates its own buffer, so this test simply
     * ensures the correct hash is produced for a known file.
     */
    @Test
    void testCalculateFileHash_BufferStateIrrelevant() throws Exception {
        byte[] data = "buffer test".getBytes();
        File file = new File(tempDir, "bufferState.txt");
        Files.write(file.toPath(), data);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(data);
        String expected = ByteUtilities.encode(md.digest());
        // Open the channel, but we won't touch its internal buffer – the method uses its own.
        try (FileInputStream fis = new FileInputStream(file);
            FileChannel channel = fis.getChannel()) {
            MessageDigest fresh = MessageDigest.getInstance("SHA-256");
            String actual = EncryptionUtilities.calculateFileHash(channel, fresh);
            assertEquals(expected, actual);
        }
    }
}
