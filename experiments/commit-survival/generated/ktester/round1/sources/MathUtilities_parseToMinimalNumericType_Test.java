package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MathUtilities_parseToMinimalNumericType_Test {

    /* --------------------------------------------------------------------- */
    private static void assertNumber(Number actual, Class<?> expectedClass, Object expectedValue) {
        assertNotNull(actual, "Result must not be null");
        assertTrue(expectedClass.isInstance(actual), () -> "Expected instance of " + expectedClass.getSimpleName() + " but got " + actual.getClass().getSimpleName());
        assertEquals(expectedValue, actual, () -> "Expected value " + expectedValue + " but got " + actual);
    }

    private static Stream<Arguments> integerParsingProvider() {
        return Stream.of(// Simple positive integer
        Arguments.of("Simple positive integer without sign, length < 19 → parsed as Long.", "12345", "Long 12345"), // 18‑digit integer still fits Long
        Arguments.of("Positive integer with 18 digits (still < 19) → parsed as Long.", "123456789012345678", "Long 123456789012345678"), // Exact Long.MAX_VALUE (19 digits)
        Arguments.of("Maximum long value (19 digits, exactly Long.MAX_VALUE) → parsed via BigInteger then returned as long.", "9223372036854775807", "Long 9223372036854775807"), // Exceeds Long.MAX_VALUE
        Arguments.of("19‑digit integer exceeding Long.MAX_VALUE → parsed as BigInteger.", "9223372036854775808", "BigInteger 9223372036854775808"), // Negative with leading zeros
        Arguments.of("Negative integer with leading zeros; sign preserved and zeros stripped → parsed as Long.", "-00000123", "Long -123"), // Positive sign with leading zeros
        Arguments.of("Explicit plus sign with leading zeros; sign kept, zeros removed → parsed as Long.", "+000123", "Long 123"), // No sign, leading zeros stripped
        Arguments.of("No sign, leading zeros stripped → parsed as Long.", "000123", "Long 123"), // 19‑digit positive less than Long.MAX_VALUE (via BigInteger then back to Long)
        Arguments.of("19‑digit positive integer less than Long.MAX_VALUE; parsed via BigInteger then converted back to Long.", "1000000000000000000", "Long 1000000000000000000"), // Exact Long.MIN_VALUE
        Arguments.of("Exact Long.MIN_VALUE with sign; length > 19 triggers BigInteger path, then converted back to Long.", "-9223372036854775808", "Long -9223372036854775808"), // Below Long.MIN_VALUE
        Arguments.of("Integer one less than Long.MIN_VALUE; outside Long range → returned as BigInteger.", "-9223372036854775809", "BigInteger -9223372036854775809"));
    }

    private static Stream<Arguments> decimalParsingProvider() {
        return Stream.of(Arguments.of("Simple decimal without exponent, mantissa digits < 17 → parsed as Double.", "123.456", "Double 123.456"), Arguments.of("Decimal where total mantissa digits = 17 (16 before '.' + 1 after) → exceeds Double threshold, parsed as BigDecimal.", "1234567890123456.7", "BigDecimal 1234567890123456.7"), Arguments.of("Positive sign with leading zeros; zeros stripped, decimal retained, mantissa < 17 → parsed as Double.", "+00123.4500", "Double 123.45"), Arguments.of("Negative sign with leading zeros before decimal point; after stripping zeros the string becomes \"-.00123\" which is a valid Double.", "-0000.00123", "Double -0.00123"));
    }

    private static Stream<Arguments> exponentParsingProvider() {
        return Stream.of(Arguments.of("Decimal with exponent, mantissa < 17 and exponent magnitude < 308 → parsed as Double.", "1.23e5", "Double 123000.0"), Arguments.of("Exponent magnitude >= 308 forces use of BigDecimal despite small mantissa.", "1e309", "BigDecimal 1E309"), Arguments.of("Negative exponent with magnitude >= 308 (sign ignored for magnitude check) → parsed as BigDecimal.", "1e-309", "BigDecimal 1E-309"), Arguments.of("Explicit plus sign in exponent; magnitude < 308 → parsed as Double.", "1e+5", "Double 100000.0"), Arguments.of("Zero with exponent; still fits Double criteria.", "0e0", "Double 0.0"));
    }

    private static Stream<Arguments> largeMantissaProvider() {
        return Stream.of(Arguments.of("Mantissa digits = 19 (> 17) without exponent → parsed as BigDecimal.", "12345678901234567.89", "BigDecimal 12345678901234567.89"), Arguments.of("Mantissa digits = 16, exponent magnitude = 2 (<308) → parsed as Double.", "1234567890123456e2", "Double 123456789012345600.0"), Arguments.of("Mantissa digits = 17, exponent present → exceeds Double mantissa limit, parsed as BigDecimal.", "12345678901234567e2", "BigDecimal 1234567890123456700"));
    }

    private static Stream<Arguments> exponentBoundaryProvider() {
        return Stream.of(Arguments.of("Exponent magnitude exactly 307 (<308) → parsed as Double.", "1e307", "Double 1.0E307"), Arguments.of("Exponent magnitude exactly 308 (not <308) → parsed as BigDecimal.", "1e308", "BigDecimal 1E308"));
    }

    private static Stream<Arguments> signAndZeroEdgeProvider() {
        return Stream.of(Arguments.of("String \"0\" becomes empty after stripping leading zeros, causing Long.parseLong to fail.", "0", "NumberFormatException"), Arguments.of("All‑zero string stripped to empty → NumberFormatException.", "0000", "NumberFormatException"), Arguments.of("Negative zero becomes \"-\" after sign handling and zero stripping → NumberFormatException.", "-0", "NumberFormatException"), Arguments.of("Positive zero with explicit sign becomes \"+\" after stripping zeros → NumberFormatException.", "+0", "NumberFormatException"));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(Arguments.of("Empty string cannot be parsed; should throw NumberFormatException.", ""), Arguments.of("Non‑numeric characters present; parsing fails.", "abc123"), Arguments.of("Multiple decimal points; invalid numeric format.", "1.2.3"));
    }

    @Test
    void parsesSimpleIntegerWithinLongRange() {
        Number result = MathUtilities.parseToMinimalNumericType("123");
        assertTrue(result instanceof Long, "Result should be a Long");
        assertEquals(123L, result);
    }

    @Test
    void parsesIntegerWithLeadingZeros() {
        Number result = MathUtilities.parseToMinimalNumericType("000123");
        assertTrue(result instanceof Long);
        assertEquals(123L, result);
    }

    @Test
    void parsesNegativeIntegerWithLeadingZeros() {
        Number result = MathUtilities.parseToMinimalNumericType("-00123");
        assertTrue(result instanceof Long);
        assertEquals(-123L, result);
    }

    @Test
    void parsesPositiveSignAndLeadingZeros() {
        Number result = MathUtilities.parseToMinimalNumericType("+00123");
        assertTrue(result instanceof Long);
        assertEquals(123L, result);
    }

    @Test
    void parsesZeroString() {
        Number result = MathUtilities.parseToMinimalNumericType("0");
        assertTrue(result instanceof Long);
        assertEquals(0L, result);
    }

    @Test
    void parsesNegativeZeroString() {
        Number result = MathUtilities.parseToMinimalNumericType("-0");
        assertTrue(result instanceof Long);
        assertEquals(0L, result);
    }

    @Test
    void parsesIntegerExceedingLongMax() {
        // Long.MAX_VALUE = 9223372036854775807
        // Long.MAX_VALUE + 1
        String beyondMax = "9223372036854775808";
        Number result = MathUtilities.parseToMinimalNumericType(beyondMax);
        assertTrue(result instanceof BigInteger);
        assertEquals(new BigInteger(beyondMax), result);
    }

    @Test
    void parsesIntegerBelowLongMin() {
        // Long.MIN_VALUE = -9223372036854775808
        // Long.MIN_VALUE - 1
        String belowMin = "-9223372036854775809";
        Number result = MathUtilities.parseToMinimalNumericType(belowMin);
        assertTrue(result instanceof BigInteger);
        assertEquals(new BigInteger(belowMin), result);
    }

    @Test
    void parsesDecimalWithSmallMantissaToDouble() {
        Number result = MathUtilities.parseToMinimalNumericType("123.45");
        assertTrue(result instanceof Double);
        assertEquals(123.45, (Double) result, 0.000_000_001);
    }

    @Test
    void parsesDecimalWithLargeMantissaToBigDecimal() {
        // 19 fractional digits → mantissaSize >= 17
        Number result = MathUtilities.parseToMinimalNumericType("0.1234567890123456789");
        assertTrue(result instanceof BigDecimal);
        assertEquals(new BigDecimal("0.1234567890123456789"), result);
    }

    @Test
    void parsesScientificNotationWithinExponentRangeToDouble() {
        Number result = MathUtilities.parseToMinimalNumericType("1e5");
        assertTrue(result instanceof Double);
        assertEquals(100000.0, (Double) result, 0.000_001);
    }

    @Test
    void parsesScientificNotationWithLargeExponentToBigDecimal() {
        // Exponent 309 is outside the Double safe range (|exp| < 308)
        Number result = MathUtilities.parseToMinimalNumericType("1e309");
        assertTrue(result instanceof BigDecimal);
        assertEquals(new BigDecimal("1e309"), result);
    }

    @Test
    void parsesDecimalWithExponentAndSmallMantissaToDouble() {
        Number result = MathUtilities.parseToMinimalNumericType("1.2345e2");
        assertTrue(result instanceof Double);
        assertEquals(123.45, (Double) result, 0.000_001);
    }

    @Test
    void parsesDecimalWithExponentAndLargeMantissaToBigDecimal() {
        // Mantissa > 16 digits forces BigDecimal
        Number result = MathUtilities.parseToMinimalNumericType("1234567890123456.78e2");
        assertTrue(result instanceof BigDecimal);
        assertEquals(new BigDecimal("1234567890123456.78e2"), result);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("integerParsingProvider")
    void integerParsing(String description, String input, String expected) {
        if (expected.startsWith("NumberFormatException")) {
            assertThrows(NumberFormatException.class, () -> MathUtilities.parseToMinimalNumericType(input), "Expected NumberFormatException for input: " + input);
        } else {
            String[] parts = expected.split(" ", 2);
            String type = parts[0];
            String valueStr = parts[1];
            Number result = MathUtilities.parseToMinimalNumericType(input);
            switch(type) {
                case "Long":
                    assertNumber(result, Long.class, Long.valueOf(valueStr));
                    break;
                case "BigInteger":
                    assertNumber(result, BigInteger.class, new BigInteger(valueStr));
                    break;
                default:
                    fail("Unsupported expected type: " + type);
            }
        }
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("decimalParsingProvider")
    void decimalParsing(String description, String input, String expected) {
        Number result = MathUtilities.parseToMinimalNumericType(input);
        String[] parts = expected.split(" ", 2);
        String type = parts[0];
        String valueStr = parts[1];
        switch(type) {
            case "Double":
                assertNumber(result, Double.class, Double.valueOf(valueStr));
                break;
            case "BigDecimal":
                assertNumber(result, BigDecimal.class, new BigDecimal(valueStr));
                break;
            default:
                fail("Unsupported expected type: " + type);
        }
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exponentParsingProvider")
    void exponentParsing(String description, String input, String expected) {
        Number result = MathUtilities.parseToMinimalNumericType(input);
        String[] parts = expected.split(" ", 2);
        String type = parts[0];
        String valueStr = parts[1];
        switch(type) {
            case "Double":
                assertNumber(result, Double.class, Double.valueOf(valueStr));
                break;
            case "BigDecimal":
                assertNumber(result, BigDecimal.class, new BigDecimal(valueStr));
                break;
            default:
                fail("Unsupported expected type: " + type);
        }
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("largeMantissaProvider")
    void largeMantissa(String description, String input, String expected) {
        Number result = MathUtilities.parseToMinimalNumericType(input);
        String[] parts = expected.split(" ", 2);
        String type = parts[0];
        String valueStr = parts[1];
        switch(type) {
            case "Double":
                assertNumber(result, Double.class, Double.valueOf(valueStr));
                break;
            case "BigDecimal":
                assertNumber(result, BigDecimal.class, new BigDecimal(valueStr));
                break;
            default:
                fail("Unsupported expected type: " + type);
        }
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exponentBoundaryProvider")
    void exponentBoundary(String description, String input, String expected) {
        Number result = MathUtilities.parseToMinimalNumericType(input);
        String[] parts = expected.split(" ", 2);
        String type = parts[0];
        String valueStr = parts[1];
        switch(type) {
            case "Double":
                assertNumber(result, Double.class, Double.valueOf(valueStr));
                break;
            case "BigDecimal":
                assertNumber(result, BigDecimal.class, new BigDecimal(valueStr));
                break;
            default:
                fail("Unsupported expected type: " + type);
        }
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("signAndZeroEdgeProvider")
    void signAndZeroEdge(String description, String input, String expected) {
        if (expected.equals("NumberFormatException")) {
            assertThrows(NumberFormatException.class, () -> MathUtilities.parseToMinimalNumericType(input), "Expected NumberFormatException for input: " + input);
        } else {
            // Not expected in current JSON, but kept for completeness
            fail("Unexpected successful parse for input expected to fail: " + input);
        }
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidInputProvider")
    void invalidInput(String description, String input) {
        assertThrows(NumberFormatException.class, () -> MathUtilities.parseToMinimalNumericType(input), "Expected NumberFormatException for input: " + input);
    }
}
