package com.cedarsoftware.util.convert;

import org.junit.Test;

import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.CAUSE;
import static com.cedarsoftware.util.convert.MapConversions.CAUSE_MESSAGE;
import static com.cedarsoftware.util.convert.MapConversions.CLASS;
import static com.cedarsoftware.util.convert.MapConversions.MESSAGE;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link ThrowableConversions}.
 *
 * The tests cover all logical branches of {@code ThrowableConversions.toMap(...)}:
 * <ul>
 *   <li>Throwable without a cause</li>
 *   <li>Throwable with a cause</li>
 *   <li>Null messages (both for the throwable and its cause)</li>
 *   <li>Custom {@code Throwable} subclasses</li>
 *   <li>Invalid {@code from} argument types (expecting {@code ClassCastException})</li>
 *   <li>Null {@code from} argument (expecting {@code NullPointerException})</li>
 * </ul>
 *
 * The {@code Converter} argument is not used by the method under test, but a real
 * instance is supplied to satisfy the method signature.
 */
public class ThrowableConversionsTest {

    /** Helper to create a minimal {@link Converter} instance. */
    private static Converter createConverter() {
        // ConverterOptions has a public no‑arg constructor (as per the library API).
        return new Converter(new ConverterOptions());
    }

    @Test
    public void testToMapWithoutCause() {
        String msg = "simple error";
        IllegalArgumentException ex = new IllegalArgumentException(msg);

        Map<String, Object> map = ThrowableConversions.toMap(ex, createConverter());

        // Expected entries
        assertEquals(2, map.size());
        assertEquals(IllegalArgumentException.class.getName(), map.get(CLASS));
        assertEquals(msg, map.get(MESSAGE));

        // Cause related entries must be absent
        assertFalse(map.containsKey(CAUSE));
        assertFalse(map.containsKey(CAUSE_MESSAGE));
    }

    @Test
    public void testToMapWithCause() {
        String causeMsg = "root cause";
        NullPointerException cause = new NullPointerException(causeMsg);
        RuntimeException wrapper = new RuntimeException("wrapper", cause);

        Map<String, Object> map = ThrowableConversions.toMap(wrapper, createConverter());

        // Expected entries
        assertEquals(4, map.size());
        assertEquals(RuntimeException.class.getName(), map.get(CLASS));
        assertEquals("wrapper", map.get(MESSAGE));
        assertEquals(NullPointerException.class.getName(), map.get(CAUSE));
        assertEquals(causeMsg, map.get(CAUSE_MESSAGE));
    }

    @Test
    public void testToMapWithNullMessages() {
        // Cause with null message
        Exception cause = new Exception();
        // Throwable with null message but with a cause
        Exception ex = new Exception(null, cause);

        Map<String, Object> map = ThrowableConversions.toMap(ex, createConverter());

        assertEquals(4, map.size());
        assertEquals(Exception.class.getName(), map.get(CLASS));
        assertNull(map.get(MESSAGE));                 // top‑level message is null
        assertEquals(Exception.class.getName(), map.get(CAUSE));
        assertNull(map.get(CAUSE_MESSAGE));           // cause message is null
    }

    /**
     * Verify that a custom subclass of {@link Throwable} is handled correctly.
     */
    @Test
    public void testToMapWithCustomThrowableSubclass() {
        class MyError extends Error {
            MyError(String message) { super(message); }
        }

        MyError error = new MyError("my error");
        Map<String, Object> map = ThrowableConversions.toMap(error, createConverter());

        assertEquals(2, map.size());
        assertEquals(MyError.class.getName(), map.get(CLASS));
        assertEquals("my error", map.get(MESSAGE));
        assertFalse(map.containsKey(CAUSE));
        assertFalse(map.containsKey(CAUSE_MESSAGE));
    }

    /**
     * The static method casts the supplied object to {@link Throwable} without checking.
     * Passing an incompatible type should result in a {@link ClassCastException}.
     */
    @Test(expected = ClassCastException.class)
    public void testToMapWithInvalidFromObjectThrowsClassCastException() {
        String notAThrowable = "I am not a throwable";
        ThrowableConversions.toMap(notAThrowable, createConverter());
    }

    /**
     * Passing {@code null} as the source object leads to a {@link NullPointerException}
     * when the method attempts to dereference the casted value.
     */
    @Test(expected = NullPointerException.class)
    public void testToMapWithNullFromThrowsNullPointerException() {
        ThrowableConversions.toMap(null, createConverter());
    }

    /**
     * Ensure that the returned map preserves insertion order (LinkedHashMap behavior).
     * The order should be: CLASS, MESSAGE, (optional) CAUSE, CAUSE_MESSAGE.
     */
    @Test
    public void testMapInsertionOrder() {
        Exception cause = new Exception("cause");
        Exception ex = new Exception("top", cause);

        Map<String, Object> map = ThrowableConversions.toMap(ex, createConverter());

        // Verify ordering by iterating over the entry set.
        String[] expectedKeys = {CLASS, MESSAGE, CAUSE, CAUSE_MESSAGE};
        int index = 0;
        for (String key : map.keySet()) {
            assertEquals("Unexpected key order at position " + index, expectedKeys[index], key);
            index++;
        }
        assertEquals("Map should contain exactly 4 entries", 4, map.size());
    }
}
