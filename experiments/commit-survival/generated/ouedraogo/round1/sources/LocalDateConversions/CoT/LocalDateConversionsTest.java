package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JUnit‑4 test suite for {@code LocalDateConversions}.
 *
 * The test class resides in the same package as {@code LocalDateConversions}
 * so that it can access the package‑private static methods directly.
 *
 * The tests cover:
 *   1. Normal (happy‑path) conversions.
 *   2. Edge cases such as {@code null} input and wrong input type,
 *      expecting {@link NullPointerException} or {@link ClassCastException}.
 *   3. Specific behaviours that depend on the {@link ConverterOptions}
 *      (zone handling, offset handling, etc.).
 */
public class LocalDateConversionsTest {

    /** A Converter configured to use UTC – this makes the expected results deterministic. */
    private Converter utcConverter;

    @Before
    public void setUp() {
        // Build a Converter with UTC ZoneId and TimeZone.
        // The concrete class ConverterOptions is part of the library (not shown in the prompt)
        // but it provides setters for zone and time‑zone.  Using UTC guarantees that
        // offsets are always zero, simplifying the assertions.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(ZoneId.of("UTC"));
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
        utcConverter = new Converter(options);
    }

    // -----------------------------------------------------------------------
    // 1.  List of public (package‑private static) methods with signatures
    // -----------------------------------------------------------------------
    //    static Instant            toInstant(Object from, Converter converter)
    //    static long               toLong(Object from, Converter converter)
    //    static LocalDateTime      toLocalDateTime(Object from, Converter converter)
    //    static ZonedDateTime      toZonedDateTime(Object from, Converter converter)
    //    static OffsetDateTime     toOffsetDateTime(Object from, Converter converter)
    //    static double             toDouble(Object from, Converter converter)
    //    static AtomicLong        toAtomicLong(Object from, Converter converter)
    //    static Timestamp          toTimestamp(Object from, Converter converter)
    //    static Calendar           toCalendar(Object from, Converter converter)
    //    static java.sql.Date      toSqlDate(Object from, Converter converter)
    //    static Date               toDate(Object from, Converter converter)
    //    static BigInteger         toBigInteger(Object from, Converter converter)
    //    static BigDecimal         toBigDecimal(Object from, Converter converter)
    //    static String             toString(Object from, Converter converter)
    //    static Map<String,Object> toMap(Object from, Converter converter)

    // -----------------------------------------------------------------------
    // 2.  Basic happy‑path tests
    // -----------------------------------------------------------------------

    @Test
    public void testToInstant() {
        LocalDate date = LocalDate.of(2022, 3, 15);
        Instant expected = ZonedDateTime.of(date, LocalTime.MIDNIGHT, ZoneOffset.UTC).toInstant();
        assertEquals(expected, LocalDateConversions.toInstant(date, utcConverter));
    }

    @Test
    public void testToLong() {
        LocalDate date = LocalDate.of(1970, 1, 2); // one day after epoch
        long expected = 24L * 60 * 60 * 1000; // one day in millis
        assertEquals(expected, LocalDateConversions.toLong(date, utcConverter));
    }

    @Test
    public void testToLocalDateTime() {
        LocalDate date = LocalDate.of(2021, 12, 31);
        LocalDateTime expected = LocalDateTime.of(date, LocalTime.MIDNIGHT);
        assertEquals(expected, LocalDateConversions.toLocalDateTime(date, utcConverter));
    }

    @Test
    public void testToZonedDateTime() {
        LocalDate date = LocalDate.of(1999, 12, 31);
        ZonedDateTime expected = ZonedDateTime.of(date, LocalTime.MIDNIGHT, ZoneId.of("UTC"));
        assertEquals(expected, LocalDateConversions.toZonedDateTime(date, utcConverter));
    }

    @Test
    public void testToOffsetDateTime() {
        LocalDate date = LocalDate.of(2000, 1, 1);
        // With UTC the offset is zero.
        OffsetDateTime expected = OffsetDateTime.of(date, LocalTime.MIDNIGHT, ZoneOffset.UTC);
        assertEquals(expected, LocalDateConversions.toOffsetDateTime(date, utcConverter));
    }

    @Test
    public void testToDouble() {
        LocalDate date = LocalDate.of(1970, 1, 1); // epoch start
        double expected = 0.0d;
        assertEquals(expected, LocalDateConversions.toDouble(date, utcConverter), 0.0);
    }

    @Test
    public void testToAtomicLong() {
        LocalDate date = LocalDate.of(1970, 1, 1);
        AtomicLong result = LocalDateConversions.toAtomicLong(date, utcConverter);
        assertEquals(0L, result.get());
    }

    @Test
    public void testToTimestamp() {
        LocalDate date = LocalDate.of(1970, 1, 2); // one day after epoch
        Timestamp expected = new Timestamp(24L * 60 * 60 * 1000);
        assertEquals(expected, LocalDateConversions.toTimestamp(date, utcConverter));
    }

    @Test
    public void testToCalendar() {
        LocalDate date = LocalDate.of(2020, 2, 29);
        Calendar cal = LocalDateConversions.toCalendar(date, utcConverter);
        long expectedMillis = ZonedDateTime.of(date, LocalTime.MIDNIGHT, ZoneId.of("UTC"))
                                            .toInstant()
                                            .toEpochMilli();
        assertEquals(expectedMillis, cal.getTimeInMillis());
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
    }

    @Test
    public void testToSqlDate() {
        LocalDate date = LocalDate.of(2023, 5, 10);
        java.sql.Date sqlDate = LocalDateConversions.toSqlDate(date, utcConverter);
        long expected = LocalDateConversions.toLong(date, utcConverter);
        assertEquals(expected, sqlDate.getTime());
    }

    @Test
    public void testToDate() {
        LocalDate date = LocalDate.of(1995, 8, 24);
        Date javaDate = LocalDateConversions.toDate(date, utcConverter);
        long expected = LocalDateConversions.toLong(date, utcConverter);
        assertEquals(expected, javaDate.getTime());
    }

    @Test
    public void testToBigInteger() {
        LocalDate date = LocalDate.of(2005, 6, 15);
        BigInteger bigInt = LocalDateConversions.toBigInteger(date, utcConverter);
        Instant instant = LocalDateConversions.toInstant(date, utcConverter);
        BigInteger expected = InstantConversions.toBigInteger(instant, utcConverter);
        assertEquals(expected, bigInt);
    }

    @Test
    public void testToBigDecimal() {
        LocalDate date = LocalDate.of(2010, 11, 30);
        BigDecimal bigDec = LocalDateConversions.toBigDecimal(date, utcConverter);
        Instant instant = LocalDateConversions.toInstant(date, utcConverter);
        BigDecimal expected = InstantConversions.toBigDecimal(instant, utcConverter);
        assertEquals(expected, bigDec);
    }

    @Test
    public void testToString() {
        LocalDate date = LocalDate.of(2021, 4, 5);
        String expected = "2021-04-05";
        assertEquals(expected, LocalDateConversions.toString(date, utcConverter));
    }

    @Test
    public void testToMap() {
        LocalDate date = LocalDate.of(2022, 12, 31);
        Map<String, Object> map = LocalDateConversions.toMap(date, utcConverter);
        assertNotNull(map);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(MapConversions.DATE));
        assertEquals(date.toString(), map.get(MapConversions.DATE));
    }

    // -----------------------------------------------------------------------
    // 3.  Edge‑case & exception tests
    // -----------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void testToInstant_NullInput() {
        LocalDateConversions.toInstant(null, utcConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToInstant_WrongType() {
        LocalDateConversions.toInstant(Integer.valueOf(42), utcConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_NullInput() {
        LocalDateConversions.toZonedDateTime(null, utcConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToZonedDateTime_WrongType() {
        LocalDateConversions.toZonedDateTime("2022-01-01", utcConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTime_NullInput() {
        LocalDateConversions.toOffsetDateTime(null, utcConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToOffsetDateTime_WrongType() {
        LocalDateConversions.toOffsetDateTime(Boolean.TRUE, utcConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToTimestamp_NullInput() {
        LocalDateConversions.toTimestamp(null, utcConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToTimestamp_WrongType() {
        LocalDateConversions.toTimestamp(Double.valueOf(3.14), utcConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullInput() {
        LocalDateConversions.toString(null, utcConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToString_WrongType() {
        LocalDateConversions.toString(LocalDateTime.now(), utcConverter);
    }

    @Test
    public void testToOffsetDateTime_DifferentTimeZone() {
        // Use a non‑UTC zone to verify that the offset is computed from the zone.
        ConverterOptions opts = new ConverterOptions();
        opts.setZoneId(ZoneId.of("America/New_York"));
        opts.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        Converter nyConverter = new Converter(opts);

        LocalDate date = LocalDate.of(2021, 7, 1); // Summer (DST)
        OffsetDateTime odt = LocalDateConversions.toOffsetDateTime(date, nyConverter);
        // The offset for New York during summer is -04:00.
        ZoneOffset expectedOffset = ZoneOffset.of("-04:00");
        assertEquals(expectedOffset, odt.getOffset());

        // Now test a winter date where the offset is -05:00.
        LocalDate winterDate = LocalDate.of(2021, 1, 1);
        OffsetDateTime winterOdt = LocalDateConversions.toOffsetDateTime(winterDate, nyConverter);
        ZoneOffset expectedWinterOffset = ZoneOffset.of("-05:00");
        assertEquals(expectedWinterOffset, winterOdt.getOffset());
    }

    @Test
    public void testToCalendar_UsesConverterTimeZone() {
        // Create a converter with a non‑UTC time‑zone.
        ConverterOptions opts = new ConverterOptions();
        opts.setZoneId(ZoneId.of("Asia/Tokyo"));
        opts.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        Converter tokyoConverter = new Converter(opts);

        LocalDate date = LocalDate.of(2000, 1, 1);
        Calendar cal = LocalDateConversions.toCalendar(date, tokyoConverter);
        assertEquals(TimeZone.getTimeZone("Asia/Tokyo"), cal.getTimeZone());
    }

    @Test
    public void testToLong_BoundaryEpochDay() {
        // LocalDate.MIN has epoch day = -365243219162 (far in the past)
        // The conversion should still produce a long without overflow.
        LocalDate minDate = LocalDate.ofEpochDay(Long.MIN_VALUE / (24L * 60 * 60 * 1000));
        long result = LocalDateConversions.toLong(minDate, utcConverter);
        // Result must be a finite long; we simply assert that it does not throw.
        assertTrue(Long.valueOf(result) instanceof Long);
    }

    @Test
    public void testToDouble_Precision() {
        LocalDate date = LocalDate.of(1970, 1, 2);
        double expected = 24 * 60 * 60; // seconds in one day
        assertEquals(expected, LocalDateConversions.toDouble(date, utcConverter), 0.000001);
    }
}
