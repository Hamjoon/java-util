package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.Test;

import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.CAUSE;
import static com.cedarsoftware.util.convert.MapConversions.CAUSE_MESSAGE;
import static com.cedarsoftware.util.convert.MapConversions.CLASS;
import static com.cedarsoftware.util.convert.MapConversions.MESSAGE;
import static org.junit.Assert.*;

public class ThrowableConversionsTest {

    /**
     * Basic conversion of an exception that has no cause.
     */
    @Test
    public void testToMapWithoutCause() {
        Exception ex = new Exception("simple message");
        Map<String, Object> map = ThrowableConversions.toMap(ex, null);

        // Verify that the map implementation is the expected CompactLinkedMap
        assertTrue(map instanceof CompactLinkedMap);

        assertEquals(ex.getClass().getName(), map.get(CLASS));
        assertEquals(ex.getMessage(), map.get(MESSAGE));

        // No cause – the map must NOT contain those keys
        assertFalse(map.containsKey(CAUSE));
        assertFalse(map.containsKey(CAUSE_MESSAGE));
    }

    /**
     * Conversion of an exception that has a cause – both class and cause message must be present.
     */
    @Test
    public void testToMapWithCause() {
        IllegalArgumentException cause = new IllegalArgumentException("illegal argument");
        Exception ex = new Exception("wrapper exception", cause);
        Map<String, Object> map = ThrowableConversions.toMap(ex, null);

        assertEquals(ex.getClass().getName(), map.get(CLASS));
        assertEquals(ex.getMessage(), map.get(MESSAGE));

        // Cause information must be present
        assertEquals(cause.getClass().getName(), map.get(CAUSE));
        assertEquals(cause.getMessage(), map.get(CAUSE_MESSAGE));
    }

    /**
     * Edge case where the exception message and/or the cause message are {@code null}.
     * The map should contain the keys with {@code null} values.
     */
    @Test
    public void testToMapWithNullMessages() {
        RuntimeException cause = new RuntimeException((String) null);
        Exception ex = new Exception(null, cause);
        Map<String, Object> map = ThrowableConversions.toMap(ex, null);

        // Top‑level exception fields
        assertEquals(ex.getClass().getName(), map.get(CLASS));
        assertNull(map.get(MESSAGE));

        // Cause fields – class is always present, message may be null
        assertEquals(cause.getClass().getName(), map.get(CAUSE));
        assertNull(map.get(CAUSE_MESSAGE));
    }

    /**
     * Verify that when the throwable has no cause the map does not contain the cause‑related entries.
     */
    @Test
    public void testMapDoesNotContainCauseKeysWhenNoCause() {
        Exception ex = new Exception("no cause");
        Map<String, Object> map = ThrowableConversions.toMap(ex, null);

        assertFalse("Map should not contain CAUSE key when there is no cause", map.containsKey(CAUSE));
        assertFalse("Map should not contain CAUSE_MESSAGE key when there is no cause", map.containsKey(CAUSE_MESSAGE));
    }

    /**
     * Passing {@code null} as the source object should result in a {@link NullPointerException}
     * because the method immediately casts to {@link Throwable} and dereferences it.
     */
    @Test(expected = NullPointerException.class)
    public void testToMapWithNullThrowable() {
        ThrowableConversions.toMap(null, null);
    }

    /**
     * Passing an object that is not a {@link Throwable} must cause a {@link ClassCastException}
     * due to the unchecked cast inside the method.
     */
    @Test(expected = ClassCastException.class)
    public void testToMapWithNonThrowable() {
        String notAThrowable = "I am not a throwable";
        ThrowableConversions.toMap(notAThrowable, null);
    }

    /**
     * The method accepts a {@link Converter} instance but never uses it.
     * This test simply ensures that providing a non‑null converter does not affect the result.
     */
    @Test
    public void testToMapWithNonNullConverter() {
        // Minimal viable Converter – using the public constructor with default options (null is acceptable for this test)
        Converter dummyConverter = new Converter(null);
        Exception ex = new Exception("msg");
        Map<String, Object> map = ThrowableConversions.toMap(ex, dummyConverter);

        assertEquals(ex.getClass().getName(), map.get(CLASS));
        assertEquals(ex.getMessage(), map.get(MESSAGE));
        assertFalse(map.containsKey(CAUSE));
        assertFalse(map.containsKey(CAUSE_MESSAGE));
    }
}
