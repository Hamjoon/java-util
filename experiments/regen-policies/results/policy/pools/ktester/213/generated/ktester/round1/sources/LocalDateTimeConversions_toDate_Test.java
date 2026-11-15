package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toDate_Test {

    private Converter converter;

    /* --------------------------------------------------------------
     *  Helper dummy class used in custom‑converter test
     * -------------------------------------------------------------- */
    private static class DummyObject {
        // No content – just a placeholder type.
    }

    @BeforeEach
    void setupBeforeEach() {
        // The Converter can be used as‑is – it contains all built‑in conversions.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> numericConversionProvider() {
        return Stream.of(Arguments.of(1622548800000L, 1622548800000L, "Long value should be interpreted directly as epoch milliseconds."), Arguments.of(12345, 12345L, "Integer value should be widened to long and used as epoch milliseconds."), Arguments.of("1622548800000", 1622548800000L, "String containing a numeric epoch millis should be parsed to long."));
    }

    private static Stream<Arguments> dateTimeObjectProvider() {
        return Stream.of(Arguments.of(new Date(1622548800000L), 1622548800000L, "java.util.Date should be converted by extracting its time value."), Arguments.of(Timestamp.valueOf("2021-06-01 12:00:00.123"), 1622548800123L, "java.sql.Timestamp should be converted using its nanosecond‑adjusted epoch."), Arguments.of(Instant.ofEpochMilli(1622548800000L), 1622548800000L, "Instant should be converted by taking its epoch milliseconds."), Arguments.of(LocalDateTime.of(2021, 6, 1, 12, 0, 0), 1622548800000L, "LocalDateTime should be converted using the system default ZoneOffset (UTC in CI)."), Arguments.of(createCalendarWithMillis(1622548800000L), 1622548800000L, "Calendar should be converted by extracting its time in milliseconds."));
    }

    private static Calendar createCalendarWithMillis(long epochMillis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMillis);
        return cal;
    }

    private static Stream<Arguments> boundaryEpochProvider() {
        return Stream.of(Arguments.of(0L, 0L, "Zero epoch milliseconds should produce the Unix epoch start."), Arguments.of(-86_400_000L, -86_400_000L, "Negative epoch milliseconds should be handled correctly (one day before epoch)."), Arguments.of(Long.MAX_VALUE, Long.MAX_VALUE, "Maximum long value should be accepted and wrapped in a Date without overflow."));
    }

    private static Stream<Arguments> bigNumberProvider() {
        return Stream.of(Arguments.of(new BigDecimal("1622548800123"), 1622548800123L, "BigDecimal containing epoch millis should be converted by extracting its long value."), Arguments.of(new BigInteger("1622548800123"), 1622548800123L, "BigInteger should be treated like a numeric long source."), Arguments.of(new AtomicLong(1622548800123L), 1622548800123L, "AtomicLong should expose its value for conversion."), Arguments.of(1622548800123.0d, 1622548800123L, "Double value is truncated to long before creating the Date."), Arguments.of(1622548800.123f, 1622548800L, "Float is cast to long (fractional part discarded)."));
    }

    private static Stream<Arguments> temporalProvider() {
        return Stream.of(Arguments.of(java.sql.Date.valueOf("2021-06-01"), LocalDate.of(2021, 6, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), "java.sql.Date should be converted by using its underlying epoch milliseconds."), Arguments.of(LocalDate.of(2021, 6, 1), LocalDate.of(2021, 6, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), "LocalDate is interpreted as midnight of that date in the default zone (UTC)."), Arguments.of(LocalTime.of(12, 0, 0), LocalDate.ofEpochDay(0).atTime(12, 0, 0).atZone(ZoneOffset.UTC).toInstant().toEpochMilli(), "LocalTime is combined with epoch day (1970‑01‑01) to produce a Date."), Arguments.of(MonthDay.of(6, 1), LocalDate.of(1970, 6, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), "MonthDay is mapped to the same month/day in the epoch year (1970)."), Arguments.of(YearMonth.of(2021, 6), LocalDate.of(2021, 6, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), "YearMonth is interpreted as the first day of the month at midnight."), Arguments.of(Year.of(2021), LocalDate.of(2021, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), "Year is interpreted as the first day of the year at midnight."), Arguments.of(ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC), 1622548800000L, "ZonedDateTime is converted by extracting its instant."), Arguments.of(OffsetDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC), 1622548800000L, "OffsetDateTime conversion follows the same rule as ZonedDateTime."));
    }

    private static Stream<Arguments> stringDateProvider() {
        return Stream.of(Arguments.of("2021-06-01T12:00:00Z", 1622548800000L, "ISO‑8601 UTC string should be parsed to an instant."), Arguments.of("2021/06/01 12:00:00", 1622548800000L, "A non‑ISO but commonly used pattern should be accepted if the implementation falls back to DateTimeFormatter.ofPattern."), Arguments.of("not-a-date", null, "Malformed date string should cause a conversion failure."));
    }

    private static Stream<Arguments> numericParsingErrorProvider() {
        return Stream.of(Arguments.of("", NumberFormatException.class), Arguments.of("   ", NumberFormatException.class), // > Long.MAX_VALUE
        Arguments.of("9223372036854775808", NumberFormatException.class));
    }

    private static Stream<Arguments> specialFloatingProvider() {
        return Stream.of(Arguments.of(Double.NaN, IllegalArgumentException.class), Arguments.of(Double.POSITIVE_INFINITY, IllegalArgumentException.class), Arguments.of(Float.NEGATIVE_INFINITY, IllegalArgumentException.class));
    }

    private static Stream<Arguments> unsupportedTypeProvider() {
        return Stream.of(Arguments.of(new byte[] { 1, 2, 3 }, ConversionException.class), Arguments.of(new java.util.concurrent.atomic.AtomicInteger(123), ConversionException.class), Arguments.of(new Character('A'), ConversionException.class));
    }

    @Test
    void toDate_fromLong_returnsDateWithSameEpoch() {
        // 2022‑01‑01T00:00:00Z
        long epochMillis = 1_640_995_200_000L;
        Date result = LocalDateTimeConversions.toDate(epochMillis, converter);
        assertNotNull(result);
        assertEquals(epochMillis, result.getTime(), "Date created from a long should have the same epoch milliseconds");
    }

    @Test
    void toDate_fromDate_returnsIdenticalDate() {
        Date source = new Date(1_640_995_200_000L);
        Date result = LocalDateTimeConversions.toDate(source, converter);
        assertNotNull(result);
        assertEquals(source.getTime(), result.getTime(), "Converting a Date should preserve the epoch milliseconds");
    }

    @Test
    void toDate_fromCalendar_returnsDateWithSameEpoch() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(2022, Calendar.JANUARY, 1, 0, 0, 0);
        long expected = cal.getTimeInMillis();
        Date result = LocalDateTimeConversions.toDate(cal, converter);
        assertNotNull(result);
        assertEquals(expected, result.getTime(), "Converting a Calendar should produce a Date with the same epoch milliseconds");
    }

    @Test
    void toDate_fromNull_returnsEpochZeroDate() {
        Date result = LocalDateTimeConversions.toDate(null, converter);
        assertNotNull(result);
        assertEquals(0L, result.getTime(), "Null source should be converted to a Date representing epoch 0");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("numericConversionProvider")
    void toDate_numericConversions(Object input, long expectedEpoch, String description) {
        Date result = LocalDateTimeConversions.toDate(input, converter);
        assertNotNull(result);
        assertEquals(expectedEpoch, result.getTime(), description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("dateTimeObjectProvider")
    void toDate_dateTimeObjects(Object input, long expectedEpoch, String description) {
        Date result = LocalDateTimeConversions.toDate(input, converter);
        assertNotNull(result);
        assertEquals(expectedEpoch, result.getTime(), description);
    }

    @Test
    void toDate_customConverterFallback() {
        // Mock a Converter that returns a specific long when asked to convert the DummyObject.
        Converter custom = mock(Converter.class);
        // 2022‑01‑01T00:00:00Z
        when(custom.convert(any(), eq(Long.class))).thenReturn(1640995200000L);
        Object dummy = new DummyObject();
        Date result = LocalDateTimeConversions.toDate(dummy, custom);
        assertNotNull(result);
        assertEquals(1640995200000L, result.getTime(), "Custom converter should supply the epoch millis which is then wrapped in a Date.");
    }

    @Test
    void toDate_nullSourceThrowsNPE() {
        // According to the specification, a null source should cause NPE because toLong cannot derive a value.
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toDate(null, null), "Passing a null source with a null converter should result in NullPointerException.");
    }

    @Test
    void toDate_unsupportedTypeWithoutConverter_throwsConversionException() {
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toDate(Boolean.TRUE, converter), "Unsupported type without a custom Converter should cause a conversion failure.");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("boundaryEpochProvider")
    void toDate_boundaryEpochValues(Object input, long expectedEpoch, String description) {
        Date result = LocalDateTimeConversions.toDate(input, converter);
        assertNotNull(result);
        assertEquals(expectedEpoch, result.getTime(), description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("bigNumberProvider")
    void toDate_bigNumberConversions(Object input, long expectedEpoch, String description) {
        Date result = LocalDateTimeConversions.toDate(input, converter);
        assertNotNull(result);
        assertEquals(expectedEpoch, result.getTime(), description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("temporalProvider")
    void toDate_temporalObjects(Object input, long expectedEpoch, String description) {
        Date result = LocalDateTimeConversions.toDate(input, converter);
        assertNotNull(result);
        assertEquals(expectedEpoch, result.getTime(), description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("stringDateProvider")
    void toDate_stringParsing(Object input, long expectedEpoch, String description) {
        Date result = LocalDateTimeConversions.toDate(input, converter);
        assertNotNull(result);
        assertEquals(expectedEpoch, result.getTime(), description);
    }

    @Test
    void toDate_malformedString_throwsConversionException() {
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toDate("not-a-date", converter), "Malformed date string should cause a conversion failure.");
    }

    @Test
    void toDate_customConverterReturnsNull_throwsNPE() {
        Converter bad = mock(Converter.class);
        when(bad.convert(any(), eq(Long.class))).thenReturn(null);
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toDate("ignored", bad), "If the supplied Converter returns null, the subsequent Date constructor receives null and throws NPE.");
    }

    @Test
    void toDate_customConverterThrowsConversionException_propagates() {
        Converter bad = mock(Converter.class);
        when(bad.convert(any(), eq(Long.class))).thenThrow(new ConversionException("boom"));
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toDate("ignored", bad), "When the custom Converter throws a ConversionException, it should propagate out of toDate.");
    }

    @Test
    void toDate_arbitraryObjectWithoutConverter_throwsConversionException() {
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toDate(new Object(), converter), "An arbitrary object without a custom Converter should result in a conversion failure.");
    }

    @ParameterizedTest(name = "{index}: \"{0}\"")
    @MethodSource("numericParsingErrorProvider")
    void toDate_numericParsingErrors(String input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> LocalDateTimeConversions.toDate(input, converter), "Parsing error for input: \"" + input + "\"");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("specialFloatingProvider")
    void toDate_specialFloatingValues(Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> LocalDateTimeConversions.toDate(input, converter), "Special floating value " + input + " should be rejected.");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("unsupportedTypeProvider")
    void toDate_unsupportedTypes(Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> LocalDateTimeConversions.toDate(input, converter), "Unsupported type " + input.getClass().getSimpleName() + " should cause conversion failure.");
    }

    @Test
    void toDate_customConverterThrowsRuntimeException_propagates() {
        Converter bad = mock(Converter.class);
        when(bad.convert(any(), eq(Long.class))).thenThrow(new IllegalStateException("bad state"));
        assertThrows(IllegalStateException.class, () -> LocalDateTimeConversions.toDate("ignored", bad), "RuntimeException from custom Converter should propagate unchanged.");
    }

    @Test
    void toDate_customConverterThrowsNullPointerException_propagates() {
        Converter bad = mock(Converter.class);
        when(bad.convert(any(), eq(Long.class))).thenThrow(new NullPointerException("null!"));
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toDate("ignored", bad), "NullPointerException from custom Converter should propagate.");
    }

    @Test
    void toDate_customConverterReturnsUnsupportedType_throwsConversionException() {
        Converter bad = mock(Converter.class);
        when(bad.convert(any(), eq(Long.class))).thenReturn("I am a String, not a Long");
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toDate("ignored", bad), "When a custom Converter returns an unsupported type, toLong will fail and raise ConversionException.");
    }
}
