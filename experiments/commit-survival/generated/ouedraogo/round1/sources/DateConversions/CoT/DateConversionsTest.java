package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class DateConversionsTest {

    private Converter converter;
    private ZoneId zone = ZoneOffset.UTC; // use UTC for deterministic results
    private Date sampleDate;          // 2021-10-01T00:00:00Z
    private java.sql.Date sampleSqlDate; // same instant as sampleDate
    private Date preEpochDate;        // 1969-12-31T23:59:59.999Z (‑1 ms)

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // ConverterOptions is not provided; we assume it has a public no‑arg constructor
        // and defaults to UTC. If a different constructor is needed, adjust accordingly.
        ConverterOptions options = new ConverterOptions();
        // Force the zone to UTC (most implementations expose a setter; if not, rely on default)
        // The test suite assumes the default zone is UTC.
        converter = new Converter(options);

        sampleDate = new Date(1633046400000L); // 2021‑10‑01T00:00:00Z
        sampleSqlDate = new java.sql.Date(sampleDate.getTime());
        preEpochDate = new Date(-1L); // 1969‑12‑31T23:59:59.999Z
    }

    /* ----------------------------------------------------------------------
     *  Basic functionality tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime zdt = DateConversions.toZonedDateTime(sampleDate, converter);
        assertEquals(sampleDate.toInstant().atZone(zone), zdt);
    }

    @Test
    public void testToLong() {
        long epoch = DateConversions.toLong(sampleDate, converter);
        assertEquals(sampleDate.getTime(), epoch);
    }

    @Test
    public void testToDouble() {
        double seconds = DateConversions.toDouble(sampleDate, converter);
        assertEquals(sampleDate.getTime() / 1000.0, seconds, 0.000001);
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date sqlDate = DateConversions.toSqlDate(sampleDate, converter);
        assertEquals(sampleDate.getTime(), sqlDate.getTime());
    }

    @Test
    public void testToDate() {
        Date d = DateConversions.toDate(sampleDate, converter);
        assertEquals(sampleDate, d);
    }

    @Test
    public void testToTimestamp() {
        Timestamp ts = DateConversions.toTimestamp(sampleDate, converter);
        assertEquals(sampleDate.getTime(), ts.getTime());
    }

    @Test
    public void testToCalendar() {
        Calendar cal = DateConversions.toCalendar(sampleDate, converter);
        assertEquals(sampleDate.getTime(), cal.getTimeInMillis());
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal bd = DateConversions.toBigDecimal(sampleDate, converter);
        // The method divides epoch millis by 1_000_000_000 (GRAND) with scale 9, rounding down.
        BigDecimal expected = new BigDecimal(sampleDate.getTime())
                .divide(BigDecimalConversions.GRAND, 9, RoundingMode.DOWN);
        assertEquals(expected, bd);
    }

    @Test
    public void testToInstant_WithUtilDate() {
        Instant instant = DateConversions.toInstant(sampleDate, converter);
        assertEquals(sampleDate.toInstant(), instant);
    }

    @Test
    public void testToInstant_WithSqlDate() {
        Instant instant = DateConversions.toInstant(sampleSqlDate, converter);
        // sqlDate is a subclass of java.util.Date; the method creates a new java.util.Date
        // with the same epoch millis, therefore the instant should be identical.
        assertEquals(sampleSqlDate.getTime(), instant.toEpochMilli());
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime odt = DateConversions.toOffsetDateTime(sampleDate, converter);
        assertEquals(DateConversions.toInstant(sampleDate, converter)
                .atZone(zone).toOffsetDateTime(), odt);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = DateConversions.toLocalDateTime(sampleDate, converter);
        assertEquals(DateConversions.toZonedDateTime(sampleDate, converter).toLocalDateTime(), ldt);
    }

    @Test
    public void testToLocalDate() {
        LocalDate ld = DateConversions.toLocalDate(sampleDate, converter);
        assertEquals(DateConversions.toZonedDateTime(sampleDate, converter).toLocalDate(), ld);
    }

    @Test
    public void testToLocalTime() {
        LocalTime lt = DateConversions.toLocalTime(sampleDate, converter);
        assertEquals(DateConversions.toZonedDateTime(sampleDate, converter).toLocalTime(), lt);
    }

    @Test
    public void testToBigInteger() {
        BigInteger bi = DateConversions.toBigInteger(sampleDate, converter);
        // InstantConversions.toBigInteger converts the instant's epoch seconds to a BigInteger.
        // For UTC the epoch seconds are epochMillis / 1000.
        BigInteger expected = BigInteger.valueOf(sampleDate.getTime() / 1000L);
        assertEquals(expected, bi);
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong al = DateConversions.toAtomicLong(sampleDate, converter);
        assertEquals(sampleDate.getTime(), al.get());
    }

    @Test
    public void testSqlDateToString() {
        String s = DateConversions.sqlDateToString(sampleSqlDate, converter);
        // The string should be the same as DateConversions.toString(new Date(...))
        String expected = DateConversions.toString(new Date(sampleSqlDate.getTime()), converter);
        assertEquals(expected, s);
    }

    @Test
    public void testToString_Format() {
        String iso = DateConversions.toString(sampleDate, converter);
        // Expected pattern: yyyy-MM-dd'T'HH:mm:ss.SSS+HH:MM (or Z for UTC)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXXXX");
        ZonedDateTime expectedZdt = sampleDate.toInstant().atZone(zone);
        String expected = expectedZdt.format(formatter);
        assertEquals(expected, iso);
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = DateConversions.toMap(sampleDate, converter);
        ZonedDateTime zdt = DateConversions.toZonedDateTime(sampleDate, converter);
        assertEquals(zdt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(zdt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(zone.toString(), map.get(MapConversions.ZONE));
        assertEquals(sampleDate.getTime(), map.get(MapConversions.EPOCH_MILLIS));
    }

    /* ----------------------------------------------------------------------
     *  Edge‑case and exception tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testNullInputThrowsNPE() {
        thrown.expect(NullPointerException.class);
        DateConversions.toLong(null, converter);
    }

    @Test
    public void testPreEpochDateConversions() {
        // Verify that negative epoch millis are handled correctly.
        long epoch = DateConversions.toLong(preEpochDate, converter);
        assertEquals(-1L, epoch);

        double seconds = DateConversions.toDouble(preEpochDate, converter);
        assertEquals(-0.001, seconds, 0.000001);

        Instant instant = DateConversions.toInstant(preEpochDate, converter);
        assertEquals(-1L, instant.toEpochMilli());

        ZonedDateTime zdt = DateConversions.toZonedDateTime(preEpochDate, converter);
        assertEquals(Instant.ofEpochMilli(-1L).atZone(zone), zdt);
    }

    @Test
    public void testBigDecimalRoundingDown() {
        // Create a date where epoch millis is not an exact multiple of 1_000_000_000.
        Date oddDate = new Date(1_234_567_890L); // 1.23456789 seconds
        BigDecimal bd = DateConversions.toBigDecimal(oddDate, converter);
        // Expected value = 1.234567890 (scale 9) rounded down (no change because we already have 9 digits)
        BigDecimal expected = new BigDecimal(oddDate.getTime())
                .divide(BigDecimalConversions.GRAND, 9, RoundingMode.DOWN);
        assertEquals(expected, bd);
    }

    @Test
    public void testSqlDateToString_WithPreEpoch() {
        java.sql.Date preSql = new java.sql.Date(preEpochDate.getTime());
        String s = DateConversions.sqlDateToString(preSql, converter);
        // Ensure formatting still works (should contain a minus sign in the year)
        assertTrue(s.startsWith("-0"));
    }

    @Test
    public void testToMapContainsAllKeys() {
        Map<String, Object> map = DateConversions.toMap(sampleDate, converter);
        assertTrue(map.containsKey(MapConversions.DATE));
        assertTrue(map.containsKey(MapConversions.TIME));
        assertTrue(map.containsKey(MapConversions.ZONE));
        assertTrue(map.containsKey(MapConversions.EPOCH_MILLIS));
    }
}
