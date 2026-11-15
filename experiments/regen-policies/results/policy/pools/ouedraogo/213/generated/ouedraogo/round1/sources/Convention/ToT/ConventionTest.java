package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive JUnit 4 test cases for {@link Convention}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal (non‑exceptional) behavior</li>
 *   <li>All error paths (null, empty, false, duplicate key, unknown class, etc.)</li>
 *   <li>Verification that the supplied exception message is preserved</li>
 * </ul>
 *
 * Only the public API of {@link Convention} and the helper classes that appear in the
 * source signature ( {@link StringUtilities} and {@link ClassUtilities} ) are used.
 */
public class ConventionTest {

    /* ---------------------------------------------------------------------- */
    /*  throwIfNull                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testThrowIfNull_WithNonNull_DoesNotThrow() {
        // Normal case – should simply return without an exception.
        Convention.throwIfNull(new Object(), "should not be thrown");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfNull_WithNull_ThrowsException() {
        // Null value must trigger an IllegalArgumentException with the supplied message.
        Convention.throwIfNull(null, "value is null");
    }

    @Test
    public void testThrowIfNull_ExceptionMessagePreserved() {
        String expectedMessage = "custom null message";
        try {
            Convention.throwIfNull(null, expectedMessage);
            fail("Expected IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            assertEquals(expectedMessage, e.getMessage());
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  throwIfNullOrEmpty                                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testThrowIfNullOrEmpty_WithValidString_DoesNotThrow() {
        Convention.throwIfNullOrEmpty("non‑empty", "should not be thrown");
    }

    @Test
    public void testThrowIfNullOrEmpty_WithNull_ThrowsException() {
        try {
            Convention.throwIfNullOrEmpty(null, "null string");
            fail("Expected IllegalArgumentException for null string");
        } catch (IllegalArgumentException e) {
            assertEquals("null string", e.getMessage());
        }
    }

    @Test
    public void testThrowIfNullOrEmpty_WithEmptyString_ThrowsException() {
        try {
            Convention.throwIfNullOrEmpty("", "empty string");
            fail("Expected IllegalArgumentException for empty string");
        } catch (IllegalArgumentException e) {
            assertEquals("empty string", e.getMessage());
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  throwIfClassNotFound                                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testThrowIfClassNotFound_WithExistingClass_DoesNotThrow() {
        // java.lang.String is guaranteed to be present.
        Convention.throwIfClassNotFound("java.lang.String", Thread.currentThread().getContextClassLoader());
    }

    @Test
    public void testThrowIfClassNotFound_WithNonExistingClass_ThrowsException() {
        String bogusClass = "com.cedarsoftware.util.DoesNotExist";
        try {
            Convention.throwIfClassNotFound(bogusClass, Thread.currentThread().getContextClassLoader());
            fail("Expected IllegalArgumentException for unknown class");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unknown class: " + bogusClass));
        }
    }

    @Test
    public void testThrowIfClassNotFound_WithNullClassName_ThrowsException() {
        try {
            Convention.throwIfClassNotFound(null, Thread.currentThread().getContextClassLoader());
            fail("Expected IllegalArgumentException for null class name");
        } catch (IllegalArgumentException e) {
            assertEquals("fully qualified ClassName cannot be null or empty", e.getMessage());
        }
    }

    @Test
    public void testThrowIfClassNotFound_WithEmptyClassName_ThrowsException() {
        try {
            Convention.throwIfClassNotFound("", Thread.currentThread().getContextClassLoader());
            fail("Expected IllegalArgumentException for empty class name");
        } catch (IllegalArgumentException e) {
            assertEquals("fully qualified ClassName cannot be null or empty", e.getMessage());
        }
    }

    @Test
    public void testThrowIfClassNotFound_WithNullClassLoader_ThrowsException() {
        try {
            Convention.throwIfClassNotFound("java.lang.String", null);
            fail("Expected IllegalArgumentException for null ClassLoader");
        } catch (IllegalArgumentException e) {
            assertEquals("loader cannot be null", e.getMessage());
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  throwIfKeyExists                                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testThrowIfKeyExists_KeyAbsent_DoesNotThrow() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Convention.throwIfKeyExists(map, "b", "key already present");
    }

    @Test
    public void testThrowIfKeyExists_KeyPresent_ThrowsException() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key", 42);
        try {
            Convention.throwIfKeyExists(map, "key", "duplicate key");
            fail("Expected IllegalArgumentException for duplicate key");
        } catch (IllegalArgumentException e) {
            assertEquals("duplicate key", e.getMessage());
        }
    }

    @Test
    public void testThrowIfKeyExists_NullMap_ThrowsException() {
        try {
            Convention.throwIfKeyExists(null, "any", "map null");
            fail("Expected IllegalArgumentException for null map");
        } catch (IllegalArgumentException e) {
            assertEquals("map cannot be null", e.getMessage());
        }
    }

    @Test
    public void testThrowIfKeyExists_NullKey_ThrowsException() {
        Map<String, Integer> map = new HashMap<>();
        map.put("present", 1);
        try {
            Convention.throwIfKeyExists(map, null, "key null");
            fail("Expected IllegalArgumentException for null key");
        } catch (IllegalArgumentException e) {
            assertEquals("key cannot be null", e.getMessage());
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  throwIfFalse                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testThrowIfFalse_WithTrue_DoesNotThrow() {
        Convention.throwIfFalse(true, "should not be thrown");
    }

    @Test
    public void testThrowIfFalse_WithFalse_ThrowsException() {
        try {
            Convention.throwIfFalse(false, "logic failed");
            fail("Expected IllegalArgumentException for false logic");
        } catch (IllegalArgumentException e) {
            assertEquals("logic failed", e.getMessage());
        }
    }

    @Test
    public void testThrowIfFalse_MessagePreserved() {
        String msg = "specific error message";
        try {
            Convention.throwIfFalse(false, msg);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals(msg, e.getMessage());
        }
    }
}
