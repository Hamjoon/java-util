package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

/**
 * Unit tests for {@link EnumConversions}.
 * The tests are placed in the same package to allow access to package‑private static methods.
 */
public class EnumConversionsTest {

    /** Simple enum used for all test scenarios. */
    private enum Color {
        RED, GREEN, BLUE, YELLOW
    }

    /** Helper to obtain a dummy {@link Converter} instance – the converter is not used by EnumConversions. */
    private static Converter dummyConverter() {
        // Converter requires a ConverterOptions argument; passing null is acceptable for these tests
        return new Converter(null);
    }

    /* -------------------------------------------------
     *  Tests for toMap()
     * ------------------------------------------------- */

    @Test
    public void testToMapReturnsMapWithName() {
        Map<String,Object> result = EnumConversions.toMap(Color.GREEN, dummyConverter());

        assertNotNull("Resulting map must not be null", result);
        assertEquals("Map should contain exactly one entry", 1, result.size());
        assertTrue("Map must contain key \"name\"", result.containsKey("name"));
        assertEquals("Value for key \"name\" must be the enum name", "GREEN", result.get("name"));
    }

    /* -------------------------------------------------
     *  Tests for toEnumSet()
     * ------------------------------------------------- */

    @Test
    public void testToEnumSetFromCollectionMixedTypes() {
        List<Object> source = Arrays.asList(
                Color.RED,               // enum instance
                "GREEN",                 // name as String
                2,                       // ordinal (BLUE)
                null,                    // null should be ignored
                Color.YELLOW             // another enum instance
        );

        EnumSet<Color> result = EnumConversions.toEnumSet(source, dummyConverter(), Color.class);

        // Expected set contains RED, GREEN, BLUE (ordinal 2), YELLOW
        EnumSet<Color> expected = EnumSet.of(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW);
        assertEquals("EnumSet should contain all converted values", expected, result);
    }

    @Test
    public void testToEnumSetFromArrayMixedTypes() {
        Object[] source = new Object[] {
                "RED",
                1,               // ordinal -> GREEN
                Color.BLUE,
                null,
                3                // ordinal -> YELLOW
        };

        EnumSet<Color> result = EnumConversions.toEnumSet(source, dummyConverter(), Color.class);

        EnumSet<Color> expected = EnumSet.of(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW);
        assertEquals("EnumSet created from array must match expected values", expected, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToEnumSetWithNonEnumTargetThrows() {
        List<String> source = Collections.singletonList("RED");
        // Passing String.class (not an enum) must trigger IllegalArgumentException
        EnumConversions.toEnumSet(source, dummyConverter(), String.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToEnumSetWithInvalidSourceTypeThrows() {
        // Source is neither Collection nor array
        EnumConversions.toEnumSet("RED", dummyConverter(), Color.class);
    }

    @Test
    public void testToEnumSetNumericOrdinalOutOfRange() {
        List<Integer> source = Arrays.asList(0, 4); // 4 is out of range (valid 0‑3)

        try {
            EnumConversions.toEnumSet(source, dummyConverter(), Color.class);
            fail("Expected IllegalArgumentException for out‑of‑range ordinal");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Invalid ordinal value 4"));
        }
    }

    @Test
    public void testToEnumSetUnsupportedElementTypeThrows() {
        List<Object> source = Arrays.asList(new Date()); // Date is not convertible

        try {
            EnumConversions.toEnumSet(source, dummyConverter(), Color.class);
            fail("Expected IllegalArgumentException for unsupported element type");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(Date.class.getName()));
            assertTrue(e.getMessage().contains(Color.class.getName()));
        }
    }

    /* -------------------------------------------------
     *  Direct tests for the private helper methods via reflection
     *  (only needed for exhaustive coverage of processElement logic)
     * ------------------------------------------------- */

    @Test
    public void testProcessElementStringConversion() throws Exception {
        EnumSet<Color> set = EnumSet.noneOf(Color.class);
        // Access the private static method
        java.lang.reflect.Method m = EnumConversions.class
                .getDeclaredMethod("processElement", Object.class, EnumSet.class, Class.class, Object[].class);
        m.setAccessible(true);

        // Invoke with a String that matches an enum constant
        m.invoke(null, "BLUE", set, Color.class, null);
        assertTrue(set.contains(Color.BLUE));
    }

    @Test
    public void testProcessElementEnumInstanceConversion() throws Exception {
        EnumSet<Color> set = EnumSet.noneOf(Color.class);
        java.lang.reflect.Method m = EnumConversions.class
                .getDeclaredMethod("processElement", Object.class, EnumSet.class, Class.class, Object[].class);
        m.setAccessible(true);

        m.invoke(null, Color.YELLOW, set, Color.class, null);
        assertTrue(set.contains(Color.YELLOW));
    }

    @Test
    public void testProcessElementNumberConversion() throws Exception {
        EnumSet<Color> set = EnumSet.noneOf(Color.class);
        java.lang.reflect.Method m = EnumConversions.class
                .getDeclaredMethod("processElement", Object.class, EnumSet.class, Class.class, Object[].class);
        m.setAccessible(true);

        // ordinal 1 -> GREEN
        m.invoke(null, Integer.valueOf(1), set, Color.class, null);
        assertTrue(set.contains(Color.GREEN));
    }

    /* -------------------------------------------------
     *  Edge cases for null elements – they should be ignored
     * ------------------------------------------------- */

    @Test
    public void testArrayWithAllNullElementsReturnsEmptySet() {
        Object[] source = new Object[] { null, null };
        EnumSet<Color> result = EnumConversions.toEnumSet(source, dummyConverter(), Color.class);
        assertTrue("Resulting EnumSet should be empty when source contains only nulls", result.isEmpty());
    }

    @Test
    public void testCollectionWithAllNullElementsReturnsEmptySet() {
        List<Object> source = Arrays.asList(null, null, null);
        EnumSet<Color> result = EnumConversions.toEnumSet(source, dummyConverter(), Color.class);
        assertTrue("Resulting EnumSet should be empty when collection contains only nulls", result.isEmpty());
    }
}
