package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.zip.InflaterInputStream;
import java.util.zip.Inflater;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.*;
import java.util.Random;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class IOUtilities_compressBytes_Test {

    private static final String ORIGINAL_TEXT = "The quick brown fox jumps over the lazy dog. 0123456789";

    /**
     * Simple holder for a normal‑operation test case.
     */
    private static class NormalCase {

        final byte[] data;

        final String description;

        NormalCase(byte[] data, String description) {
            this.data = data;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * A ByteArrayOutputStream that throws IOException on the requested operation.
     */
    private static class FaultyByteArrayOutputStream extends ByteArrayOutputStream {

        enum FailureMode {

            WRITE, FLUSH, CLOSE
        }

        private final FailureMode mode;

        FaultyByteArrayOutputStream(FailureMode mode) {
            this.mode = mode;
        }

        @Override
        public void write(int b) throws IOException {
            if (mode == FailureMode.WRITE)
                throw new IOException("forced write failure");
            super.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (mode == FailureMode.WRITE)
                throw new IOException("forced write failure");
            super.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            if (mode == FailureMode.FLUSH)
                throw new IOException("forced flush failure");
            super.flush();
        }

        @Override
        public void close() throws IOException {
            if (mode == FailureMode.CLOSE)
                throw new IOException("forced close failure");
            super.close();
        }
    }

    /**
     * A ByteArrayOutputStream whose writeTo method throws IOException.
     */
    private static class WriteToFailingByteArrayOutputStream extends ByteArrayOutputStream {

        @Override
        public void writeTo(OutputStream out) throws IOException {
            throw new IOException("forced writeTo failure");
        }
    }

    /**
     * Decompresses GZIP‑style data using {@link InflaterInputStream}.
     */
    private static byte[] decompress(byte[] compressed) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
            InflaterInputStream inflater = new InflaterInputStream(bais, new Inflater())) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int n;
            while ((n = inflater.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    /**
     * Generates a byte array of the given length filled with the supplied pattern.
     */
    private static byte[] patternBytes(int length, byte pattern) {
        byte[] b = new byte[length];
        for (int i = 0; i < length; i++) {
            b[i] = pattern;
        }
        return b;
    }

    /**
     * Provides normal‑operation test data for the parameterized test.
     */
    private static Stream<NormalCase> normalCasesProvider() {
        return Stream.of(// empty input
        new NormalCase(new byte[0], "empty input"), // short text "abc"
        new NormalCase("abc".getBytes(), "short text"), // 1 MiB random data
        new NormalCase(randomBytes(1 * 1024 * 1024), "1 MiB random"), // exactly 64 KB (DeflaterOutputStream internal buffer size) pattern 0xAA
        new NormalCase(patternBytes(64 * 1024, (byte) 0xAA), "64 KB pattern"));
    }

    private static byte[] randomBytes(int size) {
        byte[] b = new byte[size];
        new Random(0xDEADBEEF).nextBytes(b);
        return b;
    }

    /**
     * Verify that {@code IOUtilities.compressBytes(ByteArrayOutputStream, ByteArrayOutputStream)}
     * correctly compresses data. The test decompresses the result using a plain
     * {@link InflaterInputStream} and checks that the original bytes are recovered.
     */
    @Test
    void compressBytes_compressesAndCanBeDecompressed() throws IOException {
        // given – original data in a ByteArrayOutputStream
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write(ORIGINAL_TEXT.getBytes());
        // when – compress the bytes into a second stream
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);
        // then – the compressed stream must contain data and be smaller (not guaranteed for tiny inputs,
        // but we still assert that something was written)
        assertTrue(compressed.size() > 0, "Compressed stream should contain data");
        // Decompress using standard Java APIs (do not rely on IOUtilities.uncompressBytes to avoid
        // testing the same implementation twice)
        byte[] compressedBytes = compressed.toByteArray();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedBytes);
            InflaterInputStream inflater = new InflaterInputStream(bais, new Inflater())) {
            ByteArrayOutputStream decompressed = new ByteArrayOutputStream();
            byte[] buffer = new byte[256];
            int read;
            while ((read = inflater.read(buffer)) != -1) {
                decompressed.write(buffer, 0, read);
            }
            String result = decompressed.toString();
            assertEquals(ORIGINAL_TEXT, result, "Decompressed text must match the original");
        }
    }

    /**
     * Verify that passing an empty {@link ByteArrayOutputStream} results in a valid
     * GZIP header (i.e., the method does not throw and produces a non‑null output).
     */
    @Test
    void compressBytes_withEmptyInputProducesValidGzipHeader() throws IOException {
        // given
        ByteArrayOutputStream empty = new ByteArrayOutputStream();
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        // when
        IOUtilities.compressBytes(empty, compressed);
        // then – compressed data must be at least the size of an empty GZIP stream (10 bytes)
        assertTrue(compressed.size() >= 10, "Compressed empty stream should contain at least GZIP header");
    }

    /**
     * Ensure that the method throws a {@link NullPointerException} when either argument is {@code null}.
     */
    @Test
    void compressBytes_nullArgumentsThrowNPE() {
        ByteArrayOutputStream valid = new ByteArrayOutputStream();
        assertThrows(NullPointerException.class, () -> IOUtilities.compressBytes(null, valid));
        assertThrows(NullPointerException.class, () -> IOUtilities.compressBytes(valid, null));
    }

    /**
     * Parameterized test that checks normal operation for several payload sizes.
     * It verifies that the data can be round‑tripped and that the compressed size
     * is smaller than the original for non‑trivial inputs.
     */
    @ParameterizedTest(name = "compressBytes normal case: {0}")
    @MethodSource("normalCasesProvider")
    void compressBytes_normalOperation(NormalCase tc) throws IOException {
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write(tc.data);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);
        assertTrue(compressed.size() > 0, "Compressed output must not be empty");
        byte[] decompressed = decompress(compressed.toByteArray());
        assertArrayEquals(tc.data, decompressed, "Decompressed data must equal original for case: " + tc.description);
        // For inputs larger than a few bytes we expect compression to reduce size
        if (tc.data.length > 100) {
            assertTrue(compressed.size() < tc.data.length, "Compressed size should be smaller than original for case: " + tc.description);
        }
    }

    /**
     * Verifies that a pre‑filled destination stream is overwritten (not appended).
     */
    @Test
    void compressBytes_overwritesPrefilledDestination() throws IOException {
        // source data
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write("hello world".getBytes());
        // destination already contains three arbitrary bytes
        ByteArrayOutputStream prefilled = new ByteArrayOutputStream();
        prefilled.write(new byte[] { 0x01, 0x02, 0x03 });
        // compress
        IOUtilities.compressBytes(original, prefilled);
        // The resulting byte array must start with the GZIP magic header (0x1f,0x8b)
        byte[] result = prefilled.toByteArray();
        assertEquals((byte) 0x1f, result[0], "Result should start with GZIP magic byte 0x1f");
        assertEquals((byte) 0x8b, result[1], "Result should start with GZIP magic byte 0x8b");
        // Decompress and verify content
        byte[] decompressed = decompress(result);
        assertArrayEquals("hello world".getBytes(), decompressed, "Decompressed payload must match original string");
    }

    /**
     * Ensures that an IOException thrown while writing the source stream propagates.
     */
    @Test
    void compressBytes_propagatesIOExceptionFromOriginalWriteTo() {
        WriteToFailingByteArrayOutputStream badOriginal = new WriteToFailingByteArrayOutputStream();
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.compressBytes(badOriginal, compressed));
        assertTrue(ex.getMessage().contains("forced writeTo"), "Exception message should indicate forced failure");
    }

    /**
     * Ensures that an IOException thrown while writing to the destination stream propagates.
     */
    @Test
    void compressBytes_propagatesIOExceptionFromCompressedWrite() {
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write(new byte[] { 0x00, 0x01, 0x02 });
        FaultyByteArrayOutputStream faulty = new FaultyByteArrayOutputStream(FaultyByteArrayOutputStream.FailureMode.WRITE);
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.compressBytes(original, faulty));
        assertTrue(ex.getMessage().contains("forced write"), "Exception should be the forced write failure");
    }

    /**
     * Ensures that an IOException thrown on flush propagates.
     */
    @Test
    void compressBytes_propagatesIOExceptionOnFlush() {
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write(new byte[] { 0x10, 0x20, 0x30 });
        FaultyByteArrayOutputStream faulty = new FaultyByteArrayOutputStream(FaultyByteArrayOutputStream.FailureMode.FLUSH);
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.compressBytes(original, faulty));
        assertTrue(ex.getMessage().contains("forced flush"), "Exception should be the forced flush failure");
    }

    /**
     * Ensures that an IOException thrown on close propagates.
     */
    @Test
    void compressBytes_propagatesIOExceptionOnClose() {
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write(new byte[] { (byte) 0xAA, (byte) 0xBB });
        FaultyByteArrayOutputStream faulty = new FaultyByteArrayOutputStream(FaultyByteArrayOutputStream.FailureMode.CLOSE);
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.compressBytes(original, faulty));
        assertTrue(ex.getMessage().contains("forced close"), "Exception should be the forced close failure");
    }

    /**
     * Verifies that re‑using a ByteArrayOutputStream that has been reset (size 0)
     * still yields a valid GZIP payload.
     */
    @Test
    void compressBytes_zeroLengthOriginalAfterReuse() throws IOException {
        // First use – write something and then reset
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write("some data".getBytes());
        // size becomes 0
        original.reset();
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);
        assertTrue(compressed.size() >= 10, "Compressed empty (reset) stream should contain GZIP header");
        // Decompress and ensure we get an empty array
        byte[] decompressed = decompress(compressed.toByteArray());
        assertEquals(0, decompressed.length, "Decompressed result of empty input must be empty");
    }

    /**
     * Verifies that compressing exactly 64 KB of data (the internal buffer size of
     * {@link DeflaterOutputStream}) works without off‑by‑one errors.
     */
    @Test
    void compressBytes_exactBufferBoundary() throws IOException {
        byte[] pattern = patternBytes(64 * 1024, (byte) 0xAA);
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write(pattern);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);
        assertTrue(compressed.size() > 0, "Compressed output must not be empty");
        byte[] decompressed = decompress(compressed.toByteArray());
        assertArrayEquals(pattern, decompressed, "Decompressed bytes must equal the original 64 KB pattern");
        assertTrue(compressed.size() < pattern.length, "Compressed size should be smaller than original for highly compressible data");
    }
}
