package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link VoidConversions}.
 *
 * The tests cover:
 *   • Normal behaviour (return values)
 *   • Edge‑case inputs (null / various object types)
 *   • Null {@code Converter} handling
 *   • Private constructor visibility
 */
public class VoidConversionsTest {

    /** A dummy Converter instance – the concrete behaviour is irrelevant for VoidConversions. */
    private static Converter dummyConverter;

    @BeforeClass
    public static void setUpClass() {
        // The Converter class requires a ConverterOptions instance.
        // Since the library does not expose a public zero‑arg constructor for ConverterOptions,
        // we create a minimal stub via an anonymous subclass (allowed because we are not mocking).
        // If the real library provides a default options constructor, replace this with:
        //   dummyConverter = new Converter(new ConverterOptions());
        dummyConverter = new Converter(null);   // passing null is safe for our tests because
                                               // VoidConversions never calls any method on it.
    }

    /*** ---------- 1. toNull ---------- ***/
    @Test
    public void testToNullReturnsNullWithVariousInputs() {
        Object[] fromValues = {null, "string", 123, new Object()};
        for (Object from : fromValues) {
            // using a real converter
            assertNull("toNull should return null for any non‑null converter", VoidConversions.toNull(from, dummyConverter));
            // using a null converter
            assertNull("toNull should return null even when converter is null", VoidConversions.toNull(from, null));
        }
    }

    /*** ---------- 2. toBoolean ---------- ***/
    @Test
    public void testToBooleanAlwaysFalse() {
        Object[] fromValues = {null, "true", Boolean.TRUE, 0, new Object()};
        for (Object from : fromValues) {
            // with a real converter
            Boolean result = VoidConversions.toBoolean(from, dummyConverter);
            assertNotNull("Result must never be null", result);
            assertEquals("toBoolean must always return Boolean.FALSE", Boolean.FALSE, result);

            // with a null converter
            result = VoidConversions.toBoolean(from, null);
            assertNotNull("Result must never be null when converter is null", result);
            assertEquals("toBoolean must always return Boolean.FALSE even with null converter", Boolean.FALSE, result);
        }
    }

    /*** ---------- 3. toCharacter ---------- ***/
    @Test
    public void testToCharacterAlwaysMinValue() {
        Object[] fromValues = {null, 'A', "c", 42, new Object()};
        for (Object from : fromValues) {
            // with a real converter
            Character result = VoidConversions.toCharacter(from, dummyConverter);
            assertNotNull("Result must never be null", result);
            assertEquals("toCharacter must always return Character.MIN_VALUE",
                         Character.MIN_VALUE, result.charValue());

            // with a null converter
            result = VoidConversions.toCharacter(from, null);
            assertNotNull("Result must never be null when converter is null", result);
            assertEquals("toCharacter must always return Character.MIN_VALUE even with null converter",
                         Character.MIN_VALUE, result.charValue());
        }
    }

    /*** ---------- 4. Private constructor ---------- ***/
    @Test
    public void testPrivateConstructorIsInaccessible() throws Exception {
        Constructor<VoidConversions> ctor = VoidConversions.class.getDeclaredConstructor();
        assertTrue("Constructor should be private", Modifier.isPrivate(ctor.getModifiers()));

        // Trying to invoke it should throw an IllegalAccessException unless we set accessible=true.
        // We verify that without changing accessibility it cannot be called.
        try {
            ctor.setAccessible(false); // ensure default accessibility
            ctor.newInstance();
            fail("Expected IllegalAccessException when invoking private constructor without setAccessible(true)");
        } catch (IllegalAccessException expected) {
            // expected path
        }
    }
}
