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

/**
 * Comprehensive JUnit‑4 test suite for {@link CalendarConversions}.
 *
 * The test class lives in the same package as {@code CalendarConversions} so that it can
 * invoke the package‑private static conversion methods directly.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal (happy‑path) conversion for each method.</li>
 *   <li>Edge cases such as {@code null} input, extreme epoch values and
 *       time‑zone peculiarities.</li>
 *   <li>Exception handling – most methods rely on a cast to {@link Calendar}
 *       and therefore throw {@link NullPointerException} or {@link ClassCastException}
 *       when the supplied object is not a {@code Calendar}.</li>
 *   <li>Special handling performed by {@code toString()} for zone‑abbreviation
 *       mapping via {@link DateUtilities#ABBREVIATION_TO_TIMEZONE}.</li>
 * </ul>
 *
 * The test suite uses only the public API listed in the prompt ( {@code Converter},
 * {@code DateUtilities}) and the standard JDK classes.  No mocking frameworks are used.
 */
public class CalendarConversionsTest {

    private Converter converter;                 // a usable Converter instance
    private Calendar utcCalendar;                // Calendar fixed to UTC
    private Calendar estCalendar;                // Calendar fixed to a zone with an abbreviation (EST)

    /**
     * Initialise a {@link Converter} with default options and two representative calendars:
     * <ul>
     *   <li>UTC – a clean, well‑known zone.</li>
     *   <li>EST – a zone whose ID is an abbreviation (no '/' character).  The test injects a mapping
     *        into {@link DateUtilities#ABBREVIATION_TO_TIMEZONE} so that {@code toString()} can resolve it.
     * </ul>
     */
    @Before
    public void setUp() {
        // The concrete implementation of ConverterOptions is not shown, but a no‑arg constructor
        // is assumed to exist.  The default options contain a ZoneId (system default) and a TimeZone.
        converter = new Converter(new ConverterOptions());

        // -----------------------------------------------------------------
        // Calendar set to a fixed instant (2022‑01‑01T12:34:56.789Z) in UTC
        // -----------------------------------------------------------------
        utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCalendar.clear();
        utcCalendar.setTimeInMillis(Instant.parse("2022-01-01T12:34:56.789Z").toEpochMilli());

        // -----------------------------------------------------------------
        // Calendar set to the same instant but using the "EST" abbreviation.
        // -----------------------------------------------------------------
        estCalendar = Calendar.getInstance(TimeZone.getTimeZone("EST"));
        estCalendar.clear();
        estCalendar.setTimeInMillis(Instant.parse("2022-01-01T12:34:56.789Z").toEpochMilli());

        // Ensure that the abbreviation map contains an entry for "EST".
        // The map is mutable even though the reference is final, so we can add our test entry.
        DateUtilities.ABBREVIATION_TO_TIMEZONE.put("EST", "America/New_York");
    }

    // -----------------------------------------------------------------
    // 1. toLong
    // -----------------------------------------------------------------
    @Test
    public void testToLong_HappyPath() {
        Long expected = utcCalendar.getTimeInMillis();
        assertEquals(expected, CalendarConversions.toLong(utcCalendar, converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInput() {
        CalendarConversions.toLong(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_WrongType() {
        CalendarConversions.toLong("not a calendar", converter);
    }

    // -----------------------------------------------------------------
    // 2. toAtomicLong
    // -----------------------------------------------------------------
    @Test
    public void testToAtomicLong_HappyPath() {
        AtomicLong expected = new AtomicLong(utcCalendar.getTimeInMillis());
        assertEquals(expected, CalendarConversions.toAtomicLong(utcCalendar, converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullInput() {
        CalendarConversions.toAtomicLong(null, converter);
    }

    // -----------------------------------------------------------------
    // 3. toDouble
    // -----------------------------------------------------------------
    @Test
    public void testToDouble_HappyPath() {
        double expected = utcCalendar.getTimeInMillis() / 1000.0;
        assertEquals(expected, CalendarConversions.toDouble(utcCalendar, converter), 0.0000001);
    }

    // -----------------------------------------------------------------
    // 4. toBigDecimal
    // -----------------------------------------------------------------
    @Test
    public void testToBigDecimal_HappyPath() throws Exception {
        // The conversion divides the epoch millis by BigDecimalConversions.GRAND.
        // Retrieve the divisor via reflection (the constant is package‑private).
        Class<?> bdConvClass = Class.forName("com.cedarsoftware.util.convert.BigDecimalConversions");
        java.lang.reflect.Field grandField = bdConvClass.getDeclaredField("GRAND");
        grandField.setAccessible(true);
        BigDecimal divisor = (BigDecimal) grandField.get(null);

        BigDecimal expected = new BigDecimal(utcCalendar.getTimeInMillis()).divide(divisor);
        assertEquals(0, expected.compareTo(CalendarConversions.toBigDecimal(utcCalendar, converter)));
    }

    // -----------------------------------------------------------------
    // 5. toBigInteger
    // -----------------------------------------------------------------
    @Test
    public void testToBigInteger_HappyPath() {
        BigInteger expected = BigInteger.valueOf(utcCalendar.getTimeInMillis() * 1_000_000L);
        assertEquals(expected, CalendarConversions.toBigInteger(utcCalendar, converter));
    }

    // -----------------------------------------------------------------
    // 6. toDate
    // -----------------------------------------------------------------
    @Test
    public void testToDate_HappyPath() {
        Date expected = utcCalendar.getTime();
        assertEquals(expected, CalendarConversions.toDate(utcCalendar, converter));
    }

    // -----------------------------------------------------------------
    // 7. toSqlDate
    // -----------------------------------------------------------------
    @Test
    public void testToSqlDate_HappyPath() {
        java.sql.Date expected = java.sql.Date.valueOf(
                utcCalendar.toInstant()
                           .atZone(converter.getOptions().getZoneId())
                           .toLocalDate()
        );
        assertEquals(expected, CalendarConversions.toSqlDate(utcCalendar, converter));
    }

    // -----------------------------------------------------------------
    // 8. toTimestamp
    // -----------------------------------------------------------------
    @Test
    public void testToTimestamp_HappyPath() {
        Timestamp expected = new Timestamp(utcCalendar.getTimeInMillis());
        assertEquals(expected, CalendarConversions.toTimestamp(utcCalendar, converter));
    }

    // -----------------------------------------------------------------
    // 9. toInstant
    // -----------------------------------------------------------------
    @Test
    public void testToInstant_HappyPath() {
        Instant expected = utcCalendar.toInstant();
        assertEquals(expected, CalendarConversions.toInstant(utcCalendar, converter));
    }

    // -----------------------------------------------------------------
    // 10. toZonedDateTime
    // -----------------------------------------------------------------
    @Test
    public void testToZonedDateTime_HappyPath() {
        ZonedDateTime expected = utcCalendar.toInstant()
                                            .atZone(utcCalendar.getTimeZone().toZoneId());
        assertEquals(expected, CalendarConversions.toZonedDateTime(utcCalendar, converter));
    }

    // -----------------------------------------------------------------
    // 11. toLocalDateTime
    // -----------------------------------------------------------------
    @Test
    public void testToLocalDateTime_HappyPath() {
        LocalDateTime expected = CalendarConversions.toZonedDateTime(utcCalendar, converter).toLocalDateTime();
        assertEquals(expected, CalendarConversions.toLocalDateTime(utcCalendar, converter));
    }

    // -----------------------------------------------------------------
    // 12. toOffsetDateTime
    // -----------------------------------------------------------------
    @Test
    public void testToOffsetDateTime_HappyPath() {
        Calendar cal = utcCalendar;
        int offsetSeconds = cal.getTimeZone().getOffset(cal.getTimeInMillis()) / 1000;
        OffsetDateTime expected = cal.toInstant().atOffset(ZoneOffset.ofTotalSeconds(offsetSeconds));
        assertEquals(expected, CalendarConversions.toOffsetDateTime(cal, converter));
    }

    // -----------------------------------------------------------------
    // 13. toLocalDate
    // -----------------------------------------------------------------
    @Test
    public void testToLocalDate_HappyPath() {
        LocalDate expected = CalendarConversions.toZonedDateTime(utcCalendar, converter).toLocalDate();
        assertEquals(expected, CalendarConversions.toLocalDate(utcCalendar, converter));
    }

    // -----------------------------------------------------------------
    // 14. toLocalTime
    // -----------------------------------------------------------------
    @Test
    public void testToLocalTime_HappyPath() {
        LocalTime expected = CalendarConversions.toZonedDateTime(utcCalendar, converter).toLocalTime();
        assertEquals(expected, CalendarConversions.toLocalTime(utcCalendar, converter));
    }

    // -----------------------------------------------------------------
    // 15. clone
    // -----------------------------------------------------------------
    @Test
    public void testClone_HappyPath() {
        Calendar cloned = CalendarConversions.clone(utcCalendar, converter);
        assertNotSame(utcCalendar, cloned);
        assertEquals(utcCalendar.getTimeInMillis(), cloned.getTimeInMillis());
        assertEquals(utcCalendar.getTimeZone(), cloned.getTimeZone());
    }

    // -----------------------------------------------------------------
    // 16. create
    // -----------------------------------------------------------------
    @Test
    public void testCreate_HappyPath() {
        long epoch = 1_600_000_000_000L; // some arbitrary instant
        Calendar created = CalendarConversions.create(epoch, converter);
        assertEquals(epoch, created.getTimeInMillis());
        // The time‑zone of the created calendar must match the converter's options.
        assertEquals(converter.getOptions().getTimeZone(), created.getTimeZone());
    }

    // -----------------------------------------------------------------
    // 17. toYear
    // -----------------------------------------------------------------
    @Test
    public void testToYear_HappyPath() {
        Year expected = Year.from(
                utcCalendar.toInstant()
                          .atZone(converter.getOptions().getZoneId())
                          .toLocalDate()
        );
        assertEquals(expected, CalendarConversions.toYear(utcCalendar, converter));
    }

    // -----------------------------------------------------------------
    // 18. toYearMonth
    // -----------------------------------------------------------------
    @Test
    public void testToYearMonth_HappyPath() {
        YearMonth expected = YearMonth.from(
                utcCalendar.toInstant()
                          .atZone(converter.getOptions().getZoneId())
                          .toLocalDate()
        );
        assertEquals(expected, CalendarConversions.toYearMonth(utcCalendar, converter));
    }

    // -----------------------------------------------------------------
    // 19. toMonthDay
    // -----------------------------------------------------------------
    @Test
    public void testToMonthDay_HappyPath() {
        MonthDay expected = MonthDay.from(
                utcCalendar.toInstant()
                          .atZone(converter.getOptions().getZoneId())
                          .toLocalDate()
        );
        assertEquals(expected, CalendarConversions.toMonthDay(utcCalendar, converter));
    }

    // -----------------------------------------------------------------
    // 20. toString – normal zone (UTC)
    // -----------------------------------------------------------------
    @Test
    public void testToString_WithZoneId() {
        String result = CalendarConversions.toString(utcCalendar, converter);
        // The formatter used for non‑offset zones ends with "XXX[VV]".
        ZonedDateTime zdt = CalendarConversions.toZonedDateTime(utcCalendar, converter);
        String expected = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                                            .withZone(zdt.getZone())
                                            .format(zdt) +
                          zdt.getOffset().getId() +
                          "[" + zdt.getZone().getId() + "]";
        assertEquals(expected, result);
    }

    // -----------------------------------------------------------------
    // 21. toString – abbreviation that must be expanded via DateUtilities map
    // -----------------------------------------------------------------
    @Test
    public void testToString_AbbreviationMapping() {
        // The EST calendar has an abbreviation ID ("EST") that does not contain '/'.
        // The map was populated in setUp() to resolve it to "America/New_York".
        String result = CalendarConversions.toString(estCalendar, converter);
        ZonedDateTime afterMapping = CalendarConversions.toZonedDateTime(estCalendar, converter)
                                                       .withZoneSameInstant(ZoneId.of("America/New_York"));
        String expected = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                                            .withZone(afterMapping.getZone())
                                            .format(afterMapping) +
                          afterMapping.getOffset().getId() +
                          "[" + afterMapping.getZone().getId() + "]";
        assertEquals(expected, result);
    }

    // -----------------------------------------------------------------
    // 22. toString – offset‑only zone (e.g., ZoneOffset.UTC)
    // -----------------------------------------------------------------
    @Test
    public void testToString_OffsetOnly() {
        Calendar offsetCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        offsetCal.clear();
        offsetCal.setTimeInMillis(0L); // 1970‑01‑01T00:00:00Z

        String result = CalendarConversions.toString(offsetCal, converter);
        // Expected format: yyyy-MM-dd'T'HH:mm:ss[.fraction]XXX
        assertTrue("Result should end with a Z offset", result.endsWith("Z"));
        assertTrue("Result should start with the epoch date", result.startsWith("1970-01-01T00:00:00"));
    }

    // -----------------------------------------------------------------
    // 23. toMap – verifies that the map contains a single entry whose value
    //            matches the string representation produced by toString()
    // -----------------------------------------------------------------
    @Test
    public void testToMap_HappyPath() throws Exception {
        Map<String, Object> map = CalendarConversions.toMap(utcCalendar, converter);
        assertEquals(1, map.size());

        // Obtain the key used by MapConversions.CALENDAR via reflection.
        Class<?> mapConvClass = Class.forName("com.cedarsoftware.util.convert.MapConversions");
        java.lang.reflect.Field keyField = mapConvClass.getDeclaredField("CALENDAR");
        keyField.setAccessible(true);
        String key = (String) keyField.get(null);

        assertTrue(map.containsKey(key));
        assertEquals(CalendarConversions.toString(utcCalendar, converter), map.get(key));
    }

    // -----------------------------------------------------------------
    // 24. Edge case – extremely large epoch (far future) for numeric conversions
    // -----------------------------------------------------------------
    @Test
    public void testNumericConversions_FarFutureEpoch() {
        long farFuture = Long.MAX_VALUE - 1_000_000L; // a large positive epoch
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.setTimeInMillis(farFuture);

        // toLong and toAtomicLong must retain the exact value.
        assertEquals(Long.valueOf(farFuture), CalendarConversions.toLong(cal, converter));
        assertEquals(new AtomicLong(farFuture), CalendarConversions.toAtomicLong(cal, converter));

        // toDouble should handle the large value without overflow (double has 53‑bit mantissa).
        double expectedDouble = farFuture / 1000.0;
        assertEquals(expectedDouble, CalendarConversions.toDouble(cal, converter), 0.0);
    }

    // -----------------------------------------------------------------
    // 25. Edge case – negative epoch (pre‑1970) for string formatting
    // -----------------------------------------------------------------
    @Test
    public void testToString_Pre1970() {
        Calendar pre1970 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        pre1970.clear();
        pre1970.setTimeInMillis(-123456789L); // 1966‑02‑02T02:26:31.211Z

        String result = CalendarConversions.toString(pre1970, converter);
        assertTrue("String representation of a pre‑1970 instant must contain a negative year",
                result.startsWith("-"));
    }
}
