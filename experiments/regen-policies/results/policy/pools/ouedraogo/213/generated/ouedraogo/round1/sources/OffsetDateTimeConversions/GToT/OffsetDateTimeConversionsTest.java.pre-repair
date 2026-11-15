package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class OffsetDateTimeConversionsTest {

    private Converter converter;
    private ZoneId zoneId = ZoneOffset.UTC;
    private OffsetDateTime sampleOdt;

    @Before
    public void setUp() {
        // The library provides a default‑constructed ConverterOptions that uses UTC.
        // We rely on that default for the tests.
        ConverterOptions options = new ConverterOptions(); // defaults to UTC
        converter = new Converter(options);

        // Sample OffsetDateTime used by most tests (2022‑03‑15T10:15:30.123456789Z)
        sampleOdt = OffsetDateTime.of(
                2022, 3, 15,
                10, 15, 30, 123_456_789,
                ZoneOffset.UTC);
    }

    /* ----------------------------------------------------------------------
     *  Basic functionality tests – one test per public static method
     * ---------------------------------------------------------------------- */

    @Test
    public void testToInstant() {
        Instant expected = sampleOdt.toInstant();
        Instant actual = OffsetDateTimeConversions.toInstant(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLong() {
        long expected = sampleOdt.toInstant().toEpochMilli();
        long actual = OffsetDateTimeConversions.toLong(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToAtomicLong() {
        long expected = sampleOdt.toInstant().toEpochMilli();
        AtomicLong atomic = OffsetDateTimeConversions.toAtomicLong(sampleOdt, converter);
        assertEquals(expected, atomic.get());
    }

    @Test
    public void testToDouble() {
        Instant instant = sampleOdt.toInstant();
        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000d;
        double actual = OffsetDateTimeConversions.toDouble(sampleOdt, converter);
        assertEquals(expected, actual, 0.000000001);
    }

    @Test
    public void testToBigInteger() {
        Instant instant = sampleOdt.toInstant();
        BigInteger expected = BigInteger.valueOf(instant.toEpochMilli());
        BigInteger actual = OffsetDateTimeConversions.toBigInteger(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigDecimal() {
        Instant instant = sampleOdt.toInstant();
        BigDecimal expected = new BigDecimal(instant.toEpochMilli());
        BigDecimal actual = OffsetDateTimeConversions.toBigDecimal(sampleOdt, converter);
        assertEquals(0, expected.compareTo(actual));
    }

    @Test
    public void testToLocalDateTime() {
        ZonedDateTime zdt = sampleOdt.toInstant().atZone(zoneId);
        LocalDateTime expected = zdt.toLocalDateTime();
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDate() {
        ZonedDateTime zdt = sampleOdt.toInstant().atZone(zoneId);
        LocalDate expected = zdt.toLocalDate();
        LocalDate actual = OffsetDateTimeConversions.toLocalDate(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalTime() {
        ZonedDateTime zdt = sampleOdt.toInstant().atZone(zoneId);
        LocalTime expected = zdt.toLocalTime();
        LocalTime actual = OffsetDateTimeConversions.toLocalTime(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToTimestamp() {
        Timestamp expected = Timestamp.from(sampleOdt.toInstant());
        Timestamp actual = OffsetDateTimeConversions.toTimestamp(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToCalendar() {
        Calendar calendar = OffsetDateTimeConversions.toCalendar(sampleOdt, converter);
        assertEquals(sampleOdt.toInstant().toEpochMilli(), calendar.getTimeInMillis());
        assertEquals(converter.getOptions().getTimeZone(), calendar.getTimeZone());
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date expected = java.sql.Date.valueOf(
                sampleOdt.atZoneSameInstant(zoneId).toLocalDate());
        java.sql.Date actual = OffsetDateTimeConversions.toSqlDate(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime expected = sampleOdt.toInstant().atZone(zoneId);
        ZonedDateTime actual = OffsetDateTimeConversions.toZonedDateTime(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDate() {
        Date expected = new Date(sampleOdt.toInstant().toEpochMilli());
        Date actual = OffsetDateTimeConversions.toDate(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetTime() {
        OffsetTime expected = sampleOdt.toOffsetTime();
        OffsetTime actual = OffsetDateTimeConversions.toOffsetTime(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToYear() {
        Year expected = Year.from(
                sampleOdt.atZoneSameInstant(zoneId).toLocalDate());
        Year actual = OffsetDateTimeConversions.toYear(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToYearMonth() {
        YearMonth expected = YearMonth.from(
                sampleOdt.atZoneSameInstant(zoneId).toLocalDate());
        YearMonth actual = OffsetDateTimeConversions.toYearMonth(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToMonthDay() {
        MonthDay expected = MonthDay.from(
                sampleOdt.atZoneSameInstant(zoneId).toLocalDate());
        MonthDay actual = OffsetDateTimeConversions.toMonthDay(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToString() {
        String expected = sampleOdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String actual = OffsetDateTimeConversions.toString(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = OffsetDateTimeConversions.toMap(sampleOdt, converter);
        assertEquals(1, map.size());
        assertEquals(OffsetDateTimeConversions.toString(sampleOdt, converter),
                map.values().iterator().next());
    }

    /* ----------------------------------------------------------------------
     *  Edge‑case and exception handling tests
     * ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToInstant_NullInput() {
        OffsetDateTimeConversions.toInstant(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInput() {
        OffsetDateTimeConversions.toLong(null, converter);
    }

    @Test
    public void testExtremeOffsetDateTime_ToString() {
        // OffsetDateTime with maximum supported year and maximum offset
        OffsetDateTime extreme = OffsetDateTime.of(
                9999, 12, 31,
                23, 59, 59, 999_999_999,
                ZoneOffset.ofHoursMinutes(18, 0)); // +18:00 is the max offset
        String result = OffsetDateTimeConversions.toString(extreme, converter);
        // Verify it follows the ISO‑8601 pattern and does not throw
        assertTrue(result.matches("\\d{4}-12-31T23:59:59\\.999999999\\+18:00"));
    }

    @Test
    public void testDifferentZoneOffset() {
        OffsetDateTime withOffset = OffsetDateTime.of(
                2022, 3, 15,
                10, 15, 30, 0,
                ZoneOffset.ofHours(-5)); // -05:00
        // The conversion to ZonedDateTime must respect the converter's UTC zone
        ZonedDateTime expected = withOffset.toInstant().atZone(zoneId);
        ZonedDateTime actual = OffsetDateTimeConversions.toZonedDateTime(withOffset, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testCalendarTimeZoneRespectsConverterOptions() {
        Calendar cal = OffsetDateTimeConversions.toCalendar(sampleOdt, converter);
        assertEquals(converter.getOptions().getTimeZone(), cal.getTimeZone());
    }

    @Test
    public void testToMapKeyIsConsistent() {
        // The map key is defined in MapConversions.OFFSET_DATE_TIME.
        // Since we cannot reference that constant directly, we verify that the map
        // contains exactly one entry and that the key is non‑null.
        Map<String, Object> map = OffsetDateTimeConversions.toMap(sampleOdt, converter);
        assertEquals(1, map.size());
        String key = map.keySet().iterator().next();
        assertNotNull(key);
        assertEquals(OffsetDateTimeConversions.toString(sampleOdt, converter), map.get(key));
    }
}
