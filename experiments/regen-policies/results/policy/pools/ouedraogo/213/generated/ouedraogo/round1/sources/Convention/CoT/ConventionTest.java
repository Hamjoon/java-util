package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * JUnit 4 test cases for {@link Convention}.
 *
 * The test class follows the required steps:
 * 1. List all public methods (see comments above each test).
 * 2. Provide a basic functional test for each method.
 * 3. Identify edge‑case / exception scenarios.
 * 4. Add dedicated tests for those scenarios.
 */
public class ConventionTest {

    /* ---------------------------------------------------------------------- */
    /* 1. Public methods of Convention (signatures)                           */
    /* ---------------------------------------------------------------------- */
    // public static void throwIfNull(Object value, String message)
    // public static void throwIfNullOrEmpty(String value, String message)
    // public static void throwIfClassNotFound(String fullyQualifiedClassName,
    //                                          ClassLoader loader)
    // public static <K,V> void throwIfKeyExists(Map<K,V> map, K key,
    //                                          String message)
    // public static void throwIfFalse(boolean logic, String message)

    /* ---------------------------------------------------------------------- */
    /* 2. Basic functional tests                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testThrowIfNull_WithNonNullValue_DoesNotThrow() {
        // should pass silently
        Convention.throwIfNull(new Object(), "value must not be null");
    }

    @Test
    public void testThrowIfNullOrEmpty_WithNonEmptyString_DoesNotThrow() {
        Convention.throwIfNullOrEmpty("not empty", "string must not be empty");
    }

    @Test
    public void testThrowIfClassNotFound_WithExistingClass_DoesNotThrow() {
        // Use the current class name – it is guaranteed to exist.
        String className = Convention.class.getName();
        ClassLoader loader = Convention.class.getClassLoader();
        Convention.throwIfClassNotFound(className, loader);
    }

    @Test
    public void testThrowIfKeyExists_KeyAbsent_DoesNotThrow() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Convention.<String, Integer>throwIfKeyExists(map, "b", "key already exists");
    }

    @Test
    public void testThrowIfFalse_WithTrueLogic_DoesNotThrow() {
        Convention.throwIfFalse(true, "logic must be true");
    }

    /* ---------------------------------------------------------------------- */
    /* 3. Edge‑case / exception handling tests                               */
    /* ---------------------------------------------------------------------- */

    // ----- throwIfNull -----------------------------------------------------

    @Test
    public void testThrowIfNull_WithNull_ThrowsIllegalArgumentException() {
        String expectedMessage = "value cannot be null";
        try {
            Convention.throwIfNull(null, expectedMessage);
            Assert.fail("Expected IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    // ----- throwIfNullOrEmpty ------------------------------------------------

    @Test
    public void testThrowIfNullOrEmpty_WithNull_ThrowsIllegalArgumentException() {
        String expectedMessage = "string is required";
        try {
            Convention.throwIfNullOrEmpty(null, expectedMessage);
            Assert.fail("Expected IllegalArgumentException for null string");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    @Test
    public void testThrowIfNullOrEmpty_WithEmptyString_ThrowsIllegalArgumentException() {
        String expectedMessage = "empty string not allowed";
        try {
            Convention.throwIfNullOrEmpty("", expectedMessage);
            Assert.fail("Expected IllegalArgumentException for empty string");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    // ----- throwIfClassNotFound ---------------------------------------------

    @Test
    public void testThrowIfClassNotFound_WithNullClassName_ThrowsIAE() {
        String expectedMessage = "fully qualified ClassName cannot be null or empty";
        try {
            Convention.throwIfClassNotFound(null, this.getClass().getClassLoader());
            Assert.fail("Expected IllegalArgumentException for null class name");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    @Test
    public void testThrowIfClassNotFound_WithEmptyClassName_ThrowsIAE() {
        String expectedMessage = "fully qualified ClassName cannot be null or empty";
        try {
            Convention.throwIfClassNotFound("", this.getClass().getClassLoader());
            Assert.fail("Expected IllegalArgumentException for empty class name");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    @Test
    public void testThrowIfClassNotFound_WithNullLoader_ThrowsIAE() {
        String expectedMessage = "loader cannot be null";
        try {
            Convention.throwIfClassNotFound("java.lang.String", null);
            Assert.fail("Expected IllegalArgumentException for null ClassLoader");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    @Test
    public void testThrowIfClassNotFound_WithUnknownClass_ThrowsIAE() {
        String bogusClass = "com.cedarsoftware.util.DoesNotExist";
        ClassLoader loader = this.getClass().getClassLoader();
        try {
            Convention.throwIfClassNotFound(bogusClass, loader);
            Assert.fail("Expected IllegalArgumentException for unknown class");
        } catch (IllegalArgumentException e) {
            // Message must contain the fully‑qualified class name.
            Assert.assertTrue(e.getMessage().contains(bogusClass));
        }
    }

    // ----- throwIfKeyExists -------------------------------------------------

    @Test
    public void testThrowIfKeyExists_WithNullMap_ThrowsIAE() {
        String expectedMessage = "map cannot be null";
        try {
            Convention.throwIfKeyExists(null, "key", expectedMessage);
            Assert.fail("Expected IllegalArgumentException for null map");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    @Test
    public void testThrowIfKeyExists_WithNullKey_ThrowsIAE() {
        Map<String, Integer> map = new HashMap<>();
        String expectedMessage = "key cannot be null";
        try {
            Convention.throwIfKeyExists(map, null, expectedMessage);
            Assert.fail("Expected IllegalArgumentException for null key");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    @Test
    public void testThrowIfKeyExists_KeyAlreadyPresent_ThrowsIAE() {
        Map<String, Integer> map = new HashMap<>();
        map.put("existing", 42);
        String expectedMessage = "duplicate key";
        try {
            Convention.throwIfKeyExists(map, "existing", expectedMessage);
            Assert.fail("Expected IllegalArgumentException because key already exists");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    // ----- throwIfFalse -----------------------------------------------------

    @Test
    public void testThrowIfFalse_WithFalseLogic_ThrowsIAE() {
        String expectedMessage = "logic must be true";
        try {
            Convention.throwIfFalse(false, expectedMessage);
            Assert.fail("Expected IllegalArgumentException when logic is false");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }
}
