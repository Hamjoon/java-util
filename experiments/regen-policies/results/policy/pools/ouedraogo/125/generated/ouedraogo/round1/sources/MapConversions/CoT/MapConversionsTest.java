package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CollectionUtilities;
import com.cedarsoftware.util.CompactMap;
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
 * Unit tests for {@link MapConversions}.  The class is package‑private, but the test class lives in the same
 * package so it can access the static conversion methods directly.
 *
 * The tests are grouped in three sections:
 *   1.  Simple “happy‑path” tests that verify a correct conversion when the required keys are present.
 *   2.  Edge‑case / error‑handling tests that provoke {@code IllegalArgumentException}s when the map is
 *       malformed or when illegal values are supplied.
 *   3.  Special‑case tests for methods that contain extra logic (e.g. {@code toTimestamp}, {@code toLocale},
 *       {@code toThrowable}).
 *
 * A single {@link Converter} instance is reused for all tests; it is created with default
 * {@code ConverterOptions} (the concrete class is part of the library and has a public no‑arg constructor).
 */
public class MapConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The library provides a public ConverterOptions class (not shown in the snippet) that can be instantiated.
        // Using the no‑arg constructor creates a default configuration suitable for the tests.
        converter = new Converter(new ConverterOptions());
    }

    /* -------------------------------------------------
     *  1.  Public method signatures (extracted from source)
     * -------------------------------------------------
     *
     * static Object toUUID(Object from, Converter converter)
     * static Byte   toByte(Object from, Converter converter)
     * static Short  toShort(Object from, Converter converter)
     * static Integer toInt(Object from, Converter converter)
     * static Long    toLong(Object from, Converter converter)
     * static Float   toFloat(Object from, Converter converter)
     * static Double  toDouble(Object from, Converter converter)
     * static Boolean toBoolean(Object from, Converter converter)
     * static BigDecimal   toBigDecimal(Object from, Converter converter)
     * static BigInteger   toBigInteger(Object from, Converter converter)
     * static String       toString(Object from, Converter converter)
     * static StringBuffer toStringBuffer(Object from, Converter converter)
     * static StringBuilder toStringBuilder(Object from, Converter converter)
     * static Character    toCharacter(Object from, Converter converter)
     * static AtomicInteger toAtomicInteger(Object from, Converter converter)
     * static AtomicLong    toAtomicLong(Object from, Converter converter)
     * static AtomicBoolean toAtomicBoolean(Object from, Converter converter)
     * static java.sql.Date toSqlDate(Object from, Converter converter)
     * static Date          toDate(Object from, Converter converter)
     * static Timestamp     toTimestamp(Object from, Converter converter)
     * static TimeZone      toTimeZone(Object from, Converter converter)
     * static Calendar      toCalendar(Object from, Converter converter)
     * static Locale        toLocale(Object from, Converter converter)
     * static LocalDate    toLocalDate(Object from, Converter converter)
     * static LocalTime    toLocalTime(Object from, Converter converter)
     * static OffsetTime   toOffsetTime(Object from, Converter converter)
     * static OffsetDateTime toOffsetDateTime(Object from, Converter converter)
     * static LocalDateTime toLocalDateTime(Object from, Converter converter)
     * static ZonedDateTime toZonedDateTime(Object from, Converter converter)
     * static Class<?>     toClass(Object from, Converter converter)
     * static Duration     toDuration(Object from, Converter converter)
     * static Instant      toInstant(Object from, Converter converter)
     * static MonthDay    toMonthDay(Object from, Converter converter)
     * static YearMonth   toYearMonth(Object from, Converter converter)
     * static Period      toPeriod(Object from, Converter converter)
     * static ZoneId      toZoneId(Object from, Converter converter)
     * static ZoneOffset  toZoneOffset(Object from, Converter converter)
     * static Year        toYear(Object from, Converter converter)
     * static URL         toURL(Object from, Converter converter)
     * static Throwable   toThrowable(Object from, Converter converter, Class<?> target)
     * static URI         toURI(Object from, Converter converter)
     * static Map<String, ?> initMap(Object from, Converter converter)
     */

    /* -------------------------------------------------
     *  2.  Basic happy‑path tests
     * ------------------------------------------------- */

    @Test
    public void testToInt_HappyPath() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.VALUE, 42);
        Integer result = MapConversions.toInt(map, converter);
        assertEquals(Integer.valueOf(42), result);
    }

    @Test
    public void testToLong_HappyPath() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", 123456789L);
        Long result = MapConversions.toLong(map, converter);
        assertEquals(Long.valueOf(123456789L), result);
    }

    @Test
    public void testToBoolean_HappyPath() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", true);
        Boolean result = MapConversions.toBoolean(map, converter);
        assertTrue(result);
    }

    @Test
    public void testToString_HappyPath() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "hello");
        String result = MapConversions.toString(map, converter);
        assertEquals("hello", result);
    }

    @Test
    public void testToBigDecimal_HappyPath() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "123.45");
        BigDecimal bd = MapConversions.toBigDecimal(map, converter);
        assertEquals(new BigDecimal("123.45"), bd);
    }

    @Test
    public void testToUUID_FromString() {
        Map<String, Object> map = new HashMap<>();
        map.put("UUID", "123e4567-e89b-12d3-a456-426655440000");
        UUID uuid = (UUID) MapConversions.toUUID(map, converter);
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426655440000"), uuid);
    }

    @Test
    public void testToUUID_FromMostLeastBits() {
        Map<String, Object> map = new HashMap<>();
        map.put("mostSigBits", 0x1234567890ABCDEFL);
        map.put("leastSigBits", 0x0FEDCBA987654321L);
        UUID uuid = (UUID) MapConversions.toUUID(map, converter);
        assertEquals(new UUID(0x1234567890ABCDEFL, 0x0FEDCBA987654321L), uuid);
    }

    @Test
    public void testToDate_FromEpochMillis() {
        Map<String, Object> map = new HashMap<>();
        map.put("epochMillis", 1609459200000L); // 2021‑01‑01T00:00:00Z
        Date date = MapConversions.toDate(map, converter);
        assertEquals(1609459200000L, date.getTime());
    }

    @Test
    public void testToTimestamp_WithNanos() {
        Map<String, Object> map = new HashMap<>();
        map.put("epochMillis", 1609459200000L);
        map.put("nanos", 123456789);
        Timestamp ts = MapConversions.toTimestamp(map, converter);
        assertEquals(1609459200000L, ts.getTime());
        assertEquals(123456789, ts.getNanos());
    }

    @Test
    public void testToLocale_AllFields() {
        Map<String, String> map = new HashMap<>();
        map.put("language", "en");
        map.put("country", "US");
        map.put("script", "Latn");
        map.put("variant", "POSIX");
        Locale locale = MapConversions.toLocale(map, converter);
        assertEquals("en", locale.getLanguage());
        assertEquals("US", locale.getCountry());
        assertEquals("Latn", locale.getScript());
        assertEquals("POSIX", locale.getVariant());
    }

    @Test
    public void testToLocalDate_FromComponents() {
        Map<String, Object> map = new HashMap<>();
        map.put("year", 2022);
        map.put("month", 3);
        map.put("day", 14);
        LocalDate ld = MapConversions.toLocalDate(map, converter);
        assertEquals(LocalDate.of(2022, 3, 14), ld);
    }

    @Test
    public void testToLocalTime_FromComponents() {
        Map<String, Object> map = new HashMap<>();
        map.put("hour", 15);
        map.put("minute", 30);
        map.put("second", 45);
        map.put("nanos", 123_456_789);
        LocalTime lt = MapConversions.toLocalTime(map, converter);
        assertEquals(LocalTime.of(15, 30, 45, 123_456_789), lt);
    }

    @Test
    public void testToOffsetTime_WithOffsetFields() {
        Map<String, Object> map = new HashMap<>();
        map.put("hour", 10);
        map.put("minute", 20);
        map.put("second", 30);
        map.put("nanos", 0);
        map.put("offsetHour", -5);
        map.put("offsetMinute", 0);
        OffsetTime ot = MapConversions.toOffsetTime(map, converter);
        assertEquals(OffsetTime.of(10, 20, 30, 0, ZoneOffset.ofHours(-5)), ot);
    }

    @Test
    public void testToZoneOffset_WithHoursMinutesSeconds() {
        Map<String, Object> map = new HashMap<>();
        map.put("hours", 2);
        map.put("minutes", 30);
        map.put("seconds", 15);
        ZoneOffset zo = MapConversions.toZoneOffset(map, converter);
        assertEquals(ZoneOffset.ofHoursMinutesSeconds(2, 30, 15), zo);
    }

    @Test
    public void testToPeriod_FromValueKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "P2Y3M5D");
        Period p = MapConversions.toPeriod(map, converter);
        assertEquals(Period.of(2, 3, 5), p);
    }

    @Test
    public void testToPeriod_FromComponents() {
        Map<String, Object> map = new HashMap<>();
        map.put("years", 1);
        map.put("months", 2);
        map.put("days", 3);
        Period p = MapConversions.toPeriod(map, converter);
        assertEquals(Period.of(1, 2, 3), p);
    }

    @Test
    public void testToThrowable_WithCauseAndMessage() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", IllegalArgumentException.class.getName());
        map.put("message", "top level");
        map.put("cause", NullPointerException.class.getName());
        map.put("causeMessage", "inner NPE");

        Throwable t = MapConversions.toThrowable(map, converter, RuntimeException.class);
        assertTrue(t instanceof IllegalArgumentException);
        assertEquals("top level", t.getMessage());
        assertNotNull(t.getCause());
        assertTrue(t.getCause() instanceof NullPointerException);
        assertEquals("inner NPE", t.getCause().getMessage());
    }

    /* -------------------------------------------------
     *  3.  Edge‑case / exception tests
     * ------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testToInt_MissingKeys() {
        Map<String, Object> map = new HashMap<>(); // empty map
        MapConversions.toInt(map, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToUUID_NoRecognisedKeys() {
        Map<String, Object> map = new HashMap<>();
        map.put("someOtherKey", "whatever");
        MapConversions.toUUID(map, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocale_MissingLanguage() {
        Map<String, String> map = new HashMap<>();
        // language key absent -> should fall back to fromMap and then throw
        MapConversions.toLocale(map, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocale_InvalidLanguage() {
        Map<String, String> map = new HashMap<>();
        map.put("language", "invalid-language-code-xyz");
        // Builder will throw IllegalArgumentException for an illegal language
        MapConversions.toLocale(map, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTime_InvalidOffset() {
        Map<String, Object> map = new HashMap<>();
        map.put("hour", 12);
        map.put("minute", 0);
        map.put("offsetHour", 25); // invalid hour offset
        map.put("offsetMinute", 0);
        MapConversions.toOffsetTime(map, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToZoneOffset_InvalidHours() {
        Map<String, Object> map = new HashMap<>();
        map.put("hours", 30); // invalid, must be -18..+18
        MapConversions.toZoneOffset(map, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToThrowable_InvalidClassName() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", "non.existent.ClassName");
        MapConversions.toThrowable(map, converter, RuntimeException.class);
    }

    @Test
    public void testInitMap_ReturnsMapWithValueKey() {
        String payload = "payload";
        Map<String, ?> result = MapConversions.initMap(payload, converter);
        assertTrue(result.containsKey("_v"));
        assertEquals(payload, result.get("_v"));
    }

    @Test
    public void testFromMap_FallbackToValueKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", 99);
        Integer i = MapConversions.toInt(map, converter);
        assertEquals(Integer.valueOf(99), i);
    }

    @Test
    public void testToURL_WithStringKey() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("URL", "http://example.com");
        URL url = MapConversions.toURL(map, converter);
        assertEquals(new URL("http://example.com"), url);
    }

    @Test
    public void testToURI_WithStringKey() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("URI", "http://example.com/path");
        URI uri = MapConversions.toURI(map, converter);
        assertEquals(new URI("http://example.com/path"), uri);
    }

    @Test
    public void testToCalendar_FromEpochMillis() {
        Map<String, Object> map = new HashMap<>();
        map.put("epochMillis", 1622505600000L); // 2021‑06‑01T00:00:00Z
        Calendar cal = MapConversions.toCalendar(map, converter);
        assertEquals(1622505600000L, cal.getTimeInMillis());
    }

    @Test
    public void testToZonedDateTime_FromComponents() {
        Map<String, Object> map = new HashMap<>();
        map.put("date", LocalDate.of(2020, 5, 20));
        map.put("time", LocalTime.of(10, 15, 30));
        map.put("zone", ZoneId.of("Europe/Paris"));
        ZonedDateTime zdt = MapConversions.toZonedDateTime(map, converter);
        assertEquals(ZonedDateTime.of(2020, 5, 20, 10, 15, 30, 0, ZoneId.of("Europe/Paris")), zdt);
    }

    @Test
    public void testToDuration_FromSecondsAndNanos() {
        Map<String, Object> map = new HashMap<>();
        map.put("seconds", 5L);
        map.put("nanos", 500_000_000);
        Duration d = MapConversions.toDuration(map, converter);
        assertEquals(Duration.ofSeconds(5, 500_000_000), d);
    }

    @Test
    public void testToInstant_FromSecondsAndNanos() {
        Map<String, Object> map = new HashMap<>();
        map.put("seconds", 10L);
        map.put("nanos", 250L);
        Instant i = MapConversions.toInstant(map, converter);
        assertEquals(Instant.ofEpochSecond(10, 250), i);
    }

    @Test
    public void testToMonthDay_FromComponents() {
        Map<String, Object> map = new HashMap<>();
        map.put("month", 12);
        map.put("day", 25);
        MonthDay md = MapConversions.toMonthDay(map, converter);
        assertEquals(MonthDay.of(12, 25), md);
    }

    @Test
    public void testToYearMonth_FromComponents() {
        Map<String, Object> map = new HashMap<>();
        map.put("year", 2023);
        map.put("month", 7);
        YearMonth ym = MapConversions.toYearMonth(map, converter);
        assertEquals(YearMonth.of(2023, 7), ym);
    }

    @Test
    public void testToYear_FromKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("year", 1999);
        Year y = MapConversions.toYear(map, converter);
        assertEquals(Year.of(1999), y);
    }
}
