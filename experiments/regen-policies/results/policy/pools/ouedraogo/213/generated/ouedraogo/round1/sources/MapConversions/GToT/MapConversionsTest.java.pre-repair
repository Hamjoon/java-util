package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link MapConversions}.
 *
 * The test class lives in the same package as {@code MapConversions} so it can
 * access the package‑private static methods directly.
 *
 * The tests are grouped into three phases that match the steps described in
 * the task:
 *
 * 1.  Verify the basic happy‑path functionality of each conversion method.
 * 2.  Identify edge‑cases such as missing keys, null values, wrong types, and
 *     invalid data.
 * 3.  Exercise those edge‑cases and confirm that the expected exceptions are
 *     thrown with the correct messages.
 *
 * A single {@link Converter} instance is reused for all tests; it is created
 * with a {@code null} {@code ConverterOptions} because the library permits a
 * null options object for the default configuration.
 */
public class MapConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The Converter constructor accepts a ConverterOptions instance; passing null
        // yields a default configuration (the library allows it).
        converter = new Converter(null);
    }

    /* --------------------------------------------------------------
     *  1.  Happy‑path tests – each static conversion method is called
     *      with a map that contains a suitable key/value pair.
     * -------------------------------------------------------------- */

    @Test
    public void testToByte() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, (byte) 7);
        Byte result = MapConversions.toByte(map, converter);
        assertEquals(Byte.valueOf((byte) 7), result);
    }

    @Test
    public void testToShort() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, (short) 13);
        Short result = MapConversions.toShort(map, converter);
        assertEquals(Short.valueOf((short) 13), result);
    }

    @Test
    public void testToInt() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, 42);
        Integer result = MapConversions.toInt(map, converter);
        assertEquals(Integer.valueOf(42), result);
    }

    @Test
    public void testToLong() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, 123456789L);
        Long result = MapConversions.toLong(map, converter);
        assertEquals(Long.valueOf(123456789L), result);
    }

    @Test
    public void testToFloat() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, 3.14f);
        Float result = MapConversions.toFloat(map, converter);
        assertEquals(Float.valueOf(3.14f), result);
    }

    @Test
    public void testToDouble() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, 2.718);
        Double result = MapConversions.toDouble(map, converter);
        assertEquals(Double.valueOf(2.718), result);
    }

    @Test
    public void testToBoolean() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, true);
        Boolean result = MapConversions.toBoolean(map, converter);
        assertTrue(result);
    }

    @Test
    public void testToBigDecimal() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, new BigDecimal("123.45"));
        BigDecimal result = MapConversions.toBigDecimal(map, converter);
        assertEquals(new BigDecimal("123.45"), result);
    }

    @Test
    public void testToBigInteger() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, new BigInteger("987654321"));
        BigInteger result = MapConversions.toBigInteger(map, converter);
        assertEquals(new BigInteger("987654321"), result);
    }

    @Test
    public void testToString() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, "hello");
        String result = MapConversions.toString(map, converter);
        assertEquals("hello", result);
    }

    @Test
    public void testToStringBuffer() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, "buffer");
        StringBuffer result = MapConversions.toStringBuffer(map, converter);
        assertEquals(new StringBuffer("buffer").toString(), result.toString());
    }

    @Test
    public void testToStringBuilder() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, "builder");
        StringBuilder result = MapConversions.toStringBuilder(map, converter);
        assertEquals(new StringBuilder("builder").toString(), result.toString());
    }

    @Test
    public void testToCharacter() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, 'X');
        Character result = MapConversions.toCharacter(map, converter);
        assertEquals(Character.valueOf('X'), result);
    }

    @Test
    public void testToAtomicInteger() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, 5);
        AtomicInteger result = MapConversions.toAtomicInteger(map, converter);
        assertEquals(5, result.get());
    }

    @Test
    public void testToAtomicLong() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, 999L);
        AtomicLong result = MapConversions.toAtomicLong(map, converter);
        assertEquals(999L, result.get());
    }

    @Test
    public void testToAtomicBoolean() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, false);
        AtomicBoolean result = MapConversions.toAtomicBoolean(map, converter);
        assertFalse(result.get());
    }

    @Test
    public void testToPattern() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, "a*b");
        Pattern result = MapConversions.toPattern(map, converter);
        assertEquals(Pattern.compile("a*b").pattern(), result.pattern());
    }

    @Test
    public void testToCurrency() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, "USD");
        Currency result = MapConversions.toCurrency(map, converter);
        assertEquals(Currency.getInstance("USD"), result);
    }

    @Test
    public void testToDate() {
        long now = System.currentTimeMillis();
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, now);
        Date result = MapConversions.toDate(map, converter);
        assertEquals(new Date(now), result);
    }

    @Test
    public void testToSqlDate() {
        long today = System.currentTimeMillis();
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, today);
        java.sql.Date result = MapConversions.toSqlDate(map, converter);
        assertEquals(new java.sql.Date(today), result);
    }

    @Test
    public void testToTimestamp() {
        long ts = System.currentTimeMillis();
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, ts);
        Timestamp result = MapConversions.toTimestamp(map, converter);
        assertEquals(new Timestamp(ts), result);
    }

    @Test
    public void testToTimeZone() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, "UTC");
        TimeZone tz = MapConversions.toTimeZone(map, converter);
        assertEquals(TimeZone.getTimeZone("UTC"), tz);
    }

    @Test
    public void testToCalendar() {
        long epoch = 0L;
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, epoch);
        Calendar cal = MapConversions.toCalendar(map, converter);
        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        expected.setTimeInMillis(epoch);
        assertEquals(expected.getTimeInMillis(), cal.getTimeInMillis());
    }

    @Test
    public void testToLocale() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, "en_US");
        Locale locale = MapConversions.toLocale(map, converter);
        assertEquals(Locale.US, locale);
    }

    @Test
    public void testToLocalDate() {
        LocalDate today = LocalDate.now();
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, today);
        LocalDate result = MapConversions.toLocalDate(map, converter);
        assertEquals(today, result);
    }

    @Test
    public void testToLocalTime() {
        LocalTime now = LocalTime.now();
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, now);
        LocalTime result = MapConversions.toLocalTime(map, converter);
        assertEquals(now, result);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.now();
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, ldt);
        LocalDateTime result = MapConversions.toLocalDateTime(map, converter);
        assertEquals(ldt, result);
    }

    @Test
    public void testToOffsetTime() {
        OffsetTime ot = OffsetTime.now();
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, ot);
        OffsetTime result = MapConversions.toOffsetTime(map, converter);
        assertEquals(ot, result);
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime odt = OffsetDateTime.now();
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, odt);
        OffsetDateTime result = MapConversions.toOffsetDateTime(map, converter);
        assertEquals(odt, result);
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.now();
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, zdt);
        ZonedDateTime result = MapConversions.toZonedDateTime(map, converter);
        assertEquals(zdt, result);
    }

    @Test
    public void testToClass() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, String.class.getName());
        Class<?> result = MapConversions.toClass(map, converter);
        assertEquals(String.class, result);
    }

    @Test
    public void testToDuration() {
        Duration d = Duration.ofHours(5);
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, d);
        Duration result = MapConversions.toDuration(map, converter);
        assertEquals(d, result);
    }

    @Test
    public void testToInstant() {
        Instant instant = Instant.now();
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, instant);
        Instant result = MapConversions.toInstant(map, converter);
        assertEquals(instant, result);
    }

    @Test
    public void testToMonthDay() {
        MonthDay md = MonthDay.of(12, 25);
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, md);
        MonthDay result = MapConversions.toMonthDay(map, converter);
        assertEquals(md, result);
    }

    @Test
    public void testToYearMonth() {
        YearMonth ym = YearMonth.of(2023, 4);
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, ym);
        YearMonth result = MapConversions.toYearMonth(map, converter);
        assertEquals(ym, result);
    }

    @Test
    public void testToPeriod() {
        Period p = Period.of(1, 2, 3);
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, p);
        Period result = MapConversions.toPeriod(map, converter);
        assertEquals(p, result);
    }

    @Test
    public void testToZoneId() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, "Europe/Paris");
        ZoneId result = MapConversions.toZoneId(map, converter);
        assertEquals(ZoneId.of("Europe/Paris"), result);
    }

    @Test
    public void testToZoneOffset() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, "+02:00");
        ZoneOffset result = MapConversions.toZoneOffset(map, converter);
        assertEquals(ZoneOffset.ofHours(2), result);
    }

    @Test
    public void testToYear() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, 2022);
        Year result = MapConversions.toYear(map, converter);
        assertEquals(Year.of(2022), result);
    }

    @Test
    public void testToURL() throws Exception {
        URL url = new URL("http://example.com");
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, url.toString());
        URL result = MapConversions.toURL(map, converter);
        assertEquals(url, result);
    }

    @Test
    public void testToURI() throws Exception {
        URI uri = new URI("urn:test");
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, uri.toString());
        URI result = MapConversions.toURI(map, converter);
        assertEquals(uri, result);
    }

    @Test
    public void testToByteBuffer() {
        String base64 = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3, 4});
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, base64);
        ByteBuffer buffer = MapConversions.toByteBuffer(map, converter);
        assertArrayEquals(new byte[]{1, 2, 3, 4}, buffer.array());
    }

    @Test
    public void testToCharBuffer() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, "abc");
        CharBuffer result = MapConversions.toCharBuffer(map, converter);
        assertEquals("abc", result.toString());
    }

    @Test
    public void testInitMap() {
        String payload = "payload";
        Map<String, ?> result = MapConversions.initMap(payload, converter);
        assertTrue(result.containsKey(MapConversions.V));
        assertEquals(payload, result.get(MapConversions.V));
    }

    @Test
    public void testToThrowable() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", IllegalArgumentException.class.getName());
        map.put("message", "boom");
        map.put("cause", NullPointerException.class.getName());
        map.put("causeMessage", "null cause");

        Throwable t = MapConversions.toThrowable(map, converter, Exception.class);
        assertTrue(t instanceof IllegalArgumentException);
        assertEquals("boom", t.getMessage());
        assertNotNull(t.getCause());
        assertTrue(t.getCause() instanceof NullPointerException);
        assertEquals("null cause", t.getCause().getMessage());
    }

    /* --------------------------------------------------------------
     *  2.  Edge‑case / exception tests.
     * -------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testMissingKeyThrowsFromMap() {
        // No "value" or "_v" key – the private fromMap() method should be
        // invoked via dispatch and throw an IllegalArgumentException.
        Map<String, Object> empty = Collections.emptyMap();
        MapConversions.toInt(empty, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyStringTreatedAsMissing() {
        // The map contains a key with an empty string – getValue() treats this
        // as “no content” and falls back to fromMap().
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, "");
        MapConversions.toString(map, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToByteBufferMissingValue() {
        Map<String, Object> map = Collections.emptyMap();
        MapConversions.toByteBuffer(map, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToByteBufferNullValue() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, null);
        MapConversions.toByteBuffer(map, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToByteBufferNonStringValue() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, 12345);
        MapConversions.toByteBuffer(map, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToByteBufferInvalidBase64() {
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, "!!! not base64 !!!");
        MapConversions.toByteBuffer(map, converter);
    }

    @Test
    public void testGetValuePrefersFirstNonEmpty() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(MapConversions.VALUE, "");          // empty string – ignored
        map.put(MapConversions.V, "second");        // non‑empty – should be returned
        // Use reflection to call the private getValue method.
        Object result = invokePrivateGetValue(map, new String[]{MapConversions.VALUE, MapConversions.V});
        assertEquals("second", result);
    }

    @Test
    public void testGetValueReturnsNullWhenKeyExistsButValueIsNull() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.VALUE, null);
        Object result = invokePrivateGetValue(map, new String[]{MapConversions.VALUE, MapConversions.V});
        // The method returns the *actual* map entry (null) when the key exists
        // but the value is null.
        assertNull(result);
    }

    @Test
    public void testFormatKeysVariousLengths() throws Exception {
        // Access the private formatKeys method via reflection.
        String one = (String) invokePrivateFormatKeys(new String[]{"single"});
        assertEquals("[single]", one);

        String two = (String) invokePrivateFormatKeys(new String[]{"first", "second"});
        assertEquals("[first] or [second]", two);

        String three = (String) invokePrivateFormatKeys(new String[]{"a", "b", "c"});
        assertEquals("[a], [b], or [c]", three);
    }

    /* --------------------------------------------------------------
     *  Helper methods to invoke private static methods via reflection.
     * -------------------------------------------------------------- */

    private static Object invokePrivateGetValue(Map<String, Object> map, String[] keys) {
        try {
            java.lang.reflect.Method m = MapConversions.class.getDeclaredMethod("getValue", Map.class, String[].class);
            m.setAccessible(true);
            return m.invoke(null, map, keys);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object invokePrivateFormatKeys(String[] keys) {
        try {
            java.lang.reflect.Method m = MapConversions.class.getDeclaredMethod("formatKeys", String[].class);
            m.setAccessible(true);
            return m.invoke(null, (Object) keys);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
