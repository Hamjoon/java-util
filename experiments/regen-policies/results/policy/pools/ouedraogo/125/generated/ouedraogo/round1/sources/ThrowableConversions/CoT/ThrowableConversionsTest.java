package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactMap;


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
 * Unit tests for {@link ThrowableConversions}.
 *
 * The test class is placed in the same package as {@code ThrowableConversions}
 * so that the package‑private {@code toMap} method can be accessed directly.
 */
public class ThrowableConversionsTest {

    /* --------------------------------------------------------------------- */
    /*  Fixtures                                                            */
    /* --------------------------------------------------------------------- */

    // The {@code Converter} argument is not used by the implementation,
    // therefore we can safely pass {@code null}.  Keeping a reference makes
    // the intent of the API clearer.
    private Converter dummyConverter;

    @Before
    public void setUp() {
        dummyConverter = null; // no real Converter needed for current logic
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* --------------------------------------------------------------------- */
    /*  Basic functionality tests                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void toMap_withoutCause_returnsClassAndMessageOnly() {
        String msg = "simple error";
        Exception ex = new Exception(msg);

        Map<String, Object> result = ThrowableConversions.toMap(ex, dummyConverter);

        // Expected keys
        assertEquals(2, result.size());
        assertEquals(Exception.class.getName(), result.get(CLASS));
        assertEquals(msg, result.get(MESSAGE));

        // Cause‑related keys must be absent
        assertFalse("Should not contain CAUSE key", result.containsKey(CAUSE));
        assertFalse("Should not contain CAUSE_MESSAGE key", result.containsKey(CAUSE_MESSAGE));
    }

    @Test
    public void toMap_withCause_includesCauseInformation() {
        String causeMsg = "root cause";
        IllegalArgumentException cause = new IllegalArgumentException(causeMsg);
        RuntimeException ex = new RuntimeException("wrapper", cause);

        Map<String, Object> result = ThrowableConversions.toMap(ex, dummyConverter);

        // Expected keys
        assertEquals(4, result.size());
        assertEquals(RuntimeException.class.getName(), result.get(CLASS));
        assertEquals("wrapper", result.get(MESSAGE));
        assertEquals(IllegalArgumentException.class.getName(), result.get(CAUSE));
        assertEquals(causeMsg, result.get(CAUSE_MESSAGE));
    }

    @Test
    public void toMap_withNullMessage_handlesNullGracefully() {
        Exception ex = new Exception((String) null);

        Map<String, Object> result = ThrowableConversions.toMap(ex, dummyConverter);

        assertEquals(2, result.size());
        assertEquals(Exception.class.getName(), result.get(CLASS));
        assertNull("Message should be null", result.get(MESSAGE));
    }

    @Test
    public void toMap_withCauseHavingNullMessage_includesNullCauseMessage() {
        NullPointerException cause = new NullPointerException(); // message == null
        Exception ex = new Exception("outer", cause);

        Map<String, Object> result = ThrowableConversions.toMap(ex, dummyConverter);

        assertEquals(4, result.size());
        assertEquals(Exception.class.getName(), result.get(CLASS));
        assertEquals("outer", result.get(MESSAGE));
        assertEquals(NullPointerException.class.getName(), result.get(CAUSE));
        assertNull("Cause message should be null", result.get(CAUSE_MESSAGE));
    }

    /* --------------------------------------------------------------------- */
    /*  Edge‑case / exception handling tests                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void toMap_nullThrowable_throwsNullPointerException() {
        thrown.expect(NullPointerException.class);
        ThrowableConversions.toMap(null, dummyConverter);
    }

    @Test
    public void toMap_nonThrowable_throwsClassCastException() {
        thrown.expect(ClassCastException.class);
        ThrowableConversions.toMap("I am not a Throwable", dummyConverter);
    }

    /* --------------------------------------------------------------------- */
    /*  Additional sanity check – map implementation type                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void toMap_returnsCompactMapInstance() {
        Exception ex = new Exception("test");
        Map<String, Object> result = ThrowableConversions.toMap(ex, dummyConverter);

        // The builder of CompactMap returns a CompactMap instance.
        // Using instanceof is safe because CompactMap implements Map.
        assertTrue("Result should be a CompactMap", result instanceof CompactMap);
    }
}
