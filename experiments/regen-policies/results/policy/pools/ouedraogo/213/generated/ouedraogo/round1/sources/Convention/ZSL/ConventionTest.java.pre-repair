package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * JUnit 4 test suite that exercises every public static method of
 * {@link Convention}.  The tests verify both the “happy‑path” (no exception
 * thrown) and the error‑path (the correct {@link IllegalArgumentException}
 * is thrown with the expected message).
 *
 * The tests deliberately avoid any external mocking frameworks as required
 * by the task – they use only real objects constructed with the public API
 * of the classes that are part of the same library.
 */
public class ConventionTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* --------------------------------------------------------------
     *  Private constructor
     * -------------------------------------------------------------- */
    @Test
    public void testPrivateConstructorIsNotAccessible() throws Exception {
        Constructor<Convention> ctor = Convention.class.getDeclaredConstructor();
        // The constructor must be private
        assert Modifier.isPrivate(ctor.getModifiers()) :
                "Convention constructor should be private";

        // Make it accessible just to be able to instantiate (coverage)
        ctor.setAccessible(true);
        Object instance = ctor.newInstance();
        // The class is designed as a static utility – the instance should be null
        // because the constructor does nothing.  We only care that it can be
        // invoked without throwing an unexpected exception.
        assert instance != null;
    }

    /* --------------------------------------------------------------
     *  throwIfNull
     * -------------------------------------------------------------- */
    @Test
    public void testThrowIfNull_WithNonNullDoesNotThrow() {
        // No exception expected
        Convention.throwIfNull(new Object(), "should not throw");
    }

    @Test
    public void testThrowIfNull_WithNullThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("null value not allowed");
        Convention.throwIfNull(null, "null value not allowed");
    }

    /* --------------------------------------------------------------
     *  throwIfNullOrEmpty
     * -------------------------------------------------------------- */
    @Test
    public void testThrowIfNullOrEmpty_WithValidStringDoesNotThrow() {
        Convention.throwIfNullOrEmpty("hello", "must not be empty");
    }

    @Test
    public void testThrowIfNullOrEmpty_WithNullThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("null string");
        Convention.throwIfNullOrEmpty(null, "null string");
    }

    @Test
    public void testThrowIfNullOrEmpty_WithEmptyStringThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("empty string");
        Convention.throwIfNullOrEmpty("", "empty string");
    }

    /* --------------------------------------------------------------
     *  throwIfClassNotFound
     * -------------------------------------------------------------- */
    @Test
    public void testThrowIfClassNotFound_WithExistingClassDoesNotThrow() {
        // java.lang.String is guaranteed to be present
        String className = "java.lang.String";
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Convention.throwIfClassNotFound(className, loader);
    }

    @Test
    public void testThrowIfClassNotFound_WithMissingClassThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unknown class: com.cedarsoftware.util.DoesNotExist was not found.");
        String missing = "com.cedarsoftware.util.DoesNotExist";
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Convention.throwIfClassNotFound(missing, loader);
    }

    @Test
    public void testThrowIfClassNotFound_WithNullClassNameThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("fully qualified ClassName cannot be null or empty");
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Convention.throwIfClassNotFound(null, loader);
    }

    @Test
    public void testThrowIfClassNotFound_WithNullLoaderThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("loader cannot be null");
        Convention.throwIfClassNotFound("java.lang.String", null);
    }

    /* --------------------------------------------------------------
     *  throwIfKeyExists
     * -------------------------------------------------------------- */
    @Test
    public void testThrowIfKeyExists_KeyAbsentDoesNotThrow() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        Convention.throwIfKeyExists(map, "two", "key already present");
    }

    @Test
    public void testThrowIfKeyExists_KeyPresentThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("duplicate key");
        Map<String, Integer> map = new HashMap<>();
        map.put("key", 42);
        Convention.throwIfKeyExists(map, "key", "duplicate key");
    }

    @Test
    public void testThrowIfKeyExists_NullMapThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("map cannot be null");
        Convention.throwIfKeyExists(null, "any", "won't matter");
    }

    @Test
    public void testThrowIfKeyExists_NullKeyThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("key cannot be null");
        Map<String, Integer> map = new HashMap<>();
        Convention.throwIfKeyExists(map, null, "won't matter");
    }

    /* --------------------------------------------------------------
     *  throwIfFalse
     * -------------------------------------------------------------- */
    @Test
    public void testThrowIfFalse_WithTrueDoesNotThrow() {
        Convention.throwIfFalse(true, "should not throw");
    }

    @Test
    public void testThrowIfFalse_WithFalseThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("logic failed");
        Convention.throwIfFalse(false, "logic failed");
    }
}
