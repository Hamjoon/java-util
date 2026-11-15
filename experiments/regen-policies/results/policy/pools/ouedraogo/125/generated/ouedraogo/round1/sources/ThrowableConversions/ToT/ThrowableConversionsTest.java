package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.CAUSE;
import static com.cedarsoftware.util.convert.MapConversions.CAUSE_MESSAGE;
import static com.cedarsoftware.util.convert.MapConversions.CLASS;
import static com.cedarsoftware.util.convert.MapConversions.MESSAGE;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ThrowableConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical conversion of an exception with a message and a cause.</li>
 *   <li>Conversion when the exception message is {@code null}.</li>
 *   <li>Conversion when the cause is {@code null}.</li>
 *   <li>Conversion when the cause exists but its message is {@code null}.</li>
 *   <li>Conversion of a subclass of {@link Throwable} (e.g., {@link Error}).</li>
 *   <li>Behaviour when a non‑Throwable object is supplied (expect {@link ClassCastException}).</li>
 * </ul>
 *
 * The {@code Converter} argument is not used by {@code ThrowableConversions.toMap},
 * therefore we safely pass {@code null} in all tests.
 */
public class ThrowableConversionsTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The Converter is not used inside ThrowableConversions.toMap, but we create a
        // minimal instance to satisfy the method signature.  The constructor requires
        // a ConverterOptions object, which is not part of the public API supplied.
        // Since the parameter is never dereferenced, passing null is sufficient.
        dummyConverter = null;
    }

    /**
     * Verify that a regular exception with a message and a non‑null cause
     * is converted into a map containing all four expected entries.
     */
    @Test
    public void testToMap_WithMessageAndCause() {
        IllegalArgumentException cause = new IllegalArgumentException("inner cause");
        Exception ex = new Exception("top level", cause);

        Map<String, Object> result = ThrowableConversions.toMap(ex, dummyConverter);

        assertEquals("Map should contain exactly four entries", 4, result.size());
        assertEquals("Class name must be stored", Exception.class.getName(), result.get(CLASS));
        assertEquals("Exception message must be stored", "top level", result.get(MESSAGE));
        assertEquals("Cause class name must be stored", IllegalArgumentException.class.getName(), result.get(CAUSE));
        assertEquals("Cause message must be stored", "inner cause", result.get(CAUSE_MESSAGE));
    }

    /**
     * Verify conversion when the exception message is {@code null}.
     * The map must contain the {@code null} value for the MESSAGE key.
     */
    @Test
    public void testToMap_NullMessage() {
        Exception ex = new Exception((String) null);

        Map<String, Object> result = ThrowableConversions.toMap(ex, dummyConverter);

        assertEquals("Map should contain exactly two entries (no cause)", 2, result.size());
        assertEquals("Class name must be stored", Exception.class.getName(), result.get(CLASS));
        assertNull("Message should be null", result.get(MESSAGE));
        assertFalse("CAUSE key must be absent", result.containsKey(CAUSE));
        assertFalse("CAUSE_MESSAGE key must be absent", result.containsKey(CAUSE_MESSAGE));
    }

    /**
     * Verify conversion when the exception has no cause.
     * Only CLASS and MESSAGE keys should be present.
     */
    @Test
    public void testToMap_NoCause() {
        Exception ex = new Exception("no cause here");

        Map<String, Object> result = ThrowableConversions.toMap(ex, dummyConverter);

        assertEquals("Map should contain exactly two entries", 2, result.size());
        assertEquals("Class name must be stored", Exception.class.getName(), result.get(CLASS));
        assertEquals("Exception message must be stored", "no cause here", result.get(MESSAGE));
        assertFalse("CAUSE key must be absent when cause is null", result.containsKey(CAUSE));
        assertFalse("CAUSE_MESSAGE key must be absent when cause is null", result.containsKey(CAUSE_MESSAGE));
    }

    /**
     * Verify conversion when the cause exists but its message is {@code null}.
     */
    @Test
    public void testToMap_CauseWithNullMessage() {
        IllegalStateException cause = new IllegalStateException((String) null);
        RuntimeException ex = new RuntimeException("wrapper", cause);

        Map<String, Object> result = ThrowableConversions.toMap(ex, dummyConverter);

        assertEquals("Map should contain exactly four entries", 4, result.size());
        assertEquals("Class name must be stored", RuntimeException.class.getName(), result.get(CLASS));
        assertEquals("Exception message must be stored", "wrapper", result.get(MESSAGE));
        assertEquals("Cause class name must be stored", IllegalStateException.class.getName(), result.get(CAUSE));
        assertNull("Cause message should be null", result.get(CAUSE_MESSAGE));
    }

    /**
     * Verify conversion of a subclass of {@link Throwable} (here, {@link Error}).
     */
    @Test
    public void testToMap_ErrorSubclass() {
        Error error = new AssertionError("assertion failed");

        Map<String, Object> result = ThrowableConversions.toMap(error, dummyConverter);

        assertEquals("Map should contain exactly two entries", 2, result.size());
        assertEquals("Class name must be stored", AssertionError.class.getName(), result.get(CLASS));
        assertEquals("Error message must be stored", "assertion failed", result.get(MESSAGE));
        assertFalse("CAUSE key must be absent for an Error without cause", result.containsKey(CAUSE));
    }

    /**
     * Verify that passing a non‑Throwable object results in a {@link ClassCastException}.
     */
    @Test
    public void testToMap_NonThrowableThrowsClassCastException() {
        thrown.expect(ClassCastException.class);
        thrown.expectMessage("java.lang.String cannot be cast to java.lang.Throwable");

        // The method casts the input to Throwable; a String should trigger the exception.
        ThrowableConversions.toMap("I am not a throwable", dummyConverter);
    }
}
