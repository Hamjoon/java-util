package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactLinkedMap;


import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.CAUSE;
import static com.cedarsoftware.util.convert.MapConversions.CAUSE_MESSAGE;
import static com.cedarsoftware.util.convert.MapConversions.CLASS;
import static com.cedarsoftware.util.convert.MapConversions.MESSAGE;

/**
 * Comprehensive JUnit‑4 test suite for {@link ThrowableConversions}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Typical conversion of a {@link Throwable} without a cause.</li>
 *   <li>Conversion when a cause is present.</li>
 *   <li>Handling of {@code null} messages on both the throwable and its cause.</li>
 *   <li>Conversion of a subclass of {@link Throwable} (e.g., {@link IllegalArgumentException}).</li>
 *   <li>Verification that the returned map is a {@link CompactLinkedMap} and that no extra keys are added.</li>
 * </ul>
 *
 * <p>Only the public API shown in the prompt is used – no mocking, no subclassing of library
 * types, and no reliance on any other version of the library.
 */
public class ThrowableConversionsTest {

    /** A dummy {@link Converter} instance – the implementation of {@code toMap} does not use it. */
    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The Converter constructor requires a ConverterOptions instance.
        // The test does not depend on its behaviour, so we create it with a null argument
        // (the constructor accepts null according to the library’s source code).
        dummyConverter = new Converter(null);
    }

    /**
     * Helper that extracts a value from the map and casts it safely.
     */
    @SuppressWarnings("unchecked")
    private static <T> T get(Map<String, Object> map, String key) {
        return (T) map.get(key);
    }

    /** -----------------------------------------------------------------------
     *  1️⃣  Typical case – a Throwable with no cause and a non‑null message.
     *  ----------------------------------------------------------------------- */
    @Test
    public void testToMap_NoCause_NonNullMessage() {
        String msg = "Something went wrong";
        Exception ex = new Exception(msg);

        Map<String, Object> result = ThrowableConversions.toMap(ex, dummyConverter);

        // Basic contract checks
        assertNotNull("Result map must not be null", result);
        assertTrue("Result map should be a CompactLinkedMap",
                result instanceof CompactLinkedMap);

        // Expected entries
        assertEquals("Class name must be stored under key CLASS",
                ex.getClass().getName(), get(result, CLASS));
        assertEquals("Message must be stored under key MESSAGE",
                msg, get(result, MESSAGE));

        // No cause → keys CAUSE and CAUSE_MESSAGE must be absent
        assertFalse("Map must not contain key CAUSE when there is no cause",
                result.containsKey(CAUSE));
        assertFalse("Map must not contain key CAUSE_MESSAGE when there is no cause",
                result.containsKey(CAUSE_MESSAGE));
    }

    /** -----------------------------------------------------------------------
     *  2️⃣  Typical case – a Throwable with a cause (both messages non‑null).
     *  ----------------------------------------------------------------------- */
    @Test
    public void testToMap_WithCause_NonNullMessages() {
        String causeMsg = "Root cause";
        IllegalArgumentException cause = new IllegalArgumentException(causeMsg);
        RuntimeException ex = new RuntimeException("Higher level failure", cause);

        Map<String, Object> result = ThrowableConversions.toMap(ex, dummyConverter);

        // Verify top‑level fields
        assertEquals(ex.getClass().getName(), get(result, CLASS));
        assertEquals("Higher level failure", get(result, MESSAGE));

        // Verify cause fields
        assertEquals(cause.getClass().getName(), get(result, CAUSE));
        assertEquals(causeMsg, get(result, CAUSE_MESSAGE));
    }

    /** -----------------------------------------------------------------------
     *  3️⃣  Edge case – Throwable with a null message and a cause that also has a null message.
     *  ----------------------------------------------------------------------- */
    @Test
    public void testToMap_NullMessages() {
        // Cause with null message
        NullPointerException cause = new NullPointerException();
        // Throwable with null message but with a cause
        Exception ex = new Exception(null, cause);

        Map<String, Object> result = ThrowableConversions.toMap(ex, dummyConverter);

        // Top‑level message is null
        assertTrue("MESSAGE key must be present even if value is null",
                result.containsKey(MESSAGE));
        assertNull("Top‑level message should be null", get(result, MESSAGE));

        // Cause fields must be present (class name is never null)
        assertEquals(cause.getClass().getName(), get(result, CAUSE));
        // Cause message is null
        assertTrue("CAUSE_MESSAGE key must be present even if value is null",
                result.containsKey(CAUSE_MESSAGE));
        assertNull("Cause message should be null", get(result, CAUSE_MESSAGE));
    }

    /** -----------------------------------------------------------------------
     *  4️⃣  Edge case – Throwable whose cause is itself (circular reference).
     *       The implementation only reads the cause’s class/name once, so it must not
     *       throw a StackOverflowError.
     *  ----------------------------------------------------------------------- */
    @Test
    public void testToMap_CircularCause() {
        // Create a Throwable that references itself as its cause.
        // This is possible via reflection but for the purpose of this test we can
        // use a subclass that sets the cause in the constructor.
        class SelfReferencingException extends Exception {
            SelfReferencingException() {
                super((Throwable) null);
                initCause(this); // set cause to itself
            }
        }

        SelfReferencingException ex = new SelfReferencingException();

        // The method should still return a map with the class name of the cause (which
        // is the same as the throwable itself) and a null message for the cause.
        Map<String, Object> result = ThrowableConversions.toMap(ex, dummyConverter);

        assertEquals(SelfReferencingException.class.getName(), get(result, CLASS));
        assertNull("Top‑level message should be null", get(result, MESSAGE));

        // Cause entries must be present and refer to the same class
        assertEquals(SelfReferencingException.class.getName(), get(result, CAUSE));
        assertNull("Cause message should be null", get(result, CAUSE_MESSAGE));
    }

    /** -----------------------------------------------------------------------
     *  5️⃣  Verify that the map does not contain any unexpected keys.
     *  ----------------------------------------------------------------------- */
    @Test
    public void testToMap_NoExtraKeys() {
        Exception ex = new Exception("msg");
        Map<String, Object> result = ThrowableConversions.toMap(ex, dummyConverter);

        // Expected keys set
        assertEquals("Map should contain exactly 2 keys when no cause is present",
                2, result.size());
        assertTrue(result.containsKey(CLASS));
        assertTrue(result.containsKey(MESSAGE));
    }

    /** -----------------------------------------------------------------------
     *  6️⃣  Verify that passing {@code null} as the source object throws a
     *       {@link ClassCastException} (the method casts without a null‑check).
     *  ----------------------------------------------------------------------- */
    @Test(expected = ClassCastException.class)
    public void testToMap_NullSourceThrows() {
        // The implementation casts the input to Throwable; a null value causes
        // a ClassCastException when the cast is performed.
        ThrowableConversions.toMap(null, dummyConverter);
    }

    /** -----------------------------------------------------------------------
     *  7️⃣  Verify that passing an object that is not a {@link Throwable}
     *       also throws a {@link ClassCastException}.
     *  ----------------------------------------------------------------------- */
    @Test(expected = ClassCastException.class)
    public void testToMap_NonThrowableSourceThrows() {
        String notAThrowable = "I am not a throwable";
        ThrowableConversions.toMap(notAThrowable, dummyConverter);
    }
}
