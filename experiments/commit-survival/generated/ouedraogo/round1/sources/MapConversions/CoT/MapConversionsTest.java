package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactLinkedMap;


import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.UUID;

import static org.junit.Assert.*;

public class MapConversionsTest {

    /** All static conversion methods in {@link MapConversions}. */
    /*
        static Object toUUID(Object from, Converter converter)
        static Byte toByte(Object from, Converter converter)
        static Short toShort(Object from, Converter converter)
        static Integer toInt(Object from, Converter converter)
        static Long toLong(Object from, Converter converter)
        static Float toFloat(Object from, Converter converter)
        static Double toDouble(Object from, Converter converter)
        static Boolean toBoolean(Object from, Converter converter)
        static BigDecimal toBigDecimal(Object from, Converter converter)
        static BigInteger toBigInteger(Object from, Converter converter)
        static String toString(Object from, Converter converter)
        static StringBuffer toStringBuffer(Object from, Converter converter)
        static StringBuilder toStringBuilder(Object from, Converter converter)
        static Character toCharacter(Object from, Converter converter)
        static AtomicInteger toAtomicInteger(Object from, Converter converter)
        static AtomicLong toAtomicLong(Object from, Converter converter)
        static AtomicBoolean toAtomicBoolean(Object from, Converter converter)
        static java.sql.Date toSqlDate(Object from, Converter converter)
        static Date toDate(Object from, Converter converter)
        static Timestamp toTimestamp(Object from, Converter converter)
        static TimeZone toTimeZone(Object from, Converter converter)
        static Calendar toCalendar(Object from, Converter converter)
        static Locale toLocale(Object from, Converter converter)
        static LocalDate toLocalDate(Object from, Converter converter)
        static LocalTime toLocalTime(Object from, Converter converter)
        static OffsetTime toOffsetTime(Object from, Converter converter)
        static OffsetDateTime toOffsetDateTime(Object from, Converter converter)
        static LocalDateTime toLocalDateTime(Object from, Converter converter)
        static ZonedDateTime toZonedDateTime(Object from, Converter converter)
        static Class<?> toClass(Object from, Converter converter)
        static Duration toDuration(Object from, Converter converter)
        static Instant toInstant(Object from, Converter converter)
        static MonthDay toMonthDay(Object from, Converter converter)
        static YearMonth toYearMonth(Object from, Converter converter)
        static Period toPeriod(Object from, Converter converter)
        static ZoneId toZoneId(Object from, Converter converter)
        static ZoneOffset toZoneOffset(Object from, Converter converter)
        static Year toYear(Object from, Converter converter)
        static URL toURL(Object from, Converter converter)
        static Throwable toThrowable(Object from, Converter converter)
        static URI toURI(Object from, Converter converter)
        static Map<String, ?> initMap(Object from, Converter converter)
    */

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions is not part of the supplied API; passing null is acceptable for these tests.
        converter = new Converter(null);
    }

    /** Helper to create a map with a single "value" entry. */
    private Map<String, Object> valueMap(Object v) {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("value", v);
        return map;
    }

    /** Helper to create a map with a single "_v" entry (the internal alias). */
    private Map<String, Object> vMap(Object v) {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("_v", v);
        return map;
    }

    // ----------------------------------------------------------------------
    // Basic happy‑path tests for each conversion method
    // ----------------------------------------------------------------------
    @Test
    public void testToByte() {
        Map<String, Object> map = valueMap((byte) 7);
        assertEquals(Byte.valueOf((byte) 7), MapConversions.toByte(map, converter));
    }

    @Test
    public void testToShort() {
        Map<String, Object> map = valueMap((short) 123);
        assertEquals(Short.valueOf((short) 123), MapConversions.toShort(map, converter));
    }

    @Test
    public void testToInt() {
        Map<String, Object> map = valueMap(42);
        assertEquals(Integer.valueOf(42), MapConversions.toInt(map, converter));
    }

    @Test
    public void testToLong() {
        Map<String, Object> map = valueMap(123456789L);
        assertEquals(Long.valueOf(123456789L), MapConversions.toLong(map, converter));
    }

    @Test
    public void testToFloat() {
        Map<String, Object> map = valueMap(3.14f);
        assertEquals(Float.valueOf(3.14f), MapConversions.toFloat(map, converter));
    }

    @Test
    public void testToDouble() {
        Map<String, Object> map = valueMap(2.71828);
        assertEquals(Double.valueOf(2.71828), MapConversions.toDouble(map, converter));
    }

    @Test
    public void testToBoolean() {
        Map<String, Object> map = valueMap(Boolean.TRUE);
        assertEquals(Boolean.TRUE, MapConversions.toBoolean(map, converter));
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal bd = new BigDecimal("12345.6789");
        Map<String, Object> map = valueMap(bd);
        assertEquals(bd, MapConversions.toBigDecimal(map, converter));
    }

    @Test
    public void testToBigInteger() {
        BigInteger bi = new BigInteger("9876543210");
        Map<String, Object> map = valueMap(bi);
        assertEquals(bi, MapConversions.toBigInteger(map, converter));
    }

    @Test
    public void testToString() {
        Map<String, Object> map = valueMap("hello world");
        assertEquals("hello world", MapConversions.toString(map, converter));
    }

    @Test
    public void testToStringBuffer() {
        StringBuffer sb = new StringBuffer("buf");
        Map<String, Object> map = valueMap(sb);
        assertEquals(sb, MapConversions.toStringBuffer(map, converter));
    }

    @Test
    public void testToStringBuilder() {
        StringBuilder sb = new StringBuilder("builder");
        Map<String, Object> map = valueMap(sb);
        assertEquals(sb, MapConversions.toStringBuilder(map, converter));
    }

    @Test
    public void testToCharacter() {
        Map<String, Object> map = valueMap('X');
        assertEquals(Character.valueOf('X'), MapConversions.toCharacter(map, converter));
    }

    @Test
    public void testToAtomicInteger() {
        AtomicInteger ai = new AtomicInteger(10);
        Map<String, Object> map = valueMap(ai);
        assertEquals(ai, MapConversions.toAtomicInteger(map, converter));
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong al = new AtomicLong(12345L);
        Map<String, Object> map = valueMap(al);
        assertEquals(al, MapConversions.toAtomicLong(map, converter));
    }

    @Test
    public void testToAtomicBoolean() {
        AtomicBoolean ab = new AtomicBoolean(true);
        Map<String, Object> map = valueMap(ab);
        assertEquals(ab, MapConversions.toAtomicBoolean(map, converter));
    }

    @Test
    public void testToDateWithEpochMillis() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("epochMillis", 0L);
        Date d = MapConversions.toDate(map, converter);
        assertEquals(0L, d.getTime());
    }

    @Test
    public void testToSqlDateWithEpochMillis() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("epochMillis", 0L);
        java.sql.Date d = MapConversions.toSqlDate(map, converter);
        assertEquals(0L, d.getTime());
    }

    @Test
    public void testToTimestampWithEpochMillisAndNanos() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("epochMillis", 1_000L);
        map.put("nanos", 123456789);
        Timestamp ts = MapConversions.toTimestamp(map, converter);
        assertEquals(1_000L, ts.getTime());
        assertEquals(123456789, ts.getNanos());
    }

    @Test
    public void testToTimeZone() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("zone", "UTC");
        TimeZone tz = MapConversions.toTimeZone(map, converter);
        assertEquals(TimeZone.getTimeZone("UTC"), tz);
    }

    @Test
    public void testToCalendarWithEpochMillis() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("epochMillis", 0L);
        Calendar cal = MapConversions.toCalendar(map, converter);
        assertEquals(0L, cal.getTimeInMillis());
    }

    @Test
    public void testToLocaleFull() {
        Map<String, String> map = new CompactLinkedMap<>();
        map.put("language", "en");
        map.put("country", "US");
        map.put("script", "Latn");
        map.put("variant", "POSIX");
        Locale loc = MapConversions.toLocale(map, converter);
        assertEquals(new Locale.Builder()
                .setLanguage("en")
                .setRegion("US")
                .setScript("Latn")
                .setVariant("POSIX")
                .build(), loc);
    }

    @Test
    public void testToLocalDateFromComponents() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("year", 2023);
        map.put("month", 5);
        map.put("day", 17);
        LocalDate ld = MapConversions.toLocalDate(map, converter);
        assertEquals(LocalDate.of(2023, 5, 17), ld);
    }

    @Test
    public void testToLocalTimeFromComponents() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("hour", 14);
        map.put("minute", 30);
        map.put("second", 45);
        map.put("nanos", 123456789);
        LocalTime lt = MapConversions.toLocalTime(map, converter);
        assertEquals(LocalTime.of(14, 30, 45, 123456789), lt);
    }

    @Test
    public void testToOffsetTimeFromComponentsAndOffset() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("hour", 9);
        map.put("minute", 15);
        map.put("second", 0);
        map.put("nanos", 0);
        map.put("offsetHour", -5);
        map.put("offsetMinute", 0);
        OffsetTime ot = MapConversions.toOffsetTime(map, converter);
        assertEquals(OffsetTime.of(9, 15, 0, 0, ZoneOffset.ofHours(-5)), ot);
    }

    @Test
    public void testToOffsetDateTimeFromTimeAndOffsetOnly() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("time", LocalDateTime.of(2022, 12, 31, 23, 59, 59));
        map.put("offset", ZoneOffset.ofHoursMinutes(2, 30));
        OffsetDateTime odt = MapConversions.toOffsetDateTime(map, converter);
        assertEquals(OffsetDateTime.of(LocalDateTime.of(2022, 12, 31, 23, 59, 59), ZoneOffset.ofHoursMinutes(2, 30)), odt);
    }

    @Test
    public void testToLocalDateTimeFromDateAndTime() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("date", LocalDate.of(2021, 1, 1));
        map.put("time", LocalTime.of(12, 0));
        LocalDateTime ldt = MapConversions.toLocalDateTime(map, converter);
        assertEquals(LocalDateTime.of(2021, 1, 1, 12, 0), ldt);
    }

    @Test
    public void testToZonedDateTimeFromAllComponents() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("date", LocalDate.of(2020, 2, 29));
        map.put("time", LocalTime.of(6, 30));
        map.put("zone", ZoneId.of("Europe/Paris"));
        ZonedDateTime zdt = MapConversions.toZonedDateTime(map, converter);
        assertEquals(ZonedDateTime.of(LocalDate.of(2020, 2, 29), LocalTime.of(6, 30), ZoneId.of("Europe/Paris")), zdt);
    }

    @Test
    public void testToClass() {
        Map<String, Object> map = valueMap(String.class);
        assertEquals(String.class, MapConversions.toClass(map, converter));
    }

    @Test
    public void testToDurationFromSecondsAndNanos() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("seconds", 5L);
        map.put("nanos", 500);
        Duration d = MapConversions.toDuration(map, converter);
        assertEquals(Duration.ofSeconds(5, 500), d);
    }

    @Test
    public void testToInstantFromSecondsAndNanos() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("seconds", 10L);
        map.put("nanos", 250L);
        Instant i = MapConversions.toInstant(map, converter);
        assertEquals(Instant.ofEpochSecond(10L, 250L), i);
    }

    @Test
    public void testToMonthDay() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("month", 2);
        map.put("day", 29);
        MonthDay md = MapConversions.toMonthDay(map, converter);
        assertEquals(MonthDay.of(2, 29), md);
    }

    @Test
    public void testToYearMonth() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("year", 1999);
        map.put("month", 12);
        YearMonth ym = MapConversions.toYearMonth(map, converter);
        assertEquals(YearMonth.of(1999, 12), ym);
    }

    @Test
    public void testToPeriodFromComponents() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("years", 2);
        map.put("months", 3);
        map.put("days", 15);
        Period p = MapConversions.toPeriod(map, converter);
        assertEquals(Period.of(2, 3, 15), p);
    }

    @Test
    public void testToZoneIdFromZoneKey() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("zone", "America/New_York");
        ZoneId zid = MapConversions.toZoneId(map, converter);
        assertEquals(ZoneId.of("America/New_York"), zid);
    }

    @Test
    public void testToZoneOffsetFromHoursMinutesSeconds() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("hours", -3);
        map.put("minutes", 30);
        map.put("seconds", 0);
        ZoneOffset zo = MapConversions.toZoneOffset(map, converter);
        assertEquals(ZoneOffset.ofHoursMinutesSeconds(-3, 30, 0), zo);
    }

    @Test
    public void testToYear() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("year", 2025);
        Year y = MapConversions.toYear(map, converter);
        assertEquals(Year.of(2025), y);
    }

    @Test
    public void testToURL() throws Exception {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("URL", "http://example.com");
        URL url = MapConversions.toURL(map, converter);
        assertEquals(new URL("http://example.com"), url);
    }

    @Test
    public void testToThrowable() throws Exception {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("class", IllegalArgumentException.class.getName());
        map.put("message", "test message");
        Throwable t = MapConversions.toThrowable(map, converter);
        assertTrue(t instanceof IllegalArgumentException);
        assertEquals("test message", t.getMessage());
    }

    @Test
    public void testToURI() throws Exception {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("URI", "http://example.org/path");
        URI uri = MapConversions.toURI(map, converter);
        assertEquals(new URI("http://example.org/path"), uri);
    }

    @Test
    public void testInitMap() {
        String payload = "payload";
        Map<String, ?> result = MapConversions.initMap(payload, converter);
        assertTrue(result.containsKey("_v"));
        assertEquals(payload, result.get("_v"));
    }

    // ----------------------------------------------------------------------
    // Edge‑case and exception tests
    // ----------------------------------------------------------------------
    @Test(expected = IllegalArgumentException.class)
    public void testToUUIDMissingAllKeys() {
        Map<String, Object> empty = new CompactLinkedMap<>();
        MapConversions.toUUID(empty, converter);
    }

    @Test
    public void testToUUIDFromMostAndLeastSigBits() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("mostSigBits", 0x1234567890ABCDEFL);
        map.put("leastSigBits", 0x0FEDCBA098765432L);
        UUID uuid = (UUID) MapConversions.toUUID(map, converter);
        assertEquals(new UUID(0x1234567890ABCDEFL, 0x0FEDCBA098765432L), uuid);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocaleMissingLanguage() {
        Map<String, String> map = new CompactLinkedMap<>();
        // language key is present but empty – triggers fallback which throws
        map.put("language", "");
        MapConversions.toLocale(map, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTimeInvalidOffset() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("hour", 10);
        map.put("minute", 20);
        map.put("offsetHour", 25); // invalid hour (>24)
        map.put("offsetMinute", 0);
        MapConversions.toOffsetTime(map, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToZoneOffsetInvalidHours() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("hours", 30); // invalid hour offset
        MapConversions.toZoneOffset(map, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToThrowableInvalidClassName() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("class", "com.nonexistent.FakeException");
        map.put("message", "oops");
        MapConversions.toThrowable(map, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToURLMissingKey() {
        Map<String, Object> map = new CompactLinkedMap<>();
        // No "URL" key and no fallback value – should throw
        MapConversions.toURL(map, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToURIInvalidContent() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("URI", "   "); // whitespace only – considered empty
        MapConversions.toURI(map, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromMapNoRecognisedKey() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("unknown", "value");
        // Directly invoke a private helper via reflection to ensure it throws the expected message.
        try {
            java.lang.reflect.Method m = MapConversions.class.getDeclaredMethod("fromMap", Object.class, Converter.class, Class.class, String[].class, String[].class);
            m.setAccessible(true);
            m.invoke(null, map, converter, Integer.class, new String[]{"foo"}, new String[]{"bar"});
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }
}
