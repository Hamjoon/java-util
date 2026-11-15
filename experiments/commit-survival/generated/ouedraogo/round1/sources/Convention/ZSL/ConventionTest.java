package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for {@link Convention}.
 *
 * The tests cover:
 * <ul>
 *   <li>{@link Convention#throwIfNull(Object, String)}</li>
 *   <li>{@link Convention#throwIfNullOrEmpty(String, String)}</li>
 *   <li>{@link Convention#throwIfClassNotFound(String, ClassLoader)}</li>
 *   <li>{@link Convention#throwIfKeyExists(Map, Object, String)}</li>
 *   <li>{@link Convention#throwIfFalse(boolean, String)}</li>
 * </ul>
 *
 * All tests are written using JUnit 4.
 */
public class ConventionTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* -------------------------------------------------------------
     * Tests for throwIfNull
     * ------------------------------------------------------------- */

    @Test
    public void testThrowIfNull_WithNonNull_DoesNotThrow() {
        // No exception expected
        Convention.throwIfNull(new Object(), "Should not throw");
    }

    @Test
    public void testThrowIfNull_WithNull_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("null value not allowed");
        Convention.throwIfNull(null, "null value not allowed");
    }

    /* -------------------------------------------------------------
     * Tests for throwIfNullOrEmpty
     * ------------------------------------------------------------- */

    @Test
    public void testThrowIfNullOrEmpty_WithNonEmptyString_DoesNotThrow() {
        Convention.throwIfNullOrEmpty("non-empty", "must not be empty");
    }

    @Test
    public void testThrowIfNullOrEmpty_WithEmptyString_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("empty not allowed");
        Convention.throwIfNullOrEmpty("", "empty not allowed");
    }

    @Test
    public void testThrowIfNullOrEmpty_WithNullString_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("null not allowed");
        Convention.throwIfNullOrEmpty(null, "null not allowed");
    }

    /* -------------------------------------------------------------
     * Tests for throwIfClassNotFound
     * ------------------------------------------------------------- */

    @Test
    public void testThrowIfClassNotFound_WithExistingClass_DoesNotThrow() {
        // java.lang.String is guaranteed to exist
        Convention.throwIfClassNotFound("java.lang.String", getClass().getClassLoader());
    }

    @Test
    public void testThrowIfClassNotFound_WithNonExistingClass_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unknown class: com.example.DoesNotExist was not found.");
        Convention.throwIfClassNotFound("com.example.DoesNotExist", getClass().getClassLoader());
    }

    @Test
    public void testThrowIfClassNotFound_WithNullClassName_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("fully qualified ClassName cannot be null or empty");
        Convention.throwIfClassNotFound(null, getClass().getClassLoader());
    }

    @Test
    public void testThrowIfClassNotFound_WithEmptyClassName_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("fully qualified ClassName cannot be null or empty");
        Convention.throwIfClassNotFound("", getClass().getClassLoader());
    }

    @Test
    public void testThrowIfClassNotFound_WithNullClassLoader_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("loader cannot be null");
        Convention.throwIfClassNotFound("java.lang.String", null);
    }

    /* -------------------------------------------------------------
     * Tests for throwIfKeyExists
     * ------------------------------------------------------------- */

    @Test
    public void testThrowIfKeyExists_KeyAbsent_DoesNotThrow() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        Convention.throwIfKeyExists(map, "two", "Key already exists");
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
    public void testThrowIfKeyExists_NullMap_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("map cannot be null");
        Convention.throwIfKeyExists(null, "any", "msg");
    }

    @Test
    public void testThrowIfKeyExists_NullKey_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("key cannot be null");
        Convention.throwIfKeyExists(new HashMap<>(), null, "msg");
    }

    /* -------------------------------------------------------------
     * Tests for throwIfFalse
     * ------------------------------------------------------------- */

    @Test
    public void testThrowIfFalse_WithTrue_DoesNotThrow() {
        Convention.throwIfFalse(true, "should not throw");
    }

    @Test
    public void testThrowIfFalse_WithFalse_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("condition failed");
        Convention.throwIfFalse(false, "condition failed");
    }
}
