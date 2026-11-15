package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CalendarConversions_toLocalDateTime_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter needs a non‑null options instance; default options are sufficient for these tests
        converter = new Converter(new ConverterOptions());
    }

    @Test
    void shouldReturnSameLocalDateTimeWhenSourceIsLocalDateTime() {
        // truncate nanos for deterministic comparison
        LocalDateTime now = LocalDateTime.now().withNano(0);
        LocalDateTime result = CalendarConversions.toLocalDateTime(now, converter);
        assertEquals(now, result);
    }

    @Test
    void shouldConvertZonedDateTimeToLocalDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 17, 12, 34, 56, 0, ZoneOffset.UTC);
        LocalDateTime expected = zdt.toLocalDateTime();
        LocalDateTime result = CalendarConversions.toLocalDateTime(zdt, converter);
        assertEquals(expected, result);
    }

    @Test
    void shouldConvertInstantToLocalDateTimeUsingUTC() {
        Instant instant = Instant.parse("2023-05-17T12:34:56Z");
        LocalDateTime expected = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        LocalDateTime result = CalendarConversions.toLocalDateTime(instant, converter);
        assertEquals(expected, result);
    }

    @Test
    void shouldConvertDateToLocalDateTimeUsingUTC() {
        // 2023-04-01T00:00:00Z
        long epochMilli = 1_680_000_000_000L;
        Date date = new Date(epochMilli);
        LocalDateTime expected = LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
        LocalDateTime result = CalendarConversions.toLocalDateTime(date, converter);
        assertEquals(expected, result);
    }

    @Test
    void shouldConvertCalendarToLocalDateTimeUsingUTC() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // 2023-04-01T00:00:00Z
        cal.setTimeInMillis(1_680_000_000_000L);
        LocalDateTime expected = LocalDateTime.ofInstant(cal.toInstant(), ZoneOffset.UTC);
        LocalDateTime result = CalendarConversions.toLocalDateTime(cal, converter);
        assertEquals(expected, result);
    }

    @Test
    void shouldConvertTimestampToLocalDateTimeUsingUTC() {
        long epochMilli = 1_680_000_000_000L;
        Timestamp ts = new Timestamp(epochMilli);
        LocalDateTime expected = LocalDateTime.ofInstant(ts.toInstant(), ZoneOffset.UTC);
        LocalDateTime result = CalendarConversions.toLocalDateTime(ts, converter);
        assertEquals(expected, result);
    }

    @Test
    void shouldConvertLongEpochMilliToLocalDateTimeUsingUTC() {
        long epochMilli = 1_680_000_000_000L;
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneOffset.UTC);
        LocalDateTime result = CalendarConversions.toLocalDateTime(epochMilli, converter);
        assertEquals(expected, result);
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        // The original implementation delegates to toZonedDateTime, which returns null for a null source.
        assertNull(CalendarConversions.toLocalDateTime(null, converter));
    }

    @Test
    @DisplayName("toLocalDateTime throws NullPointerException when source is null")
    void test_toLocalDateTime_nullFrom() {
        // Using a real (non‑null) Converter – the method should fail before touching it.
        Converter dummy = new Converter(new ConverterOptions());
        assertThrows(NullPointerException.class, () -> CalendarConversions.toLocalDateTime(null, dummy), "Expected NullPointerException when 'from' argument is null");
    }

    @Test
    @DisplayName("toLocalDateTime throws NullPointerException when converter is null")
    void test_toLocalDateTime_nullConverter() {
        LocalDateTime source = LocalDateTime.now();
        assertThrows(NullPointerException.class, () -> CalendarConversions.toLocalDateTime(source, null), "Expected NullPointerException when 'converter' argument is null");
    }

    @Test
    @DisplayName("toLocalDateTime throws IllegalArgumentException for unsupported type")
    void test_toLocalDateTime_unsupportedType() {
        // Boolean is not a supported conversion source for dates/times.
        Boolean unsupported = Boolean.TRUE;
        Converter dummy = new Converter(new ConverterOptions());
        assertThrows(IllegalArgumentException.class, () -> CalendarConversions.toLocalDateTime(unsupported, dummy), "Expected IllegalArgumentException for unsupported source type");
    }
}
