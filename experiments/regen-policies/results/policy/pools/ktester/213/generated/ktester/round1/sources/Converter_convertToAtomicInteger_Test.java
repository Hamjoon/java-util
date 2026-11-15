package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToAtomicInteger_Test {

    /* -----------------------------------------------------------
     *  Helper method used by all parameterised tests.
     * ----------------------------------------------------------- */
    private void assertConversion(Object source, Integer expectedValue, Class<? extends Throwable> expectedException, String description) {
        if (expectedException != null) {
            // conversion must throw the expected exception
            Throwable thrown = assertThrows(expectedException, () -> Converter.convertToAtomicInteger(source), () -> "Expected exception for case: " + description);
            // optional: verify that the message is not empty
            assertNotNull(thrown.getMessage(), "Exception should contain a message");
        } else {
            // conversion should succeed (or return null)
            AtomicInteger result = Converter.convertToAtomicInteger(source);
            if (expectedValue == null) {
                assertNull(result, "Result must be null for case: " + description);
            } else {
                assertNotNull(result, "Result must not be null for case: " + description);
                assertEquals(expectedValue.intValue(), result.get(), () -> "Incorrect value for case: " + description);
            }
        }
    }

    private static Stream<Arguments> validConversionsProvider() {
        return Stream.of(// plain Integer
        Arguments.of(42, 42, null, "Integer → AtomicInteger"), // Double – fractional part truncated
        Arguments.of(3.14d, 3, null, "Double → AtomicInteger (truncation)"), // numeric String
        Arguments.of("123", 123, null, "String \"123\" → AtomicInteger"), // whitespace‑padded signed String
        Arguments.of("  -7  ", -7, null, "String with spaces and sign → AtomicInteger"), // Boolean true / false
        Arguments.of(Boolean.TRUE, 1, null, "Boolean true → AtomicInteger (1)"), Arguments.of(Boolean.FALSE, 0, null, "Boolean false → AtomicInteger (0)"), // hexadecimal String
        Arguments.of("0xFF", 255, null, "Hexadecimal string → AtomicInteger"), // octal String
        Arguments.of("077", 63, null, "Octal string → AtomicInteger"), // Map with a \"value\" entry
        Arguments.of(Map.of("value", "55"), 55, null, "Map with value key → AtomicInteger"), // null input
        Arguments.of(null, null, null, "null input → null result"), // already an AtomicInteger
        Arguments.of(new AtomicInteger(99), 99, null, "AtomicInteger input → same value"), // Long within range
        Arguments.of(7L, 7, null, "Long 7L → AtomicInteger"), // Long exceeding Integer.MAX_VALUE – lower 32‑bits truncation (as per typical cast)
        Arguments.of(1234567890123L, (int) (1234567890123L & 0xffffffffL), null, "Long > Integer.MAX_VALUE → truncated int"), // String representing Integer.MAX_VALUE
        Arguments.of(String.valueOf(Integer.MAX_VALUE), Integer.MAX_VALUE, null, "String Integer.MAX_VALUE → AtomicInteger"), // String with explicit plus sign
        Arguments.of("+42", 42, null, "String \"+42\" → AtomicInteger"), // String with leading zeros
        Arguments.of("000123", 123, null, "String \"000123\" → AtomicInteger"));
    }

    private static Stream<Arguments> unsupportedConversionsProvider() {
        return Stream.of(// generic Object
        Arguments.of(new Object(), null, IllegalArgumentException.class, "Object → IllegalArgumentException"), // List
        Arguments.of(List.of(1, 2, 3), null, IllegalArgumentException.class, "List → IllegalArgumentException"), // non‑numeric String
        Arguments.of("not-a-number", null, IllegalArgumentException.class, "Non‑numeric String → IllegalArgumentException"), // binary notation (unsupported)
        Arguments.of("0b1010", null, IllegalArgumentException.class, "Binary notation String → IllegalArgumentException"), // NaN string
        Arguments.of("NaN", null, IllegalArgumentException.class, "\"NaN\" String → IllegalArgumentException"));
    }

    private static Stream<Arguments> additionalNumericProvider() {
        return Stream.of(// byte
        Arguments.of((byte) 7, 7, null, "byte 7 → AtomicInteger"), // short
        Arguments.of((short) -12, -12, null, "short -12 → AtomicInteger"), // float
        Arguments.of(9.99f, 9, null, "float 9.99f → AtomicInteger (truncation)"), // BigDecimal with fractional part
        Arguments.of(new BigDecimal("12345.67"), 12345, null, "BigDecimal \"12345.67\" → AtomicInteger (fraction discarded)"), // BigInteger too large
        Arguments.of(new BigInteger("9876543210"), null, IllegalArgumentException.class, "BigInteger > Integer.MAX_VALUE → IllegalArgumentException"), // numeric character
        Arguments.of('8', 8, null, "Character '8' → AtomicInteger"), // non‑numeric character
        Arguments.of('a', null, IllegalArgumentException.class, "Character 'a' → IllegalArgumentException"));
    }

    private static Stream<Arguments> atomicWrapperProvider() {
        return Stream.of(// AtomicLong within int range
        Arguments.of(new AtomicLong(55L), 55, null, "AtomicLong 55L → AtomicInteger"), // AtomicLong exceeding int range
        Arguments.of(new AtomicLong(5_000_000_000L), null, IllegalArgumentException.class, "AtomicLong > Integer.MAX_VALUE → IllegalArgumentException"), // AtomicBoolean true / false
        Arguments.of(new AtomicBoolean(true), 1, null, "AtomicBoolean true → AtomicInteger (1)"), Arguments.of(new AtomicBoolean(false), 0, null, "AtomicBoolean false → AtomicInteger (0)"));
    }

    private static Stream<Arguments> edgeCaseStringProvider() {
        return Stream.of(// explicit plus sign
        Arguments.of("+42", 42, null, "String \"+42\" → AtomicInteger"), // leading zeros
        Arguments.of("000123", 123, null, "String \"000123\" → AtomicInteger"), // empty string
        Arguments.of("", null, IllegalArgumentException.class, "Empty string → IllegalArgumentException"), // scientific notation (unsupported)
        Arguments.of("1e3", null, IllegalArgumentException.class, "Scientific notation \"1e3\" → IllegalArgumentException"), // underscores inside numeric string (unsupported)
        Arguments.of("1_000", null, IllegalArgumentException.class, "Underscore numeric string \"1_000\" → IllegalArgumentException"), // literal "true"
        Arguments.of("true", null, IllegalArgumentException.class, "String \"true\" → IllegalArgumentException"));
    }

    private static Stream<Arguments> unsupportedNonNumericProvider() {
        return Stream.of(Arguments.of(new Date(), null, IllegalArgumentException.class, "java.util.Date → IllegalArgumentException"), Arguments.of(Calendar.getInstance(), null, IllegalArgumentException.class, "java.util.Calendar → IllegalArgumentException"), Arguments.of(LocalDate.now(), null, IllegalArgumentException.class, "java.time.LocalDate → IllegalArgumentException"));
    }

    private static Stream<Arguments> bufferAndCharSequenceProvider() {
        return Stream.of(// CharBuffer containing numeric characters
        Arguments.of(CharBuffer.wrap("56"), 56, null, "CharBuffer \"56\" → AtomicInteger"), // ByteBuffer with ASCII digits '7' and '8'
        Arguments.of(ByteBuffer.wrap(new byte[] { '7', '8' }), 78, null, "ByteBuffer \"78\" → AtomicInteger"));
    }

    private static Stream<Arguments> specialFloatingPointProvider() {
        return Stream.of(Arguments.of(Double.NaN, null, IllegalArgumentException.class, "Double.NaN → IllegalArgumentException"), Arguments.of(Double.POSITIVE_INFINITY, null, IllegalArgumentException.class, "Double.POSITIVE_INFINITY → IllegalArgumentException"), Arguments.of(Double.NEGATIVE_INFINITY, null, IllegalArgumentException.class, "Double.NEGATIVE_INFINITY → IllegalArgumentException"), Arguments.of(Float.NaN, null, IllegalArgumentException.class, "Float.NaN → IllegalArgumentException"), Arguments.of(Float.POSITIVE_INFINITY, null, IllegalArgumentException.class, "Float.POSITIVE_INFINITY → IllegalArgumentException"), Arguments.of(Float.NEGATIVE_INFINITY, null, IllegalArgumentException.class, "Float.NEGATIVE_INFINITY → IllegalArgumentException"));
    }

    @Test
    void shouldConvertIntegerObjectToAtomicInteger() {
        // given
        Integer source = 42;
        // when
        AtomicInteger result = Converter.convertToAtomicInteger(source);
        // then
        assertNotNull(result, "Result must not be null");
        assertEquals(42, result.get(), "AtomicInteger should contain the original int value");
    }

    @Test
    void shouldConvertStringRepresentingNumberToAtomicInteger() {
        // given
        String source = "123";
        // when
        AtomicInteger result = Converter.convertToAtomicInteger(source);
        // then
        assertNotNull(result);
        assertEquals(123, result.get());
    }

    @Test
    void shouldConvertLongToAtomicInteger() {
        // given
        Long source = 7L;
        // when
        AtomicInteger result = Converter.convertToAtomicInteger(source);
        // then
        assertNotNull(result);
        assertEquals(7, result.get());
    }

    @Test
    void shouldConvertMapWithValueKeyToAtomicInteger() {
        // given – map format used by Converter for scalar values
        Map<String, Object> source = Map.of("value", "55");
        // when
        AtomicInteger result = Converter.convertToAtomicInteger(source);
        // then
        assertNotNull(result);
        assertEquals(55, result.get());
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        // given
        Object source = null;
        // when
        AtomicInteger result = Converter.convertToAtomicInteger(source);
        // then
        assertNull(result, "Conversion of null should yield null for object target types");
    }

    @Test
    void shouldConvertAtomicIntegerDirectly() {
        // given
        AtomicInteger source = new AtomicInteger(99);
        // when
        AtomicInteger result = Converter.convertToAtomicInteger(source);
        // then
        // The converter should return the same instance or an equivalent one.
        // Equality of value is sufficient for the contract.
        assertNotNull(result);
        assertEquals(99, result.get());
    }

    @Test
    void shouldThrowExceptionForUnsupportedType() {
        // given
        // no conversion defined for plain Object -> AtomicInteger
        Object source = new Object();
        // when / then
        assertThrows(RuntimeException.class, () -> Converter.convertToAtomicInteger(source), "Unsupported conversion should result in a RuntimeException (or subclass)");
    }

    /* -----------------------------------------------------------
     *  1. Valid conversions (numeric, boolean, string, map, etc.)
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validConversionsProvider")
    @DisplayName("convertToAtomicInteger – valid conversions")
    void shouldConvertValidValues(Object source, Integer expected, Class<? extends Throwable> ignoredException, String description) {
        assertConversion(source, expected, null, description);
    }

    /* -----------------------------------------------------------
     *  2. Unsupported types / error handling (expect IllegalArgumentException)
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("unsupportedConversionsProvider")
    @DisplayName("convertToAtomicInteger – unsupported types and error handling")
    void shouldThrowForUnsupported(Object source, Integer ignored, Class<? extends Throwable> expectedException, String description) {
        assertConversion(source, null, expectedException, description);
    }

    /* -----------------------------------------------------------
     *  3. Additional numeric primitive/wrapper types
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("additionalNumericProvider")
    @DisplayName("convertToAtomicInteger – additional numeric types")
    void shouldConvertAdditionalNumeric(Object source, Integer expected, Class<? extends Throwable> ignored, String description) {
        assertConversion(source, expected, null, description);
    }

    /* -----------------------------------------------------------
     *  4. Atomic wrapper types
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("atomicWrapperProvider")
    @DisplayName("convertToAtomicInteger – atomic wrapper types")
    void shouldConvertAtomicWrappers(Object source, Integer expected, Class<? extends Throwable> ignored, String description) {
        assertConversion(source, expected, null, description);
    }

    /* -----------------------------------------------------------
     *  5. Edge‑case string formats
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("edgeCaseStringProvider")
    @DisplayName("convertToAtomicInteger – edge‑case string formats")
    void shouldHandleEdgeCaseStrings(Object source, Integer expected, Class<? extends Throwable> expectedException, String description) {
        assertConversion(source, expected, expectedException, description);
    }

    /* -----------------------------------------------------------
     *  6. Unsupported non‑numeric objects (Date, Calendar, LocalDate)
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("unsupportedNonNumericProvider")
    @DisplayName("convertToAtomicInteger – unsupported non‑numeric objects")
    void shouldRejectNonNumericObjects(Object source, Integer ignored, Class<? extends Throwable> expectedException, String description) {
        assertConversion(source, null, expectedException, description);
    }

    /* -----------------------------------------------------------
     *  7. Buffer and CharSequence types
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("bufferAndCharSequenceProvider")
    @DisplayName("convertToAtomicInteger – buffer and char‑sequence types")
    void shouldConvertBuffers(Object source, Integer expected, Class<? extends Throwable> ignored, String description) {
        assertConversion(source, expected, null, description);
    }

    /* -----------------------------------------------------------
     *  8. Special floating‑point values (NaN, infinities)
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("specialFloatingPointProvider")
    @DisplayName("convertToAtomicInteger – special floating‑point values")
    void shouldRejectSpecialFloatingPoints(Object source, Integer ignored, Class<? extends Throwable> expectedException, String description) {
        assertConversion(source, null, expectedException, description);
    }

    /* -----------------------------------------------------------
     *  9. Additional edge cases from original test suite
     * ----------------------------------------------------------- */
    @Test
    @DisplayName("convertToAtomicInteger – blank string should raise IllegalArgumentException")
    void shouldThrowForBlankString() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToAtomicInteger("   "), "Blank string must not be convertible");
    }

    @Test
    @DisplayName("convertToAtomicInteger – string larger than Integer.MAX_VALUE should raise IllegalArgumentException")
    void shouldThrowForStringBeyondIntMax() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToAtomicInteger("2147483648"), "Overflow beyond Integer.MAX_VALUE must fail");
    }

    @Test
    @DisplayName("convertToAtomicInteger – string smaller than Integer.MIN_VALUE should raise IllegalArgumentException")
    void shouldThrowForStringBelowIntMin() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToAtomicInteger("   -2147483649   "), "Underflow beyond Integer.MIN_VALUE must fail");
    }

    @Test
    @DisplayName("convertToAtomicInteger – numeric string with decimal point but no fraction")
    void shouldParseIntegerStringWithTrailingDotZero() {
        AtomicInteger result = Converter.convertToAtomicInteger("123.0");
        assertNotNull(result);
        assertEquals(123, result.get());
    }

    @Test
    @DisplayName("convertToAtomicInteger – numeric string with fractional part should truncate")
    void shouldTruncateFractionalPart() {
        AtomicInteger result = Converter.convertToAtomicInteger("123.45");
        assertNotNull(result);
        assertEquals(123, result.get());
    }
}
