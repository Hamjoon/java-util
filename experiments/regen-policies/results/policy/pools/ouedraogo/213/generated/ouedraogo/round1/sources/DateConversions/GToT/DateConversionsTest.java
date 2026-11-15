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

public class DateConversionsTest {

    private Converter converter;
    private ZoneId zoneId = ZoneOffset.UTC;   // assume default options use UTC

    @Before
    public void setUp() {
        // ConverterOptions is not shown; we assume it has a public no‑arg constructor
        // and defaults to UTC. If a different constructor is required, adjust accordingly.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /* ----------------------------------------------------------------------
     *  Basic happy‑path tests for each conversion method
     * ---------------------------------------------------------------------- */

    @Test
    public void testToZonedDateTime() {
        Date date = new Date(1_600_000_000_000L); // 2020‑09‑13T12:26:40Z
        ZonedDateTime zdt = DateConversions.toZonedDateTime(date, converter);
        assertEquals(Instant.ofEpochMilli(date.getTime()), zdt.toInstant());
        assertEquals(zoneId, zdt.getZone());
    }

    @Test
    public void testToLong() {
        Date date = new Date(123456789L);
        long l = DateConversions.toLong(date, converter);
        assertEquals(date.getTime(), l);
    }

    @Test
    public void testToDouble() {
        Date date = new Date(1_234_567L);
        double d = DateConversions.toDouble(date, converter);
        assertEquals(date.getTime() / 1000.0, d, 0.0);
    }

    @Test
    public void testToSqlDate() {
        // 2021‑03‑14 02:30:00 UTC (a DST‑transition moment in many zones, but we stay in UTC)
        Date date = new Date(1_613_080_200_000L);
        java.sql.Date sqlDate = DateConversions.toSqlDate(date, converter);
        LocalDate expected = Instant.ofEpochMilli(date.getTime()).atZone(zoneId).toLocalDate();
        assertEquals(expected, sqlDate.toLocalDate());
    }

    @Test
    public void testToDate() {
        Date src = new Date(987654321L);
        Date dst = DateConversions.toDate(src, converter);
        assertEquals(src.getTime(), dst.getTime());
        // ensure a new instance is returned
        assertNotSame(src, dst);
    }

    @Test
    public void testToTimestamp() {
        Date src = new Date(555555555L);
        Timestamp ts = DateConversions.toTimestamp(src, converter);
        assertEquals(src.getTime(), ts.getTime());
    }

    @Test
    public void testToCalendar() {
        Date src = new Date(1_000_000L);
        Calendar cal = DateConversions.toCalendar(src, converter);
        assertEquals(src.getTime(), cal.getTimeInMillis());
    }

    @Test
    public void testToBigDecimal() {
        Date src = new Date(1_234_567_890L);
        BigDecimal bd = DateConversions.toBigDecimal(src, converter);
        // Expected: epoch millis divided by 1000 with 9 decimal places, rounding down
        BigDecimal expected = new BigDecimal(src.getTime())
                .divide(new BigDecimal(1000), 9, RoundingMode.DOWN);
        assertEquals(0, expected.compareTo(bd));
        assertEquals(9, bd.scale());
    }

    @Test
    public void testToInstantWithUtilDate() {
        Date src = new Date(42L);
        Instant i = DateConversions.toInstant(src, converter);
        assertEquals(src.toInstant(), i);
    }

    @Test
    public void testToInstantWithSqlDate() {
        java.sql.Date src = java.sql.Date.valueOf("2022-01-01");
        // java.sql.Date's getTime() is midnight UTC of the given day
        Instant i = DateConversions.toInstant(src, converter);
        assertEquals(new java.util.Date(src.getTime()).toInstant(), i);
    }

    @Test
    public void testToOffsetDateTime() {
        Date src = new Date(1_600_000_000_000L);
        OffsetDateTime odt = DateConversions.toOffsetDateTime(src, converter);
        OffsetDateTime expected = DateConversions.toInstant(src, converter)
                .atZone(zoneId).toOffsetDateTime();
        assertEquals(expected, odt);
    }

    @Test
    public void testToLocalDateTime() {
        Date src = new Date(1_600_000_000_000L);
        LocalDateTime ldt = DateConversions.toLocalDateTime(src, converter);
        LocalDateTime expected = DateConversions.toZonedDateTime(src, converter).toLocalDateTime();
        assertEquals(expected, ldt);
    }

    @Test
    public void testToLocalDate() {
        Date src = new Date(1_600_000_000_000L);
        LocalDate ld = DateConversions.toLocalDate(src, converter);
        LocalDate expected = DateConversions.toZonedDateTime(src, converter).toLocalDate();
        assertEquals(expected, ld);
    }

    @Test
    public void testToLocalTime() {
        Date src = new Date(1_600_000_123_456L); // includes a non‑zero time‑of‑day part
        LocalTime lt = DateConversions.toLocalTime(src, converter);
        LocalDateTime ldt = LocalDateTime.ofInstant(src.toInstant(), zoneId);
        assertEquals(ldt.toLocalTime(), lt);
    }

    @Test
    public void testToBigInteger() {
        Date src = new Date(1_234_567_890L);
        BigInteger bi = DateConversions.toBigInteger(src, converter);
        // The exact conversion performed by InstantConversions is not visible,
        // but we can assert that the result is non‑null and positive for a positive epoch.
        assertNotNull(bi);
        assertTrue(bi.signum() >= 0);
    }

    @Test
    public void testToAtomicLong() {
        Date src = new Date(777L);
        AtomicLong al = DateConversions.toAtomicLong(src, converter);
        assertEquals(src.getTime(), al.get());
    }

    @Test
    public void testToYear() {
        Date src = new Date(1_600_000_000_000L); // 2020‑09‑13
        Year y = DateConversions.toYear(src, converter);
        assertEquals(2020, y.getValue());
    }

    @Test
    public void testToYearMonth() {
        Date src = new Date(1_600_000_000_000L); // 2020‑09‑13
        YearMonth ym = DateConversions.toYearMonth(src, converter);
        assertEquals(2020, ym.getYear());
        assertEquals(9, ym.getMonthValue());
    }

    @Test
    public void testToMonthDay() {
        Date src = new Date(1_600_000_000_000L); // 2020‑09‑13
        MonthDay md = DateConversions.toMonthDay(src, converter);
        assertEquals(9, md.getMonthValue());
        assertEquals(13, md.getDayOfMonth());
    }

    @Test
    public void testToString() {
        // 2021‑01‑01T00:00:00.123Z
        Date src = new Date(1_609_459_200_123L);
        String s = DateConversions.toString(src, converter);
        // The formatter forces exactly three decimal places
        assertEquals("2021-01-01T00:00:00.123Z", s);
    }

    @Test
    public void testToMap() {
        Date src = new Date(1_609_459_200_123L);
        Map<String, Object> map = DateConversions.toMap(src, converter);
        assertEquals(1, map.size());
        // The key constant is not visible; we verify the single value matches toString()
        Object value = map.values().iterator().next();
        assertEquals(DateConversions.toString(src, converter), value);
    }

    /* ----------------------------------------------------------------------
     *  Edge‑case and exception tests
     * ---------------------------------------------------------------------- */

    @Test(expected = ClassCastException.class)
    public void testToZonedDateTimeWithInvalidType() {
        DateConversions.toZonedDateTime("not a date", converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTimeWithNull() {
        DateConversions.toZonedDateTime(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLongWithInvalidType() {
        DateConversions.toLong(123, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLongWithNull() {
        DateConversions.toLong(null, converter);
    }

    @Test
    public void testToBigDecimalRoundingDown() {
        // epoch millis that would cause rounding up if not truncated
        Date src = new Date(1_999L); // 0.001999 seconds
        BigDecimal bd = DateConversions.toBigDecimal(src, converter);
        // Expected 0.001 with 9 decimal places (rounded down)
        BigDecimal expected = new BigDecimal("0.001000000");
        assertEquals(0, expected.compareTo(bd));
    }

    @Test
    public void testToStringZeroMillis() {
        Date src = new Date(0L);
        String s = DateConversions.toString(src, converter);
        assertEquals("1970-01-01T00:00:00.000Z", s);
    }

    @Test(expected = ClassCastException.class)
    public void testToSqlDateWithInvalidType() {
        DateConversions.toSqlDate(42, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToSqlDateWithNull() {
        DateConversions.toSqlDate(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToInstantWithInvalidType() {
        DateConversions.toInstant(new Object(), converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToInstantWithNull() {
        DateConversions.toInstant(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMapWithInvalidType() {
        DateConversions.toMap(12345, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToMapWithNull() {
        DateConversions.toMap(null, converter);
    }
}
