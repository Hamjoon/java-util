package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

public class VoidConversionsTest {

    /**
     * List of methods under test (package‑private static):
     *  - static Object toNull(Object from, Converter converter)
     *  - static Boolean toBoolean(Object from, Converter converter)
     *  - static Character toCharacter(Object from, Converter converter)
     */

    // -----------------------------------------------------------------
    // Basic functionality tests – the methods ignore their inputs and
    // always return the same constant value.
    // -----------------------------------------------------------------

    @Test
    public void testToNullReturnsNull() {
        // typical non‑null input
        Object result = VoidConversions.toNull("any string", null);
        assertNull("toNull should always return null", result);

        // edge case – null as the source object
        result = VoidConversions.toNull(null, null);
        assertNull("toNull should return null even when the source is null", result);
    }

    @Test
    public void testToBooleanAlwaysFalse() {
        // typical input
        Boolean result = VoidConversions.toBoolean(123, null);
        assertSame("toBoolean should always return Boolean.FALSE (same instance)", Boolean.FALSE, result);

        // edge case – null source
        result = VoidConversions.toBoolean(null, null);
        assertSame("toBoolean should return Boolean.FALSE even when source is null", Boolean.FALSE, result);
    }

    @Test
    public void testToCharacterAlwaysMinValue() {
        // typical input
        Character result = VoidConversions.toCharacter(new Object(), null);
        assertEquals("toCharacter should always return Character.MIN_VALUE", Character.valueOf(Character.MIN_VALUE), result);

        // edge case – null source
        result = VoidConversions.toCharacter(null, null);
        assertEquals("toCharacter should return Character.MIN_VALUE even when source is null", Character.valueOf(Character.MIN_VALUE), result);
    }

    // -----------------------------------------------------------------
    // Additional edge‑case tests – different types for the `from` argument.
    // The implementation does not inspect the argument, but the tests
    // confirm that no unexpected exceptions are thrown for any object.
    // -----------------------------------------------------------------

    @Test
    public void testToBooleanWithVariousObjectTypes() {
        Object[] samples = {
                "string",
                0,
                1L,
                3.14,
                true,
                false,
                new int[]{1,2,3},
                new java.util.Date()
        };

        for (Object sample : samples) {
            Boolean result = VoidConversions.toBoolean(sample, null);
            assertSame("toBoolean must return Boolean.FALSE for any input type", Boolean.FALSE, result);
        }
    }

    @Test
    public void testToCharacterWithVariousObjectTypes() {
        Object[] samples = {
                "string",
                42,
                0L,
                2.718,
                true,
                new java.util.ArrayList<>(),
                new java.util.HashMap<>()
        };

        for (Object sample : samples) {
            Character result = VoidConversions.toCharacter(sample, null);
            assertEquals("toCharacter must return Character.MIN_VALUE for any input type",
                    Character.valueOf(Character.MIN_VALUE), result);
        }
    }

    @Test
    public void testToNullWithVariousObjectTypes() {
        Object[] samples = {
                "string",
                99,
                0L,
                1.618,
                false,
                new java.util.LinkedList<>(),
                new java.util.HashSet<>()
        };

        for (Object sample : samples) {
            Object result = VoidConversions.toNull(sample, null);
            assertNull("toNull must return null for any input type", result);
        }
    }

    // -----------------------------------------------------------------
    // Exception handling – the current implementation never throws,
    // but we verify that passing a non‑null Converter does not cause
    // any failure.  (The Converter class is part of the public API and
    // can be instantiated with a null options object for this purpose.)
    // -----------------------------------------------------------------

    @Test
    public void testMethodsWithNonNullConverterDoNotThrow() {
        // Construct a Converter with a null options object – allowed for test purposes
        Converter converter = new Converter(null);

        // All calls should succeed and return the expected constant values
        assertNull(VoidConversions.toNull("x", converter));
        assertSame(Boolean.FALSE, VoidConversions.toBoolean("x", converter));
        assertEquals(Character.valueOf(Character.MIN_VALUE), VoidConversions.toCharacter("x", converter));
    }
}
