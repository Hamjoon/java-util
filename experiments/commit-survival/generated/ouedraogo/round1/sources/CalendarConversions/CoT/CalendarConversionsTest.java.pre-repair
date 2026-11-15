package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Date;

/**
 * JUnit‑4 test suite for {@link CalendarConversions}.
 * The tests are placed in the same package so they can access the package‑private static methods.
 */
public class CalendarConversionsTest {

    private Calendar utcCalendar;
    private Calendar tzCalendar;
    private Converter converter;

    /** A fixed instant used for all tests: 2022‑03‑15T10:20:30.123Z */
    private static final long FIXED_EPOCH_MILLIS = 1647349230123L;

    @Before
    public void setUp() {
        // Calendar in UTC
        utcCalendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        utcCalendar.clear();
        utcCalendar.setTimeInMillis(FIXED_EPOCH_MILLIS);

        // Calendar in a non‑UTC zone (America/New_York, which is UTC‑5 in winter)
        tzCalendar = new GregorianCalendar(TimeZone.getTimeZone("America/New_York"));
        tzCalendar.clear();
        tzCalendar.setTimeInMillis(FIXED_EPOCH_MILLIS);

        // Converter with default options – the default options contain a TimeZone, which is sufficient for the tests.
        converter = new Converter(new ConverterOptions());
    }

    // ----------------------------------------------------------------------
    // Basic functionality tests
    // ----------------------------------------------------------------------

    @Test
    public void testToZonedDateTime_Utc() {
        ZonedDateTime zdt = CalendarConversions.toZonedDateTime(utcCalendar, converter);
        assertEquals(ZoneId.of("UTC"), zdt.getZone());
        assertEquals(Instant.ofEpochMilli(FIXED_EPOCH_MILLIS), zdt.toInstant());
    }

    @Test
    public void testToZonedDateTime_NonUtc() {
        ZonedDateTime zdt = CalendarConversions.toZonedDateTime(tzCalendar, converter);
        assertEquals(ZoneId.of("America/New_York"), zdt.getZone());
        assertEquals(Instant.ofEpochMilli(FIXED_EPOCH_MILLIS), zdt.toInstant());
    }

    @Test
    public void testToLong() {
        Long epoch = CalendarConversions.toLong(utcCalendar, converter);
        assertEquals(FIXED_EPOCH_MILLIS, epoch.longValue());
    }

    @Test
    public void testToDouble() {
        double seconds = CalendarConversions.toDouble(utcCalendar, converter);
        assertEquals(FIXED_EPOCH_MILLIS / 1000.0, seconds, 0.000001);
    }

    @Test
    public void testToDate() {
        Date date = CalendarConversions.toDate(utcCalendar, converter);
        assertEquals(utcCalendar.getTime(), date);
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date sqlDate = CalendarConversions.toSqlDate(utcCalendar, converter);
        assertEquals(new java.sql.Date(FIXED_EPOCH_MILLIS), sqlDate);
    }

    @Test
    public void testToTimestamp() {
        Timestamp ts = CalendarConversions.toTimestamp(utcCalendar, converter);
        assertEquals(FIXED_EPOCH_MILLIS, ts.getTime());
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong al = CalendarConversions.toAtomicLong(utcCalendar, converter);
        assertEquals(FIXED_EPOCH_MILLIS, al.get());
    }

    @Test
    public void testToInstant() {
        Instant instant = CalendarConversions.toInstant(utcCalendar, converter);
        assertEquals(Instant.ofEpochMilli(FIXED_EPOCH_MILLIS), instant);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = CalendarConversions.toLocalDateTime(utcCalendar, converter);
        assertEquals(LocalDateTime.ofInstant(Instant.ofEpochMilli(FIXED_EPOCH_MILLIS), ZoneOffset.UTC), ldt);
    }

    @Test
    public void testToLocalDate() {
        LocalDate ld = CalendarConversions.toLocalDate(utcCalendar, converter);
        assertEquals(LocalDate.ofInstant(Instant.ofEpochMilli(FIXED_EPOCH_MILLIS), ZoneOffset.UTC), ld);
    }

    @Test
    public void testToLocalTime() {
        LocalTime lt = CalendarConversions.toLocalTime(utcCalendar, converter);
        assertEquals(LocalTime.ofInstant(Instant.ofEpochMilli(FIXED_EPOCH_MILLIS), ZoneOffset.UTC), lt);
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal bd = CalendarConversions.toBigDecimal(utcCalendar, converter);
        // Expected value: epochMillis / BigDecimalConversions.GRAND
        BigDecimal expected = new BigDecimal(FIXED_EPOCH_MILLIS).divide(BigDecimalConversions.GRAND);
        assertEquals(0, expected.compareTo(bd));
    }

    @Test
    public void testToBigInteger() {
        BigInteger bi = CalendarConversions.toBigInteger(utcCalendar, converter);
        BigInteger expected = BigInteger.valueOf(FIXED_EPOCH_MILLIS * 1_000_000L);
        assertEquals(expected, bi);
    }

    @Test
    public void testClone() {
        Calendar cloned = CalendarConversions.clone(utcCalendar, converter);
        assertNotSame(utcCalendar, cloned);
        assertEquals(utcCalendar.getTimeInMillis(), cloned.getTimeInMillis());
        assertEquals(utcCalendar.getTimeZone(), cloned.getTimeZone());
    }

    @Test
    public void testCreate() {
        Calendar created = CalendarConversions.create(FIXED_EPOCH_MILLIS, converter);
        assertEquals(FIXED_EPOCH_MILLIS, created.getTimeInMillis());
        // The created calendar should use the time zone from the converter options (system default if none set)
        assertEquals(converter.getOptions().getTimeZone(), created.getTimeZone());
    }

    @Test
    public void testToString() {
        String s = CalendarConversions.toString(utcCalendar, converter);
        // The library’s DateConversions.toString is the canonical representation.
        String expected = DateConversions.toString(utcCalendar.getTime(), converter);
        assertEquals(expected, s);
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime odt = CalendarConversions.toOffsetDateTime(utcCalendar, converter);
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(utcCalendar.getTimeZone().getOffset(FIXED_EPOCH_MILLIS) / 1000);
        OffsetDateTime expected = Instant.ofEpochMilli(FIXED_EPOCH_MILLIS).atOffset(expectedOffset);
        assertEquals(expected, odt);
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = CalendarConversions.toMap(utcCalendar, converter);
        assertEquals(4, map.size());

        // DATE entry – month is 1‑based in the map
        String expectedDate = LocalDate.of(
                utcCalendar.get(Calendar.YEAR),
                utcCalendar.get(Calendar.MONTH) + 1,
                utcCalendar.get(Calendar.DAY_OF_MONTH)
        ).toString();
        assertEquals(expectedDate, map.get(MapConversions.DATE));

        // TIME entry – nanosecond part derived from MILLISECOND field
        String expectedTime = LocalTime.of(
                utcCalendar.get(Calendar.HOUR_OF_DAY),
                utcCalendar.get(Calendar.MINUTE),
                utcCalendar.get(Calendar.SECOND),
                utcCalendar.get(Calendar.MILLISECOND) * 1_000_000
        ).toString();
        assertEquals(expectedTime, map.get(MapConversions.TIME));

        assertEquals(utcCalendar.getTimeZone().toZoneId().toString(), map.get(MapConversions.ZONE));
        assertEquals(FIXED_EPOCH_MILLIS, map.get(MapConversions.EPOCH_MILLIS));
    }

    // ----------------------------------------------------------------------
    // Edge‑case / exception tests
    // ----------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_Null() {
        CalendarConversions.toZonedDateTime(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToZonedDateTime_WrongType() {
        CalendarConversions.toZonedDateTime("not a calendar", converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_Null() {
        CalendarConversions.toLong(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_WrongType() {
        CalendarConversions.toLong(Integer.valueOf(5), converter);
    }

    @Test
    public void testCreate_NegativeEpoch() {
        long negativeEpoch = -123456789L; // before 1970‑01‑01
        Calendar cal = CalendarConversions.create(negativeEpoch, converter);
        assertEquals(negativeEpoch, cal.getTimeInMillis());
    }

    @Test
    public void testClone_Independence() {
        Calendar original = (Calendar) utcCalendar.clone();
        Calendar cloned = CalendarConversions.clone(original, converter);
        // Mutate the clone and verify original stays unchanged
        cloned.add(Calendar.DAY_OF_MONTH, 1);
        assertNotEquals(original.getTimeInMillis(), cloned.getTimeInMillis());
    }

    @Test(expected = NullPointerException.class)
    public void testCreate_NullConverter() {
        CalendarConversions.create(FIXED_EPOCH_MILLIS, null);
    }

    @Test
    public void testToMap_MonthOffset() {
        // Verify that month value is correctly incremented (Calendar.MONTH is zero‑based)
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(2021, Calendar.FEBRUARY, 1); // month = 1 (Feb)
        Map<String, Object> map = CalendarConversions.toMap(cal, converter);
        assertEquals("2021-02-01", map.get(MapConversions.DATE));
    }
}
