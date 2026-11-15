package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalTime;
import java.util.Date;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toAtomicInteger_Test {

    /**
     * The shared Converter used for all tests.
     */
    private static Converter converter;

    @Mock
    private Converter mockConverterReturning999;

    @Mock
    private Converter exceptionThrowingConverter;

    /**
     * Simple POJO used for custom‑object tests.
     */
    private static class CustomObject {

        final String payload;

        CustomObject(String payload) {
            this.payload = payload;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance – the default options are sufficient
        // for the built‑in conversions we are exercising.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release; the static Converter will be GC‑ed after the suite.
        converter = null;
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static BigInteger bi(String value) {
        return new BigInteger(value);
    }

    private static LocalTime lt(int hour, int minute, int second, int nano) {
        return LocalTime.of(hour, minute, second, nano);
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Integer
        Arguments.of(42, 42, "Integer input"), // Long larger than int (truncation)
        Arguments.of(1234567890123L, (int) 1234567890123L, "Long input larger than Integer.MAX_VALUE"), // String numeric
        Arguments.of("100", 100, "String containing a decimal number"), // String with whitespace
        Arguments.of("   250   ", 250, "String with surrounding whitespace"), // Double (truncates fractional part)
        Arguments.of(12345.67, 12345, "Double input"), // String representing a double
        Arguments.of("12345.67", 12345, "String representing a decimal number"), // BigDecimal
        Arguments.of(bd("98765.4321"), 98765, "BigDecimal with fractional part"), // BigInteger overflow
        Arguments.of(bi("2147483648"), (int) 2147483648L, "BigInteger one greater than Integer.MAX_VALUE"), // LocalTime (12:34:56)
        Arguments.of(lt(12, 34, 56, 0), (int) (12 * 60 * 60 * 1_000_000_000L + 34 * 60 * 1_000_000_000L + 56 * 1_000_000_000L), "LocalTime midday"), // Boolean true / false
        Arguments.of(Boolean.TRUE, 1, "Boolean true"), Arguments.of(Boolean.FALSE, 0, "Boolean false"));
    }

    private static Stream<Arguments> negativeZeroProvider() {
        return Stream.of(Arguments.of(0, 0, "Zero integer"), Arguments.of(-123, -123, "Negative integer"), Arguments.of("-456", -456, "Negative numeric string"), Arguments.of(-12345.67, -12345, "Negative double"));
    }

    private static Stream<Arguments> boundaryLongProvider() {
        return Stream.of(Arguments.of(Long.MAX_VALUE, (int) Long.MAX_VALUE, "Long.MAX_VALUE"), Arguments.of(Long.MIN_VALUE, (int) Long.MIN_VALUE, "Long.MIN_VALUE"));
    }

    private static Stream<Arguments> bigDecimalEdgeProvider() {
        return Stream.of(Arguments.of(bd("9223372036854775807"), (int) Long.MAX_VALUE, "BigDecimal > Integer.MAX_VALUE"), Arguments.of(bd("-9223372036854775809"), (int) Long.MIN_VALUE, "BigDecimal just below Long.MIN_VALUE"));
    }

    private static Stream<Arguments> bigIntegerEdgeProvider() {
        return Stream.of(Arguments.of(bi("9223372036854775807"), (int) Long.MAX_VALUE, "BigInteger > Integer.MAX_VALUE"), Arguments.of(bi("-9223372036854775809"), (int) Long.MIN_VALUE, "BigInteger just below Long.MIN_VALUE"));
    }

    private static Stream<Arguments> stringFormattingProvider() {
        return Stream.of(Arguments.of("+007", 7, "String with plus sign and leading zeros"), Arguments.of("000123", 123, "String with leading zeros"), Arguments.of("1e3", 1000, "Scientific notation"));
    }

    private static Stream<Arguments> unsupportedTypesProvider() {
        return Stream.of(Arguments.of(new Object(), "plain Object"), Arguments.of(new Date(), "java.util.Date"), Arguments.of(new StringBuilder("123"), "StringBuilder (CharSequence)"), Arguments.of(new int[] { 1, 2, 3 }, "int[] array"));
    }

    private static Stream<Arguments> specialFloatingProvider() {
        return Stream.of(Arguments.of("NaN", NumberFormatException.class, "String \"NaN\""), Arguments.of("Infinity", NumberFormatException.class, "String \"Infinity\""), Arguments.of(Double.NaN, IllegalArgumentException.class, "Double.NaN"), Arguments.of(Double.POSITIVE_INFINITY, IllegalArgumentException.class, "Double.POSITIVE_INFINITY"), Arguments.of(Float.NaN, IllegalArgumentException.class, "Float.NaN"), Arguments.of(Float.POSITIVE_INFINITY, IllegalArgumentException.class, "Float.POSITIVE_INFINITY"));
    }

    private static Stream<Arguments> invalidNumericStringProvider() {
        return Stream.of(Arguments.of("123abc", "Alphanumeric suffix"), Arguments.of("0xFF", "Hexadecimal notation"));
    }

    @Test
    @DisplayName("Convert Integer to AtomicInteger")
    void testFromInteger() {
        Integer source = 42;
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(source, converter);
        assertNotNull(result);
        assertEquals(42, result.intValue(), "AtomicInteger should contain the integer value");
    }

    @Test
    @DisplayName("Convert Long to AtomicInteger (truncates to int)")
    void testFromLong() {
        Long source = 123L;
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(source, converter);
        assertNotNull(result);
        assertEquals(123, result.intValue(), "AtomicInteger should contain the long value truncated to int");
    }

    @Test
    @DisplayName("Convert numeric String to AtomicInteger")
    void testFromString() {
        String source = "7";
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(source, converter);
        assertNotNull(result);
        assertEquals(7, result.intValue(), "AtomicInteger should contain the numeric value parsed from the string");
    }

    @Test
    @DisplayName("Convert null to AtomicInteger (defaults to zero)")
    void testFromNull() {
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(null, converter);
        assertNotNull(result);
        assertEquals(0, result.intValue(), "Null input should result in an AtomicInteger with value 0");
    }

    @Test
    @DisplayName("Convert Double to AtomicInteger (truncates fractional part)")
    void testFromDouble() {
        Double source = 9.99;
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(source, converter);
        assertNotNull(result);
        // The built‑in conversion to long truncates the fractional part.
        assertEquals(9, result.intValue(), "AtomicInteger should contain the integral part of the double");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("validConversionProvider")
    @DisplayName("Valid conversions to AtomicInteger")
    void testValidConversions(Object source, int expected, String description) {
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(source, converter);
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result.intValue(), () -> "AtomicInteger value mismatch for case: " + description);
    }

    @Test
    @DisplayName("Null source should throw NullPointerException")
    void testNullSourceThrows() {
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toAtomicInteger(null, converter), "Null source must raise NullPointerException");
    }

    @Test
    @DisplayName("Empty string should throw NumberFormatException")
    void testEmptyStringThrows() {
        assertThrows(NumberFormatException.class, () -> LocalTimeConversions.toAtomicInteger("", converter), "Empty string must raise NumberFormatException");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("negativeZeroProvider")
    @DisplayName("Negative and zero conversions")
    void testNegativeZero(Object source, int expected, String description) {
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(source, converter);
        assertEquals(expected, result.intValue(), () -> "Unexpected value for: " + description);
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("boundaryLongProvider")
    @DisplayName("Boundary long values")
    void testBoundaryLongs(Long source, int expected) {
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(source, converter);
        assertEquals(expected, result.intValue(), "Incorrect truncation for boundary long value");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("bigDecimalEdgeProvider")
    @DisplayName("BigDecimal edge cases")
    void testBigDecimalEdges(BigDecimal source, int expected, String description) {
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(source, converter);
        assertEquals(expected, result.intValue(), () -> "Failed for: " + description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("bigIntegerEdgeProvider")
    @DisplayName("BigInteger edge cases")
    void testBigIntegerEdges(BigInteger source, int expected, String description) {
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(source, converter);
        assertEquals(expected, result.intValue(), () -> "Failed for: " + description);
    }

    @Test
    @DisplayName("LocalTime at end of day overflow")
    void testLocalTimeOverflow() {
        LocalTime endOfDay = lt(23, 59, 59, 999_999_999);
        // Expected value: low 32 bits of 86_399_999_999_999 nanoseconds
        // 86_399_999_999_999
        long totalNanos = endOfDay.toNanoOfDay();
        int expected = (int) totalNanos;
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(endOfDay, converter);
        assertEquals(expected, result.intValue(), "Incorrect low‑32‑bit value for end‑of‑day LocalTime");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("stringFormattingProvider")
    @DisplayName("String formatting variations")
    void testStringFormatting(String source, int expected, String description) {
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(source, converter);
        assertEquals(expected, result.intValue(), () -> "Failed for: " + description);
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("unsupportedTypesProvider")
    @DisplayName("Unsupported source types should throw IllegalArgumentException")
    void testUnsupportedTypes(Object source, String description) {
        assertThrows(IllegalArgumentException.class, () -> LocalTimeConversions.toAtomicInteger(source, converter), () -> "Expected IllegalArgumentException for " + description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("specialFloatingProvider")
    @DisplayName("Special floating‑point values")
    void testSpecialFloatingValues(Object source, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> LocalTimeConversions.toAtomicInteger(source, converter), () -> "Expected " + expectedException.getSimpleName() + " for " + description);
    }

    @Test
    @DisplayName("String with only whitespace should throw NumberFormatException")
    void testWhitespaceStringThrows() {
        assertThrows(NumberFormatException.class, () -> LocalTimeConversions.toAtomicInteger("   ", converter), "Whitespace‑only string must raise NumberFormatException");
    }

    @ParameterizedTest(name = "Invalid numeric string: {1}")
    @MethodSource("invalidNumericStringProvider")
    @DisplayName("Invalid numeric strings")
    void testInvalidNumericStrings(String source, String description) {
        assertThrows(NumberFormatException.class, () -> LocalTimeConversions.toAtomicInteger(source, converter), () -> "Expected NumberFormatException for " + description);
    }

    @Test
    @DisplayName("Mocked Converter returning 999 should be wrapped in AtomicInteger")
    void testMockConverterReturnsValue() {
        // Stub the mock to return 999L when asked to convert any object to Long
        when(mockConverterReturning999.convert(any(), eq(Long.class))).thenReturn(999L);
        CustomObject payload = new CustomObject("ignored");
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(payload, mockConverterReturning999);
        assertEquals(999, result.intValue(), "Mock converter's long value should be wrapped in AtomicInteger");
        // Verify that the converter was indeed invoked
        verify(mockConverterReturning999).convert(eq(payload), eq(Long.class));
    }

    @Test
    @DisplayName("Exception thrown by Converter should propagate")
    void testConverterExceptionPropagation() {
        when(exceptionThrowingConverter.convert(any(), eq(Long.class))).thenThrow(new RuntimeException("Conversion failure"));
        assertThrows(RuntimeException.class, () -> LocalTimeConversions.toAtomicInteger(123, exceptionThrowingConverter), "RuntimeException from converter must propagate");
    }

    @Test
    @DisplayName("Custom object without a converter should raise IllegalArgumentException")
    void testCustomObjectWithoutConverter() {
        CustomObject obj = new CustomObject("data");
        assertThrows(IllegalArgumentException.class, () -> LocalTimeConversions.toAtomicInteger(obj, converter), "Custom object without conversion support must raise IllegalArgumentException");
    }
}
