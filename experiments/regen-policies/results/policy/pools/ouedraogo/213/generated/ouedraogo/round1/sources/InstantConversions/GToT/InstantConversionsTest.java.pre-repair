package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

import static org.junit.Assert.*;

public class InstantConversionsTest {

    private Converter converterUTC;
    private Converter converterNY;   // America/New_York (has DST)

    /** 
     * Helper to create a Converter with given ZoneId/TimeZone.
     * The real library’s {@code ConverterOptions} class provides setters for these values.
     * If the actual API differs, adjust the test accordingly.
     */
    private static Converter createConverter(ZoneId zoneId) {
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(zoneId);
        options.setTimeZone(TimeZone.getTimeZone(zoneId));
        return new Converter(options);
    }

    @Before
    public void setUp() {
        converterUTC = createConverter(ZoneOffset.UTC);
        converterNY  = createConverter(ZoneId.of("America/New_York"));
    }

    /* ------------------------------------------------------------------------
     *  1. toMap
     * ------------------------------------------------------------------------ */
    @Test
    public void testToMap_Basic() {
        Instant instant = Instant.parse("2023-03-15T12:34:56.123456789Z");
        Map result = InstantConversions.toMap(instant, converterUTC);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(instant.toString(), result.get(MapConversions.INSTANT));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInstant() {
        InstantConversions.toMap(null, converterUTC);
    }

    /* ------------------------------------------------------------------------
     *  2. toZonedDateTime
     * ------------------------------------------------------------------------ */
    @Test
    public void testToZonedDateTime_UTC() {
        Instant instant = Instant.parse("2022-01-01T00:00:00Z");
        ZonedDateTime zdt = InstantConversions.toZonedDateTime(instant, converterUTC);
        assertEquals(ZoneOffset.UTC, zdt.getOffset());
        assertEquals(instant, zdt.toInstant());
    }

    @Test
    public void testToZonedDateTime_NonUTC() {
        Instant instant = Instant.parse("2022-06-01T12:00:00Z");
        ZonedDateTime zdt = InstantConversions.toZonedDateTime(instant, converterNY);
        // New York is UTC‑4 in June (EDT)
        assertEquals(ZoneId.of("America/New_York"), zdt.getZone());
        assertEquals(ZoneOffset.ofHours(-4), zdt.getOffset());
        assertEquals(instant, zdt.toInstant());
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_NullInstant() {
        InstantConversions.toZonedDateTime(null, converterUTC);
    }

    /* ------------------------------------------------------------------------
     *  3. toOffsetDateTime
     * ------------------------------------------------------------------------ */
    @Test
    public void testToOffsetDateTime_UTC() {
        Instant instant = Instant.parse("2021-12-31T23:00:00Z");
        OffsetDateTime odt = InstantConversions.toOffsetDateTime(instant, converterUTC);
        assertEquals(ZoneOffset.UTC, odt.getOffset());
        assertEquals(instant, odt.toInstant());
    }

    @Test
    public void testToOffsetDateTime_NonUTC_WithDST() {
        // 2021-11-07 06:30:00Z is 01:30:00 EST (after DST ends)
        Instant instant = Instant.parse("2021-11-07T06:30:00Z");
        OffsetDateTime odt = InstantConversions.toOffsetDateTime(instant, converterNY);
        // New York offset after DST end is -5 hours
        assertEquals(ZoneOffset.ofHours(-5), odt.getOffset());
        assertEquals(instant, odt.toInstant());
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTime_NullInstant() {
        InstantConversions.toOffsetDateTime(null, converterUTC);
    }

    /* ------------------------------------------------------------------------
     *  4. toLong (epoch milli)
     * ------------------------------------------------------------------------ */
    @Test
    public void testToLong_Basic() {
        Instant instant = Instant.ofEpochMilli(1_600_000_000_123L);
        long epochMilli = InstantConversions.toLong(instant, converterUTC);
        assertEquals(1_600_000_000_123L, epochMilli);
    }

    @Test
    public void testToLong_EpochZero() {
        Instant instant = Instant.ofEpochMilli(0L);
        assertEquals(0L, InstantConversions.toLong(instant, converterUTC));
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInstant() {
        InstantConversions.toLong(null, converterUTC);
    }

    /* ------------------------------------------------------------------------
     *  5. toDouble (seconds with fractional nanoseconds)
     * ------------------------------------------------------------------------ */
    @Test
    public void testToDouble_WithNanos() {
        // 1 second + 500,000,001 nanoseconds = 1.500000001 seconds
        Instant instant = Instant.ofEpochSecond(1, 500_000_001);
        double d = InstantConversions.toDouble(instant, converterUTC);
        assertEquals(1.500000001, d, 1e-12);
    }

    @Test
    public void testToDouble_NegativeInstant() {
        // -2 seconds and -300,000,000 nanos => -2.3 seconds
        Instant instant = Instant.ofEpochSecond(-2, -300_000_000);
        double d = InstantConversions.toDouble(instant, converterUTC);
        assertEquals(-2.3, d, 1e-12);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullInstant() {
        InstantConversions.toDouble(null, converterUTC);
    }

    /* ------------------------------------------------------------------------
     *  6. toAtomicLong
     * ------------------------------------------------------------------------ */
    @Test
    public void testToAtomicLong_Basic() {
        Instant instant = Instant.ofEpochMilli(12345L);
        AtomicLong al = InstantConversions.toAtomicLong(instant, converterUTC);
        assertEquals(12345L, al.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullInstant() {
        InstantConversions.toAtomicLong(null, converterUTC);
    }

    /* ------------------------------------------------------------------------
     *  7. toTimestamp
     * ------------------------------------------------------------------------ */
    @Test
    public void testToTimestamp_Basic() {
        Instant instant = Instant.parse("2020-02-29T12:34:56.789Z");
        Timestamp ts = InstantConversions.toTimestamp(instant, converterUTC);
        assertEquals(Timestamp.from(instant), ts);
    }

    @Test(expected = NullPointerException.class)
    public void testToTimestamp_NullInstant() {
        InstantConversions.toTimestamp(null, converterUTC);
    }

    /* ------------------------------------------------------------------------
     *  8. toSqlDate
     * ------------------------------------------------------------------------ */
    @Test
    public void testToSqlDate_UTC() {
        Instant instant = Instant.parse("2022-12-25T00:00:00Z");
        java.sql.Date sqlDate = InstantConversions.toSqlDate(instant, converterUTC);
        assertEquals(java.sql.Date.valueOf("2022-12-25"), sqlDate);
    }

    @Test
    public void testToSqlDate_NonUTC_DateShift() {
        // 2022-12-31T23:30:00Z is 2022-12-31 18:30 in New York (still same day)
        Instant instant = Instant.parse("2022-12-31T23:30:00Z");
        java.sql.Date sqlDate = InstantConversions.toSqlDate(instant, converterNY);
        assertEquals(java.sql.Date.valueOf("2022-12-31"), sqlDate);
    }

    @Test(expected = NullPointerException.class)
    public void testToSqlDate_NullInstant() {
        InstantConversions.toSqlDate(null, converterUTC);
    }

    /* ------------------------------------------------------------------------
     *  9. toDate (java.util.Date)
     * ------------------------------------------------------------------------ */
    @Test
    public void testToDate_Basic() {
        Instant instant = Instant.ofEpochMilli(9_876_543_210L);
        Date date = InstantConversions.toDate(instant, converterUTC);
        assertEquals(9_876_543_210L, date.getTime());
    }

    @Test(expected = NullPointerException.class)
    public void testToDate_NullInstant() {
        InstantConversions.toDate(null, converterUTC);
    }

    /* ------------------------------------------------------------------------
     * 10. toCalendar
     * ------------------------------------------------------------------------ */
    @Test
    public void testToCalendar_Basic() {
        Instant instant = Instant.ofEpochMilli(1_234_567_890L);
        Calendar cal = InstantConversions.toCalendar(instant, converterUTC);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(1_234_567_890L, cal.getTimeInMillis());
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendar_NullInstant() {
        InstantConversions.toCalendar(null, converterUTC);
    }

    /* ------------------------------------------------------------------------
     * 11. toBigInteger (nanoseconds since epoch)
     * ------------------------------------------------------------------------ */
    @Test
    public void testToBigInteger_Basic() {
        Instant instant = Instant.ofEpochSecond(2, 3); // 2 seconds + 3 nanos
        BigInteger bi = InstantConversions.toBigInteger(instant, converterUTC);
        BigInteger expected = BigInteger.valueOf(2).multiply(BigInteger.valueOf(1_000_000_000L))
                                            .add(BigInteger.valueOf(3));
        assertEquals(expected, bi);
    }

    @Test
    public void testToBigInteger_NegativeInstant() {
        Instant instant = Instant.ofEpochSecond(-1, -500_000_000); // -1.5 seconds
        BigInteger bi = InstantConversions.toBigInteger(instant, converterUTC);
        BigInteger expected = BigInteger.valueOf(-1).multiply(BigInteger.valueOf(1_000_000_000L))
                                            .add(BigInteger.valueOf(-500_000_000));
        assertEquals(expected, bi);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullInstant() {
        InstantConversions.toBigInteger(null, converterUTC);
    }

    /* ------------------------------------------------------------------------
     * 12. toBigDecimal (seconds with nanos as fraction)
     * ------------------------------------------------------------------------ */
    @Test
    public void testToBigDecimal_WithNanos() {
        Instant instant = Instant.ofEpochSecond(10, 250_000_000); // 10.25 seconds
        BigDecimal bd = InstantConversions.toBigDecimal(instant, converterUTC);
        assertEquals(new BigDecimal("10.25"), bd);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullInstant() {
        InstantConversions.toBigDecimal(null, converterUTC);
    }

    /* ------------------------------------------------------------------------
     * 13. toLocalDateTime
     * ------------------------------------------------------------------------ */
    @Test
    public void testToLocalDateTime_UTC() {
        Instant instant = Instant.parse("2023-07-04T10:15:30Z");
        LocalDateTime ldt = InstantConversions.toLocalDateTime(instant, converterUTC);
        assertEquals(LocalDateTime.of(2023, 7, 4, 10, 15, 30), ldt);
    }

    @Test
    public void testToLocalDateTime_NonUTC() {
        Instant instant = Instant.parse("2023-01-01T02:00:00Z");
        // In New York (UTC‑5) this is 2022-12-31 21:00
        LocalDateTime ldt = InstantConversions.toLocalDateTime(instant, converterNY);
        assertEquals(LocalDateTime.of(2022, 12, 31, 21, 0, 0), ldt);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTime_NullInstant() {
        InstantConversions.toLocalDateTime(null, converterUTC);
    }

    /* ------------------------------------------------------------------------
     * 14. toLocalDate
     * ------------------------------------------------------------------------ */
    @Test
    public void testToLocalDate_UTC() {
        Instant instant = Instant.parse("2020-02-29T23:59:59Z");
        LocalDate ld = InstantConversions.toLocalDate(instant, converterUTC);
        assertEquals(LocalDate.of(2020, 2, 29), ld);
    }

    @Test
    public void testToLocalDate_NonUTC_DateRollOver() {
        // 2020-03-01T02:00:00Z is 2020-02-29 21:00 in New York (still Feb 29)
        Instant instant = Instant.parse("2020-03-01T02:00:00Z");
        LocalDate ld = InstantConversions.toLocalDate(instant, converterNY);
        assertEquals(LocalDate.of(2020, 2, 29), ld);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDate_NullInstant() {
        InstantConversions.toLocalDate(null, converterUTC);
    }

    /* ------------------------------------------------------------------------
     * 15. toLocalTime
     * ------------------------------------------------------------------------ */
    @Test
    public void testToLocalTime_UTC() {
        Instant instant = Instant.parse("2021-08-15T12:34:56.789Z");
        LocalTime lt = InstantConversions.toLocalTime(instant, converterUTC);
        assertEquals(LocalTime.of(12, 34, 56, 789_000_000), lt);
    }

    @Test
    public void testToLocalTime_NonUTC() {
        // 2021-08-15T00:30:00Z is 2021-08-14 20:30 in New York (UTC‑4)
        Instant instant = Instant.parse("2021-08-15T00:30:00Z");
        LocalTime lt = InstantConversions.toLocalTime(instant, converterNY);
        assertEquals(LocalTime.of(20, 30, 0), lt);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullInstant() {
        InstantConversions.toLocalTime(null, converterUTC);
    }

    /* ------------------------------------------------------------------------
     *  Edge‑case tests for extreme Instant values
     * ------------------------------------------------------------------------ */
    @Test
    public void testExtremeInstants() {
        // Instant.MAX (year +1000000000) – only test that conversion does not throw
        Instant max = Instant.MAX;
        assertNotNull(InstantConversions.toLong(max, converterUTC));
        assertNotNull(InstantConversions.toDouble(max, converterUTC));
        assertNotNull(InstantConversions.toBigInteger(max, converterUTC));
        assertNotNull(InstantConversions.toBigDecimal(max, converterUTC));

        // Instant.MIN (year -1000000000)
        Instant min = Instant.MIN;
        assertNotNull(InstantConversions.toLong(min, converterUTC));
        assertNotNull(InstantConversions.toDouble(min, converterUTC));
        assertNotNull(InstantConversions.toBigInteger(min, converterUTC));
        assertNotNull(InstantConversions.toBigDecimal(min, converterUTC));
    }
}
