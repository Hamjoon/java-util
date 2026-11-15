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

public class LocalDateConversionsTest {

    private Converter converter;
    private ZoneId zoneId;
    private TimeZone timeZone;

    @Before
    public void setUp() {
        // Use the system default zone & time‑zone – this matches the defaults used by
        // ConverterOptions when no explicit configuration is supplied.
        zoneId = ZoneId.systemDefault();
        timeZone = TimeZone.getDefault();

        // ConverterOptions is part of the library; it has a public no‑arg constructor and
        // returns the system defaults for getZoneId() and getTimeZone().
        ConverterOptions options = new ConverterOptions();   // assumes default ctor
        converter = new Converter(options);
    }

    @Test
    public void testToInstant() {
        LocalDate ld = LocalDate.of(2023, 3, 15);
        Instant expected = ZonedDateTime.of(ld, LocalTime.MIDNIGHT, zoneId).toInstant();
        Instant actual = LocalDateConversions.toInstant(ld, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLong() {
        LocalDate ld = LocalDate.of(2021, 12, 31);
        long expected = LocalDateConversions.toInstant(ld, converter).toEpochMilli();
        long actual = LocalDateConversions.toLong(ld, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDate ld = LocalDate.of(2020, 2, 29);
        LocalDateTime expected = ZonedDateTime.of(ld, LocalTime.MIDNIGHT, zoneId).toLocalDateTime();
        LocalDateTime actual = LocalDateConversions.toLocalDateTime(ld, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToZonedDateTime() {
        LocalDate ld = LocalDate.of(1999, 1, 1);
        ZonedDateTime expected = ZonedDateTime.of(ld, LocalTime.MIDNIGHT, zoneId);
        ZonedDateTime actual = LocalDateConversions.toZonedDateTime(ld, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetDateTime() {
        LocalDate ld = LocalDate.of(2000, 6, 30);
        // Compute the offset that the library uses: it asks the TimeZone for the offset
        // at the current instant (System.currentTimeMillis()).
        int offsetSeconds = timeZone.getOffset(System.currentTimeMillis()) / 1000;
        ZoneOffset offset = ZoneOffset.ofTotalSeconds(offsetSeconds);
        OffsetDateTime expected = OffsetDateTime.of(ld, LocalTime.MIDNIGHT, offset);
        OffsetDateTime actual = LocalDateConversions.toOffsetDateTime(ld, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDouble() {
        LocalDate ld = LocalDate.of(1970, 1, 2); // one day after epoch
        double expected = LocalDateConversions.toInstant(ld, converter).toEpochMilli() / 1000d;
        double actual = LocalDateConversions.toDouble(ld, converter);
        assertEquals(expected, actual, 0.000001);
    }

    @Test
    public void testToAtomicLong() {
        LocalDate ld = LocalDate.of(2010, 5, 20);
        long expected = LocalDateConversions.toLong(ld, converter);
        AtomicLong atomic = LocalDateConversions.toAtomicLong(ld, converter);
        assertEquals(expected, atomic.get());
    }

    @Test
    public void testToTimestamp() {
        LocalDate ld = LocalDate.of(2005, 8, 15);
        // Timestamp is built from epochDay * 86400 * 1000 (midnight UTC)
        long expectedMillis = ld.toEpochDay() * 86400L * 1000L;
        Timestamp ts = LocalDateConversions.toTimestamp(ld, converter);
        assertEquals(expectedMillis, ts.getTime());
    }

    @Test
    public void testToCalendar() {
        LocalDate ld = LocalDate.of(2015, 11, 30);
        Calendar cal = LocalDateConversions.toCalendar(ld, converter);
        assertEquals(timeZone, cal.getTimeZone());
        long expectedMillis = LocalDateConversions.toInstant(ld, converter).toEpochMilli();
        assertEquals(expectedMillis, cal.getTimeInMillis());
    }

    @Test
    public void testToSqlDate() {
        LocalDate ld = LocalDate.of(1995, 12, 25);
        java.sql.Date sqlDate = LocalDateConversions.toSqlDate(ld, converter);
        long expected = LocalDateConversions.toLong(ld, converter);
        assertEquals(expected, sqlDate.getTime());
    }

    @Test
    public void testToDate() {
        LocalDate ld = LocalDate.of(1980, 7, 4);
        Date date = LocalDateConversions.toDate(ld, converter);
        long expected = LocalDateConversions.toLong(ld, converter);
        assertEquals(expected, date.getTime());
    }

    @Test
    public void testToBigInteger() {
        LocalDate ld = LocalDate.of(2022, 1, 1);
        // The library’s InstantConversions converts the instant to a BigInteger representing
        // the epoch milli value. We replicate that expectation.
        BigInteger expected = BigInteger.valueOf(LocalDateConversions.toLong(ld, converter));
        BigInteger actual = LocalDateConversions.toBigInteger(ld, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigDecimal() {
        LocalDate ld = LocalDate.of(2022, 1, 1);
        // Expected BigDecimal is the epoch milli as a decimal.
        BigDecimal expected = BigDecimal.valueOf(LocalDateConversions.toLong(ld, converter));
        BigDecimal actual = LocalDateConversions.toBigDecimal(ld, converter);
        assertEquals(0, expected.compareTo(actual));
    }

    @Test
    public void testToString() {
        LocalDate ld = LocalDate.of(2024, 2, 29);
        String expected = ld.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String actual = LocalDateConversions.toString(ld, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToMap() {
        LocalDate ld = LocalDate.of(2024, 3, 31);
        Map<String, Object> map = LocalDateConversions.toMap(ld, converter);
        assertNotNull(map);
        // MapConversions.DATE is defined as "date" in the library – we assert that key/value are present.
        assertTrue(map.containsKey("date"));
        assertEquals(ld.toString(), map.get("date"));
    }
}
