package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;

/**
 * JUnit‑4 test suite for {@link Convention}.
 *
 * The tests cover:
 *   • Normal (non‑exception) behaviour
 *   • All identified edge‑cases and exception paths
 *
 * No external mocking frameworks are used; only the classes that appear in the
 * source code or in the allowed dependency signatures are instantiated.
 */
public class ConventionTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* --------------------------------------------------------------
     *  Tests for throwIfNull(Object, String)
     * -------------------------------------------------------------- */

    @Test
    public void testThrowIfNull_WithNonNullValue_DoesNotThrow() {
        // Should simply return without exception
        Convention.throwIfNull(new Object(), "value must not be null");
    }

    @Test
    public void testThrowIfNull_WithNullValue_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("must be supplied");
        Convention.throwIfNull(null, "must be supplied");
    }

    /* --------------------------------------------------------------
     *  Tests for throwIfNullOrEmpty(String, String)
     * -------------------------------------------------------------- */

    @Test
    public void testThrowIfNullOrEmpty_WithValidString_DoesNotThrow() {
        Convention.throwIfNullOrEmpty("hello", "must not be null/empty");
    }

    @Test
    public void testThrowIfNullOrEmpty_WithNullString_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("cannot be null");
        Convention.throwIfNullOrEmpty(null, "cannot be null");
    }

    @Test
    public void testThrowIfNullOrEmpty_WithEmptyString_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("empty");
        Convention.throwIfNullOrEmpty("", "empty");
    }

    /* --------------------------------------------------------------
     *  Tests for throwIfClassNotFound(String, ClassLoader)
     * -------------------------------------------------------------- */

    @Test
    public void testThrowIfClassNotFound_WithExistingClass_DoesNotThrow() {
        // java.lang.String is guaranteed to exist
        Convention.throwIfClassNotFound("java.lang.String", this.getClass().getClassLoader());
    }

    @Test
    public void testThrowIfClassNotFound_WithNullClassName_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("fully qualified ClassName cannot be null or empty");
        Convention.throwIfClassNotFound(null, this.getClass().getClassLoader());
    }

    @Test
    public void testThrowIfClassNotFound_WithEmptyClassName_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("fully qualified ClassName cannot be null or empty");
        Convention.throwIfClassNotFound("", this.getClass().getClassLoader());
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
        Convention.throwIfClassNotFound(unknown, this.getClass().getClassLoader());
    }

    /* --------------------------------------------------------------
     *  Tests for throwIfKeyExists(Map<K,V>, K, String)
     * -------------------------------------------------------------- */

    @Test
    public void testThrowIfKeyExists_KeyNotPresent_DoesNotThrow() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Convention.throwIfKeyExists(map, "b", "key b already exists");
    }

    @Test
    public void testThrowIfKeyExists_KeyPresent_ThrowsIllegalArgumentException() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key", 42);
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("duplicate");
        Convention.throwIfKeyExists(map, "key", "duplicate");
    }

    @Test
    public void testThrowIfKeyExists_WithNullMap_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("map cannot be null");
        Convention.throwIfKeyExists(null, "any", "message");
    }

    @Test
    public void testThrowIfKeyExists_WithNullKey_ThrowsIllegalArgumentException() {
        Map<String, Integer> map = new HashMap<>();
        map.put("present", 1);
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("key cannot be null");
        Convention.throwIfKeyExists(map, null, "message");
    }

    /* --------------------------------------------------------------
     *  Tests for throwIfFalse(boolean, String)
     * -------------------------------------------------------------- */

    @Test
    public void testThrowIfFalse_WithTrue_DoesNotThrow() {
        Convention.throwIfFalse(true, "should not fail");
    }

    @Test
    public void testThrowIfFalse_WithFalse_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("must be true");
        Convention.throwIfFalse(false, "must be true");
    }
}
