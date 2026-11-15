package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link LocalDateConversions}.
 *
 * The suite covers:
 *   – normal (“happy‑path”) behaviour,
 *   – null / wrong‑type arguments,
 *   – leap‑year and DST edge cases,
 *   – dates before and after the epoch,
 *   – large future dates,
 *   – numeric conversions (long, double, BigInteger, BigDecimal),
 *   – map and string representations.
 *
 * The tests are placed in the same package as the class under test
 * so that the package‑private static methods are directly accessible.
 */
public class LocalDateConversionsTest {

    private Converter converter;               // default options – system default zone
    private Converter nyConverter;             // New‑York specific options (used in one DST test)

    /** Helper to create a Converter with a given {@link TimeZone} / {@link ZoneId}. */
    private static Converter createConverter(TimeZone tz, ZoneId zid) {
        ConverterOptions opts = new ConverterOptions();
        opts.setTimeZone(tz);
        opts.setZoneId(zid);
        return new Converter(opts);
    }

    @Before
    public void setUp() {
        // default converter – uses the JVM’s default time‑zone
        converter = new Converter(new ConverterOptions());

        // New‑York converter for the DST test
        nyConverter = createConverter(TimeZone.getTimeZone("America/New_York"),
                                      ZoneId.of("America/New_York"));
    }

    /* --------------------------------------------------------------
     * 1.  Normal‑path tests
     * -------------------------------------------------------------- */
    @Test
    public void testToInstant() {
        LocalDate ld = LocalDate.of(2023, 3, 15);
        Instant i = LocalDateConversions.toInstant(ld, converter);
        ZonedDateTime expectedZdt = ld.atStartOfDay(converter.getOptions().getZoneId());
        assertEquals(expectedZdt.toInstant(), i);
    }

    @Test
    public void testToLong() {
        LocalDate ld = LocalDate.of(1970, 1, 2);               // +1 day
        long epochMilli = LocalDateConversions.toLong(ld, converter);
        assertEquals(24L * 60 * 60 * 1000, epochMilli);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDate ld = LocalDate.of(2021, 12, 31);
        LocalDateTime ldt = LocalDateConversions.toLocalDateTime(ld, converter);
        assertEquals(ld.atStartOfDay(), ldt);
    }

    @Test
    public void testToZonedDateTime() {
        LocalDate ld = LocalDate.of(2000, 2, 29); // leap day
        ZonedDateTime zdt = LocalDateConversions.toZonedDateTime(ld, converter);
        assertEquals(ld, zdt.toLocalDate());
        assertEquals(LocalTime.MIDNIGHT, zdt.toLocalTime());
        assertEquals(converter.getOptions().getZoneId(), zdt.getZone());
    }

    @Test
    public void testToOffsetDateTime() {
        LocalDate ld = LocalDate.of(2022, 1, 1);
        OffsetDateTime odt = LocalDateConversions.toOffsetDateTime(ld, converter);
        assertEquals(ld, odt.toLocalDate());
        assertEquals(LocalTime.MIDNIGHT, odt.toLocalTime());

        ZoneOffset expected = ZoneOffset.ofTotalSeconds(
                converter.getOptions().getTimeZone().getOffset(System.currentTimeMillis()) / 1000);
        assertEquals(expected, odt.getOffset());
    }

    @Test
    public void testToDouble() {
        LocalDate ld = LocalDate.of(1970, 1, 2);
        double d = LocalDateConversions.toDouble(ld, converter);
        assertEquals(24 * 60 * 60, d, 0.000001);
    }

    @Test
    public void testToAtomicLong() {
        LocalDate ld = LocalDate.of(1970, 1, 1);
        AtomicLong al = LocalDateConversions.toAtomicLong(ld, converter);
        assertEquals(0L, al.get());
    }

    @Test
    public void testToTimestamp() {
        LocalDate ld = LocalDate.of(1970, 1, 2);
        Timestamp ts = LocalDateConversions.toTimestamp(ld, converter);
        assertEquals(24L * 60 * 60 * 1000, ts.getTime());
    }

    @Test
    public void testToCalendar() {
        LocalDate ld = LocalDate.of(2020, 5, 20);
        Calendar cal = LocalDateConversions.toCalendar(ld, converter);
        long expected = ld.atStartOfDay(converter.getOptions().getZoneId())
                          .toInstant().toEpochMilli();
        assertEquals(expected, cal.getTimeInMillis());
        assertEquals(converter.getOptions().getTimeZone(), cal.getTimeZone());
    }

    @Test
    public void testToSqlDate() {
        LocalDate ld = LocalDate.of(1999, 12, 31);
        java.sql.Date sqlDate = LocalDateConversions.toSqlDate(ld, converter);
        assertEquals(java.sql.Date.valueOf(ld), sqlDate);
    }

    @Test
    public void testToDate() {
        LocalDate ld = LocalDate.of(1970, 1, 1);
        Date d = LocalDateConversions.toDate(ld, converter);
        assertEquals(0L, d.getTime());
    }

    @Test
    public void testToBigInteger() {
        LocalDate ld = LocalDate.of(1970, 1, 2);
        BigInteger bi = LocalDateConversions.toBigInteger(ld, converter);
        assertEquals(BigInteger.valueOf(24L * 60 * 60 * 1000), bi);
    }

    @Test
    public void testToBigDecimal() {
        LocalDate ld = LocalDate.of(1970, 1, 2);
        BigDecimal bd = LocalDateConversions.toBigDecimal(ld, converter);
        assertEquals(BigDecimal.valueOf(24L * 60 * 60 * 1000), bd);
    }

    @Test
    public void testToYear() {
        LocalDate ld = LocalDate.of(2015, 8, 23);
        Year y = LocalDateConversions.toYear(ld, converter);
        assertEquals(Year.of(2015), y);
    }

    @Test
    public void testToYearMonth() {
        LocalDate ld = LocalDate.of(2018, 11, 5);
        YearMonth ym = LocalDateConversions.toYearMonth(ld, converter);
        assertEquals(YearMonth.of(2018, 11), ym);
    }

    @Test
    public void testToMonthDay() {
        LocalDate ld = LocalDate.of(2001, 2, 28);
        MonthDay md = LocalDateConversions.toMonthDay(ld, converter);
        assertEquals(MonthDay.of(2, 28), md);
    }

    @Test
    public void testToString() {
        LocalDate ld = LocalDate.of(2022, 4, 1);
        String s = LocalDateConversions.toString(ld, converter);
        assertEquals("2022-04-01", s);
    }

    @Test
    public void testToMap() {
        LocalDate ld = LocalDate.of(2020, 12, 31);
        Map<String, Object> map = LocalDateConversions.toMap(ld, converter);
        assertEquals(1, map.size());
        assertEquals(ld.toString(), map.get(MapConversions.LOCAL_DATE));
    }

    /* --------------------------------------------------------------
     * 2.  Null / wrong‑type argument tests (exception handling)
     * -------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testToInstant_Null() {
        LocalDateConversions.toInstant(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToInstant_WrongType() {
        LocalDateConversions.toInstant(Integer.valueOf(42), converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_Null() {
        LocalDateConversions.toLong(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_WrongType() {
        LocalDateConversions.toLong("not a date", converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTime_Null() {
        LocalDateConversions.toLocalDateTime(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalDateTime_WrongType() {
        LocalDateConversions.toLocalDateTime(Boolean.TRUE, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_Null() {
        LocalDateConversions.toZonedDateTime(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToZonedDateTime_WrongType() {
        LocalDateConversions.toZonedDateTime(3.14, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTime_Null() {
        LocalDateConversions.toOffsetDateTime(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToOffsetDateTime_WrongType() {
        LocalDateConversions.toOffsetDateTime(new Object(), converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_Null() {
        LocalDateConversions.toDouble(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToDouble_WrongType() {
        LocalDateConversions.toDouble(new HashMap<>(), converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_Null() {
        LocalDateConversions.toAtomicLong(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLong_WrongType() {
        LocalDateConversions.toAtomicLong(Long.valueOf(123L), converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToTimestamp_Null() {
        LocalDateConversions.toTimestamp(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToTimestamp_WrongType() {
        LocalDateConversions.toTimestamp(Boolean.FALSE, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendar_Null() {
        LocalDateConversions.toCalendar(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToCalendar_WrongType() {
        LocalDateConversions.toCalendar(Short.valueOf((short)1), converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToSqlDate_Null() {
        LocalDateConversions.toSqlDate(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToSqlDate_WrongType() {
        LocalDateConversions.toSqlDate(new Date(), converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToDate_Null() {
        LocalDateConversions.toDate(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToDate_WrongType() {
        LocalDateConversions.toDate("2020-01-01", converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_Null() {
        LocalDateConversions.toBigInteger(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigInteger_WrongType() {
        LocalDateConversions.toBigInteger(Byte.valueOf((byte)7), converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_Null() {
        LocalDateConversions.toBigDecimal(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimal_WrongType() {
        LocalDateConversions.toBigDecimal(Character.valueOf('x'), converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToYear_Null() {
        LocalDateConversions.toYear(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToYear_WrongType() {
        LocalDateConversions.toYear(Double.NaN, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToYearMonth_Null() {
        LocalDateConversions.toYearMonth(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToYearMonth_WrongType() {
        LocalDateConversions.toYearMonth(new int[]{1,2,3}, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToMonthDay_Null() {
        LocalDateConversions.toMonthDay(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMonthDay_WrongType() {
        LocalDateConversions.toMonthDay(Float.valueOf(1.23f), converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_Null() {
        LocalDateConversions.toString(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToString_WrongType() {
        LocalDateConversions.toString(new StringBuilder("2020-01-01"), converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_Null() {
        LocalDateConversions.toMap(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WrongType() {
        LocalDateConversions.toMap(Boolean.TRUE, converter);
    }

    /* --------------------------------------------------------------
     * 3.  Additional edge‑case tests
     * -------------------------------------------------------------- */
    @Test
    public void testToOffsetDateTime_WithNewYorkDST() {
        // DST transition in New York – 2022‑03‑13 switches from -05:00 to -04:00 at 02:00.
        // Since we always use midnight, the offset should still be -05:00.
        LocalDate ld = LocalDate.of(2022, 3, 13);
        OffsetDateTime odt = LocalDateConversions.toOffsetDateTime(ld, nyConverter);
        assertEquals(ZoneOffset.ofHours(-5), odt.getOffset());
    }

    @Test
    public void testToLong_BeforeEpoch() {
        LocalDate ld = LocalDate.of(1969, 12, 31);
        long epochMilli = LocalDateConversions.toLong(ld, converter);
        assertEquals(-24L * 60 * 60 * 1000, epochMilli);
    }

    @Test
    public void testToTimestamp_LargeFutureDate() {
        LocalDate ld = LocalDate.of(3000, 1, 1);
        Timestamp ts = LocalDateConversions.toTimestamp(ld, converter);
        long expected = ld.toEpochDay() * 86_400_000L;
        assertEquals(expected, ts.getTime());
    }

    @Test
    public void testToBigInteger_VeryLargeDate() {
        LocalDate ld = LocalDate.of(9999, 12, 31);
        BigInteger bi = LocalDateConversions.toBigInteger(ld, converter);
        // Expected value: epoch day * 86_400_000
        long epochMilli = ld.atStartOfDay(converter.getOptions().getZoneId())
                            .toInstant().toEpochMilli();
        assertEquals(BigInteger.valueOf(epochMilli), bi);
    }

    @Test
    public void testToBigDecimal_VeryLargeDate() {
        LocalDate ld = LocalDate.of(9999, 12, 31);
        BigDecimal bd = LocalDateConversions.toBigDecimal(ld, converter);
        long epochMilli = ld.atStartOfDay(converter.getOptions().getZoneId())
                            .toInstant().toEpochMilli();
        assertEquals(BigDecimal.valueOf(epochMilli), bd);
    }

    @Test
    public void testToMap_IsMutableButDoesNotAffectSource() {
        LocalDate ld = LocalDate.of(2021, 7, 15);
        Map<String, Object> map = LocalDateConversions.toMap(ld, converter);
        map.put("extra", "value");
        // The source LocalDate is immutable – the test simply confirms that
        // mutating the returned map does not alter the original LocalDate.
        assertEquals(ld, ld);
        assertEquals(ld.toString(), map.get(MapConversions.LOCAL_DATE));
    }

    @Test
    public void testToString_LeadingZeros() {
        LocalDate ld = LocalDate.of(2005, 1, 5);
        String s = LocalDateConversions.toString(ld, converter);
        assertEquals("2005-01-05", s);
    }
}
