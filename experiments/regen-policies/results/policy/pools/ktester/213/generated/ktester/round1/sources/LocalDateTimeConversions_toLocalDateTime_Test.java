package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

class LocalDateTimeConversions_toLocalDateTime_Test {

    private Converter converter;

    /**
     * Simple placeholder class to demonstrate custom conversion.
     */
    private static final class MyCustomTemporal {
        // no fields – just a marker type
    }

    /**
     * Minimal dummy Converter that does nothing; used for null‑source test.
     */
    private static final class DummyConverter extends Converter {

        DummyConverter() {
            super(new ConverterOptions());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Converter needs a ConverterOptions instance; the default constructor gives sane defaults.
        converter = new Converter(new ConverterOptions());
    }

    @Test
    void shouldReturnSameLocalDateTimeWhenSourceIsLocalDateTime() {
        // given
        LocalDateTime source = LocalDateTime.of(2023, Month.AUGUST, 15, 12, 34, 56, 123_000_000);
        // when
        LocalDateTime result = LocalDateTimeConversions.toLocalDateTime(source, converter);
        // then
        assertEquals(source, result);
    }

    @Test
    void shouldConvertFromZonedDateTime() {
        // given
        ZonedDateTime source = ZonedDateTime.of(2023, 8, 15, 12, 34, 56, 0, ZoneId.of("America/New_York"));
        // when
        LocalDateTime result = LocalDateTimeConversions.toLocalDateTime(source, converter);
        // then – the LocalDateTime part of the ZonedDateTime is retained
        assertEquals(source.toLocalDateTime(), result);
    }

    @Test
    void shouldConvertFromSqlTimestamp() {
        // given
        Timestamp ts = Timestamp.valueOf("2023-08-15 12:34:56.123456789");
        // when
        LocalDateTime result = LocalDateTimeConversions.toLocalDateTime(ts, converter);
        // then
        assertEquals(ts.toLocalDateTime(), result);
    }

    @Test
    void shouldConvertFromString() {
        // given
        String source = "2023-08-15T12:34:56.123";
        // when
        LocalDateTime result = LocalDateTimeConversions.toLocalDateTime(source, converter);
        // then
        LocalDateTime expected = LocalDateTime.parse(source, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        assertEquals(expected, result);
    }

    @Test
    void shouldConvertFromMapContainingTimeAndNanos() {
        // given – map mimicking the structure the Converter expects for date‑time types
        Map<String, Object> source = new LinkedHashMap<>();
        // epoch milli for 2023‑08‑15T12:34:56.123Z
        long epochMilli = Instant.parse("2023-08-15T12:34:56.123Z").toEpochMilli();
        source.put("time", epochMilli);
        // additional nanos beyond the millisecond part
        source.put("nanos", 123_000_000);
        // when
        LocalDateTime result = LocalDateTimeConversions.toLocalDateTime(source, converter);
        // then – the map is interpreted as an instant in UTC, then converted to a LocalDateTime
        Instant instant = Instant.ofEpochMilli(epochMilli).plusNanos(123_000_000);
        LocalDateTime expected = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        assertEquals(expected, result);
    }

    // -----------------------------------------------------------------
    @Test
    void shouldReturnNullWhenSourceIsNullAndConverterIsNull() {
        assertNull(LocalDateTimeConversions.toLocalDateTime(null, null));
    }

    @Test
    void shouldReturnSameLocalDateTimeWhenConverterIsNull() {
        LocalDateTime source = LocalDateTime.of(2023, 5, 10, 15, 30, 45);
        assertEquals(source, LocalDateTimeConversions.toLocalDateTime(source, null));
    }

    // -----------------------------------------------------------------
    @Test
    void shouldConvertZonedDateTimeUtcToLocalDateTime() {
        ZonedDateTime source = ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneOffset.UTC);
        LocalDateTime expected = LocalDateTime.of(2023, 5, 10, 15, 30, 45);
        assertEquals(expected, LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    @Test
    void shouldConvertOffsetDateTimeToLocalDateTime() {
        OffsetDateTime source = OffsetDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneOffset.ofHours(2));
        LocalDateTime expected = LocalDateTime.of(2023, 5, 10, 15, 30, 45);
        assertEquals(expected, LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    // -----------------------------------------------------------------
    @Test
    void shouldConvertUtilDateToLocalDateTime() {
        Date source = Date.from(Instant.parse("2023-05-10T15:30:45Z"));
        LocalDateTime expected = LocalDateTime.ofInstant(source.toInstant(), ZoneId.systemDefault());
        assertEquals(expected, LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    @Test
    void shouldConvertCalendarToLocalDateTime() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTime(Date.from(Instant.parse("2023-05-10T15:30:45Z")));
        LocalDateTime expected = LocalDateTime.ofInstant(calendar.toInstant(), ZoneId.systemDefault());
        assertEquals(expected, LocalDateTimeConversions.toLocalDateTime(calendar, converter));
    }

    // -----------------------------------------------------------------
    @Test
    void shouldConvertLongEpochMilliToLocalDateTime() {
        // 2023-05-10T15:30:45Z
        long epochMilli = 1683731445000L;
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault());
        assertEquals(expected, LocalDateTimeConversions.toLocalDateTime(epochMilli, converter));
    }

    @Test
    void shouldConvertAtomicLongEpochMilliToLocalDateTime() {
        AtomicLong source = new AtomicLong(1683731445000L);
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(source.get()), ZoneId.systemDefault());
        assertEquals(expected, LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    // -----------------------------------------------------------------
    @Test
    void shouldParseIsoStringToLocalDateTime() {
        String source = "2023-05-10T15:30:45";
        LocalDateTime expected = LocalDateTime.of(2023, 5, 10, 15, 30, 45);
        assertEquals(expected, LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    @Test
    void shouldParseSpaceSeparatedStringToLocalDateTime() {
        String source = "2023-05-10 15:30:45";
        LocalDateTime expected = LocalDateTime.of(2023, 5, 10, 15, 30, 45);
        assertEquals(expected, LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    // -----------------------------------------------------------------
    @Test
    void shouldConvertLocalDateToMidnightLocalDateTime() {
        LocalDate source = LocalDate.of(2023, 5, 10);
        LocalDateTime expected = LocalDateTime.of(2023, 5, 10, 0, 0);
        assertEquals(expected, LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    @Test
    void shouldConvertYearMonthToFirstDayMidnight() {
        YearMonth source = YearMonth.of(2023, 5);
        LocalDateTime expected = LocalDateTime.of(2023, 5, 1, 0, 0);
        assertEquals(expected, LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    @Test
    void shouldConvertYearToJanFirstMidnight() {
        Year source = Year.of(2023);
        LocalDateTime expected = LocalDateTime.of(2023, 1, 1, 0, 0);
        assertEquals(expected, LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    @Test
    void shouldConvertMonthDayToCurrentYearMidnight() {
        MonthDay source = MonthDay.of(5, 10);
        int currentYear = Year.now().getValue();
        LocalDateTime expected = LocalDateTime.of(currentYear, 5, 10, 0, 0);
        assertEquals(expected, LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    // -----------------------------------------------------------------
    @Test
    void shouldThrowConversionExceptionForUnsupportedObject() {
        Object source = new Object();
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    // -----------------------------------------------------------------
    @Test
    void shouldConvertSqlTimestampToLocalDateTime() {
        Timestamp source = new Timestamp(1683731445000L);
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(source.getTime()), ZoneId.systemDefault());
        assertEquals(expected, LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    @Test
    void shouldConvertSqlDateToMidnightLocalDateTime() {
        java.sql.Date source = new java.sql.Date(1683731445000L);
        // java.sql.Date normalises the time to midnight in the default time‑zone
        LocalDateTime expected = LocalDateTime.ofInstant(source.toInstant(), ZoneId.systemDefault()).withHour(0).withMinute(0).withSecond(0).withNano(0);
        assertEquals(expected, LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    // -----------------------------------------------------------------
    @Test
    void shouldConvertInstantEpochToLocalDateTime() {
        Instant source = Instant.parse("1970-01-01T00:00:00Z");
        LocalDateTime expected = LocalDateTime.ofInstant(source, ZoneOffset.UTC);
        assertEquals(expected, LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    @Test
    void shouldConvertInstantPreEpochToLocalDateTime() {
        Instant source = Instant.parse("1969-12-31T23:59:59Z");
        LocalDateTime expected = LocalDateTime.ofInstant(source, ZoneOffset.UTC);
        assertEquals(expected, LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    @Test
    void shouldConvertIntegerEpochMilliToLocalDateTime() {
        // milliseconds since epoch
        Integer source = 1234567890;
        Instant instant = Instant.ofEpochMilli(source.longValue());
        LocalDateTime expected = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        assertEquals(expected, LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    // -----------------------------------------------------------------
    @Test
    void shouldParseIsoStringWithOffsetAndDiscardOffset() {
        String source = "2023-05-10T15:30:45+02:00";
        LocalDateTime expected = LocalDateTime.of(2023, 5, 10, 15, 30, 45);
        assertEquals(expected, LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    @Test
    void shouldParseDateOnlyStringAsMidnight() {
        String source = "2023-05-10";
        LocalDateTime expected = LocalDateTime.of(2023, 5, 10, 0, 0);
        assertEquals(expected, LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    // -----------------------------------------------------------------
    @Test
    void shouldThrowConversionExceptionForDouble() {
        Double source = 3.14159;
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    @Test
    void shouldThrowConversionExceptionForLongMaxValue() {
        Long source = Long.MAX_VALUE;
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    // -----------------------------------------------------------------
    @Test
    void shouldThrowConversionExceptionForLocalTime() {
        LocalTime source = LocalTime.of(15, 30, 45);
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    // -----------------------------------------------------------------
    @Test
    void shouldUseCustomConverterToConvertNonStandardObject() {
        // Arrange: a dummy object that the custom converter knows how to handle
        Object custom = new MyCustomTemporal();
        // Mock a Converter that returns a specific ZonedDateTime for the dummy object
        Converter mockConverter = mock(Converter.class);
        ZonedDateTime zoned = ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        when(mockConverter.convert(eq(custom), eq(ZonedDateTime.class))).thenReturn(zoned);
        // Act
        LocalDateTime result = LocalDateTimeConversions.toLocalDateTime(custom, mockConverter);
        // Assert
        LocalDateTime expected = zoned.toLocalDateTime();
        assertEquals(expected, result);
        verify(mockConverter).convert(eq(custom), eq(ZonedDateTime.class));
    }

    // -----------------------------------------------------------------
    @Test
    void shouldThrowConversionExceptionForInvalidMonthString() {
        String source = "2023-13-10T15:30:45";
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    @Test
    void shouldThrowConversionExceptionForNonexistentDateString() {
        String source = "2023-02-30";
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    @Test
    void shouldThrowConversionExceptionForCompletelyInvalidString() {
        String source = "not-a-date-time";
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    // -----------------------------------------------------------------
    @Test
    void shouldThrowConversionExceptionForFloat() {
        Float source = 3.14f;
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    @Test
    void shouldThrowConversionExceptionForShort() {
        Short source = (short) 12345;
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    @Test
    void shouldThrowConversionExceptionForBigDecimal() {
        BigDecimal source = new BigDecimal("1683731445000");
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    @Test
    void shouldThrowConversionExceptionForBigInteger() {
        BigInteger source = new BigInteger("1683731445000");
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    // -----------------------------------------------------------------
    @Test
    void shouldThrowConversionExceptionForDuration() {
        Duration source = Duration.ofHours(5);
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    @Test
    void shouldThrowConversionExceptionForPeriod() {
        Period source = Period.ofDays(10);
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toLocalDateTime(source, converter));
    }

    // -----------------------------------------------------------------
    @Test
    void shouldReturnNullWhenSourceIsNullButConverterIsProvided() {
        Converter dummy = new DummyConverter();
        assertNull(LocalDateTimeConversions.toLocalDateTime(null, dummy));
    }
}
