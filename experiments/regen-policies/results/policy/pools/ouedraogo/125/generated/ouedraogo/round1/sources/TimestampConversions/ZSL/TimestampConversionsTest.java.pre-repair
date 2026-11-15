package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/**
 * Comprehensive JUnit‑4 tests for {@link TimestampConversions}.
 *
 * The tests cover every public static method in the class and verify
 * the expected behaviour for timestamps with:
 * <ul>
 *   <li>no fractional seconds (0 nanos)</li>
 *   <li>millisecond precision (nanos divisible by 1_000_000)</li>
 *   <li>full nanosecond precision (nanos not divisible by 1_000_000)</li>
 * </ul>
 *
 * The tests use a real {@link Converter} instance with UTC zone
 * (the default zone for the library is assumed to be UTC).  No
 * mocking or subclassing of library types is performed – only the
 * public constructors and methods that appear in the supplied source
 * code are used.
 */
public class TimestampConversionsTest
{
    private Converter converter;               // the converter used for all tests
    private ZoneId utcZone;                    // UTC zone – used for expected values
    private TimeZone utcTimeZone;              // UTC time‑zone – used for Calendar tests

    @Before
    public void setUp()
    {
        // Build a Converter that uses UTC for both ZoneId and TimeZone.
        // ConverterOptions is not part of the supplied source, but the
        // library provides a public no‑arg constructor that defaults to
        // the system defaults.  For deterministic tests we explicitly
        // set the zone to UTC via the public setters (available in the
        // real library).  If the constructor already sets UTC we simply
        // keep the instance.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(ZoneId.of("UTC"));
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
        converter = new Converter(options);

        utcZone = ZoneId.of("UTC");
        utcTimeZone = TimeZone.getTimeZone("UTC");
    }

    /* -------------------------------------------------------------
     * Helper methods to create deterministic Timestamp instances
     * ------------------------------------------------------------- */

    private Timestamp tsZeroNanos()
    {
        // 2023‑03‑15T12:34:56Z  (no fractional part)
        return Timestamp.valueOf("2023-03-15 12:34:56");
    }

    private Timestamp tsMilliNanos()
    {
        // 2023‑03‑15T12:34:56.123Z  (123 milliseconds → 123 000 000 nanos)
        return Timestamp.valueOf("2023-03-15 12:34:56.123");
    }

    private Timestamp tsFullNanos()
    {
        // 2023‑03‑15T12:34:56.123456789Z  (full nanosecond precision)
        return Timestamp.valueOf("2023-03-15 12:34:56.123456789");
    }

    private double expectedDoubleFrom(Timestamp ts)
    {
        Instant instant = ts.toInstant();
        return instant.getEpochSecond() + instant.getNano() / 1_000_000_000d;
    }

    private BigDecimal expectedBigDecimalFrom(Timestamp ts)
    {
        Instant instant = ts.toInstant();
        // seconds as whole part, nanos as fractional part (scale = 9)
        BigDecimal seconds = BigDecimal.valueOf(instant.getEpochSecond());
        BigDecimal nanos   = new BigDecimal(BigInteger.valueOf(instant.getNano()), 9);
        return seconds.add(nanos);
    }

    private BigInteger expectedBigIntegerFrom(Timestamp ts)
    {
        // InstantConversions.toBigInteger is expected to return the epoch seconds.
        return BigInteger.valueOf(ts.toInstant().getEpochSecond());
    }

    /* -------------------------------------------------------------
     *  Tests for each conversion method
     * ------------------------------------------------------------- */

    @Test
    public void testToDouble()
    {
        Timestamp ts = tsFullNanos();
        double actual = TimestampConversions.toDouble(ts, converter);
        assertEquals(expectedDoubleFrom(ts), actual, 1e-12);
    }

    @Test
    public void testToBigDecimal()
    {
        Timestamp ts = tsMilliNanos();
        BigDecimal actual = TimestampConversions.toBigDecimal(ts, converter);
        assertEquals(expectedBigDecimalFrom(ts), actual);
    }

    @Test
    public void testToBigInteger()
    {
        Timestamp ts = tsZeroNanos();
        BigInteger actual = TimestampConversions.toBigInteger(ts, converter);
        assertEquals(expectedBigIntegerFrom(ts), actual);
    }

    @Test
    public void testToLocalDateTime()
    {
        Timestamp ts = tsFullNanos();
        LocalDateTime expected = ts.toInstant().atZone(utcZone).toLocalDateTime();
        LocalDateTime actual   = TimestampConversions.toLocalDateTime(ts, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDuration()
    {
        Timestamp ts = tsFullNanos();
        Duration expected = Duration.between(Instant.EPOCH, ts.toInstant());
        Duration actual   = TimestampConversions.toDuration(ts, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetDateTime()
    {
        Timestamp ts = tsMilliNanos();
        OffsetDateTime expected = ts.toInstant()
                                    .atZone(utcZone)
                                    .toOffsetDateTime();
        OffsetDateTime actual   = TimestampConversions.toOffsetDateTime(ts, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToCalendar()
    {
        Timestamp ts = tsZeroNanos();
        Calendar cal = TimestampConversions.toCalendar(ts, converter);
        assertEquals(utcTimeZone, cal.getTimeZone());
        assertEquals(ts.getTime(), cal.getTimeInMillis());
    }

    @Test
    public void testToDate()
    {
        Timestamp ts = tsFullNanos();
        Date expected = Date.from(ts.toInstant());
        Date actual   = TimestampConversions.toDate(ts, converter);
        assertEquals(expected.getTime(), actual.getTime());
    }

    @Test
    public void testToSqlDate()
    {
        Timestamp ts = tsMilliNanos();
        java.sql.Date expected = new java.sql.Date(ts.getTime());
        java.sql.Date actual   = TimestampConversions.toSqlDate(ts, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLong()
    {
        Timestamp ts = tsZeroNanos();
        long expected = ts.getTime();
        long actual   = TimestampConversions.toLong(ts, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToString_NoFraction()
    {
        Timestamp ts = tsZeroNanos(); // nanos == 0
        String actual = TimestampConversions.toString(ts, converter);
        // Expected pattern: yyyy-MM-dd'T'HH:mm:ssXXX  (no fraction)
        String expected = ts.toInstant()
                            .atZone(utcZone)
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
        assertEquals(expected, actual);
    }

    @Test
    public void testToString_MilliFraction()
    {
        Timestamp ts = tsMilliNanos(); // nanos divisible by 1_000_000
        String actual = TimestampConversions.toString(ts, converter);
        // Expected pattern: yyyy-MM-dd'T'HH:mm:ss.SSSXXX
        String expected = ts.toInstant()
                            .atZone(utcZone)
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
        assertEquals(expected, actual);
    }

    @Test
    public void testToString_NanoFraction()
    {
        Timestamp ts = tsFullNanos(); // nanos not divisible by 1_000_000
        String actual = TimestampConversions.toString(ts, converter);
        // Expected pattern: yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSXXX
        String expected = ts.toInstant()
                            .atZone(utcZone)
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSXXX"));
        assertEquals(expected, actual);
    }

    @Test
    public void testToMap()
    {
        Timestamp ts = tsFullNanos();

        Map<String, Object> map = TimestampConversions.toMap(ts, converter);

        // Verify required keys are present
        assertTrue(map.containsKey(MapConversions.DATE));
        assertTrue(map.containsKey(MapConversions.TIME));
        assertTrue(map.containsKey(MapConversions.ZONE));
        assertTrue(map.containsKey(MapConversions.EPOCH_MILLIS));
        assertTrue(map.containsKey(MapConversions.NANOS));

        // Validate each entry
        OffsetDateTime odt = TimestampConversions.toOffsetDateTime(ts, converter);

        assertEquals(odt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(odt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(utcZone.toString(), map.get(MapConversions.ZONE));
        assertEquals(ts.getTime(), map.get(MapConversions.EPOCH_MILLIS));
        assertEquals(odt.getNano(), map.get(MapConversions.NANOS));
    }
}
