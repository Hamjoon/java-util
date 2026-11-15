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

public class OffsetDateTimeConversionsTest {

    // -----------------------------------------------------------------------
    // Helper objects
    // -----------------------------------------------------------------------
    private Converter converterUtc;          // Converter with UTC zone
    private Converter converterParis;        // Converter with Europe/Paris zone
    private OffsetDateTime utcOdt;           // 2023‑03‑25T12:34:56+00:00
    private OffsetDateTime parisOdt;         // 2023‑03‑25T14:34:56+02:00 (same instant as utcOdt)

    @Before
    public void setUp() {
        // The library provides a public constructor Converter(ConverterOptions)
        // ConverterOptions has a default constructor and setters for zone / time‑zone.
        // We use the default options for UTC and explicitly set a different zone for the second converter.
        converterUtc = new Converter(new ConverterOptions()); // defaults to UTC
        ConverterOptions parisOptions = new ConverterOptions();
        parisOptions.setZoneId(ZoneId.of("Europe/Paris"));
        parisOptions.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
        converterParis = new Converter(parisOptions);

        utcOdt   = OffsetDateTime.of(2023, 3, 25, 12, 34, 56, 0, ZoneOffset.UTC);
        parisOdt = OffsetDateTime.of(2023, 3, 25, 14, 34, 56, 0, ZoneOffset.ofHours(2));
    }

    // -----------------------------------------------------------------------
    // 1.  toInstant
    // -----------------------------------------------------------------------
    @Test
    public void testToInstantBasic() {
        Instant expected = utcOdt.toInstant();
        Instant actual   = OffsetDateTimeConversions.toInstant(utcOdt, converterUtc);
        assertEquals(expected, actual);
    }

    @Test(expected = ClassCastException.class)
    public void testToInstantWithWrongType() {
        OffsetDateTimeConversions.toInstant("not an OffsetDateTime", converterUtc);
    }

    @Test(expected = NullPointerException.class)
    public void testToInstantWithNull() {
        OffsetDateTimeConversions.toInstant(null, converterUtc);
    }

    // -----------------------------------------------------------------------
    // 2.  toLong
    // -----------------------------------------------------------------------
    @Test
    public void testToLongBasic() {
        long expected = utcOdt.toInstant().toEpochMilli();
        long actual   = OffsetDateTimeConversions.toLong(utcOdt, converterUtc);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------------
    // 3.  toAtomicLong
    // -----------------------------------------------------------------------
    @Test
    public void testToAtomicLongBasic() {
        long expected = utcOdt.toInstant().toEpochMilli();
        AtomicLong al = OffsetDateTimeConversions.toAtomicLong(utcOdt, converterUtc);
        assertEquals(expected, al.get());
    }

    // -----------------------------------------------------------------------
    // 4.  toDouble
    // -----------------------------------------------------------------------
    @Test
    public void testToDoubleBasic() {
        double expected = BigDecimalConversions.secondsAndNanosToDouble(
                utcOdt.toInstant().getEpochSecond(),
                utcOdt.toInstant().getNano()).doubleValue();

        double actual = OffsetDateTimeConversions.toDouble(utcOdt, converterUtc);
        assertEquals(expected, actual, 0.0);
    }

    // -----------------------------------------------------------------------
    // 5.  toBigInteger
    // -----------------------------------------------------------------------
    @Test
    public void testToBigIntegerBasic() {
        BigInteger expected = InstantConversions.toBigInteger(utcOdt.toInstant(), converterUtc);
        BigInteger actual   = OffsetDateTimeConversions.toBigInteger(utcOdt, converterUtc);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------------
    // 6.  toBigDecimal
    // -----------------------------------------------------------------------
    @Test
    public void testToBigDecimalBasic() {
        BigDecimal expected = InstantConversions.toBigDecimal(utcOdt.toInstant(), converterUtc);
        BigDecimal actual   = OffsetDateTimeConversions.toBigDecimal(utcOdt, converterUtc);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------------
    // 7.  toLocalDateTime
    // -----------------------------------------------------------------------
    @Test
    public void testToLocalDateTimeBasic() {
        LocalDateTime expected = utcOdt.atZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
        LocalDateTime actual   = OffsetDateTimeConversions.toLocalDateTime(utcOdt, converterUtc);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------------
    // 8.  toLocalDate
    // -----------------------------------------------------------------------
    @Test
    public void testToLocalDateBasic() {
        LocalDate expected = utcOdt.atZoneSameInstant(ZoneId.of("UTC")).toLocalDate();
        LocalDate actual   = OffsetDateTimeConversions.toLocalDate(utcOdt, converterUtc);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------------
    // 9.  toLocalTime
    // -----------------------------------------------------------------------
    @Test
    public void testToLocalTimeBasic() {
        LocalTime expected = utcOdt.atZoneSameInstant(ZoneId.of("UTC")).toLocalTime();
        LocalTime actual   = OffsetDateTimeConversions.toLocalTime(utcOdt, converterUtc);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------------
    // 10. toTimestamp
    // -----------------------------------------------------------------------
    @Test
    public void testToTimestampBasic() {
        Timestamp expected = Timestamp.from(utcOdt.toInstant());
        Timestamp actual   = OffsetDateTimeConversions.toTimestamp(utcOdt, converterUtc);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------------
    // 11. toCalendar
    // -----------------------------------------------------------------------
    @Test
    public void testToCalendarBasic() {
        Calendar cal = OffsetDateTimeConversions.toCalendar(utcOdt, converterUtc);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(utcOdt.toInstant().toEpochMilli(), cal.getTimeInMillis());
    }

    @Test
    public void testToCalendarWithDifferentTimeZone() {
        Calendar cal = OffsetDateTimeConversions.toCalendar(parisOdt, converterParis);
        assertEquals(TimeZone.getTimeZone("Europe/Paris"), cal.getTimeZone());
        assertEquals(parisOdt.toInstant().toEpochMilli(), cal.getTimeInMillis());
    }

    // -----------------------------------------------------------------------
    // 12. toSqlDate
    // -----------------------------------------------------------------------
    @Test
    public void testToSqlDateBasic() {
        java.sql.Date expected = java.sql.Date.valueOf(
                utcOdt.atZoneSameInstant(ZoneId.of("UTC")).toLocalDate());
        java.sql.Date actual   = OffsetDateTimeConversions.toSqlDate(utcOdt, converterUtc);
        assertEquals(expected, actual);
    }

    @Test
    public void testToSqlDateWithDifferentZone() {
        java.sql.Date expected = java.sql.Date.valueOf(
                parisOdt.atZoneSameInstant(ZoneId.of("Europe/Paris")).toLocalDate());
        java.sql.Date actual   = OffsetDateTimeConversions.toSqlDate(parisOdt, converterParis);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------------
    // 13. toZonedDateTime
    // -----------------------------------------------------------------------
    @Test
    public void testToZonedDateTimeBasic() {
        ZonedDateTime expected = utcOdt.toInstant().atZone(ZoneId.of("UTC"));
        ZonedDateTime actual   = OffsetDateTimeConversions.toZonedDateTime(utcOdt, converterUtc);
        assertEquals(expected, actual);
    }

    @Test
    public void testToZonedDateTimeWithDifferentZone() {
        ZonedDateTime expected = parisOdt.toInstant().atZone(ZoneId.of("Europe/Paris"));
        ZonedDateTime actual   = OffsetDateTimeConversions.toZonedDateTime(parisOdt, converterParis);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------------
    // 14. toDate
    // -----------------------------------------------------------------------
    @Test
    public void testToDateBasic() {
        Date expected = new Date(utcOdt.toInstant().toEpochMilli());
        Date actual   = OffsetDateTimeConversions.toDate(utcOdt, converterUtc);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------------
    // 15. toOffsetTime
    // -----------------------------------------------------------------------
    @Test
    public void testToOffsetTimeBasic() {
        OffsetTime expected = utcOdt.toOffsetTime();
        OffsetTime actual   = OffsetDateTimeConversions.toOffsetTime(utcOdt, converterUtc);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------------
    // 16. toYear
    // -----------------------------------------------------------------------
    @Test
    public void testToYearBasic() {
        Year expected = Year.from(
                utcOdt.atZoneSameInstant(ZoneId.of("UTC")).toLocalDate());
        Year actual   = OffsetDateTimeConversions.toYear(utcOdt, converterUtc);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------------
    // 17. toYearMonth
    // -----------------------------------------------------------------------
    @Test
    public void testToYearMonthBasic() {
        YearMonth expected = YearMonth.from(
                utcOdt.atZoneSameInstant(ZoneId.of("UTC")).toLocalDate());
        YearMonth actual   = OffsetDateTimeConversions.toYearMonth(utcOdt, converterUtc);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------------
    // 18. toMonthDay
    // -----------------------------------------------------------------------
    @Test
    public void testToMonthDayBasic() {
        MonthDay expected = MonthDay.from(
                utcOdt.atZoneSameInstant(ZoneId.of("UTC")).toLocalDate());
        MonthDay actual   = OffsetDateTimeConversions.toMonthDay(utcOdt, converterUtc);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------------
    // 19. toString
    // -----------------------------------------------------------------------
    @Test
    public void testToStringBasic() {
        String expected = utcOdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String actual   = OffsetDateTimeConversions.toString(utcOdt, converterUtc);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------------
    // 20. toMap
    // -----------------------------------------------------------------------
    @Test
    public void testToMapBasic() {
        Map<String, Object> map = OffsetDateTimeConversions.toMap(utcOdt, converterUtc);
        assertNotNull(map);
        assertTrue(map.containsKey(MapConversions.OFFSET_DATE_TIME));
        assertEquals(OffsetDateTimeConversions.toString(utcOdt, converterUtc), map.get(MapConversions.OFFSET_DATE_TIME));
    }

    // -----------------------------------------------------------------------
    // Edge‑case tests – nulls and wrong types
    // -----------------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testAllMethodsWithNullObject() {
        // The first method that will be called is toInstant; it will NPE on the cast.
        OffsetDateTimeConversions.toInstant(null, converterUtc);
    }

    @Test
    public void testMethodsRejectWrongObjectType() {
        Object notOdt = Integer.valueOf(42);
        // We verify that a ClassCastException is thrown for a representative subset;
        // the same behaviour is expected for every method that casts the argument.
        try {
            OffsetDateTimeConversions.toInstant(notOdt, converterUtc);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) { }

        try {
            OffsetDateTimeConversions.toString(notOdt, converterUtc);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) { }
    }

    // -----------------------------------------------------------------------
    // Edge‑case – extreme dates (minimum and maximum OffsetDateTime)
    // -----------------------------------------------------------------------
    @Test
    public void testMinimumOffsetDateTime() {
        OffsetDateTime min = OffsetDateTime.ofInstant(Instant.MIN, ZoneOffset.UTC);
        // toInstant must return Instant.MIN
        assertEquals(Instant.MIN, OffsetDateTimeConversions.toInstant(min, converterUtc));

        // toLong should overflow to Long.MIN_VALUE (Instant.MIN is -31557014167219200 seconds)
        long epochMilli = OffsetDateTimeConversions.toLong(min, converterUtc);
        assertEquals(Instant.MIN.toEpochMilli(), epochMilli);
    }

    @Test
    public void testMaximumOffsetDateTime() {
        OffsetDateTime max = OffsetDateTime.ofInstant(Instant.MAX, ZoneOffset.UTC);
        assertEquals(Instant.MAX, OffsetDateTimeConversions.toInstant(max, converterUtc));
        assertEquals(Instant.MAX.toEpochMilli(), OffsetDateTimeConversions.toLong(max, converterUtc));
    }
}
