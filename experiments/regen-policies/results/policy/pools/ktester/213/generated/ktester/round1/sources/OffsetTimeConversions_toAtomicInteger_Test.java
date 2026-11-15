package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OffsetTimeConversions_toAtomicInteger_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter requires a ConverterOptions instance – use the default constructor.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// Valid numeric conversions
        Arguments.of(42, null, 42, "Integer input should be converted directly to an AtomicInteger with the same value."), Arguments.of(123L, null, 123, "Long input within int range should be cast to int and wrapped in an AtomicInteger."), Arguments.of("99", null, 99, "String containing a decimal number should be parsed, converted to long, then to int."), Arguments.of(new BigDecimal("12345.67"), null, 12345, "BigDecimal is truncated to its long value (12345) before being cast to int."), Arguments.of(new BigInteger("9876543210"), null, (int) 9876543210L, "BigInteger larger than int range is cast to int, resulting in overflow (lower 32 bits)."), // Overflow handling
        Arguments.of(5_000_000_000L, null, (int) 5_000_000_000L, "Long value exceeding Integer.MAX_VALUE is truncated to the lower 32 bits when cast to int."), Arguments.of(-5_000_000_000L, null, (int) -5_000_000_000L, "Negative long value exceeding int range wraps around similarly after casting."), // OffsetTime conversion
        Arguments.of(OffsetTime.of(12, 34, 56, 0, ZoneOffset.UTC), null, (int) OffsetTimeConversions.toLong(OffsetTime.of(12, 34, 56, 0, ZoneOffset.UTC), new Converter(new ConverterOptions())), "OffsetTime representing 12:34:56 UTC is converted to seconds since midnight and wrapped."), Arguments.of(OffsetTime.of(23, 59, 59, 999_999_999, ZoneOffset.ofHoursMinutes(5, 30)), null, 86_399, "OffsetTime with maximum time-of-day is converted to seconds since midnight; nanoseconds ignored."), // LocalDate conversion
        Arguments.of(LocalDate.of(1970, 1, 2), null, 1, "LocalDate one day after the epoch is converted to epoch day count (1)."), Arguments.of(LocalDate.of(1969, 12, 31), null, -1, "LocalDate one day before the epoch is converted to epoch day count (-1)."), // Instant conversion
        Arguments.of(Instant.ofEpochSecond(12_345L), null, 12_345, "Instant representing 12345 seconds since the epoch is converted directly."), Arguments.of(Instant.ofEpochMilli(9_876_543_210L), null, 9_876_543, "Instant from milliseconds is converted to seconds (truncating remainder)."), // Floating‑point numeric conversions
        Arguments.of(123.987d, null, 123, "Double value is truncated to its integral part before casting to int."), Arguments.of(-45.67f, null, -45, "Float value is truncated toward zero before conversion."), // Integer boundary values
        Arguments.of(Integer.MAX_VALUE, null, Integer.MAX_VALUE, "Maximum int value is preserved."), Arguments.of(Integer.MIN_VALUE, null, Integer.MIN_VALUE, "Minimum int value is preserved."), // String parsing edge cases
        Arguments.of("  42 ", null, 42, "String with surrounding whitespace is trimmed and parsed correctly."), Arguments.of("+7", null, 7, "String with an explicit plus sign is parsed as a positive number."));
    }

    private static Stream<Arguments> provideExceptionConversions() {
        return Stream.of(// Invalid numeric parsing
        Arguments.of("not-a-number", null, NumberFormatException.class, "A non‑numeric string cannot be parsed to a long; NumberFormatException should propagate."), // Null source (the implementation delegates to toLong which throws NPE)
        Arguments.of(null, null, NullPointerException.class, "Passing null as the source object should cause a NullPointerException inside toLong."), // String edge cases
        Arguments.of("", null, NumberFormatException.class, "Empty string cannot be parsed to a number and triggers NumberFormatException."), // Converter edge cases
        Arguments.of("ignored", mockConverterReturningNull(), NullPointerException.class, "When the supplied Converter returns null, toLong attempts to unbox a null Long, causing NPE."), Arguments.of("ignored", mockConverterReturningBooleanTrue(), ClassCastException.class, "Converter returns a Boolean, which cannot be cast to Number; a ClassCastException is expected."), // Unsupported source type
        Arguments.of(new Object(), null, IllegalArgumentException.class, "Unsupported source type without a Converter should throw IllegalArgumentException."), Arguments.of(new Object(), mockConverterThrowingIllegalStateException(), IllegalStateException.class, "Converter throws IllegalStateException; the exception must propagate."));
    }

    /* --------------------------------------------------------------
     *  Helper methods to create mocked Converters for exception scenarios
     * -------------------------------------------------------------- */
    private static Converter mockConverterReturningNull() {
        Converter mock = Mockito.mock(Converter.class);
        // Simulate returning null for a Long conversion.
        when(mock.convert(any(), eq(Long.class))).thenReturn(null);
        return mock;
    }

    private static Converter mockConverterReturningBooleanTrue() {
        Converter mock = Mockito.mock(Converter.class);
        // Simulate returning a Boolean instead of a Number.
        when(mock.convert(any(), eq(Long.class))).thenReturn(Boolean.TRUE);
        return mock;
    }

    private static Converter mockConverterThrowingIllegalStateException() {
        Converter mock = Mockito.mock(Converter.class);
        // Simulate throwing IllegalStateException from the converter.
        when(mock.convert(any(), eq(Long.class))).thenThrow(new IllegalStateException("converter failure"));
        return mock;
    }

    @Test
    void toAtomicInteger_fromOffsetTime_returnsExpectedValue() {
        // given
        OffsetTime offsetTime = OffsetTime.of(12, 34, 56, 0, ZoneOffset.UTC);
        // when
        AtomicInteger result = OffsetTimeConversions.toAtomicInteger(offsetTime, converter);
        // then
        long expectedLong = OffsetTimeConversions.toLong(offsetTime, converter);
        assertEquals((int) expectedLong, result.intValue(), "AtomicInteger value should match the long conversion of the OffsetTime");
    }

    @Test
    void toAtomicInteger_fromStringNumber_returnsParsedValue() {
        // given
        String source = "12345";
        // when
        AtomicInteger result = OffsetTimeConversions.toAtomicInteger(source, converter);
        // then
        assertEquals(12345, result.intValue(), "AtomicInteger should contain the integer parsed from the string");
    }

    @Test
    void toAtomicInteger_fromLong_returnsSameValue() {
        // given
        Long source = 9876L;
        // when
        AtomicInteger result = OffsetTimeConversions.toAtomicInteger(source, converter);
        // then
        assertEquals(source.intValue(), result.intValue(), "AtomicInteger should contain the int value of the supplied Long");
    }

    @Test
    void toAtomicInteger_fromMapWithValueKey_returnsConvertedValue() {
        // given
        Map<String, Object> source = Map.of("value", "321");
        // when
        AtomicInteger result = OffsetTimeConversions.toAtomicInteger(source, converter);
        // then
        assertEquals(321, result.intValue(), "AtomicInteger should be created from the map's \"value\" entry");
    }

    @Test
    void toAtomicInteger_fromNull_returnsZero() {
        // given
        Object source = null;
        // when
        AtomicInteger result = OffsetTimeConversions.toAtomicInteger(source, converter);
        // then
        assertEquals(0, result.intValue(), "Null input should be converted to an AtomicInteger with value 0");
    }

    /* --------------------------------------------------------------
     *  Successful numeric / temporal conversions
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("provideSuccessfulConversions")
    void testSuccessfulConversions(Object from, Converter conv, int expected, String description) {
        // when
        AtomicInteger result = OffsetTimeConversions.toAtomicInteger(from, conv);
        // then
        assertEquals(expected, result.intValue(), () -> "Expected AtomicInteger value " + expected + " for case: " + description);
    }

    /* --------------------------------------------------------------
     *  Expected exception cases (invalid conversions)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("provideExceptionConversions")
    void testExceptionConversions(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        // when / then
        assertThrows(expectedException, () -> OffsetTimeConversions.toAtomicInteger(from, conv), () -> "Expected exception " + expectedException.getSimpleName() + " for case: " + description);
    }

    /* --------------------------------------------------------------
     *  Custom converter successful conversion
     * -------------------------------------------------------------- */
    @Test
    void testCustomConverterSuccessfulConversion() {
        // given
        // placeholder type
        Object source = new com.example.CustomType("123");
        Converter mockConverter = Mockito.mock(Converter.class);
        // The conversion contract is not defined; we assume a generic convert(Object, Class) method.
        when(mockConverter.convert(any(), eq(Long.class))).thenReturn(123L);
        // when
        AtomicInteger result = OffsetTimeConversions.toAtomicInteger(source, mockConverter);
        // then
        assertEquals(123, result.intValue(), "Custom converter should return Long 123 which is then wrapped in AtomicInteger.");
    }
}
