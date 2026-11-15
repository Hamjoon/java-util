package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactLinkedMap;


import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Map;

/**
 * Unit tests for {@link ThrowableConversions}.
 * <p>
 * The tests focus on the {@code toMap} method, verifying that it correctly
 * extracts the class name, message, and (optionally) cause information from a
 * {@link Throwable}. The {@link Converter} argument is not used by the method
 * itself, so a minimal instance (constructed with a {@code null} options object)
 * is sufficient.
 * </p>
 */
public class ThrowableConversionsTest {

    /**
     * Helper method to create a {@link Converter} instance required by the
     * {@code toMap} method. The library's {@code Converter} only needs a
     * {@code ConverterOptions} object; passing {@code null} is acceptable for
     * the purposes of these tests because the options are never consulted.
     */
    private static Converter createConverter() {
        return new Converter(null);
    }

    @Test
    public void testToMapWithoutCause() {
        String msg = "simple error";
        IllegalArgumentException ex = new IllegalArgumentException(msg);

        Map<String, Object> result = ThrowableConversions.toMap(ex, createConverter());

        // Verify map type
        assertTrue("Result should be a CompactLinkedMap", result instanceof CompactLinkedMap);

        // Required entries
        assertEquals("Class name must match", IllegalArgumentException.class.getName(), result.get(CLASS));
        assertEquals("Message must match", msg, result.get(MESSAGE));

        // Cause related entries must be absent
        assertFalse("Map should not contain CAUSE key", result.containsKey(CAUSE));
        assertFalse("Map should not contain CAUSE_MESSAGE key", result.containsKey(CAUSE_MESSAGE));
    }

    @Test
    public void testToMapWithCause() {
        String causeMsg = "root cause";
        NullPointerException cause = new NullPointerException(causeMsg);
        RuntimeException wrapper = new RuntimeException("wrapper", cause);

        Map<String, Object> result = ThrowableConversions.toMap(wrapper, createConverter());

        // Verify map type
        assertTrue("Result should be a CompactLinkedMap", result instanceof CompactLinkedMap);

        // Top‑level exception data
        assertEquals(RuntimeException.class.getName(), result.get(CLASS));
        assertEquals("wrapper", result.get(MESSAGE));

        // Cause data
        assertEquals(NullPointerException.class.getName(), result.get(CAUSE));
        assertEquals(causeMsg, result.get(CAUSE_MESSAGE));
    }

    @Test
    public void testToMapWithNullMessageAndNullCauseMessage() {
        Exception cause = new Exception((String) null);
        Exception top = new Exception(null, cause);

        Map<String, Object> result = ThrowableConversions.toMap(top, createConverter());

        // Class and message (null)
        assertEquals(Exception.class.getName(), result.get(CLASS));
        assertNull("Message should be null", result.get(MESSAGE));

        // Cause class present, cause message null
        assertEquals(Exception.class.getName(), result.get(CAUSE));
        assertNull("Cause message should be null", result.get(CAUSE_MESSAGE));
    }

    @Test
    public void testMapIsMutableAndPreservesInsertionOrder() {
        // Use a throwable with a cause to ensure all keys are inserted.
        IllegalStateException cause = new IllegalStateException("state");
        IllegalStateException ex = new IllegalStateException("top", cause);

        Map<String, Object> result = ThrowableConversions.toMap(ex, createConverter());

        // Mutate the map and verify that changes are reflected.
        result.put("customKey", "customValue");
        assertEquals("customValue", result.get("customKey"));

        // Verify insertion order by checking the iterator sequence.
        // Expected order: CLASS, MESSAGE, CAUSE, CAUSE_MESSAGE, customKey
        String[] expectedKeys = { CLASS, MESSAGE, CAUSE, CAUSE_MESSAGE, "customKey" };
        int index = 0;
        for (String key : result.keySet()) {
            assertEquals("Key order should be preserved", expectedKeys[index], key);
            index++;
        }
        assertEquals("All expected keys should have been iterated", expectedKeys.length, index);
    }
}
