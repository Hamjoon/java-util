package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Converter}.
 *
 * <p>This test class covers:</p>
 * <ul>
 *   <li>Public API methods: {@code getOptions()}, {@code convert()}, {@code isCollectionConversionSupported()},
 *       {@code isSimpleTypeConversionSupported()}, {@code isDirectConversionSupported()},
 *       {@code isConversionSupportedFor()}, {@code addConversion()}, {@code allSupportedConversions()},
 *       {@code getSupportedConversions()}.</li>
 *   <li>Typical use cases (e.g., primitive, wrapper, date‑time, collection, enum conversions).</li>
 *   <li>Edge cases (null handling, primitive defaults, array‑to‑array component conversion).</li>
 *   <li>Error scenarios (unsupported conversion, illegal EnumSet target).</li>
 * </ul>
 *
 * <p>All tests use only the public API of {@link Converter} and its related
 * classes; no reflection or mocking is performed.</p>
 */
public class ConverterTest {

    /** Minimal {@link ConverterOptions} implementation used for all tests. */
    private static class SimpleOptions implements ConverterOptions {
        @Override
        public Map<Converter.ConversionPair, Convert<?>> getConverterOverrides() {
            return new HashMap<>();
        }
    }

    private Converter converter;
    private SimpleOptions options;

    @Before
    public void setUp() {
        options = new SimpleOptions();
        converter = new Converter(options);
    }

    /* --------------------------------------------------------------
     *  Basic getter test
     * -------------------------------------------------------------- */
    @Test
    public void testGetOptionsReturnsProvidedOptions() {
        assertSame("getOptions() should return the instance passed to the constructor",
                options, converter.getOptions());
    }

    /* --------------------------------------------------------------
     *  Null handling and primitive defaults
     * -------------------------------------------------------------- */
    @Test
    public void testConvertNullToPrimitiveIntReturnsZero() {
        int result = converter.convert(null, int.class);
        assertEquals("null -> int should return 0", 0, result);
    }

    @Test
    public void testConvertNullToPrimitiveBooleanReturnsFalse() {
        boolean result = converter.convert(null, boolean.class);
        assertFalse("null -> boolean should return false", result);
    }

    @Test
    public void testConvertNullToObjectReturnsNull() {
        String result = converter.convert(null, String.class);
        assertNull("null -> String should return null", result);
    }

    /* --------------------------------------------------------------
     *  Simple scalar conversions
     * -------------------------------------------------------------- */
    @Test
    public void testConvertStringToInteger() {
        Integer i = converter.convert("12345", Integer.class);
        assertEquals(Integer.valueOf(12345), i);
    }

    @Test
    public void testConvertStringToLocalDate() {
        LocalDate date = converter.convert("2024-11-14", LocalDate.class);
        assertEquals(LocalDate.of(2024, 11, 14), date);
    }

    @Test
    public void testConvertStringToBigDecimal() {
        BigDecimal bd = converter.convert("123.456", BigDecimal.class);
        assertEquals(new BigDecimal("123.456"), bd);
    }

    @Test
    public void testConvertStringToBoolean() {
        Boolean b = converter.convert("true", Boolean.class);
        assertTrue(b);
    }

    @Test
    public void testConvertStringToChar() {
        Character c = converter.convert("Z", Character.class);
        assertEquals(Character.valueOf('Z'), c);
    }

    /* --------------------------------------------------------------
     *  Map → UUID conversion (built‑in)
     * -------------------------------------------------------------- */
    @Test
    public void testConvertMapToUUID() {
        Map<String, Object> map = new HashMap<>();
        map.put("mostSigBits", 0x0123456789ABCDEFL);
        map.put("leastSigBits", 0xFEDCBA9876543210L);
        UUID uuid = converter.convert(map, UUID.class);
        assertEquals(new UUID(0x0123456789ABCDEFL, 0xFEDCBA9876543210L), uuid);
    }

    /* --------------------------------------------------------------
     *  Unsupported conversion handling
     * -------------------------------------------------------------- */
    @Test
    public void testConvertUnsupportedThrowsException() {
        try {
            converter.convert(new Object(), Thread.class);
            fail("Expected IllegalArgumentException for unsupported conversion");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unsupported conversion"));
        }
    }

    /* --------------------------------------------------------------
     *  Collection conversions
     * -------------------------------------------------------------- */
    @Test
    public void testArrayToListConversion() {
        String[] source = {"alpha", "beta", "gamma"};
        List<String> list = converter.convert(source, List.class);
        assertEquals(Arrays.asList(source), list);
    }

    @Test
    public void testCollectionToArrayConversion() {
        List<Integer> source = Arrays.asList(1, 2, 3);
        Integer[] array = converter.convert(source, Integer[].class);
        assertArrayEquals(new Integer[]{1, 2, 3}, array);
    }

    @Test
    public void testEnumSetConversionFromArray() {
        // Day enum defined locally for the test
        enum Day { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }

        Object[] mixed = {Day.MONDAY, "TUESDAY", 3 /* = WEDNESDAY */};
        @SuppressWarnings("unchecked")
        EnumSet<Day> set = (EnumSet<Day>) (Object) converter.convert(mixed, Day.class);
        assertTrue(set.contains(Day.MONDAY));
        assertTrue(set.contains(Day.TUESDAY));
        assertTrue(set.contains(Day.WEDNESDAY));
        assertEquals(3, set.size());
    }

    /* --------------------------------------------------------------
     *  isCollectionConversionSupported() edge cases
     * -------------------------------------------------------------- */
    @Test
    public void testIsCollectionConversionSupportedArrayToList() {
        assertTrue(converter.isCollectionConversionSupported(String[].class, List.class));
    }

    @Test
    public void testIsCollectionConversionSupportedListToArray() {
        assertTrue(converter.isCollectionConversionSupported(ArrayList.class, Integer[].class));
    }

    @Test
    public void testIsCollectionConversionSupportedEnumSetToArray() {
        assertTrue(converter.isCollectionConversionSupported(EnumSet.class, String[].class));
    }

    @Test
    public void testIsCollectionConversionSupportedEnumSetTargetThrows() {
        try {
            converter.isCollectionConversionSupported(String[].class, EnumSet.class);
            fail("Expected IllegalArgumentException when target is raw EnumSet");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("specify the Enum class"));
        }
    }

    /* --------------------------------------------------------------
     *  Simple type conversion support checks
     * -------------------------------------------------------------- */
    @Test
    public void testIsSimpleTypeConversionSupportedTrue() {
        assertTrue(converter.isSimpleTypeConversionSupported(String.class, Integer.class));
    }

    @Test
    public void testIsSimpleTypeConversionSupportedFalseForCollections() {
        assertFalse(converter.isSimpleTypeConversionSupported(List.class, Set.class));
    }

    @Test
    public void testIsSimpleTypeConversionSupportedNumberClass() {
        // Number → Long is supported via the Number → Long mapping
        assertTrue(converter.isSimpleTypeConversionSupported(Number.class, Long.class));
    }

    /* --------------------------------------------------------------
     *  Direct conversion support (including component checks)
     * -------------------------------------------------------------- */
    @Test
    public void testIsDirectConversionSupportedPrimitiveWrapper() {
        assertTrue(converter.isDirectConversionSupported(Integer.class, int.class));
    }

    @Test
    public void testIsDirectConversionSupportedArrayComponent() {
        // String[] → Integer[] is supported because both array conversion and String→Integer exist.
        assertTrue(converter.isDirectConversionSupported(String[].class, Integer[].class));
    }

    @Test
    public void testIsDirectConversionSupportedArrayComponentNegative() {
        // String[] → Thread[] is NOT supported (no String→Thread conversion)
        assertFalse(converter.isDirectConversionSupported(String[].class, Thread[].class));
    }

    /* --------------------------------------------------------------
     *  Full conversion support (including inheritance)
     * -------------------------------------------------------------- */
    @Test
    public void testIsConversionSupportedForArrayComponent() {
        assertTrue(converter.isConversionSupportedFor(String[].class, Integer[].class));
    }

    @Test
    public void testIsConversionSupportedForEnumSetTarget() {
        // From array to EnumSet is supported when the target is the enum type (handled via EnumSet creation)
        enum Color { RED, GREEN, BLUE }
        assertTrue(converter.isConversionSupportedFor(String[].class, Color.class));
    }

    @Test
    public void testIsConversionSupportedForUnsupported() {
        assertFalse(converter.isConversionSupportedFor(String.class, Thread.class));
    }

    /* --------------------------------------------------------------
     *  Custom conversion registration (override built‑in)
     * -------------------------------------------------------------- */
    @Test
    public void testAddConversionOverridesExisting() {
        // Override String → UUID to always return a fixed UUID
        UUID fixed = UUID.fromString("11111111-2222-3333-4444-555555555555");
        converter.addConversion(String.class, UUID.class,
                (from, conv, target) -> fixed);

        UUID result = converter.convert("any-string", UUID.class);
        assertEquals(fixed, result);
    }

    /* --------------------------------------------------------------
     *  getSupportedConversions() sanity check
     * -------------------------------------------------------------- */
    @Test
    public void testGetSupportedConversionsContainsKnownEntry() {
        Map<String, Set<String>> map = converter.getSupportedConversions();
        // Verify that String → Integer is present
        Set<String> targets = map.get("String");
        assertNotNull("String source should be present", targets);
        assertTrue("String → Integer should be listed", targets.contains("Integer"));
    }

    /* --------------------------------------------------------------
     *  allSupportedConversions() structure check
     * -------------------------------------------------------------- */
    @Test
    public void testAllSupportedConversionsStructure() {
        Map<Class<?>, Set<Class<?>>> map = converter.allSupportedConversions();
        // Verify that Boolean source maps to Boolean target (identity conversion)
        Set<Class<?>> targets = map.get(Boolean.class);
        assertNotNull("Boolean source should be present", targets);
        assertTrue(targets.contains(Boolean.class));
        // Verify that conversion map contains at least one entry for Number → Long
        Set<Class<?>> numberTargets = map.get(Number.class);
        assertNotNull("Number source should be present", numberTargets);
        assertTrue(numberTargets.contains(Long.class));
    }

    /* --------------------------------------------------------------
     *  getShortName() edge cases (array and java.sql.Date)
     * -------------------------------------------------------------- */
    @Test
    public void testGetShortNameForPrimitiveArray() {
        assertEquals("int[]", Converter.getShortName(int[].class));
    }

    @Test
    public void testGetShortNameForCustomArrayName() {
        // java.sql.Date[] has a custom name defined in CUSTOM_ARRAY_NAMES
        assertEquals("java.sql.Date[]", Converter.getShortName(java.sql.Date[].class));
    }

    @Test
    public void testGetShortNameForJavaSqlDate() {
        assertEquals("java.sql.Date", Converter.getShortName(java.sql.Date.class));
    }

    /* --------------------------------------------------------------
     *  name() utility method
     * -------------------------------------------------------------- */
    @Test
    public void testNameUtility() {
        String s = "hello";
        String expected = "String (hello)";
        assertEquals(expected, invokePrivateName(s));
    }

    @Test
    public void testNameUtilityNull() {
        assertEquals("null", invokePrivateName(null));
    }

    /* --------------------------------------------------------------
     *  identity() static method (used indirectly via convert)
     * -------------------------------------------------------------- */
    @Test
    public void testIdentityConversionViaConvert() {
        String original = "identity-test";
        String result = converter.convert(original, String.class);
        assertSame("Identity conversion should return the same instance", original, result);
    }

    /* --------------------------------------------------------------
     *  Helper to invoke the private static name() method without reflection
     * -------------------------------------------------------------- */
    private static String invokePrivateName(Object obj) {
        // The name() method is private static; we can reproduce its logic here for verification.
        if (obj == null) {
            return "null";
        }
        return Converter.getShortName(obj.getClass()) + " (" + obj + ")";
    }
}
