package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
import com.cedarsoftware.util.DateUtilities;
import com.cedarsoftware.util.StringUtilities;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Constructor;
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
 * <p>
 * The tests cover:
 * <ul>
 *   <li>All public static conversion methods – a basic “happy path” test for each.</li>
 *   <li>Edge‑case handling such as missing keys, malformed values and illegal arguments.</li>
 *   <li>Exception‑throwing paths (e.g. {@code toLocale} with an invalid language, {@code toOffsetTime}
 *       with an illegal offset, {@code toThrowable} with a non‑existent class, etc.).</li>
 *   <li>The {@code initMap} factory method.</li>
 * </ul>
 *
 * The real {@link Converter} implementation is used (no mocking).  It is assumed that the
 * library’s default {@code ConverterOptions} constructor creates a usable options object.
 */
public class MapConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The library provides a no‑arg constructor for ConverterOptions.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /* --------------------------------------------------------------
     * Helper utilities
     * -------------------------------------------------------------- */

    /** Shortcut to create a mutable map backed by {@link CompactLinkedMap}. */
    private static CompactLinkedMap<String, Object> map(Object... keyVals) {
        CompactLinkedMap<String, Object> m = new CompactLinkedMap<>();
        for (int i = 0; i < keyVals.length; i += 2) {
            @SuppressWarnings("unchecked")
            String key = (String) keyVals[i];
            Object val = keyVals[i + 1];
            m.put(key, val);
        }
        return m;
    }

    /* --------------------------------------------------------------
     * 1.  Basic “happy‑path” tests for each conversion method
     * -------------------------------------------------------------- */

    @Test
    public void testToUUID_direct() {
        UUID expected = UUID.randomUUID();
        CompactLinkedMap<String, Object> m = map(MapConversions.UUID, expected);
        Object result = MapConversions.toUUID(m, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToUUID_mostLeastBits() {
        long most = 0x0123456789ABCDEFL;
        long least = 0xFEDCBA9876543210L;
        CompactLinkedMap<String, Object> m = map(
                MapConversions.MOST_SIG_BITS, most,
                MapConversions.LEAST_SIG_BITS, least);
        UUID uuid = (UUID) MapConversions.toUUID(m, converter);
        assertEquals(new UUID(most, least), uuid);
    }

    @Test
    public void testToByte() {
        CompactLinkedMap<String, Object> m = map(MapConversions.VALUE, (byte) 7);
        Byte b = MapConversions.toByte(m, converter);
        assertEquals(Byte.valueOf((byte) 7), b);
    }

    @Test
    public void testToShort() {
        CompactLinkedMap<String, Object> m = map(MapConversions.VALUE, (short) 123);
        Short s = MapConversions.toShort(m, converter);
        assertEquals(Short.valueOf((short) 123), s);
    }

    @Test
    public void testToInt() {
        CompactLinkedMap<String, Object> m = map(MapConversions.VALUE, 42);
        Integer i = MapConversions.toInt(m, converter);
        assertEquals(Integer.valueOf(42), i);
    }

    @Test
    public void testToLong() {
        CompactLinkedMap<String, Object> m = map(MapConversions.VALUE, 123456789L);
        Long l = MapConversions.toLong(m, converter);
        assertEquals(Long.valueOf(123456789L), l);
    }

    @Test
    public void testToFloat() {
        CompactLinkedMap<String, Object> m = map(MapConversions.VALUE, 3.14f);
        Float f = MapConversions.toFloat(m, converter);
        assertEquals(Float.valueOf(3.14f), f);
    }

    @Test
    public void testToDouble() {
        CompactLinkedMap<String, Object> m = map(MapConversions.VALUE, 2.71828);
        Double d = MapConversions.toDouble(m, converter);
        assertEquals(Double.valueOf(2.71828), d);
    }

    @Test
    public void testToBoolean() {
        CompactLinkedMap<String, Object> m = map(MapConversions.VALUE, true);
        Boolean b = MapConversions.toBoolean(m, converter);
        assertTrue(b);
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal expected = new BigDecimal("12345.6789");
        CompactLinkedMap<String, Object> m = map(MapConversions.VALUE, expected);
        BigDecimal bd = MapConversions.toBigDecimal(m, converter);
        assertEquals(expected, bd);
    }

    @Test
    public void testToBigInteger() {
        BigInteger expected = new BigInteger("987654321");
        CompactLinkedMap<String, Object> m = map(MapConversions.VALUE, expected);
        BigInteger bi = MapConversions.toBigInteger(m, converter);
        assertEquals(expected, bi);
    }

    @Test
    public void testToString() {
        CompactLinkedMap<String, Object> m = map(MapConversions.VALUE, "hello");
        String s = MapConversions.toString(m, converter);
        assertEquals("hello", s);
    }

    @Test
    public void testToStringBuffer() {
        CompactLinkedMap<String, Object> m = map(MapConversions.VALUE, "buf");
        StringBuffer sb = MapConversions.toStringBuffer(m, converter);
        assertEquals("buf", sb.toString());
    }

    @Test
    public void testToStringBuilder() {
        CompactLinkedMap<String, Object> m = map(MapConversions.VALUE, "builder");
        StringBuilder sb = MapConversions.toStringBuilder(m, converter);
        assertEquals("builder", sb.toString());
    }

    @Test
    public void testToCharacter() {
        CompactLinkedMap<String, Object> m = map(MapConversions.VALUE, 'X');
        Character c = MapConversions.toCharacter(m, converter);
        assertEquals(Character.valueOf('X'), c);
    }

    @Test
    public void testToAtomicInteger() {
        CompactLinkedMap<String, Object> m = map(MapConversions.VALUE, 10);
        AtomicInteger ai = MapConversions.toAtomicInteger(m, converter);
        assertEquals(10, ai.get());
    }

    @Test
    public void testToAtomicLong() {
        CompactLinkedMap<String, Object> m = map(MapConversions.VALUE, 123L);
        AtomicLong al = MapConversions.toAtomicLong(m, converter);
        assertEquals(123L, al.get());
    }

    @Test
    public void testToAtomicBoolean() {
        CompactLinkedMap<String, Object> m = map(MapConversions.VALUE, true);
        AtomicBoolean ab = MapConversions.toAtomicBoolean(m, converter);
        assertTrue(ab.get());
    }

    @Test
    public void testToSqlDate_fromEpochMillis() {
        long epoch = 1_600_000_000_000L; // 2020‑09‑13T12:26:40Z
        CompactLinkedMap<String, Object> m = map(MapConversions.EPOCH_MILLIS, epoch);
        java.sql.Date d = MapConversions.toSqlDate(m, converter);
        assertEquals(new java.sql.Date(epoch), d);
    }

    @Test
    public void testToDate_fromEpochMillis() {
        long epoch = 1_600_000_000_123L; // includes milliseconds
        CompactLinkedMap<String, Object> m = map(MapConversions.EPOCH_MILLIS, epoch);
        Date d = MapConversions.toDate(m, converter);
        assertEquals(new Date(epoch), d);
    }

    @Test
    public void testToTimestamp_withEpochAndNanos() {
        long epoch = 1_600_000_000_000L;
        int nanos = 123_456_789;
        CompactLinkedMap<String, Object> m = map(
                MapConversions.EPOCH_MILLIS, epoch,
                MapConversions.NANOS, nanos);
        Timestamp ts = MapConversions.toTimestamp(m, converter);
        assertEquals(epoch, ts.getTime());
        assertEquals(nanos, ts.getNanos());
    }

    @Test
    public void testToTimestamp_fromDateTimeComponents() {
        // 2020‑01‑02T03:04:05.006Z
        CompactLinkedMap<String, Object> m = map(
                MapConversions.DATE, LocalDate.of(2020, 1, 2),
                MapConversions.TIME, LocalTime.of(3, 4, 5, 6_000_000),
                MapConversions.ZONE, ZoneOffset.UTC);
        Timestamp ts = MapConversions.toTimestamp(m, converter);
        assertEquals(Instant.parse("2020-01-02T03:04:05.006Z").toEpochMilli(), ts.getTime());
        assertEquals(6_000_000, ts.getNanos());
    }

    @Test
    public void testToTimeZone() {
        CompactLinkedMap<String, Object> m = map(MapConversions.ZONE, "America/New_York");
        TimeZone tz = MapConversions.toTimeZone(m, converter);
        assertEquals(TimeZone.getTimeZone("America/New_York"), tz);
    }

    @Test
    public void testToCalendar_fromEpochMillis() {
        long epoch = 1_600_000_123_456L;
        CompactLinkedMap<String, Object> m = map(MapConversions.EPOCH_MILLIS, epoch);
        Calendar cal = MapConversions.toCalendar(m, converter);
        assertEquals(epoch, cal.getTimeInMillis());
    }

    @Test
    public void testToLocale_fullBuilder() {
        CompactLinkedMap<String, Object> m = map(
                MapConversions.LANGUAGE, "en",
                MapConversions.COUNTRY, "US",
                MapConversions.SCRIPT, "Latn",
                MapConversions.VARIANT, "POSIX");
        Locale loc = MapConversions.toLocale(m, converter);
        assertEquals("en", loc.getLanguage());
        assertEquals("US", loc.getCountry());
        assertEquals("Latn", loc.getScript());
        assertEquals("POSIX", loc.getVariant());
    }

    @Test
    public void testToLocalDate_fromComponents() {
        CompactLinkedMap<String, Object> m = map(
                MapConversions.YEAR, 2021,
                MapConversions.MONTH, 12,
                MapConversions.DAY, 31);
        LocalDate ld = MapConversions.toLocalDate(m, converter);
        assertEquals(LocalDate.of(2021, 12, 31), ld);
    }

    @Test
    public void testToLocalTime_fromComponents() {
        CompactLinkedMap<String, Object> m = map(
                MapConversions.HOUR, 14,
                MapConversions.MINUTE, 30,
                MapConversions.SECOND, 15,
                MapConversions.NANOS, 123_456_789);
        LocalTime lt = MapConversions.toLocalTime(m, converter);
        assertEquals(LocalTime.of(14, 30, 15, 123_456_789), lt);
    }

    @Test
    public void testToOffsetTime_withExplicitOffset() {
        CompactLinkedMap<String, Object> m = map(
                MapConversions.HOUR, 9,
                MapConversions.MINUTE, 45,
                MapConversions.SECOND, 0,
                MapConversions.NANOS, 0,
                MapConversions.OFFSET_HOUR, -5,
                MapConversions.OFFSET_MINUTE, 0);
        OffsetTime ot = MapConversions.toOffsetTime(m, converter);
        assertEquals(OffsetTime.of(9, 45, 0, 0, ZoneOffset.ofHours(-5)), ot);
    }

    @Test
    public void testToOffsetDateTime_dateAndTimeAndOffset() {
        CompactLinkedMap<String, Object> m = map(
                MapConversions.DATE, LocalDate.of(2022, 3, 15),
                MapConversions.TIME, LocalTime.of(12, 0),
                MapConversions.OFFSET, ZoneOffset.ofHours(2));
        OffsetDateTime odt = MapConversions.toOffsetDateTime(m, converter);
        assertEquals(OffsetDateTime.of(2022, 3, 15, 12, 0, 0, 0, ZoneOffset.ofHours(2)), odt);
    }

    @Test
    public void testToLocalDateTime_withDateOnly() {
        CompactLinkedMap<String, Object> m = map(
                MapConversions.DATE, LocalDate.of(2021, 5, 20));
        LocalDateTime ldt = MapConversions.toLocalDateTime(m, converter);
        assertEquals(LocalDateTime.of(2021, 5, 20, 0, 0), ldt);
    }

    @Test
    public void testToZonedDateTime_fromEpochMillis() {
        long epoch = 1_600_000_000_000L;
        CompactLinkedMap<String, Object> m = map(MapConversions.EPOCH_MILLIS, epoch);
        ZonedDateTime zdt = MapConversions.toZonedDateTime(m, converter);
        assertEquals(Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()), zdt);
    }

    @Test
    public void testToClass() {
        CompactLinkedMap<String, Object> m = map(MapConversions.VALUE, Integer.class.getName());
        // The generic fromMap will try to convert the string to a Class object.
        Class<?> clazz = MapConversions.toClass(m, converter);
        assertEquals(Integer.class, clazz);
    }

    @Test
    public void testToDuration() {
        CompactLinkedMap<String, Object> m = map(
                MapConversions.SECONDS, 10L,
                MapConversions.NANOS, 500_000_000);
        Duration d = MapConversions.toDuration(m, converter);
        assertEquals(Duration.ofSeconds(10, 500_000_000), d);
    }

    @Test
    public void testToInstant() {
        CompactLinkedMap<String, Object> m = map(
                MapConversions.SECONDS, 1_600_000_000L,
                MapConversions.NANOS, 123_456_789);
        Instant i = MapConversions.toInstant(m, converter);
        assertEquals(Instant.ofEpochSecond(1_600_000_000L, 123_456_789), i);
    }

    @Test
    public void testToMonthDay() {
        CompactLinkedMap<String, Object> m = map(
                MapConversions.MONTH, 2,
                MapConversions.DAY, 29);
        MonthDay md = MapConversions.toMonthDay(m, converter);
        assertEquals(MonthDay.of(2, 29), md);
    }

    @Test
    public void testToYearMonth() {
        CompactLinkedMap<String, Object> m = map(
                MapConversions.YEAR, 2022,
                MapConversions.MONTH, 4);
        YearMonth ym = MapConversions.toYearMonth(m, converter);
        assertEquals(YearMonth.of(2022, 4), ym);
    }

    @Test
    public void testToPeriod_withValueKey() {
        CompactLinkedMap<String, Object> m = map(
                MapConversions.VALUE, Period.of(1, 2, 3));
        Period p = MapConversions.toPeriod(m, converter);
        assertEquals(Period.of(1, 2, 3), p);
    }

    @Test
    public void testToPeriod_fromComponents() {
        CompactLinkedMap<String, Object> m = map(
                MapConversions.YEARS, 5,
                MapConversions.MONTHS, 6,
                MapConversions.DAYS, 7);
        Period p = MapConversions.toPeriod(m, converter);
        assertEquals(Period.of(5, 6, 7), p);
    }

    @Test
    public void testToZoneId_fromId() {
        CompactLinkedMap<String, Object> m = map(MapConversions.ID, "Europe/Paris");
        ZoneId zid = MapConversions.toZoneId(m, converter);
        assertEquals(ZoneId.of("Europe/Paris"), zid);
    }

    @Test
    public void testToZoneOffset_fromHoursMinutesSeconds() {
        CompactLinkedMap<String, Object> m = map(
                MapConversions.HOURS, -3,
                MapConversions.MINUTES, 30,
                MapConversions.SECONDS, 0);
        ZoneOffset zo = MapConversions.toZoneOffset(m, converter);
        assertEquals(ZoneOffset.ofHoursMinutes(-3, 30), zo);
    }

    @Test
    public void testToYear() {
        CompactLinkedMap<String, Object> m = map(MapConversions.YEAR, 1999);
        Year y = MapConversions.toYear(m, converter);
        assertEquals(Year.of(1999), y);
    }

    @Test
    public void testToURL() throws Exception {
        String urlStr = "http://example.com/test";
        CompactLinkedMap<String, Object> m = map(MapConversions.URL_KEY, urlStr);
        URL url = MapConversions.toURL(m, converter);
        assertEquals(new URL(urlStr), url);
    }

    @Test
    public void testToURI() throws Exception {
        String uriStr = "urn:test:12345";
        CompactLinkedMap<String, Object> m = map(MapConversions.URI_KEY, uriStr);
        URI uri = MapConversions.toURI(m, converter);
        assertEquals(new URI(uriStr), uri);
    }

    @Test
    public void testToThrowable() throws Exception {
        CompactLinkedMap<String, Object> m = map(
                MapConversions.CLASS, IllegalArgumentException.class.getName(),
                MapConversions.MESSAGE, "bad argument");
        Throwable t = MapConversions.toThrowable(m, converter);
        assertTrue(t instanceof IllegalArgumentException);
        assertEquals("bad argument", t.getMessage());
    }

    @Test
    public void testInitMap() {
        String original = "originalValue";
        Map<String, ?> result = MapConversions.initMap(original, converter);
        assertTrue(result.containsKey(MapConversions.V));
        assertEquals(original, result.get(MapConversions.V));
    }

    /* --------------------------------------------------------------
     * 2.  Edge‑case / error handling tests
     * -------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testToUUID_missingAllKeys() {
        // Empty map – should fall through to fromMap and throw.
        CompactLinkedMap<String, Object> m = map();
        MapConversions.toUUID(m, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocale_missingLanguage() {
        // Language empty → fromMap will be invoked and throw.
        CompactLinkedMap<String, Object> m = map(
                MapConversions.LANGUAGE, "",
                MapConversions.COUNTRY, "US");
        MapConversions.toLocale(m, converter);
    }

    @Test
    public void testToLocale_invalidLanguageThrowsIllegalArgumentException() {
        CompactLinkedMap<String, Object> m = map(
                MapConversions.LANGUAGE, "invalid-language-code",
                MapConversions.COUNTRY, "US");
        try {
            MapConversions.toLocale(m, converter);
            fail("Expected IllegalArgumentException due to invalid language");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Locale language"));
        }
    }

    @Test
    public void testToOffsetTime_invalidOffsetThrowsIllegalArgumentException() {
        CompactLinkedMap<String, Object> m = map(
                MapConversions.HOUR, 10,
                MapConversions.MINUTE, 20,
                MapConversions.OFFSET_HOUR, 25,   // invalid hour offset (> 23)
                MapConversions.OFFSET_MINUTE, 0);
        try {
            MapConversions.toOffsetTime(m, converter);
            fail("Expected IllegalArgumentException because offset hour is out of range");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Offset 'hour' and 'minute' are not correct"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToZoneOffset_missingHoursKey() {
        // No HOURS key – fromMap will be invoked and throw.
        CompactLinkedMap<String, Object> m = map();
        MapConversions.toZoneOffset(m, converter);
    }

    @Test
    public void testToThrowable_unknownClassThrowsIllegalArgumentException() {
        CompactLinkedMap<String, Object> m = map(
                MapConversions.CLASS, "com.example.DoesNotExist",
                MapConversions.MESSAGE, "oops");
        try {
            MapConversions.toThrowable(m, converter);
            fail("Expected IllegalArgumentException because class does not exist");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unable to reconstruct exception"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToURI_missingKey() {
        // No URI key – fromMap will throw.
        CompactLinkedMap<String, Object> m = map();
        MapConversions.toURI(m, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToClass_missingValue() {
        // No value – fromMap will throw.
        CompactLinkedMap<String, Object> m = map();
        MapConversions.toClass(m, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToDuration_missingSeconds() {
        // No SECONDS key – should delegate to fromMap and throw.
        CompactLinkedMap<String, Object> m = map();
        MapConversions.toDuration(m, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToInstant_missingSeconds() {
        // No SECONDS key – should delegate to fromMap and throw.
        CompactLinkedMap<String, Object> m = map();
        MapConversions.toInstant(m, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToMonthDay_missingComponent() {
        // Missing DAY key.
        CompactLinkedMap<String, Object> m = map(MapConversions.MONTH, 5);
        MapConversions.toMonthDay(m, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToYearMonth_missingComponent() {
        // Missing MONTH key.
        CompactLinkedMap<String, Object> m = map(MapConversions.YEAR, 2020);
        MapConversions.toYearMonth(m, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalDate_missingAllComponents() {
        // No DATE nor YEAR/MONTH/DAY – should throw.
        CompactLinkedMap<String, Object> m = map();
        MapConversions.toLocalDate(m, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_missingHourMinute() {
        // No HOUR/MINUTE – should throw.
        CompactLinkedMap<String, Object> m = map();
        MapConversions.toLocalTime(m, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetDateTime_missingBothTimeAndOffset() {
        // Neither TIME nor OFFSET present.
        CompactLinkedMap<String, Object> m = map();
        MapConversions.toOffsetDateTime(m, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToZonedDateTime_missingAll() {
        // No EPOCH_MILLIS, no DATE/TIME/ZONE – should throw.
        CompactLinkedMap<String, Object> m = map();
        MapConversions.toZonedDateTime(m, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromMap_noSupportedKeys() {
        // Directly invoke the private helper via reflection to verify the exception message format.
        try {
            java.lang.reflect.Method mth = MapConversions.class.getDeclaredMethod(
                    "fromMap", Object.class, Converter.class, Class.class, String[][].class);
            mth.setAccessible(true);
            CompactLinkedMap<String, Object> map = map();
            mth.invoke(null, map, converter, Integer.class, new String[][]{{"foo"}});
            fail("Expected IllegalArgumentException from fromMap");
        } catch (Exception e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof IllegalArgumentException);
            assertTrue(cause.getMessage().contains("To convert from Map to"));
        }
    }

    /* --------------------------------------------------------------
     * 3.  Additional sanity checks for the internal epoch conversion
     * -------------------------------------------------------------- */

    @Test
    public void testToEpochMillis_timeOnly() throws Exception {
        // Use reflection to call the private toEpochMillis method.
        java.lang.reflect.Method method = MapConversions.class.getDeclaredMethod(
                "toEpochMillis", Object.class, Converter.class);
        method.setAccessible(true);

        ZonedDateTime now = ZonedDateTime.now();
        CompactLinkedMap<String, Object> m = map(MapConversions.TIME, now);
        @SuppressWarnings("unchecked")
        Map.Entry<Long, Integer> entry = (Map.Entry<Long, Integer>) method.invoke(null, m, converter);

        assertEquals(now.toInstant().toEpochMilli(), entry.getKey().longValue());
        // nanos should match the source ZonedDateTime's nanos.
        assertEquals(now.getNano(), entry.getValue().intValue());
    }

    @Test
    public void testToEpochMillis_dateTimeAndZone() throws Exception {
        java.lang.reflect.Method method = MapConversions.class.getDeclaredMethod(
                "toEpochMillis", Object.class, Converter.class);
        method.setAccessible(true);

        CompactLinkedMap<String, Object> m = map(
                MapConversions.DATE, LocalDate.of(2021, 7, 1),
                MapConversions.TIME, LocalTime.of(12, 34, 56, 789_000_000),
                MapConversions.ZONE, ZoneId.of("UTC"));
        @SuppressWarnings("unchecked")
        Map.Entry<Long, Integer> entry = (Map.Entry<Long, Integer>) method.invoke(null, m, converter);

        ZonedDateTime expected = ZonedDateTime.of(
                LocalDate.of(2021, 7, 1),
                LocalTime.of(12, 34, 56, 789_000_000),
                ZoneId.of("UTC"));
        assertEquals(expected.toInstant().toEpochMilli(), entry.getKey().longValue());
        assertEquals(789_000_000, entry.getValue().intValue());
    }
}
