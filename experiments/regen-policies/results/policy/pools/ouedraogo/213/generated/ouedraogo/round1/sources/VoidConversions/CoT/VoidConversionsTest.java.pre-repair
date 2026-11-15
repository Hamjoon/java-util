package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test suite for {@link VoidConversions}. All methods are package‑private static,
 * therefore the test class resides in the same package to gain access.
 *
 * The class under test deliberately ignores its inputs and always returns a constant:
 * <ul>
 *   <li>{@code toNull}      → {@code null}</li>
 *   <li>{@code toBoolean}   → {@code Boolean.FALSE}</li>
 *   <li>{@code toCharacter} → {@code Character.MIN_VALUE}</li>
 * </ul>
 *
 * The tests are organised in three sections:
 * <ol>
 *   <li>Extraction of public (exposed) method signatures.</li>
 *   <li>Basic functional tests – verify the constant return values.</li>
 *   <li>Edge‑case tests – pass {@code null}, unusual objects and a {@code null}
 *       {@link Converter} to confirm that no exception is thrown and the
 *       contract is still honoured.</li>
 * </ol>
 */
public class VoidConversionsTest {

    /* ----------------------------------------------------------------------
     *  1.  Public (exposed) method signatures extracted from VoidConversions
     * ---------------------------------------------------------------------- */
    // The class defines three package‑private static methods:
    //   static Object    toNull(Object from, Converter converter)
    //   static Boolean   toBoolean(Object from, Converter converter)
    //   static Character toCharacter(Object from, Converter converter)

    /* ----------------------------------------------------------------------
     *  2.  Basic functional tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testToNullReturnsNull() {
        Object result = VoidConversions.toNull("anyString", null);
        assertNull("toNull should always return null", result);
    }

    @Test
    public void testToBooleanAlwaysFalse() {
        Boolean result = VoidConversions.toBoolean(Boolean.TRUE, null);
        assertEquals("toBoolean should always return Boolean.FALSE", Boolean.FALSE, result);
    }

    @Test
    public void testToCharacterAlwaysMinValue() {
        Character result = VoidConversions.toCharacter(123, null);
        assertEquals("toCharacter should always return Character.MIN_VALUE",
                Character.valueOf(Character.MIN_VALUE), result);
    }

    /* ----------------------------------------------------------------------
     *  3.  Edge‑case and exception‑handling tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testToNullWithNullFromAndNullConverter() {
        // Both parameters are null – method must still return null without NPE.
        Object result = VoidConversions.toNull(null, null);
        assertNull("toNull must return null even when both arguments are null", result);
    }

    @Test
    public void testToNullWithComplexObject() {
        // Pass an arbitrary complex object; result must still be null.
        Object complex = new java.util.HashMap<String, Integer>();
        complex.put("key", 42);
        Object result = VoidConversions.toNull(complex, null);
        assertNull("toNull must ignore complex objects and return null", result);
    }

    @Test
    public void testToBooleanWithVariousFromValues() {
        Object[] testValues = {
                null,
                Boolean.TRUE,
                Boolean.FALSE,
                "true",
                1,
                new Object(),
                new int[]{1,2,3}
        };
        for (Object val : testValues) {
            Boolean result = VoidConversions.toBoolean(val, null);
            assertEquals("toBoolean must always return Boolean.FALSE for input: " + val,
                    Boolean.FALSE, result);
        }
    }

    @Test
    public void testToBooleanWithNullConverter() {
        // Ensure a null Converter does not cause any failure.
        Boolean result = VoidConversions.toBoolean("anything", null);
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    public void testToCharacterWithVariousFromValues() {
        Object[] testValues = {
                null,
                'A',
                "string",
                12345,
                new Object(),
                new char[]{'x', 'y'}
        };
        for (Object val : testValues) {
            Character result = VoidConversions.toCharacter(val, null);
            assertEquals("toCharacter must always return Character.MIN_VALUE for input: " + val,
                    Character.valueOf(Character.MIN_VALUE), result);
        }
    }

    @Test
    public void testToCharacterWithNullConverter() {
        // Ensure a null Converter does not cause any failure.
        Character result = VoidConversions.toCharacter("anything", null);
        assertEquals(Character.valueOf(Character.MIN_VALUE), result);
    }

    @Test
    public void testAllMethodsWithNonNullConverterInstance() {
        // Build a real Converter instance (options may be null – the class does not use them).
        Converter converter = new Converter(null);

        // toNull
        assertNull("toNull with real Converter should still return null",
                VoidConversions.toNull("foo", converter));

        // toBoolean
        assertEquals("toBoolean with real Converter should still return FALSE",
                Boolean.FALSE,
                VoidConversions.toBoolean("foo", converter));

        // toCharacter
        assertEquals("toCharacter with real Converter should still return MIN_VALUE",
                Character.valueOf(Character.MIN_VALUE),
                VoidConversions.toCharacter("foo", converter));
    }
}
