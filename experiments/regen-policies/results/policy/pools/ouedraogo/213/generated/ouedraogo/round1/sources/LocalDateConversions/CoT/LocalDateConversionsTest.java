package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.Assert.*;

public class LocalDateConversionsTest {

    /*** -----------------------------------------------------------------
     *  List of public (package‑private static) methods in LocalDateConversions
     *  -----------------------------------------------------------------
     *  static Instant          toInstant(Object from, Converter converter)
     *  static long            toLong(Object from, Converter converter)
     *  static LocalDateTime   toLocalDateTime(Object from, Converter converter)
     *  static ZonedDateTime   toZonedDateTime(Object from, Converter converter)
     *  static OffsetDateTime  toOffsetDateTime(Object from, Converter converter)
     *  static double          toDouble(Object from, Converter converter)
     *  static AtomicLong      toAtomicLong(Object from, Converter converter)
     *  static Timestamp       toTimestamp(Object from, Converter converter)
     *  static Calendar        toCalendar(Object from, Converter converter)
     *  static java.sql.Date   toSqlDate(Object from, Converter converter)
     *  static Date            toDate(Object from, Converter converter)
     *  static BigInteger      toBigInteger(Object from, Converter converter)
     *  static BigDecimal      toBigDecimal(Object from, Converter converter)
     *  static Year            toYear(Object from, Converter converter)
     *  static YearMonth       toYearMonth(Object from, Converter converter)
     *  static MonthDay        toMonthDay(Object from, Converter converter)
     *  static String          toString(Object from, Converter converter)
     *  static Map<String,Object> toMap(Object from, Converter converter)
     * -----------------------------------------------------------------***/

    private Converter converter;
    private LocalDate sampleDate;                 // 2023‑03‑15
    private ZoneId systemZone;                    // system default zone used by ConverterOptions

    @Before
    public void setUp() {
        // The real Converter class expects a ConverterOptions instance.
        // For the purpose of these tests we rely on the default constructor
        // (the library provides a zero‑arg constructor that creates sensible defaults).
        converter = new Converter(new ConverterOptions());
        sampleDate = LocalDate.of(2023, Month.MARCH, 15);
        systemZone = converter.getOptions().getZoneId();
    }

    /*** ------------------- Basic functionality tests ------------------- ***/

    @Test
    public void testToInstant() {
        Instant instant = LocalDateConversions.toInstant(sampleDate, converter);
        ZonedDateTime zdt = ZonedDateTime.of(sampleDate, LocalTime.MIDNIGHT, systemZone);
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
        assertEquals(ZonedDateTime.of(sampleDate, LocalTime.MIDNIGHT, systemZone), zdt);
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime odt = LocalDateConversions.toOffsetDateTime(sampleDate, converter);
        TimeZone tz = converter.getOptions().getTimeZone();
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(tz.getOffset(System.currentTimeMillis()) / 1000);
        assertEquals(OffsetDateTime.of(sampleDate, LocalTime.MIDNIGHT, expectedOffset), odt);
    }

    @Test
    public void testToDouble() {
        double seconds = LocalDateConversions.toDouble(sampleDate, converter);
        long epochMilli = LocalDateConversions.toLong(sampleDate, converter);
        assertEquals(epochMilli / 1000d, seconds, 0.000001);
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong atomic = LocalDateConversions.toAtomicLong(sampleDate, converter);
        assertEquals(LocalDateConversions.toLong(sampleDate, converter), atomic.get());
    }

    @Test
    public void testToTimestamp() {
        Timestamp ts = LocalDateConversions.toTimestamp(sampleDate, converter);
        long expected = sampleDate.toEpochDay() * 86_400_000L; // days → milliseconds
        assertEquals(expected, ts.getTime());
    }

    @Test
    public void testToCalendar() {
        Calendar cal = LocalDateConversions.toCalendar(sampleDate, converter);
        ZonedDateTime zdt = LocalDateConversions.toZonedDateTime(sampleDate, converter);
        assertEquals(zdt.toInstant().toEpochMilli(), cal.getTimeInMillis());
        assertEquals(converter.getOptions().getTimeZone(), cal.getTimeZone());
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
        assertEquals(InstantConversions.toBigInteger(instant, converter), bi);
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal bd = LocalDateConversions.toBigDecimal(sampleDate, converter);
        Instant instant = LocalDateConversions.toInstant(sampleDate, converter);
        assertEquals(InstantConversions.toBigDecimal(instant, converter), bd);
    }

    @Test
    public void testToYear() {
        Year year = LocalDateConversions.toYear(sampleDate, converter);
        assertEquals(Year.of(2023), year);
    }

    @Test
    public void testToYearMonth() {
        YearMonth ym = LocalDateConversions.toYearMonth(sampleDate, converter);
        assertEquals(YearMonth.of(2023, 3), ym);
    }

    @Test
    public void testToMonthDay() {
        MonthDay md = LocalDateConversions.toMonthDay(sampleDate, converter);
        assertEquals(MonthDay.of(3, 15), md);
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

    /*** ----------------------- Edge‑case tests ----------------------- ***/

    @Test(expected = ClassCastException.class)
    public void testToZonedDateTimeWithWrongType() {
        // Passing a String instead of LocalDate forces the cast to fail
        LocalDateConversions.toZonedDateTime("2023-03-15", converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTimeWithNull() {
        LocalDateConversions.toZonedDateTime(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToOffsetDateTimeWithWrongType() {
        LocalDateConversions.toOffsetDateTime(12345, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTimeWithNull() {
        LocalDateConversions.toOffsetDateTime(null, converter);
    }

    @Test
    public void testToTimestampEpochBoundary() {
        // 1970‑01‑01 (epoch day = 0) should produce timestamp 0
        LocalDate epoch = LocalDate.ofEpochDay(0);
        Timestamp ts = LocalDateConversions.toTimestamp(epoch, converter);
        assertEquals(0L, ts.getTime());
    }

    @Test
    public void testToLongNegativeEpoch() {
        // Date before 1970‑01‑01 yields negative epoch milliseconds
        LocalDate beforeEpoch = LocalDate.of(1969, 12, 31);
        long epochMilli = LocalDateConversions.toLong(beforeEpoch, converter);
        assertTrue(epochMilli < 0);
    }

    @Test
    public void testToMapImmutability() {
        Map<String, Object> map = LocalDateConversions.toMap(sampleDate, converter);
        // The returned map should be mutable (LinkedHashMap) but changes must not affect the original conversion
        map.put("extra", "value");
        assertEquals(2, map.size());
        assertTrue(map.containsKey("extra"));
    }

    @Test
    public void testToStringLeapDay() {
        LocalDate leapDay = LocalDate.of(2020, 2, 29);
        String s = LocalDateConversions.toString(leapDay, converter);
        assertEquals("2020-02-29", s);
    }

    @Test
    public void testToYearMonthEndOfMonth() {
        LocalDate date = LocalDate.of(2021, 4, 30);
        YearMonth ym = LocalDateConversions.toYearMonth(date, converter);
        assertEquals(YearMonth.of(2021, 4), ym);
    }

    @Test
    public void testToMonthDayFebruary28NonLeap() {
        LocalDate date = LocalDate.of(2019, 2, 28);
        MonthDay md = LocalDateConversions.toMonthDay(date, converter);
        assertEquals(MonthDay.of(2, 28), md);
    }
}
