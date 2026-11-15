package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ByteUtilities_decode_Test {

    /* --------------------------------------------------------------------- *
     *  Test data shared by several test methods
     * --------------------------------------------------------------------- */
    private static final byte[] ARRAY_1 = new byte[] { (byte) 0xFF, (byte) 0x00 };

    private static final byte[] ARRAY_2 = new byte[] { 0x01, 0x23, 0x45, 0x67 };

    private static final String HEX_1_UPPER = "FF00";

    private static final String HEX_2_UPPER = "01234567";

    private static final String HEX_1_LOWER = "ff00";

    private static final String HEX_INVALID_CHARS = "GGHH";

    /**
     * Provides valid hexadecimal strings together with their expected byte arrays.
     */
    private static Stream<Arguments> validHexProvider() {
        return Stream.of(// Empty string → empty array
        Arguments.of("", new byte[0]), // Single byte cases
        Arguments.of("00", new byte[] { 0 }), Arguments.of("0F", new byte[] { 15 }), // Mixed‑case longer string
        Arguments.of("a1b2c3", new byte[] { (byte) 0xA1, (byte) 0xB2, (byte) 0xC3 }), // All 'F's (0xFF = -1 in signed byte)
        Arguments.of("FFFFFFFF", new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }));
    }

    /**
     * Provides strings with an odd number of characters – method must return {@code null}.
     */
    private static Stream<Arguments> oddLengthProvider() {
        return Stream.of(Arguments.of("0"), Arguments.of("ABC"));
    }

    /**
     * Provides inputs containing non‑hexadecimal characters.
     * The expected result follows the actual implementation: each invalid digit becomes -1,
     * which yields a byte value of -17 (or -16 when paired with a valid digit).
     */
    private static Stream<Arguments> nonHexProvider() {
        return Stream.of(// Both characters invalid → -17
        Arguments.of("ZZ", new byte[] { (byte) -17 }), // First char invalid, second valid → -16 (0xF0)
        Arguments.of("G0", new byte[] { (byte) -16 }));
    }

    @Test
    @DisplayName("decode() returns correct byte array for valid upper‑case hex strings")
    void testDecodeValidUpperCase() {
        assertArrayEquals(ARRAY_1, ByteUtilities.decode(HEX_1_UPPER));
        assertArrayEquals(ARRAY_2, ByteUtilities.decode(HEX_2_UPPER));
    }

    @Test
    @DisplayName("decode() is case‑insensitive")
    void testDecodeValidLowerCase() {
        assertArrayEquals(ARRAY_1, ByteUtilities.decode(HEX_1_LOWER));
    }

    @Test
    @DisplayName("decode() returns null for strings with odd length")
    void testDecodeOddLength() {
        // 3 characters
        assertNull(ByteUtilities.decode("ABC"));
        // 5 characters
        assertNull(ByteUtilities.decode("12345"));
    }

    @Test
    @DisplayName("decode() processes non‑hex characters (digit() returns -1)")
    void testDecodeInvalidCharacters() {
        // "GG" -> both digits are -1, hi*16 + lo = (-1*16) + (-1) = -17 => (byte) -17
        // "HH" -> same result, so the resulting array is { -17, -17 }
        byte[] expected = new byte[] { (byte) -17, (byte) -17 };
        assertArrayEquals(expected, ByteUtilities.decode(HEX_INVALID_CHARS));
    }

    @Test
    @DisplayName("Utility class has a private constructor")
    void testConstructorIsPrivate() throws Exception {
        Class<?> clazz = ByteUtilities.class;
        assertTrue(Modifier.isFinal(clazz.getModifiers()), "Class should be final");
        Constructor<?> constructor = clazz.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");
        constructor.setAccessible(true);
        Object instance = constructor.newInstance();
        assertNotNull(instance, "Instance created via reflection should not be null");
    }

    @ParameterizedTest(name = "[{index}] decode(\"{0}\") → {1}")
    @MethodSource("validHexProvider")
    @DisplayName("decode() – valid hexadecimal strings")
    void testDecodeValidHexStrings(String hex, byte[] expected) {
        assertArrayEquals(expected, ByteUtilities.decode(hex));
    }

    @ParameterizedTest(name = "[{index}] decode(\"{0}\") → null")
    @MethodSource("oddLengthProvider")
    @DisplayName("decode() – odd length strings")
    void testDecodeOddLengthStrings(String hex) {
        assertNull(ByteUtilities.decode(hex));
    }

    @ParameterizedTest(name = "[{index}] decode(\"{0}\") → {1}")
    @MethodSource("nonHexProvider")
    @DisplayName("decode() – non‑hex characters")
    void testDecodeNonHexCharacters(String hex, byte[] expected) {
        assertArrayEquals(expected, ByteUtilities.decode(hex));
    }

    /**
     * Mixed‑case handling – the method should be case‑insensitive.
     */
    @Test
    @DisplayName("decode() – mixed case handling")
    void testDecodeMixedCase() {
        byte[] expected = new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0xEF };
        assertArrayEquals(expected, ByteUtilities.decode("aBcDeF"));
    }

    /**
     * Leading zeros and whitespace handling.
     * Whitespace is *not* a hex digit; because the total length becomes odd,
     * the method returns {@code null}. The test verifies this behaviour.
     */
    @Test
    @DisplayName("decode() – leading zeros")
    void testDecodeLeadingZeros() {
        byte[] expected = new byte[] { 0, 1 };
        assertArrayEquals(expected, ByteUtilities.decode("0001"));
    }

    @Test
    @DisplayName("decode() – whitespace leads to null (odd length)")
    void testDecodeWhitespaceOddLength() {
        // The string contains a space; length is 3 → odd → null
        assertNull(ByteUtilities.decode(" 0F"));
    }

    /**
     * Boundary values for signed bytes.
     */
    @Test
    @DisplayName("decode() – maximum byte values")
    void testDecodeMaximumByteValues() {
        byte[] expected = new byte[] { 0x7F, (byte) 0x80, (byte) 0xFF };
        assertArrayEquals(expected, ByteUtilities.decode("7F80FF"));
    }

    /**
     * Null input should raise a {@link NullPointerException}.
     */
    @Test
    @DisplayName("decode() – null input")
    void testDecodeNullInput() {
        assertThrows(NullPointerException.class, () -> ByteUtilities.decode(null));
    }
}
