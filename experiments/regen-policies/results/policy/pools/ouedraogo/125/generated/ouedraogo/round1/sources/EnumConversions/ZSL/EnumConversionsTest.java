package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.util.*;

public class EnumConversionsTest {

    /** Simple enum used for all test scenarios. */
    private enum TestEnum {
        A, B, C
    }

    /** Helper that creates a dummy Converter – the EnumConversions methods never use it. */
    private static Converter dummyConverter() {
        // Converter requires a ConverterOptions instance; passing null is acceptable for our tests
        return new Converter(null);
    }

    @Test
    public void testToMapReturnsName() {
        Map<String, Object> result = EnumConversions.toMap(TestEnum.B, dummyConverter());

        Assert.assertEquals("Result map should contain exactly one entry", 1, result.size());
        Assert.assertTrue("Result map should contain key \"name\"", result.containsKey("name"));
        Assert.assertEquals("Enum name should be stored", "B", result.get("name"));
    }

    @Test
    public void testToEnumSetFromCollectionMixedTypes() {
        List<Object> source = Arrays.asList(
                "A",                 // String name
                TestEnum.B,          // Enum instance
                2,                   // Ordinal (C)
                null,                // Null should be ignored
                "C"                  // String name again
        );

        EnumSet<TestEnum> result = EnumConversions.toEnumSet(source, dummyConverter(), TestEnum.class);

        Assert.assertEquals("EnumSet should contain three distinct values", 3, result.size());
        Assert.assertTrue(result.contains(TestEnum.A));
        Assert.assertTrue(result.contains(TestEnum.B));
        Assert.assertTrue(result.contains(TestEnum.C));
    }

    @Test
    public void testToEnumSetFromArrayMixedTypes() {
        Object[] source = new Object[] {
                "C",                 // String name
                0,                   // Ordinal (A)
                TestEnum.B,          // Enum instance
                null                 // Null ignored
        };

        EnumSet<TestEnum> result = EnumConversions.toEnumSet(source, dummyConverter(), TestEnum.class);

        Assert.assertEquals("EnumSet should contain three distinct values", 3, result.size());
        Assert.assertTrue(result.contains(TestEnum.A));
        Assert.assertTrue(result.contains(TestEnum.B));
        Assert.assertTrue(result.contains(TestEnum.C));
    }

    @Test
    public void testToEnumSetWithEmptyCollection() {
        Collection<Object> empty = Collections.emptyList();
        EnumSet<TestEnum> result = EnumConversions.toEnumSet(empty, dummyConverter(), TestEnum.class);
        Assert.assertTrue("Resulting EnumSet should be empty", result.isEmpty());
    }

    @Test
    public void testToEnumSetInvalidTargetNotEnum() {
        try {
            EnumConversions.toEnumSet(Arrays.asList("A"), dummyConverter(), String.class);
            Assert.fail("Expected IllegalArgumentException for non‑enum target type");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("must be an Enum"));
        }
    }

    @Test
    public void testToEnumSetInvalidSourceType() {
        try {
            EnumConversions.toEnumSet(123, dummyConverter(), TestEnum.class);
            Assert.fail("Expected IllegalArgumentException for invalid source type");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("Source must be a Collection or Array"));
        }
    }

    @Test
    public void testToEnumSetInvalidOrdinalThrows() {
        List<Object> source = Arrays.asList(5); // Ordinal out of range for TestEnum (size 3)
        try {
            EnumConversions.toEnumSet(source, dummyConverter(), TestEnum.class);
            Assert.fail("Expected IllegalArgumentException for out‑of‑range ordinal");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("Invalid ordinal value"));
        }
    }

    @Test
    public void testToEnumSetUnsupportedElementTypeThrows() {
        List<Object> source = Arrays.asList(new Object());
        try {
            EnumConversions.toEnumSet(source, dummyConverter(), TestEnum.class);
            Assert.fail("Expected IllegalArgumentException for unsupported element type");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("found in source collection/array is not convertible"));
        }
    }
}
