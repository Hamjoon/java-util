package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.Random;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class IOUtilities_compressBytes_3_Test {

    private FastByteArrayOutputStream original;

    private FastByteArrayOutputStream compressed;

    @BeforeEach
    void setupBeforeEach() {
        original = new FastByteArrayOutputStream();
        compressed = new FastByteArrayOutputStream();
        // default deterministic data – individual tests may replace these streams
        original.write("This is a test string for compression".getBytes(StandardCharsets.UTF_8));
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent
        original = null;
        compressed = null;
    }

    /* -------------------------------------------------
     *  Normal operation tests (parameterized)
     * ------------------------------------------------- */
    private static Stream<org.junit.jupiter.params.provider.Arguments> normalDataProvider() {
        // 1. Small string "Hello World"
        FastByteArrayOutputStream hello = new FastByteArrayOutputStream();
        hello.write("Hello World".getBytes(StandardCharsets.UTF_8));
        // 2. Empty stream
        FastByteArrayOutputStream empty = new FastByteArrayOutputStream();
        // 3. Large random payload (1 048 576 bytes)
        FastByteArrayOutputStream large = new FastByteArrayOutputStream();
        byte[] random = new byte[1_048_576];
        new Random(0xDEADBEEF).nextBytes(random);
        large.write(random);
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of(hello, "Hello World"), org.junit.jupiter.params.provider.Arguments.of(empty, ""), org.junit.jupiter.params.provider.Arguments.of(large, "large payload"));
    }

    @Test
    void testCompressBytesProducesCompressedDataAndCanUncompress() throws IOException {
        // GIVEN – original and compressed streams are prepared in @BeforeEach
        // WHEN
        IOUtilities.compressBytes(original, compressed);
        // THEN
        byte[] compressedBytes = compressed.toByteArray();
        assertNotNull(compressedBytes, "Compressed byte array should not be null");
        assertTrue(compressedBytes.length > 0, "Compressed byte array should contain data");
        // The compressed data must differ from the original raw data
        assertFalse(Arrays.equals(original.toByteArray(), compressedBytes), "Compressed data should not be identical to original data");
        // Decompress and verify we get back the original content
        byte[] uncompressed = IOUtilities.uncompressBytes(compressedBytes);
        assertArrayEquals(original.toByteArray(), uncompressed, "Decompressed data must match the original data");
    }

    @Test
    void testCompressBytesNullOriginalThrowsException() {
        assertThrows(NullPointerException.class, () -> IOUtilities.compressBytes(null, compressed), "Passing a null original stream should throw NullPointerException");
    }

    @Test
    void testCompressBytesNullCompressedThrowsException() {
        assertThrows(NullPointerException.class, () -> IOUtilities.compressBytes(original, null), "Passing a null compressed stream should throw NullPointerException");
    }

    @ParameterizedTest(name = "compressBytes normal operation – {1}")
    @MethodSource("normalDataProvider")
    void testCompressBytesNormalOperation(FastByteArrayOutputStream src, String description) throws IOException {
        // GIVEN – src already contains the data, compressed is fresh
        FastByteArrayOutputStream dst = new FastByteArrayOutputStream();
        // WHEN
        IOUtilities.compressBytes(src, dst);
        // THEN – compressed data must be non‑null and decompress to the original bytes
        byte[] compressedBytes = dst.toByteArray();
        assertNotNull(compressedBytes, "Compressed byte array should not be null");
        assertTrue(compressedBytes.length > 0, "Compressed byte array should contain data");
        // Decompress and verify equality
        byte[] uncompressed = IOUtilities.uncompressBytes(compressedBytes);
        assertArrayEquals(src.toByteArray(), uncompressed, "Decompressed data must match the original data for case: " + description);
    }

    /* -------------------------------------------------
     *  Exception propagation from source stream
     * ------------------------------------------------- */
    @Test
    void testCompressBytesOriginalWriteThrowsIOException() {
        // Simulate IOException during original.toByteArray() by overriding write()
        FastByteArrayOutputStream badOriginal = new FastByteArrayOutputStream() {

            @Override
            public void write(byte[] b, int off, int len) {
                throw new RuntimeException(new IOException("write failed"));
            }
        };
        // populate (won't be used)
        badOriginal.write("test".getBytes(StandardCharsets.UTF_8));
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.compressBytes(badOriginal, compressed));
        assertTrue(ex.getMessage().contains("write failed"));
    }

    @Test
    void testCompressBytesOriginalSizeThrowsIOException() {
        // Override size() to throw IOException (wrapped in RuntimeException because size() cannot declare checked)
        FastByteArrayOutputStream badOriginal = new FastByteArrayOutputStream() {

            @Override
            public int size() {
                try {
                    throw new IOException("size failed");
                } catch (IOException e) {
                    // Wrap to satisfy the method signature
                    throw new RuntimeException(e);
                }
            }
        };
        badOriginal.write("data".getBytes(StandardCharsets.UTF_8));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> IOUtilities.compressBytes(badOriginal, compressed));
        assertTrue(ex.getCause() instanceof IOException);
        assertEquals("size failed", ex.getCause().getMessage());
    }

    @Test
    void testCompressBytesOriginalToByteArrayReturnsNull() {
        FastByteArrayOutputStream nullReturningOriginal = new FastByteArrayOutputStream() {

            @Override
            public byte[] toByteArray() {
                return null;
            }
        };
        nullReturningOriginal.write("anything".getBytes(StandardCharsets.UTF_8));
        assertThrows(NullPointerException.class, () -> IOUtilities.compressBytes(nullReturningOriginal, compressed), "If original.toByteArray() returns null a NullPointerException should be thrown");
    }

    /* -------------------------------------------------
     *  Exception propagation from destination stream
     * ------------------------------------------------- */
    @Test
    void testCompressBytesCompressedFlushThrowsIOException() {
        FastByteArrayOutputStream badCompressed = new FastByteArrayOutputStream() {

            @Override
            public void flush() throws IOException {
                throw new IOException("flush failed");
            }
        };
        assertThrows(IOException.class, () -> IOUtilities.compressBytes(original, badCompressed), "IOException from flush should be propagated");
    }

    @Test
    void testCompressBytesCompressedCloseThrowsIOException() {
        FastByteArrayOutputStream badCompressed = new FastByteArrayOutputStream() {

            @Override
            public void close() throws IOException {
                throw new IOException("close failed");
            }
        };
        assertThrows(IOException.class, () -> IOUtilities.compressBytes(original, badCompressed), "IOException from close should be propagated");
    }

    /* -------------------------------------------------
     *  Resource cleanup verification
     * ------------------------------------------------- */
    @Test
    void testCompressBytesClosesCompressedStreamExactlyOnce() throws IOException {
        // Spy on a fresh stream to capture close()
        FastByteArrayOutputStream spyCompressed = spy(new FastByteArrayOutputStream());
        IOUtilities.compressBytes(original, spyCompressed);
        // Verify close() called exactly once
        verify(spyCompressed, times(1)).close();
    }

    /* -------------------------------------------------
     *  Original stream immutability
     * ------------------------------------------------- */
    @Test
    void testOriginalStreamIsNotClosedAndRemainsUnchanged() throws IOException {
        FastByteArrayOutputStream spyOriginal = spy(new FastByteArrayOutputStream());
        byte[] before = "immutable".getBytes(StandardCharsets.UTF_8);
        spyOriginal.write(before);
        IOUtilities.compressBytes(spyOriginal, compressed);
        // Original stream should never be closed
        verify(spyOriginal, never()).close();
        // Its internal bytes must stay the same
        assertArrayEquals(before, spyOriginal.toByteArray(), "Original stream's content must remain unchanged after compression");
    }
}
