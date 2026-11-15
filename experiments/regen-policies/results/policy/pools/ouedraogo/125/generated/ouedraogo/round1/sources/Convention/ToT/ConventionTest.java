package com.cedarsoftware.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive JUnit‑4 test suite for {@link Convention}.
 *
 * Covers:
 *  • Normal (non‑exception) execution paths
 *  • All validation / error scenarios
 *  • Edge cases such as {@code null} and empty values
 *
 * The tests use only the public API of {@code Convention} and standard JDK classes.
 */
public class ConventionTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* --------------------------------------------------------------------- */
    /*  Tests for throwIfNull                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void throwIfNull_givenNonNull_doesNotThrow() {
        // No exception expected – the method should simply return.
        Convention.throwIfNull(new Object(), "object must not be null");
    }

    @Test
    public void throwIfNull_givenNull_throwsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("must be non‑null");
        Convention.throwIfNull(null, "must be non-null");
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for throwIfNullOrEmpty                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void throwIfNullOrEmpty_givenNonEmptyString_doesNotThrow() {
        Convention.throwIfNullOrEmpty("hello", "should not throw");
    }

    @Test
    public void throwIfNullOrEmpty_givenEmptyString_throwsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("empty not allowed");
        Convention.throwIfNullOrEmpty("", "empty not allowed");
    }

    @Test
    public void throwIfNullOrEmpty_givenNullString_throwsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("null not allowed");
        Convention.throwIfNullOrEmpty(null, "null not allowed");
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for throwIfClassNotFound                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void throwIfClassNotFound_withExistingClass_doesNotThrow() {
        // java.lang.String is guaranteed to exist.
        Convention.throwIfClassNotFound("java.lang.String", ClassLoader.getSystemClassLoader());
    }

    @Test
    public void throwIfClassNotFound_withNullClassName_throwsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("fully qualified ClassName cannot be null or empty");
        Convention.throwIfClassNotFound(null, ClassLoader.getSystemClassLoader());
    }

    @Test
    public void throwIfClassNotFound_withEmptyClassName_throwsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("fully qualified ClassName cannot be null or empty");
        Convention.throwIfClassNotFound("", ClassLoader.getSystemClassLoader());
    }

    @Test
    public void throwIfClassNotFound_withNullClassLoader_throwsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("loader cannot be null");
        Convention.throwIfClassNotFound("java.lang.String", null);
    }

    @Test
    public void throwIfClassNotFound_withUnknownClass_throwsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unknown class: com.nonexistent.Foo was not found.");
        Convention.throwIfClassNotFound("com.nonexistent.Foo", ClassLoader.getSystemClassLoader());
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for throwIfKeyExists                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void throwIfKeyExists_keyAbsent_doesNotThrow() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        Convention.throwIfKeyExists(map, "two", "key already present");
    }

    @Test
    public void throwIfKeyExists_keyPresent_throwsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("duplicate");
        Map<String, Integer> map = new HashMap<>();
        map.put("key", 42);
        Convention.throwIfKeyExists(map, "key", "duplicate");
    }

    @Test
    public void throwIfKeyExists_nullMap_throwsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("map cannot be null");
        Convention.throwIfKeyExists(null, "any", "should not matter");
    }

    @Test
    public void throwIfKeyExists_nullKey_throwsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("key cannot be null");
        Map<String, Integer> map = new HashMap<>();
        map.put("present", 1);
        Convention.throwIfKeyExists(map, null, "key is null");
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for throwIfFalse                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void throwIfFalse_givenTrue_doesNotThrow() {
        Convention.throwIfFalse(true, "should not be thrown");
    }

    @Test
    public void throwIfFalse_givenFalse_throwsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("must be true");
        Convention.throwIfFalse(false, "must be true");
    }
}
