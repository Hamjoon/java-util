package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for {@link Convention}.
 * <p>
 * The tests cover all public static methods and verify both the
 * successful path (no exception) and the failure path (exception with
 * the expected message) for each method.
 */
public class ConventionTest {

    /* -------------------------------------------------------
     *  throwIfNull
     * ------------------------------------------------------- */
    @Test
    public void testThrowIfNull_WithNonNullValue_DoesNotThrow() {
        // No exception expected
        Convention.throwIfNull(new Object(), "should not throw");
    }

    @Test
    public void testThrowIfNull_WithNullValue_ThrowsIllegalArgumentException() {
        try {
            Convention.throwIfNull(null, "value is null");
            fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            assertEquals("value is null", e.getMessage());
        }
    }

    /* -------------------------------------------------------
     *  throwIfNullOrEmpty
     * ------------------------------------------------------- */
    @Test
    public void testThrowIfNullOrEmpty_WithValidString_DoesNotThrow() {
        Convention.throwIfNullOrEmpty("non‑empty", "should not throw");
    }

    @Test
    public void testThrowIfNullOrEmpty_WithNull_ThrowsIllegalArgumentException() {
        try {
            Convention.throwIfNullOrEmpty(null, "string is null");
            fail("Expected IllegalArgumentException for null string");
        } catch (IllegalArgumentException e) {
            assertEquals("string is null", e.getMessage());
        }
    }

    @Test
    public void testThrowIfNullOrEmpty_WithEmptyString_ThrowsIllegalArgumentException() {
        try {
            Convention.throwIfNullOrEmpty("", "string is empty");
            fail("Expected IllegalArgumentException for empty string");
        } catch (IllegalArgumentException e) {
            assertEquals("string is empty", e.getMessage());
        }
    }

    /* -------------------------------------------------------
     *  throwIfClassNotFound
     * ------------------------------------------------------- */
    @Test
    public void testThrowIfClassNotFound_WithValidClass_DoesNotThrow() {
        // Using a core JDK class guarantees that it can be loaded.
        String className = "java.lang.String";
        ClassLoader loader = ClassLoader.getSystemClassLoader();

        Convention.throwIfClassNotFound(className, loader);
    }

    @Test
    public void testThrowIfClassNotFound_WithNullClassName_ThrowsIllegalArgumentException() {
        try {
            Convention.throwIfClassNotFound(null, ClassLoader.getSystemClassLoader());
            fail("Expected IllegalArgumentException for null class name");
        } catch (IllegalArgumentException e) {
            assertEquals("fully qualified ClassName cannot be null or empty", e.getMessage());
        }
    }

    @Test
    public void testThrowIfClassNotFound_WithEmptyClassName_ThrowsIllegalArgumentException() {
        try {
            Convention.throwIfClassNotFound("", ClassLoader.getSystemClassLoader());
            fail("Expected IllegalArgumentException for empty class name");
        } catch (IllegalArgumentException e) {
            assertEquals("fully qualified ClassName cannot be null or empty", e.getMessage());
        }
    }

    @Test
    public void testThrowIfClassNotFound_WithNullClassLoader_ThrowsIllegalArgumentException() {
        try {
            Convention.throwIfClassNotFound("java.lang.String", null);
            fail("Expected IllegalArgumentException for null class loader");
        } catch (IllegalArgumentException e) {
            assertEquals("loader cannot be null", e.getMessage());
        }
    }

    @Test
    public void testThrowIfClassNotFound_WithNonExistentClass_ThrowsIllegalArgumentException() {
        String bogusClass = "com.this.class.DoesNotExist";
        try {
            Convention.throwIfClassNotFound(bogusClass, ClassLoader.getSystemClassLoader());
            fail("Expected IllegalArgumentException for unknown class");
        } catch (IllegalArgumentException e) {
            assertEquals(
                "Unknown class: " + bogusClass + " was not found.",
                e.getMessage()
            );
        }
    }

    /* -------------------------------------------------------
     *  throwIfKeyExists
     * ------------------------------------------------------- */
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
        try {
            Convention.throwIfKeyExists(map, "key", "duplicate key");
            fail("Expected IllegalArgumentException because key already exists");
        } catch (IllegalArgumentException e) {
            assertEquals("duplicate key", e.getMessage());
        }
    }

    @Test
    public void testThrowIfKeyExists_WithNullMap_ThrowsIllegalArgumentException() {
        try {
            Convention.throwIfKeyExists(null, "any", "map null");
            fail("Expected IllegalArgumentException for null map");
        } catch (IllegalArgumentException e) {
            assertEquals("map cannot be null", e.getMessage());
        }
    }

    @Test
    public void testThrowIfKeyExists_WithNullKey_ThrowsIllegalArgumentException() {
        Map<String, Integer> map = new HashMap<>();
        try {
            Convention.throwIfKeyExists(map, null, "key null");
            fail("Expected IllegalArgumentException for null key");
        } catch (IllegalArgumentException e) {
            assertEquals("key cannot be null", e.getMessage());
        }
    }

    /* -------------------------------------------------------
     *  throwIfFalse
     * ------------------------------------------------------- */
    @Test
    public void testThrowIfFalse_WithTrue_DoesNotThrow() {
        Convention.throwIfFalse(true, "should not throw");
    }

    @Test
    public void testThrowIfFalse_WithFalse_ThrowsIllegalArgumentException() {
        try {
            Convention.throwIfFalse(false, "condition failed");
            fail("Expected IllegalArgumentException because logic is false");
        } catch (IllegalArgumentException e) {
            assertEquals("condition failed", e.getMessage());
        }
    }
}
