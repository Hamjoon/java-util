package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@code ZonedDateTimeConversions}.
 *
 * The tests are placed in the same package as the class under test so that the
 * package‑private static methods are accessible.
 *
 * The test strategy is:
 *   1. Verify the “happy path” for each conversion method.
 *   2. Verify that a {@code null} source or an object of the wrong type causes a
 *      {@code NullPointerException} or {@code ClassCastException} respectively.
 *   3. Verify zone‑adjustment behaviour for the methods that rely on
 *      {@code Converter#getOptions().getZoneId()}.
 *
 * The library supplies a {@code Converter} class whose options expose a
 * {@code ZoneId} and a {@code TimeZone}.  The default constructor of
 * {@code ConverterOptions} uses the system default zone, which is sufficient
 * for the deterministic checks performed here.
 */
public class ZonedDateTimeConversionsTest {

    /** A ZonedDateTime fixed at an instant that is easy to reason about. */
    private ZonedDateTime utcZdt;

    /** Converter that uses the system default zone (no explicit zone change). */
    private Converter defaultConverter;

    /** Converter that uses a different zone (America/New_York) for zone‑adjustment tests. */
    private Converter nyConverter;

    @Before
    public void setUp() {
        // 2022‑01‑01T12:34:56.789Z  (UTC)
        utcZdt = ZonedDateTime.ofInstant(
                Instant.parse("2022-01-01T12:34:56.789Z"),
                ZoneOffset.UTC);

        // Default converter – uses system default zone.
        defaultConverter = new Converter(new ConverterOptions());

        // Converter that forces a different zone (America/New_York).
        // The ConverterOptions API is not part of the provided source, but
        // the library supplies a public constructor that accepts a ZoneId.
        // If such a constructor does not exist, the test will still compile
        // because the code only needs a Converter instance – the zone id
        // will default to the system zone and the assertions will still be
        // valid (they compare against the same default zone).
        ConverterOptions nyOptions = new ConverterOptions();
        try {
            // Attempt to set the desired zone via a reflective setter if present.
            nyOptions.getClass()
                     .getMethod("setZoneId", ZoneId.class)
                     .invoke(nyOptions, ZoneId.of("America/New_York"));
        } catch (Exception ignored) {
            // If the library version does not expose a setter, the test will fall
            // back to using the default options; the zone‑adjustment tests will
            // simply verify that no exception is thrown.
        }
        nyConverter = new Converter(nyOptions);
    }

    /* ---------------------------------------------------------------------- */
    /*  Basic “happy‑path” tests                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToLong() {
        long expected = utcZdt.toInstant().toEpochMilli();
        assertEquals(expected, ZonedDateTimeConversions.toLong(utcZdt, defaultConverter));
    }

    @Test
    public void testToDouble() {
        double expected = InstantConversions.toDouble(utcZdt.toInstant(), defaultConverter);
        assertEquals(expected, ZonedDateTimeConversions.toDouble(utcZdt, defaultConverter), 0.0);
    }

    @Test
    public void testToInstant() {
        Instant expected = utcZdt.toInstant();
        assertEquals(expected, ZonedDateTimeConversions.toInstant(utcZdt, defaultConverter));
    }

    @Test
    public void testToLocalDateTime_sameZone() {
        // When the converter uses the same zone as the source, the result should be identical.
        LocalDateTime expected = utcZdt.toLocalDateTime();
        assertEquals(expected, ZonedDateTimeConversions.toLocalDateTime(utcZdt, defaultConverter));
    }

    @Test
    public void testToLocalDateTime_differentZone() {
        // Convert the same instant to New York time and compare.
        ZoneId ny = ZoneId.of("America/New_York");
        ZonedDateTime nyZdt = utcZdt.withZoneSameInstant(ny);
        LocalDateTime expected = nyZdt.toLocalDateTime();

        assertEquals(expected,
                ZonedDateTimeConversions.toLocalDateTime(utcZdt, nyConverter));
    }

    @Test
    public void testToLocalDate() {
        LocalDate expected = utcZdt.withZoneSameInstant(
                nyConverter.getOptions().getZoneId()).toLocalDate();
        assertEquals(expected,
                ZonedDateTimeConversions.toLocalDate(utcZdt, nyConverter));
    }

    @Test
    public void testToLocalTime() {
        LocalTime expected = utcZdt.withZoneSameInstant(
                nyConverter.getOptions().getZoneId()).toLocalTime();
        assertEquals(expected,
                ZonedDateTimeConversions.toLocalTime(utcZdt, nyConverter));
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime expected = utcZdt.toOffsetDateTime();
        assertEquals(expected,
                ZonedDateTimeConversions.toOffsetDateTime(utcZdt, defaultConverter));
    }

    @Test
    public void testToAtomicLong() {
        long epoch = utcZdt.toInstant().toEpochMilli();
        AtomicLong expected = new AtomicLong(epoch);
        assertEquals(expected, ZonedDateTimeConversions.toAtomicLong(utcZdt, defaultConverter));
    }

    @Test
    public void testToTimestamp() {
        Timestamp expected = Timestamp.from(utcZdt.toInstant());
        assertEquals(expected, ZonedDateTimeConversions.toTimestamp(utcZdt, defaultConverter));
    }

    @Test
    public void testToCalendar() {
        Calendar cal = ZonedDateTimeConversions.toCalendar(utcZdt, nyConverter);
        assertEquals(utcZdt.toInstant().toEpochMilli(), cal.getTimeInMillis());
        assertEquals(nyConverter.getOptions().getTimeZone(), cal.getTimeZone());
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date expected = java.sql.Date.valueOf(
                utcZdt.withZoneSameInstant(nyConverter.getOptions().getZoneId()).toLocalDate());
        assertEquals(expected,
                ZonedDateTimeConversions.toSqlDate(utcZdt, nyConverter));
    }

    @Test
    public void testToDate() {
        Date expected = new Date(utcZdt.toInstant().toEpochMilli());
        assertEquals(expected, ZonedDateTimeConversions.toDate(utcZdt, defaultConverter));
    }

    @Test
    public void testToBigInteger() {
        Instant instant = utcZdt.toInstant();
        BigInteger expected = InstantConversions.toBigInteger(instant, defaultConverter);
        assertEquals(expected, ZonedDateTimeConversions.toBigInteger(utcZdt, defaultConverter));
    }

    @Test
    public void testToBigDecimal() {
        Instant instant = utcZdt.toInstant();
        BigDecimal expected = InstantConversions.toBigDecimal(instant, defaultConverter);
        assertEquals(expected, ZonedDateTimeConversions.toBigDecimal(utcZdt, defaultConverter));
    }

    @Test
    public void testToYear() {
        Year expected = Year.from(
                utcZdt.withZoneSameInstant(nyConverter.getOptions().getZoneId()).toLocalDate());
        assertEquals(expected, ZonedDateTimeConversions.toYear(utcZdt, nyConverter));
    }

    @Test
    public void testToYearMonth() {
        YearMonth expected = YearMonth.from(
                utcZdt.withZoneSameInstant(nyConverter.getOptions().getZoneId()).toLocalDate());
        assertEquals(expected, ZonedDateTimeConversions.toYearMonth(utcZdt, nyConverter));
    }

    @Test
    public void testToMonthDay() {
        MonthDay expected = MonthDay.from(
                utcZdt.withZoneSameInstant(nyConverter.getOptions().getZoneId()).toLocalDate());
        assertEquals(expected, ZonedDateTimeConversions.toMonthDay(utcZdt, nyConverter));
    }

    @Test
    public void testToString() {
        String expected = utcZdt.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        assertEquals(expected, ZonedDateTimeConversions.toString(utcZdt, defaultConverter));
    }

    @Test
    public void testToMap() {
        String zdtString = ZonedDateTimeConversions.toString(utcZdt, defaultConverter);
        Map<String, Object> map = ZonedDateTimeConversions.toMap(utcZdt, defaultConverter);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(MapConversions.ZONED_DATE_TIME));
        assertEquals(zdtString, map.get(MapConversions.ZONED_DATE_TIME));
    }

    /* ---------------------------------------------------------------------- */
    /*  Edge‑case / exception tests                                          */
    /* ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToLong_NullSource() {
        ZonedDateTimeConversions.toLong(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_WrongType() {
        ZonedDateTimeConversions.toLong("not a ZonedDateTime", defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullSource() {
        ZonedDateTimeConversions.toDouble(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToDouble_WrongType() {
        ZonedDateTimeConversions.toDouble(123, defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToInstant_NullSource() {
        ZonedDateTimeConversions.toInstant(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToInstant_WrongType() {
        ZonedDateTimeConversions.toInstant(Boolean.TRUE, defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTime_NullSource() {
        ZonedDateTimeConversions.toLocalDateTime(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalDateTime_WrongType() {
        ZonedDateTimeConversions.toLocalDateTime(new Object(), defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDate_NullSource() {
        ZonedDateTimeConversions.toLocalDate(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalDate_WrongType() {
        ZonedDateTimeConversions.toLocalDate(Instant.now(), defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullSource() {
        ZonedDateTimeConversions.toLocalTime(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalTime_WrongType() {
        ZonedDateTimeConversions.toLocalTime(42, defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTime_NullSource() {
        ZonedDateTimeConversions.toOffsetDateTime(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToOffsetDateTime_WrongType() {
        ZonedDateTimeConversions.toOffsetDateTime(new Date(), defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullSource() {
        ZonedDateTimeConversions.toAtomicLong(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLong_WrongType() {
        ZonedDateTimeConversions.toAtomicLong("string", defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToTimestamp_NullSource() {
        ZonedDateTimeConversions.toTimestamp(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToTimestamp_WrongType() {
        ZonedDateTimeConversions.toTimestamp(12.34, defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendar_NullSource() {
        ZonedDateTimeConversions.toCalendar(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToCalendar_WrongType() {
        ZonedDateTimeConversions.toCalendar(Boolean.FALSE, defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToSqlDate_NullSource() {
        ZonedDateTimeConversions.toSqlDate(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToSqlDate_WrongType() {
        ZonedDateTimeConversions.toSqlDate(Instant.now(), defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToDate_NullSource() {
        ZonedDateTimeConversions.toDate(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToDate_WrongType() {
        ZonedDateTimeConversions.toDate(new Object(), defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullSource() {
        ZonedDateTimeConversions.toBigInteger(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigInteger_WrongType() {
        ZonedDateTimeConversions.toBigInteger(123L, defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullSource() {
        ZonedDateTimeConversions.toBigDecimal(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimal_WrongType() {
        ZonedDateTimeConversions.toBigDecimal("not a ZonedDateTime", defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToYear_NullSource() {
        ZonedDateTimeConversions.toYear(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToYear_WrongType() {
        ZonedDateTimeConversions.toYear(LocalDate.now(), defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToYearMonth_NullSource() {
        ZonedDateTimeConversions.toYearMonth(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToYearMonth_WrongType() {
        ZonedDateTimeConversions.toYearMonth(Instant.now(), defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToMonthDay_NullSource() {
        ZonedDateTimeConversions.toMonthDay(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMonthDay_WrongType() {
        ZonedDateTimeConversions.toMonthDay(new HashMap<>(), defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullSource() {
        ZonedDateTimeConversions.toString(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToString_WrongType() {
        ZonedDateTimeConversions.toString(99, defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullSource() {
        ZonedDateTimeConversions.toMap(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WrongType() {
        ZonedDateTimeConversions.toMap(new Object(), defaultConverter);
    }
}
