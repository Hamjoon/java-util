package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactMap;
import org.junit.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import static org.junit.Assert.*;
import static com.cedarsoftware.util.convert.MapConversions.*;

public class ThrowableConversionsTest {

    /**
     * Verify that a simple {@link Throwable} without a cause is converted correctly.
     * The resulting map must contain only CLASS and MESSAGE entries, must be an
     * instance of {@link CompactMap}, and must preserve insertion order.
     */
    @Test
    public void testToMapWithoutCause() {
        IllegalArgumentException ex = new IllegalArgumentException("bad argument");
        // the method does not use the converter, passing null is safe
        Map<String, Object> result = ThrowableConversions.toMap(ex, null);

        // basic content checks
        assertTrue("Result should be a CompactMap", result instanceof CompactMap);
        assertEquals("Map should contain exactly 2 entries", 2, result.size());
        assertEquals("CLASS entry incorrect",
                ex.getClass().getName(), result.get(CLASS));
        assertEquals("MESSAGE entry incorrect",
                "bad argument", result.get(MESSAGE));
        assertFalse("Map must not contain CAUSE entry", result.containsKey(CAUSE));
        assertFalse("Map must not contain CAUSE_MESSAGE entry", result.containsKey(CAUSE_MESSAGE));

        // verify insertion order: CLASS first, then MESSAGE
        Iterator<String> keyIter = result.keySet().iterator();
        List<String> observedOrder = new ArrayList<>();
        while (keyIter.hasNext()) {
            observedOrder.add(keyIter.next());
        }
        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(CLASS);
        expectedOrder.add(MESSAGE);
        assertEquals("Insertion order of keys is incorrect", expectedOrder, observedOrder);
    }

    /**
     * Verify conversion when the throwable has a cause. All four entries
     * (CLASS, MESSAGE, CAUSE, CAUSE_MESSAGE) must be present and in the proper order.
     */
    @Test
    public void testToMapWithCause() {
        NullPointerException cause = new NullPointerException("null pointer");
        RuntimeException ex = new RuntimeException("runtime failure", cause);

        Map<String, Object> result = ThrowableConversions.toMap(ex, null);

        assertTrue("Result should be a CompactMap", result instanceof CompactMap);
        assertEquals("Map should contain exactly 4 entries", 4, result.size());

        assertEquals("CLASS entry incorrect",
                ex.getClass().getName(), result.get(CLASS));
        assertEquals("MESSAGE entry incorrect",
                "runtime failure", result.get(MESSAGE));
        assertEquals("CAUSE entry incorrect",
                cause.getClass().getName(), result.get(CAUSE));
        assertEquals("CAUSE_MESSAGE entry incorrect",
                "null pointer", result.get(CAUSE_MESSAGE));

        // verify insertion order: CLASS, MESSAGE, CAUSE, CAUSE_MESSAGE
        Iterator<String> keyIter = result.keySet().iterator();
        List<String> observedOrder = new ArrayList<>();
        while (keyIter.hasNext()) {
            observedOrder.add(keyIter.next());
        }
        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(CLASS);
        expectedOrder.add(MESSAGE);
        expectedOrder.add(CAUSE);
        expectedOrder.add(CAUSE_MESSAGE);
        assertEquals("Insertion order of keys is incorrect", expectedOrder, observedOrder);
    }

    /**
     * When the supplied object is not a {@link Throwable}, the method should
     * throw a {@link ClassCastException} because of the unchecked cast.
     */
    @Test(expected = ClassCastException.class)
    public void testToMapWithNonThrowable() {
        ThrowableConversions.toMap("I am not a throwable", null);
    }

    /**
     * Passing {@code null} as the source object causes a {@link NullPointerException}
     * when the implementation attempts to call {@code getClass()} on the casted value.
     */
    @Test(expected = NullPointerException.class)
    public void testToMapWithNullThrowable() {
        ThrowableConversions.toMap(null, null);
    }

    /**
     * Verify that a throwable with a {@code null} message is handled correctly.
     * The MESSAGE entry should be present with a {@code null} value.
     */
    @Test
    public void testToMapWithNullMessage() {
        Exception ex = new Exception(); // message is null
        Map<String, Object> result = ThrowableConversions.toMap(ex, null);

        assertEquals("Map should contain exactly 2 entries", 2, result.size());
        assertTrue("Result should contain MESSAGE key", result.containsKey(MESSAGE));
        assertNull("MESSAGE value should be null when throwable has no message", result.get(MESSAGE));
    }

    /**
     * Ensure that the returned map is mutable – a new entry can be added
     * without affecting the original conversion logic.
     */
    @Test
    public void testResultMapIsMutable() {
        IllegalStateException ex = new IllegalStateException("illegal state");
        Map<String, Object> result = ThrowableConversions.toMap(ex, null);

        int originalSize = result.size();
        result.put("extra", 12345);
        assertEquals("Map size should increase after adding a new entry", originalSize + 1, result.size());
        assertEquals("Extra entry value mismatch", 12345, result.get("extra"));
    }
}
