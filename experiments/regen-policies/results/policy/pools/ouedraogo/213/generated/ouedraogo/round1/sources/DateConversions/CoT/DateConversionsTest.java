package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Unit tests for {@link DateConversions}.
 *
 * The tests are written against the public (package‑visible) static methods
 * of {@code DateConversions}.  They verify the normal behaviour as well as
 * edge‑case handling (null arguments, wrong argument types, special
 * handling of {@code java.sql.Date}, etc.).
 */
public class DateConversionsTest {

    private static final long FIXED_EPOCH_MILLIS = 1_609_459_200_123L; // 2021‑01‑01T00:00:00.123Z
    private Date utilDate;
    private java.sql.Date sqlDate;
    private Converter converter;   // uses default options (UTC zone)

    @Before
    public void setUp() {
        utilDate = new Date(FIXED_EPOCH_MILLIS);
        sqlDate  = new java.sql.Date(FIXED_EPOCH_MILLIS);
        // ConverterOptions is part of the library; we assume a no‑arg constructor gives UTC.
        ConverterOptions options = new ConverterOptions(); // default zone = UTC
        converter = new Converter(options);
    }

    // -----------------------------------------------------------------
    // 1. toZonedDateTime
    // -----------------------------------------------------------------
    @Test
    public void testToZonedDateTime_Normal() {
        ZonedDateTime zdt = DateConversions.toZonedDateTime(utilDate, converter);
        assertEquals(Instant.ofEpochMilli(FIXED_EPOCH_MILLIS), zdt.toInstant());
        assertEquals(converter.getOptions().getZoneId(), zdt.getZone());
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_NullFrom() {
        DateConversions.toZonedDateTime(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToZonedDateTime_WrongType() {
        DateConversions.toZonedDateTime("not a date", converter);
    }

    // -----------------------------------------------------------------
    // 2. toLong
    // -----------------------------------------------------------------
    @Test
    public void testToLong_Normal() {
        assertEquals(FIXED_EPOCH_MILLIS, DateConversions.toLong(utilDate, converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullFrom() {
        DateConversions.toLong(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_WrongType() {
        DateConversions.toLong(Boolean.TRUE, converter);
    }

    // -----------------------------------------------------------------
    // 3. toDouble
    // -----------------------------------------------------------------
    @Test
    public void testToDouble_Normal() {
        double expected = FIXED_EPOCH_MILLIS / 1000.0;
        assertEquals(expected, DateConversions.toDouble(utilDate, converter), 0.0000001);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullFrom() {
        DateConversions.toDouble(null, converter);
    }

    // -----------------------------------------------------------------
    // 4. toSqlDate
    // -----------------------------------------------------------------
    @Test
    public void testToSqlDate_FromUtilDate() {
        java.sql.Date result = DateConversions.toSqlDate(utilDate, converter);
        // The SQL date represents the same calendar day (UTC)
        assertEquals(LocalDate.ofInstant(Instant.ofEpochMilli(FIXED_EPOCH_MILLIS),
                                          converter.getOptions().getZoneId()),
                     result.toLocalDate());
    }

    @Test
    public void testToSqlDate_FromSqlDate() {
        java.sql.Date result = DateConversions.toSqlDate(sqlDate, converter);
        assertEquals(sqlDate, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToSqlDate_NullFrom() {
        DateConversions.toSqlDate(null, converter);
    }

    // -----------------------------------------------------------------
    // 5. toDate
    // -----------------------------------------------------------------
    @Test
    public void testToDate_Normal() {
        Date result = DateConversions.toDate(utilDate, converter);
        assertEquals(FIXED_EPOCH_MILLIS, result.getTime());
    }

    @Test(expected = NullPointerException.class)
    public void testToDate_NullFrom() {
        DateConversions.toDate(null, converter);
    }

    // -----------------------------------------------------------------
    // 6. toTimestamp
    // -----------------------------------------------------------------
    @Test
    public void testToTimestamp_Normal() {
        Timestamp ts = DateConversions.toTimestamp(utilDate, converter);
        assertEquals(FIXED_EPOCH_MILLIS, ts.getTime());
    }

    @Test(expected = NullPointerException.class)
    public void testToTimestamp_NullFrom() {
        DateConversions.toTimestamp(null, converter);
    }

    // -----------------------------------------------------------------
    // 7. toCalendar
    // -----------------------------------------------------------------
    @Test
    public void testToCalendar_Normal() {
        Calendar cal = DateConversions.toCalendar(utilDate, converter);
        assertEquals(FIXED_EPOCH_MILLIS, cal.getTimeInMillis());
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendar_NullFrom() {
        DateConversions.toCalendar(null, converter);
    }

    // -----------------------------------------------------------------
    // 8. toBigDecimal
    // -----------------------------------------------------------------
    @Test
    public void testToBigDecimal_Normal() {
        // The library defines BIGDecimalConversions.GRAND = new BigDecimal(1000)
        BigDecimal expected = new BigDecimal(FIXED_EPOCH_MILLIS)
                                    .divide(new BigDecimal(1000), 9, RoundingMode.DOWN);
        BigDecimal actual = DateConversions.toBigDecimal(utilDate, converter);
        assertEquals(9, actual.scale());
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullFrom() {
        DateConversions.toBigDecimal(null, converter);
    }

    // -----------------------------------------------------------------
    // 9. toInstant
    // -----------------------------------------------------------------
    @Test
    public void testToInstant_NormalDate() {
        Instant instant = DateConversions.toInstant(utilDate, converter);
        assertEquals(utilDate.toInstant(), instant);
    }

    @Test
    public void testToInstant_SqlDateSpecialCase() {
        // java.sql.Date overrides toInstant to return midnight; the method should
        // first wrap it in a util.Date, thus preserving the original millisecond value.
        Instant instant = DateConversions.toInstant(sqlDate, converter);
        assertEquals(Instant.ofEpochMilli(FIXED_EPOCH_MILLIS), instant);
    }

    @Test(expected = NullPointerException.class)
    public void testToInstant_NullFrom() {
        DateConversions.toInstant(null, converter);
    }

    // -----------------------------------------------------------------
    // 10. toOffsetDateTime
    // -----------------------------------------------------------------
    @Test
    public void testToOffsetDateTime_Normal() {
        OffsetDateTime odt = DateConversions.toOffsetDateTime(utilDate, converter);
        assertEquals(DateConversions.toInstant(utilDate, converter),
                     odt.toInstant());
        assertEquals(converter.getOptions().getZoneId(),
                     odt.getOffset().getRules().getOffset(Instant.now()).getId().equals("Z") ?
                     ZoneOffset.UTC : odt.getOffset());
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTime_NullFrom() {
        DateConversions.toOffsetDateTime(null, converter);
    }

    // -----------------------------------------------------------------
    // 11. toLocalDateTime
    // -----------------------------------------------------------------
    @Test
    public void testToLocalDateTime_Normal() {
        LocalDateTime ldt = DateConversions.toLocalDateTime(utilDate, converter);
        assertEquals(LocalDateTime.ofInstant(Instant.ofEpochMilli(FIXED_EPOCH_MILLIS),
                                             converter.getOptions().getZoneId()),
                     ldt);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTime_NullFrom() {
        DateConversions.toLocalDateTime(null, converter);
    }

    // -----------------------------------------------------------------
    // 12. toLocalDate
    // -----------------------------------------------------------------
    @Test
    public void testToLocalDate_Normal() {
        LocalDate ld = DateConversions.toLocalDate(utilDate, converter);
        assertEquals(LocalDate.ofInstant(Instant.ofEpochMilli(FIXED_EPOCH_MILLIS),
                                          converter.getOptions().getZoneId()),
                     ld);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDate_NullFrom() {
        DateConversions.toLocalDate(null, converter);
    }

    // -----------------------------------------------------------------
    // 13. toLocalTime
    // -----------------------------------------------------------------
    @Test
    public void testToLocalTime_Normal() {
        LocalTime lt = DateConversions.toLocalTime(utilDate, converter);
        assertEquals(LocalTime.ofInstant(Instant.ofEpochMilli(FIXED_EPOCH_MILLIS),
                                          converter.getOptions().getZoneId()),
                     lt);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullFrom() {
        DateConversions.toLocalTime(null, converter);
    }

    // -----------------------------------------------------------------
    // 14. toBigInteger
    // -----------------------------------------------------------------
    @Test
    public void testToBigInteger_Normal() {
        BigInteger bi = DateConversions.toBigInteger(utilDate, converter);
        assertNotNull(bi);
        // Verify that the BigInteger corresponds to the epoch seconds (as InstantConversions does)
        // The exact formula is not part of this test suite; we just ensure consistency with Instant.
        Instant instant = DateConversions.toInstant(utilDate, converter);
        BigInteger expected = InstantConversions.toBigInteger(instant, converter);
        assertEquals(expected, bi);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullFrom() {
        DateConversions.toBigInteger(null, converter);
    }

    // -----------------------------------------------------------------
    // 15. toAtomicLong
    // -----------------------------------------------------------------
    @Test
    public void testToAtomicLong_Normal() {
        AtomicLong al = DateConversions.toAtomicLong(utilDate, converter);
        assertEquals(FIXED_EPOCH_MILLIS, al.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullFrom() {
        DateConversions.toAtomicLong(null, converter);
    }

    // -----------------------------------------------------------------
    // 16. toYear
    // -----------------------------------------------------------------
    @Test
    public void testToYear_Normal() {
        Year year = DateConversions.toYear(utilDate, converter);
        assertEquals(Year.of(2021), year);
    }

    @Test(expected = NullPointerException.class)
    public void testToYear_NullFrom() {
        DateConversions.toYear(null, converter);
    }

    // -----------------------------------------------------------------
    // 17. toYearMonth
    // -----------------------------------------------------------------
    @Test
    public void testToYearMonth_Normal() {
        YearMonth ym = DateConversions.toYearMonth(utilDate, converter);
        assertEquals(YearMonth.of(2021, 1), ym);
    }

    @Test(expected = NullPointerException.class)
    public void testToYearMonth_NullFrom() {
        DateConversions.toYearMonth(null, converter);
    }

    // -----------------------------------------------------------------
    // 18. toMonthDay
    // -----------------------------------------------------------------
    @Test
    public void testToMonthDay_Normal() {
        MonthDay md = DateConversions.toMonthDay(utilDate, converter);
        assertEquals(MonthDay.of(1, 1), md);
    }

    @Test(expected = NullPointerException.class)
    public void testToMonthDay_NullFrom() {
        DateConversions.toMonthDay(null, converter);
    }

    // -----------------------------------------------------------------
    // 19. toString
    // -----------------------------------------------------------------
    @Test
    public void testToString_Normal() {
        String iso = DateConversions.toString(utilDate, converter);
        // Expected format: 2021-01-01T00:00:00.123Z
        assertEquals("2021-01-01T00:00:00.123Z", iso);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullFrom() {
        DateConversions.toString(null, converter);
    }

    // -----------------------------------------------------------------
    // 20. toMap
    // -----------------------------------------------------------------
    @Test
    public void testToMap_Normal() {
        Map<String,Object> map = DateConversions.toMap(utilDate, converter);
        assertNotNull(map);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(MapConversions.DATE));
        assertEquals(DateConversions.toString(utilDate, converter), map.get(MapConversions.DATE));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullFrom() {
        DateConversions.toMap(null, converter);
    }
}
