package com.cedarsoftware.util;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for {@link Convention}. All public static methods are exercised,
 * covering normal operation as well as the various error conditions that
 * result in {@link IllegalArgumentException}.
 */
public class ConventionTest {

    /* --------------------------------------------------------------------- *
     * throwIfNull
     * --------------------------------------------------------------------- */

    @Test
    public void testThrowIfNull_WithNonNullValue_DoesNotThrow() {
        // No exception expected for a non‑null argument.
        Convention.throwIfNull(new Object(), "value must not be null");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfNull_WithNullValue_ThrowsException() {
        // The supplied message should be propagated in the exception.
        Convention.throwIfNull(null, "value is null");
    }

    /* --------------------------------------------------------------------- *
     * throwIfNullOrEmpty
     * --------------------------------------------------------------------- */

    @Test
    public void testThrowIfNullOrEmpty_WithNonEmptyString_DoesNotThrow() {
        Convention.throwIfNullOrEmpty("hello", "must not be empty");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfNullOrEmpty_WithNullString_ThrowsException() {
        Convention.throwIfNullOrEmpty(null, "string is null");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfNullOrEmpty_WithEmptyString_ThrowsException() {
        Convention.throwIfNullOrEmpty("", "string is empty");
    }

    /* --------------------------------------------------------------------- *
     * throwIfFalse
     * --------------------------------------------------------------------- */

    @Test
    public void testThrowIfFalse_WithTrue_DoesNotThrow() {
        Convention.throwIfFalse(true, "logic should pass");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfFalse_WithFalse_ThrowsException() {
        Convention.throwIfFalse(false, "logic failed");
    }

    /* --------------------------------------------------------------------- *
     * throwIfClassNotFound
     * --------------------------------------------------------------------- */

    @Test
    public void testThrowIfClassNotFound_WithExistingClass_DoesNotThrow() {
        // java.lang.String is guaranteed to be present.
        Convention.throwIfClassNotFound(
                "java.lang.String",
                Thread.currentThread().getContextClassLoader());
    }

    @Test
    public void testThrowIfClassNotFound_WithNonExistingClass_ThrowsException() {
        try {
            Convention.throwIfClassNotFound(
                    "com.nonexistent.FooBarBaz",
                    Thread.currentThread().getContextClassLoader());
            fail("Expected IllegalArgumentException for unknown class");
        } catch (IllegalArgumentException e) {
            assertTrue("Exception message should contain 'Unknown class'",
                    e.getMessage().contains("Unknown class"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfClassNotFound_WithNullClassName_ThrowsException() {
        Convention.throwIfClassNotFound(null,
                Thread.currentThread().getContextClassLoader());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfClassNotFound_WithEmptyClassName_ThrowsException() {
        Convention.throwIfClassNotFound("",
                Thread.currentThread().getContextClassLoader());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfClassNotFound_WithNullClassLoader_ThrowsException() {
        Convention.throwIfClassNotFound("java.lang.String", null);
    }

    /* --------------------------------------------------------------------- *
     * throwIfKeyExists
     * --------------------------------------------------------------------- */

    @Test
    public void testThrowIfKeyExists_KeyAbsent_DoesNotThrow() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        // key "c" does not exist – no exception expected.
        Convention.throwIfKeyExists(map, "c", "key already present");
    }

    @Test
    public void testThrowIfKeyExists_KeyPresent_ThrowsException() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key1", 42);
        try {
            Convention.throwIfKeyExists(map, "key1", "duplicate key");
            fail("Expected IllegalArgumentException because key already exists");
        } catch (IllegalArgumentException e) {
            assertEquals("duplicate key", e.getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfKeyExists_NullMap_ThrowsException() {
        Convention.throwIfKeyExists(null, "anyKey", "map cannot be null");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfKeyExists_NullKey_ThrowsException() {
        Map<String, Integer> map = new HashMap<>();
        map.put("existing", 1);
        Convention.throwIfKeyExists(map, null, "key cannot be null");
    }
}
