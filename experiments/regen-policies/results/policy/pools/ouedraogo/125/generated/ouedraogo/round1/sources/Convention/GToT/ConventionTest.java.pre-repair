package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;

/**
 * JUnit 4 test cases for {@link Convention}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal (happy‑path) usage of each public static method.</li>
 *   <li>Edge‑case and exception scenarios identified from the source code.</li>
 * </ul>
 *
 * All tests are written using only the classes, constructors and methods that appear
 * in the supplied source code or its listed dependencies.
 */
public class ConventionTest {

    /** JUnit rule to verify that an exception with a specific message is thrown. */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* ---------------------------------------------------------------------- */
    /*  Tests for throwIfNull(Object, String)                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testThrowIfNull_WithNonNullValue_DoesNotThrow() {
        // Normal case – value is non‑null, no exception expected.
        Convention.throwIfNull(new Object(), "value must not be null");
    }

    @Test
    public void testThrowIfNull_WithNullValue_ThrowsIllegalArgumentException() {
        // Edge case – value is null, should throw with the supplied message.
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("value cannot be null");
        Convention.throwIfNull(null, "value cannot be null");
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for throwIfNullOrEmpty(String, String)                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testThrowIfNullOrEmpty_WithValidString_DoesNotThrow() {
        Convention.throwIfNullOrEmpty("hello", "string must not be empty");
    }

    @Test
    public void testThrowIfNullOrEmpty_WithNullString_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("null not allowed");
        Convention.throwIfNullOrEmpty(null, "null not allowed");
    }

    @Test
    public void testThrowIfNullOrEmpty_WithEmptyString_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("empty not allowed");
        Convention.throwIfNullOrEmpty("", "empty not allowed");
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for throwIfClassNotFound(String, ClassLoader)                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testThrowIfClassNotFound_WithExistingClass_DoesNotThrow() {
        // Using the current class loader and a known class name.
        Convention.throwIfClassNotFound(
                "com.cedarsoftware.util.Convention",
                this.getClass().getClassLoader());
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
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unknown class: com.nonexistent.Foo was not found.");
        Convention.throwIfClassNotFound("com.nonexistent.Foo", this.getClass().getClassLoader());
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for throwIfKeyExists(Map<K,V>, K, String)                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testThrowIfKeyExists_KeyAbsent_DoesNotThrow() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Convention.throwIfKeyExists(map, "b", "key already present");
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
        map.put("x", 1);
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("key cannot be null");
        Convention.throwIfKeyExists(map, null, "should not matter");
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for throwIfFalse(boolean, String)                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testThrowIfFalse_WithTrueLogic_DoesNotThrow() {
        Convention.throwIfFalse(true, "should not fail");
    }

    @Test
    public void testThrowIfFalse_WithFalseLogic_ThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("logic failed");
        Convention.throwIfFalse(false, "logic failed");
    }
}
