package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.io.*;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class IOUtilities_uncompressBytes_2_Test {

    /* -----------------------------------------------------------------------
     *  Test data used by several test cases.
     *  ----------------------------------------------------------------------- */
    private static final byte[] PREFIX;

    private static final byte[] SUFFIX;

    private static final byte[] GZIP_HELLO_WORLD;

    /**
     * Simple DTO used by the {@link #positiveCases()} stream.
     */
    private static final class PositiveCase {

        final String name;

        final byte[] bytes;

        final int offset;

        final int len;

        // null when we expect the same reference
        final byte[] expected;

        final boolean expectSameReference;

        PositiveCase(String name, byte[] bytes, int offset, int len, byte[] expected, boolean expectSameReference) {
            this.name = name;
            this.bytes = bytes;
            this.offset = offset;
            this.len = len;
            this.expected = expected;
            this.expectSameReference = expectSameReference;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * -----------------------------------------------------------------------
     *  Helper – compress a byte array using GZIP (the same algorithm that
     *  IOUtilities.uncompressBytes expects).
     *  ----------------------------------------------------------------------- **
     */
    private static byte[] gzipCompress(byte[] data) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(data);
            gzip.finish();
            return bos.toByteArray();
        }
    }

    /* -----------------------------------------------------------------------
     *  Parameterised positive test cases (no exception expected).
     *  ----------------------------------------------------------------------- */
    private static Stream<PositiveCase> positiveCases() {
        return Stream.of(// Non‑GZIP data – should be returned unchanged (same reference)
        new PositiveCase("Non‑GZIP data", "Hello World".getBytes(StandardCharsets.UTF_8), 0, 11, // expected array is the same as input, handled specially
        null, true), // Full‑array GZIP data – should be decompressed to the original string
        new PositiveCase("Valid GZIP data with full array", GZIP_HELLO_WORLD, 0, GZIP_HELLO_WORLD.length, "Hello World".getBytes(StandardCharsets.UTF_8), false), // GZIP data with non‑zero offset/length – slice inside a larger buffer
        new PositiveCase("Valid GZIP data with non‑zero offset and length", combine(PREFIX, GZIP_HELLO_WORLD, SUFFIX), PREFIX.length, GZIP_HELLO_WORLD.length, "Hello World".getBytes(StandardCharsets.UTF_8), false), // Zero length – method should simply return an empty array
        new PositiveCase("Zero length", GZIP_HELLO_WORLD, 0, 0, new byte[0], false));
    }

    /* -----------------------------------------------------------------------
     *  Utility methods and holder classes for the parameterised tests.
     *  ----------------------------------------------------------------------- */
    private static byte[] combine(byte[] prefix, byte[] middle, byte[] suffix) {
        byte[] combined = new byte[prefix.length + middle.length + suffix.length];
        System.arraycopy(prefix, 0, combined, 0, prefix.length);
        System.arraycopy(middle, 0, combined, prefix.length, middle.length);
        System.arraycopy(suffix, 0, combined, prefix.length + middle.length, suffix.length);
        return combined;
    }

    /**
     * -----------------------------------------------------------------------
     *  Test 1 – when the supplied bytes are a GZIP stream the method must
     *  return the original (de‑compressed) content.
     *  ----------------------------------------------------------------------- **
     */
    @Test
    void uncompressBytes_whenGzipped_returnsDecompressed() throws IOException {
        // GIVEN
        byte[] original = "Hello, GZIP world!".getBytes(StandardCharsets.UTF_8);
        byte[] gzipped = gzipCompress(original);
        // WHEN
        byte[] result = IOUtilities.uncompressBytes(gzipped, 0, gzipped.length);
        // THEN
        assertArrayEquals(original, result, "The uncompressed bytes should match the original payload");
    }

    /**
     * -----------------------------------------------------------------------
     *  Test 2 – the method must honour the offset/length parameters.  We embed
     *  a valid GZIP payload inside a larger array and ask the method to work
     *  only on the slice that contains the compressed data.
     *  ----------------------------------------------------------------------- **
     */
    @Test
    void uncompressBytes_withOffsetAndLength_returnsDecompressedSlice() throws IOException {
        // GIVEN
        byte[] original = "Slice test payload".getBytes(StandardCharsets.UTF_8);
        byte[] gzipped = gzipCompress(original);
        // Build a larger buffer: [padding][gzipped][trailing]
        byte[] prefix = "AAA".getBytes(StandardCharsets.UTF_8);
        byte[] suffix = "ZZZ".getBytes(StandardCharsets.UTF_8);
        byte[] combined = new byte[prefix.length + gzipped.length + suffix.length];
        System.arraycopy(prefix, 0, combined, 0, prefix.length);
        System.arraycopy(gzipped, 0, combined, prefix.length, gzipped.length);
        System.arraycopy(suffix, 0, combined, prefix.length + gzipped.length, suffix.length);
        int offset = prefix.length;
        int len = gzipped.length;
        // WHEN
        byte[] result = IOUtilities.uncompressBytes(combined, offset, len);
        // THEN
        assertArrayEquals(original, result, "The method should correctly decompress the slice defined by offset/len");
    }

    /**
     * -----------------------------------------------------------------------
     *  Test 3 – if the supplied bytes are NOT GZIP‑compressed the method must
     *  simply return the same array instance (no copy is performed).
     *  ----------------------------------------------------------------------- **
     */
    @Test
    void uncompressBytes_whenNotGzipped_returnsSameArray() {
        // GIVEN
        byte[] notCompressed = "Plain text, no compression".getBytes(StandardCharsets.UTF_8);
        // WHEN
        byte[] result = IOUtilities.uncompressBytes(notCompressed, 0, notCompressed.length);
        // THEN
        assertSame(notCompressed, result, "For non‑gzip data the original array reference should be returned");
    }

    /**
     * -----------------------------------------------------------------------
     *  Test 4 – defensive check: passing a null array should result in a
     *  NullPointerException (the method delegates to ByteUtilities.isGzipped,
     *  which will NPE on a null argument).  This verifies the method does not
     *  swallow the exception.
     *  ----------------------------------------------------------------------- **
     */
    @Test
    void uncompressBytes_nullArray_throwsNPE() {
        assertThrows(NullPointerException.class, () -> IOUtilities.uncompressBytes(null, 0, 0), "A null input should propagate a NullPointerException");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("positiveCases")
    void uncompressBytes_positiveTests(PositiveCase tc) throws IOException {
        // WHEN
        byte[] result = IOUtilities.uncompressBytes(tc.bytes, tc.offset, tc.len);
        // THEN
        if (tc.expectSameReference) {
            // For non‑gzip input the contract says “return the original array unchanged”
            assertSame(tc.bytes, result, "Non‑gzip input must return the same array instance");
        } else {
            assertArrayEquals(tc.expected, result, "Decompressed payload does not match expected data");
        }
    }

    /* -----------------------------------------------------------------------
     *  Negative test cases – each must result in a RuntimeException (or a
     *  subclass such as IndexOutOfBoundsException, NullPointerException, etc.).
     *  ----------------------------------------------------------------------- */
    @Test
    void uncompressBytes_offsetExceedsArrayBounds_throwsRuntimeException() {
        byte[] data = { 1, 2, 3 };
        assertThrows(RuntimeException.class, () -> IOUtilities.uncompressBytes(data, 5, 1), "Offset larger than array size should cause a RuntimeException");
    }

    @Test
    void uncompressBytes_lengthExceedsArrayBounds_throwsRuntimeException() {
        assertThrows(RuntimeException.class, () -> IOUtilities.uncompressBytes(GZIP_HELLO_WORLD, 0, GZIP_HELLO_WORLD.length + 10), "Length beyond array end should cause a RuntimeException");
    }

    @Test
    void uncompressBytes_negativeOffset_throwsRuntimeException() {
        assertThrows(RuntimeException.class, () -> IOUtilities.uncompressBytes(GZIP_HELLO_WORLD, -1, 10), "Negative offset should cause a RuntimeException");
    }

    @Test
    void uncompressBytes_negativeLength_throwsRuntimeException() {
        assertThrows(RuntimeException.class, () -> IOUtilities.uncompressBytes(GZIP_HELLO_WORLD, 0, -5), "Negative length should cause a RuntimeException");
    }

    @Test
    void uncompressBytes_nullArray_throwsRuntimeException() {
        assertThrows(RuntimeException.class, () -> IOUtilities.uncompressBytes(null, 0, 0), "Null input should cause a RuntimeException (NPE wrapped)");
    }

    @Test
    void uncompressBytes_corruptedGzipHeader_throwsRuntimeException() {
        // A byte array that starts with the GZIP magic numbers but is otherwise invalid
        byte[] corrupted = new byte[] { (byte) 0x1f, (byte) 0x8b, 0, 0, 1, 2, 3 };
        assertThrows(RuntimeException.class, () -> IOUtilities.uncompressBytes(corrupted, 0, corrupted.length), "Corrupted GZIP data should result in a RuntimeException");
    }
}
