package com.cedarsoftware.util.convert;

import org.junit.Test;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.CLASS;
import static com.cedarsoftware.util.convert.MapConversions.MESSAGE;
import static com.cedarsoftware.util.convert.MapConversions.CAUSE;
import static com.cedarsoftware.util.convert.MapConversions.CAUSE_MESSAGE;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link ThrowableConversions}.
 *
 * The tests cover:
 *   • Normal conversion of a Throwable without a cause.
 *   • Conversion of a Throwable that has a cause.
 *   • Edge cases where the Throwable or its cause has a {@code null} message.
 *   • Invalid input types (non‑Throwable) and {@code null} input values.
 */
public class ThrowableConversionsTest {

    /**
     * Verify that a simple {@link Exception} without a cause is converted
     * into a map containing only the CLASS and MESSAGE entries.
     */
    @Test
    public void testToMap_NoCause() {
        Exception ex = new Exception("simple message");
        Map<String, Object> map = ThrowableConversions.toMap(ex, null);

        assertEquals("Map should contain exactly two entries", 2, map.size());
        assertEquals(Exception.class.getName(), map.get(CLASS));
        assertEquals("simple message", map.get(MESSAGE));
        assertFalse("Map must not contain CAUSE key", map.containsKey(CAUSE));
        assertFalse("Map must not contain CAUSE_MESSAGE key", map.containsKey(CAUSE_MESSAGE));
    }

    /**
     * Verify that a Throwable with a cause produces a map that includes
     * both the cause class name and the cause message.
     */
    @Test
    public void testToMap_WithCause() {
        IllegalArgumentException cause = new IllegalArgumentException("root cause");
        RuntimeException ex = new RuntimeException("wrapper", cause);

        Map<String, Object> map = ThrowableConversions.toMap(ex, null);

        assertEquals("Map should contain four entries", 4, map.size());
        assertEquals(RuntimeException.class.getName(), map.get(CLASS));
        assertEquals("wrapper", map.get(MESSAGE));
        assertEquals(IllegalArgumentException.class.getName(), map.get(CAUSE));
        assertEquals("root cause", map.get(CAUSE_MESSAGE));
    }

    /**
     * Ensure that a {@code null} message on the primary Throwable is
     * represented as a {@code null} value in the resulting map.
     */
    @Test
    public void testToMap_NullMessage() {
        Exception ex = new Exception((String) null);
        Map<String, Object> map = ThrowableConversions.toMap(ex, null);

        assertEquals(2, map.size());
        assertEquals(Exception.class.getName(), map.get(CLASS));
        assertNull("Message entry should be null", map.get(MESSAGE));
    }

    /**
     * Ensure that a {@code null} message on the cause is represented as
     * {@code null} in the CAUSE_MESSAGE entry.
     */
    @Test
    public void testToMap_CauseWithNullMessage() {
        Exception cause = new Exception((String) null);
        Exception ex = new Exception("outer", cause);

        Map<String, Object> map = ThrowableConversions.toMap(ex, null);

        assertEquals(4, map.size());
        assertEquals(Exception.class.getName(), map.get(CLASS));
        assertEquals("outer", map.get(MESSAGE));
        assertEquals(Exception.class.getName(), map.get(CAUSE));
        assertNull("Cause message should be null", map.get(CAUSE_MESSAGE));
    }

    /**
     * Passing an object that is not a {@link Throwable} should result in a
     * {@link ClassCastException} because the method casts the argument.
     */
    @Test(expected = ClassCastException.class)
    public void testToMap_NonThrowableInput() {
        String notThrowable = "I am not a throwable";
        // This call must throw ClassCastException
        ThrowableConversions.toMap(notThrowable, null);
    }

    /**
     * Passing {@code null} as the source object should trigger a
     * {@link NullPointerException} when the method attempts to cast.
     */
    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        // Null source leads to NPE on cast
        ThrowableConversions.toMap(null, null);
    }
}
