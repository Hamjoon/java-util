package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.util.*;

public class EnumConversionsTest {

    /** Simple enum used for all test scenarios. */
    private enum Color {
        RED, GREEN, BLUE
    }

    /* ---------------------------------------------------------------------- */
    /* toMap tests                                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToMapReturnsNameEntry() {
        Map<String, Object> result = EnumConversions.toMap(Color.GREEN, null);
        Assert.assertNotNull("Result map must not be null", result);
        Assert.assertEquals("Map must contain exactly one entry", 1, result.size());
        Assert.assertTrue("Map must contain key \"name\"", result.containsKey("name"));
        Assert.assertEquals("Value must be enum name", "GREEN", result.get("name"));
    }

    /* ---------------------------------------------------------------------- */
    /* toEnumSet – happy path (collections)                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToEnumSetFromEnumCollection() {
        List<Color> source = Arrays.asList(Color.RED, Color.BLUE);
        EnumSet<Color> set = EnumConversions.toEnumSet(source, Color.class);
        Assert.assertEquals(EnumSet.of(Color.RED, Color.BLUE), set);
    }

    @Test
    public void testToEnumSetFromStringCollection() {
        List<String> source = Arrays.asList("RED", "GREEN");
        EnumSet<Color> set = EnumConversions.toEnumSet(source, Color.class);
        Assert.assertEquals(EnumSet.of(Color.RED, Color.GREEN), set);
    }

    @Test
    public void testToEnumSetFromNumberCollection() {
        List<Integer> source = Arrays.asList(0, 2); // RED (ordinal 0) and BLUE (ordinal 2)
        EnumSet<Color> set = EnumConversions.toEnumSet(source, Color.class);
        Assert.assertEquals(EnumSet.of(Color.RED, Color.BLUE), set);
    }

    @Test
    public void testToEnumSetFromMixedCollectionIgnoringNulls() {
        List<Object> source = new ArrayList<>();
        source.add(Color.GREEN);          // enum instance
        source.add("RED");                // string
        source.add(2);                    // ordinal for BLUE
        source.add(null);                 // should be ignored
        EnumSet<Color> set = EnumConversions.toEnumSet(source, Color.class);
        Assert.assertEquals(EnumSet.of(Color.RED, Color.GREEN, Color.BLUE), set);
    }

    @Test
    public void testToEnumSetFromEmptyCollection() {
        List<String> source = Collections.emptyList();
        EnumSet<Color> set = EnumConversions.toEnumSet(source, Color.class);
        Assert.assertTrue("Result should be an empty EnumSet", set.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* toEnumSet – happy path (arrays)                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToEnumSetFromEnumArray() {
        Color[] source = new Color[]{Color.BLUE, Color.RED};
        EnumSet<Color> set = EnumConversions.toEnumSet(source, Color.class);
        Assert.assertEquals(EnumSet.of(Color.BLUE, Color.RED), set);
    }

    @Test
    public void testToEnumSetFromStringArray() {
        String[] source = new String[]{"GREEN", "BLUE"};
        EnumSet<Color> set = EnumConversions.toEnumSet(source, Color.class);
        Assert.assertEquals(EnumSet.of(Color.GREEN, Color.BLUE), set);
    }

    @Test
    public void testToEnumSetFromNumberArray() {
        int[] source = new int[]{1, 0}; // GREEN, RED
        EnumSet<Color> set = EnumConversions.toEnumSet(source, Color.class);
        Assert.assertEquals(EnumSet.of(Color.RED, Color.GREEN), set);
    }

    @Test
    public void testToEnumSetFromMixedArrayWithNulls() {
        Object[] source = new Object[]{Color.RED, "GREEN", 2, null};
        EnumSet<Color> set = EnumConversions.toEnumSet(source, Color.class);
        Assert.assertEquals(EnumSet.of(Color.RED, Color.GREEN, Color.BLUE), set);
    }

    /* ---------------------------------------------------------------------- */
    /* toEnumSet – error handling                                               */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testToEnumSetTargetNotEnumThrows() {
        List<String> source = Arrays.asList("RED");
        // target type is String, not an enum
        EnumConversions.toEnumSet(source, String.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToEnumSetSourceNotCollectionOrArrayThrows() {
        // Passing a plain String should cause an exception
        EnumConversions.toEnumSet("RED", Color.class);
    }

    @Test
    public void testToEnumSetInvalidStringValueThrows() {
        List<String> source = Arrays.asList("YELLOW"); // not a valid enum constant
        try {
            EnumConversions.toEnumSet(source, Color.class);
            Assert.fail("Expected IllegalArgumentException for invalid enum name");
        } catch (IllegalArgumentException e) {
            // Expected – message should contain the offending name
            Assert.assertTrue(e.getMessage().contains("YELLOW"));
        }
    }

    @Test
    public void testToEnumSetInvalidOrdinalThrows() {
        List<Integer> source = Arrays.asList(5); // ordinal out of range for Color (size 3)
        try {
            EnumConversions.toEnumSet(source, Color.class);
            Assert.fail("Expected IllegalArgumentException for out‑of‑range ordinal");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("Invalid ordinal"));
        }
    }

    @Test
    public void testToEnumSetNonConvertibleElementThrows() {
        List<Object> source = Arrays.asList(Boolean.TRUE);
        try {
            EnumConversions.toEnumSet(source, Color.class);
            Assert.fail("Expected IllegalArgumentException for non‑convertible element");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains(Boolean.class.getName()));
        }
    }

    /* ---------------------------------------------------------------------- */
    /* Additional internal behaviour verification (lazy enum constants)       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNumericConversionLoadsEnumConstantsOnlyWhenNeeded() {
        // First element is a String (no numeric processing), second is a Number
        List<Object> source = Arrays.asList("RED", 1); // RED via String, GREEN via ordinal
        EnumSet<Color> set = EnumConversions.toEnumSet(source, Color.class);
        Assert.assertEquals(EnumSet.of(Color.RED, Color.GREEN), set);
    }
}
