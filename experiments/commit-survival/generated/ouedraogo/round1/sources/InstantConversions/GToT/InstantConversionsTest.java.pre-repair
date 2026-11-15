package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class InstantConversionsTest {

    private Converter utcConverter;   // Converter that uses UTC zone / timezone
    private Converter defaultConverter; // Converter that uses the JVM defaults

    @Before
    public void setUp() {
        // The concrete classes ConverterOptions, Converter are part of the library.
        // They provide a default constructor and setters for zoneId / timeZone.
        // The test assumes such setters exist; if they are immutable, the
        // constructors can be used instead.

        // ---- UTC based converter ------------------------------------------------
        ConverterOptions utcOptions = new ConverterOptions();
        utcOptions.setZoneId(ZoneId.of("UTC"));
        utcOptions.setTimeZone(TimeZone.getTimeZone("UTC"));
        utcConverter = new Converter(utcOptions);

        // ---- Default (system) converter -----------------------------------------
        ConverterOptions defaultOptions = new ConverterOptions(); // defaults to system zone / tz
        defaultConverter = new Converter(defaultOptions);
    }

    /* -------------------------------------------------------------
       1. toMap
       ------------------------------------------------------------- */
    @Test
    public void testToMap_Basic() {
        Instant instant = Instant.ofEpochSecond(10L, 123_456_789);
        Map<String, Object> map = InstantConversions.toMap(instant, utcConverter);
        assertEquals(2, map.size());
        assertEquals(10L, map.get("seconds"));
        assertEquals(123_456_789, map.get("nanos"));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInstant() {
        InstantConversions.toMap(null, utcConverter);
    }

    /* -------------------------------------------------------------
       2. toZonedDateTime
       ------------------------------------------------------------- */
    @Test
    public void testToZonedDateTime_Utc() {
        Instant instant = Instant.ofEpochSecond(1622548800L); // 2021-06-01T12:00:00Z
        ZonedDateTime zdt = InstantConversions.toZonedDateTime(instant, utcConverter);
        assertEquals(ZoneId.of("UTC"), zdt.getZone());
        assertEquals(instant, zdt.toInstant());
    }

    @Test
    public void testToZonedDateTime_SystemDefault() {
        Instant instant = Instant.now();
        ZonedDateTime zdt = InstantConversions.toZonedDateTime(instant, defaultConverter);
        assertEquals(ZoneId.systemDefault(), zdt.getZone());
        assertEquals(instant, zdt.toInstant());
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_NullInstant() {
        InstantConversions.toZonedDateTime(null, utcConverter);
    }

    /* -------------------------------------------------------------
       3. toOffsetDateTime
       ------------------------------------------------------------- */
    @Test
    public void testToOffsetDateTime_Utc() {
        Instant instant = Instant.ofEpochSecond(1622548800L, 500_000_000); // 2021‑06‑01T12:00:00.5Z
        OffsetDateTime odt = InstantConversions.toOffsetDateTime(instant, utcConverter);
        assertEquals(ZoneOffset.UTC, odt.getOffset());
        assertEquals(instant, odt.toInstant());
    }

    @Test
    public void testToOffsetDateTime_SystemDefault() {
        Instant instant = Instant.now();
        OffsetDateTime odt = InstantConversions.toOffsetDateTime(instant, defaultConverter);
        ZoneOffset expected = ZoneOffset.ofTotalSeconds(
                TimeZone.getDefault().getOffset(instant.toEpochMilli()) / 1000);
        assertEquals(expected, odt.getOffset());
        assertEquals(instant, odt.toInstant());
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTime_NullInstant() {
        InstantConversions.toOffsetDateTime(null, utcConverter);
    }

    /* -------------------------------------------------------------
       4. toLong
       ------------------------------------------------------------- */
    @Test
    public void testToLong_Basic() {
        long epochMilli = 1_622_548_801_234L;
        Instant instant = Instant.ofEpochMilli(epochMilli);
        long result = InstantConversions.toLong(instant, utcConverter);
        assertEquals(epochMilli, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInstant() {
        InstantConversions.toLong(null, utcConverter);
    }

    @Test(expected = DateTimeException.class)
    public void testToLong_OutOfRangeInstant() {
        // Instant beyond the range representable by epoch milli (Long.MAX_VALUE ms)
        Instant instant = Instant.ofEpochSecond(Long.MAX_VALUE / 1000 + 1);
        InstantConversions.toLong(instant, utcConverter);
    }

    /* -------------------------------------------------------------
       5. toDouble
       ------------------------------------------------------------- */
    @Test
    public void testToDouble_Precision() {
        Instant instant = Instant.ofEpochSecond(123456789L, 987_654_321);
        double expected = new BigDecimal("123456789.987654321").doubleValue();
        double result = InstantConversions.toDouble(instant, utcConverter);
        assertEquals(expected, result, 0.0);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullInstant() {
        InstantConversions.toDouble(null, utcConverter);
    }

    /* -------------------------------------------------------------
       6. toAtomicLong
       ------------------------------------------------------------- */
    @Test
    public void testToAtomicLong_Basic() {
        long epochMilli = 1_622_548_801_234L;
        Instant instant = Instant.ofEpochMilli(epochMilli);
        AtomicLong atomic = InstantConversions.toAtomicLong(instant, utcConverter);
        assertEquals(epochMilli, atomic.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullInstant() {
        InstantConversions.toAtomicLong(null, utcConverter);
    }

    /* -------------------------------------------------------------
       7. toTimestamp
       ------------------------------------------------------------- */
    @Test
    public void testToTimestamp_Basic() {
        Instant instant = Instant.now();
        Timestamp ts = InstantConversions.toTimestamp(instant, utcConverter);
        assertEquals(Timestamp.from(instant), ts);
    }

    @Test(expected = NullPointerException.class)
    public void testToTimestamp_NullInstant() {
        InstantConversions.toTimestamp(null, utcConverter);
    }

    /* -------------------------------------------------------------
       8. toSqlDate
       ------------------------------------------------------------- */
    @Test
    public void testToSqlDate_Basic() {
        long epochMilli = 1_600_000_000_000L;
        Instant instant = Instant.ofEpochMilli(epochMilli);
        java.sql.Date sqlDate = InstantConversions.toSqlDate(instant, utcConverter);
        assertEquals(new java.sql.Date(epochMilli), sqlDate);
    }

    @Test(expected = NullPointerException.class)
    public void testToSqlDate_NullInstant() {
        InstantConversions.toSqlDate(null, utcConverter);
    }

    /* -------------------------------------------------------------
       9. toDate
       ------------------------------------------------------------- */
    @Test
    public void testToDate_Basic() {
        long epochMilli = 1_600_000_000_000L;
        Instant instant = Instant.ofEpochMilli(epochMilli);
        Date date = InstantConversions.toDate(instant, utcConverter);
        assertEquals(new Date(epochMilli), date);
    }

    @Test(expected = NullPointerException.class)
    public void testToDate_NullInstant() {
        InstantConversions.toDate(null, utcConverter);
    }

    /* -------------------------------------------------------------
       10. toCalendar
       ------------------------------------------------------------- */
    @Test
    public void testToCalendar_Basic() {
        long epochMilli = 1_600_000_000_123L;
        Instant instant = Instant.ofEpochMilli(epochMilli);
        Calendar cal = InstantConversions.toCalendar(instant, utcConverter);
        assertEquals(epochMilli, cal.getTimeInMillis());
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendar_NullInstant() {
        InstantConversions.toCalendar(null, utcConverter);
    }

    /* -------------------------------------------------------------
       11. toBigInteger
       ------------------------------------------------------------- */
    @Test
    public void testToBigInteger_Basic() {
        Instant instant = Instant.ofEpochSecond(5L, 250_000_000);
        BigInteger expected = BigInteger.valueOf(5L).multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(250_000_000L));
        assertEquals(expected, InstantConversions.toBigInteger(instant, utcConverter));
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullInstant() {
        InstantConversions.toBigInteger(null, utcConverter);
    }

    /* -------------------------------------------------------------
       12. toBigDecimal
       ------------------------------------------------------------- */
    @Test
    public void testToBigDecimal_Basic() {
        Instant instant = Instant.ofEpochSecond(42L, 123_456_789);
        BigDecimal expected = new BigDecimal("42.123456789");
        assertEquals(0, expected.compareTo(InstantConversions.toBigDecimal(instant, utcConverter)));
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullInstant() {
        InstantConversions.toBigDecimal(null, utcConverter);
    }

    /* -------------------------------------------------------------
       13. toLocalDateTime
       ------------------------------------------------------------- */
    @Test
    public void testToLocalDateTime_Utc() {
        Instant instant = Instant.ofEpochSecond(1622548800L); // 2021‑06‑01T12:00:00Z
        LocalDateTime ldt = InstantConversions.toLocalDateTime(instant, utcConverter);
        assertEquals(LocalDateTime.ofInstant(instant, ZoneId.of("UTC")), ldt);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTime_NullInstant() {
        InstantConversions.toLocalDateTime(null, utcConverter);
    }

    /* -------------------------------------------------------------
       14. toLocalDate
       ------------------------------------------------------------- */
    @Test
    public void testToLocalDate_Utc() {
        Instant instant = Instant.ofEpochSecond(1622548800L); // 2021‑06‑01
        LocalDate ld = InstantConversions.toLocalDate(instant, utcConverter);
        assertEquals(LocalDate.ofInstant(instant, ZoneId.of("UTC")), ld);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDate_NullInstant() {
        InstantConversions.toLocalDate(null, utcConverter);
    }

    /* -------------------------------------------------------------
       15. toLocalTime
       ------------------------------------------------------------- */
    @Test
    public void testToLocalTime_Utc() {
        Instant instant = Instant.ofEpochSecond(1622548800L, 500_000_000); // 12:00:00.5
        LocalTime lt = InstantConversions.toLocalTime(instant, utcConverter);
        assertEquals(LocalTime.ofInstant(instant, ZoneId.of("UTC")), lt);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullInstant() {
        InstantConversions.toLocalTime(null, utcConverter);
    }

    /* -------------------------------------------------------------
       16. Edge cases – null source for all methods (looped test)
       ------------------------------------------------------------- */
    @Test
    public void testAllMethods_NullSource() {
        Object nullObj = null;
        // The order matches the static methods in InstantConversions
        try { InstantConversions.toMap(nullObj, utcConverter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { InstantConversions.toZonedDateTime(nullObj, utcConverter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { InstantConversions.toOffsetDateTime(nullObj, utcConverter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { InstantConversions.toLong(nullObj, utcConverter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { InstantConversions.toDouble(nullObj, utcConverter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { InstantConversions.toAtomicLong(nullObj, utcConverter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { InstantConversions.toTimestamp(nullObj, utcConverter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { InstantConversions.toSqlDate(nullObj, utcConverter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { InstantConversions.toDate(nullObj, utcConverter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { InstantConversions.toCalendar(nullObj, utcConverter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { InstantConversions.toBigInteger(nullObj, utcConverter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { InstantConversions.toBigDecimal(nullObj, utcConverter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { InstantConversions.toLocalDateTime(nullObj, utcConverter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { InstantConversions.toLocalDate(nullObj, utcConverter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { InstantConversions.toLocalTime(nullObj, utcConverter); fail("NPE expected"); } catch (NullPointerException ignored) {}
    }
}
