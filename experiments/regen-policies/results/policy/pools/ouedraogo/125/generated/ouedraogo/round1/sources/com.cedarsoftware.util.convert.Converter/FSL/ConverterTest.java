package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link Converter}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>basic {@code convert} functionality (null handling, primitive wrappers, collections)</li>
 *   <li>custom conversion registration via {@code addConversion}</li>
 *   <li>the various “is…Supported” query methods</li>
 *   <li>utility methods {@code getShortName}, {@code name}, {@code identity} and {@code unsupported}</li>
 *   <li>the introspection APIs {@code allSupportedConversions} and {@code getSupportedConversions}</li>
 * </ul>
 * </p>
 */
public class ConverterTest {

    private Converter converter;

    /** Minimal {@link ConverterOptions} implementation – all defaults are {@code null} or empty. */
    private static class TestOptions implements ConverterOptions {
        @Override public Map<Converter.ConversionPair, Convert<?>> getConverterOverrides() {
            return new HashMap<>();
        }
    }

    @Before
    public void setUp() {
        converter = new Converter(new TestOptions());
    }

    /* --------------------------------------------------------------------- */
    /*  Basic conversion tests                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testConvertNullToPrimitives() {
        // null → int  => 0
        assertEquals(0, (int) converter.convert(null, int.class));
        // null → boolean => false
        assertEquals(false, (boolean) converter.convert(null, boolean.class));
        // null → char => '\0'
        assertEquals('\0', (char) converter.convert(null, char.class));
    }

    @Test
    public void testConvertNullToObject() {
        assertNull(converter.convert(null, String.class));
        assertNull(converter.convert(null, UUID.class));
    }

    @Test
    public void testSimpleStringToIntegerConversion() {
        Integer i = converter.convert("123", Integer.class);
        assertEquals(Integer.valueOf(123), i);
    }

    @Test
    public void testStringToBigDecimalConversion() {
        BigDecimal bd = converter.convert("12.34", BigDecimal.class);
        assertEquals(new BigDecimal("12.34"), bd);
    }

    @Test
    public void testMapToUUIDConversion() {
        Map<String, Object> map = new HashMap<>();
        map.put("mostSigBits", 0x0123456789ABCDEFL);
        map.put("leastSigBits", 0xFEDCBA9876543210L);
        UUID uuid = converter.convert(map, UUID.class);
        assertEquals(new UUID(0x0123456789ABCDEFL, 0xFEDCBA9876543210L), uuid);
    }

    /* --------------------------------------------------------------------- */
    /*  Custom conversion registration                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddCustomConversionOverridesBuiltIn() {
        // custom conversion: String → Integer returns the length of the string
        converter.addConversion(String.class, Integer.class,
                (from, conv) -> ((String) from).length());

        Integer result = converter.convert("abcd", Integer.class);
        assertEquals(Integer.valueOf(4), result);
    }

    @Test
    public void testAddNewConversionForPreviouslyUnsupportedPair() {
        // there is no built‑in conversion from URI → URL (it exists, but we will replace it)
        converter.addConversion(URI.class, URL.class,
                (from, conv) -> {
                    try {
                        return new URL(((URI) from).toString());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        URI uri = URI.create("http://example.com");
        URL url = converter.convert(uri, URL.class);
        assertEquals("http://example.com", url.toString());
    }

    /* --------------------------------------------------------------------- */
    /*  Collection‑related conversion queries                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsCollectionConversionSupported_ArrayToList() {
        assertTrue(converter.isCollectionConversionSupported(String[].class, List.class));
    }

    @Test
    public void testIsCollectionConversionSupported_ListToArray() {
        assertTrue(converter.isCollectionConversionSupported(List.class, Integer[].class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsCollectionConversionSupported_EnumSetTargetThrows() {
        // target is raw EnumSet – the API requires an enum class instead
        converter.isCollectionConversionSupported(String[].class, EnumSet.class);
    }

    @Test
    public void testIsCollectionConversionSupported_ArrayToArrayDifferentComponent() {
        assertTrue(converter.isCollectionConversionSupported(String[].class, Integer[].class));
    }

    @Test
    public void testIsCollectionConversionSupported_ArrayToArraySameComponent() {
        // same component type – conversion is a no‑op and therefore not “supported” by this helper
        assertFalse(converter.isCollectionConversionSupported(Integer[].class, Integer[].class));
    }

    /* --------------------------------------------------------------------- */
    /*  Simple‑type conversion queries                                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsSimpleTypeConversionSupported_Positive() {
        assertTrue(converter.isSimpleTypeConversionSupported(String.class, Integer.class));
    }

    @Test
    public void testIsSimpleTypeConversionSupported_Negative() {
        assertFalse(converter.isSimpleTypeConversionSupported(List.class, Set.class));
        assertFalse(converter.isSimpleTypeConversionSupported(String[].class, Integer[].class));
    }

    /* --------------------------------------------------------------------- */
    /*  Direct conversion queries                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsDirectConversionSupported_PrimitiveWrapper() {
        assertTrue(converter.isDirectConversionSupported(String.class, Integer.class));
    }

    @Test
    public void testIsDirectConversionSupported_ArrayComponent() {
        // conversion is supported only if component conversion is also direct
        assertTrue(converter.isDirectConversionSupported(String[].class, Integer[].class));
    }

    @Test
    public void testIsDirectConversionSupported_ArraySameComponent() {
        // same component type – no conversion needed → not considered “direct”
        assertFalse(converter.isDirectConversionSupported(Integer[].class, Integer[].class));
    }

    /* --------------------------------------------------------------------- */
    /*  Full conversion support queries                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsConversionSupportedFor_ArrayToArray() {
        assertTrue(converter.isConversionSupportedFor(String[].class, Integer[].class));
    }

    @Test
    public void testIsConversionSupportedFor_ArrayToArray_ComponentUnsupported() {
        // there is no conversion from String to Byte (so String[] → Byte[] must be false)
        assertFalse(converter.isConversionSupportedFor(String[].class, Byte[].class));
    }

    @Test
    public void testIsConversionSupportedFor_SimpleTypes() {
        assertTrue(converter.isConversionSupportedFor(String.class, Integer.class));
        assertFalse(converter.isConversionSupportedFor(String.class, UUID.class));
    }

    /* --------------------------------------------------------------------- */
    /*  Actual conversion using collection helpers                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testConvertArrayToList() {
        String[] source = {"a", "b", "c"};
        List<String> list = converter.convert(source, List.class);
        assertEquals(Arrays.asList("a", "b", "c"), list);
    }

    @Test
    public void testConvertListToArray() {
        List<Integer> source = Arrays.asList(5, 6, 7);
        Integer[] array = converter.convert(source, Integer[].class);
        assertArrayEquals(new Integer[]{5, 6, 7}, array);
    }

    @Test
    public void testConvertArrayToArrayDifferentComponent() {
        String[] src = {"10", "20"};
        Integer[] dst = converter.convert(src, Integer[].class);
        assertArrayEquals(new Integer[]{10, 20}, dst);
    }

    /* --------------------------------------------------------------------- */
    /*  Utility methods                                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetShortName_ArrayTypes() {
        assertEquals("byte[]", Converter.getShortName(byte[].class));
        assertEquals("char[]", Converter.getShortName(char[].class));
        assertEquals("java.sql.Date[]", Converter.getShortName(java.sql.Date[].class));
    }

    @Test
    public void testGetShortName_SpecialClasses() {
        assertEquals("Date", Converter.getShortName(Date.class));
        assertEquals("LocalDate", Converter.getShortName(LocalDate.class));
    }

    @Test
    public void testNameMethod() {
        String s = "hello";
        String expected = "String (hello)";
        assertEquals(expected, Converter.name(s));

        assertEquals("null", Converter.name(null));
    }

    @Test
    public void testIdentityMethod() {
        String original = "identity";
        String result = Converter.identity(original, converter);
        assertSame(original, result);
    }

    @Test
    public void testUnsupportedMethod() {
        String result = Converter.identity(null, converter); // identity returns null for null
        assertNull(Converter.unsupported(result, converter));
    }

    /* --------------------------------------------------------------------- */
    /*  Introspection APIs                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAllSupportedConversionsContainsKnownMapping() {
        Map<Class<?>, Set<Class<?>>> map = converter.allSupportedConversions();
        // String → Integer must be present
        assertTrue(map.containsKey(String.class));
        assertTrue(map.get(String.class).contains(Integer.class));
        // UUID → String also must be present
        assertTrue(map.get(UUID.class).contains(String.class));
    }

    @Test
    public void testGetSupportedConversionsContainsKnownMapping() {
        Map<String, Set<String>> map = converter.getSupportedConversions();
        // key names are short names
        assertTrue(map.containsKey("String"));
        Set<String> targets = map.get("String");
        assertTrue(targets.contains("Integer"));
        assertTrue(targets.contains("UUID"));
    }

    @Test
    public void testGetOptionsReturnsProvidedOptions() {
        ConverterOptions opts = new TestOptions();
        Converter c = new Converter(opts);
        assertSame(opts, c.getOptions());
    }
}
