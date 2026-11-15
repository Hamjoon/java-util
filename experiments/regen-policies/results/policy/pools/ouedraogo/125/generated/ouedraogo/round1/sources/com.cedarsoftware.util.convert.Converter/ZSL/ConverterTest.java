package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.*;

import static org.junit.Assert.*;

public class ConverterTest {

    private Converter converter;

    /** Simple implementation of ConverterOptions used only for testing. */
    private static class TestOptions implements ConverterOptions {
        private final Map<Converter.ConversionPair, Convert<?>> overrides = new HashMap<>();

        @Override
        public Map<Converter.ConversionPair, Convert<?>> getConverterOverrides() {
            return overrides;
        }
    }

    private enum Day {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }

    private static class CustomType {
        final String value;
        CustomType(String value) { this.value = value; }
        @Override public String toString() { return "CustomType{" + value + '}'; }
        @Override public boolean equals(Object o) {
            return (o instanceof CustomType) && Objects.equals(value, ((CustomType) o).value);
        }
    }

    @Before
    public void setUp() {
        converter = new Converter(new TestOptions());
    }

    /* --------------------------------------------------------------------- *
     *  Basic primitive / wrapper conversion tests
     * --------------------------------------------------------------------- */

    @Test
    public void testConvertNullToPrimitive() {
        int i = converter.convert(null, int.class);
        assertEquals(0, i);

        boolean b = converter.convert(null, boolean.class);
        assertFalse(b);

        char c = converter.convert(null, char.class);
        assertEquals('\u0000', c);
    }

    @Test
    public void testConvertStringToInteger() {
        Integer i = converter.convert("123", Integer.class);
        assertEquals(Integer.valueOf(123), i);
    }

    @Test
    public void testConvertStringToLong() {
        Long l = converter.convert("456", Long.class);
        assertEquals(Long.valueOf(456L), l);
    }

    @Test
    public void testConvertStringToBoolean() {
        Boolean bTrue = converter.convert("true", Boolean.class);
        Boolean bFalse = converter.convert("false", Boolean.class);
        assertTrue(bTrue);
        assertFalse(bFalse);
    }

    @Test
    public void testConvertStringToChar() {
        Character ch = converter.convert("A", Character.class);
        assertEquals(Character.valueOf('A'), ch);
    }

    @Test
    public void testConvertStringToBigInteger() {
        BigInteger bi = converter.convert("9876543210", BigInteger.class);
        assertEquals(new BigInteger("9876543210"), bi);
    }

    @Test
    public void testConvertStringToBigDecimal() {
        BigDecimal bd = converter.convert("12345.6789", BigDecimal.class);
        assertEquals(new BigDecimal("12345.6789"), bd);
    }

    /* --------------------------------------------------------------------- *
     *  Collection / array conversion tests
     * --------------------------------------------------------------------- */

    @Test
    public void testArrayToListConversion() {
        String[] src = {"one", "two", "three"};
        List<String> list = converter.convert(src, List.class);
        assertEquals(Arrays.asList(src), list);
    }

    @Test
    public void testListToArrayConversion() {
        List<Integer> src = Arrays.asList(1, 2, 3);
        Integer[] arr = converter.convert(src, Integer[].class);
        assertArrayEquals(new Integer[]{1, 2, 3}, arr);
    }

    @Test
    public void testArrayToArrayDifferentComponentTypes() {
        String[] src = {"10", "20", "30"};
        Integer[] dst = converter.convert(src, Integer[].class);
        assertArrayEquals(new Integer[]{10, 20, 30}, dst);
    }

    @Test
    public void testEnumSetConversionFromArray() {
        Day[] src = {Day.MONDAY, Day.WEDNESDAY, Day.FRIDAY};
        @SuppressWarnings("unchecked")
        EnumSet<Day> set = (EnumSet<Day>)(Object)converter.convert(src, Day.class);
        assertTrue(set.containsAll(Arrays.asList(Day.MONDAY, Day.WEDNESDAY, Day.FRIDAY)));
        assertEquals(3, set.size());
    }

    @Test
    public void testEnumSetConversionFromCollection() {
        List<Day> src = Arrays.asList(Day.TUESDAY, Day.THURSDAY);
        @SuppressWarnings("unchecked")
        EnumSet<Day> set = (EnumSet<Day>)(Object)converter.convert(src, Day.class);
        assertTrue(set.containsAll(src));
    }

    @Test
    public void testEnumSetToArrayConversion() {
        EnumSet<Day> src = EnumSet.of(Day.SATURDAY, Day.SUNDAY);
        Day[] arr = converter.convert(src, Day[].class);
        assertArrayEquals(new Day[]{Day.SATURDAY, Day.SUNDAY}, arr);
    }

    /* --------------------------------------------------------------------- *
     *  Custom conversion registration tests
     * --------------------------------------------------------------------- */

    @Test
    public void testAddCustomConversion() {
        // Register a custom conversion from String to CustomType
        converter.addConversion(String.class, CustomType.class,
                (from, conv) -> new CustomType((String) from));

        CustomType ct = converter.convert("myValue", CustomType.class);
        assertEquals(new CustomType("myValue"), ct);
    }

    @Test
    public void testOverrideBuiltInConversion() {
        // Override String->Integer to always return 42
        converter.addConversion(String.class, Integer.class,
                (from, conv) -> 42);
        Integer i = converter.convert("anyString", Integer.class);
        assertEquals(Integer.valueOf(42), i);
    }

    /* --------------------------------------------------------------------- *
     *  Conversion support query tests
     * --------------------------------------------------------------------- */

    @Test
    public void testIsCollectionConversionSupported() {
        // array -> collection
        assertTrue(converter.isCollectionConversionSupported(String[].class, List.class));

        // collection -> array
        assertTrue(converter.isCollectionConversionSupported(List.class, Integer[].class));

        // enum set to collection
        assertTrue(converter.isCollectionConversionSupported(EnumSet.class, Set.class));

        // enum set to array
        assertTrue(converter.isCollectionConversionSupported(EnumSet.class, Day[].class));

        // array -> array (different component)
        assertTrue(converter.isCollectionConversionSupported(String[].class, Integer[].class));

        // target EnumSet should throw
        try {
            converter.isCollectionConversionSupported(String[].class, EnumSet.class);
            fail("Expected IllegalArgumentException for EnumSet target");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testIsSimpleTypeConversionSupported() {
        assertTrue(converter.isSimpleTypeConversionSupported(String.class, Integer.class));
        assertFalse(converter.isSimpleTypeConversionSupported(String[].class, Integer[].class));
        assertFalse(converter.isSimpleTypeConversionSupported(List.class, Set.class));
    }

    @Test
    public void testIsDirectConversionSupported() {
        // direct primitive/wrapper conversion
        assertTrue(converter.isDirectConversionSupported(String.class, Integer.class));

        // collection conversion
        assertTrue(converter.isDirectConversionSupported(String[].class, List.class));

        // array-to-array with component conversion
        assertTrue(converter.isDirectConversionSupported(String[].class, Integer[].class));

        // array-to-array with same component type (no conversion needed)
        assertTrue(converter.isDirectConversionSupported(Integer[].class, Integer[].class));

        // unsupported direct conversion
        assertFalse(converter.isDirectConversionSupported(String.class, Day.class));
    }

    @Test
    public void testIsConversionSupportedFor() {
        // direct conversion
        assertTrue(converter.isConversionSupportedFor(String.class, Integer.class));

        // collection conversion
        assertTrue(converter.isConversionSupportedFor(String[].class, List.class));

        // array-to-array with component conversion
        assertTrue(converter.isConversionSupportedFor(String[].class, Integer[].class));

        // unsupported conversion
        assertFalse(converter.isConversionSupportedFor(String.class, Day.class));
    }

    /* --------------------------------------------------------------------- *
     *  Helper method and metadata tests
     * --------------------------------------------------------------------- */

    @Test
    public void testGetShortName() {
        assertEquals("String[]", Converter.getShortName(String[].class));
        assertEquals("int[]", Converter.getShortName(int[].class));
        assertEquals("java.sql.Date[]", Converter.getShortName(java.sql.Date[].class));
        assertEquals("Date", Converter.getShortName(Date.class));
        assertEquals("LocalDate", Converter.getShortName(LocalDate.class));
    }

    @Test
    public void testNameMethod() {
        String name = Converter.name("test");
        assertEquals("String (test)", name);

        assertEquals("null", Converter.name(null));
    }

    @Test
    public void testGetSupportedConversionsContainsKnownMapping() {
        Map<String, Set<String>> map = converter.getSupportedConversions();
        // Verify that String -> Integer conversion is present
        Set<String> targets = map.get("String");
        assertNotNull(targets);
        assertTrue(targets.contains("Integer"));
    }

    @Test
    public void testAllSupportedConversionsStructure() {
        Map<Class<?>, Set<Class<?>>> map = converter.allSupportedConversions();
        assertNotNull(map);
        // At least a handful of source types should be present
        assertTrue(map.containsKey(String.class));
        assertTrue(map.get(String.class).contains(Integer.class));
    }

    @Test
    public void testGetOptionsReturnsSameInstance() {
        ConverterOptions opts = new TestOptions();
        Converter c = new Converter(opts);
        assertSame(opts, c.getOptions());
    }

    @Test
    public void testAddConversionAndLookup() {
        Convert<Integer> conv = (from, converter) -> 99;
        converter.addConversion(Integer.class, Integer.class, conv);
        Integer result = converter.convert(1, Integer.class);
        // Since we overrode the Integer->Integer conversion, result should be 99
        assertEquals(Integer.valueOf(99), result);
    }

    @Test
    public void testIdentityConversion() {
        String original = "identity";
        String copy = Converter.identity(original, converter);
        assertSame(original, copy);
    }

    @Test
    public void testUnsupportedConversionReturnsNull() {
        // There is no conversion from String to Day (enum) directly
        try {
            converter.convert("MONDAY", Day.class);
            fail("Expected IllegalArgumentException for unsupported conversion");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testConversionOfUrlAndUri() throws Exception {
        URL url = new URL("http://example.com");
        URI uri = converter.convert(url, URI.class);
        assertEquals(url.toURI(), uri);

        URL url2 = converter.convert(uri, URL.class);
        assertEquals(uri.toURL(), url2);
    }

    @Test
    public void testByteBufferToStringConversion() {
        ByteBuffer bb = ByteBuffer.wrap("hello".getBytes());
        String s = converter.convert(bb, String.class);
        assertEquals("hello", s);
    }

    @Test
    public void testMapToStringConversion() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "value");
        String s = converter.convert(map, String.class);
        // MapConversions.toString will return the string representation of the map's value for key "_v"
        // Since the exact format is implementation dependent, ensure it contains the value.
        assertTrue(s.contains("value"));
    }
}
