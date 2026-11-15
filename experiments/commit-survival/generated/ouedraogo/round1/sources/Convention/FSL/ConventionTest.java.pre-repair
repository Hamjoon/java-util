package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class ConventionTest {

    /* ---------- throwIfNull ---------- */

    @Test
    public void testThrowIfNull_WithNonNull_ShouldNotThrow() {
        // No exception expected
        Convention.throwIfNull(new Object(), "should not throw");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfNull_WithNull_ShouldThrow() {
        Convention.throwIfNull(null, "value is null");
    }

    /* ---------- throwIfNullOrEmpty ---------- */

    @Test
    public void testThrowIfNullOrEmpty_WithNonEmptyString_ShouldNotThrow() {
        Convention.throwIfNullOrEmpty("abc", "won't throw");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfNullOrEmpty_WithNull_ShouldThrow() {
        Convention.throwIfNullOrEmpty(null, "null not allowed");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfNullOrEmpty_WithEmptyString_ShouldThrow() {
        Convention.throwIfNullOrEmpty("", "empty not allowed");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfNullOrEmpty_WithWhitespaceString_ShouldThrow() {
        // Assuming StringUtilities.isEmpty treats whitespace as empty; if not, this test will fail.
        Convention.throwIfNullOrEmpty("   ", "whitespace not allowed");
    }

    /* ---------- throwIfClassNotFound ---------- */

    @Test
    public void testThrowIfClassNotFound_WithExistingClass_ShouldNotThrow() {
        // java.lang.String is guaranteed to exist
        Convention.throwIfClassNotFound("java.lang.String", ClassLoader.getSystemClassLoader());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfClassNotFound_WithNullClassName_ShouldThrow() {
        Convention.throwIfClassNotFound(null, ClassLoader.getSystemClassLoader());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfClassNotFound_WithEmptyClassName_ShouldThrow() {
        Convention.throwIfClassNotFound("", ClassLoader.getSystemClassLoader());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfClassNotFound_WithNullClassLoader_ShouldThrow() {
        Convention.throwIfClassNotFound("java.lang.String", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfClassNotFound_WithNonExistingClass_ShouldThrow() {
        Convention.throwIfClassNotFound("com.cedarsoftware.util.DoesNotExist", ClassLoader.getSystemClassLoader());
    }

    /* ---------- throwIfKeyExists ---------- */

    @Test
    public void testThrowIfKeyExists_KeyAbsent_ShouldNotThrow() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Convention.throwIfKeyExists(map, "b", "key exists");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfKeyExists_KeyPresent_ShouldThrow() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key", 42);
        Convention.throwIfKeyExists(map, "key", "duplicate key");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfKeyExists_NullMap_ShouldThrow() {
        Convention.throwIfKeyExists(null, "any", "map is null");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfKeyExists_NullKey_ShouldThrow() {
        Map<String, Integer> map = new HashMap<>();
        map.put("some", 1);
        Convention.throwIfKeyExists(map, null, "key is null");
    }

    /* ---------- throwIfFalse ---------- */

    @Test
    public void testThrowIfFalse_WithTrue_ShouldNotThrow() {
        Convention.throwIfFalse(true, "should not fail");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfFalse_WithFalse_ShouldThrow() {
        Convention.throwIfFalse(false, "logic failed");
    }

    /* ---------- Additional sanity checks ---------- */

    @Test
    public void testThrowIfFalse_MessagePreserved() {
        String msg = "custom message";
        try {
            Convention.throwIfFalse(false, msg);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals(msg, e.getMessage());
        }
    }

    @Test
    public void testThrowIfNullOrEmpty_MessagePreserved() {
        String msg = "null not allowed";
        try {
            Convention.throwIfNullOrEmpty(null, msg);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals(msg, e.getMessage());
        }
    }

    @Test
    public void testThrowIfKeyExists_MessagePreserved() {
        Map<String, String> map = new HashMap<>();
        map.put("k", "v");
        String msg = "duplicate entry";
        try {
            Convention.throwIfKeyExists(map, "k", msg);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals(msg, e.getMessage());
        }
    }
}
