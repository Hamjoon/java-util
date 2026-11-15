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
 * These tests exercise all logical branches in {@code toMap}:
 * <ul>
 *   <li>Throwable without a cause</li>
 *   <li>Throwable with a cause (both cause message present and null)</li>
 *   <li>Throwable with a null message</li>
 *   <li>Invalid input (non‑Throwable) – expect {@link ClassCastException}</li>
 *   <li>Insertion order of the resulting {@link CompactMap}</li>
 * </ul>
 */
public class ThrowableConversionsTest {

    /**
     * Helper that creates a {@link Converter} instance.
     * The {@code toMap} implementation does not use the converter,
     * but a non‑null instance keeps the API contract clear.
     */
    private static Converter createConverter() {
        // The library provides a no‑arg constructor for ConverterOptions in the real code base.
        // If that constructor is unavailable, passing {@code null} is still safe because the
        // method under test never dereferences the Converter.
        try {
            Class<?> optionsClass = Class.forName("com.cedarsoftware.util.convert.ConverterOptions");
            Object options = optionsClass.getDeclaredConstructor().newInstance();
            return new Converter((ConverterOptions) options);
        } catch (Exception e) {
            // Fallback – use null; the method does not touch the converter.
            return null;
        }
    }

    @Test
    public void toMap_WithSimpleException_NoCause() {
        Exception ex = new IllegalArgumentException("invalid argument");
        Map<String, Object> map = ThrowableConversions.toMap(ex, createConverter());

        assertNotNull("Resulting map must not be null", map);
        assertEquals("Class name must match", IllegalArgumentException.class.getName(), map.get(CLASS));
        assertEquals("Message must be stored", "invalid argument", map.get(MESSAGE));

        // No cause – the map must not contain cause keys
        assertFalse("Map must not contain CAUSE key when no cause", map.containsKey(CAUSE));
        assertFalse("Map must not contain CAUSE_MESSAGE key when no cause", map.containsKey(CAUSE_MESSAGE));
    }

    @Test
    public void toMap_WithExceptionHavingCause_WithMessage() {
        NullPointerException cause = new NullPointerException("null pointer");
        RuntimeException ex = new RuntimeException("wrapper", cause);
        Map<String, Object> map = ThrowableConversions.toMap(ex, createConverter());

        assertEquals(RuntimeException.class.getName(), map.get(CLASS));
        assertEquals("wrapper", map.get(MESSAGE));
        assertEquals(NullPointerException.class.getName(), map.get(CAUSE));
        assertEquals("null pointer", map.get(CAUSE_MESSAGE));
    }

    @Test
    public void toMap_WithExceptionHavingCause_NullCauseMessage() {
        Exception cause = new Exception((String) null);
        Exception ex = new Exception("top level", cause);
        Map<String, Object> map = ThrowableConversions.toMap(ex, createConverter());

        assertEquals(Exception.class.getName(), map.get(CLASS));
        assertEquals("top level", map.get(MESSAGE));
        assertEquals(Exception.class.getName(), map.get(CAUSE));
        assertNull("Cause message should be null when cause.getMessage() is null", map.get(CAUSE_MESSAGE));
    }

    @Test
    public void toMap_WithExceptionNullMessage_NoCause() {
        Exception ex = new Exception((String) null);
        Map<String, Object> map = ThrowableConversions.toMap(ex, createConverter());

        assertEquals(Exception.class.getName(), map.get(CLASS));
        assertNull("Message entry should be null when exception message is null", map.get(MESSAGE));
        assertFalse(map.containsKey(CAUSE));
        assertFalse(map.containsKey(CAUSE_MESSAGE));
    }

    @Test
    public void toMap_InsertionOrderIsPreserved() {
        Exception ex = new IllegalStateException("state error");
        Map<String, Object> map = ThrowableConversions.toMap(ex, createConverter());

        // Expected order: CLASS, MESSAGE (and optionally CAUSE, CAUSE_MESSAGE)
        Iterator<String> it = map.keySet().iterator();
        assertTrue("Map should contain at least one key", it.hasNext());
        assertEquals("First key must be CLASS", CLASS, it.next());
        assertTrue("Map should contain a second key", it.hasNext());
        assertEquals("Second key must be MESSAGE", MESSAGE, it.next());

        // No cause in this case – iterator should be exhausted
        assertFalse("No further keys expected when there is no cause", it.hasNext());
    }

    @Test
    public void toMap_WithExceptionHavingCause_InsertionOrderIncludesCause() {
        IllegalArgumentException cause = new IllegalArgumentException("bad arg");
        Exception ex = new Exception("top", cause);
        Map<String, Object> map = ThrowableConversions.toMap(ex, createConverter());

        Iterator<String> it = map.keySet().iterator();
        assertEquals("First key must be CLASS", CLASS, it.next());
        assertEquals("Second key must be MESSAGE", MESSAGE, it.next());
        assertEquals("Third key must be CAUSE", CAUSE, it.next());
        assertEquals("Fourth key must be CAUSE_MESSAGE", CAUSE_MESSAGE, it.next());
        assertFalse("No more keys expected", it.hasNext());
    }

    @Test(expected = ClassCastException.class)
    public void toMap_WithNonThrowable_ThrowsClassCastException() {
        // Passing an object that is not a Throwable should result in a ClassCastException
        String notAThrowable = "I am not an exception";
        ThrowableConversions.toMap(notAThrowable, createConverter());
    }
}
