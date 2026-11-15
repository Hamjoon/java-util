package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
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

public class LocalDateTimeConversionsTest {

    private Converter converter;
    private ZoneId zoneId;
    private TimeZone timeZone;

    /** a sample LocalDateTime used for every test */
    private static final LocalDateTime SAMPLE_LDT = LocalDateTime.of(
            2023, Month.MARCH, 15,
            12, 34, 56, 123_000_000); // 123 ms nanoseconds

    @Before
    public void setUp() {
        // force a deterministic default time‑zone for the whole JVM
        timeZone = TimeZone.getTimeZone("UTC");
        TimeZone.setDefault(timeZone);
        zoneId = ZoneId.of("UTC");

        // The library’s ConverterOptions class is not shown, but it normally
        // picks up the JVM default zone.  Creating a default instance is enough
        // for deterministic behaviour in this test suite.
        ConverterOptions options = new ConverterOptions();   // assumes a public no‑arg ctor
        converter = new Converter(options);
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime zdt = LocalDateTimeConversions.toZonedDateTime(SAMPLE_LDT, converter);
        assertEquals(SAMPLE_LDT, zdt.toLocalDateTime());
        assertEquals(zoneId, zdt.getZone());
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime odt = LocalDateTimeConversions.toOffsetDateTime(SAMPLE_LDT, converter);
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(
                timeZone.getOffset(System.currentTimeMillis()) / 1000);
        assertEquals(SAMPLE_LDT, odt.toLocalDateTime());
        assertEquals(expectedOffset, odt.getOffset());
    }

    @Test
    public void testToInstant() {
        Instant instant = LocalDateTimeConversions.toInstant(SAMPLE_LDT, converter);
        Instant expected = SAMPLE_LDT.atZone(zoneId).toInstant();
        assertEquals(expected, instant);
    }

    @Test
    public void testToLong() {
        long epochMilli = LocalDateTimeConversions.toLong(SAMPLE_LDT, converter);
        long expected = SAMPLE_LDT.atZone(zoneId).toInstant().toEpochMilli();
        assertEquals(expected, epochMilli);
    }

    @Test
    public void testToDouble() {
        double d = LocalDateTimeConversions.toDouble(SAMPLE_LDT, converter);
        // InstantConversions.toDouble is expected to return the epoch milli as a double
        double expected = (double) LocalDateTimeConversions.toLong(SAMPLE_LDT, converter);
        assertEquals(expected, d, 0.0);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = LocalDateTimeConversions.toLocalDateTime(SAMPLE_LDT, converter);
        assertEquals(SAMPLE_LDT, ldt);
    }

    @Test
    public void testToLocalDate() {
        LocalDate date = LocalDateTimeConversions.toLocalDate(SAMPLE_LDT, converter);
        assertEquals(SAMPLE_LDT.toLocalDate(), date);
    }

    @Test
    public void testToLocalTime() {
        LocalTime time = LocalDateTimeConversions.toLocalTime(SAMPLE_LDT, converter);
        assertEquals(SAMPLE_LDT.toLocalTime(), time);
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong atomic = LocalDateTimeConversions.toAtomicLong(SAMPLE_LDT, converter);
        assertEquals(LocalDateTimeConversions.toLong(SAMPLE_LDT, converter), atomic.get());
    }

    @Test
    public void testToTimestamp() {
        Timestamp ts = LocalDateTimeConversions.toTimestamp(SAMPLE_LDT, converter);
        long expectedMillis = SAMPLE_LDT.atZone(zoneId).toInstant().toEpochMilli();
        assertEquals(expectedMillis, ts.getTime());
    }

    @Test
    public void testToCalendar() {
        Calendar cal = LocalDateTimeConversions.toCalendar(SAMPLE_LDT, converter);
        long expectedMillis = SAMPLE_LDT.atZone(zoneId).toInstant().toEpochMilli();
        assertEquals(expectedMillis, cal.getTimeInMillis());
        assertEquals(timeZone, cal.getTimeZone());
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date sqlDate = LocalDateTimeConversions.toSqlDate(SAMPLE_LDT, converter);
        long expected = LocalDateTimeConversions.toLong(SAMPLE_LDT, converter);
        assertEquals(expected, sqlDate.getTime());
    }

    @Test
    public void testToDate() {
        Date date = LocalDateTimeConversions.toDate(SAMPLE_LDT, converter);
        long expected = LocalDateTimeConversions.toLong(SAMPLE_LDT, converter);
        assertEquals(expected, date.getTime());
    }

    @Test
    public void testToBigInteger() {
        BigInteger bi = LocalDateTimeConversions.toBigInteger(SAMPLE_LDT, converter);
        // InstantConversions.toBigInteger is expected to return the epoch milli as a BigInteger
        BigInteger expected = BigInteger.valueOf(LocalDateTimeConversions.toLong(SAMPLE_LDT, converter));
        assertEquals(expected, bi);
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal bd = LocalDateTimeConversions.toBigDecimal(SAMPLE_LDT, converter);
        // Expected value is the epoch milli represented as a BigDecimal
        BigDecimal expected = BigDecimal.valueOf(LocalDateTimeConversions.toLong(SAMPLE_LDT, converter));
        assertEquals(0, expected.compareTo(bd));
    }

    @Test
    public void testToString() {
        String s = LocalDateTimeConversions.toString(SAMPLE_LDT, converter);
        String expected = SAMPLE_LDT.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        assertEquals(expected, s);
    }

    @Test
    public void testToMap() {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocalDateTimeConversions.toMap(SAMPLE_LDT, converter);
        assertEquals(2, map.size());
        assertEquals(SAMPLE_LDT.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(SAMPLE_LDT.toLocalTime().toString(), map.get(MapConversions.TIME));
        // ensure the concrete map implementation is the library’s CompactLinkedMap
        assertTrue(map instanceof CompactLinkedMap);
    }
}
