package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Converter}.
 *
 * The tests are organized to cover:
 * <ul>
 *   <li>Extraction of all public API methods.</li>
 *   <li>Basic successful conversions for a representative set of types.</li>
 *   <li>Edge‑case handling (null source, primitive targets, unsupported conversions).</li>
 *   <li>Collection/array/EnumSet conversion paths.</li>
 *   <li>Custom user‑defined conversion registration.</li>
 *   <li>Utility methods that report conversion support.</li>
 *   <li>Introspection of the supported conversion map.</li>
 * </ul>
 */
public class ConverterTest {

    /** Simple {@link ConverterOptions} implementation that supplies no overrides. */
    private static class SimpleOptions implements ConverterOptions {
        @Override
        public Map<Converter.ConversionPair, Convert<?>> getConverterOverrides() {
            return Collections.emptyMap();
        }
    }

    private Converter converter;

    @Before
    public void setUp() {
        converter = new Converter(new SimpleOptions());
    }

    /* ----------------------------------------------------------------------
     *  1.  Public API extraction (for documentation / sanity check)
     * ---------------------------------------------------------------------- */
    @Test
    public void publicMethodsArePresent() {
        // Verify that each expected public method exists with the correct signature.
        // This is a compile‑time safety net – if the signature changes the test will fail.
        try {
            Converter.class.getMethod("getOptions");
            Converter.class.getMethod("convert", Object.class, Class.class);
            Converter.class.getMethod("isCollectionConversionSupported", Class.class, Class.class);
            Converter.class.getMethod("isSimpleTypeConversionSupported", Class.class, Class.class);
            Converter.class.getMethod("isDirectConversionSupported", Class.class, Class.class);
            Converter.class.getMethod("isConversionSupportedFor", Class.class, Class.class);
            Converter.class.getMethod("allSupportedConversions");
            Converter.class.getMethod("getSupportedConversions");
            Converter.class.getMethod("addConversion", Class.class, Class.class, Convert.class);
        } catch (NoSuchMethodException e) {
            fail("Expected public method missing: " + e.getMessage());
        }
    }

    /* ----------------------------------------------------------------------
     *  2.  Basic successful conversions
     * ---------------------------------------------------------------------- */
    @Test
    public void convertStringToInteger() {
        Integer i = converter.convert("123", Integer.class);
        assertEquals(Integer.valueOf(123), i);
    }

    @Test
    public void convertStringToLongPrimitive() {
        long l = converter.convert("456", long.class);
        assertEquals(456L, l);
    }

    @Test
    public void convertStringToBoolean() {
        Boolean b = converter.convert("true", Boolean.class);
        assertTrue(b);
    }

    @Test
    public void convertStringToBigInteger() {
        BigInteger bi = converter.convert("999999999999999999", BigInteger.class);
        assertEquals(new BigInteger("999999999999999999"), bi);
    }

    @Test
    public void convertDateToString() {
        Date now = new Date(0L); // epoch
        String s = converter.convert(now, String.class);
        // The exact format is implementation‑dependent but must contain the epoch time.
        assertTrue(s.contains("1970"));
    }

    /* ----------------------------------------------------------------------
     *  3.  Null handling & primitive defaults
     * ---------------------------------------------------------------------- */
    @Test
    public void convertNullToObjectReturnsNull() {
        assertNull(converter.convert(null, Integer.class));
        assertNull(converter.convert(null, String.class));
        assertNull(converter.convert(null, Date.class));
    }

    @Test
    public void convertNullToPrimitiveReturnsDefault() {
        assertEquals(0, (int) converter.convert(null, int.class));
        assertEquals(0L, (long) converter.convert(null, long.class));
        assertEquals(0.0f, (float) converter.convert(null, float.class), 0.0f);
        assertEquals(0.0d, (double) converter.convert(null, double.class), 0.0d);
        assertFalse((boolean) converter.convert(null, boolean.class));
        assertEquals('\u0000', (char) converter.convert(null, char.class));
    }

    /* ----------------------------------------------------------------------
     *  4.  Unsupported conversion handling
     * ---------------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void convertUnsupportedThrowsException() {
        // Thread.class has no registered conversion from String.
        converter.convert("something", Thread.class);
    }

    /* ----------------------------------------------------------------------
     *  5.  Collection/Array/EnumSet conversions
     * ---------------------------------------------------------------------- */
    @Test
    public void arrayToListConversion() {
        String[] src = {"a", "b", "c"};
        List<String> list = converter.convert(src, List.class);
        assertEquals(Arrays.asList("a", "b", "c"), list);
    }

    @Test
    public void listToArrayConversion() {
        List<Integer> src = Arrays.asList(1, 2, 3);
        Integer[] arr = converter.convert(src, Integer[].class);
        assertArrayEquals(new Integer[]{1, 2, 3}, arr);
    }

    @Test
    public void arrayToArrayComponentConversion() {
        String[] src = {"10", "20"};
        Integer[] arr = converter.convert(src, Integer[].class);
        assertArrayEquals(new Integer[]{10, 20}, arr);
    }

    @Test
    public void enumSetConversionFromArray() {
        enum Day { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }

        Object[] src = {"MONDAY", Day.WEDNESDAY, "FRIDAY", null};
        @SuppressWarnings("unchecked")
        EnumSet<Day> result = (EnumSet<Day>) (Object) converter.convert(src, Day.class);
        assertEquals(EnumSet.of(Day.MONDAY, Day.WEDNESDAY, Day.FRIDAY), result);
    }

    /* ----------------------------------------------------------------------
     *  6.  Custom user‑defined conversion
     * ---------------------------------------------------------------------- */
    @Test
    public void addAndUseCustomConversion() {
        // Convert a String like "hex:FF" to a BigInteger (hexadecimal).
        converter.addConversion(String.class, BigInteger.class,
                (from, conv) -> {
                    String s = (String) from;
                    if (s.startsWith("hex:")) {
                        return new BigInteger(s.substring(4), 16);
                    }
                    return new BigInteger(s); // fallback
                });

        BigInteger bi = converter.convert("hex:FF", BigInteger.class);
        assertEquals(new BigInteger("255"), bi);
    }

    /* ----------------------------------------------------------------------
     *  7.  Conversion‑support query methods
     * ---------------------------------------------------------------------- */
    @Test
    public void isSimpleTypeConversionSupported() {
        assertTrue(converter.isSimpleTypeConversionSupported(String.class, Integer.class));
        assertFalse(converter.isSimpleTypeConversionSupported(List.class, Set.class));
    }

    @Test
    public void isDirectConversionSupported() {
        // Direct primitive‑wrapper conversion exists.
        assertTrue(converter.isDirectConversionSupported(Integer.class, Long.class));

        // Collection conversion (array → List) is supported.
        assertTrue(converter.isDirectConversionSupported(String[].class, List.class));

        // Same‑type array conversion is NOT considered a direct conversion.
        assertFalse(converter.isDirectConversionSupported(String[].class, String[].class));

        // Array‑to‑array with differing component types is direct (via component conversion).
        assertTrue(converter.isDirectConversionSupported(String[].class, Integer[].class));
    }

    @Test
    public void isConversionSupportedFor() {
        // Simple direct conversion
        assertTrue(converter.isConversionSupportedFor(String.class, Integer.class));

        // Collection conversion
        assertTrue(converter.isConversionSupportedFor(String[].class, List.class));

        // Inherited conversion (Number → Double via Long → Double)
        assertTrue(converter.isConversionSupportedFor(Long.class, Double.class));

        // Unsupported path
        assertFalse(converter.isConversionSupportedFor(Thread.class, String.class));
    }

    @Test
    public void isCollectionConversionSupportedBehaviour() {
        // Valid array → List conversion
        assertTrue(converter.isCollectionConversionSupported(String[].class, List.class));

        // Valid List → array conversion
        assertTrue(converter.isCollectionConversionSupported(ArrayList.class, String[].class));

        // Same component type array → array should be false (no conversion needed)
        assertFalse(converter.isCollectionConversionSupported(String[].class, String[].class));

        // EnumSet target must be rejected with an informative exception
        try {
            converter.isCollectionConversionSupported(String[].class, EnumSet.class);
            fail("Expected IllegalArgumentException for EnumSet target");
        } catch (IllegalArgumentException ignored) {
        }
    }

    /* ----------------------------------------------------------------------
     *  8.  Supported‑conversion introspection
     * ---------------------------------------------------------------------- */
    @Test
    public void getSupportedConversionsContainsKnownMapping() {
        Map<String, Set<String>> map = converter.getSupportedConversions();
        Set<String> targets = map.get("String");
        assertNotNull("String key must exist", targets);
        assertTrue("String → Integer should be supported", targets.contains("Integer"));
        assertTrue("String → Boolean should be supported", targets.contains("Boolean"));
        assertTrue("String → BigInteger should be supported", targets.contains("BigInteger"));
    }

    @Test
    public void allSupportedConversionsStructure() {
        Map<Class<?>, Set<Class<?>>> map = converter.allSupportedConversions();
        Set<Class<?>> intTargets = map.get(String.class);
        assertNotNull(intTargets);
        assertTrue(intTargets.contains(Integer.class));
        assertTrue(intTargets.contains(Boolean.class));
    }

    /* ----------------------------------------------------------------------
     *  9.  Accessor for options (trivial but part of public API)
     * ---------------------------------------------------------------------- */
    @Test
    public void getOptionsReturnsProvidedInstance() {
        ConverterOptions opts = new SimpleOptions();
        Converter c = new Converter(opts);
        assertSame(opts, c.getOptions());
    }
}
