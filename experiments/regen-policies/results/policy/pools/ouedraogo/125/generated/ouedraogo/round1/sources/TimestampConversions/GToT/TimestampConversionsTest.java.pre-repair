package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class TimestampConversionsTest {

    private Converter converter;
    private ZoneId zoneId;
    private TimeZone timeZone;

    @Before
    public void setUp() {
        // The real library provides a ConverterOptions class.  For the purpose of these
        // tests we rely on the default constructor of ConverterOptions (which is public
        // in the library) and the default zone (system default).  If the library changes
        // the constructor signature the test will need to be updated accordingly.
        converter = new Converter(new ConverterOptions());
        zoneId = converter.getOptions().getZoneId();
        timeZone = converter.getOptions().getTimeZone();
    }

    // -----------------------------------------------------------------------
    // Helper to create Timestamp with specific epoch millis and nanos
    // -----------------------------------------------------------------------
    private Timestamp ts(long epochMilli, int nanos) {
        Timestamp ts = new Timestamp(epochMilli);
        ts.setNanos(nanos);
        return ts;
    }

    // -----------------------------------------------------------------------
    // 1. toDouble
    // -----------------------------------------------------------------------
    @Test
    public void testToDouble_Basic() {
        Timestamp ts = ts(1_600_000_000_000L, 123_456_789); // 2020‑09‑13T12:26:40.123456789Z
        double actual = TimestampConversions.toDouble(ts, converter);
        Duration d = TimestampConversions.toDuration(ts, converter);
        double expected = BigDecimalConversions.secondsAndNanosToDouble(d.getSeconds(), d.getNano()).doubleValue();
        assertEquals(expected, actual, 0.0);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullInput() {
        TimestampConversions.toDouble(null, converter);
    }

    // -----------------------------------------------------------------------
    // 2. toBigDecimal
    // -----------------------------------------------------------------------
    @Test
    public void testToBigDecimal_Basic() {
        Timestamp ts = ts(1_600_000_000_000L, 500_000_000); // .5 seconds
        BigDecimal actual = TimestampConversions.toBigDecimal(ts, converter);
        // The BigDecimal representation must be equal to the double conversion
        // (the library uses the same helper method internally).
        double asDouble = TimestampConversions.toDouble(ts, converter);
        assertEquals(BigDecimal.valueOf(asDouble), actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullInput() {
        TimestampConversions.toBigDecimal(null, converter);
    }

    // -----------------------------------------------------------------------
    // 3. toBigInteger
    // -----------------------------------------------------------------------
    @Test
    public void testToBigInteger_Basic() {
        Timestamp ts = ts(1_600_000_000_000L, 0);
        BigInteger actual = TimestampConversions.toBigInteger(ts, converter);
        // Instant epoch seconds should be the same as the BigInteger value.
        BigInteger expected = BigInteger.valueOf(ts.toInstant().getEpochSecond());
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullInput() {
        TimestampConversions.toBigInteger(null, converter);
    }

    // -----------------------------------------------------------------------
    // 4. toLocalDateTime
    // -----------------------------------------------------------------------
    @Test
    public void testToLocalDateTime_Basic() {
        Timestamp ts = ts(1_600_000_000_000L, 0);
        LocalDateTime actual = TimestampConversions.toLocalDateTime(ts, converter);
        LocalDateTime expected = ts.toInstant().atZone(zoneId).toLocalDateTime();
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTime_NullInput() {
        TimestampConversions.toLocalDateTime(null, converter);
    }

    // -----------------------------------------------------------------------
    // 5. toDuration
    // -----------------------------------------------------------------------
    @Test
    public void testToDuration_Basic() {
        Timestamp ts = ts(1_600_000_000_000L, 250_000_000);
        Duration actual = TimestampConversions.toDuration(ts, converter);
        Duration expected = Duration.between(Instant.EPOCH, ts.toInstant());
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToDuration_NullInput() {
        TimestampConversions.toDuration(null, converter);
    }

    // -----------------------------------------------------------------------
    // 6. toOffsetDateTime
    // -----------------------------------------------------------------------
    @Test
    public void testToOffsetDateTime_Basic() {
        Timestamp ts = ts(1_600_000_000_000L, 0);
        OffsetDateTime actual = TimestampConversions.toOffsetDateTime(ts, converter);
        OffsetDateTime expected = ts.toInstant().atZone(zoneId).toOffsetDateTime();
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTime_NullInput() {
        TimestampConversions.toOffsetDateTime(null, converter);
    }

    // -----------------------------------------------------------------------
    // 7. toCalendar
    // -----------------------------------------------------------------------
    @Test
    public void testToCalendar_Basic() {
        Timestamp ts = ts(1_600_000_000_123L, 0);
        Calendar cal = TimestampConversions.toCalendar(ts, converter);
        assertEquals(timeZone, cal.getTimeZone());
        assertEquals(ts.getTime(), cal.getTimeInMillis());
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendar_NullInput() {
        TimestampConversions.toCalendar(null, converter);
    }

    // -----------------------------------------------------------------------
    // 8. toDate
    // -----------------------------------------------------------------------
    @Test
    public void testToDate_Basic() {
        Timestamp ts = ts(1_600_000_000_000L, 123_456_789);
        Date actual = TimestampConversions.toDate(ts, converter);
        Date expected = Date.from(ts.toInstant());
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToDate_NullInput() {
        TimestampConversions.toDate(null, converter);
    }

    // -----------------------------------------------------------------------
    // 9. toSqlDate
    // -----------------------------------------------------------------------
    @Test
    public void testToSqlDate_Basic() {
        Timestamp ts = ts(1_600_000_000_000L, 0);
        java.sql.Date actual = TimestampConversions.toSqlDate(ts, converter);
        java.sql.Date expected = new java.sql.Date(ts.getTime());
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToSqlDate_NullInput() {
        TimestampConversions.toSqlDate(null, converter);
    }

    // -----------------------------------------------------------------------
    // 10. toLong
    // -----------------------------------------------------------------------
    @Test
    public void testToLong_Basic() {
        Timestamp ts = ts(1_600_000_000_000L, 0);
        long actual = TimestampConversions.toLong(ts, converter);
        assertEquals(ts.getTime(), actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInput() {
        TimestampConversions.toLong(null, converter);
    }

    // -----------------------------------------------------------------------
    // 11. toString
    // -----------------------------------------------------------------------
    @Test
    public void testToString_ZeroNanos() {
        Timestamp ts = ts(1_600_000_000_000L, 0);
        String result = TimestampConversions.toString(ts, converter);
        // pattern = "yyyy-MM-dd'T'HH:mm:ssXXX"
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
                .withZone(zoneId);
        String expected = fmt.format(ts.toInstant());
        assertEquals(expected, result);
    }

    @Test
    public void testToString_MillisNanos() {
        Timestamp ts = ts(1_600_000_000_000L, 123_000_000); // exactly 123 ms
        String result = TimestampConversions.toString(ts, converter);
        // pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                .withZone(zoneId);
        String expected = fmt.format(ts.toInstant());
        assertEquals(expected, result);
    }

    @Test
    public void testToString_NanoPrecision() {
        Timestamp ts = ts(1_600_000_000_000L, 123_456_789);
        String result = TimestampConversions.toString(ts, converter);
        // pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSXXX"
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSXXX")
                .withZone(zoneId);
        String expected = fmt.format(ts.toInstant());
        assertEquals(expected, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullInput() {
        TimestampConversions.toString(null, converter);
    }

    // -----------------------------------------------------------------------
    // 12. toMap
    // -----------------------------------------------------------------------
    @Test
    public void testToMap_Basic() {
        Timestamp ts = ts(1_600_000_000_000L, 987_654_321);
        Map<String, Object> map = TimestampConversions.toMap(ts, converter);

        OffsetDateTime odt = TimestampConversions.toOffsetDateTime(ts, converter);
        assertEquals(odt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(odt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(zoneId.toString(), map.get(MapConversions.ZONE));
        assertEquals(ts.getTime(), map.get(MapConversions.EPOCH_MILLIS));
        assertEquals(odt.getNano(), map.get(MapConversions.NANOS));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        TimestampConversions.toMap(null, converter);
    }

    // -----------------------------------------------------------------------
    // Edge case: Timestamp representing a moment before the Unix epoch
    // -----------------------------------------------------------------------
    @Test
    public void testNegativeTimestamp_Conversions() {
        // 1969‑12‑31T23:59:59.999 UTC (one millisecond before epoch)
        Timestamp ts = ts(-1L, 999_000_000);
        // toDuration should be negative
        Duration d = TimestampConversions.toDuration(ts, converter);
        assertTrue(d.isNegative());

        // toLong must match the raw millis value
        assertEquals(-1L, TimestampConversions.toLong(ts, converter));

        // toString should correctly format the negative instant
        String result = TimestampConversions.toString(ts, converter);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSXXX")
                .withZone(zoneId);
        String expected = fmt.format(ts.toInstant());
        assertEquals(expected, result);
    }
}
