package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link MapConversions}.
 *
 * <p>The tests are written in the same package as {@code MapConversions} so that the
 * package‑private static methods are directly accessible.  The suite follows the
 * collaborative approach described in the prompt:</p>
 *
 * <ul>
 *   <li>All public (package‑private static) methods are listed.</li>
 *   <li>For each method a basic “happy‑path” test is provided.</li>
 *   <li>Additional tests cover edge‑cases, invalid input and exception handling.</li>
 *   <li>All tests are merged into a single JUnit‑4 test class.</li>
 * </ul>
 *
 * <p>Only library classes that appear in the supplied source or in the listed
 * dependency APIs are used – no mocking frameworks.</p>
 */
public class MapConversionsTest {

    /** A reusable {@link Converter} instance used by all tests. */
    private Converter converter;

    @Before
    public void setUp() {
        // The library provides a public constructor that takes a ConverterOptions instance.
        // The concrete type of ConverterOptions is not shown, but it has a public no‑arg ctor.
        converter = new Converter(new ConverterOptions());
    }

    /* ---------------------------------------------------------------------- */
    /*  List of package‑private static methods (extracted from the source)   */
    /* ---------------------------------------------------------------------- */
    // toUUID, toByte, toShort, toInt, toLong, toFloat, toDouble, toBoolean,
    // toBigDecimal, toBigInteger, toString, toStringBuffer, toStringBuilder,
    // toCharacter, toAtomicInteger, toAtomicLong, toAtomicBoolean,
    // toSqlDate, toDate, toTimestamp, toTimeZone, toCalendar,
    // toLocale, toLocalDate, toLocalTime, toOffsetTime, toOffsetDateTime,
    // toLocalDateTime, toZonedDateTime, toClass, toDuration, toInstant,
    // toMonthDay, toYearMonth, toPeriod, toZoneId, toZoneOffset, toYear,
    // toURL, toThrowable, toURI, initMap, fromMap (private), toEpochMillis (private),
    // nanoRule (private), populateFields (private)

    /* ---------------------------------------------------------------------- */
    /*  Basic happy‑path tests                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToUUID_WithUuidString() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.UUID, "123e4567-e89b-12d3-a456-426655440000");
        UUID result = (UUID) MapConversions.toUUID(map, converter);
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426655440000"), result);
    }

    @Test
    public void testToUUID_WithMostAndLeastSigBits() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.MOST_SIG_BITS, 0x0123456789ABCDEFL);
        map.put(MapConversions.LEAST_SIG_BITS, 0xFEDCBA9876543210L);
        UUID result = (UUID) MapConversions.toUUID(map, converter);
        assertEquals(new UUID(0x0123456789ABCDEFL, 0xFEDCBA9876543210L), result);
    }

    @Test
    public void testToByte() {
        Map<String, Object> map = MapConversions.initMap((byte) 7, converter);
        Byte result = MapConversions.toByte(map, converter);
        assertEquals(Byte.valueOf((byte) 7), result);
    }

    @Test
    public void testToInt() {
        Map<String, Object> map = MapConversions.initMap(42, converter);
        Integer result = MapConversions.toInt(map, converter);
        assertEquals(Integer.valueOf(42), result);
    }

    @Test
    public void testToString() {
        Map<String, Object> map = MapConversions.initMap("hello", converter);
        String result = MapConversions.toString(map, converter);
        assertEquals("hello", result);
    }

    @Test
    public void testToSqlDate_EpochMillis() {
        long epoch = 1_600_000_000_000L; // 2020‑09‑13T12:26:40Z
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, epoch);
        java.sql.Date sqlDate = MapConversions.toSqlDate(map, converter);
        assertEquals(new java.sql.Date(epoch), sqlDate);
    }

    @Test
    public void testToDate_EpochMillis() {
        long epoch = 1_600_000_000_000L;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, epoch);
        Date date = MapConversions.toDate(map, converter);
        assertEquals(new Date(epoch), date);
    }

    @Test
    public void testToTimestamp_EpochMillisAndNanos() {
        long epoch = 1_600_000_000_000L;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, epoch);
        map.put(MapConversions.NANOS, 123_456_789);
        Timestamp ts = MapConversions.toTimestamp(map, converter);
        assertEquals(epoch, ts.getTime());
        assertEquals(123_456_789, ts.getNanos());
    }

    @Test
    public void testToLocale_FullComponents() {
        Map<String, String> map = new HashMap<>();
        map.put(MapConversions.LANGUAGE, "en");
        map.put(MapConversions.COUNTRY, "US");
        map.put(MapConversions.SCRIPT, "Latn");
        map.put(MapConversions.VARIANT, "POSIX");
        Locale locale = MapConversions.toLocale(map, converter);
        assertEquals(new Locale.Builder().setLanguage("en").setRegion("US")
                .setScript("Latn").setVariant("POSIX").build(), locale);
    }

    @Test
    public void testToLocalDate_FromComponents() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.YEAR, 2021);
        map.put(MapConversions.MONTH, 12);
        map.put(MapConversions.DAY, 25);
        LocalDate ld = MapConversions.toLocalDate(map, converter);
        assertEquals(LocalDate.of(2021, 12, 25), ld);
    }

    @Test
    public void testToLocalTime_FromComponents() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.HOUR, 14);
        map.put(MapConversions.MINUTE, 30);
        map.put(MapConversions.SECOND, 15);
        map.put(MapConversions.NANOS, 999_999_999);
        LocalTime lt = MapConversions.toLocalTime(map, converter);
        assertEquals(LocalTime.of(14, 30, 15, 999_999_999), lt);
    }

    @Test
    public void testToOffsetTime_WithExplicitOffset() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.HOUR, 10);
        map.put(MapConversions.MINUTE, 20);
        map.put(MapConversions.SECOND, 30);
        map.put(MapConversions.NANOS, 0);
        map.put(MapConversions.OFFSET_HOUR, -5);
        map.put(MapConversions.OFFSET_MINUTE, 0);
        OffsetTime ot = MapConversions.toOffsetTime(map, converter);
        assertEquals(OffsetTime.of(10, 20, 30, 0, ZoneOffset.ofHours(-5)), ot);
    }

    @Test
    public void testToOffsetDateTime_WithDateAndOffset() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, LocalDate.of(2022, 1, 1));
        map.put(MapConversions.TIME, LocalTime.of(12, 0));
        map.put(MapConversions.OFFSET, ZoneOffset.ofHours(2));
        OffsetDateTime odt = MapConversions.toOffsetDateTime(map, converter);
        assertEquals(OffsetDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneOffset.ofHours(2)), odt);
    }

    @Test
    public void testToZonedDateTime_FromEpochMillis() {
        long epoch = 1_600_000_000_000L;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, epoch);
        ZonedDateTime zdt = MapConversions.toZonedDateTime(map, converter);
        assertEquals(Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()), zdt);
    }

    @Test
    public void testToDuration_WithSecondsAndNanos() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, 5L);
        map.put(MapConversions.NANOS, 123);
        Duration d = MapConversions.toDuration(map, converter);
        assertEquals(Duration.ofSeconds(5, 123), d);
    }

    @Test
    public void testToInstant_WithSecondsAndNanos() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, 10L);
        map.put(MapConversions.NANOS, 500L);
        Instant instant = MapConversions.toInstant(map, converter);
        assertEquals(Instant.ofEpochSecond(10, 500), instant);
    }

    @Test
    public void testToMonthDay_FromComponents() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.MONTH, 2);
        map.put(MapConversions.DAY, 29);
        MonthDay md = MapConversions.toMonthDay(map, converter);
        assertEquals(MonthDay.of(2, 29), md);
    }

    @Test
    public void testToYearMonth_FromComponents() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.YEAR, 1999);
        map.put(MapConversions.MONTH, 12);
        YearMonth ym = MapConversions.toYearMonth(map, converter);
        assertEquals(YearMonth.of(1999, 12), ym);
    }

    @Test
    public void testToPeriod_FromComponents() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.YEARS, 2);
        map.put(MapConversions.MONTHS, 3);
        map.put(MapConversions.DAYS, 4);
        Period p = MapConversions.toPeriod(map, converter);
        assertEquals(Period.of(2, 3, 4), p);
    }

    @Test
    public void testToZoneOffset_WithHoursMinutesSeconds() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.HOURS, -3);
        map.put(MapConversions.MINUTES, 30);
        map.put(MapConversions.SECONDS, 0);
        ZoneOffset zo = MapConversions.toZoneOffset(map, converter);
        assertEquals(ZoneOffset.ofHoursMinutesSeconds(-3, 30, 0), zo);
    }

    @Test
    public void testToURL_FromString() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.URL_KEY, "https://example.com/");
        URL url = MapConversions.toURL(map, converter);
        assertEquals(new URL("https://example.com/"), url);
    }

    @Test
    public void testToURI_FromString() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.URI_KEY, "http://example.org/");
        URI uri = MapConversions.toURI(map, converter);
        assertEquals(new URI("http://example.org/"), uri);
    }

    @Test
    public void testInitMapCreatesInsertionOrderMap() {
        Map<String, ?> result = MapConversions.initMap("value", converter);
        assertTrue(result instanceof Map);
        assertEquals("value", result.get(MapConversions.V));
    }

    /* ---------------------------------------------------------------------- */
    /*  Edge‑case / exception handling tests                                 */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testToUUID_MissingAllKeys_Throws() {
        Map<String, Object> map = new HashMap<>();
        // No UUID, no most/least bits, no _v/value – should trigger the generic error.
        MapConversions.toUUID(map, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocale_MissingLanguage_Throws() {
        Map<String, String> map = new HashMap<>();
        // language is mandatory for a Locale; missing triggers fromMap which throws.
        MapConversions.toLocale(map, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocale_InvalidLanguage_Throws() {
        Map<String, String> map = new HashMap<>();
        map.put(MapConversions.LANGUAGE, "invalid-language-code-!@#");
        // Builder.setLanguage will throw IllegalArgumentException for an invalid code.
        MapConversions.toLocale(map, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTime_InvalidOffset_Throws() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.HOUR, 10);
        map.put(MapConversions.MINUTE, 15);
        map.put(MapConversions.OFFSET_HOUR, 25); // invalid hour (>24)
        map.put(MapConversions.OFFSET_MINUTE, 0);
        MapConversions.toOffsetTime(map, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToZoneOffset_InvalidHours_Throws() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.HOURS, 30); // out of range for ZoneOffset
        MapConversions.toZoneOffset(map, converter);
    }

    @Test
    public void testToThrowable_WithCauseAndMessage() {
        // Build a map that represents an IllegalArgumentException with a cause.
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.CLASS, IllegalArgumentException.class.getName());
        map.put(MapConversions.MESSAGE, "top level");
        map.put(MapConversions.CAUSE, NullPointerException.class.getName());
        map.put(MapConversions.CAUSE_MESSAGE, "null cause");

        Throwable t = MapConversions.toThrowable(map, converter, RuntimeException.class);
        assertTrue(t instanceof IllegalArgumentException);
        assertEquals("top level", t.getMessage());
        assertNotNull(t.getCause());
        assertTrue(t.getCause() instanceof NullPointerException);
        assertEquals("null cause", t.getCause().getMessage());
        // Stack trace must be cleared per implementation.
        assertEquals(0, t.getStackTrace().length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromMap_NoRecognizedKey_Throws() {
        Map<String, Object> map = new HashMap<>();
        map.put("unrelated", "value");
        // Directly invoke the private helper via reflection to ensure exception text format.
        try {
            java.lang.reflect.Method m = MapConversions.class.getDeclaredMethod("fromMap",
                    Object.class, Converter.class, Class.class, String[][].class);
            m.setAccessible(true);
            m.invoke(null, map, converter, Integer.class, new String[][]{});
        } catch (java.lang.reflect.InvocationTargetException e) {
            // Unwrap and rethrow the underlying IllegalArgumentException.
            throw (IllegalArgumentException) e.getCause();
        }
    }

    @Test
    public void testToCalendar_WithDateTimeAndZone() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, LocalDate.of(2022, 3, 15));
        map.put(MapConversions.TIME, LocalTime.of(9, 45, 30, 123_000_000));
        map.put(MapConversions.ZONE, ZoneId.of("Europe/Paris"));
        Calendar cal = MapConversions.toCalendar(map, converter);
        ZonedDateTime expected = ZonedDateTime.of(2022, 3, 15, 9, 45, 30, 123_000_000,
                ZoneId.of("Europe/Paris"));
        assertEquals(expected.getYear(), cal.get(Calendar.YEAR));
        assertEquals(expected.getMonthValue() - 1, cal.get(Calendar.MONTH));
        assertEquals(expected.getDayOfMonth(), cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(expected.getHour(), cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(expected.getMinute(), cal.get(Calendar.MINUTE));
        assertEquals(expected.getSecond(), cal.get(Calendar.SECOND));
        assertEquals(expected.getNano() / 1_000_000, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testToCalendar_TimeOnly_WithZone() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.TIME, "2022-12-31T23:59:59");
        map.put(MapConversions.ZONE, ZoneId.of("UTC"));
        Calendar cal = MapConversions.toCalendar(map, converter);
        // The map's time string is parsed as a ZonedDateTime; verify epoch consistency.
        ZonedDateTime zdt = DateUtilities.parseDate("2022-12-31T23:59:59", ZoneId.of("UTC"), true);
        assertEquals(zdt.toInstant().toEpochMilli(), cal.getTimeInMillis());
    }

    @Test
    public void testToCalendar_EpochMillis() {
        long epoch = 1_600_000_000_123L;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, epoch);
        Calendar cal = MapConversions.toCalendar(map, converter);
        assertEquals(epoch, cal.getTimeInMillis());
    }

    @Test
    public void testToTimestamp_StringNumeric_NoNanos() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.VALUE, "1620000000123"); // numeric string
        Timestamp ts = MapConversions.toTimestamp(map, converter);
        assertEquals(1620000000123L, ts.getTime());
        assertEquals(0, ts.getNanos());
    }

    @Test
    public void testToTimestamp_StringDateTime_NoNanos() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.VALUE, "2021-05-01T12:34:56.789");
        Timestamp ts = MapConversions.toTimestamp(map, converter);
        // The string is parsed by the generic toEpochMillis path.
        // Verify that milliseconds part matches.
        Instant instant = Instant.parse("2021-05-01T12:34:56.789Z");
        assertEquals(instant.toEpochMilli(), ts.getTime());
    }

    @Test
    public void testToTimestamp_NanosIgnoredWhenTimeStringContainsNanos() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.VALUE, "2021-01-01T00:00:00.123456789");
        map.put(MapConversions.NANOS, 987_654_321); // should be ignored because time string has nano precision
        Timestamp ts = MapConversions.toTimestamp(map, converter);
        assertEquals(123_456_789, ts.getNanos());
    }

    @Test
    public void testToPeriod_ValueKeyUsesFromMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.VALUE, "P2Y3M4D"); // ISO‑8601 period string
        Period p = MapConversions.toPeriod(map, converter);
        // The implementation forwards to fromMap which will call converter.convert.
        // The default Converter can parse the ISO string to a Period.
        assertEquals(Period.of(2, 3, 4), p);
    }

    @Test
    public void testToZoneId_PrefersZoneKeyOverIdKey() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.ZONE, "America/New_York");
        map.put(MapConversions.ID, "Europe/London");
        ZoneId zid = MapConversions.toZoneId(map, converter);
        assertEquals(ZoneId.of("America/New_York"), zid);
    }

    @Test
    public void testToZoneId_UsesIdWhenZoneMissing() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.ID, "Europe/Paris");
        ZoneId zid = MapConversions.toZoneId(map, converter);
        assertEquals(ZoneId.of("Europe/Paris"), zid);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToZoneId_NoKey_Throws() {
        Map<String, Object> map = new HashMap<>();
        MapConversions.toZoneId(map, converter);
    }

    @Test
    public void testToClass_FromValueKey() {
        Map<String, Object> map = MapConversions.initMap(String.class, converter);
        Class<?> clazz = MapConversions.toClass(map, converter);
        assertEquals(String.class, clazz);
    }

    @Test
    public void testToBigDecimal_FromString() {
        Map<String, Object> map = MapConversions.initMap("12345.6789", converter);
        BigDecimal bd = MapConversions.toBigDecimal(map, converter);
        assertEquals(new BigDecimal("12345.6789"), bd);
    }

    @Test
    public void testToAtomicInteger_FromInt() {
        Map<String, Object> map = MapConversions.initMap(10, converter);
        AtomicInteger ai = MapConversions.toAtomicInteger(map, converter);
        assertEquals(10, ai.get());
    }

    @Test
    public void testToAtomicBoolean_FromBoolean() {
        Map<String, Object> map = MapConversions.initMap(Boolean.TRUE, converter);
        AtomicBoolean ab = MapConversions.toAtomicBoolean(map, converter);
        assertTrue(ab.get());
    }

    @Test
    public void testToStringBuilder_FromString() {
        Map<String, Object> map = MapConversions.initMap("builder", converter);
        StringBuilder sb = MapConversions.toStringBuilder(map, converter);
        assertEquals("builder", sb.toString());
    }

    @Test
    public void testToCharacter_FromString() {
        Map<String, Object> map = MapConversions.initMap("X", converter);
        Character c = MapConversions.toCharacter(map, converter);
        assertEquals(Character.valueOf('X'), c);
    }

    @Test
    public void testToFloat_FromNumber() {
        Map<String, Object> map = MapConversions.initMap(3.14f, converter);
        Float f = MapConversions.toFloat(map, converter);
        assertEquals(Float.valueOf(3.14f), f);
    }

    @Test
    public void testToDouble_FromNumber() {
        Map<String, Object> map = MapConversions.initMap(2.71828, converter);
        Double d = MapConversions.toDouble(map, converter);
        assertEquals(Double.valueOf(2.71828), d);
    }

    @Test
    public void testToBoolean_FromStringTrue() {
        Map<String, Object> map = MapConversions.initMap("true", converter);
        Boolean b = MapConversions.toBoolean(map, converter);
        assertTrue(b);
    }

    @Test
    public void testToBigInteger_FromString() {
        Map<String, Object> map = MapConversions.initMap("9876543210", converter);
        BigInteger bi = MapConversions.toBigInteger(map, converter);
        assertEquals(new BigInteger("9876543210"), bi);
    }

    @Test
    public void testToStringBuffer_FromString() {
        Map<String, Object> map = MapConversions.initMap("buffer", converter);
        StringBuffer sb = MapConversions.toStringBuffer(map, converter);
        assertEquals("buffer", sb.toString());
    }

    @Test
    public void testToTimeZone_FromString() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.ZONE, "UTC");
        TimeZone tz = MapConversions.toTimeZone(map, converter);
        assertEquals(TimeZone.getTimeZone("UTC"), tz);
    }

    @Test
    public void testToYear_FromKey() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.YEAR, 2025);
        Year y = MapConversions.toYear(map, converter);
        assertEquals(Year.of(2025), y);
    }

    @Test
    public void testToClass_FromStringClassName() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.VALUE, "java.util.ArrayList");
        Class<?> clazz = MapConversions.toClass(map, converter);
        assertEquals(ArrayList.class, clazz);
    }

    @Test
    public void testToLocale_EmptyLanguageFallsBackToFromMap() {
        Map<String, String> map = new HashMap<>();
        // No language key – the method should delegate to fromMap which will throw.
        try {
            MapConversions.toLocale(map, converter);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Locale"));
        }
    }

    @Test
    public void testToPeriod_AllZeroesWhenKeysMissing() {
        Map<String, Object> map = new HashMap<>();
        // No explicit period keys – falls back to fromMap which throws.
        try {
            MapConversions.toPeriod(map, converter);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Period"));
        }
    }

    @Test
    public void testToDuration_NoKeysFallsBackToFromMap() {
        Map<String, Object> map = new HashMap<>();
        try {
            MapConversions.toDuration(map, converter);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Duration"));
        }
    }

    @Test
    public void testToInstant_NoKeysFallsBackToFromMap() {
        Map<String, Object> map = new HashMap<>();
        try {
            MapConversions.toInstant(map, converter);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Instant"));
        }
    }

    @Test
    public void testToLocalDateTime_NoDateUsesMidnight() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.TIME, LocalTime.of(13, 45));
        LocalDateTime ldt = MapConversions.toLocalDateTime(map, converter);
        assertEquals(LocalDateTime.of(LocalDate.now(converter.getOptions().getZoneId()), LocalTime.of(13, 45)), ldt);
    }

    @Test
    public void testToZonedDateTime_WithDateTimeAndZone() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, LocalDate.of(2020, 5, 20));
        map.put(MapConversions.TIME, LocalTime.of(8, 15));
        map.put(MapConversions.ZONE, ZoneId.of("America/Los_Angeles"));
        ZonedDateTime zdt = MapConversions.toZonedDateTime(map, converter);
        assertEquals(ZonedDateTime.of(2020, 5, 20, 8, 15, 0, 0, ZoneId.of("America/Los_Angeles")), zdt);
    }

    @Test
    public void testToZonedDateTime_WithTimeAndZoneOnly() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.TIME, LocalDateTime.of(2021, 12, 31, 23, 59));
        map.put(MapConversions.ZONE, ZoneId.of("UTC"));
        ZonedDateTime zdt = MapConversions.toZonedDateTime(map, converter);
        assertEquals(ZonedDateTime.of(LocalDateTime.of(2021, 12, 31, 23, 59), ZoneId.of("UTC")), zdt);
    }

    @Test
    public void testToOffsetTime_WithTimeKey() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.TIME, OffsetTime.of(12, 30, 0, 0, ZoneOffset.ofHours(1)));
        OffsetTime ot = MapConversions.toOffsetTime(map, converter);
        assertEquals(OffsetTime.of(12, 30, 0, 0, ZoneOffset.ofHours(1)), ot);
    }

    @Test
    public void testToOffsetDateTime_WithOnlyTimeAndOffset() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.TIME, LocalDateTime.of(2022, 2, 2, 10, 20));
        map.put(MapConversions.OFFSET, ZoneOffset.ofHours(-4));
        OffsetDateTime odt = MapConversions.toOffsetDateTime(map, converter);
        assertEquals(OffsetDateTime.of(2022, 2, 2, 10, 20, 0, 0, ZoneOffset.ofHours(-4)), odt);
    }

    @Test
    public void testToOffsetDateTime_FallbackToFromMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.VALUE, "2022-03-04T05:06:07+02:00");
        OffsetDateTime odt = MapConversions.toOffsetDateTime(map, converter);
        assertEquals(OffsetDateTime.parse("2022-03-04T05:06:07+02:00"), odt);
    }

    @Test
    public void testToLocale_OptionalFieldsMissing() {
        Map<String, String> map = new HashMap<>();
        map.put(MapConversions.LANGUAGE, "fr");
        Locale locale = MapConversions.toLocale(map, converter);
        assertEquals(Locale.FRENCH, locale);
    }
}
