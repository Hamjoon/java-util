package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@org.junit.jupiter.api.extension.ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ByteUtilities_isGzipped_Test {

    /* --------------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final byte[] input;

        final boolean expected;

        final String description;

        ArgumentsHolder(byte[] input, boolean expected, String description) {
            this.input = input;
            this.expected = expected;
            this.description = description;
        }

        // JUnit 5's @MethodSource expects a single argument, so we expose this holder.
        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Boundary and Null Cases
     */
    static Stream<ArgumentsHolder> boundaryAndNullProvider() {
        return Stream.of(new ArgumentsHolder(null, false, "Null array should be treated as size 0 and return false."), new ArgumentsHolder(new byte[] {}, false, "Empty array (size 0) is smaller than the minimum GZIP header size, so false."), new ArgumentsHolder(new byte[] { 0x1F }, false, "Array with a single byte (size 1) is smaller than required 2 bytes, returns false."));
    }

    /**
     * Valid GZIP Header Cases
     */
    static Stream<ArgumentsHolder> validHeaderProvider() {
        return Stream.of(new ArgumentsHolder(new byte[] { 0x1F, (byte) 0x8B }, true, "Exact two‑byte array containing the GZIP magic numbers 0x1F (31) and 0x8B (139) should be recognised as gzipped."), new ArgumentsHolder(new byte[] { 0x1F, (byte) 0x8B, 0, 1, 2, 3 }, true, "Array longer than two bytes where the first two bytes are the GZIP header should still return true."));
    }

    /**
     * Invalid Header Cases
     */
    static Stream<ArgumentsHolder> invalidHeaderProvider() {
        return Stream.of(new ArgumentsHolder(new byte[] { 0x1F, 0x00 }, false, "First byte matches 0x1F but second byte does not match 0x8B; should return false."), new ArgumentsHolder(new byte[] { 0x00, (byte) 0x8B }, false, "Second byte matches 0x8B but first byte is incorrect; should return false."), new ArgumentsHolder(new byte[] { 0x00, 0x00, 0x1F, (byte) 0x8B }, false, "GZIP header appears later in the array but not at the start; method checks only the first two bytes, so false."));
    }

    /**
     * Signed Byte Representation Cases
     */
    static Stream<ArgumentsHolder> signedByteProvider() {
        return Stream.of(new ArgumentsHolder(new byte[] { 0x1F, (byte) -117 }, true, "Java bytes are signed; 0x8B equals -117. Providing -117 should still be recognised as the GZIP header."), new ArgumentsHolder(new byte[] { (byte) -117, 0x1F }, false, "Order matters; even though both values correspond to header bytes, they are swapped, so result is false."));
    }

    /**
     * Large Array Edge Cases
     */
    static Stream<ArgumentsHolder> largeArrayProvider() {
        // Very large array (over 1 000 bytes) with correct header
        byte[] largeGood = new byte[1200];
        largeGood[0] = 0x1F;
        largeGood[1] = (byte) 0x8B;
        // fill the rest with zeros (default)
        // Large array with incorrect header (31, 140)
        byte[] largeBad = new byte[1200];
        largeBad[0] = 0x1F;
        // 140 decimal
        largeBad[1] = (byte) 0x8C;
        return Stream.of(new ArgumentsHolder(largeGood, true, "Very large array (over 1 000 bytes) whose first two bytes form the GZIP magic header should still be identified as gzipped without scanning the whole array."), new ArgumentsHolder(largeBad, false, "Large array where the first two bytes are not the gzip header (31, 140) – the method must return false without scanning the rest."));
    }

    @Test
    void returnsFalseWhenArrayIsNull() {
        assertFalse(ByteUtilities.isGzipped(null), "Null input should be considered not gzipped");
    }

    @Test
    void returnsFalseWhenArrayLengthIsLessThanTwo() {
        assertFalse(ByteUtilities.isGzipped(new byte[] {}), "Empty array cannot be gzipped");
        assertFalse(ByteUtilities.isGzipped(new byte[] { 0x1F }), "Array of length 1 cannot be gzipped");
    }

    @Test
    void returnsTrueWhenArrayStartsWithGzipMagicNumbers() {
        byte[] gzipped = new byte[] { (byte) 0x1F, (byte) 0x8B, 0x00, 0x01 };
        assertTrue(ByteUtilities.isGzipped(gzipped), "Array beginning with 0x1F,0x8B should be recognised as gzipped");
    }

    @Test
    void returnsFalseWhenFirstTwoBytesDoNotMatchGzipMagicNumbers() {
        byte[] notGzipped1 = new byte[] { (byte) 0x1F, (byte) 0x00 };
        byte[] notGzipped2 = new byte[] { (byte) 0x00, (byte) 0x8B };
        byte[] notGzipped3 = new byte[] { (byte) 0x00, (byte) 0x00, 0x01 };
        assertFalse(ByteUtilities.isGzipped(notGzipped1), "Second byte differs from 0x8B, should not be gzipped");
        assertFalse(ByteUtilities.isGzipped(notGzipped2), "First byte differs from 0x1F, should not be gzipped");
        assertFalse(ByteUtilities.isGzipped(notGzipped3), "Both bytes differ from the gzip magic numbers");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("boundaryAndNullProvider")
    void testBoundaryAndNullCases(ArgumentsHolder holder) {
        assertEquals(holder.expected, ByteUtilities.isGzipped(holder.input), holder.description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validHeaderProvider")
    void testValidHeaderCases(ArgumentsHolder holder) {
        assertEquals(holder.expected, ByteUtilities.isGzipped(holder.input), holder.description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidHeaderProvider")
    void testInvalidHeaderCases(ArgumentsHolder holder) {
        assertEquals(holder.expected, ByteUtilities.isGzipped(holder.input), holder.description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("signedByteProvider")
    void testSignedByteRepresentationCases(ArgumentsHolder holder) {
        assertEquals(holder.expected, ByteUtilities.isGzipped(holder.input), holder.description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("largeArrayProvider")
    void testLargeArrayEdgeCases(ArgumentsHolder holder) {
        assertEquals(holder.expected, ByteUtilities.isGzipped(holder.input), holder.description);
    }
}
