package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.io.IOException;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Consumer;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_size_Test {

    /**
     * Instance used by most tests – created fresh before each test.
     */
    private FastByteArrayOutputStream stream;

    /* --------------------------------------------------------------
     *  Helper – a tiny wrapper that mimics the static utility shown in the
     *  problem statement.  It is placed inside the test class to avoid
     *  pulling in unrelated production code.
     * -------------------------------------------------------------- */
    private static final class FastByteArrayUtil {

        static void compressBytes(FastByteArrayOutputStream original, FastByteArrayOutputStream compressed) throws IOException {
            // Minimal implementation that uses only public API of FastByteArrayOutputStream
            // (the real implementation would use DeflaterOutputStream, but that is not
            // required for exercising the size() contract).
            // Here we simply copy the original bytes to the compressed stream.
            compressed.write(original.toByteArray(), 0, original.size());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // default capacity (32)
        stream = new FastByteArrayOutputStream();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release the reference – helps the GC in large test suites
        stream = null;
    }

    /**
     * Supplies the different write scenarios together with the expected size.
     *
     * <p>Each {@link Arguments} entry contains:
     * <ul>
     *   <li>a human‑readable description (used as the test name),</li>
     *   <li>a {@link Consumer} that performs the required writes on the stream,</li>
     *   <li>the expected result of {@code size()} after the operations.</li>
     * </ul>
     * </p>
     */
    private static Stream<Arguments> provideSizeAfterWritesCases() {
        return Stream.of(// 1. Single byte via write(int)
        Arguments.of("After writing a single byte using write(int), size() should return 1.", (Consumer<FastByteArrayOutputStream>) s -> {
            try {
                s.write(1);
            } catch (IOException ignored) {
            }
        }, 1), // 2. Three sequential write(int) calls
        Arguments.of("After writing three bytes sequentially using write(int), size() should reflect the total count.", (Consumer<FastByteArrayOutputStream>) s -> {
            try {
                s.write(10);
                s.write(20);
                s.write(30);
            } catch (IOException ignored) {
            }
        }, 3), // 3. writeBytes(byte[])
        Arguments.of("Using writeBytes(byte[]) to write an entire array, size() should equal the array length.", (Consumer<FastByteArrayOutputStream>) s -> {
            try {
                s.writeBytes(new byte[] { 5, 6, 7, 8 });
            } catch (IOException ignored) {
            }
        }, 4), // 4. write(byte[], off, len)
        Arguments.of("Using write(byte[], off, len) with offset and length, size() should equal the number of bytes actually written.", (Consumer<FastByteArrayOutputStream>) s -> {
            try {
                s.write(new byte[] { 9, 10, 11, 12, 13 }, 1, 3);
            } catch (IOException ignored) {
            }
        }, 3), // 5. Write then reset
        Arguments.of("After writing some bytes and then calling reset(), size() should return to 0.", (Consumer<FastByteArrayOutputStream>) s -> {
            try {
                s.write(new byte[] { 1, 2, 3 });
                s.reset();
            } catch (IOException ignored) {
            }
        }, 0), // 6. Large array (10,000 bytes) – tests buffer growth handling
        Arguments.of("Writing a large buffer tests that size() correctly handles values beyond the initial internal buffer capacity.", (Consumer<FastByteArrayOutputStream>) s -> {
            byte[] large = new byte[10_000];
            for (int i = 0; i < large.length; i++) {
                large[i] = (byte) (i & 0xFF);
            }
            try {
                s.write(large);
            } catch (IOException ignored) {
            }
        }, // Fill with a deterministic pattern (optional)
        10_000), // 7. Negative int value (treated as unsigned)
        Arguments.of("Even if the byte value is negative (treated as unsigned), size() increments by one.", (Consumer<FastByteArrayOutputStream>) s -> {
            try {
                s.write(-1);
            } catch (IOException ignored) {
            }
        }, 1));
    }

    @Test
    void sizeIsZeroImmediatelyAfterConstruction() {
        assertEquals(0, stream.size(), "A newly constructed FastByteArrayOutputStream must report size 0");
    }

    @Test
    void sizeIncrementsAfterWritingSingleByte() throws IOException {
        // write 'A'
        stream.write(0x41);
        assertEquals(1, stream.size(), "Size should be 1 after writing a single byte");
    }

    @Test
    void sizeReflectsNumberOfBytesWrittenFromArray() throws IOException {
        byte[] data = "HelloWorld".getBytes();
        stream.write(data);
        assertEquals(data.length, stream.size(), "Size must equal the length of the written byte array");
    }

    @Test
    void sizeReflectsPartialArrayWrite() throws IOException {
        // 8 bytes
        byte[] data = "abcdefgh".getBytes();
        // write "cdef"
        stream.write(data, 2, 4);
        assertEquals(4, stream.size(), "Size should be the number of bytes written via write(byte[],off,len)");
        assertArrayEquals("cdef".getBytes(), stream.toByteArray(), "Internal buffer must contain exactly the written slice");
    }

    @Test
    void sizeResetsToZeroAfterCallingReset() throws IOException {
        stream.write("some data".getBytes());
        assertTrue(stream.size() > 0, "Pre‑condition: size must be > 0 before reset");
        stream.reset();
        assertEquals(0, stream.size(), "After reset() the size must be 0");
        assertArrayEquals(new byte[0], stream.toByteArray(), "After reset() the byte array representation must be empty");
    }

    @Test
    void sizeAfterWriteBytesMethod() throws IOException {
        // writeBytes(byte[]) is a convenience that writes the whole array
        byte[] payload = { 10, 20, 30, 40, 50 };
        stream.writeBytes(payload);
        assertEquals(payload.length, stream.size(), "Size must match the length of the array passed to writeBytes");
        assertArrayEquals(payload, stream.toByteArray(), "Content written via writeBytes must be identical to the source array");
    }

    @Test
    void sizeAfterMultipleWritesAndAutomaticBufferGrowth() throws IOException {
        // The default buffer is 32 bytes; write more than that to trigger growth.
        byte[] manyBytes = new byte[70];
        // fill with 0..69
        Arrays.setAll(manyBytes, i -> (byte) i);
        stream.write(manyBytes);
        assertEquals(manyBytes.length, stream.size(), "Size must reflect total bytes after a write that forces internal buffer expansion");
        // Verify a few random positions to ensure data integrity after growth
        assertEquals(0, stream.toByteArray()[0] & 0xFF);
        assertEquals(69, stream.toByteArray()[69] & 0xFF);
    }

    @Test
    void sizeIsUsedCorrectlyByAnExternalUtilityMethod() throws IOException {
        // Simulate the usage pattern shown in the documentation:
        // compressBytes(original, compressed)
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        original.write("CompressMe".getBytes());
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        // The utility method internally calls original.size()
        FastByteArrayUtil.compressBytes(original, compressed);
        // The compressed stream must contain *some* data (non‑empty) and its size
        // must be consistent with the byte array it returns.
        assertTrue(compressed.size() > 0, "Compressed output should not be empty");
        assertEquals(compressed.size(), compressed.toByteArray().length, "size() must be equal to the length of the byte array returned by toByteArray()");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideSizeAfterWritesCases")
    void sizeAfterWrites(String description, Consumer<FastByteArrayOutputStream> writer, int expectedSize) {
        // Use a fresh instance for each parameter set to avoid cross‑contamination.
        FastByteArrayOutputStream local = new FastByteArrayOutputStream();
        writer.accept(local);
        assertEquals(expectedSize, local.size(), () -> "Size mismatch: " + description);
    }
}
