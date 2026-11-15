package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.util.*;
import java.time.*;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;

/**
 * Comprehensive JUnit4 tests for {@link Converter}.
 *
 * The test class lives in the same package as {@link Converter} so that
 * package‑private members (e.g., {@code pair()}, {@code VALUE},
 * {@code ClassLevel} and other helpers) are directly accessible.
 */
public class ConverterTest {

    private Converter converter;

    /**
     * Minimal {@link ConverterOptions} implementation – all default methods are used.
     */
    private static final ConverterOptions DEFAULT_OPTIONS = new ConverterOptions() {
        // No overrides – defaults (null) are sufficient for the tests.
    };

    @Before
    public void setUp() {
        converter = new Converter(DEFAULT_OPTIONS);
    }

    @After
    public void tearDown() {
        converter = null;
    }

    /* --------------------------------------------------------------
     *  Tests for the public convert(...) API
     * -------------------------------------------------------------- */

    @Test
    public void testConvertNullToReferenceTypes() {
        // Void → Integer (should be null)
        Integer i = converter.convert(null, Integer.class);
        assertNull(i);

        // Void → Byte (should be null)
        Byte b = converter.convert(null, Byte.class);
        assertNull(b);
    }

    @Test
    public void testConvertNullToPrimitiveTypes() {
        // Void → int (primitive) – should return zero
        Integer i = converter.convert(null, int.class);
        assertEquals(Integer.valueOf(0), i);

        // Void → boolean (primitive) – should return false
        Boolean bool = converter.convert(null, boolean.class);
        assertEquals(Boolean.FALSE, bool);
    }

    @Test
    public void testIdentityConversionReturnsSameInstance() {
        Integer original = 123;
        Integer result = converter.convert(original, Integer.class);
        // The conversion for Integer → Integer is the identity function.
        assertSame("Identity conversion should return the original instance", original, result);
    }

    @Test
    public void testStringToIntegerConversion() {
        Integer result = converter.convert("456", Integer.class);
        assertEquals(Integer.valueOf(456), result);
    }

    @Test
    public void testIntegerToStringConversion() {
        String result = converter.convert(789, String.class);
        assertEquals("789", result);
    }

    @Test
    public void testBooleanToByteConversion() {
        Byte trueByte = converter.convert(Boolean.TRUE, Byte.class);
        Byte falseByte = converter.convert(Boolean.FALSE, Byte.class);
        assertEquals(Byte.valueOf((byte)1), trueByte);
        assertEquals(Byte.valueOf((byte)0), falseByte);
    }

    @Test
    public void testMapToIntegerConversionUsingValueKey() {
        Map<String, Object> map = new HashMap<>();
        map.put(Converter.VALUE, "321"); // the special key used by MapConversions
        Integer result = converter.convert(map, Integer.class);
        assertEquals(Integer.valueOf(321), result);
    }

    @Test
    public void testMapSubclassConversionViaInheritance() {
        // TreeMap is a subclass of Map – inheritance lookup must succeed.
        TreeMap<String, Object> map = new TreeMap<>();
        map.put(Converter.VALUE, "654");
        Integer result = converter.convert(map, Integer.class);
        assertEquals(Integer.valueOf(654), result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnsupportedConversionThrowsException() {
        // There is no conversion from String to Thread in the factory DB.
        converter.convert("foo", Thread.class);
    }

    /* --------------------------------------------------------------
     *  Tests for conversion‑support query methods
     * -------------------------------------------------------------- */

    @Test
    public void testIsDirectConversionSupportedFor() {
        // Direct mapping exists: Integer → String
        assertTrue(converter.isDirectConversionSupportedFor(Integer.class, String.class));

        // Direct mapping does NOT exist: String → Thread
        assertFalse(converter.isDirectConversionSupportedFor(String.class, Thread.class));
    }

    @Test
    public void testIsConversionSupportedForDirectAndInherited() {
        // Direct conversion: String → Boolean
        assertTrue(converter.isConversionSupportedFor(String.class, Boolean.class));

        // Inherited conversion: TreeMap (subclass of Map) → String
        assertTrue(converter.isConversionSupportedFor(TreeMap.class, String.class));

        // Unsupported conversion
        assertFalse(converter.isConversionSupportedFor(String.class, Thread.class));
    }

    /* --------------------------------------------------------------
     *  Tests for adding custom conversions
     * -------------------------------------------------------------- */

    @Test
    public void testAddCustomConversionOverridesFactory() {
        // Override String → Integer to always return 42 regardless of input.
        Convert<Integer> custom = (from, conv) -> 42;
        converter.addConversion(String.class, Integer.class, custom);

        // Verify that the new conversion is used.
        Integer result = converter.convert("any value", Integer.class);
        assertEquals(Integer.valueOf(42), result);

        // The helper method should now report a direct conversion.
        assertTrue(converter.isDirectConversionSupportedFor(String.class, Integer.class));
    }

    @Test
    public void testAddConversionForNewPair() {
        // No existing conversion from BigInteger → URI; add one.
        Convert<URI> toUri = (from, conv) -> URI.create("urn:custom:" + ((BigInteger) from).toString(16));
        converter.addConversion(BigInteger.class, URI.class, toUri);

        BigInteger big = new BigInteger("255");
        URI uri = converter.convert(big, URI.class);
        assertEquals("urn:custom:ff", uri.toString());

        assertTrue(converter.isDirectConversionSupportedFor(BigInteger.class, URI.class));
    }

    /* --------------------------------------------------------------
     *  Tests for the auxiliary static helpers
     * -------------------------------------------------------------- */

    @Test
    public void testGetShortNameForSqlDate() {
        // java.sql.Date should return the fully‑qualified name.
        assertEquals("java.sql.Date", Converter.getShortName(java.sql.Date.class));
    }

    @Test
    public void testGetShortNameForRegularClass() {
        assertEquals("String", Converter.getShortName(String.class));
    }

    @Test
    public void testAllSupportedConversionsContainsExpectedEntry() {
        Map<Class<?>, Set<Class<?>>> all = converter.allSupportedConversions();
        // There must be a conversion from String to Integer.
        assertTrue("Map should contain key String.class", all.containsKey(String.class));
        assertTrue("String should be convertible to Integer", all.get(String.class).contains(Integer.class));
    }

    @Test
    public void testGetSupportedConversionsContainsExpectedEntry() {
        Map<String, Set<String>> nameMap = converter.getSupportedConversions();
        // The map should contain the string name "String" with a target "Integer".
        assertTrue(nameMap.containsKey("String"));
        assertTrue(nameMap.get("String").contains("Integer"));
    }

    @Test
    public void testClassLevelCompareToOrdering() {
        // level 0 (exact class) should be ordered before level 1 (superclass/interface)
        Converter.ClassLevel level0 = new Converter.ClassLevel(String.class, 0);
        Converter.ClassLevel level1 = new Converter.ClassLevel(Object.class, 1);
        assertTrue("Level 0 should be less than Level 1", level0.compareTo(level1) < 0);
        assertTrue("Level 1 should be greater than Level 0", level1.compareTo(level0) > 0);

        // Between same level, class types (class vs interface) affect ordering.
        Converter.ClassLevel ifaceLevel = new Converter.ClassLevel(Comparable.class, 1);
        // Object (class) should come before an interface at the same level.
        assertTrue("Class should precede interface at same level", level1.compareTo(ifaceLevel) < 0);
        assertTrue("Interface should follow class at same level", ifaceLevel.compareTo(level1) > 0);
    }

    /* --------------------------------------------------------------
     *  Tests for the getOptions() accessor
     * -------------------------------------------------------------- */

    @Test
    public void testGetOptionsReturnsProvidedInstance() {
        assertSame("Converter should return the options instance supplied at construction",
                DEFAULT_OPTIONS, converter.getOptions());
    }
}
