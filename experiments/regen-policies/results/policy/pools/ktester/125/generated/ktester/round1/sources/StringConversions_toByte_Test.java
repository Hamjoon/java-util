package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

class StringConversions_toByte_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use default options – the Converter does not need any special configuration for the
        // String → Byte conversion exercised by StringConversions.toByte
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Provides all test cases described in the JSON specification.
     *
     * Each argument consists of:
     *   - the {@code from} value (may be {@code null}, {@code String}, or any Object)
     *   - the expected outcome:
     *       * a numeric string representing the expected byte value,
     *       * or the simple name of an exception class that should be thrown.
     */
    private static Stream<Arguments> toByteTestCases() {
        return Stream.of(// Null input → treated as empty → 0
        Arguments.of(null, "0", "Null input should be treated as empty and return byte value 0."), // Empty string
        Arguments.of("", "0", "Empty string is considered empty and should return byte value 0."), // Simple valid values
        Arguments.of("0", "0", "A valid byte string within range should be parsed directly by Byte.valueOf."), Arguments.of("127", "127", "Maximum byte value should be parsed successfully."), Arguments.of("-128", "-128", "Minimum byte value should be parsed successfully."), // Out‑of‑range values
        Arguments.of("128", "IllegalArgumentException", "Value exceeds Byte.MAX_VALUE; Byte.valueOf throws NumberFormatException, toLong returns null, causing IllegalArgumentException."), Arguments.of("-129", "IllegalArgumentException", "Value is less than Byte.MIN_VALUE; same fallback path leads to IllegalArgumentException."), // Non‑numeric strings
        Arguments.of("abc", "IllegalArgumentException", "Non‑numeric string cannot be parsed; toLong returns null, resulting in IllegalArgumentException."), Arguments.of("1e2", "IllegalArgumentException", "Scientific notation is not accepted; Byte.valueOf throws, toLong cannot parse within range, resulting in IllegalArgumentException."), // Decimal representations
        Arguments.of("12.5", "IllegalArgumentException", "Decimal string is not a valid byte; toLong cannot produce a value within byte range, so an IllegalArgumentException is thrown."), Arguments.of("12.0", "12", "Decimal representation that equals an integer within byte range; Byte.valueOf fails, but toLong parses it to 12 and the method returns byte 12."), Arguments.of("0.0", "0", "Decimal string representing zero; Byte.valueOf throws NumberFormatException, toLong parses as 0 within range, method returns byte 0."), Arguments.of("-128.0", "-128", "Decimal representation of minimum byte; Byte.valueOf fails, toLong parses to -128, method returns byte -128."), Arguments.of("127.999", "IllegalArgumentException", "Decimal value slightly above max byte; toLong returns null because out of range, causing IllegalArgumentException."), Arguments.of("12.0000000001", "IllegalArgumentException", "Decimal with fractional part beyond zero; toLong cannot produce a whole number within byte range, leading to IllegalArgumentException."), // Whitespace handling
        Arguments.of("   5", "IllegalArgumentException", "String with leading whitespace cannot be parsed by Byte.valueOf; toLong also fails, leading to IllegalArgumentException."), Arguments.of("  -7", "IllegalArgumentException", "String with leading whitespace and a negative sign cannot be parsed by Byte.valueOf; toLong also fails, resulting in IllegalArgumentException."), Arguments.of("  +42", "IllegalArgumentException", "String with leading whitespace and plus sign cannot be parsed; toLong also fails, leading to IllegalArgumentException."), Arguments.of("  ", "0", "String containing only whitespace is considered empty by StringUtilities.isEmpty, method returns byte 0."), // Explicit plus sign
        Arguments.of("+10", "10", "String with explicit plus sign is a valid byte representation; should be parsed directly."), Arguments.of("+0", "0", "Explicit plus sign with zero; Byte.valueOf parses directly, returns byte 0."), // Non‑String objects – should cause ClassCastException
        Arguments.of(123, "ClassCastException", "Non‑String object (Integer) passed as 'from' causes a ClassCastException when the method casts it to String."), Arguments.of(new StringBuilder("10"), "ClassCastException", "A StringBuilder instance is not a String; casting to String fails, resulting in ClassCastException."), Arguments.of(new HashMap<>(), "ClassCastException", "An arbitrary Object (empty map literal) is supplied; the cast to String triggers a ClassCastException."));
    }

    @Test
    void emptyStringReturnsZeroByte() {
        // given
        String input = "";
        // when
        Byte result = StringConversions.toByte(input, converter);
        // then
        assertEquals((byte) 0, result);
    }

    @Test
    void nullStringThrowsNullPointerException() {
        // given
        String input = null;
        // when / then
        assertThrows(NullPointerException.class, () -> StringConversions.toByte(input, converter));
    }

    @Test
    void validByteStringIsConverted() {
        // given
        String input = "42";
        // when
        Byte result = StringConversions.toByte(input, converter);
        // then
        assertEquals((byte) 42, result);
    }

    @Test
    void outOfRangeStringThrowsIllegalArgumentException() {
        // given
        // greater than Byte.MAX_VALUE (127)
        String input = "200";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toByte(input, converter));
        assertTrue(ex.getMessage().contains("not parseable as a byte value"));
    }

    @Test
    void nonNumericStringThrowsIllegalArgumentException() {
        // given
        String input = "abc";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toByte(input, converter));
        assertTrue(ex.getMessage().contains("not parseable as a byte value"));
    }

    @Test
    void nullStringIsTreatedAsEmptyAndReturnsZeroByte() {
        // given
        String input = null;
        // when
        Byte result = StringConversions.toByte(input, converter);
        // then
        assertEquals((byte) 0, result);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("toByteTestCases")
    void testToByteVariousInputs(Object from, String expected, String description) {
        // The description is only used for the test name; it is not needed in the body.
        if ("IllegalArgumentException".equals(expected)) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toByte(from, converter), "Expected IllegalArgumentException for input: " + from);
            assertTrue(ex.getMessage().contains("not parseable as a byte value"), "Exception message should contain hint about parse failure.");
        } else if ("ClassCastException".equals(expected)) {
            assertThrows(ClassCastException.class, () -> StringConversions.toByte(from, converter), "Expected ClassCastException for non‑String input: " + from);
        } else {
            // Expected a concrete byte value
            Byte result = StringConversions.toByte(from, converter);
            byte expectedByte = Byte.parseByte(expected);
            assertEquals(expectedByte, result, "Byte value mismatch for input: " + from);
        }
    }
}
