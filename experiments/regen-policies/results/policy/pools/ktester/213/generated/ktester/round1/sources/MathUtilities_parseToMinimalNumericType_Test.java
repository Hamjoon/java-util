package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import static com.cedarsoftware.util.MathUtilities.parseToMinimalNumericType;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MathUtilities_parseToMinimalNumericType_Test {

    private static Stream<Arguments> integerParsingProvider() {
        return Stream.of(Arguments.of("Simple positive integer within Long range should be parsed as Long.", "123", Long.class, 123L), Arguments.of("Leading zeros are stripped; result fits in Long.", "000123", Long.class, 123L), Arguments.of("All‑zero string becomes a single zero and is returned as Long.", "0000", Long.class, 0L), Arguments.of("Negative integer should keep the sign and be returned as Long.", "-456", Long.class, -456L), Arguments.of("Explicit plus sign is accepted; result is a Long.", "+789", Long.class, 789L), Arguments.of("Combination of sign and leading zeros; sign is preserved and value fits in Long.", "-0000123", Long.class, -123L), Arguments.of("Maximum Long value; should be parsed as Long.", "9223372036854775807", Long.class, 9223372036854775807L), Arguments.of("Value just above Long.MAX_VALUE; method must fall back to BigInteger.", "9223372036854775808", BigInteger.class, new BigInteger("9223372036854775808")), Arguments.of("Value just below Long.MIN_VALUE; should be returned as BigInteger.", "-9223372036854775809", BigInteger.class, new BigInteger("-9223372036854775809")), Arguments.of("Zero without sign is parsed as Long.", "0", Long.class, 0L), Arguments.of("Negative zero is normalized to 0 and returned as Long.", "-0", Long.class, 0L));
    }

    private static Stream<Arguments> decimalParsingProvider() {
        return Stream.of(Arguments.of("16‑digit mantissa with a decimal point fits within Double precision; method should return a Double.", "0.1234567890123456", Double.class, 0.1234567890123456), Arguments.of("17‑digit mantissa exceeds the Double mantissa threshold; method must fall back to BigDecimal.", "0.12345678901234567", BigDecimal.class, new BigDecimal("0.12345678901234567")), Arguments.of("Leading zeros are stripped before parsing; remaining value has a decimal point and fits in Double.", "00012.3400", Double.class, 12.34), Arguments.of("Negative zero with a decimal point should be parsed as a Double preserving the sign (result is -0.0).", "-0.0", Double.class, -0.0), Arguments.of("Explicit plus sign with decimal zero is accepted and returned as a Double.", "+0.0", Double.class, 0.0));
    }

    private static Stream<Arguments> malformedDecimalProvider() {
        return Stream.of(Arguments.of("A string containing only a decimal point has no mantissa; Double.parseDouble(\".\") throws NumberFormatException.", "."), Arguments.of("Trailing decimal point without fractional digits results in Double.parseDouble(\"0.\") which throws NumberFormatException.", "0."), Arguments.of("Leading decimal point without an integer part causes Double.parseDouble(\".123\") to throw NumberFormatException.", ".123"));
    }

    private static Stream<Arguments> malformedExponentProvider() {
        return Stream.of(Arguments.of("Exponent indicator without following digits leads to Double.parseDouble(\"1e\") throwing NumberFormatException.", "1e"), Arguments.of("Upper‑case exponent indicator without digits also triggers Double.parseDouble to fail.", "123E"));
    }

    private static Stream<Arguments> exponentOverflowProvider() {
        return Stream.of(Arguments.of("Exponent value exceeds the range of int; Integer.parseInt throws NumberFormatException before the method can decide to use Double or BigDecimal.", "1e9999999999"), Arguments.of("Even though mantissa size is small, the large exponent (\"1000\") overflows int parsing, causing NumberFormatException.", "0.123e1000"));
    }

    private static Stream<Arguments> nonNumericIntegerProvider() {
        return Stream.of(Arguments.of("String contains a non‑digit character; Long.parseLong throws NumberFormatException.", "12a3"), Arguments.of("Underscores are not stripped by the method; Long.parseLong encounters illegal characters and throws NumberFormatException.", "-9_223_372_036_854_775_808"));
    }

    @Test
    void parseLongWithinRange_returnsLong() {
        Number n = parseToMinimalNumericType("12345");
        assertTrue(n instanceof Long, "Expected a Long for a value that fits in a long");
        assertEquals(12345L, n);
    }

    @Test
    void parseLongOutOfRange_returnsBigInteger() {
        // Value larger than Long.MAX_VALUE
        // Long.MAX_VALUE + 1
        String big = "9223372036854775808";
        Number n = parseToMinimalNumericType(big);
        assertTrue(n instanceof BigInteger, "Expected a BigInteger for a value outside long range");
        assertEquals(new BigInteger(big), n);
    }

    @Test
    void parseNegativeLongWithinRange_returnsLong() {
        Number n = parseToMinimalNumericType("-9876543210");
        assertTrue(n instanceof Long);
        assertEquals(-9876543210L, n);
    }

    @Test
    void parseDoubleSimple_returnsDouble() {
        Number n = parseToMinimalNumericType("3.14159");
        assertTrue(n instanceof Double);
        assertEquals(3.14159, n.doubleValue(), 0.0);
    }

    @Test
    void parseDoubleWithExponentAndSmallMantissa_returnsDouble() {
        // Mantissa length < 17 and exponent magnitude < 308
        Number n = parseToMinimalNumericType("1.23e5");
        assertTrue(n instanceof Double);
        assertEquals(1.23e5, n.doubleValue(), 0.0);
    }

    @Test
    void parseDoubleWithLargeExponent_returnsBigDecimal() {
        // Exponent magnitude >= 308 forces a BigDecimal
        String sci = "1e309";
        Number n = parseToMinimalNumericType(sci);
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal(sci), n);
    }

    @Test
    void parseDoubleWithLongMantissa_returnsBigDecimal() {
        // 18 digits in mantissa -> should be a BigDecimal
        String value = "123456789012345678.0";
        Number n = parseToMinimalNumericType(value);
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal(value), n);
    }

    @Test
    void parseZeroWithLeadingZeros_returnsLongZero() {
        Number n = parseToMinimalNumericType("0000");
        assertTrue(n instanceof Long);
        assertEquals(0L, n);
    }

    @Test
    void parseNegativeZeroWithLeadingZeros_returnsLongZero() {
        Number n = parseToMinimalNumericType("-0000");
        assertTrue(n instanceof Long);
        assertEquals(0L, n);
    }

    @Test
    void parsePositiveWithLeadingPlusAndZeros_returnsLong() {
        Number n = parseToMinimalNumericType("+000123");
        assertTrue(n instanceof Long);
        assertEquals(123L, n);
    }

    @Test
    void parseScientificNotationExactlyDoubleMax_returnsBigDecimal() {
        // Double.MAX_VALUE in scientific notation – algorithm chooses BigDecimal for accuracy
        String maxDoubleSci = "1.7976931348623157e308";
        Number n = parseToMinimalNumericType(maxDoubleSci);
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal(maxDoubleSci), n);
    }

    @Test
    void parseInvalidScientificNotationExceedingDouble_returnsBigDecimal() {
        // Exponent far beyond double range
        String tooBig = "1e1024";
        Number n = parseToMinimalNumericType(tooBig);
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal(tooBig), n);
    }

    @Test
    void parseExponentWithLeadingZeros_returnsDouble() {
        // Exponent has many leading zeros but overall exponent magnitude < 308
        String s = "1.45e+0000000000000000000000307";
        Number n = parseToMinimalNumericType(s);
        assertTrue(n instanceof Double, "Expected Double when exponent magnitude is within double limits");
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("integerParsingProvider")
    void integerParsing_withoutDecimalOrExponent(String description, String input, Class<?> expectedClass, Number expectedValue) {
        Number result = parseToMinimalNumericType(input);
        assertTrue(expectedClass.isInstance(result), () -> "Expected instance of " + expectedClass.getSimpleName() + " for input \"" + input + "\"");
        assertEquals(expectedValue, result, () -> "Incorrect value for input \"" + input + "\"");
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("decimalParsingProvider")
    void decimalParsing_withoutExponent(String description, String input, Class<?> expectedClass, Number expectedValue) {
        Number result = parseToMinimalNumericType(input);
        assertTrue(expectedClass.isInstance(result), () -> "Expected instance of " + expectedClass.getSimpleName() + " for input \"" + input + "\"");
        if (expectedClass == Double.class) {
            assertEquals(((Double) expectedValue).doubleValue(), result.doubleValue(), () -> "Incorrect double value for input \"" + input + "\"");
        } else if (expectedClass == BigDecimal.class) {
            assertEquals(expectedValue, result, () -> "Incorrect BigDecimal value for input \"" + input + "\"");
        }
    }

    // --------------------------------------------------------------------- //
    @Test
    void nullInput_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parseToMinimalNumericType(null), "Passing null should result in NullPointerException (startsWith call).");
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("malformedDecimalProvider")
    void malformedDecimal_withoutDigits(String description, String input) {
        assertThrows(NumberFormatException.class, () -> parseToMinimalNumericType(input), "Expected NumberFormatException for malformed decimal \"" + input + "\"");
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("malformedExponentProvider")
    void malformedExponent_missingDigits(String description, String input) {
        assertThrows(NumberFormatException.class, () -> parseToMinimalNumericType(input), "Expected NumberFormatException for malformed exponent \"" + input + "\"");
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exponentOverflowProvider")
    void exponentOverflow_IntegerParseIntFails(String description, String input) {
        assertThrows(NumberFormatException.class, () -> parseToMinimalNumericType(input), "Expected NumberFormatException due to exponent overflow for input \"" + input + "\"");
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nonNumericIntegerProvider")
    void nonNumericIntegerParsing(String description, String input) {
        assertThrows(NumberFormatException.class, () -> parseToMinimalNumericType(input), "Expected NumberFormatException for non‑numeric integer input \"" + input + "\"");
    }
}
