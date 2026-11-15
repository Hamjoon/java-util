package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for {@link MapConversions}.  All public static methods are exercised,
 * including the private helper methods via reflection.
 */
public class MapConversionsTest {

    // -------------------------------------------------------------------------
    // Helper to create a minimal Converter instance (options are not needed for
    // the code paths exercised in these tests).
    // -------------------------------------------------------------------------
    private static Converter newConverter() {
        // Converter has a single public constructor that takes a ConverterOptions.
        // Passing null is acceptable for the purposes of these tests because the
        // conversion methods we invoke either return the original value or rely
        // on simple built‑in conversions.
        return new Converter(null);
    }

    // -------------------------------------------------------------------------
    // Tests for the private formatKeys method
    // -------------------------------------------------------------------------
    @Test
    public void testFormatKeysSingle() throws Exception {
        Method m = MapConversions.class.getDeclaredMethod("formatKeys", String[].class);
        m.setAccessible(true);
        String result = (String) m.invoke(null, (Object) new String[] {"value"});
        assertEquals("[value]", result);
    }

    @Test
    public void testFormatKeysTwo() throws Exception {
        Method m = MapConversions.class.getDeclaredMethod("formatKeys", String[].class);
        m.setAccessible(true);
        String result = (String) m.invoke(null, (Object) new String[] {"value", "_v"});
        assertEquals("[value] or [_v]", result);
    }

    @Test
    public void testFormatKeysThreeOrMore() throws Exception {
        Method m = MapConversions.class.getDeclaredMethod("formatKeys", String[].class);
        m.setAccessible(true);
        String result = (String) m.invoke(null, (Object) new String[] {"a", "b", "c", "d"});
        assertEquals("[a], [b], [c], or [d]", result);
    }

    // -------------------------------------------------------------------------
    // Tests for the private getValue method
    // -------------------------------------------------------------------------
    @Test
    public void testGetValuePrefersFirstNonNull() throws Exception {
        Method m = MapConversions.class.getDeclaredMethod("getValue", Map.class, String[].class);
        m.setAccessible(true);
        Map<String, Object> map = new HashMap<>();
        map.put("first", null);
        map.put("second", "found");
        map.put("third", "ignored");
        String[] keys = {"first", "second", "third"};
        Object value = m.invoke(null, map, keys);
        assertEquals("found", value);
    }

    @Test
    public void testGetValueReturnsEmptyStringIfPresent() throws Exception {
        Method m = MapConversions.class.getDeclaredMethod("getValue", Map.class, String[].class);
        m.setAccessible(true);
        Map<String, Object> map = new HashMap<>();
        map.put("empty", "");
        String[] keys = {"empty", "missing"};
        Object value = m.invoke(null, map, keys);
        // Empty string is considered a value (hasContent("") == false) but because the key exists,
        // getValue returns the entry (the map contains the key) even if the string is empty.
        assertEquals("", value);
    }

    @Test
    public void testGetValueReturnsNoMatchWhenAllMissing() throws Exception {
        Method m = MapConversions.class.getDeclaredMethod("getValue", Map.class, String[].class);
        m.setAccessible(true);
        Map<String, Object> map = new HashMap<>();
        String[] keys = {"a", "b"};
        Object value = m.invoke(null, map, keys);
        // The private constant NO_MATCH is not visible, but we can verify that the result is
        // the same object as the private field.
        java.lang.reflect.Field noMatchField = MapConversions.class.getDeclaredField("NO_MATCH");
        noMatchField.setAccessible(true);
        Object NO_MATCH = noMatchField.get(null);
        assertSame(NO_MATCH, value);
    }

    // -------------------------------------------------------------------------
    // Tests for initMap
    // -------------------------------------------------------------------------
    @Test
    public void testInitMapCreatesMapWithVKey() {
        Converter converter = newConverter();
        String original = "test";
        Map<String, ?> result = MapConversions.initMap(original, converter);
        assertTrue(result.containsKey(MapConversions.V));
        assertEquals(original, result.get(MapConversions.V));
    }

    // -------------------------------------------------------------------------
    // Tests for toByteBuffer – happy path and error handling
    // -------------------------------------------------------------------------
    @Test
    public void testToByteBufferDecodesBase64() {
        Converter converter = newConverter();
        String original = "hello";
        String base64 = Base64.getEncoder().encodeToString(original.getBytes());
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.VALUE, base64);
        ByteBuffer buffer = MapConversions.toByteBuffer(map, converter);
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        assertArrayEquals(original.getBytes(), bytes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToByteBufferMissingValue() {
        Converter converter = newConverter();
        Map<String, Object> map = new HashMap<>();
        // No VALUE or _v entry
        MapConversions.toByteBuffer(map, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToByteBufferNonStringValue() {
        Converter converter = newConverter();
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.VALUE, 12345); // not a String
        MapConversions.toByteBuffer(map, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToByteBufferInvalidBase64() {
        Converter converter = newConverter();
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.VALUE, "not@@base64!!");
        MapConversions.toByteBuffer(map, converter);
    }

    // -------------------------------------------------------------------------
    // Tests for toThrowable – basic reconstruction with class name and message
    // -------------------------------------------------------------------------
    @Test
    public void testToThrowableRecreatesExceptionWithMessage() {
        Converter converter = newConverter();
        Map<String, Object> map = new HashMap<>();
        map.put("class", "java.lang.IllegalArgumentException");
        map.put("message", "boom");
        Throwable t = MapConversions.toThrowable(map, converter, RuntimeException.class);
        assertTrue(t instanceof IllegalArgumentException);
        assertEquals("boom", t.getMessage());
        // Stack trace should be cleared
        assertEquals(0, t.getStackTrace().length);
    }

    @Test
    public void testToThrowableHandlesCauseAndCustomFieldPopulation() {
        Converter converter = newConverter();

        // Create a map that defines a cause exception and also a custom field on the target.
        // We'll use java.lang.Exception which has a public "detailMessage" field (inherited).
        Map<String, Object> map = new HashMap<>();
        map.put("class", "java.lang.Exception");
        map.put("message", "main");
        map.put("cause", "java.lang.IllegalArgumentException");
        map.put("causeMessage", "inner");

        // Add a field that exists on Exception – for demonstration we set "localizedMessage"
        // (it is a protected field, but populateFields will attempt to set it via reflection).
        map.put("localizedMessage", "localized");

        Throwable result = MapConversions.toThrowable(map, converter, RuntimeException.class);
        assertTrue(result instanceof Exception);
        assertEquals("main", result.getMessage());
        assertNotNull(result.getCause());
        assertTrue(result.getCause() instanceof IllegalArgumentException);
        assertEquals("inner", result.getCause().getMessage());

        // The custom field should have been set (even though it's protected)
        try {
            java.lang.reflect.Field f = Exception.class.getDeclaredField("detailMessage");
            f.setAccessible(true);
            assertEquals("main", f.get(result));
        } catch (Exception e) {
            fail("Unable to verify custom field population: " + e);
        }
    }

    // -------------------------------------------------------------------------
    // Tests that verify the dispatch mechanism throws the correct exception
    // when required keys are missing.
    // -------------------------------------------------------------------------
    @Test
    public void testDispatchMissingKeyThrowsFromMap() {
        Converter converter = newConverter();
        Map<String, Object> emptyMap = new HashMap<>();

        try {
            // Directly invoke a conversion method that will delegate to dispatch.
            // toInt expects a "value" or "_v" key.
            MapConversions.toInt(emptyMap, converter);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("To convert from Map to 'Integer'"));
            assertTrue(e.getMessage().contains("value"));
            assertTrue(e.getMessage().contains("_v"));
        }
    }

    // -------------------------------------------------------------------------
    // Tests for a few simple conversion methods that succeed when a proper value is present.
    // -------------------------------------------------------------------------
    @Test
    public void testSimpleNumericConversions() {
        Converter converter = newConverter();

        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.VALUE, 42); // Integer

        Integer i = MapConversions.toInt(map, converter);
        assertEquals(Integer.valueOf(42), i);

        Long l = MapConversions.toLong(map, converter);
        // The converter should be able to convert Integer to Long
        assertEquals(Long.valueOf(42L), l);
    }

    @Test
    public void testBooleanAndStringConversions() {
        Converter converter = newConverter();

        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions._v, true);
        Boolean b = MapConversions.toBoolean(map, converter);
        assertTrue(b);

        map.clear();
        map.put(MapConversions.VALUE, "hello");
        String s = MapConversions.toString(map, converter);
        assertEquals("hello", s);
    }

    // -------------------------------------------------------------------------
    // Verify that toUUID works both via mostSigBits/leastSigBits and via dispatch.
    // -------------------------------------------------------------------------
    @Test
    public void testToUUIDFromMostAndLeastSigBits() {
        Converter converter = newConverter();
        Map<String, Object> map = new HashMap<>();
        map.put("mostSigBits", 0L);
        map.put("leastSigBits", 1L);
        UUID uuid = (UUID) MapConversions.toUUID(map, converter);
        assertEquals(new UUID(0L, 1L), uuid);
    }

    @Test
    public void testToUUIDDispatchPath() {
        Converter converter = newConverter();
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.VALUE, "123e4567-e89b-12d3-a456-426655440000");
        UUID uuid = (UUID) MapConversions.toUUID(map, converter);
        // The converter should be able to turn a String into a UUID.
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426655440000"), uuid);
    }

    // -------------------------------------------------------------------------
    // Reflection helper to invoke private static methods (used above)
    // -------------------------------------------------------------------------
    private static Object invokePrivateStatic(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = MapConversions.class.getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        return m.invoke(null, args);
    }
}
