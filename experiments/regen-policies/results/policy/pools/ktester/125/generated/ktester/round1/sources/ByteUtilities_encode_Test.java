package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

// retained from the template; no mocks are required here
@ExtendWith(MockitoExtension.class)
class ByteUtilities_encode_Test {

    /**
     * Sample data used by several tests
     */
    private byte[] sampleArray;

    /**
     * Expected hexadecimal representation of {@link #sampleArray}
     */
    private String expectedHex;

    /**
     * Empty byte array and its expected representation
     */
    private byte[] emptyArray;

    private String emptyHex;

    @BeforeEach
    void setupBeforeEach() {
        // Typical mixed‑sign values (the same values used in the original test suite)
        sampleArray = new byte[] { -1, 0, 0x01, 0x23, 0x45, 0x67 };
        expectedHex = "FF0001234567";
        // Edge case: empty input
        emptyArray = new byte[0];
        emptyHex = "";
    }

    private static Stream<Arguments> provideEncodeByteArrayCases() {
        return Stream.of(// empty array
        Arguments.of(new byte[] {}, ""), // single zero byte
        Arguments.of(new byte[] { 0 }, "00"), // single 0x0F byte
        Arguments.of(new byte[] { 0x0F }, "0F"), // single 0xFF byte
        Arguments.of(new byte[] { (byte) 0xFF }, "FF"), // typical array covering all hex digits
        Arguments.of(new byte[] { 0x12, 0x34, 0x56, (byte) 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0 }, "123456789ABCDEF0"), // negative values: -1 → 0xFF, -128 → 0x80, 127 → 0x7F
        Arguments.of(new byte[] { (byte) -1, (byte) -128, (byte) 127 }, "FF807F"), // sequential low values 0‑9
        Arguments.of(new byte[] { 0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9 }, "00010203040506070809"));
    }

    /**
     * Verify that the private constructor is indeed private and can be instantiated via reflection.
     */
    @Test
    void testConstructorIsPrivate() throws Exception {
        Class<?> clazz = ByteUtilities.class;
        assertEquals(Modifier.FINAL, clazz.getModifiers() & Modifier.FINAL, "ByteUtilities should be declared final");
        var constructor = clazz.getDeclaredConstructor();
        assertEquals(Modifier.PRIVATE, constructor.getModifiers() & Modifier.PRIVATE, "ByteUtilities constructor should be private");
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance(), "Instantiating the private constructor via reflection should succeed");
    }

    /**
     * Basic encoding test using the sample data defined in {@link #setupBeforeEach()}.
     */
    @Test
    void testEncodeTypicalValues() {
        // given – sampleArray and expectedHex are prepared in @BeforeEach
        // when
        String actual = ByteUtilities.encode(sampleArray);
        // then
        assertEquals(expectedHex, actual, "Encoding of mixed‑sign byte array did not produce the expected hex string");
    }

    /**
     * Encoding an empty byte array must return an empty string.
     */
    @Test
    void testEncodeEmptyArray() {
        // given – emptyArray and emptyHex are prepared in @BeforeEach
        // when
        String actual = ByteUtilities.encode(emptyArray);
        // then
        assertEquals(emptyHex, actual, "Encoding an empty byte array should yield an empty string");
    }

    /**
     * Encode every possible byte value (0‑255) and verify the round‑trip using decode.
     */
    @Test
    void testEncodeAllByteValuesRoundTrip() {
        // given
        byte[] allBytes = new byte[256];
        for (int i = 0; i < 256; i++) {
            allBytes[i] = (byte) i;
        }
        // when
        String hex = ByteUtilities.encode(allBytes);
        byte[] decoded = ByteUtilities.decode(hex);
        // then
        assertArrayEquals(allBytes, decoded, "Round‑trip encode/decode of all byte values should be loss‑less");
        // Additionally verify that the hex string length is exactly 2 * 256
        assertEquals(512, hex.length(), "Hex string length should be twice the number of input bytes");
    }

    /**
     * Verify that the method never returns null for a non‑null input.
     */
    @Test
    void testEncodeNeverReturnsNullForNonNullInput() {
        // given
        byte[] data = new byte[] { 0x0A, 0x0B, 0x0C };
        // when
        String result = ByteUtilities.encode(data);
        // then
        assertNotNull(result, "Encode should never return null for a valid byte array");
    }

    /**
     * Parameterised test that covers the generic "encode byte array to hex string"
     * cases (empty array, single byte, typical multi‑byte, negative values, etc.).
     */
    @ParameterizedTest(name = "{index}: encode({0}) ⇒ \"{1}\"")
    @MethodSource("provideEncodeByteArrayCases")
    void testEncodeByteArrayToHexString(byte[] input, String expected) {
        assertEquals(expected, ByteUtilities.encode(input));
    }

    /**
     * Encode a {@code null} reference – the method should throw {@link NullPointerException}.
     */
    @Test
    void testEncodeNullInput() {
        assertThrows(NullPointerException.class, () -> ByteUtilities.encode(null), "Passing a null byte array should result in NullPointerException");
    }

    /**
     * Encode a single byte where only the high nibble is non‑zero (0xA0).
     */
    @Test
    void testEncodeSingleByteHighNibbleOnly() {
        byte[] input = new byte[] { (byte) 0xA0 };
        assertEquals("A0", ByteUtilities.encode(input));
    }

    /**
     * Encode a single byte where both nibbles are non‑zero (0xAB).
     */
    @Test
    void testEncodeSingleByteBothNibblesNonZero() {
        byte[] input = new byte[] { (byte) 0xAB };
        assertEquals("AB", ByteUtilities.encode(input));
    }

    /**
     * Encode a 256‑byte array containing every possible byte value (0‑255)
     * and verify the produced string matches the expected 512‑character hex string.
     */
    @Test
    void testEncodeLargeSequentialByteArray() {
        // Build the 0‑255 array
        byte[] all = new byte[256];
        for (int i = 0; i < 256; i++) {
            all[i] = (byte) i;
        }
        // Expected string generated on‑the‑fly – this mirrors the logic of encode()
        StringBuilder sb = new StringBuilder(512);
        for (byte b : all) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        String expected = sb.toString();
        String actual = ByteUtilities.encode(all);
        assertEquals(expected, actual, "Encoding a 256‑byte array should produce the exact concatenation of two‑character hex values");
        assertEquals(512, actual.length(), "The resulting hex string length must be 512 characters");
    }

    /**
     * Verify that an array whose length would cause the internal {@code char[]} allocation
     * to overflow throws {@link NegativeArraySizeException}.
     *
     * <p>
     * Creating such a large array is memory‑intensive; the test is marked {@code @Disabled}
     * on environments where the required heap is unavailable.  When enabled, the test
     * confirms the overflow behaviour.
     * </p>
     */
    @Test
    @Disabled("Disabled by default – requires >1 GB heap to allocate the test array")
    void testEncodeArrayLengthCausingIntegerOverflow() {
        // 1 073 741 824
        int length = (Integer.MAX_VALUE / 2) + 1;
        // Attempt to allocate the array – may throw OutOfMemoryError before encode() runs.
        // The test is primarily illustrative; in a real CI environment the JVM should be
        // started with sufficient heap if this test is to be enabled.
        byte[] hugeArray = new byte[length];
        assertThrows(NegativeArraySizeException.class, () -> ByteUtilities.encode(hugeArray), "Encoding an array whose length*2 overflows should throw NegativeArraySizeException");
    }
}
