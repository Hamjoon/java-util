package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import static com.cedarsoftware.util.MathUtilities.parseToMinimalNumericType;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MathUtilities_parseToMinimalNumericType_Test {

    private static Stream<Arguments> integerWithoutSignProvider() {
        return Stream.of(Arguments.of("12345", 12345L), // leading zeros stripped
        Arguments.of("000456", 456L));
    }

    private static Stream<Arguments> signHandlingProvider() {
        return Stream.of(Arguments.of("+00123", 123L), Arguments.of("-00000123", -123L));
    }

    private static Stream<Arguments> longBoundaryProvider() {
        return Stream.of(Arguments.of("9223372036854775807", Long.MAX_VALUE), Arguments.of("-9223372036854775808", Long.MIN_VALUE));
    }

    private static Stream<Arguments> doubleDecimalProvider() {
        return Stream.of(Arguments.of("123.456", 123.456), Arguments.of("-0.00123", -0.00123), Arguments.of("3.1415", 3.1415), Arguments.of("-0000.00123", -0.00123), Arguments.of("1.23e2", 123.0), Arguments.of("1.23e5", 123000.0), Arguments.of("1.7976931348623157e308", Double.MAX_VALUE), Arguments.of("2.2250738585072014e-308", Double.parseDouble("2.2250738585072014e-308")), // mantissa <17, exponent 307 <308
        Arguments.of("1.45e+0000000000000000000000307", 1.45e307));
    }

    private static Stream<Arguments> bigDecimalProvider() {
        return Stream.of(// mantissa >= 17 digits
        Arguments.of("123456789012345678.9", "123456789012345678.9"), // exponent magnitude >= 308
        Arguments.of("1e309", "1e309"), Arguments.of("1e1024", "1e1024"), // mantissa size exactly 17 with exponent forces BigDecimal
        Arguments.of("12345678901234567e2", "12345678901234567e2"), // large exponent with decimal point
        Arguments.of("1.23e309", "1.23e309"));
    }

    @Test
    void parsesSimpleLongWithinRange() {
        Number n = parseToMinimalNumericType("12345");
        assertTrue(n instanceof Long, "Should be a Long for values fitting in long");
        assertEquals(12345L, n);
    }

    @Test
    void parsesNegativeLongWithLeadingZeros() {
        Number n = parseToMinimalNumericType("-00000123");
        assertTrue(n instanceof Long);
        assertEquals(-123L, n);
    }

    @Test
    void parsesBigIntegerWhenBeyondLongRange() {
        // Long.MAX_VALUE + 1
        String big = "9223372036854775808";
        Number n = parseToMinimalNumericType(big);
        assertTrue(n instanceof BigInteger, "Should fall back to BigInteger");
        assertEquals(new BigInteger(big), n);
    }

    @Test
    void parsesBigIntegerWithExplicitSignAndZeros() {
        // Long.MAX_VALUE with plus sign
        String val = "+000009223372036854775807";
        Number n = parseToMinimalNumericType(val);
        assertTrue(n instanceof Long);
        assertEquals(Long.MAX_VALUE, n);
    }

    @Test
    void parsesSimpleDouble() {
        Number n = parseToMinimalNumericType("3.1415");
        assertTrue(n instanceof Double);
        assertEquals(3.1415, n);
    }

    @Test
    void parsesDoubleWithExponentWithinRange() {
        Number n = parseToMinimalNumericType("1.23e2");
        assertTrue(n instanceof Double);
        assertEquals(123.0, n);
    }

    @Test
    void switchesToBigDecimalWhenExponentTooLargeForDouble() {
        // Exponent 1024 is far beyond Double's max exponent (≈308)
        String sci = "1e1024";
        Number n = parseToMinimalNumericType(sci);
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal(sci), n);
    }

    @Test
    void switchesToBigDecimalWhenMantissaExceedsDoublePrecision() {
        // 18 digits before decimal point – beyond the 17‑digit safe zone
        String val = "1234567890123456789.0";
        Number n = parseToMinimalNumericType(val);
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal(val), n);
    }

    @Test
    void parsesScientificNotationAtDoubleBoundary() {
        // Double.MAX_VALUE expressed in scientific notation – should be a Double
        String maxDoubleSci = "1.7976931348623157e308";
        Number n = parseToMinimalNumericType(maxDoubleSci);
        assertTrue(n instanceof Double);
        assertEquals(Double.MAX_VALUE, n);
    }

    @Test
    void parsesNegativeDoubleWithLeadingZerosAndSign() {
        Number n = parseToMinimalNumericType("-0000.00123");
        assertTrue(n instanceof Double);
        assertEquals(-0.00123, n);
    }

    @Test
    void parsesExponentWithManyLeadingZeros() {
        String s = "1.45e+0000000000000000000000307";
        Number n = parseToMinimalNumericType(s);
        // Mantissa size is small (<17) and exponent 307 < 308 → Double
        assertTrue(n instanceof Double);
    }

    @Test
    void parsesZeroString() {
        Number n = parseToMinimalNumericType("0");
        assertTrue(n instanceof Long);
        assertEquals(0L, n);
    }

    @Test
    void parsesNegativeZeroString() {
        Number n = parseToMinimalNumericType("-0");
        // After sign handling the string becomes "-0" → parsed as Long 0
        assertTrue(n instanceof Long);
        assertEquals(0L, n);
    }

    @Test
    void parsesVerySmallDoubleScientificNotation() {
        // Smallest positive normal double
        String tiny = "2.2250738585072014e-308";
        Number n = parseToMinimalNumericType(tiny);
        assertTrue(n instanceof Double);
        assertEquals(Double.parseDouble(tiny), n);
    }

    @Test
    void parsesBigDecimalWhenDecimalPointAndLargeExponent() {
        // Exponent 309 exceeds Double's safe exponent range
        String val = "1.23e309";
        Number n = parseToMinimalNumericType(val);
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal(val), n);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] parse \"{0}\" → Long {1}")
    @MethodSource("integerWithoutSignProvider")
    void parsesIntegerWithoutSign(String input, long expected) {
        Number n = parseToMinimalNumericType(input);
        assertTrue(n instanceof Long, "Result should be a Long");
        assertEquals(expected, n);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] parse \"{0}\" → Long {1}")
    @MethodSource("signHandlingProvider")
    void parsesSignAndLeadingZeros(String input, long expected) {
        Number n = parseToMinimalNumericType(input);
        assertTrue(n instanceof Long, "Result should be a Long");
        assertEquals(expected, n);
    }

    @Test
    void negativeZeroThrowsNumberFormatException() {
        // After sign handling "-0000" becomes "-" which cannot be parsed.
        assertThrows(NumberFormatException.class, () -> parseToMinimalNumericType("-0000"));
    }

    @Test
    void plainZeroThrowsNumberFormatException() {
        // "0" is stripped to an empty string → Long.parseLong throws.
        assertThrows(NumberFormatException.class, () -> parseToMinimalNumericType("0"));
    }

    @Test
    void negativeZeroStringThrowsNumberFormatException() {
        // "-0" becomes "-" after sign handling.
        assertThrows(NumberFormatException.class, () -> parseToMinimalNumericType("-0"));
    }

    @Test
    void plusZeroThrowsNumberFormatException() {
        // "+0" becomes "+" after sign handling.
        assertThrows(NumberFormatException.class, () -> parseToMinimalNumericType("+0"));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] parse \"{0}\" → Long {1}")
    @MethodSource("longBoundaryProvider")
    void parsesLongBoundaryValues(String input, long expected) {
        Number n = parseToMinimalNumericType(input);
        assertTrue(n instanceof Long, "Result should be a Long");
        assertEquals(expected, n);
    }

    @Test
    void parsesVeryLargeBigInteger() {
        String big = "12345678901234567890";
        Number n = parseToMinimalNumericType(big);
        assertTrue(n instanceof BigInteger);
        assertEquals(new BigInteger(big), n);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] parse \"{0}\" → Double {1}")
    @MethodSource("doubleDecimalProvider")
    void parsesDecimalAsDouble(String input, double expected) {
        Number n = parseToMinimalNumericType(input);
        assertTrue(n instanceof Double, "Result should be a Double");
        assertEquals(expected, n);
    }

    @ParameterizedTest(name = "[{index}] parse \"{0}\" → BigDecimal")
    @MethodSource("bigDecimalProvider")
    void parsesDecimalAsBigDecimal(String input, String expectedString) {
        Number n = parseToMinimalNumericType(input);
        assertTrue(n instanceof BigDecimal, "Result should be a BigDecimal");
        assertEquals(new BigDecimal(expectedString), n);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void exponentOverflowThrowsNumberFormatException() {
        // Integer.parseInt on the exponent part overflows → NumberFormatException
        assertThrows(NumberFormatException.class, () -> parseToMinimalNumericType("1e9999999999"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullInputThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parseToMinimalNumericType(null));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] parse \"{0}\" → NumberFormatException")
    @ValueSource(strings = { " 123", "123 " })
    void whitespaceInputThrowsNumberFormatException(String input) {
        assertThrows(NumberFormatException.class, () -> parseToMinimalNumericType(input));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] parse \"{0}\" → NumberFormatException")
    @ValueSource(strings = { ".", "e10", "12a3", "1e+", "+", "" })
    void malformedLiteralsThrowNumberFormatException(String input) {
        assertThrows(NumberFormatException.class, () -> parseToMinimalNumericType(input));
    }

    @Test
    void parsesBigIntegerWhenBeyondLongRange_Original() {
        // Long.MAX_VALUE + 1
        String big = "9223372036854775808";
        Number n = parseToMinimalNumericType(big);
        assertTrue(n instanceof BigInteger);
        assertEquals(new BigInteger(big), n);
    }
}
