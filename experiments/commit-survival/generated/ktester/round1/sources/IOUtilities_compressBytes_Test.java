package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Random;
import java.util.stream.Stream;
import java.util.zip.Inflater;

@ExtendWith(MockitoExtension.class)
class IOUtilities_compressBytes_Test {

    private static class IOExceptionOnCloseStream extends ByteArrayOutputStream {

        @Override
        public void close() throws IOException {
            throw new IOException("close failure");
        }
    }

    private static class IOExceptionOnWriteStream extends ByteArrayOutputStream {

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            throw new IOException("write failure");
        }
    }

    private static class IOExceptionOnFlushStream extends ByteArrayOutputStream {

        @Override
        public void flush() throws IOException {
            throw new IOException("flush failure");
        }
    }

    private static class IOExceptionOnWriteToOriginal extends ByteArrayOutputStream {

        @Override
        public void writeTo(OutputStream out) throws IOException {
            throw new IOException("writeTo failure");
        }
    }

    private static class RuntimeExceptionOnWriteToOriginal extends ByteArrayOutputStream {

        @Override
        public void writeTo(OutputStream out) {
            throw new IllegalStateException("writeTo failure");
        }
    }

    private static class RuntimeExceptionOnWriteOriginal extends ByteArrayOutputStream {

        @Override
        public void write(byte[] b, int off, int len) {
            throw new IllegalStateException("write failure");
        }
    }

    private static byte[] decompressGzip(byte[] compressed) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
            GZIPInputStream gzipIn = new GZIPInputStream(bais);
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ???;
            int n;
            while ((n = gzipIn.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static byte[] randomBytes(int length, long seed) {
        Random rnd = new Random(seed);
        byte[] data = new byte[length];
        rnd.nextBytes(data);
        return data;
    }

    private static Stream<Arguments> normalCompressionProvider() {
        return Stream.of(Arguments.of("Hello, World!".getBytes(StandardCharsets.UTF_8), "Normal compression – short ASCII string"), Arguments.of(randomBytes(1024, 0x1234ABCDL), "Normal compression – 1 KB of reproducible random bytes"), Arguments.of(new byte[0], "Normal compression – empty input"));
    }

    /**
     * Verify that {@code compressBytes(ByteArrayOutputStream, ByteArrayOutputStream)}
     * produces a GZIP‑compatible byte array that, when decompressed, yields the
     * original data.
     */
    @Test
    void compressBytes_producesValidGzip() throws IOException {
        // GIVEN – a source stream containing known data
        String originalText = "The quick brown fox jumps over the lazy dog";
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write(originalText.getBytes(StandardCharsets.UTF_8));
        // AND – an empty destination stream
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        // WHEN – the utility method is invoked
        IOUtilities.compressBytes(original, compressed);
        // THEN – the compressed bytes can be decompressed to the original content
        byte[] compressedBytes = compressed.toByteArray();
        assertTrue(compressedBytes.length > 0, "Compressed output should not be empty");
        // Decompress using standard JDK GZIPInputStream
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedBytes);
            GZIPInputStream gzipIn = new GZIPInputStream(bais);
            ByteArrayOutputStream decompressed = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = gzipIn.read(buffer)) != -1) {
                decompressed.write(buffer, 0, read);
            }
            String result = decompressed.toString(StandardCharsets.UTF_8.name());
            assertEquals(originalText, result, "Decompressed data must match the original");
        }
    }

    /**
     * Verify that a {@code null} original stream results in a {@link NullPointerException}.
     */
    @Test
    void compressBytes_nullOriginal_throwsNPE() {
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        assertThrows(NullPointerException.class, () -> IOUtilities.compressBytes(null, compressed), "Calling compressBytes with a null original stream should throw NPE");
    }

    /**
     * Verify that a {@code null} destination stream results in a {@link NullPointerException}.
     */
    @Test
    void compressBytes_nullCompressed_throwsNPE() throws IOException {
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write("sample".getBytes(StandardCharsets.UTF_8));
        assertThrows(NullPointerException.class, () -> IOUtilities.compressBytes(original, null), "Calling compressBytes with a null compressed stream should throw NPE");
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("normalCompressionProvider")
    void compressBytes_normalScenarios(byte[] inputData, String description) throws IOException {
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write(inputData);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);
        byte[] compressedBytes = compressed.toByteArray();
        assertTrue(compressedBytes.length > 0, "Compressed output should not be empty");
        byte[] decompressed = decompressGzip(compressedBytes);
        assertArrayEquals(inputData, decompressed, "Decompressed bytes must exactly match the original data");
    }

    @Test
    void compressBytes_highRedundancyData() throws IOException {
        byte[] data = new byte[10_000];
        for (int i = 0; i < data.length; i++) data[i] = 'A';
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write(data);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);
        byte[] decompressed = decompressGzip(compressed.toByteArray());
        assertArrayEquals(data, decompressed, "High‑redundancy data must round‑trip correctly");
    }

    @Test
    void compressBytes_nonPrintableBinary() throws IOException {
        byte[] data = new byte[] { 0x00, 0x01, 0x02 };
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write(data);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);
        byte[] decompressed = decompressGzip(compressed.toByteArray());
        assertArrayEquals(data, decompressed, "Non‑printable binary data must be preserved");
    }

    @Test
    void compressBytes_alternatingPattern() throws IOException {
        byte[] data = new byte[10 * 1024];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) ((i & 1) == 0 ? 0x00 : 0xFF);
        }
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write(data);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);
        byte[] decompressed = decompressGzip(compressed.toByteArray());
        assertArrayEquals(data, decompressed, "Alternating pattern must round‑trip correctly");
    }

    @Test
    void compressBytes_utf8MultibyteCharacters() throws IOException {
        String text = "漢字テスト";
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write(data);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);
        byte[] decompressed = decompressGzip(compressed.toByteArray());
        assertArrayEquals(data, decompressed, "UTF‑8 multibyte characters must survive compression");
    }

    @Test
    void compressBytes_largeRandomPayload() throws IOException {
        byte[] data = randomBytes(1 * 1024 * 1024, 0xDEADBEEFL);
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write(data);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);
        byte[] decompressed = decompressGzip(compressed.toByteArray());
        assertArrayEquals(data, decompressed, "Large random payload must be identical after round‑trip");
    }

    @Test
    void compressBytes_originalWriteToThrowsIOException_propagates() {
        ByteArrayOutputStream original = new IOExceptionOnWriteToOriginal();
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.compressBytes(original, compressed), "IOException from original.writeTo should propagate");
        assertTrue(ex.getMessage().contains("writeTo"));
    }

    @Test
    void compressBytes_originalWriteToThrowsRuntimeException_propagates() {
        ByteArrayOutputStream original = new RuntimeExceptionOnWriteToOriginal();
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> IOUtilities.compressBytes(original, compressed), "RuntimeException from original.writeTo should propagate");
        assertTrue(ex.getMessage().contains("writeTo"));
    }

    @Test
    void compressBytes_compressedCloseThrowsIOException_propagates() {
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        ByteArrayOutputStream compressed = new IOExceptionOnCloseStream();
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.compressBytes(original, compressed), "IOException from compressed.close() should propagate");
        assertTrue(ex.getMessage().contains("close"));
    }

    @Test
    void compressBytes_compressedWriteThrowsIOException_propagates() {
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write("test".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream compressed = new IOExceptionOnWriteStream();
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.compressBytes(original, compressed), "IOException from compressed.write should propagate");
        assertTrue(ex.getMessage().contains("write"));
    }

    @Test
    void compressBytes_compressedFlushThrowsIOException_propagates() {
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write("test".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream compressed = new IOExceptionOnFlushStream();
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.compressBytes(original, compressed), "IOException from compressed.flush() should propagate");
        assertTrue(ex.getMessage().contains("flush"));
    }

    @Test
    void compressBytes_originalWriteThrowsRuntimeException_propagates() {
        ByteArrayOutputStream original = new RuntimeExceptionOnWriteOriginal();
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> IOUtilities.compressBytes(original, compressed), "RuntimeException from original.write should propagate");
        assertTrue(ex.getMessage().contains("write"));
    }

    @Test
    void compressBytes_bothArgumentsNull_throwsNPE() {
        assertThrows(NullPointerException.class, () -> IOUtilities.compressBytes(null, null), "Both arguments null should result in NPE before any I/O");
    }

    @Test
    void compressBytes_originalNullCompressedNonNull_throwsNPE() {
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        assertThrows(NullPointerException.class, () -> IOUtilities.compressBytes(null, compressed), "Null original with non‑null compressed must throw NPE");
    }

    @Test
    void compressBytes_originalNonNullCompressedNull_throwsNPE() {
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write("data".getBytes(StandardCharsets.UTF_8));
        assertThrows(NullPointerException.class, () -> IOUtilities.compressBytes(original, null), "Non‑null original with null compressed must throw NPE");
    }
}
