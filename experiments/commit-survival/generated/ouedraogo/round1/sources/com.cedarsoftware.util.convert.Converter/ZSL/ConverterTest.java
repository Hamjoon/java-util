package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

public class ConverterTest {

    private Converter converter;

    /**
     * Minimal implementation of {@link ConverterOptions} that provides
     * an empty overrides map and relies on default methods for everything else.
     */
    private static class SimpleOptions implements ConverterOptions {
        @Override
        public Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> getConverterOverrides() {
            return Collections.emptyMap();
        }
    }

    @Before
    public void setUp() {
        converter = new Converter(new SimpleOptions());
    }

    /* -------------------------------------------------
     *  Basic conversion tests (String ↔ Integer ↔ String)
     * ------------------------------------------------- */

    @Test
    public void testConvertStringToInteger() {
        Integer result = converter.convert("123", Integer.class);
        assertEquals(Integer.valueOf(123), result);
    }

    @Test
    public void testConvertIntegerToString() {
        String result = converter.convert(456, String.class);
        assertEquals("456", result);
    }

    @Test
    public void testConvertMapToIntegerUsingValueKey() {
        Map<String, Object> map = new HashMap<>();
        map.put(Converter.VALUE, "789"); // "_v" is the constant used by the library
        Integer result = converter.convert(map, Integer.class);
        assertEquals(Integer.valueOf(789), result);
    }

    @Test
    public void testConvertNullToWrapperReturnsNull() {
        // Converting a null value to a wrapper type should return null (the library stores null in the map)
        assertNull(converter.convert(null, Integer.class));
        assertNull(converter.convert(null, String.class));
    }

    /* -------------------------------------------------
     *  Direct conversion support queries
     * ------------------------------------------------- */

    @Test
    public void testIsDirectConversionSupportedForKnownPair() {
        assertTrue(converter.isDirectConversionSupportedFor(String.class, Integer.class));
        assertTrue(converter.isDirectConversionSupportedFor(Integer.class, String.class));
    }

    @Test
    public void testIsDirectConversionSupportedForUnknownPair() {
        // There is no built‑in conversion from String to Thread
        assertFalse(converter.isDirectConversionSupportedFor(String.class, Thread.class));
    }

    /* -------------------------------------------------
     *  Inherited conversion support queries
     * ------------------------------------------------- */

    @Test
    public void testIsConversionSupportedForInheritedPair() {
        // AtomicInteger extends Number; conversion to Long is defined directly,
        // but conversion to Integer is also defined directly.
        assertTrue(converter.isConversionSupportedFor(AtomicInteger.class, Integer.class));
        // Conversion from Integer (Number) to Long is directly supported
        assertTrue(converter.isConversionSupportedFor(Integer.class, Long.class));
    }

    @Test
    public void testIsConversionSupportedForUnsupportedPair() {
        // No conversion path from String to java.util.concurrent.Future
        assertFalse(converter.isConversionSupportedFor(String.class, java.util.concurrent.Future.class));
    }

    /* -------------------------------------------------
     *  Adding a custom conversion and overriding defaults
     * ------------------------------------------------- */

    @Test
    public void testAddCustomConversionOverridesExisting() {
        // Define a custom conversion from String to Boolean that always returns true
        Convert<Boolean> alwaysTrue = (from, conv) -> Boolean.TRUE;

        // Verify the default conversion would normally interpret "false" as false
        Boolean before = converter.convert("false", Boolean.class);
        assertFalse(before);

        // Add the custom conversion (overwrites the factory conversion)
        converter.addConversion(String.class, Boolean.class, alwaysTrue);

        // Now the conversion must use the custom logic
        Boolean after = converter.convert("false", Boolean.class);
        assertTrue(after);
    }

    /* -------------------------------------------------
     *  Retrieval of supported conversion maps
     * ------------------------------------------------- */

    @Test
    public void testGetSupportedConversionsContainsKnownEntry() {
        Map<String, Set<String>> supported = converter.getSupportedConversions();
        // The map should contain an entry for String → Integer
        assertTrue(supported.containsKey("String"));
        assertTrue(supported.get("String").contains("Integer"));
        // And also for Integer → String
        assertTrue(supported.get("Integer").contains("String"));
    }

    @Test
    public void testAllSupportedConversionsStructure() {
        Map<Class<?>, Set<Class<?>>> all = converter.allSupportedConversions();
        // Verify that the key set includes String.class
        assertTrue(all.containsKey(String.class));
        // Verify that the target set for String includes Integer.class
        assertTrue(all.get(String.class).contains(Integer.class));
        // Verify that the target set for Integer includes String.class
        assertTrue(all.get(Integer.class).contains(String.class));
    }

    /* -------------------------------------------------
     *  Options accessor
     * ------------------------------------------------- */

    @Test
    public void testGetOptionsReturnsProvidedInstance() {
        ConverterOptions options = new SimpleOptions();
        Converter custom = new Converter(options);
        assertSame(options, custom.getOptions());
    }

    /* -------------------------------------------------
     *  Helper method behavior verification
     * ------------------------------------------------- */

    @Test
    public void testGetShortNameSpecialCaseForSqlDate() {
        // java.sql.Date should return the fully‑qualified name, not the simple name
        String shortName = Converter.getShortName(java.sql.Date.class);
        assertEquals("java.sql.Date", shortName);
    }

    @Test
    public void testGetShortNameRegularClass() {
        String shortName = Converter.getShortName(String.class);
        assertEquals("String", shortName);
    }

    /* -------------------------------------------------
     *  Unsupported conversion handling
     * ------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConvertUnsupportedThrowsException() {
        // There is no conversion from String to Thread
        converter.convert("test", Thread.class);
    }

    /* -------------------------------------------------
     *  Primitive target handling (int primitive)
     * ------------------------------------------------- */

    @Test
    public void testConvertStringToPrimitiveInt() {
        // Primitive target types are internally converted to their wrapper equivalents.
        int result = converter.convert("321", int.class);
        assertEquals(321, result);
    }

    @Test
    public void testConvertBooleanToPrimitiveByte() {
        // true → (byte)1, false → (byte)0
        byte trueByte = converter.convert(Boolean.TRUE, byte.class);
        byte falseByte = converter.convert(Boolean.FALSE, byte.class);
        assertEquals((byte) 1, trueByte);
        assertEquals((byte) 0, falseByte);
    }
}
