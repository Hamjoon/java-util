package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link LocalDateTimeConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical successful conversions for each static method.</li>
 *   <li>Edge cases – {@code null} input and wrong source type.</li>
 *   <li>Timezone‑specific behavior for {@code toOffsetDateTime} and {@code toCalendar}.</li>
 *   <li>Numeric conversions that delegate to {@link InstantConversions}.</li>
 * </ul>
 *
 * The test suite uses only the public API of the library ( {@link Converter},
 * {@link ConverterOptions}, {@link CompactMap} ) and core JDK classes.
 */
public class LocalDateTimeConversionsTest
{
    /** Fixed point in time used for all tests – 2023‑03‑25T12:34:56.789 */
    private static final LocalDateTime FIXED_LDT = LocalDateTime.of(
            2023, 3, 25,
            12, 34, 56,
            789_000_000);   // nanoseconds

    /** Epoch‑millis representation of {@link #FIXED_LDT} in UTC. */
    private static final long FIXED_EPOCH_MILLIS = 1_679_747_696_789L;

    /** Converter that works in UTC (no offset). */
    private Converter utcConverter;

    /** Converter that works in America/New_York (UTC‑04:00 during DST). */
    private Converter nyConverter;

    @Before
    public void setUp()
    {
        // ----- UTC converter -------------------------------------------------
        ConverterOptions utcOptions = new ConverterOptions();
        utcOptions.setZoneId(ZoneId.of("UTC"));
        utcOptions.setTimeZone(TimeZone.getTimeZone("UTC"));
        utcConverter = new Converter(utcOptions);

        // ----- New York converter (DST, offset –04:00) ------------------------
        ConverterOptions nyOptions = new ConverterOptions();
        nyOptions.setZoneId(ZoneId.of("America/New_York"));
        nyOptions.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        nyConverter = new Converter(nyOptions);
    }

    // ------------------------------------------------------------------------
    //  toZonedDateTime
    // ------------------------------------------------------------------------
    @Test
    public void testToZonedDateTime_Utc()
    {
        ZonedDateTime zdt = LocalDateTimeConversions.toZonedDateTime(FIXED_LDT, utcConverter);
        assertEquals(ZoneId.of("UTC"), zdt.getZone());
        assertEquals(FIXED_LDT, zdt.toLocalDateTime());
        assertEquals(FIXED_EPOCH_MILLIS, zdt.toInstant().toEpochMilli());
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_Null()
    {
        LocalDateTimeConversions.toZonedDateTime(null, utcConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToZonedDateTime_WrongType()
    {
        LocalDateTimeConversions.toZonedDateTime("not a LocalDateTime", utcConverter);
    }

    // ------------------------------------------------------------------------
    //  toOffsetDateTime
    // ------------------------------------------------------------------------
    @Test
    public void testToOffsetDateTime_NewYork()
    {
        OffsetDateTime odt = LocalDateTimeConversions.toOffsetDateTime(FIXED_LDT, nyConverter);
        ZoneOffset expectedOffset = ZoneOffset.ofHours(-4); // DST offset for 2023‑03‑25
        assertEquals(expectedOffset, odt.getOffset());
        assertEquals(FIXED_LDT, odt.toLocalDateTime());
    }

    // ------------------------------------------------------------------------
    //  toInstant
    // ------------------------------------------------------------------------
    @Test
    public void testToInstant_Utc()
    {
        Instant instant = LocalDateTimeConversions.toInstant(FIXED_LDT, utcConverter);
        assertEquals(FIXED_EPOCH_MILLIS, instant.toEpochMilli());
    }

    // ------------------------------------------------------------------------
    //  toLong
    // ------------------------------------------------------------------------
    @Test
    public void testToLong_Utc()
    {
        long epoch = LocalDateTimeConversions.toLong(FIXED_LDT, utcConverter);
        assertEquals(FIXED_EPOCH_MILLIS, epoch);
    }

    // ------------------------------------------------------------------------
    //  toDouble
    // ------------------------------------------------------------------------
    @Test
    public void testToDouble_Utc()
    {
        double d = LocalDateTimeConversions.toDouble(FIXED_LDT, utcConverter);
        assertEquals((double) FIXED_EPOCH_MILLIS, d, 0.0);
    }

    // ------------------------------------------------------------------------
    //  toLocalDateTime
    // ------------------------------------------------------------------------
    @Test
    public void testToLocalDateTime_Utc()
    {
        LocalDateTime ldt = LocalDateTimeConversions.toLocalDateTime(FIXED_LDT, utcConverter);
        assertEquals(FIXED_LDT, ldt);
    }

    // ------------------------------------------------------------------------
    //  toLocalDate
    // ------------------------------------------------------------------------
    @Test
    public void testToLocalDate_Utc()
    {
        LocalDate date = LocalDateTimeConversions.toLocalDate(FIXED_LDT, utcConverter);
        assertEquals(LocalDate.of(2023, 3, 25), date);
    }

    // ------------------------------------------------------------------------
    //  toLocalTime
    // ------------------------------------------------------------------------
    @Test
    public void testToLocalTime_Utc()
    {
        LocalTime time = LocalDateTimeConversions.toLocalTime(FIXED_LDT, utcConverter);
        assertEquals(LocalTime.of(12, 34, 56, 789_000_000), time);
    }

    // ------------------------------------------------------------------------
    //  toAtomicLong
    // ------------------------------------------------------------------------
    @Test
    public void testToAtomicLong_Utc()
    {
        AtomicLong atomic = LocalDateTimeConversions.toAtomicLong(FIXED_LDT, utcConverter);
        assertEquals(FIXED_EPOCH_MILLIS, atomic.get());
    }

    // ------------------------------------------------------------------------
    //  toTimestamp
    // ------------------------------------------------------------------------
    @Test
    public void testToTimestamp_Utc()
    {
        Timestamp ts = LocalDateTimeConversions.toTimestamp(FIXED_LDT, utcConverter);
        assertEquals(FIXED_EPOCH_MILLIS, ts.getTime());
    }

    // ------------------------------------------------------------------------
    //  toCalendar
    // ------------------------------------------------------------------------
    @Test
    public void testToCalendar_Utc()
    {
        Calendar cal = LocalDateTimeConversions.toCalendar(FIXED_LDT, utcConverter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(FIXED_EPOCH_MILLIS, cal.getTimeInMillis());
    }

    @Test
    public void testToCalendar_NewYork()
    {
        Calendar cal = LocalDateTimeConversions.toCalendar(FIXED_LDT, nyConverter);
        assertEquals(TimeZone.getTimeZone("America/New_York"), cal.getTimeZone());
        // Calendar works in the supplied timezone, but the underlying instant must be unchanged.
        assertEquals(FIXED_EPOCH_MILLIS, cal.getTimeInMillis());
    }

    // ------------------------------------------------------------------------
    //  toSqlDate
    // ------------------------------------------------------------------------
    @Test
    public void testToSqlDate_Utc()
    {
        java.sql.Date sqlDate = LocalDateTimeConversions.toSqlDate(FIXED_LDT, utcConverter);
        assertEquals(FIXED_EPOCH_MILLIS, sqlDate.getTime());
    }

    // ------------------------------------------------------------------------
    //  toDate
    // ------------------------------------------------------------------------
    @Test
    public void testToDate_Utc()
    {
        Date date = LocalDateTimeConversions.toDate(FIXED_LDT, utcConverter);
        assertEquals(FIXED_EPOCH_MILLIS, date.getTime());
    }

    // ------------------------------------------------------------------------
    //  toBigInteger
    // ------------------------------------------------------------------------
    @Test
    public void testToBigInteger_Utc()
    {
        BigInteger bi = LocalDateTimeConversions.toBigInteger(FIXED_LDT, utcConverter);
        assertEquals(BigInteger.valueOf(FIXED_EPOCH_MILLIS), bi);
    }

    // ------------------------------------------------------------------------
    //  toBigDecimal
    // ------------------------------------------------------------------------
    @Test
    public void testToBigDecimal_Utc()
    {
        BigDecimal bd = LocalDateTimeConversions.toBigDecimal(FIXED_LDT, utcConverter);
        assertEquals(BigDecimal.valueOf(FIXED_EPOCH_MILLIS), bd);
    }

    // ------------------------------------------------------------------------
    //  toString
    // ------------------------------------------------------------------------
    @Test
    public void testToString_Utc()
    {
        String s = LocalDateTimeConversions.toString(FIXED_LDT, utcConverter);
        assertEquals("2023-03-25T12:34:56.789", s);
    }

    // ------------------------------------------------------------------------
    //  toMap
    // ------------------------------------------------------------------------
    @Test
    public void testToMap_Utc()
    {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocalDateTimeConversions.toMap(FIXED_LDT, utcConverter);
        assertEquals(2, map.size());
        assertEquals("2023-03-25", map.get(MapConversions.DATE));
        assertEquals("12:34:56.789", map.get(MapConversions.TIME));
    }

    // ------------------------------------------------------------------------
    //  Error handling – null and wrong type for a selection of methods
    // ------------------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testToInstant_Null()
    {
        LocalDateTimeConversions.toInstant(null, utcConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToInstant_WrongType()
    {
        LocalDateTimeConversions.toInstant(12345, utcConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_Null()
    {
        LocalDateTimeConversions.toString(null, utcConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToString_WrongType()
    {
        LocalDateTimeConversions.toString(Boolean.TRUE, utcConverter);
    }
}
