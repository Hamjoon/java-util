package com.cedarsoftware.util.convert;

import static com.cedarsoftware.util.convert.MapConversions.CAUSE;
import static com.cedarsoftware.util.convert.MapConversions.CAUSE_MESSAGE;
import static com.cedarsoftware.util.convert.MapConversions.CLASS;
import static com.cedarsoftware.util.convert.MapConversions.MESSAGE;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for {@link ThrowableConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Conversion of a {@link Throwable} without a cause.</li>
 *   <li>Conversion of a {@link Throwable} with a cause.</li>
 *   <li>Handling of {@code null} messages and {@code null} causes.</li>
 *   <li>Conversion of a custom {@link Throwable} subclass.</li>
 *   <li>Verification that the resulting {@link java.util.LinkedHashMap} preserves insertion order.</li>
 * </ul>
 *
 * The {@code Converter} argument of {@code ThrowableConversions.toMap} is not used by the
 * implementation, therefore {@code null} is passed in all test cases.
 */
public class ThrowableConversionsTest {

    @Test
    public void testToMap_NoCause_NoMessage() {
        Exception ex = new Exception("simple error");
        Map<String, Object> map = ThrowableConversions.toMap(ex, null);

        // Basic fields must be present
        assertEquals("java.lang.Exception", map.get(CLASS));
        assertEquals("simple error", map.get(MESSAGE));

        // Cause related fields must be absent
        assertFalse("Map should not contain CAUSE when there is no cause", map.containsKey(CAUSE));
        assertFalse("Map should not contain CAUSE_MESSAGE when there is no cause", map.containsKey(CAUSE_MESSAGE));

        // Verify insertion order (CLASS, MESSAGE)
        Iterator<String> it = map.keySet().iterator();
        assertEquals(CLASS, it.next());
        assertEquals(MESSAGE, it.next());
        assertFalse("No further entries expected", it.hasNext());
    }

    @Test
    public void testToMap_WithCause() {
        IllegalArgumentException cause = new IllegalArgumentException("inner problem");
        RuntimeException top = new RuntimeException("outer problem", cause);
        Map<String, Object> map = ThrowableConversions.toMap(top, null);

        // Verify top‑level fields
        assertEquals("java.lang.RuntimeException", map.get(CLASS));
        assertEquals("outer problem", map.get(MESSAGE));

        // Verify cause fields
        assertEquals("java.lang.IllegalArgumentException", map.get(CAUSE));
        assertEquals("inner problem", map.get(CAUSE_MESSAGE));

        // Verify insertion order (CLASS, MESSAGE, CAUSE, CAUSE_MESSAGE)
        Iterator<String> it = map.keySet().iterator();
        assertEquals(CLASS, it.next());
        assertEquals(MESSAGE, it.next());
        assertEquals(CAUSE, it.next());
        assertEquals(CAUSE_MESSAGE, it.next());
        assertFalse("No further entries expected", it.hasNext());
    }

    @Test
    public void testToMap_NullMessageAndNullCause() {
        Exception ex = new Exception(null, null);
        Map<String, Object> map = ThrowableConversions.toMap(ex, null);

        // Message is null but key must exist
        assertTrue("Map must contain MESSAGE key even when value is null", map.containsKey(MESSAGE));
        assertNull("MESSAGE value should be null", map.get(MESSAGE));

        // Class name must be present
        assertEquals("java.lang.Exception", map.get(CLASS));

        // No cause related entries
        assertFalse(map.containsKey(CAUSE));
        assertFalse(map.containsKey(CAUSE_MESSAGE));

        // Order check (CLASS, MESSAGE)
        Iterator<String> it = map.keySet().iterator();
        assertEquals(CLASS, it.next());
        assertEquals(MESSAGE, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testToMap_CustomThrowableSubclass() {
        MyError error = new MyError("custom error");
        Map<String, Object> map = ThrowableConversions.toMap(error, null);

        assertEquals(MyError.class.getName(), map.get(CLASS));
        assertEquals("custom error", map.get(MESSAGE));
        assertFalse(map.containsKey(CAUSE));
        assertFalse(map.containsKey(CAUSE_MESSAGE));

        // Order verification
        Iterator<String> it = map.keySet().iterator();
        assertEquals(CLASS, it.next());
        assertEquals(MESSAGE, it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Simple {@link Error} subclass used to verify that
     * {@code ThrowableConversions} works with any {@link Throwable} type.
     */
    private static final class MyError extends Error {
        MyError(String message) {
            super(message);
        }
    }
}
