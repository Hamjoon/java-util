package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.util.zip.DataFormatException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.security.SecureRandom;
import java.util.Random;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class IOUtilities_compressBytes_3_Test {

    private static final String SAMPLE_TEXT = "The quick brown fox jumps over the lazy dog. " + "This string is deliberately long so that compression can be observed.";

    /**
     * Helper subclass that throws IOException on write(byte[],int,int).
     */
    private static class WriteFailingFastByteArrayOutputStream extends FastByteArrayOutputStream {

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            throw new IOException("Injected write failure");
        }
    }

    /**
     * Helper subclass that throws IOException on flush().
     */
    private static class FlushFailingFastByteArrayOutputStream extends FastByteArrayOutputStream {

        @Override
        public void flush() throws IOException {
            throw new IOException("Injected flush failure");
        }
    }

    /**
     * Helper subclass that throws IOException on close().
     */
    private static class CloseFailingFastByteArrayOutputStream extends FastByteArrayOutputStream {

        @Override
        public void close() throws IOException {
            throw new IOException("Injected close failure");
        }
    }

    /**
     * Helper subclass that throws IOException on toByteArray().
     */
    private static class ToByteArrayFailingFastByteArrayOutputStream extends FastByteArrayOutputStream {

        @Override
        public byte[] toByteArray() throws IOException {
            throw new IOException("Injected toByteArray failure");
        }
    }

    /**
     * Helper subclass that throws IOException on write(byte[],int,int) for the *original* stream.
     */
    private static class OriginalWriteFailingFastByteArrayOutputStream extends FastByteArrayOutputStream {

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            throw new IOException("Injected original write failure");
        }
    }

    // Example of a parameter source that could be reused for future data‑driven tests.
    private static Stream<byte[]> repetitiveDataProvider() {
        return Stream.of(new byte[0], new byte[256], new byte[1024]);
    }

    /**
     * -------------------------------------------------------------
     *  Normal usage – compress a populated FastByteArrayOutputStream
     *  and verify that the result can be decompressed back to the original
     *  byte sequence.
     *  -------------------------------------------------------------
     */
    @Test
    void testCompressBytes_successful() throws IOException {
        // given
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        original.write(SAMPLE_TEXT.getBytes());
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        // when
        IOUtilities.compressBytes(original, compressed);
        // then
        assertTrue(compressed.size() > 0, "Compressed stream should contain data");
        // The compressed size is typically smaller, but in pathological cases it can be larger.
        // We only assert that the data round‑trips correctly.
        byte[] decompressed = IOUtilities.uncompressBytes(compressed.toByteArray());
        assertArrayEquals(original.toByteArray(), decompressed, "Decompressed bytes must match the original content");
    }

    /**
     * -------------------------------------------------------------
     *  Null original argument – the method should reject nulls via
     *  Convention.throwIfNull and therefore throw an exception.
     *  -------------------------------------------------------------
     */
    @Test
    void testCompressBytes_nullOriginal_throwsException() {
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        Exception ex = assertThrows(RuntimeException.class, () -> IOUtilities.compressBytes(null, compressed));
        assertNotNull(ex.getMessage());
    }

    /**
     * -------------------------------------------------------------
     *  Null compressed argument – the method should reject nulls via
     *  Convention.throwIfNull and therefore throw an exception.
     *  -------------------------------------------------------------
     */
    @Test
    void testCompressBytes_nullCompressed_throwsException() {
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        original.write(SAMPLE_TEXT.getBytes());
        Exception ex = assertThrows(RuntimeException.class, () -> IOUtilities.compressBytes(original, null));
        assertNotNull(ex.getMessage());
    }

    /**
     * -------------------------------------------------------------
     *  Empty input – compressing an empty FastByteArrayOutputStream should
     *  produce a valid (non‑null) compressed representation that can be
     *  uncompressed back to an empty array.
     *  -------------------------------------------------------------
     */
    @Test
    void testCompressBytes_emptyInput() throws IOException {
        // no data written
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);
        assertTrue(compressed.size() > 0, "Even empty input yields a GZIP header");
        byte[] decompressed = IOUtilities.uncompressBytes(compressed.toByteArray());
        assertArrayEquals(new byte[0], decompressed, "Decompressed result of empty input must be empty");
    }

    /**
     * Small text – verify round‑trip for a short, highly compressible string.
     */
    @Test
    void testCompressBytes_smallText() throws IOException {
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        original.write("Hello World".getBytes());
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);
        byte[] roundTrip = IOUtilities.uncompressBytes(compressed.toByteArray());
        assertArrayEquals("Hello World".getBytes(), roundTrip, "Decompressed bytes must equal the original short text");
    }

    /**
     * Highly repetitive data – 1024 zero bytes should compress dramatically.
     */
    @Test
    void testCompressBytes_highlyRepetitive() throws IOException {
        // automatically filled with 0x00
        byte[] zeros = new byte[1024];
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        original.write(zeros);
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);
        assertTrue(compressed.size() < zeros.length / 2, "Compressed size should be less than half of the original for repetitive data");
        byte[] roundTrip = IOUtilities.uncompressBytes(compressed.toByteArray());
        assertArrayEquals(zeros, roundTrip, "Decompressed data must match the original zero bytes");
    }

    /**
     * Non‑compressible random data – compression should not significantly shrink the payload.
     */
    @Test
    void testCompressBytes_randomData() throws IOException {
        byte[] random = new byte[1024];
        new SecureRandom().nextBytes(random);
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        original.write(random);
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);
        // Random data may even grow a little; we only assert that it does not shrink dramatically.
        assertTrue(compressed.size() >= random.length * 0.9, "Compressed size of random data should be close to the original size");
        byte[] roundTrip = IOUtilities.uncompressBytes(compressed.toByteArray());
        assertArrayEquals(random, roundTrip, "Decompressed data must equal the original random bytes");
    }

    /**
     * Pre‑populated compressed stream – verify that data is appended, not overwritten.
     */
    @Test
    void testCompressBytes_prepopulatedCompressed() throws IOException {
        // Prepare a compressed stream that already contains the bytes of "PRE"
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        compressed.write("PRE".getBytes());
        // Original data to compress
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        original.write("ABC".getBytes());
        // Perform compression (appends after the existing "PRE")
        IOUtilities.compressBytes(original, compressed);
        byte[] full = compressed.toByteArray();
        int offset = "PRE".getBytes().length;
        int len = full.length - offset;
        // Decompress only the part that belongs to the gzip payload
        byte[] roundTrip = IOUtilities.uncompressBytes(full, offset, len);
        assertArrayEquals("ABC".getBytes(), roundTrip, "Decompressed payload after offset must equal the original bytes");
        // Ensure the prefix is still intact
        assertArrayEquals("PRE".getBytes(), java.util.Arrays.copyOfRange(full, 0, offset), "Prefix bytes must remain unchanged");
    }

    /**
     * Large repetitive payload (1 MiB) – stress test for memory usage and compression ratio.
     */
    @Test
    void testCompressBytes_largeRepetitivePayload() throws IOException {
        // 1 MiB
        int size = 1_048_576;
        byte[] pattern = new byte[] { (byte) 0xAB, (byte) 0xCD };
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = pattern[i % 2];
        }
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        original.write(data);
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);
        assertTrue(compressed.size() < 200 * 1024, "Compressed size should be well under 200 KB for a highly compressible 1 MiB payload");
        byte[] roundTrip = IOUtilities.uncompressBytes(compressed.toByteArray());
        assertArrayEquals(data, roundTrip, "Decompressed data must exactly match the original 1 MiB pattern");
    }

    /**
     * Original stream remains usable after compression – ensure it is not closed.
     */
    @Test
    void testCompressBytes_originalRemainsUsable() throws IOException {
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        original.write("Initial".getBytes());
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);
        // Write more data after compression
        original.write("More".getBytes());
        assertEquals("Initial".length() + "More".length(), original.size(), "Original stream should still accept writes after compression");
    }

    /**
     * Write IOException – compressed stream fails during gzip write.
     */
    @Test
    void testCompressBytes_writeIOExceptionPropagated() {
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        original.write("data".getBytes());
        FastByteArrayOutputStream failingCompressed = new WriteFailingFastByteArrayOutputStream();
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.compressBytes(original, failingCompressed));
        assertEquals("Injected write failure", ex.getMessage());
    }

    /**
     * toByteArray IOException – original stream fails when its byte array is requested.
     */
    @Test
    void testCompressBytes_originalToByteArrayIOExceptionPropagated() {
        FastByteArrayOutputStream original = new ToByteArrayFailingFastByteArrayOutputStream();
        // content irrelevant – toByteArray will fail
        original.write("anything".getBytes());
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.compressBytes(original, compressed));
        assertEquals("Injected toByteArray failure", ex.getMessage());
    }

    /**
     * Flush IOException – compressed stream fails on flush().
     */
    @Test
    void testCompressBytes_flushIOExceptionPropagated() {
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        original.write("flush-test".getBytes());
        FastByteArrayOutputStream failingCompressed = new FlushFailingFastByteArrayOutputStream();
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.compressBytes(original, failingCompressed));
        assertEquals("Injected flush failure", ex.getMessage());
    }

    /**
     * Close IOException – compressed stream fails when the GZIP stream is closed.
     */
    @Test
    void testCompressBytes_closeIOExceptionPropagated() {
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        original.write("close-test".getBytes());
        FastByteArrayOutputStream failingCompressed = new CloseFailingFastByteArrayOutputStream();
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.compressBytes(original, failingCompressed));
        assertEquals("Injected close failure", ex.getMessage());
    }

    /**
     * Original stream write failure – simulate a broken source that cannot be written to.
     */
    @Test
    void testCompressBytes_originalWriteFailurePropagated() {
        // The original stream throws on write; we still need some data inside it.
        // We'll first write via the superclass constructor, then replace the instance with the failing subclass.
        FastByteArrayOutputStream original = new OriginalWriteFailingFastByteArrayOutputStream();
        // Populate via the superclass method (bypassing the overridden write) using reflection.
        // This is safe for the test because we only need a non‑empty internal buffer.
        try {
            java.lang.reflect.Method writeMethod = FastByteArrayOutputStream.class.getDeclaredMethod("write", byte[].class, int.class, int.class);
            writeMethod.setAccessible(true);
            writeMethod.invoke(original, "data".getBytes(), 0, "data".length());
        } catch (Exception e) {
            fail("Failed to seed the original stream via reflection", e);
        }
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.compressBytes(original, compressed));
        assertEquals("Injected original write failure", ex.getMessage());
    }
}
