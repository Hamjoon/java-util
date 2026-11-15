package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Converter}.
 *
 * The suite covers:
 * <ul>
 *   <li>All public methods (including static helpers)</li>
 *   <li>Typical use‑cases (basic conversions, collection handling, enum sets)</li>
 *   <li>Edge cases (null handling, primitive defaults, unsupported conversions)</li>
 *   <li>Error scenarios (illegal arguments, custom conversion overrides)</li>
 * </ul>
 */
public class ConverterTest {

    /** Simple {@link ConverterOptions} implementation used for all tests. */
    private static class SimpleOptions implements ConverterOptions {
        @Override public ZoneId getZoneId() { return null; }
        @Override public Locale getLocale() { return null; }
        @Override public java.nio.charset.Charset getCharset() { return null; }
        @Override public ClassLoader getClassLoader() { return null; }
        @Override public <T> T getCustomOption(String name) { return null; }
        @Override public TimeZone getTimeZone() { return null; }
        @Override public Character trueChar() { return null; }
        @Override public Character falseChar() { return null; }
        @Override public Map<Converter.ConversionPair, Convert<?>> getConverterOverrides() {
            return Collections.emptyMap();
        }
    }

    private Converter converter;

    @Before
    public void setUp() {
        converter = new Converter(new SimpleOptions());
    }

    /*======================================================================
     *  Public instance methods
     *====================================================================*/

    @Test
    public void testConstructorAndGetOptions() {
        SimpleOptions options = new SimpleOptions();
        Converter c = new Converter(options);
        assertSame("getOptions() must return the options supplied to the constructor",
                options, c.getOptions());
    }

    @Test
    public void testConvertStringToInteger() {
        Integer result = converter.convert("123", Integer.class);
        assertEquals(Integer.valueOf(123), result);
    }

    @Test
    public void testConvertStringToLong() {
        Long result = converter.convert("9876543210", Long.class);
        assertEquals(Long.valueOf(9876543210L), result);
    }

    @Test
    public void testConvertNullToPrimitiveDefaults() {
        int i = converter.convert(null, int.class);
        boolean b = converter.convert(null, boolean.class);
        char c = converter.convert(null, char.class);
        double d = converter.convert(null, double.class);

        assertEquals(0, i);
        assertFalse(b);
        assertEquals('\0', c);
        assertEquals(0.0, d, 0.0);
    }

    @Test
    public void testConvertNullToObjectReturnsNull() {
        String s = converter.convert(null, String.class);
        Date d = converter.convert(null, Date.class);
        assertNull(s);
        assertNull(d);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertUnsupportedThrowsException() {
        // Thread has no built‑in conversion from String
        converter.convert("some", Thread.class);
    }

    @Test
    public void testAddCustomConversionOverridesBuiltIn() {
        // Register a conversion from String to Thread (custom)
        Converter.addConversion(String.class, Thread.class,
                (from, conv) -> {
                    Thread t = new Thread();
                    t.setName((String) from);
                    return t;
                });

        Thread t = converter.convert("myThread", Thread.class);
        assertNotNull(t);
        assertEquals("myThread", t.getName());
    }

    @Test
    public void testConvertArrayToList() {
        Integer[] source = new Integer[]{1, 2, 3};
        @SuppressWarnings("unchecked")
        List<Integer> list = (List<Integer>) converter.convert(source, List.class);
        assertNotNull(list);
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testConvertArrayToEnumSet() {
        // Simple enum for the test
        enum Day { MONDAY, TUESDAY, WEDNESDAY }

        Object[] source = new Object[]{ "MONDAY", Day.WEDNESDAY };
        @SuppressWarnings("unchecked")
        EnumSet<Day> set = (EnumSet<Day>) (Object) converter.convert(source, Day.class);
        assertNotNull(set);
        assertTrue(set.contains(Day.MONDAY));
        assertTrue(set.contains(Day.WEDNESDAY));
        assertEquals(2, set.size());
    }

    @Test
    public void testIsSimpleTypeConversionSupported() {
        assertTrue("String→Integer should be supported",
                converter.isSimpleTypeConversionSupported(String.class, Integer.class));
        assertFalse("String[]→Integer[] is a collection conversion, not simple",
                converter.isSimpleTypeConversionSupported(String[].class, Integer[].class));
        assertFalse("List→Set is a collection conversion, not simple",
                converter.isSimpleTypeConversionSupported(List.class, Set.class));
    }

    @Test
    public void testIsConversionSupportedForArrayComponents() {
        assertTrue("String[]→Integer[] should be supported because component conversion exists",
                converter.isConversionSupportedFor(String[].class, Integer[].class));
        assertFalse("String[]→Thread[] is not supported (no component conversion)",
                converter.isConversionSupportedFor(String[].class, Thread[].class));
    }

    @Test
    public void testIsCollectionConversionSupportedVariousScenarios() {
        // array → collection
        assertTrue(Converter.isCollectionConversionSupported(Integer[].class, List.class));

        // collection → array
        assertTrue(Converter.isCollectionConversionSupported(List.class, String[].class));

        // enum set → collection
        assertTrue(Converter.isCollectionConversionSupported(EnumSet.class, Set.class));

        // source not collection/array
        assertFalse(Converter.isCollectionConversionSupported(String.class, List.class));

        // target EnumSet should throw IllegalArgumentException
        try {
            Converter.isCollectionConversionSupported(String[].class, EnumSet.class);
            fail("Expected IllegalArgumentException when target is EnumSet.class");
        } catch (IllegalArgumentException ignored) {
        }
    }

    /*======================================================================
     *  Public static helper methods
     *====================================================================*/

    @Test
    public void testPairCreatesDistinctInstances() {
        Converter.ConversionPair p1 = Converter.pair(String.class, Integer.class);
        Converter.ConversionPair p2 = Converter.pair(String.class, Integer.class);
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertNotSame(p1, p2); // distinct objects
    }

    @Test
    public void testGetShortNameForArrayTypes() {
        assertEquals("int[]", Converter.getShortName(int[].class));
        assertEquals("String[]", Converter.getShortName(String[].class));
        // custom name for java.sql.Date[]
        assertEquals("java.sql.Date[]",
                Converter.getShortName(java.sql.Date[].class));
    }

    @Test
    public void testGetSupportedConversionsContainsKnownMapping() {
        Map<String, Set<String>> map = Converter.getSupportedConversions();
        Set<String> targets = map.get("String");
        assertNotNull("String should appear as a source type", targets);
        assertTrue("String→Integer must be listed",
                targets.contains("Integer"));
        assertTrue("String→Long must be listed",
                targets.contains("Long"));
    }

    @Test
    public void testAllSupportedConversionsStructure() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        Set<Class<?>> targets = map.get(String.class);
        assertNotNull(targets);
        assertTrue(targets.contains(Integer.class));
        assertTrue(targets.contains(Long.class));
    }

    @Test
    public void testIdentityReturnsSameInstance() {
        String original = "identity";
        String result = Converter.identity(original, converter);
        assertSame(original, result);
    }

    @Test
    public void testUnsupportedConversionReturnsNullViaHelper() {
        // The private helper `unsupported` returns null; we can reach it indirectly
        // by requesting a conversion that is explicitly marked UNSUPPORTED.
        // Map → Map is UNSUPPORTED (see static init block).
        Map<String, Integer> src = new HashMap<>();
        src.put("a", 1);
        @SuppressWarnings("unchecked")
        Map<String, Integer> result = converter.convert(src, Map.class);
        // The conversion should fall back to inheritance lookup and finally throw;
        // however, the internal UNSUPPORTED value is used only when a pair is found.
        // To test the behaviour we ensure that the conversion throws the expected exception.
        try {
            converter.convert(src, Map.class);
            fail("Expected IllegalArgumentException for unsupported Map→Map conversion");
        } catch (IllegalArgumentException ignored) {
        }
    }
}
