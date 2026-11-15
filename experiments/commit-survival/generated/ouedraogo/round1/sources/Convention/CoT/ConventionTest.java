package com.cedarsoftware.util;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for {@link Convention}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal behaviour (no exception thrown)</li>
 *   <li>Edge cases and expected {@link IllegalArgumentException}s</li>
 * </ul>
 *
 * The test class follows the steps requested:
 * <ol>
 *   <li>List of public methods (see comments above each test group)</li>
 *   <li>Basic test cases for each method</li>
 *   <li>Identification of edge‑case / exception scenarios</li>
 *   <li>Specific tests for those scenarios</li>
 *   <li>All tests merged into a single JUnit‑4 test file</li>
 * </ol>
 */
public class ConventionTest {

    /* ----------------------------------------------------------------------
     *  Public methods of Convention (extracted)
     * ----------------------------------------------------------------------
     *
     * 1. public static void throwIfNull(Object value, String message)
     * 2. public static void throwIfNullOrEmpty(String value, String message)
     * 3. public static void throwIfClassNotFound(String fullyQualifiedClassName,
     *                                            ClassLoader loader)
     * 4. public static <K,V> void throwIfKeyExists(Map<K,V> map,
     *                                            K key,
     *                                            String message)
     * 5. public static void throwIfFalse(boolean logic, String message)
     *
     */

    /* ----------------------------------------------------------------------
     *  1. Tests for throwIfNull(...)
     * ---------------------------------------------------------------------- */

    @Test
    public void testThrowIfNull_WithNonNullValue_DoesNotThrow() {
        // basic positive case – no exception expected
        Convention.throwIfNull(new Object(), "value must not be null");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfNull_WithNullValue_ThrowsException() {
        Convention.throwIfNull(null, "value must not be null");
    }

    /* ----------------------------------------------------------------------
     *  2. Tests for throwIfNullOrEmpty(...)
     * ---------------------------------------------------------------------- */

    @Test
    public void testThrowIfNullOrEmpty_WithNonEmptyString_DoesNotThrow() {
        Convention.throwIfNullOrEmpty("hello", "string must not be null/empty");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfNullOrEmpty_WithNullString_ThrowsException() {
        Convention.throwIfNullOrEmpty(null, "string must not be null/empty");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfNullOrEmpty_WithEmptyString_ThrowsException() {
        Convention.throwIfNullOrEmpty("", "string must not be null/empty");
    }

    /* ----------------------------------------------------------------------
     *  3. Tests for throwIfClassNotFound(...)
     * ---------------------------------------------------------------------- */

    @Test
    public void testThrowIfClassNotFound_WithExistingClass_DoesNotThrow() {
        // Use a class that is guaranteed to exist – this class itself.
        String existingClass = Convention.class.getName();
        ClassLoader loader = Convention.class.getClassLoader();
        Convention.throwIfClassNotFound(existingClass, loader);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfClassNotFound_WithNullClassName_ThrowsException() {
        Convention.throwIfClassNotFound(null, Convention.class.getClassLoader());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfClassNotFound_WithEmptyClassName_ThrowsException() {
        Convention.throwIfClassNotFound("", Convention.class.getClassLoader());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfClassNotFound_WithNullLoader_ThrowsException() {
        // class name is valid, but loader is null
        Convention.throwIfClassNotFound(Convention.class.getName(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfClassNotFound_WithUnknownClass_ThrowsException() {
        // Choose a class name that almost certainly does not exist.
        String bogusClass = "com.cedarsoftware.util.DoesNotExist12345";
        Convention.throwIfClassNotFound(bogusClass, Convention.class.getClassLoader());
    }

    /* ----------------------------------------------------------------------
     *  4. Tests for throwIfKeyExists(...)
     * ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfKeyExists_WithNullMap_ThrowsException() {
        Map<String, String> map = null;
        Convention.throwIfKeyExists(map, "key", "map cannot be null");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfKeyExists_WithNullKey_ThrowsException() {
        Map<String, String> map = new HashMap<>();
        map.put("existing", "value");
        Convention.throwIfKeyExists(map, null, "key cannot be null");
    }

    @Test
    public void testThrowIfKeyExists_KeyNotPresent_DoesNotThrow() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        // key "three" is not present – no exception expected
        Convention.throwIfKeyExists(map, "three", "key already exists");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfKeyExists_KeyPresent_ThrowsException() {
        Map<String, Integer> map = new HashMap<>();
        map.put("alpha", 10);
        // key "alpha" already exists – should trigger exception
        Convention.throwIfKeyExists(map, "alpha", "key already exists");
    }

    /* ----------------------------------------------------------------------
     *  5. Tests for throwIfFalse(...)
     * ---------------------------------------------------------------------- */

    @Test
    public void testThrowIfFalse_WithTrueLogic_DoesNotThrow() {
        Convention.throwIfFalse(true, "logic must be true");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfFalse_WithFalseLogic_ThrowsException() {
        Convention.throwIfFalse(false, "logic must be true");
    }
}
