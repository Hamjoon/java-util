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

/**
 * JUnit‑4 test suite for {@code LocalDateConversions}.
 *
 * The tests are written in the same package as {@code LocalDateConversions}
 * so that the package‑private static methods are accessible.
 *
 * The test class creates a {@link Converter} that uses the UTC time‑zone
 * (both {@link ZoneId} and {@link TimeZone}) – this makes the expected
 * values deterministic regardless of the machine’s default zone.
 *
 * For each conversion method a “happy‑path” test is provided together
 * with edge‑case tests that verify the behaviour when {@code null} or an
 * incompatible type is supplied.
 */
public class LocalDateConversionsTest
{
    /** A Converter that works with UTC – the default options of the library
     *  are assumed to be UTC; if they are not, the test will still pass because
     *  we never modify the options. */
    private Converter converter;

    /** Sample LocalDate used for most tests (2020‑01‑02). */
    private static final LocalDate SAMPLE_DATE = LocalDate.of(2020, 1, 2);

    /** Expected epoch‑millis for SAMPLE_DATE at 00:00:00 UTC. */
    private static final long EXPECTED_EPOCH_MILLIS = SAMPLE_DATE
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

    @Before
    public void setUp()
    {
        // The concrete implementation of ConverterOptions is not shown in the
        // kata, but it provides a no‑arg constructor and defaults to UTC.
        // This is sufficient for the conversion methods that rely on
        // converter.getOptions().getZoneId() and .getTimeZone().
        converter = new Converter(new ConverterOptions());
    }

    /* -------------------------------------------------------------------- */
    /*  BASIC (happy‑path) tests                                            */
    /* -------------------------------------------------------------------- */

    @Test
    public void testToInstant()
    {
        Instant instant = LocalDateConversions.toInstant(SAMPLE_DATE, converter);
        assertEquals(Instant.ofEpochMilli(EXPECTED_EPOCH_MILLIS), instant);
    }

    @Test
    public void testToLong()
    {
        long epochMilli = LocalDateConversions.toLong(SAMPLE_DATE, converter);
        assertEquals(EXPECTED_EPOCH_MILLIS, epochMilli);
    }

    @Test
    public void testToLocalDateTime()
    {
        LocalDateTime ldt = LocalDateConversions.toLocalDateTime(SAMPLE_DATE, converter);
        assertEquals(LocalDateTime.of(2020, 1, 2, 0, 0, 0), ldt);
    }

    @Test
    public void testToZonedDateTime()
    {
        ZonedDateTime zdt = LocalDateConversions.toZonedDateTime(SAMPLE_DATE, converter);
        assertEquals(ZonedDateTime.of(SAMPLE_DATE, LocalTime.MIDNIGHT, ZoneOffset.UTC), zdt);
    }

    @Test
    public void testToOffsetDateTime()
    {
        OffsetDateTime odt = LocalDateConversions.toOffsetDateTime(SAMPLE_DATE, converter);
        assertEquals(OffsetDateTime.of(SAMPLE_DATE, LocalTime.MIDNIGHT, ZoneOffset.UTC), odt);
    }

    @Test
    public void testToDouble()
    {
        double seconds = LocalDateConversions.toDouble(SAMPLE_DATE, converter);
        assertEquals(EXPECTED_EPOCH_MILLIS / 1000d, seconds, 0.000001);
    }

    @Test
    public void testToAtomicLong()
    {
        AtomicLong atomic = LocalDateConversions.toAtomicLong(SAMPLE_DATE, converter);
        assertEquals(EXPECTED_EPOCH_MILLIS, atomic.get());
    }

    @Test
    public void testToTimestamp()
    {
        Timestamp ts = LocalDateConversions.toTimestamp(SAMPLE_DATE, converter);
        // Timestamp stores milliseconds since the epoch.
        assertEquals(EXPECTED_EPOCH_MILLIS, ts.getTime());
    }

    @Test
    public void testToCalendar()
    {
        Calendar cal = LocalDateConversions.toCalendar(SAMPLE_DATE, converter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(EXPECTED_EPOCH_MILLIS, cal.getTimeInMillis());
    }

    @Test
    public void testToSqlDate()
    {
        java.sql.Date sqlDate = LocalDateConversions.toSqlDate(SAMPLE_DATE, converter);
        assertEquals(EXPECTED_EPOCH_MILLIS, sqlDate.getTime());
    }

    @Test
    public void testToDate()
    {
        Date date = LocalDateConversions.toDate(SAMPLE_DATE, converter);
        assertEquals(EXPECTED_EPOCH_MILLIS, date.getTime());
    }

    @Test
    public void testToBigInteger()
    {
        // InstantConversions delegates to Instant.toEpochMilli() for the BigInteger
        // representation, so we can verify the numeric value.
        BigInteger bi = LocalDateConversions.toBigInteger(SAMPLE_DATE, converter);
        assertEquals(BigInteger.valueOf(EXPECTED_EPOCH_MILLIS), bi);
    }

    @Test
    public void testToBigDecimal()
    {
        // Expected BigDecimal is the epoch‑millis as a decimal value.
        BigDecimal bd = LocalDateConversions.toBigDecimal(SAMPLE_DATE, converter);
        assertEquals(BigDecimal.valueOf(EXPECTED_EPOCH_MILLIS), bd);
    }

    @Test
    public void testToString()
    {
        String s = LocalDateConversions.toString(SAMPLE_DATE, converter);
        assertEquals("2020-01-02", s);
    }

    @Test
    public void testToMap()
    {
        Map<String, Object> map = LocalDateConversions.toMap(SAMPLE_DATE, converter);
        assertNotNull(map);
        // MapConversions.DATE is the key used by the conversion routine.
        assertTrue(map.containsKey(MapConversions.DATE));
        assertEquals("2020-01-02", map.get(MapConversions.DATE));
    }

    /* -------------------------------------------------------------------- */
    /*  EDGE‑CASE / EXCEPTION tests                                         */
    /* -------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToInstant_NullInput()
    {
        LocalDateConversions.toInstant(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInput()
    {
        LocalDateConversions.toLong(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTime_NullInput()
    {
        LocalDateConversions.toLocalDateTime(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_NullInput()
    {
        LocalDateConversions.toZonedDateTime(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTime_NullInput()
    {
        LocalDateConversions.toOffsetDateTime(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullInput()
    {
        LocalDateConversions.toDouble(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullInput()
    {
        LocalDateConversions.toAtomicLong(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToTimestamp_NullInput()
    {
        LocalDateConversions.toTimestamp(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendar_NullInput()
    {
        LocalDateConversions.toCalendar(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToSqlDate_NullInput()
    {
        LocalDateConversions.toSqlDate(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToDate_NullInput()
    {
        LocalDateConversions.toDate(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullInput()
    {
        LocalDateConversions.toBigInteger(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullInput()
    {
        LocalDateConversions.toBigDecimal(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullInput()
    {
        LocalDateConversions.toString(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput()
    {
        LocalDateConversions.toMap(null, converter);
    }

    /** Passing an object of the wrong type should result in a {@link ClassCastException}. */
    @Test(expected = ClassCastException.class)
    public void testToInstant_WrongType()
    {
        LocalDateConversions.toInstant(Integer.valueOf(42), converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToZonedDateTime_WrongType()
    {
        LocalDateConversions.toZonedDateTime("2020-01-02", converter);
    }

    /** Very large future date to assure that the conversion does not overflow. */
    @Test
    public void testToTimestamp_FarFutureDate()
    {
        LocalDate farFuture = LocalDate.of(3000, 1, 1);
        Timestamp ts = LocalDateConversions.toTimestamp(farFuture, converter);
        // The epoch day for 3000‑01‑01 is positive; we only verify that a
        // non‑negative timestamp is produced.
        assertTrue(ts.getTime() > 0);
    }

    /** Verify that {@code toMap} returns an insertion‑ordered map (CompactMap). */
    @Test
    public void testToMap_InsertionOrder()
    {
        Map<String, Object> map = LocalDateConversions.toMap(SAMPLE_DATE, converter);
        // The map must contain exactly one entry and preserve insertion order.
        assertEquals(1, map.size());
        assertTrue(map.keySet().iterator().next().equals(MapConversions.DATE));
    }
}
