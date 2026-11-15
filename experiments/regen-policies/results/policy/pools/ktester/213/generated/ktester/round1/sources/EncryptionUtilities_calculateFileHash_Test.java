package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.NonReadableChannelException;
import java.nio.file.*;
import java.util.Arrays;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_calculateFileHash_Test {

    private static final String SAMPLE_TEXT = "Hello World! EncryptionUtilities Test";

    private Path tempFile;

    /**
     * Custom MessageDigest that throws an unchecked exception from {@code engineUpdate}.
     * This allows us to verify that such exceptions are not swallowed by {@code calculateFileHash}.
     */
    private static final class ExplodingMessageDigest extends MessageDigest {

        protected ExplodingMessageDigest() {
            super("Exploding");
        }

        @Override
        protected void engineUpdate(byte input) {
            throw new IllegalStateException("engineUpdate(byte) exploded");
        }

        @Override
        protected void engineUpdate(byte[] input, int offset, int len) {
            throw new IllegalStateException("engineUpdate(byte[],int,int) exploded");
        }

        @Override
        protected byte[] engineDigest() {
            return new byte[0];
        }

        @Override
        protected void engineReset() {
            // no‑op
        }
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Create a temporary file containing a known byte sequence for the “sample text” tests.
        tempFile = Files.createTempFile("EncryptionUtilitiesTest", ".tmp");
        try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
            fos.write(SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8));
        }
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Delete the temporary file after each test.
        if (tempFile != null) {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Helper to convert a byte array to a lower‑case hex string (the format used by ByteUtilities.encode).
     */
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    @Test
    void calculateFileHash_MD5_matchesDirectDigest() throws Exception {
        // Arrange
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        // Compute expected hash directly using the same MessageDigest.
        md.update(SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8));
        String expected = toHex(md.digest());
        // Act
        try (FileInputStream fis = new FileInputStream(tempFile.toFile());
            FileChannel channel = fis.getChannel()) {
            String actual = EncryptionUtilities.calculateFileHash(channel, EncryptionUtilities.getMD5Digest());
            // Assert
            assertEquals(expected, actual, "MD5 hash of the file should match the direct digest");
        }
    }

    @Test
    void calculateFileHash_SHA1_matchesDirectDigest() throws Exception {
        // Arrange
        MessageDigest md = EncryptionUtilities.getSHA1Digest();
        md.update(SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8));
        String expected = toHex(md.digest());
        // Act
        try (FileInputStream fis = new FileInputStream(tempFile.toFile());
            FileChannel channel = fis.getChannel()) {
            String actual = EncryptionUtilities.calculateFileHash(channel, EncryptionUtilities.getSHA1Digest());
            // Assert
            assertEquals(expected, actual, "SHA-1 hash of the file should match the direct digest");
        }
    }

    @Test
    void calculateFileHash_SHA256_matchesDirectDigest() throws Exception {
        // Arrange
        MessageDigest md = EncryptionUtilities.getSHA256Digest();
        md.update(SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8));
        String expected = toHex(md.digest());
        // Act
        try (FileInputStream fis = new FileInputStream(tempFile.toFile());
            FileChannel channel = fis.getChannel()) {
            String actual = EncryptionUtilities.calculateFileHash(channel, EncryptionUtilities.getSHA256Digest());
            // Assert
            assertEquals(expected, actual, "SHA-256 hash of the file should match the direct digest");
        }
    }

    @Test
    void calculateFileHash_SHA512_matchesDirectDigest() throws Exception {
        // Arrange
        MessageDigest md = EncryptionUtilities.getSHA512Digest();
        md.update(SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8));
        String expected = toHex(md.digest());
        // Act
        try (FileInputStream fis = new FileInputStream(tempFile.toFile());
            FileChannel channel = fis.getChannel()) {
            String actual = EncryptionUtilities.calculateFileHash(channel, EncryptionUtilities.getSHA512Digest());
            // Assert
            assertEquals(expected, actual, "SHA-512 hash of the file should match the direct digest");
        }
    }

    @Test
    void calculateFileHash_EmptyFile_returnsCorrectHash() throws Exception {
        // Create an empty file.
        Path emptyFile = Files.createTempFile("EncryptionUtilitiesEmpty", ".tmp");
        try (FileInputStream fis = new FileInputStream(emptyFile.toFile());
            FileChannel channel = fis.getChannel()) {
            MessageDigest md = EncryptionUtilities.getSHA256Digest();
            // digest of empty input
            md.update(new byte[0]);
            String expected = toHex(md.digest());
            String actual = EncryptionUtilities.calculateFileHash(channel, EncryptionUtilities.getSHA256Digest());
            assertEquals(expected, actual, "Hash of an empty file should match the digest of zero bytes");
        } finally {
            Files.deleteIfExists(emptyFile);
        }
    }

    @Test
    @DisplayName("Normal operation – small file (4 bytes) with SHA‑256")
    void calculateFileHash_smallFile_SHA256() throws Exception {
        // Arrange – create a 4‑byte file [0x01,0x02,0x03,0x04]
        Path smallFile = Files.createTempFile("calcHashSmall", ".tmp");
        byte[] data = new byte[] { 0x01, 0x02, 0x03, 0x04 };
        Files.write(smallFile, data, StandardOpenOption.WRITE);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(data);
        String expected = toHex(digest.digest());
        // Act & Assert
        try (FileChannel channel = FileChannel.open(smallFile, StandardOpenOption.READ)) {
            String actual = EncryptionUtilities.calculateFileHash(channel, MessageDigest.getInstance("SHA-256"));
            assertEquals(expected, actual);
        } finally {
            Files.deleteIfExists(smallFile);
        }
    }

    @Test
    @DisplayName("Exact buffer size (64 KB) processed in a single iteration – SHA‑1")
    void calculateFileHash_exactBufferSize_SHA1() throws Exception {
        // 64 KB of the same byte value 0x7A
        final int size = 64 * 1024;
        byte[] bytes = new byte[size];
        Arrays.fill(bytes, (byte) 0x7A);
        Path file = Files.createTempFile("exactBuffer", ".tmp");
        Files.write(file, bytes, StandardOpenOption.WRITE);
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(bytes);
        String expected = toHex(md.digest());
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            String actual = EncryptionUtilities.calculateFileHash(channel, MessageDigest.getInstance("SHA-1"));
            assertEquals(expected, actual);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    @DisplayName("File just over buffer size (65 KB) – two reads – MD5")
    void calculateFileHash_justOverBufferSize_MD5() throws Exception {
        // 65 KB = 64 KB + 1 KB (65536 + 1024 = 66560) of zero bytes
        final int size = 65 * 1024;
        // all zeros by default
        byte[] bytes = new byte[size];
        Path file = Files.createTempFile("overBuffer", ".tmp");
        Files.write(file, bytes, StandardOpenOption.WRITE);
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(bytes);
        String expected = toHex(md.digest());
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            String actual = EncryptionUtilities.calculateFileHash(channel, MessageDigest.getInstance("MD5"));
            assertEquals(expected, actual);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    @DisplayName("Null FileChannel argument should throw NullPointerException")
    void calculateFileHash_nullChannel_throwsNPE() throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.calculateFileHash(null, md));
    }

    @Test
    @DisplayName("Null MessageDigest argument should throw NullPointerException")
    void calculateFileHash_nullDigest_throwsNPE() throws Exception {
        try (FileChannel channel = FileChannel.open(tempFile, StandardOpenOption.READ)) {
            assertThrows(NullPointerException.class, () -> EncryptionUtilities.calculateFileHash(channel, null));
        }
    }

    @Test
    @DisplayName("Underlying channel throws IOException – exception propagates")
    void calculateFileHash_channelThrowsIOException_propagates() throws Exception {
        // Mock a FileChannel that throws IOException on the first read()
        FileChannel mockChannel = mock(FileChannel.class);
        when(mockChannel.read(any(ByteBuffer.class))).thenThrow(new IOException("forced I/O error"));
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        assertThrows(IOException.class, () -> EncryptionUtilities.calculateFileHash(mockChannel, md));
        verify(mockChannel).read(any(ByteBuffer.class));
    }

    @Test
    @DisplayName("Channel returns 0 then -1 – should treat as empty input")
    void calculateFileHash_zeroReadThenEof_returnsHashOfEmpty() throws Exception {
        // Mock read() to return 0 (no bytes) then -1 (EOF)
        FileChannel mockChannel = mock(FileChannel.class);
        when(mockChannel.read(any(ByteBuffer.class))).thenReturn(0).thenReturn(-1);
        MessageDigest md = MessageDigest.getInstance("MD5");
        // Expected hash of zero bytes
        String expected = toHex(md.digest());
        String actual = EncryptionUtilities.calculateFileHash(mockChannel, MessageDigest.getInstance("MD5"));
        assertEquals(expected, actual);
        verify(mockChannel, times(2)).read(any(ByteBuffer.class));
    }

    @Test
    @DisplayName("Non‑readable channel (opened write‑only) throws NonReadableChannelException")
    void calculateFileHash_writeOnlyChannel_throwsNonReadableChannelException() throws Exception {
        // Create a temporary file and open it with WRITE only
        Path file = Files.createTempFile("writeOnly", ".tmp");
        try (FileChannel writeOnly = FileChannel.open(file, StandardOpenOption.WRITE)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            assertThrows(NonReadableChannelException.class, () -> EncryptionUtilities.calculateFileHash(writeOnly, md));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    @DisplayName("Digest that throws unchecked exception propagates the exception")
    void calculateFileHash_digestThrowsUncheckedException_propagates() throws Exception {
        // Real file with a single byte
        Path file = Files.createTempFile("explodingDigest", ".tmp");
        Files.write(file, new byte[] { 0x01 }, StandardOpenOption.WRITE);
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            MessageDigest exploding = new ExplodingMessageDigest();
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.calculateFileHash(channel, exploding));
            assertTrue(ex.getMessage().contains("exploded"));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
