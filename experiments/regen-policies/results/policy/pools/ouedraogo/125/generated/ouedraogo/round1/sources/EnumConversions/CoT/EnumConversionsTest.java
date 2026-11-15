package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.*;

import static org.junit.Assert.*;

public class EnumConversionsTest {

    // Dummy enum used for testing
    private enum Color {
        RED, GREEN, BLUE
    }

    private Converter converter;

    @Before
    public void setUp() {
        // Converter has no behaviour needed for EnumConversions, a simple instance is enough.
        converter = new Converter(new ConverterOptions());
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    // -----------------------------------------------------------------
    // 1. List of public static methods in EnumConversions
    // -----------------------------------------------------------------
    //   static Map<String,Object> toMap(Object from, Converter converter)
    //   static <T extends Enum<T>> EnumSet<T> toEnumSet(Object from, Converter converter, Class<?> target)

    // -----------------------------------------------------------------
    // 2. Basic test cases for each method
    // -----------------------------------------------------------------

    @Test
    public void testToMapBasic() {
        // Given an enum instance
        Color enumInstance = Color.GREEN;

        // When converting to map
        Map<String, Object> result = EnumConversions.toMap(enumInstance, converter);

        // Then map contains only the "name" entry with correct value
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("name"));
        assertEquals("GREEN", result.get("name"));
    }

    @Test
    public void testToEnumSetFromCollectionOfEnums() {
        List<Color> source = Arrays.asList(Color.RED, Color.BLUE);
        EnumSet<Color> result = EnumConversions.toEnumSet(source, converter, Color.class);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(Color.RED));
        assertTrue(result.contains(Color.BLUE));
    }

    @Test
    public void testToEnumSetFromArrayOfEnums() {
        Color[] source = new Color[]{Color.GREEN, Color.BLUE};
        EnumSet<Color> result = EnumConversions.toEnumSet(source, converter, Color.class);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(Color.GREEN));
        assertTrue(result.contains(Color.BLUE));
    }

    @Test
    public void testToEnumSetFromCollectionOfStrings() {
        List<String> source = Arrays.asList("RED", "GREEN");
        EnumSet<Color> result = EnumConversions.toEnumSet(source, converter, Color.class);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(Color.RED));
        assertTrue(result.contains(Color.GREEN));
    }

    @Test
    public void testToEnumSetFromArrayOfStrings() {
        String[] source = new String[]{"BLUE", "RED"};
        EnumSet<Color> result = EnumConversions.toEnumSet(source, converter, Color.class);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(Color.BLUE));
        assertTrue(result.contains(Color.RED));
    }

    @Test
    public void testToEnumSetFromCollectionOfNumbers() {
        List<Integer> source = Arrays.asList(0, 2); // RED (0) and BLUE (2)
        EnumSet<Color> result = EnumConversions.toEnumSet(source, converter, Color.class);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(Color.RED));
        assertTrue(result.contains(Color.BLUE));
    }

    @Test
    public void testToEnumSetFromArrayOfNumbers() {
        Integer[] source = new Integer[]{1, 2}; // GREEN (1) and BLUE (2)
        EnumSet<Color> result = EnumConversions.toEnumSet(source, converter, Color.class);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(Color.GREEN));
        assertTrue(result.contains(Color.BLUE));
    }

    // -----------------------------------------------------------------
    // 3. Edge cases & exception handling
    // -----------------------------------------------------------------

    @Test
    public void testToEnumSetTargetNotEnumThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("target type java.lang.String must be an Enum");

        EnumConversions.toEnumSet(Arrays.asList("RED"), converter, String.class);
    }

    @Test
    public void testToEnumSetSourceNotCollectionOrArrayThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Source must be a Collection or Array");

        EnumConversions.toEnumSet("RED", converter, Color.class);
    }

    @Test
    public void testToEnumSetInvalidStringThrows() {
        thrown.expect(IllegalArgumentException.class);
        // Enum.valueOf throws IllegalArgumentException with a message containing the invalid name
        EnumConversions.toEnumSet(Arrays.asList("PURPLE"), converter, Color.class);
    }

    @Test
    public void testToEnumSetInvalidOrdinalLowThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Invalid ordinal value -1");

        EnumConversions.toEnumSet(Arrays.asList(-1), converter, Color.class);
    }

    @Test
    public void testToEnumSetInvalidOrdinalHighThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Invalid ordinal value 3");

        EnumConversions.toEnumSet(Arrays.asList(3), converter, Color.class);
    }

    @Test
    public void testToEnumSetUnsupportedElementTypeThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("java.util.Date found in source collection/array is not convertible to");

        List<Date> source = Arrays.asList(new Date());
        EnumConversions.toEnumSet(source, converter, Color.class);
    }

    @Test
    public void testToEnumSetNullElementsAreIgnored() {
        // Mix of null and valid entries – nulls should be skipped without error
        List<Object> source = Arrays.asList(null, "RED", null, 1);
        EnumSet<Color> result = EnumConversions.toEnumSet(source, converter, Color.class);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(Color.RED));
        assertTrue(result.contains(Color.GREEN));
    }

    // -----------------------------------------------------------------
    // 4. Additional sanity checks
    // -----------------------------------------------------------------

    @Test
    public void testToMapWithNullEnumThrowsClassCastException() {
        thrown.expect(ClassCastException.class);
        EnumConversions.toMap(null, converter);
    }

    @Test
    public void testToEnumSetEmptyCollectionReturnsEmptySet() {
        List<Color> empty = Collections.emptyList();
        EnumSet<Color> result = EnumConversions.toEnumSet(empty, converter, Color.class);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToEnumSetEmptyArrayReturnsEmptySet() {
        Color[] empty = new Color[0];
        EnumSet<Color> result = EnumConversions.toEnumSet(empty, converter, Color.class);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
