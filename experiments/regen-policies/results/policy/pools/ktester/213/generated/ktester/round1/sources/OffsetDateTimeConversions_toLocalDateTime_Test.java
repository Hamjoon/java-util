package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toLocalDateTime_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * Create a {@link Converter} once for the whole test suite.
     */
    @BeforeAll
    static void setupBeforeAll() {
        // Default options provide the full set of built‑in conversions.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC when many test classes run.
        converter = null;
    }

    @Test
    void shouldConvertOffsetDateTimeToLocalDateTime() {
        OffsetDateTime odt = OffsetDateTime.of(2023, 5, 17, 14, 30, 45, 123_000_000, ZoneOffset.ofHoursMinutes(2, 0));
        LocalDateTime expected = odt.toLocalDateTime();
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(odt, converter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertZonedDateTimeToLocalDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2021, 12, 31, 23, 59, 59, 0, ZoneId.of("America/New_York"));
        LocalDateTime expected = zdt.toLocalDateTime();
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(zdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertStringIso8601ToLocalDateTime() {
        // ISO‑8601 string that includes an offset – the converter can parse it.
        String iso = "2022-08-15T10:20:30+03:00";
        // The expected LocalDateTime is the same instant without the offset.
        OffsetDateTime odt = OffsetDateTime.parse(iso);
        LocalDateTime expected = odt.toLocalDateTime();
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(iso, converter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertMapRepresentationToLocalDateTime() {
        // The Converter recognises a map with keys "time" (epoch‑millis) and "nanos".
        // 2022‑01‑01T00:00:00Z
        long epochMilli = 1_640_995_200_000L;
        // plus 0.5 seconds
        int nanos = 500_000_000;
        // Build the map that mimics the internal representation used by the library.
        var map = new java.util.LinkedHashMap<String, Object>();
        map.put("time", epochMilli);
        map.put("nanos", nanos);
        // optional – the converter will honour it if present
        map.put("offset", "+02:00");
        // Expected LocalDateTime = instant + offset, then stripped of zone.
        Instant instant = Instant.ofEpochMilli(epochMilli).plusNanos(nanos);
        OffsetDateTime odt = instant.atOffset(ZoneOffset.of("+02:00"));
        LocalDateTime expected = odt.toLocalDateTime();
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(map, converter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        assertNull(OffsetDateTimeConversions.toLocalDateTime(null, converter));
    }

    @Test
    void shouldThrowExceptionForUnsupportedType() {
        // no conversion path to OffsetDateTime/ZonedDateTime
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toLocalDateTime(unsupported, converter));
    }

    @Test
    void shouldConvertJavaUtilDateToLocalDateTime() {
        Date date = Date.from(Instant.parse("2021-07-20T10:20:30Z"));
        LocalDateTime expected = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(date, converter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertSqlTimestampToLocalDateTime() {
        Timestamp ts = Timestamp.valueOf("2020-01-01 12:00:00");
        LocalDateTime expected = LocalDateTime.of(2020, 1, 1, 12, 0, 0);
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(ts, converter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertEpochMillisLongToLocalDateTime() {
        long epochMillis = Instant.parse("2019-03-15T08:30:00Z").toEpochMilli();
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(Long.valueOf(epochMillis), converter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertIsoStringWithoutOffsetToLocalDateTime() {
        String iso = "2023-04-01T12:30:45";
        LocalDateTime expected = LocalDateTime.of(2023, 4, 1, 12, 30, 45);
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(iso, converter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertIsoDateOnlyStringToLocalDateTime() {
        String iso = "2021-09-30";
        LocalDateTime expected = LocalDateTime.of(2021, 9, 30, 0, 0);
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(iso, converter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertInstantToLocalDateTime() {
        Instant instant = Instant.parse("2024-02-29T12:34:56Z");
        LocalDateTime expected = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(instant, converter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertCalendarToLocalDateTime() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(Instant.parse("2022-11-05T08:15:30Z").toEpochMilli());
        LocalDateTime expected = LocalDateTime.ofInstant(cal.toInstant(), ZoneId.systemDefault());
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(cal, converter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldReturnSameLocalDateTimeWhenSourceIsLocalDateTime() {
        LocalDateTime src = LocalDateTime.of(2021, 12, 31, 23, 59, 59);
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(src, converter);
        assertEquals(src, actual);
    }

    @Test
    void shouldConvertLocalDateToLocalDateTimeAtMidnight() {
        LocalDate date = LocalDate.of(2020, 2, 29);
        LocalDateTime expected = LocalDateTime.of(2020, 2, 29, 0, 0);
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(date, converter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertYearMonthToFirstDayMidnight() {
        YearMonth ym = YearMonth.of(2019, 7);
        LocalDateTime expected = LocalDateTime.of(2019, 7, 1, 0, 0);
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(ym, converter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertYearToFirstDayMidnight() {
        Year year = Year.of(1995);
        LocalDateTime expected = LocalDateTime.of(1995, 1, 1, 0, 0);
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(year, converter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertSqlDateToLocalDateTimeAtMidnight() {
        java.sql.Date sqlDate = java.sql.Date.valueOf("2005-06-15");
        LocalDateTime expected = LocalDateTime.of(2005, 6, 15, 0, 0);
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(sqlDate, converter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertZeroEpochMillisToLocalDateTime() {
        Long zero = Long.valueOf(0L);
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochSecond(0), ZoneId.systemDefault());
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(zero, converter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertNegativeEpochMillisToLocalDateTime() {
        // 1969‑12‑31T00:00:00Z
        Long negative = Long.valueOf(-86_400_000L);
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochSecond(-86_400), ZoneId.systemDefault());
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(negative, converter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForUnsupportedObject() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toLocalDateTime(unsupported, converter));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForBoolean() {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toLocalDateTime(Boolean.TRUE, converter));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForInteger() {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toLocalDateTime(Integer.valueOf(123456789), converter));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForByteArray() {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toLocalDateTime(new byte[] { 0x01, 0x02 }, converter));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForLocalTime() {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toLocalDateTime(LocalTime.of(15, 30, 45), converter));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForBigDecimal() {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toLocalDateTime(new BigDecimal("12345.67"), converter));
    }

    @Test
    void shouldPropagateDateTimeParseExceptionForUnparsableString() {
        String bad = "not-a-date-time";
        assertThrows(DateTimeParseException.class, () -> OffsetDateTimeConversions.toLocalDateTime(bad, converter));
    }

    @Test
    void shouldPropagateDateTimeParseExceptionForEmptyString() {
        String empty = "";
        assertThrows(DateTimeParseException.class, () -> OffsetDateTimeConversions.toLocalDateTime(empty, converter));
    }

    @Test
    void shouldPropagateDateTimeParseExceptionForImpossibleDate() {
        String impossible = "2021-02-30T12:00:00";
        assertThrows(DateTimeParseException.class, () -> OffsetDateTimeConversions.toLocalDateTime(impossible, converter));
    }

    @Test
    void shouldPropagateExceptionFromCustomConverter() {
        // Create a mock Converter that throws an IllegalStateException when asked to convert to ZonedDateTime
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(any(), eq(ZonedDateTime.class))).thenThrow(new IllegalStateException("converter failure"));
        String input = "2022-01-01T00:00:00";
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> OffsetDateTimeConversions.toLocalDateTime(input, mockConverter));
        assertEquals("converter failure", ex.getMessage());
    }
}
