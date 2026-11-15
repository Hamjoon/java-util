package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class IOUtilities_compressBytes_4_Test {

    /**
     * Supplies the successful test cases.
     */
    static Stream<Arguments> successfulCompressionProvider() {
        return Stream.of(// Standard case: full content
        Arguments.of("Full array compression (\"Hello World\")", "Hello World".getBytes(), 0, 11, "Hello World".getBytes()), // Partial compression: slice "World"
        Arguments.of("Partial slice (offset 6, len 5) -> \"World\"", "Hello World".getBytes(), 6, 5, "World".getBytes()), // Zero‑length request
        Arguments.of("Zero‑length request (len=0)", "Hello World".getBytes(), 0, 0, new byte[0]), // Empty source array, zero length
        Arguments.of("Empty source array, zero length", new byte[0], 0, 0, new byte[0]), // Offset at array length with zero length (legal)
        Arguments.of("Offset at array length with zero length", "ABC".getBytes(), 3, 0, new byte[0]), // Offset non‑zero, length to end
        Arguments.of("Slice from middle to end (\"ABCDEFGHIJ\", offset=3, len=7) -> \"DEFGHIJ\"", "ABCDEFGHIJ".getBytes(), 3, 7, "DEFGHIJ".getBytes()), // Offset + len exactly array length
        Arguments.of("Trailing slice (\"12345\", offset=2, len=3) -> \"345\"", "12345".getBytes(), 2, 3, "345".getBytes()), // Large random payload (≈1 MiB)
        Arguments.of("Large random payload (1_024_000 bytes)", generateRandomBytes(1_024_000), 0, 1_024_000, // expected slice is the whole array
        generateRandomBytes(// placeholder, will be replaced below
        0)));
    }

    /**
     * Helper to generate a random byte array of the requested size.
     */
    private static byte[] generateRandomBytes(int size) {
        byte[] data = new byte[size];
        // deterministic seed for reproducibility
        new Random(0x5A5A5A5AL).nextBytes(data);
        return data;
    }

    /**
     * Supplies the failure test cases.
     */
    static Stream<Arguments> failureCompressionProvider() {
        return Stream.of(// Null source array
        Arguments.of("Null source array", null, 0, 0, NullPointerException.class), // Negative offset
        Arguments.of("Negative offset (-1) with positive length", "Hello World".getBytes(), -1, 5, IndexOutOfBoundsException.class), // Offset + len exceeds array length
        Arguments.of("Offset+len exceeds length (offset=8, len=5)", "Hello World".getBytes(), 8, 5, IndexOutOfBoundsException.class), // Negative length
        Arguments.of("Negative length (-1)", "Hello".getBytes(), 0, -1, IndexOutOfBoundsException.class), // Offset greater than array length (len=0)
        Arguments.of("Offset beyond array length (offset=4, len=0) on \"ABC\"", "ABC".getBytes(), 4, 0, IndexOutOfBoundsException.class), // Empty source array with offset beyond length
        Arguments.of("Empty array with offset=1, len=0", new byte[0], 1, 0, IndexOutOfBoundsException.class), // Negative offset with zero length
        Arguments.of("Negative offset with zero length", "Hello".getBytes(), -1, 0, IndexOutOfBoundsException.class));
    }

    /**
     * Verify that compressing a byte array and then uncompressing it yields the original data.
     */
    @Test
    void compressBytes_roundTrip_success() {
        // GIVEN
        byte[] original = "The quick brown fox jumps over the lazy dog".getBytes();
        // WHEN
        byte[] compressed = IOUtilities.compressBytes(original, 0, original.length);
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        // THEN
        assertArrayEquals(original, uncompressed, "Uncompressed data should match the original");
    }

    /**
     * Verify that the offset/length parameters are honoured.
     */
    @Test
    void compressBytes_withOffsetAndLength_success() {
        // GIVEN
        byte[] prefix = "prefix".getBytes();
        byte[] payload = "payload-data".getBytes();
        byte[] suffix = "suffix".getBytes();
        // Build a combined array: prefix + payload + suffix
        byte[] combined = new byte[prefix.length + payload.length + suffix.length];
        System.arraycopy(prefix, 0, combined, 0, prefix.length);
        System.arraycopy(payload, 0, combined, prefix.length, payload.length);
        System.arraycopy(suffix, 0, combined, prefix.length + payload.length, suffix.length);
        int offset = prefix.length;
        int len = payload.length;
        // WHEN
        byte[] compressed = IOUtilities.compressBytes(combined, offset, len);
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        // THEN
        assertArrayEquals(payload, uncompressed, "Only the selected slice should be compressed and recovered");
    }

    /**
     * Passing a {@code null} byte array should result in a {@link RuntimeException}
     * that wraps the original {@link NullPointerException}.
     */
    @Test
    void compressBytes_nullArray_throwsRuntimeException() {
        // GIVEN
        byte[] nullArray = null;
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> IOUtilities.compressBytes(nullArray, 0, 0), "Compressing a null array should throw a RuntimeException");
        assertTrue(ex.getCause() instanceof NullPointerException, "Cause of the RuntimeException should be NullPointerException");
    }

    /**
     * Providing an offset/length that exceeds the bounds of the array should also
     * result in a {@link RuntimeException} wrapping the underlying {@link IndexOutOfBoundsException}.
     */
    @Test
    void compressBytes_invalidOffsetLength_throwsRuntimeException() {
        // GIVEN
        byte[] data = "abc".getBytes();
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> IOUtilities.compressBytes(data, 2, 5), "Out‑of‑bounds offset/length should cause a RuntimeException");
        assertTrue(ex.getCause() instanceof IndexOutOfBoundsException, "Cause should be IndexOutOfBoundsException");
    }

    /**
     * Zero‑length compression should return a valid (non‑null) compressed array
     * that can be uncompressed back to an empty array.
     */
    @Test
    void compressBytes_zeroLength_success() {
        // GIVEN
        byte[] data = "some data".getBytes();
        // WHEN
        byte[] compressed = IOUtilities.compressBytes(data, 0, 0);
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        // THEN
        assertEquals(0, uncompressed.length, "Uncompressed result of zero‑length input should be empty");
        assertNotNull(compressed, "Compressed result should not be null even for zero‑length input");
    }

    /**
     * Parameterised test covering all successful compression scenarios defined in the JSON spec.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulCompressionProvider")
    void compressBytes_successfulScenarios(String description, byte[] source, int offset, int len, byte[] expectedSlice) {
        // Act
        byte[] compressed = IOUtilities.compressBytes(source, offset, len);
        assertNotNull(compressed, "Compressed result must not be null");
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(expectedSlice, uncompressed, () -> "Decompressed data should match expected slice for case: " + description);
    }

    /**
     * Parameterised test covering all scenarios that must result in a {@link RuntimeException}.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("failureCompressionProvider")
    void compressBytes_failureScenarios(String description, byte[] source, int offset, int len, Class<? extends Throwable> expectedCause) {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> IOUtilities.compressBytes(source, offset, len), () -> "Expected RuntimeException for case: " + description);
        assertTrue(expectedCause.isInstance(ex.getCause()), () -> "Expected cause of type " + expectedCause.getSimpleName() + " but got " + (ex.getCause() == null ? "null" : ex.getCause().getClass().getSimpleName()));
    }
}
