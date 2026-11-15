package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toAtomicLong_Test {

    /**
     * Shared Converter used by all test methods.
     */
    private static Converter converter;

    /**
     * Shared default Converter used by most test methods.
     */
    private static Converter defaultConverter;

    /**
     * Simple POJO used for custom‑converter tests.
     */
    private static final class MyCustomType {

        final String value;

        MyCustomType(String value) {
            this.value = value;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a Converter with default options.
        // ConverterOptions has a public no‑arg constructor in the library.
        ConverterOptions options = new ConverterOptions();
        defaultConverter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but tidy.
        converter = null;
    }

    /**
     * Creates a {@link Calendar} set to UTC midnight of the given date.
     */
    private static Calendar createUtcCalendar(int year, int monthZeroBased, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(year, monthZeroBased, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    private static Stream<Arguments> numericConversionProvider() {
        return Stream.of(// Long instance
        Arguments.of(123L, 123L, "Direct Long instance should be wrapped without conversion."), // Integer
        Arguments.of(45, 45L, "Integer is converted to long and wrapped."), // String number
        Arguments.of("6789", 6789L, "String containing a valid decimal number is parsed and wrapped."), // BigDecimal (fractional part truncated)
        Arguments.of(new BigDecimal("12345.67"), 12345L, "BigDecimal is converted via longValue() (truncates fractional part) and wrapped."), // BigInteger
        Arguments.of(new BigInteger("9876543210"), 9876543210L, "BigInteger is converted via longValue() and wrapped."));
    }

    private static Stream<Arguments> dateTimeConversionProvider() {
        return Stream.of(// java.util.Date → epoch millis
        Arguments.of(new Date(1609459200000L), 1609459200000L, "java.util.Date is converted to its epoch‑millis representation."), // Instant → epoch seconds
        Arguments.of(Instant.ofEpochSecond(1609459200L), 1609459200L, "Instant is converted to epoch seconds."), // LocalDateTime interpreted as UTC → epoch seconds
        Arguments.of(LocalDateTime.of(2021, 1, 1, 0, 0), 1609459200L, "LocalDateTime is interpreted as UTC and converted to epoch seconds."), // ZonedDateTime UTC → epoch seconds
        Arguments.of(ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), 1609459200L, "ZonedDateTime is converted to epoch seconds."), // OffsetDateTime with +02:00 offset → epoch seconds (UTC = 2020‑12‑31T22:00)
        Arguments.of(OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(2)), 1609452000L, "OffsetDateTime is converted to epoch seconds taking the offset into account."));
    }

    private static Stream<Arguments> boundaryNumericProvider() {
        return Stream.of(Arguments.of(Long.MAX_VALUE, Long.MAX_VALUE, "Maximum long value should be wrapped without alteration."), Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE, "Minimum long value should be wrapped without alteration."), // BigDecimal > Long.MAX_VALUE but truncated to Long.MAX_VALUE
        Arguments.of(new BigDecimal("9223372036854775807.999"), Long.MAX_VALUE, "BigDecimal larger than Long.MAX_VALUE but with fractional part is truncated to Long.MAX_VALUE."), // BigDecimal < Long.MIN_VALUE but truncated to Long.MIN_VALUE
        Arguments.of(new BigDecimal("-9223372036854775808.001"), Long.MIN_VALUE, "BigDecimal smaller than Long.MIN_VALUE but with fractional part is truncated to Long.MIN_VALUE."), // Double value – fractional part dropped
        Arguments.of(12345.678, 12345L, "Double value is treated as Number; fractional part is dropped during conversion."));
    }

    private static Stream<Arguments> stringParsingProvider() {
        return Stream.of(Arguments.of("  42  ", 42L, "String containing leading and trailing whitespace is trimmed before parsing."), Arguments.of("+123", 123L, "String with an explicit plus sign is parsed correctly."));
    }

    @Test
    void toAtomicLong_givenNull_returnsZeroAtomicLong() {
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(null, converter);
        assertNotNull(result, "Result must never be null");
        assertEquals(0L, result.get(), "Null input should be converted to zero");
    }

    @Test
    void toAtomicLong_givenLong_returnsSameValue() {
        Long source = 123L;
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(source, converter);
        assertEquals(123L, result.get(), "Long value should be preserved");
    }

    @Test
    void toAtomicLong_givenInteger_returnsConvertedLong() {
        Integer source = 45;
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(source, converter);
        assertEquals(45L, result.get(), "Integer should be converted to long");
    }

    @Test
    void toAtomicLong_givenStringNumber_returnsParsedLong() {
        String source = "67";
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(source, converter);
        assertEquals(67L, result.get(), "Numeric string should be parsed to long");
    }

    @Test
    void toAtomicLong_givenLocalDateTime_convertsToEpochMilli() {
        // LocalDateTime of 2020‑01‑01T00:00:00 (UTC) → epoch milli = 1577836800000
        LocalDateTime source = LocalDateTime.of(2020, 1, 1, 0, 0);
        // The library converts LocalDateTime to epoch seconds (or millis) via toLong;
        // we assert the expected epoch milli value.
        long expected = source.toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(source, converter);
        assertEquals(expected, result.get(), "LocalDateTime should be converted to epoch milliseconds");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("numericConversionProvider")
    void toAtomicLong_validNumericConversions(Object source, long expected, String description) {
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(source, defaultConverter);
        assertNotNull(result);
        assertEquals(expected, result.get(), description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("dateTimeConversionProvider")
    void toAtomicLong_dateAndTimeConversions(Object source, long expected, String description) {
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(source, defaultConverter);
        assertNotNull(result);
        assertEquals(expected, result.get(), description);
    }

    @Test
    void toAtomicLong_nullInput_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toAtomicLong(null, defaultConverter), "Passing null as the source object should result in a NullPointerException.");
    }

    @Test
    void toAtomicLong_nonNumericString_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> LocalDateTimeConversions.toAtomicLong("notANumber", defaultConverter), "A non‑numeric String cannot be parsed to a long and should cause NumberFormatException.");
    }

    @Test
    void toAtomicLong_unsupportedObject_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toAtomicLong(new Object(), defaultConverter), "An unsupported type should trigger IllegalArgumentException.");
    }

    @Test
    void toAtomicLong_customConverter_usedForUnsupportedType() {
        // Mock a Converter that knows how to handle MyCustomType
        Converter customConverter = mock(Converter.class);
        when(customConverter.convert(any(MyCustomType.class), eq(Long.class))).thenReturn(999L);
        MyCustomType src = new MyCustomType("value");
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(src, customConverter);
        assertEquals(999L, result.get(), "Custom converter should provide the long value.");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("boundaryNumericProvider")
    void toAtomicLong_boundaryNumericValues(Object source, long expected, String description) {
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(source, defaultConverter);
        assertEquals(expected, result.get(), description);
    }

    @Test
    void toAtomicLong_sqlTimestamp_convertsToEpochMilli() {
        Timestamp ts = new Timestamp(1609459200000L);
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(ts, defaultConverter);
        assertEquals(1609459200000L, result.get(), "java.sql.Timestamp is converted to its epoch‑millis representation.");
    }

    @Test
    void toAtomicLong_calendarUtc_convertsToEpochMilli() {
        Calendar cal = createUtcCalendar(2021, Calendar.JANUARY, 1, 0, 0, 0);
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(cal, defaultConverter);
        assertEquals(1609459200000L, result.get(), "Calendar set to UTC midnight 2021‑01‑01 yields the same epoch‑millis as Date.");
    }

    @Test
    void toAtomicLong_zonedDateTimeWithNegativeOffset() {
        ZonedDateTime zdt = ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-5));
        // UTC time = 2021‑01‑01T05:00Z → epoch seconds = 1609477200
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(zdt, defaultConverter);
        assertEquals(1609477200L, result.get(), "ZonedDateTime with a -05:00 offset is converted to epoch seconds accounting for the offset.");
    }

    @Test
    void toAtomicLong_offsetDateTimeWithNegativeOffset() {
        OffsetDateTime odt = OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-3));
        // UTC time = 2021‑01‑01T03:00Z → epoch seconds = 1609470000
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(odt, defaultConverter);
        assertEquals(1609470000L, result.get(), "OffsetDateTime with a -03:00 offset is converted to epoch seconds taking the offset into account.");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("stringParsingProvider")
    void toAtomicLong_stringParsingEdgeCases(String source, long expected, String description) {
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(source, defaultConverter);
        assertEquals(expected, result.get(), description);
    }

    @Test
    void toAtomicLong_hexString_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> LocalDateTimeConversions.toAtomicLong("0x10", defaultConverter), "Hexadecimal notation is not supported; parsing should fail with NumberFormatException.");
    }

    @Test
    void toAtomicLong_customConverterReturnsNull_throwsNullPointerException() {
        Converter nullReturningConverter = mock(Converter.class);
        when(nullReturningConverter.convert(any(), eq(Long.class))).thenReturn(null);
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toAtomicLong(new Object(), nullReturningConverter), "When a custom Converter returns null for an unsupported type, toLong should throw NullPointerException.");
    }

    @Test
    void toAtomicLong_customConverterReturnsString_parsedToLong() {
        Converter stringReturningConverter = mock(Converter.class);
        when(stringReturningConverter.convert(any(MyCustomType.class), eq(Long.class))).thenReturn("555");
        MyCustomType src = new MyCustomType("value");
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(src, stringReturningConverter);
        assertEquals(555L, result.get(), "Custom Converter returns a numeric String; the method should parse it and wrap the resulting long.");
    }

    @Test
    void toAtomicLong_unsupportedCollection_withoutConverter_throwsIllegalArgumentException() {
        List<Integer> list = new ArrayList<>();
        assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toAtomicLong(list, defaultConverter), "A collection type that has no direct conversion should trigger IllegalArgumentException when no Converter is supplied.");
    }

    @Test
    void toAtomicLong_converterThrowsRuntimeException_propagates() {
        Converter throwingConverter = mock(Converter.class);
        when(throwingConverter.convert(any(), eq(Long.class))).thenThrow(new RuntimeException("boom"));
        assertThrows(RuntimeException.class, () -> LocalDateTimeConversions.toAtomicLong(new Object(), throwingConverter), "The supplied Converter throws a RuntimeException during conversion; toAtomicLong should propagate the exception.");
    }

    @Test
    void toAtomicLong_converterReturnsUnsupportedType_throwsIllegalArgumentException() {
        Converter converterReturningUnsupported = mock(Converter.class);
        // Return an object that toLong cannot handle (e.g., a plain Object)
        when(converterReturningUnsupported.convert(any(), eq(Long.class))).thenReturn(new Object());
        assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toAtomicLong(new Object(), converterReturningUnsupported), "Converter returns an object of a type that toLong cannot handle; method should raise IllegalArgumentException.");
    }

    @Test
    void toAtomicLong_nullSourceWithCustomConverter_throwsNullPointerException() {
        Converter anyConverter = mock(Converter.class);
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toAtomicLong(null, anyConverter), "Null source should always result in NullPointerException, even with a custom converter.");
    }
}
