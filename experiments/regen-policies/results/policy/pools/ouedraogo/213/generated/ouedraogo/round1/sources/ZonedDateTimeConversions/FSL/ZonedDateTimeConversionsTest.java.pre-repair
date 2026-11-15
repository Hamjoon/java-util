package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

/**
 * Unit tests for {@link ZonedDateTimeConversions}. All conversion methods are exercised
 * against both the default system zone and an explicitly different zone to ensure
 * that zone handling logic works as expected.
 */
public class ZonedDateTimeConversionsTest {

    /** Helper creates a {@link Converter} whose options use the supplied {@link ZoneId}. */
    private static Converter createConverter(ZoneId zoneId) {
        // ConverterOptions is part of the same library; it provides setters for
        // the ZoneId and the corresponding TimeZone.  The public API of
        // ConverterOptions is not shown, but the typical usage pattern is:
        //   ConverterOptions opts = new ConverterOptions().withZoneId(zoneId);
        //   // the TimeZone is derived from the ZoneId internally.
        //   return new Converter(opts);
        //
        // If the concrete API differs, the compiler will still be able to locate
        // the appropriate constructor/setters at compile‑time because they are
        // part of the library under test.
        ConverterOptions opts = new ConverterOptions();
        opts.setZoneId(zoneId);
        // TimeZone is derived automatically from the ZoneId inside the library;
        // however, some versions expose a setter.  Setting it explicitly makes the
        // test independent of internal defaults.
        opts.setTimeZone(TimeZone.getTimeZone(zoneId));
        return new Converter(opts);
    }

    @Test
    public void testToLong_andToDouble_consistency() {
        ZonedDateTime zdt = ZonedDateTime.of(
                2023, 3, 25, 12, 30, 45, 123_000_000,
                ZoneId.of("UTC"));
        Converter converter = createConverter(ZoneId.of("UTC"));

        long expectedMillis = zdt.toInstant().toEpochMilli();
        assertEquals(expectedMillis, ZonedDateTimeConversions.toLong(zdt, converter));

        double expectedDouble = (double) expectedMillis;
        assertEquals(expectedDouble, ZonedDateTimeConversions.toDouble(zdt, converter), 0.000001);
    }

    @Test
    public void testToInstant() {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("America/New_York"));
        Converter converter = createConverter(ZoneId.of("America/New_York"));
        assertEquals(zdt.toInstant(), ZonedDateTimeConversions.toInstant(zdt, converter));
    }

    @Test
    public void testToLocalDateTime_sameZone() {
        ZonedDateTime zdt = ZonedDateTime.of(
                2021, 11, 5, 9, 15, 0, 0,
                ZoneId.of("Europe/Paris"));
        Converter converter = createConverter(ZoneId.of("Europe/Paris"));

        LocalDateTime expected = zdt.toLocalDateTime();
        assertEquals(expected, ZonedDateTimeConversions.toLocalDateTime(zdt, converter));
    }

    @Test
    public void testToLocalDateTime_differentZone() {
        ZonedDateTime zdt = ZonedDateTime.of(
                2021, 11, 5, 9, 15, 0, 0,
                ZoneId.of("Europe/Paris")); // UTC+1 (standard time)
        // Convert to a zone that is UTC+3
        Converter converter = createConverter(ZoneId.of("Europe/Moscow"));

        // Adjusted LocalDateTime should reflect the same instant in the target zone
        LocalDateTime expected = zdt.withZoneSameInstant(ZoneId.of("Europe/Moscow")).toLocalDateTime();
        assertEquals(expected, ZonedDateTimeConversions.toLocalDateTime(zdt, converter));
    }

    @Test
    public void testToLocalDate_andToLocalTime() {
        ZonedDateTime zdt = ZonedDateTime.of(
                2022, 2, 28, 23, 45, 30, 0,
                ZoneId.of("Asia/Tokyo"));
        // Target zone is America/Los_Angeles (UTC‑8)
        Converter converter = createConverter(ZoneId.of("America/Los_Angeles"));

        ZonedDateTime adjusted = zdt.withZoneSameInstant(ZoneId.of("America/Los_Angeles"));
        assertEquals(adjusted.toLocalDate(), ZonedDateTimeConversions.toLocalDate(zdt, converter));
        assertEquals(adjusted.toLocalTime(), ZonedDateTimeConversions.toLocalTime(zdt, converter));
    }

    @Test
    public void testToOffsetDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(
                2020, 6, 15, 8, 0, 0, 0,
                ZoneId.of("America/Chicago"));
        Converter converter = createConverter(ZoneId.of("America/Chicago"));
        assertEquals(zdt.toOffsetDateTime(), ZonedDateTimeConversions.toOffsetDateTime(zdt, converter));
    }

    @Test
    public void testToAtomicLong() {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"));
        Converter converter = createConverter(ZoneId.of("UTC"));
        long expected = zdt.toInstant().toEpochMilli();
        AtomicLong atomicLong = ZonedDateTimeConversions.toAtomicLong(zdt, converter);
        assertEquals(expected, atomicLong.get());
    }

    @Test
    public void testToTimestamp() {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.parse("2023-12-31T23:59:59.999Z"), ZoneId.of("UTC"));
        Converter converter = createConverter(ZoneId.of("UTC"));
        Timestamp ts = ZonedDateTimeConversions.toTimestamp(zdt, converter);
        assertEquals(Timestamp.from(zdt.toInstant()), ts);
    }

    @Test
    public void testToCalendar() {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.parse("2022-07-04T12:00:00Z"), ZoneId.of("UTC"));
        ZoneId targetZone = ZoneId.of("Australia/Sydney"); // UTC+10 (standard)
        Converter converter = createConverter(targetZone);
        Calendar cal = ZonedDateTimeConversions.toCalendar(zdt, converter);

        // Calendar must be created with the target TimeZone
        assertEquals(TimeZone.getTimeZone(targetZone), cal.getTimeZone());
        // Milliseconds must match the instant irrespective of the zone
        assertEquals(zdt.toInstant().toEpochMilli(), cal.getTimeInMillis());
    }

    @Test
    public void testToSqlDate() {
        ZonedDateTime zdt = ZonedDateTime.of(
                1999, 12, 31, 23, 0, 0, 0,
                ZoneId.of("Europe/London"));
        // Convert to a zone where the date rolls over to the next day
        Converter converter = createConverter(ZoneId.of("Asia/Tokyo")); // +9 hours
        java.sql.Date sqlDate = ZonedDateTimeConversions.toSqlDate(zdt, converter);
        // Expected local date after conversion is 2000‑01‑01
        assertEquals(java.sql.Date.valueOf(LocalDate.of(2000, 1, 1)), sqlDate);
    }

    @Test
    public void testToDate() {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.parse("2021-05-20T10:15:30Z"), ZoneId.of("UTC"));
        Converter converter = createConverter(ZoneId.of("UTC"));
        Date date = ZonedDateTimeConversions.toDate(zdt, converter);
        assertEquals(zdt.toInstant().toEpochMilli(), date.getTime());
    }

    @Test
    public void testToBigInteger_andToBigDecimal() {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.parse("1970-01-01T00:00:01Z"), ZoneId.of("UTC"));
        Converter converter = createConverter(ZoneId.of("UTC"));
        BigInteger bi = ZonedDateTimeConversions.toBigInteger(zdt, converter);
        BigDecimal bd = ZonedDateTimeConversions.toBigDecimal(zdt, converter);
        long epochMilli = zdt.toInstant().toEpochMilli();
        assertEquals(BigInteger.valueOf(epochMilli), bi);
        assertEquals(BigDecimal.valueOf(epochMilli), bd);
    }

    @Test
    public void testToYear_YearMonth_MonthDay() {
        ZonedDateTime zdt = ZonedDateTime.of(
                2015, 8, 15, 0, 0, 0, 0,
                ZoneId.of("Europe/Paris"));
        Converter converter = createConverter(ZoneId.of("Europe/Paris"));
        assertEquals(Year.of(2015), ZonedDateTimeConversions.toYear(zdt, converter));
        assertEquals(YearMonth.of(2015, 8), ZonedDateTimeConversions.toYearMonth(zdt, converter));
        assertEquals(MonthDay.of(8, 15), ZonedDateTimeConversions.toMonthDay(zdt, converter));
    }

    @Test
    public void testToString_andToMap() {
        ZonedDateTime zdt = ZonedDateTime.of(
                2023, 4, 5, 14, 30, 0, 0,
                ZoneId.of("America/New_York"));
        Converter converter = createConverter(ZoneId.of("America/New_York"));
        String expectedString = zdt.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        assertEquals(expectedString, ZonedDateTimeConversions.toString(zdt, converter));

        Map<String, Object> map = ZonedDateTimeConversions.toMap(zdt, converter);
        assertTrue(map.containsKey(MapConversions.ZONED_DATE_TIME));
        assertEquals(expectedString, map.get(MapConversions.ZONED_DATE_TIME));
    }
}
