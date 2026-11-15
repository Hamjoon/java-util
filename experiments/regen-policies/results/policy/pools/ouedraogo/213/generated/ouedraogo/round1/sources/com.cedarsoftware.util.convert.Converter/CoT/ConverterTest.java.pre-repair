package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.time.ZoneId;
import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link Converter}.
 *
 * <p>This test class follows the requested steps:</p>
 * <ol>
 *   <li>Lists all public methods of {@link Converter}.</li>
 *   <li>Provides a basic test for each public method.</li>
 *   <li>Identifies edge‑cases and exception scenarios.</li>
 *   <li>Adds dedicated tests for those edge‑cases.</li>
 *   <li>Combines everything into a single test file delimited by the required markers.</li>
 * </ol>
 */
public class ConverterTest {

    /** Minimal {@link ConverterOptions} implementation required for constructing a {@link Converter}. */
    private static class StubOptions implements ConverterOptions {
        @Override public ZoneId getZoneId() { return null; }
        @Override public Locale getLocale() { return null; }
        @Override public java.nio.charset.Charset getCharset() { return null; }
        @Override public ClassLoader getClassLoader() { return null; }
        @Override public <T> T getCustomOption(String name) { return null; }
        @Override public TimeZone getTimeZone() { return null; }
        @Override public Character trueChar() { return null; }
        @Override public Character falseChar() { return null; }
        @Override public Map<Converter.ConversionPair, Convert<?>> getConverterOverrides() { return new HashMap<>(); }
    }

    private Converter converter;
    private StubOptions options;

    @Before
    public void setUp() {
        options = new StubOptions();
        converter = new Converter(options);
    }

    /* --------------------------------------------------------------
     *  1. List of public methods (for reference only – not executed)
     * --------------------------------------------------------------
     *
     * public ConverterOptions getOptions()
     * public <T> T convert(Object from, Class<T> toType)
     * public static ConversionPair pair(Class<?> source, Class<?> target)
     * public static boolean isCollectionConversionSupported(Class<?> sourceType, Class<?> target)
     * public boolean isSimpleTypeConversionSupported(Class<?> source, Class<?> target)
     * public boolean isConversionSupportedFor(Class<?> source, Class<?> target)
     * public static Map<Class<?>, Set<Class<?>>> allSupportedConversions()
     * public static Map<String, Set<String>> getSupportedConversions()
     * public static Convert<?> addConversion(Class<?> source, Class<?> target, Convert<?> conversionMethod)
     * public static <T> T identity(T from, Converter converter)
     *
     * -------------------------------------------------------------- */

    /* -------------------------- Basic Tests -------------------------- */

    @Test
    public void testGetOptions() {
        assertSame("Converter should return the options supplied at construction",
                options, converter.getOptions());
    }

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
    public void testConvertNullToPrimitiveIntReturnsZero() {
        int result = converter.convert(null, int.class);
        assertEquals(0, result);
    }

    @Test
    public void testConvertNullToPrimitiveBooleanReturnsFalse() {
        boolean result = converter.convert(null, boolean.class);
        assertFalse(result);
    }

    @Test
    public void testConvertNullToPrimitiveCharReturnsZeroChar() {
        char result = converter.convert(null, char.class);
        assertEquals('\u0000', result);
    }

    @Test
    public void testConvertNullToObjectReturnsNull() {
        String result = converter.convert(null, String.class);
        assertNull(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertWithNullTargetClassThrows() {
        converter.convert("anything", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertUnsupportedConversionThrows() {
        // There is no built‑in conversion from String to Thread
        converter.convert("abc", Thread.class);
    }

    @Test
    public void testStaticPairCreatesDistinctObjectsButEqualKeys() {
        Converter.ConversionPair p1 = Converter.pair(String.class, Integer.class);
        Converter.ConversionPair p2 = Converter.pair(String.class, Integer.class);
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertEquals(String.class, p1.getSource());
        assertEquals(Integer.class, p1.getTarget());
    }

    @Test
    public void testAddConversionOverridesBuiltInBehaviour() {
        // custom conversion: "YES"/"NO" -> Boolean
        Convert<Boolean> yesNoToBool = (from, conv) -> {
            if ("YES".equalsIgnoreCase(String.valueOf(from))) return true;
            if ("NO".equalsIgnoreCase(String.valueOf(from))) return false;
            return null;
        };
        Converter.addConversion(String.class, Boolean.class, yesNoToBool);
        Boolean resultYes = converter.convert("YES", Boolean.class);
        Boolean resultNo = converter.convert("NO", Boolean.class);
        assertTrue(resultYes);
        assertFalse(resultNo);
    }

    @Test
    public void testIdentityConversion() {
        String original = "identity";
        String result = Converter.identity(original, converter);
        assertSame("Identity should return the same instance", original, result);
    }

    /* ------------------------ Edge‑Case Tests ------------------------ */

    @Test
    public void testIsCollectionConversionSupportedArrayToArrayDifferentComponent() {
        assertTrue("Array<String[]> -> Array<Integer[]> should be supported (different components)",
                Converter.isCollectionConversionSupported(String[].class, Integer[].class));
    }

    @Test
    public void testIsCollectionConversionSupportedArrayToArraySameComponent() {
        assertFalse("Array<String[]> -> Array<String[]> should NOT be reported as a collection conversion",
                Converter.isCollectionConversionSupported(String[].class, String[].class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsCollectionConversionSupportedEnumSetTargetThrows() {
        // target is raw EnumSet.class – the method must throw IllegalArgumentException
        Converter.isCollectionConversionSupported(String[].class, EnumSet.class);
    }

    @Test
    public void testIsSimpleTypeConversionSupportedPrimitivesAndWrappers() {
        // Primitive int to Integer (wrapper) is supported via internal handling
        assertTrue(converter.isSimpleTypeConversionSupported(int.class, Integer.class));
        // Wrapper Double to primitive double
        assertTrue(converter.isSimpleTypeConversionSupported(Double.class, double.class));
        // String to Integer (non‑collection)
        assertTrue(converter.isSimpleTypeConversionSupported(String.class, Integer.class));
        // Collection types should return false
        assertFalse(converter.isSimpleTypeConversionSupported(List.class, Set.class));
    }

    @Test
    public void testIsConversionSupportedForArrayComponentConversion() {
        // String[] -> Integer[] is supported because component conversion (String -> Integer) exists
        assertTrue(converter.isConversionSupportedFor(String[].class, Integer[].class));
        // Integer[] -> Integer[] is NOT a conversion (same component), should be false
        assertFalse(converter.isConversionSupportedFor(Integer[].class, Integer[].class));
        // List -> Set (collection conversion) should be true
        assertTrue(converter.isConversionSupportedFor(List.class, Set.class));
    }

    @Test
    public void testAllSupportedConversionsIsNotEmpty() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        assertNotNull(map);
        assertFalse("Built‑in conversion map must contain entries", map.isEmpty());
        // Spot‑check: there must be a conversion from String to Integer
        Set<Class<?>> targets = map.get(String.class);
        assertNotNull("String source should be present", targets);
        assertTrue("String → Integer should be a supported conversion", targets.contains(Integer.class));
    }

    @Test
    public void testGetSupportedConversionsNames() {
        Map<String, Set<String>> nameMap = Converter.getSupportedConversions();
        assertNotNull(nameMap);
        assertFalse(nameMap.isEmpty());
        // Spot‑check the same conversion as above, but using short names
        Set<String> targets = nameMap.get("String");
        assertNotNull(targets);
        assertTrue("String → Integer short name must be present", targets.contains("Integer"));
    }

    @Test
    public void testConvertNullToPrimitiveCharReturnsZeroChar() {
        char result = converter.convert(null, char.class);
        assertEquals('\u0000', result);
    }

    @Test
    public void testConvertBigIntegerToLong() {
        BigInteger bi = new BigInteger("1234567890123456789");
        Long l = converter.convert(bi, Long.class);
        assertEquals(Long.valueOf(1234567890123456789L), l);
    }

    @Test
    public void testConversionFromVoidSourceHandledInternally() {
        // The internal algorithm uses Void.class for null source.
        // Converting null to long should invoke the Void→Long conversion (returns 0L)
        long result = converter.convert(null, long.class);
        assertEquals(0L, result);
    }

    @Test
    public void testAddConversionClearsCache() {
        // First conversion uses built‑in logic (String → Integer)
        Integer first = converter.convert("42", Integer.class);
        assertEquals(Integer.valueOf(42), first);

        // Override with a custom conversion that always returns 99
        Convert<Integer> always99 = (from, conv) -> 99;
        Converter.addConversion(String.class, Integer.class, always99);

        // New Converter instance picks up the override (caches are cleared per type)
        Converter newConverter = new Converter(options);
        Integer overridden = newConverter.convert("any", Integer.class);
        assertEquals(Integer.valueOf(99), overridden);
    }

    @Test
    public void testConversionFromEnumSetToArray() {
        EnumSet<Day> days = EnumSet.of(Day.MONDAY, Day.FRIDAY);
        Day[] array = converter.convert(days, Day[].class);
        assertArrayEquals(new Day[]{Day.MONDAY, Day.FRIDAY}, array);
    }

    /* --------------------------------------------------------------
     *  Enum used for a few collection‑related tests.
     * -------------------------------------------------------------- */
    private enum Day { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }
}
