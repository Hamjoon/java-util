package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link DateConversions}.
 *
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Typical conversions using a known epoch millisecond value.</li>
 *   <li>Edge cases such as {@code java.sql.Date} handling and {@code null} input.</li>
 *   <li>Error scenarios – e.g. passing a non‑Date object.</li>
 *   <li>Verification of internal constants via reflection (no mocking, only real objects).</li>
 * </ul>
 * </p>
 *
 * <p>
 * The library exposes a {@link Converter} that carries a {@link ConverterOptions}
 * instance.  All conversion methods use {@code converter.getOptions().getZoneId()}.
 * The default {@code ConverterOptions} constructor uses {@code ZoneOffset.UTC},
 * which is suitable for deterministic tests.
 * </p>
 */
@RunWith(JUnit4.class)
public class DateConversionsTest {

    // -----------------------------------------------------------------------
    // Test fixtures
    // -----------------------------------------------------------------------
    private static final long EPOCH_MILLIS = 1_609_459_200_123L; // 2020‑12‑31T23:00:00.123Z (UTC)
    private Date date;
    private java.sql.Date sqlDate;
    private Converter converter;
    private ZoneId utcZone;

    @Before
    public void setUp() {
        // a plain java.util.Date
        date = new Date(EPOCH_MILLIS);

        // a java.sql.Date – it is a subclass of java.util.Date but its toInstant()
        // behaviour is overridden in DateConversions.toInstant()
        sqlDate = new java.sql.Date(EPOCH_MILLIS);

        // Converter with default options (UTC)
        converter = new Converter(new ConverterOptions());
        utcZone = converter.getOptions().getZoneId();
        assertEquals("Test assumes UTC zone", ZoneOffset.UTC, utcZone);
    }

    // -----------------------------------------------------------------------
    // Helper methods for reflection based constants
    // -----------------------------------------------------------------------
    private static BigDecimal getGrandConstant() throws Exception {
        Class<?> clazz = Class.forName("com.cedarsoftware.util.convert.BigDecimalConversions");
        Field field = clazz.getDeclaredField("GRAND");
        field.setAccessible(true);
        return (BigDecimal) field.get(null);
    }

    private static String getMapDateKey() throws Exception {
        Class<?> clazz = Class.forName("com.cedarsoftware.util.convert.MapConversions");
        Field field = clazz.getDeclaredField("DATE");
        field.setAccessible(true);
        return (String) field.get(null);
    }

    // -----------------------------------------------------------------------
    // Tests for each conversion method
    // -----------------------------------------------------------------------

    @Test
    public void testToZonedDateTime_Typical() {
        ZonedDateTime zdt = DateConversions.toZonedDateTime(date, converter);
        ZonedDateTime expected = Instant.ofEpochMilli(EPOCH_MILLIS).atZone(utcZone);
        assertEquals(expected, zdt);
    }

    @Test
    public void testToLong_Typical() {
        long l = DateConversions.toLong(date, converter);
        assertEquals(EPOCH_MILLIS, l);
    }

    @Test
    public void testToDouble_Typical() {
        double d = DateConversions.toDouble(date, converter);
        assertEquals(EPOCH_MILLIS / 1000.0, d, 0.000001);
    }

    @Test
    public void testToSqlDate_Typical() {
        java.sql.Date sql = DateConversions.toSqlDate(date, converter);
        // The LocalDate part of the instant in UTC is 2020‑12‑31
        assertEquals(java.sql.Date.valueOf(LocalDate.of(2020, 12, 31)), sql);
    }

    @Test
    public void testToDate_Typical() {
        Date copy = DateConversions.toDate(date, converter);
        assertEquals(date, copy);
        // ensure a new instance is created
        assertNotSame(date, copy);
    }

    @Test
    public void testToTimestamp_Typical() {
        Timestamp ts = DateConversions.toTimestamp(date, converter);
        assertEquals(EPOCH_MILLIS, ts.getTime());
    }

    @Test
    public void testToCalendar_Typical() {
        Calendar cal = DateConversions.toCalendar(date, converter);
        assertEquals(EPOCH_MILLIS, cal.getTimeInMillis());
    }

    @Test
    public void testToBigDecimal_Typical() throws Exception {
        BigDecimal result = DateConversions.toBigDecimal(date, converter);
        BigDecimal GRAND = getGrandConstant();

        // Expected value = epochMillis / GRAND (scale 9, rounding down)
        BigDecimal expected = new BigDecimal(EPOCH_MILLIS)
                .divide(GRAND, 9, RoundingMode.DOWN);
        assertEquals(expected, result);
    }

    @Test
    public void testToInstant_WithUtilDate() {
        Instant instant = DateConversions.toInstant(date, converter);
        assertEquals(Instant.ofEpochMilli(EPOCH_MILLIS), instant);
    }

    @Test
    public void testToInstant_WithSqlDate() {
        // Special branch – should produce the same instant as a plain util.Date
        Instant instant = DateConversions.toInstant(sqlDate, converter);
        assertEquals(Instant.ofEpochMilli(EPOCH_MILLIS), instant);
    }

    @Test
    public void testToOffsetDateTime_Typical() {
        OffsetDateTime odt = DateConversions.toOffsetDateTime(date, converter);
        OffsetDateTime expected = DateConversions.toInstant(date, converter)
                .atZone(utcZone)
                .toOffsetDateTime();
        assertEquals(expected, odt);
    }

    @Test
    public void testToLocalDateTime_Typical() {
        LocalDateTime ldt = DateConversions.toLocalDateTime(date, converter);
        LocalDateTime expected = DateConversions.toZonedDateTime(date, converter).toLocalDateTime();
        assertEquals(expected, ldt);
    }

    @Test
    public void testToLocalDate_Typical() {
        LocalDate ld = DateConversions.toLocalDate(date, converter);
        LocalDate expected = DateConversions.toZonedDateTime(date, converter).toLocalDate();
        assertEquals(expected, ld);
    }

    @Test
    public void testToLocalTime_Typical() {
        LocalTime lt = DateConversions.toLocalTime(date, converter);
        LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(EPOCH_MILLIS), utcZone);
        assertEquals(ldt.toLocalTime(), lt);
    }

    @Test
    public void testToBigInteger_Typical() throws Exception {
        // InstantConversions.toBigInteger simply returns the epoch milliseconds as a BigInteger
        // (verified via reflection of the method implementation is not required;
        // we assert the contract by comparing with the expected value)
        BigInteger bi = DateConversions.toBigInteger(date, converter);
        assertEquals(BigInteger.valueOf(EPOCH_MILLIS), bi);
    }

    @Test
    public void testToAtomicLong_Typical() {
        AtomicLong al = DateConversions.toAtomicLong(date, converter);
        assertEquals(EPOCH_MILLIS, al.get());
    }

    @Test
    public void testToYear_Typical() {
        Year year = DateConversions.toYear(date, converter);
        assertEquals(Year.of(2020), year);
    }

    @Test
    public void testToYearMonth_Typical() {
        YearMonth ym = DateConversions.toYearMonth(date, converter);
        assertEquals(YearMonth.of(2020, 12), ym);
    }

    @Test
    public void testToMonthDay_Typical() {
        MonthDay md = DateConversions.toMonthDay(date, converter);
        assertEquals(MonthDay.of(12, 31), md);
    }

    @Test
    public void testToString_FormatHasThreeMillisDigits() {
        String s = DateConversions.toString(date, converter);
        // The formatter is built with appendInstant(3) → exactly three decimal places
        assertTrue("String should contain 3‑digit millis fraction", s.matches(".*\\.\\d{3}Z$"));
        // Verify the instant it represents
        Instant parsed = Instant.from(DateConversions.MILLIS_FMT.parse(s));
        assertEquals(Instant.ofEpochMilli(EPOCH_MILLIS), parsed);
    }

    @Test
    public void testToMap_Typical() throws Exception {
        Map<String, Object> map = DateConversions.toMap(date, converter);
        assertEquals(1, map.size());
        String key = getMapDateKey(); // should be "date"
        assertTrue(map.containsKey(key));
        assertEquals(DateConversions.toString(date, converter), map.get(key));
    }

    // -----------------------------------------------------------------------
    // Edge‑case / error scenario tests
    // -----------------------------------------------------------------------

    @Test(expected = ClassCastException.class)
    public void testToZonedDateTime_WithNonDate() {
        DateConversions.toZonedDateTime("not a date", converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInput() {
        DateConversions.toLong(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullInput() {
        DateConversions.toString(null, converter);
    }

    @Test
    public void testToInstant_NullInput_ShouldThrowNPE() {
        try {
            DateConversions.toInstant(null, converter);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {}
    }

    @Test
    public void testToLocalTime_EdgeMidnight() {
        // 2021‑01‑01T00:00:00.000Z
        Date midnight = new Date(1609459200000L);
        LocalTime lt = DateConversions.toLocalTime(midnight, converter);
        assertEquals(LocalTime.MIDNIGHT, lt);
    }

    @Test
    public void testToBigDecimal_RoundingDown() throws Exception {
        // epoch millis that would cause rounding up if not rounded down
        long millis = 1234L; // 1.234 seconds
        Date d = new Date(millis);
        BigDecimal result = DateConversions.toBigDecimal(d, converter);
        BigDecimal GRAND = getGrandConstant();
        BigDecimal expected = new BigDecimal(millis).divide(GRAND, 9, RoundingMode.DOWN);
        assertEquals(expected, result);
    }

    @Test
    public void testToMap_ContainsOnlyDateKey() throws Exception {
        Map<String, Object> map = DateConversions.toMap(date, converter);
        String key = getMapDateKey();
        assertEquals(1, map.size());
        assertTrue(map.containsKey(key));
        assertFalse(map.containsKey("time"));
    }
}
