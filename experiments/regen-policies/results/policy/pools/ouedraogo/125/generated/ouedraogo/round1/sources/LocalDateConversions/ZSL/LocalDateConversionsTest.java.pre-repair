package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactMap;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class LocalDateConversionsTest {

    private static final LocalDate TEST_DATE = LocalDate.of(2023, 3, 15);
    private Converter converter;
    private ZoneId zoneId;
    private TimeZone timeZone;

    @Before
    public void setUp() {
        // The library provides a public ConverterOptions class (not shown here).
        // We rely on its default constructor and default behaviour (system zone).
        // If the library allows configuration we would set it here, but the default
        // behaviour is sufficient for deterministic tests.
        ConverterOptions options = new ConverterOptions(); // assumes public no‑arg ctor
        converter = new Converter(options);
        zoneId = converter.getOptions().getZoneId();
        timeZone = converter.getOptions().getTimeZone();
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime expected = ZonedDateTime.of(TEST_DATE, LocalTime.MIDNIGHT, zoneId);
        ZonedDateTime actual = LocalDateConversions.toZonedDateTime(TEST_DATE, converter);
        assertEquals("ZonedDateTime conversion mismatch", expected, actual);
    }

    @Test
    public void testToInstant() {
        ZonedDateTime zdt = LocalDateConversions.toZonedDateTime(TEST_DATE, converter);
        Instant expected = zdt.toInstant();
        Instant actual = LocalDateConversions.toInstant(TEST_DATE, converter);
        assertEquals("Instant conversion mismatch", expected, actual);
    }

    @Test
    public void testToLong() {
        Instant instant = LocalDateConversions.toInstant(TEST_DATE, converter);
        long expected = instant.toEpochMilli();
        long actual = LocalDateConversions.toLong(TEST_DATE, converter);
        assertEquals("Long (epoch milli) conversion mismatch", expected, actual);
    }

    @Test
    public void testToLocalDateTime() {
        ZonedDateTime zdt = LocalDateConversions.toZonedDateTime(TEST_DATE, converter);
        LocalDateTime expected = zdt.toLocalDateTime();
        LocalDateTime actual = LocalDateConversions.toLocalDateTime(TEST_DATE, converter);
        assertEquals("LocalDateTime conversion mismatch", expected, actual);
    }

    @Test
    public void testToOffsetDateTime() {
        // Compute the expected offset using the same algorithm as the implementation
        int offsetSeconds = timeZone.getOffset(System.currentTimeMillis()) / 1000;
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(offsetSeconds);
        OffsetDateTime expected = OffsetDateTime.of(TEST_DATE, LocalTime.MIDNIGHT, expectedOffset);
        OffsetDateTime actual = LocalDateConversions.toOffsetDateTime(TEST_DATE, converter);
        assertEquals("OffsetDateTime conversion mismatch", expected, actual);
    }

    @Test
    public void testToDouble() {
        long epochMilli = LocalDateConversions.toLong(TEST_DATE, converter);
        double expected = epochMilli / 1000d;
        double actual = LocalDateConversions.toDouble(TEST_DATE, converter);
        assertEquals("Double conversion mismatch", expected, actual, 0.0);
    }

    @Test
    public void testToAtomicLong() {
        long expected = LocalDateConversions.toLong(TEST_DATE, converter);
        AtomicLong actual = LocalDateConversions.toAtomicLong(TEST_DATE, converter);
        assertEquals("AtomicLong value mismatch", expected, actual.get());
    }

    @Test
    public void testToTimestamp() {
        long expectedMillis = TEST_DATE.toEpochDay() * 86400L * 1000L;
        Timestamp expected = new Timestamp(expectedMillis);
        Timestamp actual = LocalDateConversions.toTimestamp(TEST_DATE, converter);
        assertEquals("Timestamp millis mismatch", expected.getTime(), actual.getTime());
    }

    @Test
    public void testToCalendar() {
        ZonedDateTime zdt = LocalDateConversions.toZonedDateTime(TEST_DATE, converter);
        Calendar calendar = LocalDateConversions.toCalendar(TEST_DATE, converter);
        assertEquals("Calendar timezone mismatch",
                timeZone.getID(),
                calendar.getTimeZone().getID());
        assertEquals("Calendar instant mismatch",
                zdt.toInstant().toEpochMilli(),
                calendar.getTimeInMillis());
    }

    @Test
    public void testToSqlDate() {
        long expectedMillis = LocalDateConversions.toLong(TEST_DATE, converter);
        java.sql.Date expected = new java.sql.Date(expectedMillis);
        java.sql.Date actual = LocalDateConversions.toSqlDate(TEST_DATE, converter);
        assertEquals("java.sql.Date value mismatch", expected, actual);
    }

    @Test
    public void testToDate() {
        long expectedMillis = LocalDateConversions.toLong(TEST_DATE, converter);
        Date expected = new Date(expectedMillis);
        Date actual = LocalDateConversions.toDate(TEST_DATE, converter);
        assertEquals("java.util.Date value mismatch", expected, actual);
    }

    @Test
    public void testToBigInteger() {
        Instant instant = LocalDateConversions.toInstant(TEST_DATE, converter);
        BigInteger expected = InstantConversions.toBigInteger(instant, converter);
        BigInteger actual = LocalDateConversions.toBigInteger(TEST_DATE, converter);
        assertEquals("BigInteger conversion mismatch", expected, actual);
    }

    @Test
    public void testToBigDecimal() {
        Instant instant = LocalDateConversions.toInstant(TEST_DATE, converter);
        BigDecimal expected = InstantConversions.toBigDecimal(instant, converter);
        BigDecimal actual = LocalDateConversions.toBigDecimal(TEST_DATE, converter);
        assertEquals("BigDecimal conversion mismatch", expected, actual);
    }

    @Test
    public void testToString() {
        String expected = TEST_DATE.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String actual = LocalDateConversions.toString(TEST_DATE, converter);
        assertEquals("String conversion mismatch", expected, actual);
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = LocalDateConversions.toMap(TEST_DATE, converter);
        assertNotNull("Resulting map should not be null", map);
        // The library's MapConversions class defines the key constant; we verify presence.
        assertTrue("Map should contain the date key", map.containsKey(MapConversions.DATE));
        assertEquals("Map value should be ISO date string",
                TEST_DATE.toString(),
                map.get(MapConversions.DATE));
        // Verify the map is a CompactMap (insertion‑order) – size should be 1.
        assertEquals("Map should contain exactly one entry", 1, map.size());
        assertTrue("Map implementation should be CompactMap",
                map instanceof CompactMap);
    }
}
