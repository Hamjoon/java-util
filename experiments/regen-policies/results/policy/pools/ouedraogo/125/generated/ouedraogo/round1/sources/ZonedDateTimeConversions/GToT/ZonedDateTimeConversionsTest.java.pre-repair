package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ZonedDateTimeConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal behaviour of each conversion method.</li>
 *   <li>Edge cases such as null input, extreme dates, and zone changes.</li>
 *   <li>Exception handling – the class does not perform explicit null checks,
 *       therefore a {@link NullPointerException} is expected when a null
 *       source object is supplied.</li>
 * </ul>
 *
 * The {@link Converter} used in the tests is created with a {@link ConverterOptions}
 * instance that supplies a deterministic {@link ZoneId} and {@link java.util.TimeZone}
 * (UTC) so that the results are repeatable regardless of the default JVM zone.
 */
public class ZonedDateTimeConversionsTest
{
    /** A Converter whose options force UTC zone – this is the zone used by the
     *  conversion methods when they need to adjust the source ZonedDateTime. */
    private Converter converter;

    /** Sample ZonedDateTime in a non‑UTC zone used for most tests. */
    private ZonedDateTime sampleZdt;

    /** Same instant as {@link #sampleZdt} but expressed in UTC – useful for
     *  expected values when the conversion method adjusts the zone. */
    private ZonedDateTime sampleZdtUtc;

    @Before
    public void setUp()
    {
        // Build ConverterOptions that return UTC for both ZoneId and TimeZone.
        ConverterOptions options = new ConverterOptions()
        {
            @Override
            public ZoneId getZoneId()
            {
                return ZoneId.of("UTC");
            }

            @Override
            public java.util.TimeZone getTimeZone()
            {
                return java.util.TimeZone.getTimeZone("UTC");
            }
        };
        converter = new Converter(options);

        // Sample instant: 2021‑03‑14T02:30:00‑04:00 (America/New_York, DST gap)
        // This instant is valid in UTC and gives us a known epoch milli.
        sampleZdt = ZonedDateTime.of(
                2021, 3, 14, 2, 30, 0, 0,
                ZoneId.of("America/New_York"));
        // Convert the same instant to UTC for expected values.
        sampleZdtUtc = sampleZdt.withZoneSameInstant(ZoneId.of("UTC"));
    }

    /* --------------------------------------------------------------------- */
    /* Helper methods                                                       */
    /* --------------------------------------------------------------------- */

    private long expectedEpochMilli()
    {
        return sampleZdt.toInstant().toEpochMilli();
    }

    /* --------------------------------------------------------------------- */
    /* Tests for each public static method                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToLong()
    {
        long result = ZonedDateTimeConversions.toLong(sampleZdt, converter);
        assertEquals("Epoch milli should match the instant of the source ZonedDateTime",
                     expectedEpochMilli(), result);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInput()
    {
        ZonedDateTimeConversions.toLong(null, converter);
    }

    @Test
    public void testToDouble()
    {
        double result = ZonedDateTimeConversions.toDouble(sampleZdt, converter);
        // InstantConversions.toDouble simply returns epoch milli as double.
        assertEquals((double) expectedEpochMilli(), result, 0.0);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullInput()
    {
        ZonedDateTimeConversions.toDouble(null, converter);
    }

    @Test
    public void testToInstant()
    {
        Instant result = ZonedDateTimeConversions.toInstant(sampleZdt, converter);
        assertEquals(sampleZdt.toInstant(), result);
    }

    @Test(expected = NullPointerException.class)
    public void testToInstant_NullInput()
    {
        ZonedDateTimeConversions.toInstant(null, converter);
    }

    @Test
    public void testToLocalDateTime()
    {
        // The method adjusts the zone to UTC before extracting LocalDateTime.
        LocalDateTime result = ZonedDateTimeConversions.toLocalDateTime(sampleZdt, converter);
        assertEquals(sampleZdtUtc.toLocalDateTime(), result);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTime_NullInput()
    {
        ZonedDateTimeConversions.toLocalDateTime(null, converter);
    }

    @Test
    public void testToLocalDate()
    {
        LocalDate result = ZonedDateTimeConversions.toLocalDate(sampleZdt, converter);
        assertEquals(sampleZdtUtc.toLocalDate(), result);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDate_NullInput()
    {
        ZonedDateTimeConversions.toLocalDate(null, converter);
    }

    @Test
    public void testToLocalTime()
    {
        LocalTime result = ZonedDateTimeConversions.toLocalTime(sampleZdt, converter);
        assertEquals(sampleZdtUtc.toLocalTime(), result);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullInput()
    {
        ZonedDateTimeConversions.toLocalTime(null, converter);
    }

    @Test
    public void testToOffsetDateTime()
    {
        OffsetDateTime result = ZonedDateTimeConversions.toOffsetDateTime(sampleZdt, converter);
        // No zone conversion is performed for OffsetDateTime.
        assertEquals(sampleZdt.toOffsetDateTime(), result);
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTime_NullInput()
    {
        ZonedDateTimeConversions.toOffsetDateTime(null, converter);
    }

    @Test
    public void testToAtomicLong()
    {
        AtomicLong result = ZonedDateTimeConversions.toAtomicLong(sampleZdt, converter);
        assertEquals(expectedEpochMilli(), result.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullInput()
    {
        ZonedDateTimeConversions.toAtomicLong(null, converter);
    }

    @Test
    public void testToTimestamp()
    {
        Timestamp ts = ZonedDateTimeConversions.toTimestamp(sampleZdt, converter);
        assertEquals(Timestamp.from(sampleZdt.toInstant()), ts);
    }

    @Test(expected = NullPointerException.class)
    public void testToTimestamp_NullInput()
    {
        ZonedDateTimeConversions.toTimestamp(null, converter);
    }

    @Test
    public void testToCalendar()
    {
        Calendar cal = ZonedDateTimeConversions.toCalendar(sampleZdt, converter);
        assertEquals("Calendar time zone must be UTC as defined in ConverterOptions",
                     java.util.TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(expectedEpochMilli(), cal.getTimeInMillis());
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendar_NullInput()
    {
        ZonedDateTimeConversions.toCalendar(null, converter);
    }

    @Test
    public void testToSqlDate()
    {
        java.sql.Date sqlDate = ZonedDateTimeConversions.toSqlDate(sampleZdt, converter);
        assertEquals(new java.sql.Date(expectedEpochMilli()), sqlDate);
    }

    @Test(expected = NullPointerException.class)
    public void testToSqlDate_NullInput()
    {
        ZonedDateTimeConversions.toSqlDate(null, converter);
    }

    @Test
    public void testToDate()
    {
        Date date = ZonedDateTimeConversions.toDate(sampleZdt, converter);
        assertEquals(new Date(expectedEpochMilli()), date);
    }

    @Test(expected = NullPointerException.class)
    public void testToDate_NullInput()
    {
        ZonedDateTimeConversions.toDate(null, converter);
    }

    @Test
    public void testToBigInteger()
    {
        BigInteger bi = ZonedDateTimeConversions.toBigInteger(sampleZdt, converter);
        assertEquals(BigInteger.valueOf(expectedEpochMilli()), bi);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullInput()
    {
        ZonedDateTimeConversions.toBigInteger(null, converter);
    }

    @Test
    public void testToBigDecimal()
    {
        BigDecimal bd = ZonedDateTimeConversions.toBigDecimal(sampleZdt, converter);
        assertEquals(BigDecimal.valueOf(expectedEpochMilli()), bd);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullInput()
    {
        ZonedDateTimeConversions.toBigDecimal(null, converter);
    }

    @Test
    public void testToString()
    {
        String s = ZonedDateTimeConversions.toString(sampleZdt, converter);
        // ISO_DATE_TIME prints the zone offset, not the zone ID.
        assertEquals(sampleZdt.format(java.time.format.DateTimeFormatter.ISO_DATE_TIME), s);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullInput()
    {
        ZonedDateTimeConversions.toString(null, converter);
    }

    @Test
    public void testToMap()
    {
        Map<String, Object> map = ZonedDateTimeConversions.toMap(sampleZdt, converter);
        assertEquals(3, map.size());
        assertEquals(sampleZdt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(sampleZdt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(sampleZdt.getZone().toString(), map.get(MapConversions.ZONE));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput()
    {
        ZonedDateTimeConversions.toMap(null, converter);
    }

    /* --------------------------------------------------------------------- */
    /* Additional edge‑case tests                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testExtremePastDate()
    {
        ZonedDateTime past = ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.MIN_VALUE + 1),
                                                    ZoneId.of("UTC"));
        long epoch = ZonedDateTimeConversions.toLong(past, converter);
        assertEquals(Long.MIN_VALUE + 1, epoch);
    }

    @Test
    public void testExtremeFutureDate()
    {
        ZonedDateTime future = ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.MAX_VALUE - 1),
                                                      ZoneId.of("UTC"));
        long epoch = ZonedDateTimeConversions.toLong(future, converter);
        assertEquals(Long.MAX_VALUE - 1, epoch);
    }

    @Test
    public void testConversionWithSameZone()
    {
        // ConverterOptions already uses UTC; supply a ZonedDateTime already in UTC.
        ZonedDateTime utcNow = ZonedDateTime.now(ZoneId.of("UTC"));
        // LocalDateTime conversion should be identical to the source's LocalDateTime.
        LocalDateTime ldt = ZonedDateTimeConversions.toLocalDateTime(utcNow, converter);
        assertEquals(utcNow.toLocalDateTime(), ldt);
    }
}
