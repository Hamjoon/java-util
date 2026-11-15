package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class IOUtilities_compressBytes_Test {

    private static final String SAMPLE_TEXT = "This is a sample text that will be compressed and then uncompressed to verify the compressBytes method works correctly.";

    private static byte[] originalBytes;

    class ThrowingCloseByteArrayOutputStream extends ByteArrayOutputStream {

        @Override
        public void close() throws IOException {
            throw new IOException("forced close failure");
        }
    }

    class ThrowingCloseByteArrayOutputStream extends ByteArrayOutputStream {

        @Override
        public void close() throws IOException {
            throw new IOException("forced close failure");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Prepare the original byte array once for all tests
        originalBytes = SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void testCompressBytes_successfulRoundTrip() throws IOException {
        // given
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write(originalBytes);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        // when
        IOUtilities.compressBytes(original, compressed);
        // then
        byte[] compressedBytes = compressed.toByteArray();
        assertNotNull(compressedBytes, "Compressed byte array should not be null");
        assertTrue(compressedBytes.length > 0, "Compressed byte array should contain data");
        assertTrue(compressedBytes.length < originalBytes.length, "Compressed data should be smaller than the original data");
        // round‑trip verification using the public uncompressBytes method
        byte[] uncompressed = IOUtilities.uncompressBytes(compressedBytes);
        assertArrayEquals(originalBytes, uncompressed, "Uncompressed bytes must match the original bytes");
    }

    @Test
    void testCompressBytes_nullOriginal_throwsException() {
        // given
        ByteArrayOutputStream original = null;
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> IOUtilities.compressBytes(original, compressed));
        assertTrue(ex.getMessage().contains("Original ByteArrayOutputStream cannot be null"));
    }

    @Test
    void testCompressBytes_nullCompressed_throwsException() {
        // given
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        ByteArrayOutputStream compressed = null;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> IOUtilities.compressBytes(original, compressed));
        assertTrue(ex.getMessage().contains("Compressed ByteArrayOutputStream cannot be null"));
    }

    /**
     * Verify that compressing an empty {@link ByteArrayOutputStream} still produces a
     * non‑null, non‑empty GZIP payload and that decompressing it yields an empty array.
     */
    @Test
    void testCompressBytes_emptyOriginal_roundTrip() throws IOException {
        // given: an empty original stream
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        // when
        IOUtilities.compressBytes(original, compressed);
        // then
        byte[] compressedBytes = compressed.toByteArray();
        assertNotNull(compressedBytes, "Compressed bytes must not be null");
        assertTrue(compressedBytes.length > 0, "Even an empty input should produce GZIP header/footer");
        // Decompress and verify we get an empty array back
        byte[] uncompressed = IOUtilities.uncompressBytes(compressedBytes);
        assertEquals(0, uncompressed.length, "Decompressed data of an empty input should be empty");
    }

    /**
     * Ensure that the method does not modify the original stream's contents.
     * After compression the original stream should still contain the same data
     * that was written to it before the call.
     */
    @Test
    void testCompressBytes_originalUnchangedAfterCompression() throws IOException {
        // given
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write(originalBytes);
        // capture a copy
        byte[] snapshotBefore = original.toByteArray();
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        // when
        IOUtilities.compressBytes(original, compressed);
        // then
        byte[] snapshotAfter = original.toByteArray();
        assertArrayEquals(snapshotBefore, snapshotAfter, "Original ByteArrayOutputStream must remain unchanged after compression");
    }

    /**
     * Simulate an {@link IOException} thrown while closing the destination stream.
     * The {@code compressBytes} method should propagate the exception to the caller.
     */
    @Test
    void testCompressBytes_ioExceptionDuringClose_isPropagated() {
        // given: a normal original stream
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write(originalBytes);
        // a custom ByteArrayOutputStream that throws IOException on close()
        class ThrowingCloseByteArrayOutputStream extends ByteArrayOutputStream {

            @Override
            public void close() throws IOException {
                throw new IOException("forced close failure");
            }
        }
        ByteArrayOutputStream compressed = new ThrowingCloseByteArrayOutputStream();
        // when / then
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.compressBytes(original, compressed));
        assertTrue(ex.getMessage().contains("forced close failure"), "IOException message should indicate the forced failure");
    }
}
