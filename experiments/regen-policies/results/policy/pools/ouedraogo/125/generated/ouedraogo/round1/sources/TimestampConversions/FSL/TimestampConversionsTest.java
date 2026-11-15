package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactMap;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import static org.junit.Assert.*;

public class TimestampConversionsTest {

    /** Helper to create a Converter with UTC zone (deterministic for tests). */
    private static Converter utcConverter() {
        // The ConverterOptions class is not part of the public API shown,
        // but the default constructor creates an options object that uses
        // the system default zone.  For deterministic results we set the
        // JVM default zone to UTC for the duration of the test.
        // This avoids any reflection or mocking.
        ZoneId previous = ZoneId.systemDefault();
        try {
            // force UTC for the test thread
            System.setProperty("user.timezone", "UTC");
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            return new Converter(new ConverterOptions());
        } finally {
            // restore original zone after construction
            System.setProperty("user.timezone", previous.getId());
            TimeZone.setDefault(TimeZone.getTimeZone(previous));
        }
    }

    @Test
    public void testToLong() {
        Timestamp ts = new Timestamp(1_600_000_000_000L); // 2020-09-13T12:26:40Z
        Converter conv = utcConverter();

        long result = TimestampConversions.toLong(ts, conv);
        assertEquals(ts.getTime(), result);
    }

    @Test
    public void testToDate() {
        Timestamp ts = new Timestamp(1_600_000_000_123L);
        ts.setNanos(123_456_789);
        Converter conv = utcConverter();

        Date result = TimestampConversions.toDate(ts, conv);
        assertEquals(Date.from(ts.toInstant()), result);
    }

    @Test
    public void testToSqlDate() {
        Timestamp ts = new Timestamp(1_600_000_000_000L);
        Converter conv = utcConverter();

        java.sql.Date result = TimestampConversions.toSqlDate(ts, conv);
        assertEquals(new java.sql.Date(ts.getTime()), result);
    }

    @Test
    public void testToCalendar() {
        Timestamp ts = new Timestamp(1_600_000_000_000L);
        ts.setNanos(0);
        Converter conv = utcConverter();

        Calendar cal = TimestampConversions.toCalendar(ts, conv);
        assertEquals(ts.getTime(), cal.getTimeInMillis());
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
    }

    @Test
    public void testToLocalDateTime() {
        Timestamp ts = new Timestamp(1_600_000_000_000L);
        ts.setNanos(0);
        Converter conv = utcConverter();

        LocalDateTime ldt = TimestampConversions.toLocalDateTime(ts, conv);
        LocalDateTime expected = ts.toInstant()
                .atZone(conv.getOptions().getZoneId())
                .toLocalDateTime();
        assertEquals(expected, ldt);
    }

    @Test
    public void testToOffsetDateTime() {
        Timestamp ts = new Timestamp(1_600_000_000_000L);
        ts.setNanos(0);
        Converter conv = utcConverter();

        OffsetDateTime odt = TimestampConversions.toOffsetDateTime(ts, conv);
        OffsetDateTime expected = ZonedDateTime.ofInstant(ts.toInstant(),
                conv.getOptions().getZoneId()).toOffsetDateTime();
        assertEquals(expected, odt);
    }

    @Test
    public void testToDuration() {
        Timestamp ts = new Timestamp(1_600_000_000_123L);
        ts.setNanos(987_654_321);
        Converter conv = utcConverter();

        Duration duration = TimestampConversions.toDuration(ts, conv);
        Duration expected = Duration.between(Instant.EPOCH, ts.toInstant());
        assertEquals(expected, duration);
    }

    @Test
    public void testToDouble() {
        Timestamp ts = new Timestamp(1_600_000_000_123L);
        ts.setNanos(500_000_000); // .5 second
        Converter conv = utcConverter();

        double d = TimestampConversions.toDouble(ts, conv);
        double expected = BigDecimalConversions.secondsAndNanosToDouble(
                ts.toInstant().getEpochSecond(),
                ts.toInstant().getNano()).doubleValue();
        assertEquals(expected, d, 0.0);
    }

    @Test
    public void testToBigDecimal() {
        Timestamp ts = new Timestamp(1_600_000_000_123L);
        ts.setNanos(250_000_000);
        Converter conv = utcConverter();

        BigDecimal bd = TimestampConversions.toBigDecimal(ts, conv);
        // Re‑create the expected value using the same algorithm as InstantConversions
        Instant instant = ts.toInstant();
        BigDecimal expected = BigDecimal.valueOf(instant.getEpochSecond())
                .add(BigDecimal.valueOf(instant.getNano(), 9));
        assertEquals(0, expected.compareTo(bd));
    }

    @Test
    public void testToBigInteger() {
        Timestamp ts = new Timestamp(1_600_000_000_123L);
        ts.setNanos(0);
        Converter conv = utcConverter();

        BigInteger bi = TimestampConversions.toBigInteger(ts, conv);
        // InstantConversions likely returns the epoch seconds as a BigInteger
        BigInteger expected = BigInteger.valueOf(ts.toInstant().getEpochSecond());
        assertEquals(expected, bi);
    }

    @Test
    public void testToStringWholeSeconds() {
        Timestamp ts = new Timestamp(1_600_000_000_000L); // exact second, nanos = 0
        Converter conv = utcConverter();

        String result = TimestampConversions.toString(ts, conv);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
                .withZone(conv.getOptions().getZoneId());
        String expected = fmt.format(ts.toInstant());
        assertEquals(expected, result);
    }

    @Test
    public void testToStringMilliseconds() {
        Timestamp ts = new Timestamp(1_600_000_000_123L); // millis part non‑zero, nanos = 0
        ts.setNanos(123_000_000); // exactly 123 ms
        Converter conv = utcConverter();

        String result = TimestampConversions.toString(ts, conv);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                .withZone(conv.getOptions().getZoneId());
        String expected = fmt.format(ts.toInstant());
        assertEquals(expected, result);
    }

    @Test
    public void testToStringNanoseconds() {
        Timestamp ts = new Timestamp(1_600_000_000_123L);
        ts.setNanos(123_456_789); // not a clean millisecond
        Converter conv = utcConverter();

        String result = TimestampConversions.toString(ts, conv);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSXXX")
                .withZone(conv.getOptions().getZoneId());
        String expected = fmt.format(ts.toInstant());
        assertEquals(expected, result);
    }

    @Test
    public void testToMap() {
        Timestamp ts = new Timestamp(1_600_000_000_123L);
        ts.setNanos(987_654_321);
        Converter conv = utcConverter();

        Map<String, Object> map = TimestampConversions.toMap(ts, conv);

        OffsetDateTime odt = TimestampConversions.toOffsetDateTime(ts, conv);
        assertEquals(odt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(odt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(conv.getOptions().getZoneId().toString(), map.get(MapConversions.ZONE));
        assertEquals(ts.getTime(), map.get(MapConversions.EPOCH_MILLIS));
        assertEquals(odt.getNano(), map.get(MapConversions.NANOS));

        // Verify map type – CompactMap preserves insertion order, but we only need content checks
        assertTrue(map instanceof CompactMap);
    }
}
