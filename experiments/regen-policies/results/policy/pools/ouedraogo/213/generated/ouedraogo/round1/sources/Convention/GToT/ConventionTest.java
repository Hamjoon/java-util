package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive JUnit‑4 test suite for {@link Convention}.
 *
 * The tests cover:
 *   • Normal (non‑exception) paths for every public method.
 *   • All identified edge‑cases and expected IllegalArgumentException throws.
 *
 * No external mocking frameworks are used – only real objects
 * constructed with the public API of Java and the classes that are
 * already part of the {@code com.cedarsoftware.util} package.
 */
public class ConventionTest {

    /* -------------------------------------------------------
     *  Tests for throwIfNull(Object, String)
     * ------------------------------------------------------- */

    @Test
    public void testThrowIfNull_WithNonNullValue_DoesNotThrow() {
        // normal case – should complete without exception
        Convention.throwIfNull(new Object(), "value must not be null");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfNull_WithNullValue_ThrowsException() {
        Convention.throwIfNull(null, "value must not be null");
    }

    /* -------------------------------------------------------
     *  Tests for throwIfNullOrEmpty(String, String)
     * ------------------------------------------------------- */

    @Test
    public void testThrowIfNullOrEmpty_WithNonEmptyString_DoesNotThrow() {
        Convention.throwIfNullOrEmpty("hello", "string must not be empty");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfNullOrEmpty_WithNullString_ThrowsException() {
        Convention.throwIfNullOrEmpty(null, "string must not be empty");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfNullOrEmpty_WithEmptyString_ThrowsException() {
        Convention.throwIfNullOrEmpty("", "string must not be empty");
    }

    /* -------------------------------------------------------
     *  Tests for throwIfClassNotFound(String, ClassLoader)
     * ------------------------------------------------------- */

    @Test
    public void testThrowIfClassNotFound_WithExistingClass_DoesNotThrow() {
        // java.lang.String is guaranteed to exist
        Convention.throwIfClassNotFound("java.lang.String",
                Thread.currentThread().getContextClassLoader());
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

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfClassNotFound_WithNonExistentClass_ThrowsException() {
        Convention.throwIfClassNotFound("com.cedarsoftware.util.DoesNotExist",
                Thread.currentThread().getContextClassLoader());
    }

    /* -------------------------------------------------------
     *  Tests for throwIfKeyExists(Map<K,V>, K, String)
     * ------------------------------------------------------- */

    @Test
    public void testThrowIfKeyExists_KeyNotPresent_DoesNotThrow() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        Convention.throwIfKeyExists(map, "two", "key already present");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfKeyExists_KeyAlreadyPresent_ThrowsException() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key", 42);
        Convention.throwIfKeyExists(map, "key", "key already present");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfKeyExists_WithNullMap_ThrowsException() {
        Convention.<String,Integer>throwIfKeyExists(null, "anyKey", "map cannot be null");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfKeyExists_WithNullKey_ThrowsException() {
        Map<String, Integer> map = new HashMap<>();
        map.put("present", 1);
        Convention.throwIfKeyExists(map, null, "key cannot be null");
    }

    /* -------------------------------------------------------
     *  Tests for throwIfFalse(boolean, String)
     * ------------------------------------------------------- */

    @Test
    public void testThrowIfFalse_WithTrue_DoesNotThrow() {
        Convention.throwIfFalse(true, "logic must be true");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfFalse_WithFalse_ThrowsException() {
        Convention.throwIfFalse(false, "logic must be true");
    }
}
