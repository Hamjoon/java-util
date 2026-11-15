package com.cedarsoftware.util.convert;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;

public class ConverterTest {

    private Converter converter;
    private ConverterOptions options;

    @Before
    public void setUp() {
        // Minimal stub implementation of ConverterOptions – only the method required for the constructor is needed.
        options = new ConverterOptions() {
            @Override public Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> getConverterOverrides() {
                return Collections.emptyMap();
            }
        };
        converter = new Converter(options);
    }

    /* ----------------------------------------------------------------------
     *  Public method list (for reference)
     * ----------------------------------------------------------------------
     *  public ConverterOptions getOptions()
     *  public <T> T convert(Object from, Class<T> toType)
     *  public boolean isConversionSupportedFor(Class<?> source, Class<?> target)
     *  public Map<Class<?>, Set<Class<?>>> allSupportedConversions()
     *  public Map<String, Set<String>> getSupportedConversions()
     *  public Convert<?> addConversion(Class<?> source, Class<?> target, Convert<?> conversionFunction)
     *  public Converter(ConverterOptions options)
     * ---------------------------------------------------------------------- */

    /*** BASIC TESTS ***/

    @Test
    public void testGetOptionsReturnsSameInstance() {
        assertSame("getOptions() should return the exact instance supplied to the constructor",
                options, converter.getOptions());
    }

    @Test
    public void testConvertStringToInteger() {
        Integer result = converter.convert("123", Integer.class);
        assertEquals("String \"123\" should convert to Integer 123", Integer.valueOf(123), result);
    }

    @Test
    public void testConvertIntegerToString() {
        String result = converter.convert(456, String.class);
        assertEquals("Integer 456 should convert to String \"456\"", "456", result);
    }

    @Test
    public void testConvertNullToObjectTarget() {
        String result = converter.convert(null, String.class);
        assertNull("Converting null to a reference type should yield null", result);
    }

    @Test
    public void testConvertNullToPrimitiveTarget() {
        int i = converter.convert(null, int.class);
        assertEquals("Converting null to primitive int should yield 0", 0, i);

        boolean b = converter.convert(null, boolean.class);
        assertFalse("Converting null to primitive boolean should yield false", b);

        char c = converter.convert(null, char.class);
        assertEquals("Converting null to primitive char should yield '\\0'",
                '\u0000', c);
    }

    @Test
    public void testConvertIntegerToPrimitiveInt() {
        int result = converter.convert(5, int.class);
        assertEquals("Integer 5 should convert to primitive int 5", 5, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertNullToTypeThrowsException() {
        // toType is null – must raise IllegalArgumentException
        converter.convert("123", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnsupportedConversionThrowsException() {
        // No conversion defined from Object -> Integer
        converter.convert(new Object(), Integer.class);
    }

    /*** EDGE‑CASE & EXCEPTION TESTS ***/

    @Test
    public void testIsConversionSupportedDirect() {
        // String -> Integer is a direct conversion defined in the factory DB
        assertTrue("Direct conversion String -> Integer should be supported",
                converter.isConversionSupportedFor(String.class, Integer.class));
    }

    @Test
    public void testIsConversionSupportedFalse() {
        // List -> Integer is not defined anywhere
        assertFalse("Unsupported conversion List -> Integer should return false",
                converter.isConversionSupportedFor(List.class, Integer.class));
    }

    @Test
    public void testAddCustomConversionOverridesFactory() {
        // Override the default String -> String conversion (identity) with a custom one
        Convert<String> custom = (from, conv) -> "custom-" + from;
        converter.addConversion(String.class, String.class, custom);

        String result = converter.convert("test", String.class);
        assertEquals("Custom conversion should be used instead of the factory identity",
                "custom-test", result);
    }

    @Test
    public void testAllSupportedConversionsContainsKnownEntry() {
        Map<Class<?>, Set<Class<?>>> map = converter.allSupportedConversions();

        // Verify that the map contains the source class String
        assertTrue("All supported conversions should contain key String.class", map.containsKey(String.class));

        // Verify that String can be converted to Integer (one of the many supported targets)
        Set<Class<?>> targets = map.get(String.class);
        assertTrue("String should be convertible to Integer", targets.contains(Integer.class));
    }

    @Test
    public void testGetSupportedConversionsContainsKnownEntry() {
        Map<String, Set<String>> map = converter.getSupportedConversions();

        // Verify that the String key is present (using the short name)
        assertTrue("Supported conversions (string map) should contain key \"String\"",
                map.containsKey("String"));

        // Verify that Integer appears in the target set for String
        Set<String> targets = map.get("String");
        assertTrue("String conversion target set should contain \"Integer\"",
                targets.contains("Integer"));
    }
}
