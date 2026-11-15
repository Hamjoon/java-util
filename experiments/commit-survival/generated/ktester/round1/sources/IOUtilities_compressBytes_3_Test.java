package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.zip.GZIPInputStream;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;

@ExtendWith(MockitoExtension.class)
class IOUtilities_compressBytes_3_Test {

    /**
     * Decompresses a GZIP‑encoded byte array using {@link GZIPInputStream}.
     *
     * @param gzipBytes the compressed data
     * @return the decompressed payload
     * @throws IOException if decompression fails
     */
    private static byte[] decompressGzip(byte[] gzipBytes) throws IOException {
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(gzipBytes));
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = gis.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    /**
     * Verifies that {@link IOUtilities#compressBytes(FastByteArrayOutputStream, FastByteArrayOutputStream)}
     * produces a valid GZIP stream that can be decompressed back to the original bytes.
     */
    @Test
    void compressBytes_producesValidGzip() throws IOException {
        // given
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        byte[] payload = "The quick brown fox jumps over the lazy dog".getBytes();
        original.write(payload);
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        // when
        IOUtilities.compressBytes(original, compressed);
        // then
        byte[] compressedBytes = compressed.toByteArray();
        assertNotNull(compressedBytes, "Compressed byte array should not be null");
        assertTrue(compressedBytes.length > 0, "Compressed byte array should contain data");
        // Decompress using standard GZIPInputStream to verify integrity
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressedBytes));
            ByteArrayOutputStream decompressed = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[256];
            int read;
            while ((read = gis.read(buffer)) != -1) {
                decompressed.write(buffer, 0, read);
            }
            byte[] result = decompressed.toByteArray();
            assertArrayEquals(original.toByteArray(), result, "Decompressed data should match the original payload");
        }
    }

    /**
     * Verifies that compressing an empty stream still yields a valid (non‑null, non‑empty) GZIP header.
     */
    @Test
    void compressBytes_emptyInputProducesHeader() throws IOException {
        // given
        // empty
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        // when
        IOUtilities.compressBytes(original, compressed);
        // then
        byte[] compressedBytes = compressed.toByteArray();
        assertNotNull(compressedBytes, "Compressed byte array should not be null");
        assertTrue(compressedBytes.length > 0, "Compressed byte array should contain at least the GZIP header");
        // The GZIP header starts with 0x1f 0x8b
        assertEquals((byte) 0x1f, compressedBytes[0], "First byte of GZIP header should be 0x1f");
        assertEquals((byte) 0x8b, compressedBytes[1], "Second byte of GZIP header should be 0x8b");
    }

    /**
     * CompressBytes_Normal
     */
    @Test
    void compressBytes_normal_nonEmptyStream() throws IOException {
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        byte[] data = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 };
        original.write(data);
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);
        byte[] result = decompressGzip(compressed.toByteArray());
        assertArrayEquals(data, result, "Decompressed data must equal the original bytes");
    }

    /**
     * CompressBytes_EmptyOriginal
     */
    @Test
    void compressBytes_emptyOriginal() throws IOException {
        // size 0
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);
        byte[] result = decompressGzip(compressed.toByteArray());
        assertArrayEquals(new byte[0], result, "Decompressed data should be an empty array");
    }

    /**
     * CompressBytes_NullOriginal
     */
    @Test
    void compressBytes_nullOriginal_throwsNPE() {
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        assertThrows(NullPointerException.class, () -> IOUtilities.compressBytes(null, compressed), "Passing null original should throw NullPointerException");
    }

    /**
     * CompressBytes_NullCompressed
     */
    @Test
    void compressBytes_nullCompressed_throwsNPE() {
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        original.write(new byte[] { 0x0A, 0x0B });
        assertThrows(NullPointerException.class, () -> IOUtilities.compressBytes(original, null), "Passing null compressed should throw NullPointerException");
    }

    /**
     * CompressBytes_LargeData
     */
    @Test
    void compressBytes_largePayload() throws IOException {
        // Build 1,048,576 bytes (0x00‑0xFF repeated 4096 times)
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        for (int i = 0; i < 4096; i++) {
            for (int b = 0; b < 256; b++) {
                original.write(b);
            }
        }
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);
        byte[] decompressed = decompressGzip(compressed.toByteArray());
        assertArrayEquals(original.toByteArray(), decompressed, "Decompressed large payload must match the original");
    }

    /**
     * CompressBytes_CompressedAlreadyHasData
     */
    @Test
    void compressBytes_destinationPrePopulated_overwrites() throws IOException {
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        original.write(new byte[] { (byte) 0xAA, (byte) 0xBB, (byte) 0xCC });
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        // pre‑populate with arbitrary bytes
        compressed.write(new byte[] { 0x11, 0x22 });
        IOUtilities.compressBytes(original, compressed);
        byte[] compressedBytes = compressed.toByteArray();
        // The GZIP header must be at the start of the resulting stream – the method should
        // effectively overwrite the previous content.
        assertEquals((byte) 0x1f, compressedBytes[0], "GZIP header must start at the beginning of the stream");
        assertEquals((byte) 0x8b, compressedBytes[1], "GZIP header must start at the beginning of the stream");
        byte[] decompressed = decompressGzip(compressedBytes);
        assertArrayEquals(original.toByteArray(), decompressed, "Decompressed data must equal the original bytes, ignoring any previous data");
    }

    /**
     * CompressBytes_ExceptionDuringWrite
     */
    @Test
    void compressBytes_faultyCompressed_writeThrows() {
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        original.write(new byte[] { 0x01, 0x02 });
        FastByteArrayOutputStream faultyCompressed = new FastByteArrayOutputStream() {

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                throw new IOException("Simulated write failure");
            }
        };
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.compressBytes(original, faultyCompressed), "IOException from write should be propagated");
        assertTrue(ex.getMessage().contains("Simulated write failure"));
    }

    /**
     * CompressBytes_ExceptionDuringClose
     */
    @Test
    void compressBytes_faultyCompressed_closeThrows() {
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        original.write(new byte[] { 0x05, 0x06, 0x07 });
        FastByteArrayOutputStream faultyCompressed = new FastByteArrayOutputStream() {

            @Override
            public void close() throws IOException {
                throw new IOException("Simulated close failure");
            }
        };
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.compressBytes(original, faultyCompressed), "IOException from close should be propagated");
        assertTrue(ex.getMessage().contains("Simulated close failure"));
    }

    /**
     * CompressBytes_ExceptionDuringFlush
     */
    @Test
    void compressBytes_faultyCompressed_flushThrows() {
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        original.write(new byte[] { 0x01, 0x02, 0x03 });
        FastByteArrayOutputStream faultyCompressed = new FastByteArrayOutputStream() {

            @Override
            public void flush() throws IOException {
                throw new IOException("Simulated flush failure");
            }
        };
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.compressBytes(original, faultyCompressed), "IOException from flush should be propagated");
        assertTrue(ex.getMessage().contains("Simulated flush failure"));
    }

    /**
     * CompressBytes_OriginalToByteArrayRuntimeException
     */
    @Test
    void compressBytes_originalToByteArrayRuntimeException_propagates() {
        FastByteArrayOutputStream original = new FastByteArrayOutputStream() {

            @Override
            public byte[] toByteArray() {
                throw new IllegalStateException("toByteArray broken");
            }
        };
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> IOUtilities.compressBytes(original, compressed), "Runtime exception from original.toByteArray() must propagate");
        assertTrue(ex.getMessage().contains("toByteArray broken"));
    }

    /**
     * CompressBytes_OriginalSizeRuntimeException
     */
    @Test
    void compressBytes_originalSizeRuntimeException_propagates() {
        FastByteArrayOutputStream original = new FastByteArrayOutputStream() {

            @Override
            public int size() {
                throw new IllegalStateException("size broken");
            }
        };
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> IOUtilities.compressBytes(original, compressed), "Runtime exception from original.size() must propagate");
        assertTrue(ex.getMessage().contains("size broken"));
    }
}
