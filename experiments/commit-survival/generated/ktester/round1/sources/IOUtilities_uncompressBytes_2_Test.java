package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Base64;
import java.util.stream.Stream;

// retained from the template; not used but harmless
@ExtendWith(MockitoExtension.class)
class IOUtilities_uncompressBytes_2_Test {

    /**
     * Helper that returns a gzip‑compressed representation of the supplied data.
     */
    private static byte[] gzip(byte[] data) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
            GZIPOutputStream gos = new GZIPOutputStream(bos)) {
            gos.write(data);
            // ensure all bytes are written
            gos.finish();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Decode a Base64 string – empty or {@code null} are handled gracefully.
     */
    private static byte[] decodeBase64(String base64) {
        return base64 == null || base64.isEmpty() ? new byte[0] : Base64.getDecoder().decode(base64);
    }

    /**
     * Valid GZIP data – both offset 0 and a non‑zero offset that still points to a full gzip stream.
     */
    private static Stream<Arguments> validGzipProvider() {
        // "Hello World"
        byte[] gzipped = decodeBase64("H4sIAAAAAAAA/8tIzcnJVwjPL8pJAQBwsR0KCAAAAA==");
        return Stream.of(Arguments.of(gzipped, 0, gzipped.length, "Hello World"), // offset 2, length 24
        Arguments.of(gzipped, 2, gzipped.length - 2, "Hello World"));
    }

    /**
     * Non‑gzip data – also tests offset handling where the slice is still non‑gzip.
     */
    private static Stream<Arguments> nonGzipProvider() {
        // "Plain Text"
        byte[] plain = decodeBase64("UGxhaW4gVGV4dA==");
        return Stream.of(Arguments.of(plain, 0, plain.length, "Plain Text"), // offset 3, length 7
        Arguments.of(plain, 3, plain.length - 3, "Plain Text"));
    }

    /**
     * Empty input – zero‑length array.
     */
    private static Stream<Arguments> emptyProvider() {
        return Stream.of(Arguments.of(new byte[0], 0, 0, ""));
    }

    /**
     * Null input – the method should throw NullPointerException when the internal
     *  ByteUtilities.isGzipped call receives a null reference.
     */
    private static Stream<Arguments> nullProvider() {
        return Stream.of(Arguments.of(null, 0, 0, NullPointerException.class));
    }

    /**
     * Negative offset or length – expect IndexOutOfBoundsException from ByteArrayInputStream.
     */
    private static Stream<Arguments> negativeParamsProvider() {
        byte[] gzipped = decodeBase64("H4sIAAAAAAAA/8tIzcnJVwjPL8pJAQBwsR0KCAAAAA==");
        return Stream.of(Arguments.of(gzipped, -1, gzipped.length, IndexOutOfBoundsException.class), Arguments.of(gzipped, 0, -5, IndexOutOfBoundsException.class));
    }

    /**
     * Offset beyond array length – also IndexOutOfBoundsException.
     */
    private static Stream<Arguments> offsetBeyondLengthProvider() {
        byte[] gzipped = decodeBase64("H4sIAAAAAAAA/8tIzcnJVwjPL8pJAQBwsR0KCAAAAA==");
        return Stream.of(Arguments.of(gzipped, 100, 0, IndexOutOfBoundsException.class));
    }

    /**
     * Length too short for a complete gzip stream – the method catches the underlying
     *  exception and re‑throws a RuntimeException.
     */
    private static Stream<Arguments> insufficientGzipLengthProvider() {
        byte[] gzipped = decodeBase64("H4sIAAAAAAAA/8tIzcnJVwjPL8pJAQBwsR0KCAAAAA==");
        return Stream.of(Arguments.of(gzipped, 0, 5, RuntimeException.class));
    }

    /**
     * GZIP data followed by extra trailing bytes – gzip stream should be read correctly.
     */
    private static Stream<Arguments> gzipWithTrailingBytesProvider() {
        // "Hello World" + two zero bytes
        byte[] gzippedWithTrailing = decodeBase64("H4sIAAAAAAAA/8tIzcnJVwjPL8pJAQBwsR0KCAAAAAABAA==");
        return Stream.of(Arguments.of(gzippedWithTrailing, 0, gzippedWithTrailing.length, "Hello World"));
    }

    /**
     * Offset+len that exceeds the underlying array – IndexOutOfBoundsException.
     */
    private static Stream<Arguments> offsetLenOutOfBoundsProvider() {
        byte[] gzipped = decodeBase64("H4sIAAAAAAAA/8tIzcnJVwjPL8pJAQBwsR0KCAAAAA==");
        return Stream.of(// 20+10 > array length (28)
        Arguments.of(gzipped, 20, 10, IndexOutOfBoundsException.class));
    }

    @Test
    @DisplayName("uncompressBytes returns original data when input is gzipped")
    void testUncompressGzippedBytes() {
        // GIVEN: a byte array that is gzipped
        byte[] original = "Hello, GZIP!".getBytes();
        byte[] gzipped = gzip(original);
        // WHEN: we call the method under test with the exact gzipped array
        byte[] result = IOUtilities.uncompressBytes(gzipped, 0, gzipped.length);
        // THEN: the result should be equal to the original uncompressed data
        assertArrayEquals(original, result);
    }

    @Test
    @DisplayName("uncompressBytes returns original array when input is not gzipped")
    void testUncompressNonGzippedBytes() {
        // GIVEN: a plain (non‑gzipped) byte array
        byte[] plain = "Just plain text".getBytes();
        // WHEN: we invoke the method
        byte[] result = IOUtilities.uncompressBytes(plain, 0, plain.length);
        // THEN: the method should return the same reference (no copy) and same content
        assertSame(plain, result);
        assertArrayEquals(plain, result);
    }

    @Test
    @DisplayName("uncompressBytes correctly handles offset and length for gzipped data inside a larger buffer")
    void testUncompressBytesWithOffsetAndLength() {
        // GIVEN: gzipped data embedded inside a larger buffer
        byte[] payload = "Embedded payload".getBytes();
        byte[] gzipped = gzip(payload);
        // Create a larger buffer with a prefix and suffix
        byte[] buffer = new byte[gzipped.length + 10];
        // prefix (5 bytes)
        System.arraycopy(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, 0, buffer, 0, 5);
        // the gzipped segment
        System.arraycopy(gzipped, 0, buffer, 5, gzipped.length);
        // suffix (5 bytes)
        System.arraycopy(new byte[] { 10, 11, 12, 13, 14 }, 0, buffer, 5 + gzipped.length, 5);
        // WHEN: we call the method with offset pointing to the start of the gzipped segment
        byte[] result = IOUtilities.uncompressBytes(buffer, 5, gzipped.length);
        // THEN: we should obtain the original payload
        assertArrayEquals(payload, result);
    }

    @Test
    @DisplayName("uncompressBytes returns the original array when offset/len point to non‑gzipped slice")
    void testUncompressBytesNonGzippedSlice() {
        // GIVEN: a buffer that contains plain text surrounded by unrelated bytes
        byte[] plain = "Plain slice".getBytes();
        byte[] buffer = new byte[plain.length + 4];
        // prefix
        System.arraycopy(new byte[] { 0, 0 }, 0, buffer, 0, 2);
        // slice we will target
        System.arraycopy(plain, 0, buffer, 2, plain.length);
        // suffix
        System.arraycopy(new byte[] { 0, 0 }, 0, buffer, 2 + plain.length, 2);
        // WHEN: we invoke the method on the slice (which is not gzipped)
        byte[] result = IOUtilities.uncompressBytes(buffer, 2, plain.length);
        // THEN: the returned array should be exactly the slice we passed (same reference)
        // because the implementation returns the original byte[] when not gzipped.
        // Since we passed a sub‑array view (the original buffer), the method will return that buffer.
        // We verify content equality and that the reference is the original buffer.
        assertSame(buffer, result);
        // Verify that the relevant portion matches the plain data
        byte[] expectedSlice = new byte[plain.length];
        System.arraycopy(buffer, 2, expectedSlice, 0, plain.length);
        assertArrayEquals(expectedSlice, result);
    }

    @ParameterizedTest(name = "[{index}] GZIP offset={1} len={2}")
    @MethodSource("validGzipProvider")
    @DisplayName("Valid GZIP input – should decompress correctly")
    void testValidGzipInput(byte[] bytes, int offset, int len, String expected) {
        byte[] result = IOUtilities.uncompressBytes(bytes, offset, len);
        assertArrayEquals(expected.getBytes(), result);
    }

    @ParameterizedTest(name = "[{index}] Non‑gzip offset={1} len={2}")
    @MethodSource("nonGzipProvider")
    @DisplayName("Non‑gzip input – should return original array unchanged")
    void testNonGzipInput(byte[] bytes, int offset, int len, String expected) {
        byte[] result = IOUtilities.uncompressBytes(bytes, offset, len);
        // The implementation returns the original array reference when not gzipped.
        assertSame(bytes, result);
        assertArrayEquals(expected.getBytes(), result);
    }

    @ParameterizedTest(name = "[{index}] Empty input")
    @MethodSource("emptyProvider")
    @DisplayName("Empty input – should return empty array")
    void testEmptyInput(byte[] bytes, int offset, int len, String expected) {
        byte[] result = IOUtilities.uncompressBytes(bytes, offset, len);
        assertEquals(0, result.length);
        assertArrayEquals(expected.getBytes(), result);
    }

    @ParameterizedTest(name = "[{index}] Null input")
    @MethodSource("nullProvider")
    @DisplayName("Null input – should throw NullPointerException")
    void testNullInput(byte[] bytes, int offset, int len, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> IOUtilities.uncompressBytes(bytes, offset, len));
    }

    @ParameterizedTest(name = "[{index}] Negative parameters")
    @MethodSource("negativeParamsProvider")
    @DisplayName("Negative offset/length – should throw IndexOutOfBoundsException")
    void testNegativeParameters(byte[] bytes, int offset, int len, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> IOUtilities.uncompressBytes(bytes, offset, len));
    }

    @ParameterizedTest(name = "[{index}] Offset beyond array length")
    @MethodSource("offsetBeyondLengthProvider")
    @DisplayName("Offset beyond array length – should throw IndexOutOfBoundsException")
    void testOffsetBeyondLength(byte[] bytes, int offset, int len, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> IOUtilities.uncompressBytes(bytes, offset, len));
    }

    @ParameterizedTest(name = "[{index}] Insufficient GZIP length")
    @MethodSource("insufficientGzipLengthProvider")
    @DisplayName("Insufficient GZIP length – should wrap underlying error in RuntimeException")
    void testInsufficientGzipLength(byte[] bytes, int offset, int len, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> IOUtilities.uncompressBytes(bytes, offset, len));
    }

    @ParameterizedTest(name = "[{index}] GZIP with trailing bytes")
    @MethodSource("gzipWithTrailingBytesProvider")
    @DisplayName("Valid GZIP with extra trailing bytes – should decompress correctly")
    void testGzipWithTrailingBytes(byte[] bytes, int offset, int len, String expected) {
        byte[] result = IOUtilities.uncompressBytes(bytes, offset, len);
        assertArrayEquals(expected.getBytes(), result);
    }

    @ParameterizedTest(name = "[{index}] Offset+len out of bounds")
    @MethodSource("offsetLenOutOfBoundsProvider")
    @DisplayName("Offset+len out of bounds – should throw IndexOutOfBoundsException")
    void testOffsetLenOutOfBounds(byte[] bytes, int offset, int len, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> IOUtilities.uncompressBytes(bytes, offset, len));
    }
}
