package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toAtomicLong_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * Simple placeholder class used for custom‑converter tests.
     */
    static final class CustomObject {
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the Converter constructor requires a non‑null options object.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared Converter reference.
        converter = null;
    }

    private static Stream<Arguments> validConversionsProvider() {
        return Stream.of(// Long
        Arguments.of(123L, 123L, "Direct conversion when source is a Long"), // Integer
        Arguments.of(45, 45L, "Integer should be coerced to long"), // Numeric String
        Arguments.of("6789", 6789L, "String representing a decimal number"), // ISO‑local‑time string
        Arguments.of("12:34:56", LocalTime.parse("12:34:56").toNanoOfDay(), "ISO‑local‑time string → nanoseconds of day"), // LocalTime instance
        Arguments.of(LocalTime.of(1, 2, 3, 4), LocalTime.of(1, 2, 3, 4).toNanoOfDay(), "LocalTime instance → nanoseconds of day"), // Calendar (set to 2023‑01‑01T00:00:00Z)
        Arguments.of(createCalendarUtc(2023, 1, 1, 0, 0, 0), 1672531200000L, "Calendar → epoch‑millis"), // BigDecimal (truncated)
        Arguments.of(new BigDecimal("12345.67"), 12345L, "BigDecimal is truncated toward zero"), // Date (epoch 0)
        Arguments.of(new Date(0L), 0L, "java.util.Date → epoch‑millis"), // LocalTime last nanosecond of day
        Arguments.of(LocalTime.of(23, 59, 59, 999_999_999), 86_399_999_999_999L, "LocalTime at last nanosecond of day"));
    }

    private static Calendar createCalendarUtc(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(year, month - 1, day, hour, minute, second);
        return cal;
    }

    private static Stream<Arguments> numericEdgeCasesProvider() {
        return Stream.of(Arguments.of((short) 7, 7L, "Short → long"), Arguments.of((byte) -3, -3L, "Byte → long preserving sign"), Arguments.of(12.34f, 12L, "Float → truncated long"), Arguments.of(-56.78d, -56L, "Double → truncated long"));
    }

    private static Stream<Arguments> bigIntegerOverflowProvider() {
        return Stream.of(Arguments.of(new BigInteger("9223372036854775807"), Long.MAX_VALUE, "BigInteger == Long.MAX_VALUE"), Arguments.of(new BigInteger("9223372036854775808"), Long.MIN_VALUE, "BigInteger > Long.MAX_VALUE wraps to Long.MIN_VALUE"));
    }

    private static Stream<Arguments> unsupportedTypesProvider() {
        return Stream.of(Arguments.of(new Object(), IllegalArgumentException.class, "Plain Object without converter should fail"), Arguments.of("   42   ", IllegalArgumentException.class, "String with whitespace – conversion should still succeed (trimmed)"), Arguments.of(true, IllegalArgumentException.class, "Boolean cannot be converted to long"));
    }

    private static Stream<Arguments> unsupportedTemporalProvider() {
        return Stream.of(Arguments.of(LocalDate.of(2023, 1, 1), IllegalArgumentException.class, "LocalDate is not supported"), Arguments.of(LocalDateTime.of(2023, 1, 1, 12, 0, 0), IllegalArgumentException.class, "LocalDateTime is not supported"), Arguments.of(Instant.ofEpochMilli(0L), IllegalArgumentException.class, "Instant is not supported"), Arguments.of(OffsetTime.now(), IllegalArgumentException.class, "OffsetTime is not supported"));
    }

    @Test
    void toAtomicLong_fromLong_returnsSameValue() {
        Long source = 12345L;
        AtomicLong result = LocalTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result, "Result must not be null");
        assertEquals(source.longValue(), result.get(), "AtomicLong should contain the same long value");
    }

    @Test
    void toAtomicLong_fromStringNumeric_returnsParsedValue() {
        String source = "987654321";
        AtomicLong result = LocalTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(987654321L, result.get());
    }

    @Test
    void toAtomicLong_fromInteger_returnsConvertedLong() {
        Integer source = 42;
        AtomicLong result = LocalTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(42L, result.get());
    }

    @Test
    void toAtomicLong_fromLocalTime_returnsNanosOfDay() {
        // LocalTime is converted to its nanosecond‑of‑day representation.
        LocalTime source = LocalTime.of(12, 34, 56, 123_456_789);
        // conversion performed by LocalTimeConversions.toLong internally
        long expected = source.toNanoOfDay();
        AtomicLong result = LocalTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @Test
    void toAtomicLong_fromNull_returnsZeroAtomicLong() {
        // According to Converter contract, null source yields 0 for numeric primitives.
        AtomicLong result = LocalTimeConversions.toAtomicLong(null, converter);
        assertNotNull(result);
        assertEquals(0L, result.get());
    }

    @Test
    void toAtomicLong_fromEmptyString_returnsZeroAtomicLong() {
        // Empty string cannot be parsed to a number; the conversion falls back to 0.
        AtomicLong result = LocalTimeConversions.toAtomicLong("", converter);
        assertNotNull(result);
        assertEquals(0L, result.get());
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionsProvider")
    void toAtomicLong_validConversions(Object from, long expected, String description) {
        AtomicLong result = LocalTimeConversions.toAtomicLong(from, converter);
        assertNotNull(result, description);
        assertEquals(expected, result.get(), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("numericEdgeCasesProvider")
    void toAtomicLong_numericEdgeCases(Object from, long expected, String description) {
        AtomicLong result = LocalTimeConversions.toAtomicLong(from, converter);
        assertEquals(expected, result.get(), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("bigIntegerOverflowProvider")
    void toAtomicLong_bigIntegerOverflow(Object from, long expected, String description) {
        AtomicLong result = LocalTimeConversions.toAtomicLong(from, converter);
        assertEquals(expected, result.get(), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("unsupportedTypesProvider")
    void toAtomicLong_unsupportedTypes(Object from, Class<? extends Throwable> exception, String description) {
        assertThrows(exception, () -> LocalTimeConversions.toAtomicLong(from, converter), description);
    }

    @Test
    void toAtomicLong_numericStringOverflow_throwsNumberFormatException() {
        // > Long.MAX_VALUE
        String overflow = "9223372036854775808";
        assertThrows(NumberFormatException.class, () -> LocalTimeConversions.toAtomicLong(overflow, converter), "Parsing overflow string must throw NumberFormatException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("unsupportedTemporalProvider")
    void toAtomicLong_unsupportedTemporal(Object from, Class<? extends Throwable> exception, String description) {
        assertThrows(exception, () -> LocalTimeConversions.toAtomicLong(from, converter), description);
    }

    @Test
    void toAtomicLong_customConverterReturnsLong() {
        // Arrange: a mock Converter that returns Long.valueOf(999) for any CustomObject.
        Converter customConverter = mock(Converter.class);
        when(customConverter.convert(any(CustomObject.class), eq(Long.class))).thenReturn(999L);
        AtomicLong result = LocalTimeConversions.toAtomicLong(new CustomObject(), customConverter);
        assertEquals(999L, result.get(), "Custom converter returning Long should be wrapped");
    }

    @Test
    void toAtomicLong_customConverterReturnsString() {
        // Arrange: mock returns a numeric String; toLong should parse it.
        Converter customConverter = mock(Converter.class);
        when(customConverter.convert(any(CustomObject.class), eq(Long.class))).thenReturn("777");
        AtomicLong result = LocalTimeConversions.toAtomicLong(new CustomObject(), customConverter);
        assertEquals(777L, result.get(), "Custom converter returning numeric String should be parsed");
    }

    @Test
    void toAtomicLong_customConverterThrowsRuntimeException() {
        // Arrange: mock throws a RuntimeException.
        Converter customConverter = mock(Converter.class);
        when(customConverter.convert(any(CustomObject.class), eq(Long.class))).thenThrow(new RuntimeException("boom"));
        assertThrows(RuntimeException.class, () -> LocalTimeConversions.toAtomicLong(new CustomObject(), customConverter), "Exception from custom converter must propagate");
    }

    @Test
    void toAtomicLong_customConverterReturnsNull() {
        // Arrange: mock returns null – toLong receives null and should NPE.
        Converter customConverter = mock(Converter.class);
        when(customConverter.convert(any(CustomObject.class), eq(Long.class))).thenReturn(null);
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toAtomicLong(new CustomObject(), customConverter), "Null result from converter should cause NullPointerException");
    }

    @Test
    void toAtomicLong_stringWithWhitespace_isTrimmedAndParsed() {
        String source = "   42   ";
        AtomicLong result = LocalTimeConversions.toAtomicLong(source, converter);
        assertEquals(42L, result.get(), "String with leading/trailing whitespace should be trimmed before parsing");
    }

    @Test
    void toAtomicLong_invalidNumericString_throwsNumberFormatException() {
        String source = "abc";
        assertThrows(NumberFormatException.class, () -> LocalTimeConversions.toAtomicLong(source, converter), "Non‑numeric string must cause NumberFormatException");
    }
}
