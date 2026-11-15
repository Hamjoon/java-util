package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive JUnit‑4 test suite for {@link Convention}.
 *
 * <p>This test class covers typical usage, edge cases and error scenarios for every
 * public static method in {@code Convention}.  All tests use only the classes,
 * constructors and methods that appear in the source code or its declared
 * dependencies (e.g. {@code StringUtilities}, {@code ClassUtilities},
 * {@code java.util.Map}).  No mocking frameworks or external libraries are used.</p>
 */
public class ConventionTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* ---------------------------------------------------------------------- */
    /* throwIfNull                                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testThrowIfNull_WithNonNullValue_DoesNotThrow() {
        // No exception should be thrown for a non‑null object.
        Convention.throwIfNull(new Object(), "should not throw");
    }

    @Test
    public void testThrowIfNull_WithNullValue_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("value is null");
        Convention.throwIfNull(null, "value is null");
    }

    /* ---------------------------------------------------------------------- */
    /* throwIfNullOrEmpty                                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testThrowIfNullOrEmpty_WithNonEmptyString_DoesNotThrow() {
        Convention.throwIfNullOrEmpty("non‑empty", "should not throw");
    }

    @Test
    public void testThrowIfNullOrEmpty_WithNullString_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("null string");
        Convention.throwIfNullOrEmpty(null, "null string");
    }

    @Test
    public void testThrowIfNullOrEmpty_WithEmptyString_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("empty string");
        Convention.throwIfNullOrEmpty("", "empty string");
    }

    /* ---------------------------------------------------------------------- */
    /* throwIfClassNotFound                                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testThrowIfClassNotFound_WithExistingClass_DoesNotThrow() {
        // java.lang.String is guaranteed to exist.
        Convention.throwIfClassNotFound("java.lang.String", Convention.class.getClassLoader());
    }

    @Test
    public void testThrowIfClassNotFound_WithNullClassName_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("fully qualified ClassName cannot be null or empty");
        Convention.throwIfClassNotFound(null, Convention.class.getClassLoader());
    }

    @Test
    public void testThrowIfClassNotFound_WithEmptyClassName_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("fully qualified ClassName cannot be null or empty");
        Convention.throwIfClassNotFound("", Convention.class.getClassLoader());
    }

    @Test
    public void testThrowIfClassNotFound_WithNullClassLoader_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("loader cannot be null");
        Convention.throwIfClassNotFound("java.lang.String", null);
    }

    @Test
    public void testThrowIfClassNotFound_WithUnknownClass_ThrowsIllegalArgumentException() {
        String unknown = "com.cedarsoftware.util.DoesNotExist";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unknown class: " + unknown + " was not found.");
        Convention.throwIfClassNotFound(unknown, Convention.class.getClassLoader());
    }

    /* ---------------------------------------------------------------------- */
    /* throwIfKeyExists                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testThrowIfKeyExists_KeyAbsent_DoesNotThrow() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        Convention.throwIfKeyExists(map, "two", "key already present");
    }

    @Test
    public void testThrowIfKeyExists_KeyPresent_ThrowsIllegalArgumentException() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key", 42);
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("duplicate key");
        Convention.throwIfKeyExists(map, "key", "duplicate key");
    }

    @Test
    public void testThrowIfKeyExists_WithNullMap_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("map cannot be null");
        Convention.throwIfKeyExists(null, "any", "should not matter");
    }

    @Test
    public void testThrowIfKeyExists_WithNullKey_ThrowsIllegalArgumentException() {
        Map<String, Integer> map = new HashMap<>();
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("key cannot be null");
        Convention.throwIfKeyExists(map, null, "should not matter");
    }

    /* ---------------------------------------------------------------------- */
    /* throwIfFalse                                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testThrowIfFalse_WithTrue_DoesNotThrow() {
        Convention.throwIfFalse(true, "should not throw");
    }

    @Test
    public void testThrowIfFalse_WithFalse_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("logic failed");
        Convention.throwIfFalse(false, "logic failed");
    }
}
