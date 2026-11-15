package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toAtomicLong_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default constructor provides sensible defaults.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared converter reference.
        converter = null;
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(Arguments.of("Long instance is converted directly", 123L, 123L), Arguments.of("String numeric value is parsed", "42", 42L), Arguments.of("BigInteger is converted to its long value", new BigInteger("9876543210"), 9876543210L), Arguments.of("LocalTime is converted to nanoseconds of day", LocalTime.of(12, 34, 56, 123456789), // ((12*3600 + 34*60 + 56) * 1_000_000_000) + 123_456_789
        ((12L * 3600 + 34L * 60 + 56L) * 1_000_000_000L) + 123_456_789L), Arguments.of("AtomicLong input yields the same value", new AtomicLong(5555L), 5555L));
    }

    private static Stream<Arguments> illegalArgumentProvider() {
        return Stream.of(Arguments.of("Boolean value is not convertible", Boolean.TRUE), Arguments.of("Arbitrary Object has no conversion path", new Object()), Arguments.of("Character type is unsupported", 'A'), Arguments.of("java.util.Date is not a recognized source type", new java.util.Date()), Arguments.of("StringBuilder is not directly supported", new StringBuilder("123")), Arguments.of("AtomicInteger is not a recognized source type", new AtomicInteger(7)));
    }

    private static Stream<Arguments> stringEdgeCaseProvider() {
        return Stream.of(Arguments.of("String with leading/trailing whitespace is trimmed and parsed", "   123   "), Arguments.of("Empty string cannot be parsed", ""), Arguments.of("Hexadecimal notation is not accepted", "0xFF"), Arguments.of("String containing only whitespace after trim", "   "), Arguments.of("String with only a plus sign", "+"), Arguments.of("String with only a minus sign", "-"), Arguments.of("String just above Long.MAX_VALUE overflows", "9223372036854775808"), Arguments.of("String just below Long.MIN_VALUE underflows", "-9223372036854775809"));
    }

    @Test
    void toAtomicLong_NullInput_ReturnsZero() {
        // given
        Object from = null;
        // when
        AtomicLong result = LocalTimeConversions.toAtomicLong(from, converter);
        // then
        assertNotNull(result, "Resulting AtomicLong should never be null");
        assertEquals(0L, result.get(), "Null input should convert to 0");
    }

    @Test
    void toAtomicLong_StringNumber_ReturnsParsedValue() {
        // given
        String from = "12345";
        // when
        AtomicLong result = LocalTimeConversions.toAtomicLong(from, converter);
        // then
        assertEquals(12345L, result.get(), "String numeric value should be parsed to long");
    }

    @Test
    void toAtomicLong_LongInput_ReturnsSameValue() {
        // given
        Long from = 9876L;
        // when
        AtomicLong result = LocalTimeConversions.toAtomicLong(from, converter);
        // then
        assertEquals(9876L, result.get(), "Long input should be returned unchanged");
    }

    @Test
    void toAtomicLong_AtomicLongInput_ReturnsSameValue() {
        // given
        AtomicLong from = new AtomicLong(55555L);
        // when
        AtomicLong result = LocalTimeConversions.toAtomicLong(from, converter);
        // then
        assertEquals(55555L, result.get(), "AtomicLong input should be returned unchanged");
    }

    @Test
    void toAtomicLong_LocalTimeInput_ReturnsNanosOfDay() {
        // given
        LocalTime time = LocalTime.of(12, 34, 56, 123_456_789);
        // conversion rule for LocalTime → long in the library
        long expectedNanos = time.toNanoOfDay();
        // when
        AtomicLong result = LocalTimeConversions.toAtomicLong(time, converter);
        // then
        assertEquals(expectedNanos, result.get(), "LocalTime should be converted to the number of nanoseconds since start of day");
    }

    /**
     * Valid conversions – happy path.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toAtomicLong_ValidConversions(String description, Object from, long expected) {
        AtomicLong result = LocalTimeConversions.toAtomicLong(from, converter);
        assertEquals(expected, result.get(), description);
    }

    /**
     * Null handling – when the source is null.
     */
    @Test
    void toAtomicLong_NullInput_ZeroResult() {
        AtomicLong result = LocalTimeConversions.toAtomicLong(null, null);
        assertEquals(0L, result.get(), "Null source with null converter should yield 0");
    }

    /**
     * Invalid type handling – expecting IllegalArgumentException.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("illegalArgumentProvider")
    void toAtomicLong_InvalidTypes_ThrowIllegalArgumentException(String description, Object from) {
        assertThrows(IllegalArgumentException.class, () -> LocalTimeConversions.toAtomicLong(from, converter), description);
    }

    /**
     * String edge cases – parsing errors.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringEdgeCaseProvider")
    void toAtomicLong_StringEdgeCases_ThrowNumberFormatException(String description, String from) {
        assertThrows(NumberFormatException.class, () -> LocalTimeConversions.toAtomicLong(from, converter), description);
    }

    /**
     * Boundary values – extreme long values.
     */
    @Test
    void toAtomicLong_BoundaryValues_StringMaxLong() {
        String from = "9223372036854775807";
        AtomicLong result = LocalTimeConversions.toAtomicLong(from, converter);
        assertEquals(Long.MAX_VALUE, result.get(), "Long.MAX_VALUE string should be parsed correctly");
    }

    @Test
    void toAtomicLong_BoundaryValues_StringMinLong() {
        String from = "-9223372036854775808";
        AtomicLong result = LocalTimeConversions.toAtomicLong(from, converter);
        assertEquals(Long.MIN_VALUE, result.get(), "Long.MIN_VALUE string should be parsed correctly");
    }

    /**
     * BigDecimal overflow – expecting ArithmeticException.
     */
    @Test
    void toAtomicLong_BigDecimalOverflow_ThrowsArithmeticException() {
        BigDecimal big = new BigDecimal("12345678901234567890");
        assertThrows(ArithmeticException.class, () -> LocalTimeConversions.toAtomicLong(big, converter), "BigDecimal exceeding long range should cause ArithmeticException");
    }

    /**
     * BigDecimal with fractional part – expecting ArithmeticException.
     */
    @Test
    void toAtomicLong_BigDecimalFraction_ThrowsArithmeticException() {
        BigDecimal fractional = new BigDecimal("1.5");
        assertThrows(ArithmeticException.class, () -> LocalTimeConversions.toAtomicLong(fractional, converter), "BigDecimal with fractional part cannot be represented as long");
    }

    /**
     * Custom converter usage – the converter forces the result to 999.
     */
    @Test
    void toAtomicLong_CustomConverter_AlwaysReturns999() {
        // Arrange: mock a Converter that returns 999L for any conversion to Long
        Converter customConverter = mock(Converter.class);
        when(customConverter.convert(any(), eq(Long.class))).thenReturn(999L);
        // The source value is irrelevant; we use a plain string.
        String from = "any value";
        // Act
        AtomicLong result = LocalTimeConversions.toAtomicLong(from, customConverter);
        // Assert
        assertEquals(999L, result.get(), "Custom converter should force the long value to 999");
        // Verify that the mock was indeed called.
        verify(customConverter, times(1)).convert(eq(from), eq(Long.class));
    }
}
