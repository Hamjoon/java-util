package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;

/**
 * JUnit‑4 test suite for {@link Convention}.
 *
 * The tests are organised in four sections that follow the steps
 * described in the task:
 *
 * 1. **Public method extraction** – a comment that lists every public static
 *    method together with its full signature.
 *
 * 2. **Basic functionality tests** – one straightforward test per method that
 *    verifies the normal (non‑exceptional) behaviour.
 *
 * 3. **Edge‑case / exception analysis** – a comment that explains the
 *    boundary conditions and error paths that each method can hit.
 *
 * 4. **Edge‑case tests** – JUnit tests that deliberately trigger the
 *    identified edge cases and assert that the correct {@link IllegalArgumentException}
 *    (or other) is thrown.
 *
 * All tests are written using only the classes, constructors and methods that
 * appear in the supplied source code (or the small helper classes mentioned
 * in the dependency list). No mocking frameworks are used.
 */
public class ConventionTest {

    /* ---------------------------------------------------------------------- */
    /* 1. PUBLIC METHODS (extracted from Convention)                           */
    /* ---------------------------------------------------------------------- */
    // private Convention()                           // constructor – not public
    // public static void throwIfNull(Object value, String message)
    // public static void throwIfNullOrEmpty(String value, String message)
    // public static void throwIfClassNotFound(String fullyQualifiedClassName, ClassLoader loader)
    // public static <K,V> void throwIfKeyExists(Map<K,V> map, K key, String message)
    // public static void throwIfFalse(boolean logic, String message)

    /* ---------------------------------------------------------------------- */
    /* 2. BASIC FUNCTIONALITY TESTS                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testThrowIfNull_WithNonNullDoesNotThrow() {
        // Normal case – value is non‑null, no exception expected.
        Convention.throwIfNull(new Object(), "should not fail");
    }

    @Test
    public void testThrowIfNullOrEmpty_WithNonEmptyStringDoesNotThrow() {
        Convention.throwIfNullOrEmpty("hello", "should not fail");
    }

    @Test
    public void testThrowIfClassNotFound_WithExistingClassDoesNotThrow() {
        // Using the current test class as a known existing class.
        Convention.throwIfClassNotFound(
                this.getClass().getName(),
                this.getClass().getClassLoader());
    }

    @Test
    public void testThrowIfKeyExists_KeyAbsentDoesNotThrow() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Convention.throwIfKeyExists(map, "b", "key already present");
    }

    @Test
    public void testThrowIfFalse_WithTrueDoesNotThrow() {
        Convention.throwIfFalse(true, "should not fail");
    }

    /* ---------------------------------------------------------------------- */
    /* 3. EDGE‑CASE & EXCEPTION SCENARIOS                                      */
    /* ---------------------------------------------------------------------- */
    // throwIfNull
    //   • value == null  → IllegalArgumentException with supplied message.
    //   • value != null  → no exception.
    //
    // throwIfNullOrEmpty
    //   • value == null          → IllegalArgumentException.
    //   • value == "" (empty)    → IllegalArgumentException.
    //   • value contains chars   → no exception.
    //
    // throwIfClassNotFound
    //   • fullyQualifiedClassName == null or "" → IllegalArgumentException
    //   • loader == null                         → IllegalArgumentException
    //   • class name not loadable                → IllegalArgumentException
    //   • class name loadable                    → no exception.
    //
    // throwIfKeyExists
    //   • map == null        → IllegalArgumentException ("map cannot be null")
    //   • key == null        → IllegalArgumentException ("key cannot be null")
    //   • map contains key   → IllegalArgumentException with supplied message
    //   • map does not contain key → no exception.
    //
    // throwIfFalse
    //   • logic == false → IllegalArgumentException with supplied message
    //   • logic == true  → no exception.

    /* ---------------------------------------------------------------------- */
    /* 4. EDGE‑CASE / EXCEPTION TESTS                                          */
    /* ---------------------------------------------------------------------- */

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    // ---------- throwIfNull ----------
    @Test
    public void testThrowIfNull_WithNullThrows() {
        String msg = "value must not be null";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(msg);
        Convention.throwIfNull(null, msg);
    }

    // ---------- throwIfNullOrEmpty ----------
    @Test
    public void testThrowIfNullOrEmpty_WithNullThrows() {
        String msg = "string cannot be null or empty";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(msg);
        Convention.throwIfNullOrEmpty(null, msg);
    }

    @Test
    public void testThrowIfNullOrEmpty_WithEmptyStringThrows() {
        String msg = "empty not allowed";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(msg);
        Convention.throwIfNullOrEmpty("", msg);
    }

    // ---------- throwIfClassNotFound ----------
    @Test
    public void testThrowIfClassNotFound_NullClassNameThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("fully qualified ClassName cannot be null or empty");
        Convention.throwIfClassNotFound(null, this.getClass().getClassLoader());
    }

    @Test
    public void testThrowIfClassNotFound_EmptyClassNameThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("fully qualified ClassName cannot be null or empty");
        Convention.throwIfClassNotFound("", this.getClass().getClassLoader());
    }

    @Test
    public void testThrowIfClassNotFound_NullClassLoaderThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("loader cannot be null");
        Convention.throwIfClassNotFound("java.lang.String", null);
    }

    @Test
    public void testThrowIfClassNotFound_NonExistingClassThrows() {
        String bogus = "com.cedarsoftware.util.DoesNotExist12345";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unknown class: " + bogus + " was not found.");
        Convention.throwIfClassNotFound(bogus, this.getClass().getClassLoader());
    }

    // ---------- throwIfKeyExists ----------
    @Test
    public void testThrowIfKeyExists_NullMapThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("map cannot be null");
        Convention.throwIfKeyExists(null, "key", "should not happen");
    }

    @Test
    public void testThrowIfKeyExists_NullKeyThrows() {
        Map<String, Integer> map = new HashMap<>();
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("key cannot be null");
        Convention.throwIfKeyExists(map, null, "should not happen");
    }

    @Test
    public void testThrowIfKeyExists_KeyAlreadyPresentThrows() {
        Map<String, Integer> map = new HashMap<>();
        map.put("exists", 10);
        String msg = "duplicate key";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(msg);
        Convention.throwIfKeyExists(map, "exists", msg);
    }

    // ---------- throwIfFalse ----------
    @Test
    public void testThrowIfFalse_WithFalseThrows() {
        String msg = "logic failed";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(msg);
        Convention.throwIfFalse(false, msg);
    }

    // Additional sanity test – true does not throw (already covered in basic tests)
}
