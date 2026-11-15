package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import java.util.Arrays;

@ExtendWith(MockitoExtension.class)
class StringUtilities_decode_Test {

    /**
     * Supplies the arguments for {@link #testDecode_parameterized(String, String, String)}.
     *
     * @return a stream of arguments (input string, expected result, description)
     */
    private static Stream<Arguments> decodeTestCases() {
        return Stream.of(// Group: decode(String) - normal and edge cases
        Arguments.of("4A6F686E", "[74, 111, 104, 110]", "Even length string containing only valid hex digits; should decode to the ASCII bytes for \"John\"."), Arguments.of("", "[]", "Empty string has length 0 (even) and should return an empty byte array."), Arguments.of("ABC", "null", "String length is odd (3); the method should hit the first branch and return null."), Arguments.of("G1", "[-15, 1]", "String has even length but contains a non‑hex character ('G'). hi = -1, lo = 1 => byte -15."), Arguments.of(null, "NullPointerException", "Passing a null reference causes s.length() to throw NullPointerException."), // Group: decode(String) - additional cases
        Arguments.of("0F", "[15]", "Even length string with upper‑case hex digits; 0x0F should decode to byte value 15."), Arguments.of("ff", "[-1]", "Even length string with lower‑case hex digits; 0xFF maps to signed byte -1."), Arguments.of("AaBb", "[-86, -69]", "Mixed‑case hex digits; \"Aa\" → 0xAA (‑86), \"Bb\" → 0xBB (‑69)."), Arguments.of("0001", "[0, 1]", "String with leading zeros; should produce bytes 0x00 and 0x01."), Arguments.of("FF00AB", "[-1, 0, -85]", "Valid hex sequence containing a high byte (FF), a zero byte, and a non‑ASCII byte (AB)."), Arguments.of("??", "[-17]", "Both characters are non‑hex; Character.digit returns -1 for each, resulting in ((-1*16)+(-1)) = -17."), Arguments.of("0 ", "[-1]", "Second character is a space (non‑hex). hi = 0, lo = -1 → byte value -1."), Arguments.of("a1", "[-95]", "Lower‑case hex with value 0xA1, which as a signed byte is -95."), Arguments.of("0000000000", "[0, 0, 0, 0, 0]", "Ten zero characters (five byte pairs) should decode to an array of five zero bytes."));
    }

    /**
     * Helper that converts a string representation like "[1, -2, 3]" into a {@code byte[]}.
     *
     * @param expected the expected value string from the JSON test case
     * @return a byte array matching the described values
     */
    private static byte[] parseExpectedByteArray(String expected) {
        // Trim surrounding brackets and whitespace
        String trimmed = expected.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        if (trimmed.isEmpty()) {
            return new byte[0];
        }
        String[] parts = trimmed.split(",");
        byte[] result = new byte[parts.length];
        for (int i = 0; i < parts.length; i++) {
            // Each part may contain leading/trailing spaces
            int intVal = Integer.parseInt(parts[i].trim());
            result[i] = (byte) intVal;
        }
        return result;
    }

    /**
     * Verify that a valid even‑length hexadecimal string is decoded correctly.
     */
    @Test
    void testDecode_validHexString_returnsExpectedBytes() {
        // "0A1B2C" -> {0x0A, 0x1B, 0x2C}
        String hex = "0A1B2C";
        byte[] expected = new byte[] { (byte) 0x0A, (byte) 0x1B, (byte) 0x2C };
        byte[] actual = StringUtilities.decode(hex);
        assertArrayEquals(expected, actual, "Decoded byte array does not match expected value");
    }

    /**
     * An empty string has length 0 (even) and should result in an empty byte array.
     */
    @Test
    void testDecode_emptyString_returnsEmptyArray() {
        byte[] actual = StringUtilities.decode("");
        assertNotNull(actual, "Result should not be null for empty input");
        assertEquals(0, actual.length, "Empty string should produce an empty byte array");
    }

    /**
     * Strings with an odd number of characters cannot be parsed as hex pairs and must return {@code null}.
     */
    @Test
    void testDecode_oddLengthString_returnsNull() {
        assertNull(StringUtilities.decode("ABC"), "Odd‑length hex string should return null");
        assertNull(StringUtilities.decode("1"), "Single‑character hex string should return null");
    }

    /**
     * Passing {@code null} to the method should result in a {@link NullPointerException}
     * because the implementation dereferences the argument without a null check.
     */
    @Test
    void testDecode_nullInput_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> StringUtilities.decode(null), "Calling decode(null) should throw NullPointerException");
    }

    /**
     * Non‑hex characters are interpreted by {@link Character#digit(char,int)} as -1.
     * The method does not validate the input, so the resulting byte values are based on -1.
     */
    @Test
    void testDecode_invalidHexCharacters_returnsCalculatedBytes() {
        // "zz" -> hi = -1, lo = -1 => (hi*16)+lo = -17 => (byte)-17 == 0xEF
        byte[] expected = new byte[] { (byte) 0xEF };
        byte[] actual = StringUtilities.decode("zz");
        assertArrayEquals(expected, actual, "Invalid hex characters should be processed as -1 per Character.digit semantics");
    }

    /**
     * Parameterized test that iterates over a comprehensive set of inputs
     * (both valid and invalid) defined in the JSON test case description.
     *
     * @param inputHex   the string passed to {@code decode}
     * @param expected   the expected result representation:
     *                   <ul>
     *                     <li>"null" – expect a {@code null} return value</li>
     *                     <li>"NullPointerException" – expect an exception</li>
     *                     <li>"[a, b, …]" – a comma‑separated list of signed byte values</li>
     *                   </ul>
     * @param description human‑readable description of the scenario
     */
    @ParameterizedTest(name = "{2}")
    @MethodSource("decodeTestCases")
    void testDecode_parameterized(String inputHex, String expected, String description) {
        // When the expected outcome is an exception, verify that it is thrown.
        if ("NullPointerException".equals(expected)) {
            assertThrows(NullPointerException.class, () -> StringUtilities.decode(inputHex), "Expected NullPointerException for input: " + inputHex);
            return;
        }
        // Normal execution – obtain the actual result.
        byte[] actual = StringUtilities.decode(inputHex);
        // Expected "null" means the method should have returned null.
        if ("null".equals(expected)) {
            assertNull(actual, "Expected null for input: " + inputHex);
            return;
        }
        // Otherwise we expect a concrete byte array.
        byte[] expectedBytes = parseExpectedByteArray(expected);
        assertArrayEquals(expectedBytes, actual, "Decoded bytes differ for input: " + inputHex);
    }
}
