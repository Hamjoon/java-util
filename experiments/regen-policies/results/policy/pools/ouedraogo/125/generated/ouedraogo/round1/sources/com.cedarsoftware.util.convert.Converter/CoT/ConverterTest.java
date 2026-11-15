package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class ConverterTest {

    private Converter converter;

    /** Simple enum used for EnumSet conversion tests */
    private enum Day {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }

    /** Minimal implementation of ConverterOptions required by the Converter constructor */
    private static class SimpleOptions implements ConverterOptions {
        @Override
        public Map<Converter.ConversionPair, Convert<?>> getConverterOverrides() {
            return new HashMap<>();
        }
    }

    @Before
    public void setUp() {
        converter = new Converter(new SimpleOptions());
    }

    /* ----------------------------------------------------------------------
     *  Tests for the primary convert(...) method
     * ---------------------------------------------------------------------- */

    @Test
    public void testSimpleStringToIntegerConversion() {
        Integer result = converter.convert("123", Integer.class);
        assertEquals(Integer.valueOf(123), result);
    }

    @Test
    public void testNullHandlingForObjectTarget() {
        assertNull(converter.convert(null, Integer.class));
        assertNull(converter.convert(null, String.class));
    }

    @Test
    public void testNullHandlingForPrimitiveTargets() {
        int intResult = converter.convert(null, int.class);
        assertEquals(0, intResult);

        long longResult = converter.convert(null, long.class);
        assertEquals(0L, longResult);

        boolean boolResult = converter.convert(null, boolean.class);
        assertFalse(boolResult);

        char charResult = converter.convert(null, char.class);
        assertEquals('\u0000', charResult);
    }

    @Test
    public void testUnsupportedConversionThrowsException() {
        try {
            converter.convert(new Object(), String.class);
            fail("Expected IllegalArgumentException for unsupported conversion");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testConvertWithNullTargetClassThrowsException() {
        try {
            converter.convert("test", null);
            fail("Expected IllegalArgumentException when target type is null");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* ----------------------------------------------------------------------
     *  Tests for collection/array conversion support
     * ---------------------------------------------------------------------- */

    @Test
    public void testArrayToCollectionConversion() {
        String[] source = {"apple", "banana", "cherry"};
        List<String> result = converter.convert(source, List.class);
        assertEquals(Arrays.asList(source), result);
    }

    @Test
    public void testCollectionToArrayConversion() {
        List<String> source = Arrays.asList("x", "y", "z");
        String[] result = converter.convert(source, String[].class);
        assertArrayEquals(new String[]{"x", "y", "z"}, result);
    }

    @Test
    public void testArrayToArrayWithDifferentComponentTypes() {
        String[] source = {"10", "20", "30"};
        Integer[] result = converter.convert(source, Integer[].class);
        assertArrayEquals(new Integer[]{10, 20, 30}, result);
    }

    @Test
    public void testEnumSetConversionFromArrayAndCollection() {
        Object[] source = {Day.MONDAY, "TUESDAY", 3}; // 3 -> WEDNESDAY (ordinal)
        @SuppressWarnings("unchecked")
        EnumSet<Day> result = (EnumSet<Day>) (Object) converter.convert(source, Day.class);
        assertTrue(result.contains(Day.MONDAY));
        assertTrue(result.contains(Day.TUESDAY));
        assertTrue(result.contains(Day.WEDNESDAY));
    }

    @Test
    public void testIsCollectionConversionSupportedPositiveCases() {
        assertTrue(converter.isCollectionConversionSupported(String[].class, List.class));
        assertTrue(converter.isCollectionConversionSupported(List.class, String[].class));
        assertTrue(converter.isCollectionConversionSupported(Day[].class, EnumSet.class) == false); // EnumSet.class is illegal, should throw later
    }

    @Test
    public void testIsCollectionConversionSupportedIllegalEnumSetTarget() {
        try {
            converter.isCollectionConversionSupported(String[].class, EnumSet.class);
            fail("Expected IllegalArgumentException when target is EnumSet.class");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testIsCollectionConversionSupportedNegativeCases() {
        assertFalse(converter.isCollectionConversionSupported(Integer.class, List.class));
        assertFalse(converter.isCollectionConversionSupported(String.class, Map.class));
    }

    /* ----------------------------------------------------------------------
     *  Tests for simple (non‑collection) conversion queries
     * ---------------------------------------------------------------------- */

    @Test
    public void testIsSimpleTypeConversionSupported() {
        assertTrue(converter.isSimpleTypeConversionSupported(String.class, Integer.class));
        assertFalse(converter.isSimpleTypeConversionSupported(String[].class, Integer[].class));
        assertFalse(converter.isSimpleTypeConversionSupported(List.class, Set.class));
    }

    @Test
    public void testIsDirectConversionSupported() {
        // Direct conversion defined in CONVERSION_DB
        assertTrue(converter.isDirectConversionSupported(String.class, Integer.class));

        // Direct array‑to‑array conversion (component conversion also direct)
        assertTrue(converter.isDirectConversionSupported(String[].class, Integer[].class));

        // Not a direct conversion (needs inheritance lookup)
        assertFalse(converter.isDirectConversionSupported(Number.class, Double.class));
    }

    @Test
    public void testIsConversionSupportedFor() {
        // Simple conversion
        assertTrue(converter.isConversionSupportedFor(String.class, Integer.class));

        // Collection conversion (array to collection)
        assertTrue(converter.isConversionSupportedFor(String[].class, List.class));

        // Array‑to‑array with component conversion
        assertTrue(converter.isConversionSupportedFor(String[].class, Integer[].class));

        // Unsupported case
        assertFalse(converter.isConversionSupportedFor(Object.class, UUID.class));
    }

    /* ----------------------------------------------------------------------
     *  Tests for adding custom conversions
     * ---------------------------------------------------------------------- */

    @Test
    public void testAddCustomConversion() {
        // Add a custom conversion from String "yes"/"no" to Boolean
        converter.addConversion(String.class, Boolean.class,
                (from, conv) -> "yes".equalsIgnoreCase((String) from));

        Boolean yes = converter.convert("yes", Boolean.class);
        Boolean no = converter.convert("no", Boolean.class);
        assertTrue(yes);
        assertFalse(no);
    }

    @Test
    public void testCustomConversionAppearsInSupportedConversions() {
        converter.addConversion(String.class, Boolean.class,
                (from, conv) -> "yes".equalsIgnoreCase((String) from));

        Map<String, Set<String>> map = converter.getSupportedConversions();
        Set<String> targets = map.get("String");
        assertNotNull(targets);
        assertTrue(targets.contains("Boolean"));
    }

    /* ----------------------------------------------------------------------
     *  Tests for auxiliary public methods
     * ---------------------------------------------------------------------- */

    @Test
    public void testAllSupportedConversionsStructure() {
        Map<Class<?>, Set<Class<?>>> map = converter.allSupportedConversions();
        assertNotNull(map);
        // At least a few known mappings should be present
        assertTrue(map.getOrDefault(String.class, Collections.emptySet()).contains(Integer.class));
        assertTrue(map.getOrDefault(Integer.class, Collections.emptySet()).contains(Long.class));
    }

    @Test
    public void testGetSupportedConversionsNames() {
        Map<String, Set<String>> map = converter.getSupportedConversions();
        assertTrue(map.getOrDefault("String", Collections.emptySet()).contains("Integer"));
        assertTrue(map.getOrDefault("Integer", Collections.emptySet()).contains("Long"));
    }

    @Test
    public void testIdentityConversion() {
        String original = "identity";
        String result = Converter.identity(original, converter);
        assertSame(original, result);
    }

    @Test
    public void testGetOptionsReturnsProvidedInstance() {
        ConverterOptions opts = new SimpleOptions();
        Converter conv = new Converter(opts);
        assertSame(opts, conv.getOptions());
    }
}
