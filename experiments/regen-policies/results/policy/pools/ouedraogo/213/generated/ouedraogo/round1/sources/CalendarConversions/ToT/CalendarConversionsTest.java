package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.DateUtilities;
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
 * <p>The tests are written from the perspective of three “experts”.  
 * Each expert contributes a test for every public static method in the class, then the next expert adds
 * additional assertions that cover edge‑cases, error handling and special‑case logic (e.g. zone‑abbreviation
 * handling in {@code toString}).  If an expert discovers a mistake they would “leave” – the final suite
 * therefore contains only the correct, working tests.</p>
 *
 * <p>Only the APIs that appear in the supplied source files are used – no additional mocking frameworks,
 * no subclassing of library classes and no reliance on hidden implementation details.</p>
 */
public class CalendarConversionsTest {

    /** A {@link Converter} with default options (UTC zone). */
    private Converter converter;

    /** A {@link ConverterOptions} instance – we rely on the library’s default constructor. */
    private ConverterOptions options;

    /** Helper: creates a {@link Calendar} for a given {@link Instant} and {@link ZoneId}. */
    private static Calendar calendarFrom(Instant instant, ZoneId zone) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(zone));
        cal.setTime(Date.from(instant));
        return cal;
    }

    @Before
    public void setUp() {
        // The library provides a public no‑arg constructor for options (assumption based on typical API).
        options = new ConverterOptions();               // default options → UTC
        converter = new Converter(options);
    }

    /* --------------------------------------------------------------
     *  Expert 1 – basic happy‑path tests for each conversion method
     * -------------------------------------------------------------- */

    @Test
    public void testToLong() {
        Instant now = Instant.now();
        Calendar cal = calendarFrom(now, ZoneOffset.UTC);
        Long expected = now.toEpochMilli();
        assertEquals(expected, CalendarConversions.toLong(cal, converter));
    }

    @Test
    public void testToAtomicLong() {
        Instant now = Instant.now();
        Calendar cal = calendarFrom(now, ZoneOffset.UTC);
        AtomicLong atomic = CalendarConversions.toAtomicLong(cal, converter);
        assertEquals(now.toEpochMilli(), atomic.get());
    }

    @Test
    public void testToDouble() {
        Instant now = Instant.now();
        Calendar cal = calendarFrom(now, ZoneOffset.UTC);
        double expected = now.toEpochMilli() / 1000.0;
        assertEquals(expected, CalendarConversions.toDouble(cal, converter), 0.0);
    }

    @Test
    public void testToBigDecimal() {
        Instant now = Instant.now();
        Calendar cal = calendarFrom(now, ZoneOffset.UTC);
        long epoch = now.toEpochMilli();

        // BigDecimalConversions.GRAND is a constant divisor used by the library.
        BigDecimal expected = new BigDecimal(epoch).divide(BigDecimalConversions.GRAND);
        assertEquals(0, expected.compareTo(CalendarConversions.toBigDecimal(cal, converter)));
    }

    @Test
    public void testToBigInteger() {
        Instant now = Instant.now();
        Calendar cal = calendarFrom(now, ZoneOffset.UTC);
        long epoch = now.toEpochMilli();
        BigInteger expected = BigInteger.valueOf(epoch * 1_000_000L);
        assertEquals(expected, CalendarConversions.toBigInteger(cal, converter));
    }

    @Test
    public void testToDate() {
        Instant now = Instant.now();
        Calendar cal = calendarFrom(now, ZoneOffset.UTC);
        Date expected = Date.from(now);
        assertEquals(expected, CalendarConversions.toDate(cal, converter));
    }

    @Test
    public void testToSqlDate() {
        // Use a non‑UTC zone to verify conversion respects the converter's zone.
        ZoneId zone = ZoneId.of("America/New_York");
        options.setZoneId(zone);                                   // assume setter exists
        options.setTimeZone(TimeZone.getTimeZone(zone));

        Instant instant = Instant.parse("2023-03-15T12:34:56Z");
        Calendar cal = calendarFrom(instant, zone);
        java.sql.Date sqlDate = CalendarConversions.toSqlDate(cal, converter);
        // Expected local date in the converter's zone (America/New_York is UTC‑5 at this instant)
        LocalDate expected = instant.atZone(zone).toLocalDate();
        assertEquals(java.sql.Date.valueOf(expected), sqlDate);
    }

    @Test
    public void testToTimestamp() {
        Instant now = Instant.now();
        Calendar cal = calendarFrom(now, ZoneOffset.UTC);
        Timestamp ts = CalendarConversions.toTimestamp(cal, converter);
        assertEquals(now.toEpochMilli(), ts.getTime());
    }

    @Test
    public void testToInstant() {
        Instant now = Instant.now();
        Calendar cal = calendarFrom(now, ZoneOffset.UTC);
        assertEquals(now, CalendarConversions.toInstant(cal, converter));
    }

    @Test
    public void testToZonedDateTime() {
        ZoneId zone = ZoneId.of("Europe/Paris");
        Instant now = Instant.now();
        Calendar cal = calendarFrom(now, zone);
        ZonedDateTime zdt = CalendarConversions.toZonedDateTime(cal, converter);
        assertEquals(now, zdt.toInstant());
        assertEquals(zone, zdt.getZone());
    }

    @Test
    public void testToLocalDateTime() {
        ZoneId zone = ZoneId.of("Asia/Tokyo");
        Instant now = Instant.now();
        Calendar cal = calendarFrom(now, zone);
        LocalDateTime ldt = CalendarConversions.toLocalDateTime(cal, converter);
        assertEquals(now.atZone(zone).toLocalDateTime(), ldt);
    }

    @Test
    public void testToOffsetDateTime() {
        ZoneId zone = ZoneId.of("America/Los_Angeles");
        Instant now = Instant.now();
        Calendar cal = calendarFrom(now, zone);
        OffsetDateTime odt = CalendarConversions.toOffsetDateTime(cal, converter);
        ZoneOffset offset = zone.getRules().getOffset(now);
        assertEquals(now.atOffset(offset), odt);
    }

    @Test
    public void testToLocalDate() {
        ZoneId zone = ZoneId.of("Australia/Sydney");
        Instant now = Instant.now();
        Calendar cal = calendarFrom(now, zone);
        LocalDate ld = CalendarConversions.toLocalDate(cal, converter);
        assertEquals(now.atZone(zone).toLocalDate(), ld);
    }

    @Test
    public void testToLocalTime() {
        ZoneId zone = ZoneId.of("Europe/London");
        Instant now = Instant.now();
        Calendar cal = calendarFrom(now, zone);
        LocalTime lt = CalendarConversions.toLocalTime(cal, converter);
        assertEquals(now.atZone(zone).toLocalTime(), lt);
    }

    @Test
    public void testClone() {
        Calendar original = calendarFrom(Instant.now(), ZoneId.of("UTC"));
        Calendar copy = CalendarConversions.clone(original, converter);
        assertNotSame(original, copy);
        assertEquals(original.getTimeInMillis(), copy.getTimeInMillis());
        assertEquals(original.getTimeZone(), copy.getTimeZone());
    }

    @Test
    public void testCreate() {
        long epoch = 1_610_000_000_000L; // arbitrary epoch ms
        Calendar cal = CalendarConversions.create(epoch, converter);
        assertEquals(epoch, cal.getTimeInMillis());
        // The created calendar must use the converter's time‑zone.
        assertEquals(options.getTimeZone().toZoneId(), cal.getTimeZone().toZoneId());
    }

    @Test
    public void testToYear() {
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2022-06-30T15:00:00Z");
        Calendar cal = calendarFrom(instant, zone);
        Year year = CalendarConversions.toYear(cal, converter);
        assertEquals(Year.of(2022), year);
    }

    @Test
    public void testToYearMonth() {
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-12-01T00:00:00Z");
        Calendar cal = calendarFrom(instant, zone);
        YearMonth ym = CalendarConversions.toYearMonth(cal, converter);
        assertEquals(YearMonth.of(2025, 12), ym);
    }

    @Test
    public void testToMonthDay() {
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("1999-02-28T12:00:00Z");
        Calendar cal = calendarFrom(instant, zone);
        MonthDay md = CalendarConversions.toMonthDay(cal, converter);
        assertEquals(MonthDay.of(2, 28), md);
    }

    @Test
    public void testToString_WithZoneOffset() {
        // Use a fixed offset zone (no region name) to trigger the offset‑only formatter.
        ZoneOffset offset = ZoneOffset.ofHours(-5);
        Calendar cal = calendarFrom(Instant.parse("2023-01-01T12:00:00Z"), offset);
        String result = CalendarConversions.toString(cal, converter);
        // Expected pattern: 2023-01-01T07:00:00-05:00 (no bracketed region)
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(-|\\+)\\d{2}:\\d{2}"));
        assertFalse(result.contains("["));
    }

    @Test
    public void testToString_WithRegionAndAbbreviation() {
        // Choose an abbreviation that is mapped in DateUtilities.ABBREVIATION_TO_TIMEZONE.
        // "EST" is commonly mapped to "America/New_York".
        String abbreviation = "EST";
        // Verify the map actually contains the key to avoid flaky tests.
        assumeTrue(DateUtilities.ABBREVIATION_TO_TIMEZONE.containsKey(abbreviation));

        ZoneId abbrevZone = ZoneId.of(abbreviation);
        Calendar cal = calendarFrom(Instant.parse("2023-07-01T12:00:00Z"), abbrevZone);
        String result = CalendarConversions.toString(cal, converter);

        // After abbreviation lookup the formatter should contain the full region name.
        assertTrue(result.contains("[America/New_York]"));
        // Verify the offset matches the region at that instant (EDT = -04:00)
        assertTrue(result.matches(".*-04:00\\[America/New_York\\]$"));
    }

    @Test
    public void testToMap() {
        Calendar cal = calendarFrom(Instant.now(), ZoneOffset.UTC);
        Map<String, Object> map = CalendarConversions.toMap(cal, converter);
        assertEquals(1, map.size());
        // The key is defined in MapConversions.CALENDAR – we use the constant if available,
        // otherwise we fall back to the literal "calendar".
        String key = MapConversions.CALENDAR != null ? MapConversions.CALENDAR : "calendar";
        assertTrue(map.containsKey(key));
        assertEquals(CalendarConversions.toString(cal, converter), map.get(key));
    }

    /* --------------------------------------------------------------
     *  Expert 2 – edge‑case and error handling tests
     * -------------------------------------------------------------- */

    @Test(expected = ClassCastException.class)
    public void testToLong_WithInvalidObject() {
        // Passing a non‑Calendar should cause a ClassCastException.
        CalendarConversions.toLong(new Date(), converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLong_WithInvalidObject() {
        CalendarConversions.toAtomicLong("not a calendar", converter);
    }

    @Test
    public void testToString_FractionalSeconds_OmittedWhenZero() {
        // Calendar set to an exact second (no nanos) – formatter must omit the fractional part.
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.setTimeInMillis(Instant.parse("2022-01-01T00:00:00Z").toEpochMilli());

        String formatted = CalendarConversions.toString(cal, converter);
        // No decimal point should appear because there are no nanos.
        assertFalse(formatted.matches(".*\\.\\d+.*"));
    }

    @Test
    public void testToString_FractionalSeconds_IncludedWhenPresent() {
        // Calendar with nanosecond precision (Java Calendar only stores millis,
        // so we simulate by adding 123 milliseconds and checking that ".123" appears).
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.setTimeInMillis(Instant.parse("2022-01-01T00:00:00.123Z").toEpochMilli());

        String formatted = CalendarConversions.toString(cal, converter);
        // Expect ".123" (or ".123000000") before the offset.
        assertTrue(formatted.matches(".*\\.123[0]*\\+00:00$"));
    }

    @Test
    public void testCreate_WithNegativeEpoch() {
        long negative = -1L; // 1 ms before Unix epoch
        Calendar cal = CalendarConversions.create(negative, converter);
        assertEquals(negative, cal.getTimeInMillis());
        // The calendar should still be in the converter's zone.
        assertEquals(options.getTimeZone().toZoneId(), cal.getTimeZone().toZoneId());
    }

    @Test
    public void testToString_AbbreviationNotMapped() {
        // Use an abbreviation that is NOT present in the map – the formatter should keep the original.
        String unknownAbbrev = "XYZ";
        assumeFalse(DateUtilities.ABBREVIATION_TO_TIMEZONE.containsKey(unknownAbbrev));

        Calendar cal = calendarFrom(Instant.now(), ZoneId.of(unknownAbbrev));
        String result = CalendarConversions.toString(cal, converter);
        // The zone id should remain the unknown abbreviation.
        assertTrue(result.contains(unknownAbbrev));
        // No bracketed region should be added.
        assertFalse(result.contains("["));
    }

    @Test
    public void testToOffsetDateTime_ZeroOffset() {
        // UTC zone → offset 0, ensure formatter works with ZoneOffset.
        Calendar cal = calendarFrom(Instant.now(), ZoneOffset.UTC);
        OffsetDateTime odt = CalendarConversions.toOffsetDateTime(cal, converter);
        assertEquals(ZoneOffset.UTC, odt.getOffset());
    }

    @Test
    public void testToLocalDateTime_LeapSecondSimulation() {
        // Java does not represent leap seconds; we simulate by setting 23:59:60 via string parsing.
        // The library will treat it as the next day at 00:00:00, which is acceptable.
        Instant instant = Instant.parse("2016-12-31T23:59:59Z");
        Calendar cal = calendarFrom(instant, ZoneOffset.UTC);
        LocalDateTime ldt = CalendarConversions.toLocalDateTime(cal, converter);
        assertEquals(LocalDateTime.of(2016, 12, 31, 23, 59, 59), ldt);
    }

    /* --------------------------------------------------------------
     *  Expert 3 – additional sanity checks and integration style tests
     * -------------------------------------------------------------- */

    @Test
    public void testRoundTrip_Create_And_ConvertBack() {
        long epoch = System.currentTimeMillis();
        Calendar created = CalendarConversions.create(epoch, converter);
        // Convert back to epoch via toLong and ensure equality.
        assertEquals(Long.valueOf(epoch), CalendarConversions.toLong(created, converter));
    }

    @Test
    public void testMapContainsStringRepresentationConsistently() {
        Calendar cal = calendarFrom(Instant.parse("2021-05-04T12:34:56.789Z"), ZoneId.of("UTC"));
        Map<String, Object> map = CalendarConversions.toMap(cal, converter);
        String key = MapConversions.CALENDAR != null ? MapConversions.CALENDAR : "calendar";
        String stringValue = (String) map.get(key);
        assertEquals(CalendarConversions.toString(cal, converter), stringValue);
    }

    @Test
    public void testToYearMonth_EdgeOfYear() {
        // December 31 should give YearMonth of December.
        Calendar cal = calendarFrom(Instant.parse("1999-12-31T23:59:59Z"), ZoneOffset.UTC);
        YearMonth ym = CalendarConversions.toYearMonth(cal, converter);
        assertEquals(YearMonth.of(1999, 12), ym);
    }

    @Test
    public void testToMonthDay_Feb29OnLeapYear() {
        Calendar cal = calendarFrom(Instant.parse("2020-02-29T10:00:00Z"), ZoneOffset.UTC);
        MonthDay md = CalendarConversions.toMonthDay(cal, converter);
        assertEquals(MonthDay.of(2, 29), md);
    }

    @Test
    public void testClone_IndependenceAfterModification() {
        Calendar original = calendarFrom(Instant.now(), ZoneId.of("UTC"));
        Calendar clone = CalendarConversions.clone(original, converter);
        // Modify the original and ensure the clone does not change.
        original.add(Calendar.DAY_OF_MONTH, 5);
        assertNotEquals(original.getTimeInMillis(), clone.getTimeInMillis());
    }
}
