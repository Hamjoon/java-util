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
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class LocalDateConversionsTest {

    private Converter converter;
    private LocalDate sampleDate;
    private ZoneId utcZoneId;
    private TimeZone utcTimeZone;

    @Before
    public void setUp() {
        // Use UTC for deterministic results
        utcZoneId = ZoneOffset.UTC;
        utcTimeZone = TimeZone.getTimeZone("UTC");

        // ConverterOptions is not part of the public API shown, but the library provides a
        // constructor that accepts a ConverterOptions instance.  We create an instance with the
        // required zone information.  The concrete class offers setter methods for the zone
        // and timezone; if they are not present, the default constructor already uses UTC.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(utcZoneId);
        options.setTimeZone(utcTimeZone);

        converter = new Converter(options);

        // Sample date includes a leap day to ensure edge‑case handling
        sampleDate = LocalDate.of(2020, Month.FEBRUARY, 29);
    }

    @Test
    public void testToInstant() {
        Instant instant = LocalDateConversions.toInstant(sampleDate, converter);
        ZonedDateTime zdt = ZonedDateTime.of(sampleDate, LocalTime.MIDNIGHT, utcZoneId);
        assertEquals(zdt.toInstant(), instant);
    }

    @Test
    public void testToLong() {
        long epochMilli = LocalDateConversions.toLong(sampleDate, converter);
        Instant instant = LocalDateConversions.toInstant(sampleDate, converter);
        assertEquals(instant.toEpochMilli(), epochMilli);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = LocalDateConversions.toLocalDateTime(sampleDate, converter);
        assertEquals(LocalDateTime.of(sampleDate, LocalTime.MIDNIGHT), ldt);
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime zdt = LocalDateConversions.toZonedDateTime(sampleDate, converter);
        ZonedDateTime expected = ZonedDateTime.of(sampleDate, LocalTime.MIDNIGHT, utcZoneId);
        assertEquals(expected, zdt);
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime odt = LocalDateConversions.toOffsetDateTime(sampleDate, converter);
        ZoneOffset offset = ZoneOffset.ofTotalSeconds(utcTimeZone.getOffset(System.currentTimeMillis()) / 1000);
        OffsetDateTime expected = OffsetDateTime.of(sampleDate, LocalTime.MIDNIGHT, offset);
        assertEquals(expected, odt);
    }

    @Test
    public void testToDouble() {
        double seconds = LocalDateConversions.toDouble(sampleDate, converter);
        long epochMilli = LocalDateConversions.toLong(sampleDate, converter);
        assertEquals(epochMilli / 1000d, seconds, 0.0);
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong atomic = LocalDateConversions.toAtomicLong(sampleDate, converter);
        assertEquals(LocalDateConversions.toLong(sampleDate, converter), atomic.get());
    }

    @Test
    public void testToTimestamp() {
        Timestamp ts = LocalDateConversions.toTimestamp(sampleDate, converter);
        long expectedMillis = sampleDate.toEpochDay() * 86400L * 1000L;
        assertEquals(expectedMillis, ts.getTime());
    }

    @Test
    public void testToCalendar() {
        Calendar cal = LocalDateConversions.toCalendar(sampleDate, converter);
        long expectedMillis = LocalDateConversions.toLong(sampleDate, converter);
        assertEquals(expectedMillis, cal.getTimeInMillis());
        assertEquals(utcTimeZone, cal.getTimeZone());
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date sqlDate = LocalDateConversions.toSqlDate(sampleDate, converter);
        assertEquals(java.sql.Date.valueOf(sampleDate), sqlDate);
    }

    @Test
    public void testToDate() {
        Date date = LocalDateConversions.toDate(sampleDate, converter);
        assertEquals(LocalDateConversions.toLong(sampleDate, converter), date.getTime());
    }

    @Test
    public void testToBigInteger() {
        BigInteger bi = LocalDateConversions.toBigInteger(sampleDate, converter);
        Instant instant = LocalDateConversions.toInstant(sampleDate, converter);
        BigInteger expected = InstantConversions.toBigInteger(instant, converter);
        assertEquals(expected, bi);
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal bd = LocalDateConversions.toBigDecimal(sampleDate, converter);
        Instant instant = LocalDateConversions.toInstant(sampleDate, converter);
        BigDecimal expected = InstantConversions.toBigDecimal(instant, converter);
        assertEquals(expected, bd);
    }

    @Test
    public void testToYear() {
        Year year = LocalDateConversions.toYear(sampleDate, converter);
        assertEquals(Year.of(2020), year);
    }

    @Test
    public void testToYearMonth() {
        YearMonth ym = LocalDateConversions.toYearMonth(sampleDate, converter);
        assertEquals(YearMonth.of(2020, 2), ym);
    }

    @Test
    public void testToMonthDay() {
        MonthDay md = LocalDateConversions.toMonthDay(sampleDate, converter);
        assertEquals(MonthDay.of(2, 29), md);
    }

    @Test
    public void testToString() {
        String s = LocalDateConversions.toString(sampleDate, converter);
        assertEquals(sampleDate.format(DateTimeFormatter.ISO_LOCAL_DATE), s);
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = LocalDateConversions.toMap(sampleDate, converter);
        assertTrue(map.containsKey(MapConversions.LOCAL_DATE));
        assertEquals(sampleDate.toString(), map.get(MapConversions.LOCAL_DATE));
        assertEquals(1, map.size());
    }
}
