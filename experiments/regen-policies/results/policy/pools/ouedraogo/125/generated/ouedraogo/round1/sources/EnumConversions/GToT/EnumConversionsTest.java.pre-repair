package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.util.*;

public class EnumConversionsTest {

    /* -----------------------------------------------------------------
     * Helper enum used for all test scenarios
     * ----------------------------------------------------------------- */
    private enum Sample {
        FIRST, SECOND, THIRD
    }

    /* -----------------------------------------------------------------
     * 1. Tests for EnumConversions.toMap(...)
     * ----------------------------------------------------------------- */
    @Test
    public void testToMap_basicEnum() {
        // Arrange
        Sample value = Sample.SECOND;
        Converter converter = new Converter(null); // converter is not used by the method

        // Act
        Map<String, Object> result = EnumConversions.toMap(value, converter);

        // Assert
        Assert.assertNotNull("Result map must not be null", result);
        Assert.assertEquals("Map must contain exactly one entry", 1, result.size());
        Assert.assertTrue("Map must contain key 'name'", result.containsKey("name"));
        Assert.assertEquals("Enum name must be stored", "SECOND", result.get("name"));
    }

    /* -----------------------------------------------------------------
     * 2. Tests for EnumConversions.toEnumSet(...)
     * ----------------------------------------------------------------- */

    // ----- happy path: collection of enum constants -----
    @Test
    public void testToEnumSet_fromEnumCollection() {
        Collection<Sample> source = Arrays.asList(Sample.FIRST, Sample.THIRD);
        Converter converter = new Converter(null);

        EnumSet<Sample> set = EnumConversions.toEnumSet(source, converter, Sample.class);
        Assert.assertEquals(2, set.size());
        Assert.assertTrue(set.contains(Sample.FIRST));
        Assert.assertTrue(set.contains(Sample.THIRD));
    }

    // ----- happy path: collection of enum names (String) -----
    @Test
    public void testToEnumSet_fromStringCollection() {
        Collection<String> source = Arrays.asList("FIRST", "SECOND");
        Converter converter = new Converter(null);

        EnumSet<Sample> set = EnumConversions.toEnumSet(source, converter, Sample.class);
        Assert.assertEquals(2, set.size());
        Assert.assertTrue(set.contains(Sample.FIRST));
        Assert.assertTrue(set.contains(Sample.SECOND));
    }

    // ----- happy path: collection of ordinals (Number) -----
    @Test
    public void testToEnumSet_fromNumberCollection() {
        Collection<Integer> source = Arrays.asList(0, 2); // FIRST and THIRD
        Converter converter = new Converter(null);

        EnumSet<Sample> set = EnumConversions.toEnumSet(source, converter, Sample.class);
        Assert.assertEquals(2, set.size());
        Assert.assertTrue(set.contains(Sample.FIRST));
        Assert.assertTrue(set.contains(Sample.THIRD));
    }

    // ----- happy path: mixed collection (enum, string, number, null) -----
    @Test
    public void testToEnumSet_fromMixedCollection() {
        Collection<Object> source = Arrays.asList(
                Sample.SECOND,          // enum
                "FIRST",                // string
                2,                      // number (ordinal for THIRD)
                null                    // null should be ignored
        );
        Converter converter = new Converter(null);

        EnumSet<Sample> set = EnumConversions.toEnumSet(source, converter, Sample.class);
        Assert.assertEquals(3, set.size());
        Assert.assertTrue(set.containsAll(Arrays.asList(Sample.FIRST, Sample.SECOND, Sample.THIRD)));
    }

    // ----- happy path: array of mixed types -----
    @Test
    public void testToEnumSet_fromMixedArray() {
        Object[] source = new Object[]{ Sample.FIRST, "SECOND", 2, null };
        Converter converter = new Converter(null);

        EnumSet<Sample> set = EnumConversions.toEnumSet(source, converter, Sample.class);
        Assert.assertEquals(3, set.size());
        Assert.assertTrue(set.containsAll(Arrays.asList(Sample.FIRST, Sample.SECOND, Sample.THIRD)));
    }

    // ----- edge case: target class is not an enum -----
    @Test(expected = IllegalArgumentException.class)
    public void testToEnumSet_targetNotEnum() {
        List<String> source = Arrays.asList("FIRST");
        Converter converter = new Converter(null);
        // Passing String.class (non‑enum) should trigger IllegalArgumentException
        EnumConversions.toEnumSet(source, converter, String.class);
    }

    // ----- edge case: source is neither collection nor array -----
    @Test(expected = IllegalArgumentException.class)
    public void testToEnumSet_sourceNotCollectionOrArray() {
        String source = "FIRST";
        Converter converter = new Converter(null);
        EnumConversions.toEnumSet(source, converter, Sample.class);
    }

    // ----- edge case: numeric ordinal out of bounds (negative) -----
    @Test(expected = IllegalArgumentException.class)
    public void testToEnumSet_numericOrdinalNegative() {
        List<Integer> source = Collections.singletonList(-1);
        Converter converter = new Converter(null);
        EnumConversions.toEnumSet(source, converter, Sample.class);
    }

    // ----- edge case: numeric ordinal out of bounds (too large) -----
    @Test(expected = IllegalArgumentException.class)
    public void testToEnumSet_numericOrdinalTooLarge() {
        List<Integer> source = Collections.singletonList(5); // Sample has only 0‑2
        Converter converter = new Converter(null);
        EnumConversions.toEnumSet(source, converter, Sample.class);
    }

    // ----- edge case: invalid enum name string -----
    @Test(expected = IllegalArgumentException.class)
    public void testToEnumSet_invalidStringName() {
        List<String> source = Collections.singletonList("UNKNOWN");
        Converter converter = new Converter(null);
        EnumConversions.toEnumSet(source, converter, Sample.class);
    }

    // ----- edge case: element type not convertible (e.g., Boolean) -----
    @Test(expected = IllegalArgumentException.class)
    public void testToEnumSet_unconvertibleElement() {
        List<Object> source = Collections.singletonList(Boolean.TRUE);
        Converter converter = new Converter(null);
        EnumConversions.toEnumSet(source, converter, Sample.class);
    }

    // ----- edge case: collection contains nulls only -----
    @Test
    public void testToEnumSet_allNullElements() {
        List<Object> source = Arrays.asList(null, null);
        Converter converter = new Converter(null);
        EnumSet<Sample> set = EnumConversions.toEnumSet(source, converter, Sample.class);
        Assert.assertTrue("EnumSet should be empty when all elements are null", set.isEmpty());
    }
}
