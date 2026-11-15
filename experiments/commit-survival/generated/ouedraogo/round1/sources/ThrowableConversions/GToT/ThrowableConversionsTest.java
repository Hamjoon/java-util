package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.Test;

import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.CAUSE;
import static com.cedarsoftware.util.convert.MapConversions.CAUSE_MESSAGE;
import static com.cedarsoftware.util.convert.MapConversions.CLASS;
import static com.cedarsoftware.util.convert.MapConversions.MESSAGE;
import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link ThrowableConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>normal conversion of a {@link Throwable} without a cause</li>
 *   <li>conversion when a cause is present (including null cause messages)</li>
 *   <li>conversion of a {@link Throwable} whose own message is {@code null}</li>
 *   <li>behaviour when the supplied {@code from} argument is not a {@link Throwable}</li>
 *   <li>behaviour when the supplied {@code from} argument is {@code null}</li>
 * </ul>
 *
 * The {@code Converter} argument is not used by the implementation, therefore {@code null}
 * is passed in all tests.
 */
public class ThrowableConversionsTest {

    /**
     * Helper that invokes the package‑private {@code toMap} method via reflection.
     * This keeps the test code readable while still exercising the real method.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> invokeToMap(Object from) {
        try {
            java.lang.reflect.Method m = ThrowableConversions.class.getDeclaredMethod("toMap", Object.class, Converter.class);
            m.setAccessible(true);
            return (Map<String, Object>) m.invoke(null, from, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Basic conversion – no cause, normal message. */
    @Test
    public void testToMap_NoCause() {
        String msg = "simple error";
        IllegalArgumentException ex = new IllegalArgumentException(msg);

        Map<String, Object> map = invokeToMap(ex);

        assertTrue("Result should be a CompactLinkedMap", map instanceof CompactLinkedMap);
        assertEquals("Class name should be stored", IllegalArgumentException.class.getName(), map.get(CLASS));
        assertEquals("Message should be stored", msg, map.get(MESSAGE));
        assertFalse("CAUSE key must be absent when there is no cause", map.containsKey(CAUSE));
        assertFalse("CAUSE_MESSAGE key must be absent when there is no cause", map.containsKey(CAUSE_MESSAGE));
    }

    /** Conversion when a cause is present and both messages are non‑null. */
    @Test
    public void testToMap_WithCause_NonNullMessages() {
        String rootMsg = "root problem";
        String wrapperMsg = "wrapper problem";

        NullPointerException root = new NullPointerException(rootMsg);
        RuntimeException wrapper = new RuntimeException(wrapperMsg, root);

        Map<String, Object> map = invokeToMap(wrapper);

        assertEquals(RuntimeException.class.getName(), map.get(CLASS));
        assertEquals(wrapperMsg, map.get(MESSAGE));
        assertEquals(NullPointerException.class.getName(), map.get(CAUSE));
        assertEquals(rootMsg, map.get(CAUSE_MESSAGE));
    }

    /** Conversion when the cause exists but its message is {@code null}. */
    @Test
    public void testToMap_WithCause_NullCauseMessage() {
        IllegalStateException cause = new IllegalStateException(); // message == null
        Exception ex = new Exception("outer", cause);

        Map<String, Object> map = invokeToMap(ex);

        assertEquals(Exception.class.getName(), map.get(CLASS));
        assertEquals("outer", map.get(MESSAGE));
        assertEquals(IllegalStateException.class.getName(), map.get(CAUSE));
        assertNull("CAUSE_MESSAGE should be null when cause message is null", map.get(CAUSE_MESSAGE));
    }

    /** Conversion when the throwable itself has a {@code null} message. */
    @Test
    public void testToMap_NullThrowableMessage() {
        Exception ex = new Exception(); // message == null

        Map<String, Object> map = invokeToMap(ex);

        assertEquals(Exception.class.getName(), map.get(CLASS));
        assertNull("MESSAGE should be null when throwable message is null", map.get(MESSAGE));
        assertFalse("CAUSE key must be absent when there is no cause", map.containsKey(CAUSE));
    }

    /** Passing an object that is not a {@link Throwable} must result in {@link ClassCastException}. */
    @Test(expected = ClassCastException.class)
    public void testToMap_NonThrowableInput() {
        String notAThrowable = "I am just a string";
        invokeToMap(notAThrowable);
    }

    /** Passing {@code null} as the source object must result in {@link NullPointerException}. */
    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        invokeToMap(null);
    }
}
