package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.Year;
import java.time.ZoneId;
import java.util.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 tests for {@link Converter}.
 * The tests cover:
 * <ul>
 *   <li>basic primitive / wrapper conversions</li>
 *   <li>null handling</li>
 *   <li>collection / array conversions (including EnumSet)</li>
 *   <li>custom conversion registration via {@code addConversion}</li>
 *   <li>utility methods {@code getShortName}, {@code name},
 *       {@code isCollectionConversionSupported},
 *       {@code isSimpleTypeConversionSupported},
 *       {@code isConversionSupportedFor},
 *       {@code getSupportedConversions} and {@code allSupportedConversions}</li>
 *   <li>identity conversion</li>
 * </ul>
 */
public class ConverterTest {

    private Converter converter;

    /** Minimal {@link ConverterOptions} implementation required for constructing a {@link Converter}. */
    private static class SimpleOptions implements ConverterOptions {
        @Override public Map<Converter.ConversionPair, Convert<?>> getConverterOverrides() {
            return Collections.emptyMap();
        }
        // All other methods use default implementations (return null / default values)
    }

    /** Simple POJO used for custom conversion tests. */
    public static final class SimpleClass {
        private final String value;
        public SimpleClass(String v) { this.value = v; }
        public String getValue() { return value; }
        @Override public boolean equals(Object o) {
            return (o instanceof SimpleClass) && Objects.equals(value, ((SimpleClass)o).value);
        }
        @Override public int hashCode() { return Objects.hash(value); }
    }

    /** Simple enum for EnumSet related tests. */
    private enum Day { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }

    @Before
    public void setUp() {
        converter = new Converter(new SimpleOptions());
    }

    /* --------------------------------------------------------------
     *  Basic primitive / wrapper conversion tests
     * -------------------------------------------------------------- */
    @Test
    public void testStringToInteger() {
        Integer i = converter.convert("123", Integer.class);
        assertEquals(Integer.valueOf(123), i);
    }

    @Test
    public void testStringToLong() {
        Long l = converter.convert("9876543210", Long.class);
        assertEquals(Long.valueOf(9876543210L), l);
    }

    @Test
    public void testStringToBoolean() {
        Boolean bTrue = converter.convert("true", Boolean.class);
        Boolean bFalse = converter.convert("false", Boolean.class);
        assertTrue(bTrue);
        assertFalse(bFalse);
    }

    @Test
    public void testStringToChar() {
        Character c = converter.convert("Z", Character.class);
        assertEquals(Character.valueOf('Z'), c);
    }

    @Test
    public void testNullToPrimitiveInt() {
        int result = converter.convert(null, int.class);
        assertEquals(0, result);
    }

    @Test
    public void testNullToPrimitiveBoolean() {
        boolean result = converter.convert(null, boolean.class);
        assertFalse(result);
    }

    @Test
    public void testNullToPrimitiveChar() {
        char result = converter.convert(null, char.class);
        assertEquals('\u0000', result);
    }

    @Test
    public void testNullToObjectReturnsNull() {
        String s = converter.convert(null, String.class);
        assertNull(s);
    }

    /* --------------------------------------------------------------
     *  Collection / array conversion tests
     * -------------------------------------------------------------- */
    @Test
    public void testArrayToList() {
        String[] source = {"a", "b", "c"};
        List<String> list = converter.convert(source, List.class);
        assertEquals(Arrays.asList("a", "b", "c"), list);
    }

    @Test
    public void testListToArray() {
        List<Integer> source = Arrays.asList(1, 2, 3);
        Integer[] array = converter.convert(source, Integer[].class);
        assertArrayEquals(new Integer[]{1,2,3}, array);
    }

    @Test
    public void testArrayToArrayComponentConversion() {
        String[] source = {"10", "20", "30"};
        Integer[] result = converter.convert(source, Integer[].class);
        assertArrayEquals(new Integer[]{10,20,30}, result);
    }

    @Test
    public void testEnumSetFromArray() {
        Object[] source = {Day.MONDAY, "WEDNESDAY", 5 /* 5 maps to FRIDAY */};
        // The target type is the enum class itself – the converter creates an EnumSet<Day>.
        @SuppressWarnings("unchecked")
        EnumSet<Day> set = (EnumSet<Day>) (Object) converter.convert(source, Day.class);
        EnumSet<Day> expected = EnumSet.of(Day.MONDAY, Day.WEDNESDAY, Day.FRIDAY);
        assertEquals(expected, set);
    }

    @Test
    public void testEnumSetToArray() {
        EnumSet<Day> source = EnumSet.of(Day.TUESDAY, Day.THURSDAY);
        Day[] array = converter.convert(source, Day[].class);
        assertArrayEquals(new Day[]{Day.TUESDAY, Day.THURSDAY}, array);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsCollectionConversionSupportedThrowsOnEnumSetTarget() {
        Converter.isCollectionConversionSupported(String[].class, EnumSet.class);
    }

    /* --------------------------------------------------------------
     *  Utility method tests
     * -------------------------------------------------------------- */
    @Test
    public void testGetShortNameForPrimitiveArray() {
        assertEquals("int[]", Converter.getShortName(int[].class));
        assertEquals("byte[]", Converter.getShortName(byte[].class));
    }

    @Test
    public void testGetShortNameForCustomArray() {
        // Custom name is registered for java.sql.Date[]
        assertEquals("java.sql.Date[]", Converter.getShortName(java.sql.Date[].class));
    }

    @Test
    public void testGetShortNameForSqlDate() {
        assertEquals("java.sql.Date", Converter.getShortName(java.sql.Date.class));
    }

    @Test
    public void testNameMethod() {
        assertEquals("null", Converter.name(null));
        Integer i = 7;
        String expected = "Integer (7)";
        assertEquals(expected, Converter.name(i));
    }

    @Test
    public void testIsCollectionConversionSupportedTrueCases() {
        assertTrue(Converter.isCollectionConversionSupported(String[].class, List.class));
        assertTrue(Converter.isCollectionConversionSupported(List.class, String[].class));
        assertTrue(Converter.isCollectionConversionSupported(EnumSet.class, Day[].class));
        assertTrue(Converter.isCollectionConversionSupported(Day[].class, EnumSet.class));
    }

    @Test
    public void testIsCollectionConversionSupportedFalseCases() {
        assertFalse(Converter.isCollectionConversionSupported(Integer.class, List.class));
        assertFalse(Converter.isCollectionConversionSupported(Map.class, Set.class));
    }

    @Test
    public void testIsSimpleTypeConversionSupported() {
        assertTrue(converter.isSimpleTypeConversionSupported(String.class, Integer.class));
        assertFalse(converter.isSimpleTypeConversionSupported(String[].class, Integer[].class));
        assertFalse(converter.isSimpleTypeConversionSupported(List.class, Set.class));
    }

    @Test
    public void testIsConversionSupportedForArrayComponent() {
        // String[] -> Integer[] is supported because component conversion exists.
        assertTrue(converter.isConversionSupportedFor(String[].class, Integer[].class));
        // Integer[] -> String[] is also supported.
        assertTrue(converter.isConversionSupportedFor(Integer[].class, String[].class));
        // Unsupported component conversion (String[] -> UUID[]) should be false.
        assertFalse(converter.isConversionSupportedFor(String[].class, UUID[].class));
    }

    @Test
    public void testIsConversionSupportedForCollections() {
        assertTrue(converter.isConversionSupportedFor(List.class, Set.class));
        assertTrue(converter.isConversionSupportedFor(Set.class, List.class));
    }

    @Test
    public void testUnsupportedConversionThrows() {
        try {
            converter.convert(new Object(), Integer.class);
            fail("Expected IllegalArgumentException for unsupported conversion");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* --------------------------------------------------------------
     *  Custom conversion registration tests
     * -------------------------------------------------------------- */
    @Test
    public void testAddCustomConversionAndConvert() {
        // Register a conversion from String to SimpleClass.
        Converter.addConversion(String.class, SimpleClass.class,
                (from, conv) -> new SimpleClass((String) from));

        SimpleClass result = converter.convert("custom-value", SimpleClass.class);
        assertEquals(new SimpleClass("custom-value"), result);
    }

    @Test
    public void testAddConversionUpdatesSupportedConversions() {
        // Before adding, there is no conversion from String to SimpleClass.
        assertFalse(converter.isSimpleTypeConversionSupported(String.class, SimpleClass.class));

        // Register conversion.
        Converter.addConversion(String.class, SimpleClass.class,
                (from, conv) -> new SimpleClass((String) from));

        // After registration, the conversion should be reported as supported.
        assertTrue(converter.isSimpleTypeConversionSupported(String.class, SimpleClass.class));

        // Verify that getSupportedConversions reflects the new mapping.
        Map<String, Set<String>> map = Converter.getSupportedConversions();
        Set<String> targets = map.get("String");
        assertNotNull(targets);
        assertTrue(targets.contains("SimpleClass"));
    }

    /* --------------------------------------------------------------
     *  Identity conversion test
     * -------------------------------------------------------------- */
    @Test
    public void testIdentityConversion() {
        String original = "identity-test";
        String result = Converter.identity(original, converter);
        assertSame(original, result);
    }

    /* --------------------------------------------------------------
     *  Supported conversion maps tests
     * -------------------------------------------------------------- */
    @Test
    public void testAllSupportedConversionsContainsKnownEntry() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        Set<Class<?>> targets = map.get(String.class);
        assertNotNull(targets);
        assertTrue(targets.contains(Integer.class));
        assertTrue(targets.contains(Boolean.class));
        assertTrue(targets.contains(BigInteger.class));
        assertTrue(targets.contains(URI.class));
    }

    @Test
    public void testGetSupportedConversionsContainsKnownEntry() {
        Map<String, Set<String>> map = Converter.getSupportedConversions();
        Set<String> targets = map.get("String");
        assertNotNull(targets);
        assertTrue(targets.contains("Integer"));
        assertTrue(targets.contains("Boolean"));
        assertTrue(targets.contains("URI"));
    }

    /* --------------------------------------------------------------
     *  Additional edge‑case conversions
     * -------------------------------------------------------------- */
    @Test
    public void testStringToLocalDate() {
        LocalDate date = converter.convert("2024-01-15", LocalDate.class);
        assertEquals(LocalDate.of(2024, 1, 15), date);
    }

    @Test
    public void testLongToTimestamp() {
        long epochMillis = 1_600_000_000_000L;
        Timestamp ts = converter.convert(epochMillis, Timestamp.class);
        assertEquals(new Timestamp(epochMillis), ts);
    }

    @Test
    public void testBigIntegerToYear() {
        BigInteger bi = BigInteger.valueOf(2022);
        Year year = converter.convert(bi, Year.class);
        assertEquals(Year.of(2022), year);
    }

    @Test
    public void testMonthDayToString() {
        MonthDay md = MonthDay.of(12, 25);
        String s = converter.convert(md, String.class);
        assertEquals(md.toString(), s);
    }

    @Test
    public void testZoneIdToString() {
        ZoneId zid = ZoneId.of("UTC");
        String s = converter.convert(zid, String.class);
        assertEquals("UTC", s);
    }
}
