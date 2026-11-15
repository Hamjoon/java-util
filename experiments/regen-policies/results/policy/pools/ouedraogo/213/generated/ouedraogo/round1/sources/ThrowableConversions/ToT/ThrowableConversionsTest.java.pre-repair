package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.Map;

/**
 * Unit tests for {@link ThrowableConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Conversion of a simple {@link Exception} with no cause.</li>
 *   <li>Conversion of an {@link Exception} that has a cause.</li>
 *   <li>Conversion of a {@link Throwable} with a {@code null} message.</li>
 *   <li>Verification that the returned map preserves insertion order (LinkedHashMap).</li>
 *   <li>Ensuring that the {@code Converter} argument is not used by the current implementation
 *       (but is still required by the method signature).</li>
 * </ul>
 *
 * Only public APIs listed in the problem statement are used; no mocking frameworks are employed.
 */
public class ThrowableConversionsTest {

    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The Converter instance is required by the toMap method signature,
        // but ThrowableConversions does not use it.  We can safely construct a
        // default Converter with default options.
        dummyConverter = new Converter(new ConverterOptions());
    }

    /**
     * Helper method to assert common entries present in the map for any Throwable.
     */
    private void assertCommonEntries(Map<String, Object> map, Throwable throwable) {
        assertEquals("Class name entry mismatch",
                throwable.getClass().getName(),
                map.get(MapConversions.CLASS));

        assertEquals("Message entry mismatch",
                throwable.getMessage(),
                map.get(MapConversions.MESSAGE));
    }

    @Test
    public void testToMap_WithSimpleException_NoCause() {
        // Arrange
        Exception ex = new IllegalArgumentException("Invalid argument");

        // Act
        Map<String, Object> result = ThrowableConversions.toMap(ex, dummyConverter);

        // Assert
        assertNotNull("Result map should not be null", result);
        assertEquals("Map should contain exactly 2 entries (class & message)",
                2, result.size());

        assertCommonEntries(result, ex);
        assertFalse("Map should not contain CAUSE key when there is no cause",
                result.containsKey(MapConversions.CAUSE));
        assertFalse("Map should not contain CAUSE_MESSAGE key when there is no cause",
                result.containsKey(MapConversions.CAUSE_MESSAGE));
    }

    @Test
    public void testToMap_WithExceptionHavingCause() {
        // Arrange
        NullPointerException cause = new NullPointerException("null pointer");
        RuntimeException ex = new RuntimeException("wrapper exception", cause);

        // Act
        Map<String, Object> result = ThrowableConversions.toMap(ex, dummyConverter);

        // Assert
        assertNotNull(result);
        // Expected entries: CLASS, MESSAGE, CAUSE, CAUSE_MESSAGE
        assertEquals(4, result.size());

        // Common entries
        assertCommonEntries(result, ex);

        // Cause specific entries
        assertEquals("Cause class entry mismatch",
                cause.getClass().getName(),
                result.get(MapConversions.CAUSE));

        assertEquals("Cause message entry mismatch",
                cause.getMessage(),
                result.get(MapConversions.CAUSE_MESSAGE));
    }

    @Test
    public void testToMap_WithNullMessageAndNullCause() {
        // Arrange
        Exception ex = new Exception((String) null); // message == null, cause == null

        // Act
        Map<String, Object> result = ThrowableConversions.toMap(ex, dummyConverter);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());

        // The CLASS entry must be present
        assertEquals(Exception.class.getName(), result.get(MapConversions.CLASS));

        // The MESSAGE entry must be present and be null
        assertTrue("Message entry should be present and null",
                result.containsKey(MapConversions.MESSAGE) && result.get(MapConversions.MESSAGE) == null);

        // No cause related entries
        assertFalse(result.containsKey(MapConversions.CAUSE));
        assertFalse(result.containsKey(MapConversions.CAUSE_MESSAGE));
    }

    @Test
    public void testToMap_PreservesInsertionOrder() {
        // Arrange
        IllegalStateException ex = new IllegalStateException("state error");

        // Act
        Map<String, Object> result = ThrowableConversions.toMap(ex, dummyConverter);

        // Assert
        assertNotNull(result);
        // The LinkedHashMap should preserve the order: CLASS, MESSAGE
        String[] expectedOrder = {MapConversions.CLASS, MapConversions.MESSAGE};
        int index = 0;
        for (String key : result.keySet()) {
            assertEquals("Insertion order mismatch at position " + index,
                    expectedOrder[index], key);
            index++;
        }
        assertEquals("Unexpected number of entries", expectedOrder.length, result.size());
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WithNonThrowableArgument_ThrowsClassCastException() {
        // Arrange
        String notAThrowable = "I am not a throwable";

        // Act
        // This should throw ClassCastException because the method casts the Object to Throwable.
        ThrowableConversions.toMap(notAThrowable, dummyConverter);
    }

    @Test
    public void testToMap_WithCustomThrowableSubclass() {
        // Arrange
        class MyError extends Error {
            MyError(String msg) { super(msg); }
        }
        MyError error = new MyError("custom error");

        // Act
        Map<String, Object> result = ThrowableConversions.toMap(error, dummyConverter);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(MyError.class.getName(), result.get(MapConversions.CLASS));
        assertEquals("custom error", result.get(MapConversions.MESSAGE));
    }
}
