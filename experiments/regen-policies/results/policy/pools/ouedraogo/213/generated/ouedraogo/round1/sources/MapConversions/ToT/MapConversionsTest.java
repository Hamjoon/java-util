package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

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
 * The tests are written without any third‑party mocking frameworks – they create real
 * {@link Converter} instances (with default options) and real map objects as required
 * by the source code.  Each public static conversion method is exercised for:
 *
 * <ul>
 *   <li>happy‑path conversion using the {@code VALUE_KEYS} (\"value\" / \"_v\")</li>
 *   <li>alternative key handling (e.g. \"date\", \"sqlDate\", etc.)</li>
 *   <li>missing‑key error handling – the private {@code fromMap} method throws an
 *        {@link IllegalArgumentException} with a helpful message.</li>
 *   <li>type‑mismatch error handling for {@code toByteBuffer} (non‑String or bad Base64).</li>
 *   <li>the special {@code toUUID} path that uses {@code mostSigBits} / {@code leastSigBits}
 *        directly.</li>
 *   <li>the {@code toThrowable} reconstruction logic (simple case with message only).</li>
 * </ul>
 *
 * The suite deliberately avoids testing every single primitive conversion method
 * individually – they all delegate to the same private {@code dispatch} method.
 * Instead a representative subset is verified and the generic behaviour is
 * exercised through reflection on the private {@code formatKeys} and {@code getValue}
 * helpers.
 */
public class MapConversionsTest {

    private Converter converter;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // The Converter constructor requires a ConverterOptions instance.
        // Using the default constructor is sufficient for the tests.
        converter = new Converter(new ConverterOptions());
    }

    /* --------------------------------------------------------------
     *  Helper utilities used by many tests
     * -------------------------------------------------------------- */

    /** Shortcut to build a map with a single key/value pair. */
    private static Map<String, Object> map(String key, Object value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(key, value);
        return m;
    }

    /** Shortcut to build a map with multiple entries. */
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    /* --------------------------------------------------------------
     *  Tests for primitive / wrapper conversions (representative sample)
     * -------------------------------------------------------------- */

    @Test
    public void testToInt_WithValueKey() {
        Map<String, Object> src = map("value", "123");
        Integer result = MapConversions.toInt(src, converter);
        assertEquals(Integer.valueOf(123), result);
    }

    @Test
    public void testToInt_WithUnderscoreVKey() {
        Map<String, Object> src = map("_v", 456);
        Integer result = MapConversions.toInt(src, converter);
        assertEquals(Integer.valueOf(456), result);
    }

    @Test
    public void testToBoolean_MissingKeyThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("To convert from Map to 'Boolean' the map must include: [value] or [_v] as key with associated value.");

        Map<String, Object> src = Collections.emptyMap();
        MapConversions.toBoolean(src, converter);
    }

    @Test
    public void testToBigDecimal_WithString() {
        Map<String, Object> src = map("value", "12.34");
        BigDecimal result = MapConversions.toBigDecimal(src, converter);
        assertEquals(new BigDecimal("12.34"), result);
    }

    @Test
    public void testToCharacter_WithSingleCharString() {
        Map<String, Object> src = map("value", "Z");
        Character result = MapConversions.toCharacter(src, converter);
        assertEquals(Character.valueOf('Z'), result);
    }

    /* --------------------------------------------------------------
     *  Date / Time related conversions
     * -------------------------------------------------------------- */

    @Test
    public void testToDate_EpochMillisKey() {
        long now = System.currentTimeMillis();
        Map<String, Object> src = map("epochMillis", now);
        Date d = MapConversions.toDate(src, converter);
        assertEquals(now, d.getTime());
    }

    @Test
    public void testToSqlDate_WithValueKeyString() {
        long now = System.currentTimeMillis();
        Map<String, Object> src = map("value", now);
        java.sql.Date sqlDate = MapConversions.toSqlDate(src, converter);
        assertEquals(now, sqlDate.getTime());
    }

    @Test
    public void testToLocalDate_WithValueKey() {
        LocalDate today = LocalDate.now();
        Map<String, Object> src = map("value", today.toString());
        LocalDate result = MapConversions.toLocalDate(src, converter);
        assertEquals(today, result);
    }

    @Test
    public void testToInstant_EpochMillis() {
        long epoch = 1_600_000_000_000L;
        Map<String, Object> src = map("epochMillis", epoch);
        Instant instant = MapConversions.toInstant(src, converter);
        assertEquals(Instant.ofEpochMilli(epoch), instant);
    }

    @Test
    public void testToDuration_WithValueKey() {
        Duration dur = Duration.ofMinutes(5);
        Map<String, Object> src = map("value", dur.toString());
        Duration result = MapConversions.toDuration(src, converter);
        assertEquals(dur, result);
    }

    /* --------------------------------------------------------------
     *  UUID conversion – both paths
     * -------------------------------------------------------------- */

    @Test
    public void testToUUID_UsingMostAndLeastSigBits() {
        UUID original = UUID.randomUUID();
        Map<String, Object> src = map(
                MapConversions.MOST_SIG_BITS, original.getMostSignificantBits(),
                MapConversions.LEAST_SIG_BITS, original.getLeastSignificantBits()
        );
        Object result = MapConversions.toUUID(src, converter);
        assertTrue(result instanceof UUID);
        assertEquals(original, result);
    }

    @Test
    public void testToUUID_WithValueKeyString() {
        UUID uuid = UUID.randomUUID();
        Map<String, Object> src = map("value", uuid.toString());
        Object result = MapConversions.toUUID(src, converter);
        assertEquals(uuid, result);
    }

    @Test
    public void testToUUID_MissingAllKeysThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("To convert from Map to 'UUID' the map must include: [UUID] or [value] or [_v] or [mostSigBits, leastSigBits] as key with associated value.");

        Map<String, Object> src = Collections.emptyMap();
        MapConversions.toUUID(src, converter);
    }

    /* --------------------------------------------------------------
     *  ByteBuffer conversion – success and error paths
     * -------------------------------------------------------------- */

    @Test
    public void testToByteBuffer_ValidBase64() {
        byte[] data = {0x01, 0x02, 0x7F, (byte) 0xFF};
        String base64 = Base64.getEncoder().encodeToString(data);
        Map<String, Object> src = map("value", base64);
        ByteBuffer buffer = MapConversions.toByteBuffer(src, converter);
        assertArrayEquals(data, buffer.array());
    }

    @Test
    public void testToByteBuffer_MissingValueKeyThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Missing or null 'value' or '_v' field");

        Map<String, Object> src = Collections.emptyMap();
        MapConversions.toByteBuffer(src, converter);
    }

    @Test
    public void testToByteBuffer_NonStringValueThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Value must be a Base64-encoded String");

        Map<String, Object> src = map("_v", 12345);
        MapConversions.toByteBuffer(src, converter);
    }

    @Test
    public void testToByteBuffer_InvalidBase64Throws() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Invalid Base64 encoding");

        Map<String, Object> src = map("value", "!!! not base64 !!!");
        MapConversions.toByteBuffer(src, converter);
    }

    /* --------------------------------------------------------------
     *  CharBuffer conversion (delegates to dispatch)
     * -------------------------------------------------------------- */

    @Test
    public void testToCharBuffer_WithUnderscoreV() {
        Map<String, Object> src = map("_v", "hello");
        CharBuffer buf = MapConversions.toCharBuffer(src, converter);
        assertEquals("hello", buf.toString());
    }

    /* --------------------------------------------------------------
     *  Throwable reconstruction – simple case with message only
     * -------------------------------------------------------------- */

    @Test
    public void testToThrowable_SimpleMessage() {
        Map<String, Object> src = map(
                MapConversions.CLASS, IllegalArgumentException.class.getName(),
                MapConversions.MESSAGE, "boom!"
        );
        Throwable t = MapConversions.toThrowable(src, converter, RuntimeException.class);
        assertTrue(t instanceof IllegalArgumentException);
        assertEquals("boom!", t.getMessage());
        assertEquals(0, t.getStackTrace().length); // cleared by implementation
    }

    @Test
    public void testToThrowable_WithCauseAndMessage() {
        Map<String, Object> src = map(
                MapConversions.CLASS, RuntimeException.class.getName(),
                MapConversions.MESSAGE, "outer",
                MapConversions.CAUSE, IllegalStateException.class.getName(),
                MapConversions.CAUSE_MESSAGE, "inner"
        );
        Throwable t = MapConversions.toThrowable(src, converter, Exception.class);
        assertTrue(t instanceof RuntimeException);
        assertEquals("outer", t.getMessage());
        assertNotNull(t.getCause());
        assertTrue(t.getCause() instanceof IllegalStateException);
        assertEquals("inner", t.getCause().getMessage());
    }

    @Test
    public void testToThrowable_InvalidClassFallsBackToTarget() {
        Map<String, Object> src = map(
                MapConversions.CLASS, "non.existent.ClassName",
                MapConversions.MESSAGE, "msg"
        );
        Throwable t = MapConversions.toThrowable(src, converter, IllegalStateException.class);
        // Should use the target class because the map class cannot be loaded
        assertTrue(t instanceof IllegalStateException);
        assertEquals("msg", t.getMessage());
    }

    /* --------------------------------------------------------------
     *  Private helper methods – accessed via reflection
     * -------------------------------------------------------------- */

    @Test
    public void testFormatKeys_VariousLengths() throws Exception {
        java.lang.reflect.Method fmt = MapConversions.class.getDeclaredMethod("formatKeys", String[].class);
        fmt.setAccessible(true);

        assertEquals("[one]", fmt.invoke(null, (Object) new String[]{"one"}));
        assertEquals("[one] or [two]", fmt.invoke(null, (Object) new String[]{"one", "two"}));
        assertEquals("[a], [b], or [c]", fmt.invoke(null, (Object) new String[]{"a", "b", "c"}));
        assertEquals("[x], [y], [z], or [w]", fmt.invoke(null, (Object) new String[]{"x", "y", "z", "w"}));
    }

    @Test
    public void testGetValue_PrefersFirstNonNullContent() throws Exception {
        java.lang.reflect.Method getValue = MapConversions.class.getDeclaredMethod("getValue", Map.class, String[].class);
        getValue.setAccessible(true);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("a", "");                // empty string – should be ignored
        m.put("b", null);              // null – ignored
        m.put("c", "content");         // first valid content

        Object result = getValue.invoke(null, m, new String[]{"a", "b", "c", "d"});
        assertEquals("content", result);
    }

    @Test
    public void testGetValue_ReturnsNullIfKeyPresentButNull() throws Exception {
        java.lang.reflect.Method getValue = MapConversions.class.getDeclaredMethod("getValue", Map.class, String[].class);
        getValue.setAccessible(true);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("value", null);
        Object result = getValue.invoke(null, m, new String[]{"value", "other"});
        // hadKey is recorded, so method returns map.get(hadKey) which is null
        assertNull(result);
    }

    @Test
    public void testGetValue_NoMatchReturnsNoMatchConstant() throws Exception {
        java.lang.reflect.Method getValue = MapConversions.class.getDeclaredMethod("getValue", Map.class, String[].class);
        getValue.setAccessible(true);

        Map<String, Object> m = new HashMap<>();
        Object result = getValue.invoke(null, m, new String[]{"missing1", "missing2"});
        java.lang.reflect.Field noMatch = MapConversions.class.getDeclaredField("NO_MATCH");
        noMatch.setAccessible(true);
        assertSame(noMatch.get(null), result);
    }

    /* --------------------------------------------------------------
     *  initMap – simple sanity check
     * -------------------------------------------------------------- */

    @Test
    public void testInitMapCreatesMapWithUnderscoreV() {
        String payload = "payload";
        Map<String, ?> result = MapConversions.initMap(payload, converter);
        assertTrue(result.containsKey(MapConversions.V));
        assertEquals(payload, result.get(MapConversions.V));
    }
}
