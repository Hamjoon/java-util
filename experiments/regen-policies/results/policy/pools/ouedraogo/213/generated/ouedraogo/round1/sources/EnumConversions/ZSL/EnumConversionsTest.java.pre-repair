/**********************************************************************************************************************
 *  EnumConversionsTest.java
 *
 *  Comprehensive JUnit‑4 test suite for {@link EnumConversions}.
 *
 *  The tests cover:
 *   • {@code toMap(Object, Converter)} – correct mapping of enum name.
 *   • {@code toEnumSet(Object, Class)} – conversion from Collections and Arrays containing:
 *        – Enum instances
 *        – String names
 *        – Numeric ordinals
 *        – Mixed valid elements with nulls (nulls are ignored)
 *   • Error handling for:
 *        – Non‑enum target type.
 *        – Source not a Collection/Array.
 *        – Elements of unsupported type.
 *        – Invalid ordinal values.
 *        – Invalid enum name strings.
 *
 *  The test class resides in the same package as {@code EnumConversions} to allow package‑private access.
 *********************************************************************************************************************/

package com.cedarsoftware.util.convert;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class EnumConversionsTest {

    /** Simple enum used throughout the tests. */
    private enum Sample {
        FIRST,
        SECOND,
        THIRD
    }

    /**
     * Helper that creates a dummy {@link Converter}.  The {@code EnumConversions.toMap} method does not use the
     * converter instance, so a simple new instance with default options is sufficient.
     */
    private Converter dummyConverter() {
        // The Converter class only has a public constructor that takes ConverterOptions.
        // Since we do not need any specific behaviour, we can pass null (options may be optional)
        // or create a new instance with a default options object if required.
        // The source code of Converter is not provided, but its constructor is public,
        // so we assume a no‑arg constructor exists via the static factory method.
        return new Converter(null);
    }

    /* --------------------------------------------------- */
    /*                     toMap tests                     */
    /* --------------------------------------------------- */

    @Test
    public void toMapReturnsMapContainingEnumName() {
        Sample enumValue = Sample.SECOND;
        Map<String, Object> result = EnumConversions.toMap(enumValue, dummyConverter());

        assertNotNull("Result map must not be null", result);
        assertEquals("Map should contain exactly one entry", 1, result.size());
        assertTrue("Map should contain the key \"name\"", result.containsKey("name"));
        assertEquals("Map value must be the enum name", "SECOND", result.get("name"));
    }

    /* --------------------------------------------------- */
    /*                 toEnumSet success cases             */
    /* --------------------------------------------------- */

    @Test
    public void toEnumSetFromCollectionOfEnumInstances() {
        Collection<Sample> source = Arrays.asList(Sample.FIRST, Sample.THIRD);
        EnumSet<Sample> set = EnumConversions.toEnumSet(source, Sample.class);

        assertEquals("EnumSet size must match source size", 2, set.size());
        assertTrue(set.contains(Sample.FIRST));
        assertTrue(set.contains(Sample.THIRD));
    }

    @Test
    public void toEnumSetFromCollectionOfStringNames() {
        Collection<String> source = Arrays.asList("FIRST", "SECOND");
        EnumSet<Sample> set = EnumConversions.toEnumSet(source, Sample.class);

        assertEquals(2, set.size());
        assertTrue(set.contains(Sample.FIRST));
        assertTrue(set.contains(Sample.SECOND));
    }

    @Test
    public void toEnumSetFromCollectionOfNumericOrdinals() {
        Collection<Number> source = Arrays.asList(0, 2); // 0 -> FIRST, 2 -> THIRD
        EnumSet<Sample> set = EnumConversions.toEnumSet(source, Sample.class);

        assertEquals(2, set.size());
        assertTrue(set.contains(Sample.FIRST));
        assertTrue(set.contains(Sample.THIRD));
    }

    @Test
    public void toEnumSetFromArrayOfEnumInstances() {
        Sample[] source = new Sample[]{Sample.SECOND, Sample.SECOND, Sample.FIRST};
        EnumSet<Sample> set = EnumConversions.toEnumSet(source, Sample.class);

        assertEquals(2, set.size());
        assertTrue(set.contains(Sample.FIRST));
        assertTrue(set.contains(Sample.SECOND));
    }

    @Test
    public void toEnumSetFromArrayOfStringNames() {
        String[] source = new String[]{"THIRD", "FIRST"};
        EnumSet<Sample> set = EnumConversions.toEnumSet(source, Sample.class);

        assertEquals(2, set.size());
        assertTrue(set.contains(Sample.THIRD));
        assertTrue(set.contains(Sample.FIRST));
    }

    @Test
    public void toEnumSetFromArrayOfNumericOrdinals() {
        Integer[] source = new Integer[]{1, 0}; // 1 -> SECOND, 0 -> FIRST
        EnumSet<Sample> set = EnumConversions.toEnumSet(source, Sample.class);

        assertEquals(2, set.size());
        assertTrue(set.contains(Sample.FIRST));
        assertTrue(set.contains(Sample.SECOND));
    }

    @Test
    public void toEnumSetIgnoresNullElements() {
        List<Object> source = Arrays.asList(null, Sample.FIRST, null, "SECOND", 2, null);
        EnumSet<Sample> set = EnumConversions.toEnumSet(source, Sample.class);

        assertEquals(3, set.size());
        assertTrue(set.containsAll(Arrays.asList(Sample.FIRST, Sample.SECOND, Sample.THIRD)));
    }

    /* --------------------------------------------------- */
    /*                toEnumSet error handling             */
    /* --------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void toEnumSetThrowsWhenTargetIsNotEnum() {
        // Use a regular class (String) as the target type.
        List<String> source = Collections.singletonList("anything");
        EnumConversions.toEnumSet(source, String.class);
    }

    @Test
    public void toEnumSetThrowsWhenSourceIsNotCollectionOrArray() {
        try {
            EnumConversions.toEnumSet("not a collection", Sample.class);
            fail("Expected IllegalArgumentException because source is not a Collection nor an Array");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("Source must be a Collection or Array"));
        }
    }

    @Test
    public void toEnumSetThrowsOnUnsupportedElementType() {
        List<Object> source = Arrays.asList(new Object(), Sample.FIRST);
        try {
            EnumConversions.toEnumSet(source, Sample.class);
            fail("Expected IllegalArgumentException for unsupported element type");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("java.lang.Object") &&
                       ex.getMessage().contains("is not convertible to"));
        }
    }

    @Test
    public void toEnumSetThrowsOnInvalidOrdinal() {
        List<Number> source = Arrays.asList(0, 5); // 5 is out of range for Sample (size = 3)
        try {
            EnumConversions.toEnumSet(source, Sample.class);
            fail("Expected IllegalArgumentException for invalid ordinal");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("Invalid ordinal value 5"));
        }
    }

    @Test
    public void toEnumSetThrowsOnInvalidStringName() {
        List<String> source = Arrays.asList("FIRST", "UNKNOWN");
        try {
            EnumConversions.toEnumSet(source, Sample.class);
            fail("Expected IllegalArgumentException for unknown enum name");
        } catch (IllegalArgumentException ex) {
            // Enum.valueOf throws IllegalArgumentException with a message that includes the bad name.
            assertTrue(ex.getMessage().contains("UNKNOWN"));
        }
    }

    /* --------------------------------------------------- */
    /*                Internal helper verification         */
    /* --------------------------------------------------- */

    @Test
    public void processElementHandlesEnumInstanceDirectly() throws Exception {
        // Access private method via reflection to verify that the lazy enumConstants array is not
        // created when processing a plain Enum instance.
        java.lang.reflect.Method method = EnumConversions.class
                .getDeclaredMethod("processElement", Object.class, EnumSet.class, Class.class, Object[].class);
        method.setAccessible(true);

        EnumSet<Sample> set = EnumSet.noneOf(Sample.class);
        Object[] enumConstants = null; // initially null

        // First call with an enum instance – should not touch enumConstants
        enumConstants = (Object[]) method.invoke(null, Sample.FIRST, set, Sample.class, enumConstants);
        assertNull("enumConstants should remain null after processing an Enum instance", enumConstants);
        assertTrue(set.contains(Sample.FIRST));

        // Second call with a numeric value – now enumConstants should be populated
        enumConstants = (Object[]) method.invoke(null, 1, set, Sample.class, enumConstants);
        assertNotNull("enumConstants must be initialized after first numeric element", enumConstants);
        assertEquals(3, enumConstants.length); // Sample has 3 constants
        assertTrue(set.contains(Sample.SECOND));
    }
}
