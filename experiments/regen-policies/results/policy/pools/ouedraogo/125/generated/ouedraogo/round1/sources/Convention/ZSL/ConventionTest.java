package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for {@link Convention}.
 *
 * These tests cover every public static method in the {@code Convention} class,
 * exercising both the successful (no‑exception) paths and the failure paths that
 * should raise {@link IllegalArgumentException} with the expected message.
 */
public class ConventionTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* --------------------------------------------------------------------- */
    /* throwIfNull                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testThrowIfNull_WithNonNull_DoesNotThrow() {
        // No exception expected
        Convention.throwIfNull(new Object(), "should not throw");
    }

    @Test
    public void testThrowIfNull_WithNull_ThrowsIllegalArgumentException() {
        String expectedMessage = "value is null";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(expectedMessage);

        Convention.throwIfNull(null, expectedMessage);
    }

    /* --------------------------------------------------------------------- */
    /* throwIfNullOrEmpty                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testThrowIfNullOrEmpty_WithValidString_DoesNotThrow() {
        Convention.throwIfNullOrEmpty("not empty", "won't be thrown");
    }

    @Test
    public void testThrowIfNullOrEmpty_WithNull_ThrowsIllegalArgumentException() {
        String expectedMessage = "null string";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(expectedMessage);

        Convention.throwIfNullOrEmpty(null, expectedMessage);
    }

    @Test
    public void testThrowIfNullOrEmpty_WithEmptyString_ThrowsIllegalArgumentException() {
        String expectedMessage = "empty string";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(expectedMessage);

        Convention.throwIfNullOrEmpty("", expectedMessage);
    }

    /* --------------------------------------------------------------------- */
    /* throwIfClassNotFound                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testThrowIfClassNotFound_WithValidClass_DoesNotThrow() {
        // Use the current class loader and a known class.
        Convention.throwIfClassNotFound("java.lang.String", this.getClass().getClassLoader());
    }

    @Test
    public void testThrowIfClassNotFound_WithNullClassName_ThrowsIllegalArgumentException() {
        String expectedMessage = "fully qualified ClassName cannot be null or empty";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(expectedMessage);

        Convention.throwIfClassNotFound(null, this.getClass().getClassLoader());
    }

    @Test
    public void testThrowIfClassNotFound_WithEmptyClassName_ThrowsIllegalArgumentException() {
        String expectedMessage = "fully qualified ClassName cannot be null or empty";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(expectedMessage);

        Convention.throwIfClassNotFound("", this.getClass().getClassLoader());
    }

    @Test
    public void testThrowIfClassNotFound_WithNullLoader_ThrowsIllegalArgumentException() {
        String expectedMessage = "loader cannot be null";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(expectedMessage);

        Convention.throwIfClassNotFound("java.lang.String", null);
    }

    @Test
    public void testThrowIfClassNotFound_WithUnknownClass_ThrowsIllegalArgumentException() {
        String unknownClass = "com.cedarsoftware.util.DoesNotExist";
        String expectedMessageStart = "Unknown class: " + unknownClass + " was not found.";

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(expectedMessageStart);

        Convention.throwIfClassNotFound(unknownClass, this.getClass().getClassLoader());
    }

    /* --------------------------------------------------------------------- */
    /* throwIfKeyExists                                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testThrowIfKeyExists_MapDoesNotContainKey_DoesNotThrow() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Convention.throwIfKeyExists(map, "b", "key already present");
    }

    @Test
    public void testThrowIfKeyExists_MapContainsKey_ThrowsIllegalArgumentException() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key", 42);

        String expectedMessage = "duplicate key";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(expectedMessage);

        Convention.throwIfKeyExists(map, "key", expectedMessage);
    }

    @Test
    public void testThrowIfKeyExists_NullMap_ThrowsIllegalArgumentException() {
        String expectedMessage = "map cannot be null";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(expectedMessage);

        Convention.throwIfKeyExists(null, "anyKey", "unused");
    }

    @Test
    public void testThrowIfKeyExists_NullKey_ThrowsIllegalArgumentException() {
        Map<String, Integer> map = new HashMap<>();
        map.put("present", 1);

        String expectedMessage = "key cannot be null";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(expectedMessage);

        Convention.throwIfKeyExists(map, null, "unused");
    }

    /* --------------------------------------------------------------------- */
    /* throwIfFalse                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testThrowIfFalse_WithTrue_DoesNotThrow() {
        Convention.throwIfFalse(true, "should not throw");
    }

    @Test
    public void testThrowIfFalse_WithFalse_ThrowsIllegalArgumentException() {
        String expectedMessage = "logic failed";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(expectedMessage);

        Convention.throwIfFalse(false, expectedMessage);
    }
}
