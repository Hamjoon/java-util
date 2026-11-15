package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ByteUtilities_isGzipped_Test {

    private static Stream<Arguments> validGzipSignatureProvider() {
        return Stream.of(// exact match of magic numbers
        // placeholder, will be replaced below
        Arguments.of(new boolean[] { true, true }, true), // first byte matches, second does not
        Arguments.of(new boolean[] { true, false }, false), // first byte does not match, second matches
        Arguments.of(new boolean[] { false, true }, false), // neither matches
        Arguments.of(new boolean[] { false, false }, false));
    }

    // false → non‑matching byte.
    private static byte[] toByteArray(boolean[] flags) {
        byte[] result = new byte[flags.length];
        for (int i = 0; i < flags.length; i++) {
            if (i == 0) {
                // first byte (0x1f)
                result[i] = flags[i] ? (byte) 0x1f : (byte) 0x00;
            } else {
                // second byte (0x8b)
                result[i] = flags[i] ? (byte) 0x8b : (byte) 0x00;
            }
        }
        return result;
    }

    private static Stream<Arguments> shortArrayProvider() {
        return Stream.of(// empty array
        Arguments.of(new byte[] {}), // single‑element array
        Arguments.of(new byte[] { (byte) 0x1f }));
    }

    private static Stream<Arguments> boundaryValuesProvider() {
        return Stream.of(// 0x1e, 0x8a
        Arguments.of(new byte[] { 30, (byte) 0x8a }), // 0x20, 0x8c
        Arguments.of(new byte[] { 32, (byte) 0x8c }));
    }

    // -----------------------------------------------------------------
    private static String arrayToString(byte[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            sb.append(String.format("0x%02X", arr[i]));
            if (i < arr.length - 1)
                sb.append(", ");
        }
        sb.append(']');
        return sb.toString();
    }

    @Test
    void givenGzippedHeader_whenIsGzippedCalled_thenReturnsTrue() {
        // given: a byte array whose first two bytes match the GZIP magic numbers 0x1f, 0x8b
        byte[] gzippedHeader = new byte[] { (byte) 0x1f, (byte) 0x8b, 0x00, 0x01 };
        // when
        boolean result = ByteUtilities.isGzipped(gzippedHeader);
        // then
        assertTrue(result, "Byte array with GZIP header should be recognized as gzipped");
    }

    @Test
    void givenNonGzippedHeader_whenIsGzippedCalled_thenReturnsFalse() {
        // given: a byte array with a different header
        byte[] nonGzipped = new byte[] { (byte) 0x00, (byte) 0x00, 0x01, 0x02 };
        // when
        boolean result = ByteUtilities.isGzipped(nonGzipped);
        // then
        assertFalse(result, "Byte array without GZIP header should not be recognized as gzipped");
    }

    @Test
    void givenShortArray_whenIsGzippedCalled_thenThrowsArrayIndexOutOfBoundsException() {
        // given: an array shorter than two bytes
        byte[] shortArray = new byte[] { (byte) 0x1f };
        // when / then
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ByteUtilities.isGzipped(shortArray), "Calling isGzipped with an array shorter than 2 bytes should throw");
    }

    @Test
    void givenNullArray_whenIsGzippedCalled_thenThrowsNullPointerException() {
        // given: a null reference
        byte[] nullArray = null;
        // when / then
        assertThrows(NullPointerException.class, () -> ByteUtilities.isGzipped(nullArray), "Calling isGzipped with null should throw NullPointerException");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] bytes={0} → expected={1}")
    @MethodSource("validGzipSignatureProvider")
    void givenValidGzipSignature_whenIsGzippedCalled_thenReturnsExpected(boolean[] bytes, boolean expected) {
        // Convert primitive boolean array to byte[] (the test data is actually int values)
        byte[] input = toByteArray(bytes);
        boolean result = ByteUtilities.isGzipped(input);
        assertEquals(expected, result, () -> "Expected isGzipped to return " + expected + " for input " + arrayToString(input));
    }

    // -----------------------------------------------------------------
    @Test
    void givenSignedByteRepresentation_whenIsGzippedCalled_thenReturnsTrue() {
        // 0x8b == -117 when interpreted as a signed Java byte
        byte[] signedSignature = new byte[] { (byte) 0x1f, (byte) -117 };
        assertTrue(ByteUtilities.isGzipped(signedSignature), "Signed byte representation of the GZIP magic numbers should be recognized as gzipped");
    }

    // -----------------------------------------------------------------
    @Test
    void givenLongArrayWithCorrectHeader_whenIsGzippedCalled_thenReturnsTrue() {
        byte[] longCorrect = new byte[] { (byte) 0x1f, (byte) 0x8b, 0, 1, 2, 3, 4 };
        assertTrue(ByteUtilities.isGzipped(longCorrect), "Method should return true when the first two bytes of a longer array match the GZIP header");
    }

    // -----------------------------------------------------------------
    @Test
    void givenLongArrayWithIncorrectHeader_whenIsGzippedCalled_thenReturnsFalse() {
        byte[] longIncorrect = new byte[] { 0, 0, (byte) 0x1f, (byte) 0x8b, 5, 6 };
        assertFalse(ByteUtilities.isGzipped(longIncorrect), "Method should return false when the first two bytes do NOT match the GZIP header, even if the signature appears later");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] bytes={0} → false")
    @MethodSource("boundaryValuesProvider")
    void givenBoundaryValues_whenIsGzippedCalled_thenReturnsFalse(byte[] bytes) {
        assertFalse(ByteUtilities.isGzipped(bytes), "Boundary values close to the GZIP magic numbers should not be recognized as gzipped");
    }
}
