package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactMap;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.Map;

/**
 * Test suite for {@link ThrowableConversions}.
 * Covers normal operation, edge cases and exception handling.
 */
public class ThrowableConversionsTest {

    private Converter converter;   // a real Converter instance (no special behavior needed)

    @Before
    public void setUp() {
        // Converter requires a ConverterOptions instance; using default constructor.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Helper to invoke the package‑private static method via reflection.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> callToMap(Object from) throws Exception {
        java.lang.reflect.Method m = ThrowableConversions.class.getDeclaredMethod("toMap", Object.class, Converter.class);
        m.setAccessible(true);
        return (Map<String, Object>) m.invoke(null, from, converter);
    }

    /* ----------------------------------------------------
     *  Basic functionality tests
     * ---------------------------------------------------- */

    @Test
    public void testToMap_WithSimpleException_NoCause() throws Exception {
        Exception ex = new IllegalArgumentException("bad argument");
        Map<String, Object> map = callToMap(ex);

        assertEquals("Class name must be stored",
                IllegalArgumentException.class.getName(),
                map.get(MapConversions.CLASS));

        assertEquals("Message must be stored",
                "bad argument",
                map.get(MapConversions.MESSAGE));

        // No cause fields should be present
        assertFalse("CAUSE key should be absent when no cause", map.containsKey(MapConversions.CAUSE));
        assertFalse("CAUSE_MESSAGE key should be absent when no cause", map.containsKey(MapConversions.CAUSE_MESSAGE));
    }

    @Test
    public void testToMap_WithExceptionHavingCause() throws Exception {
        NullPointerException cause = new NullPointerException("null pointer");
        RuntimeException ex = new RuntimeException("wrapper", cause);
        Map<String, Object> map = callToMap(ex);

        assertEquals(RuntimeException.class.getName(), map.get(MapConversions.CLASS));
        assertEquals("wrapper", map.get(MapConversions.MESSAGE));

        // Cause information must be present
        assertEquals(NullPointerException.class.getName(), map.get(MapConversions.CAUSE));
        assertEquals("null pointer", map.get(MapConversions.CAUSE_MESSAGE));
    }

    @Test
    public void testToMap_WithExceptionHavingNullMessage() throws Exception {
        Exception ex = new Exception((String) null);
        Map<String, Object> map = callToMap(ex);

        assertEquals(Exception.class.getName(), map.get(MapConversions.CLASS));
        assertNull("Message should be null when exception message is null", map.get(MapConversions.MESSAGE));
    }

    @Test
    public void testToMap_WithExceptionHavingNullCause() throws Exception {
        // Throwable can have a cause that is explicitly null (via initCause(null))
        Exception ex = new Exception("msg");
        ex.initCause(null); // explicit null cause
        Map<String, Object> map = callToMap(ex);

        assertEquals(Exception.class.getName(), map.get(MapConversions.CLASS));
        assertEquals("msg", map.get(MapConversions.MESSAGE));
        assertFalse("CAUSE key must be absent when cause is null", map.containsKey(MapConversions.CAUSE));
        assertFalse("CAUSE_MESSAGE key must be absent when cause is null", map.containsKey(MapConversions.CAUSE_MESSAGE));
    }

    /* ----------------------------------------------------
     *  Edge‑case and error handling tests
     * ---------------------------------------------------- */

    @Test(expected = ClassCastException.class)
    public void testToMap_WithNonThrowableObject() throws Exception {
        // The method casts the input to Throwable; passing a non‑Throwable must throw ClassCastException.
        Object notThrowable = new Object();
        callToMap(notThrowable);
    }

    @Test
    public void testToMap_WithThrowableHavingSelfCause() throws Exception {
        // Some Throwables (e.g., Throwable) allow setting themselves as cause – this should be handled gracefully.
        Throwable self = new Throwable("self");
        self.initCause(self); // creates a circular reference
        Map<String, Object> map = callToMap(self);

        assertEquals(Throwable.class.getName(), map.get(MapConversions.CLASS));
        assertEquals("self", map.get(MapConversions.MESSAGE));
        // The implementation stores only class name and message of the cause, no recursion.
        assertEquals(Throwable.class.getName(), map.get(MapConversions.CAUSE));
        assertEquals("self", map.get(MapConversions.CAUSE_MESSAGE));
    }

    @Test
    public void testToMap_ReturnsCompactMapImplementation() throws Exception {
        Exception ex = new IllegalStateException("state");
        Map<String, Object> map = callToMap(ex);
        // The builder creates a CompactMap with insertion order; verify the concrete class.
        assertTrue("Result should be a CompactMap", map instanceof CompactMap);
        // Verify insertion order is preserved (CLASS first, then MESSAGE, then optional cause entries)
        String[] expectedOrder = map.containsKey(MapConversions.CAUSE) ?
                new String[]{MapConversions.CLASS, MapConversions.MESSAGE, MapConversions.CAUSE, MapConversions.CAUSE_MESSAGE} :
                new String[]{MapConversions.CLASS, MapConversions.MESSAGE};

        int index = 0;
        for (String key : map.keySet()) {
            assertEquals("Key order should follow insertion order", expectedOrder[index++], key);
        }
    }

    @Test
    public void testToMap_WithCustomThrowableSubclass() throws Exception {
        class MyError extends Error {
            MyError(String msg) { super(msg); }
        }
        MyError err = new MyError("my error");
        Map<String, Object> map = callToMap(err);

        assertEquals(MyError.class.getName(), map.get(MapConversions.CLASS));
        assertEquals("my error", map.get(MapConversions.MESSAGE));
    }

    @Test
    public void testToMap_WithThrowableHavingEmptyMessage() throws Exception {
        Throwable t = new Throwable("");
        Map<String, Object> map = callToMap(t);
        assertEquals("", map.get(MapConversions.MESSAGE));
    }

    @Test
    public void testToMap_DoesNotModifyOriginalThrowable() throws Exception {
        Exception original = new Exception("original");
        callToMap(original);
        // Ensure that getCause() and getMessage() remain unchanged after conversion.
        assertNull(original.getCause());
        assertEquals("original", original.getMessage());
    }
}
