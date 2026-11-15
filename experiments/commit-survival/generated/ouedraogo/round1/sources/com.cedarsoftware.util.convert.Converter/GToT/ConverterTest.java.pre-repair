package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link Converter}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public methods of {@code Converter} (extracted from the source).</li>
 *   <li>Typical successful conversions for a representative set of source/target types.</li>
 *   <li>Edge‑case handling such as {@code null} values, primitive‑wrapper promotion, and unsupported conversions.</li>
 *   <li>Exception scenarios – mainly {@code IllegalArgumentException} for illegal arguments.</li>
 * </ul>
 *
 * The test class does **not** use any mocking frameworks – it creates real instances of
 * {@link ConverterOptions} via an anonymous implementation because the interface only
 * supplies accessor methods.
 */
public class ConverterTest {

    /** Minimal {@link ConverterOptions} implementation used for all tests. */
    private static class SimpleOptions implements ConverterOptions {
        private final Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> overrides = new HashMap<>();

        @Override public ZoneId getZoneId() { return ZoneId.systemDefault(); }
        @Override public Locale getLocale() { return Locale.US; }
        @Override public java.nio.charset.Charset getCharset() { return java.nio.charset.StandardCharsets.UTF_8; }
        @Override public ClassLoader getClassLoader() { return Thread.currentThread().getContextClassLoader(); }
        @Override public <T> T getCustomOption(String name) { return null; }
        @Override public TimeZone getTimeZone() { return TimeZone.getDefault(); }
        @Override public Character trueChar() { return 'T'; }
        @Override public Character falseChar() { return 'F'; }
        @Override public Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> getConverterOverrides() { return overrides; }
    }

    private Converter converter;

    @Before
    public void setUp() {
        converter = new Converter(new SimpleOptions());
    }

    /* -----------------------------------------------------------------------
     *  1.  Public API extraction (for reference – not a test)
     * -----------------------------------------------------------------------
     *
     *  public ConverterOptions getOptions()
     *  public <T> T convert(Object from, Class<T> toType)
     *  public boolean isDirectConversionSupportedFor(Class<?> source, Class<?> target)
     *  public boolean isConversionSupportedFor(Class<?> source, Class<?> target)
     *  public Map<Class<?>, Set<Class<?>>> allSupportedConversions()
     *  public Map<String, Set<String>> getSupportedConversions()
     *  public Convert<?> addConversion(Class<?> source, Class<?> target, Convert<?> conversionFunction)
     *
     *  (plus the package‑private static methods pair(), identity(), unsupported()
     *   and the inner class ClassLevel – these are not part of the public API and
     *   therefore are not directly tested.)
     */

    /* -----------------------------------------------------------------------
     *  2.  Basic conversion tests (happy path)
     * ----------------------------------------------------------------------- */

    @Test
    public void testConvertStringToInteger() {
        Integer result = converter.convert("123", Integer.class);
        assertEquals(Integer.valueOf(123), result);
    }

    @Test
    public void testConvertLongToDate() {
        long epochMilli = 1_600_000_000_000L;
        Date date = converter.convert(epochMilli, Date.class);
        assertEquals(new Date(epochMilli), date);
    }

    @Test
    public void testConvertBooleanToByte() {
        Byte bTrue = converter.convert(Boolean.TRUE, Byte.class);
        Byte bFalse = converter.convert(Boolean.FALSE, Byte.class);
        assertEquals(Byte.valueOf((byte)1), bTrue);
        assertEquals(Byte.valueOf((byte)0), bFalse);
    }

    @Test
    public void testConvertUuidToString() {
        UUID uuid = UUID.randomUUID();
        String s = converter.convert(uuid, String.class);
        assertEquals(uuid.toString(), s);
    }

    @Test
    public void testConvertStringToUri() throws Exception {
        String uriStr = "http://example.com/path?query=1";
        URI uri = converter.convert(uriStr, URI.class);
        assertEquals(new URI(uriStr), uri);
    }

    @Test
    public void testConvertStringToUrl() throws Exception {
        String urlStr = "http://example.com/";
        URL url = converter.convert(urlStr, URL.class);
        assertEquals(new URL(urlStr), url);
    }

    @Test
    public void testConvertLocalDateToString() {
        LocalDate ld = LocalDate.of(2020, Month.JANUARY, 15);
        String s = converter.convert(ld, String.class);
        assertEquals("2020-01-15", s);
    }

    @Test
    public void testConvertStringToLocalDate() {
        String s = "2020-01-15";
        LocalDate ld = converter.convert(s, LocalDate.class);
        assertEquals(LocalDate.of(2020, 1, 15), ld);
    }

    @Test
    public void testConvertMapToStringViaValueKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", 42);
        String s = converter.convert(map, String.class);
        assertEquals("42", s);
    }

    @Test
    public void testConvertByteArrayToString() {
        byte[] bytes = "hello".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String s = converter.convert(bytes, String.class);
        assertEquals("hello", s);
    }

    @Test
    public void testConvertStringBuilderToString() {
        StringBuilder sb = new StringBuilder("builder");
        String s = converter.convert(sb, String.class);
        assertEquals("builder", s);
    }

    @Test
    public void testConvertStringToBigDecimal() {
        BigDecimal bd = converter.convert("12345.67", BigDecimal.class);
        assertEquals(new BigDecimal("12345.67"), bd);
    }

    @Test
    public void testConvertBigDecimalToBigInteger() {
        BigDecimal bd = new BigDecimal("12345.99");
        BigInteger bi = converter.convert(bd, BigInteger.class);
        // conversion truncates toward zero
        assertEquals(new BigInteger("12345"), bi);
    }

    @Test
    public void testConvertStringToDuration() {
        String iso = "PT2H30M";
        Duration d = converter.convert(iso, Duration.class);
        assertEquals(Duration.ofHours(2).plusMinutes(30), d);
    }

    @Test
    public void testConvertStringToInstant() {
        String iso = "2020-01-01T12:34:56Z";
        Instant i = converter.convert(iso, Instant.class);
        assertEquals(Instant.parse(iso), i);
    }

    /* -----------------------------------------------------------------------
     *  3.  Edge‑case handling
     * ----------------------------------------------------------------------- */

    @Test
    public void testConvertNullToWrapperReturnsNull() {
        assertNull(converter.convert(null, Integer.class));
        assertNull(converter.convert(null, String.class));
        assertNull(converter.convert(null, UUID.class));
    }

    @Test
    public void testConvertNullToPrimitiveReturnsZeroOrFalse() {
        int i = converter.convert(null, int.class);
        assertEquals(0, i);

        boolean b = converter.convert(null, boolean.class);
        assertFalse(b);

        char c = converter.convert(null, char.class);
        assertEquals('\0', c);
    }

    @Test
    public void testPrimitivePromotionAndWrapperHandling() {
        // source is primitive int (auto‑boxed) -> target primitive long
        long l = converter.convert(5, long.class);
        assertEquals(5L, l);
    }

    @Test
    public void testIsDirectConversionSupportedFor() {
        assertTrue(converter.isDirectConversionSupportedFor(String.class, Integer.class));
        assertFalse(converter.isDirectConversionSupportedFor(String.class, List.class));
    }

    @Test
    public void testIsConversionSupportedFor_Inherited() {
        // String → CharSequence is not a direct entry but works via inheritance
        assertTrue(converter.isConversionSupportedFor(String.class, CharSequence.class));
    }

    @Test
    public void testAddCustomConversionOverridesFactory() {
        // Override String → Integer to return a constant
        Convert<Integer> constantFive = (from, conv) -> 5;
        converter.addConversion(String.class, Integer.class, constantFive);

        Integer result = converter.convert("any", Integer.class);
        assertEquals(Integer.valueOf(5), result);
    }

    @Test
    public void testAddUnsupportedConversionAndVerifyBehavior() {
        // Mark String → UUID as unsupported
        converter.addConversion(String.class, UUID.class, Converter.UNSUPPORTED);
        try {
            converter.convert("123e4567-e89b-12d3-a456-426614174000", UUID.class);
            fail("Expected IllegalArgumentException for unsupported conversion");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testAllSupportedConversionsContainsKnownEntry() {
        Map<String, Set<String>> map = converter.getSupportedConversions();
        // Verify that "String" → "Integer" is present
        assertTrue(map.containsKey("String"));
        assertTrue(map.get("String").contains("Integer"));
    }

    @Test
    public void testAllSupportedConversionsStructure() {
        Map<Class<?>, Set<Class<?>>> map = converter.allSupportedConversions();
        // Verify that the map contains a key for String.class
        assertTrue(map.containsKey(String.class));
        // Verify that Integer.class is one of the supported targets for String.class
        assertTrue(map.get(String.class).contains(Integer.class));
    }

    /* -----------------------------------------------------------------------
     *  4.  Exception scenarios
     * ----------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConvertWithNullTargetTypeThrows() {
        converter.convert("123", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertUnsupportedSourceTargetThrows() {
        // No conversion defined from Thread to Integer
        converter.convert(Thread.currentThread(), Integer.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddConversionWithNullSourceThrows() {
        converter.addConversion(null, Integer.class, (from, c) -> 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddConversionWithNullTargetThrows() {
        converter.addConversion(String.class, null, (from, c) -> 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddConversionWithNullFunctionThrows() {
        converter.addConversion(String.class, Integer.class, null);
    }

    /* -----------------------------------------------------------------------
     *  5.  Inherited conversion verification (superclass/interface handling)
     * ----------------------------------------------------------------------- */

    @Test
    public void testInheritedConversionFromArrayListToCollection() {
        // ArrayList implements List → Collection (no direct entry)
        List<Integer> list = Arrays.asList(1, 2, 3);
        Collection<Integer> col = converter.convert(list, Collection.class);
        assertEquals(list, col);
    }

    @Test
    public void testInheritedConversionFromHashMapToMap() {
        // HashMap is a concrete class that implements Map – direct entry exists,
        // but we also verify that inheritance lookup works when the entry is removed.
        Map<String, String> map = new HashMap<>();
        map.put("a", "b");
        // Temporarily remove the direct entry to force inheritance path
        Convert<?> removed = Converter.CONVERSION_DB.remove(Converter.pair(HashMap.class, Map.class));
        try {
            Map<String, String> result = converter.convert(map, Map.class);
            assertEquals(map, result);
        } finally {
            // restore original entry
            if (removed != null) {
                Converter.CONVERSION_DB.put(Converter.pair(HashMap.class, Map.class), removed);
            }
        }
    }

    /* -----------------------------------------------------------------------
     *  6.  Special handling for primitive wrappers and void source
     * ----------------------------------------------------------------------- */

    @Test
    public void testVoidSourceToPrimitiveWrapper() {
        // Source is null → treated as Void.class internally.
        // Void → Integer.class should return null (as per the factory map)
        assertNull(converter.convert(null, Integer.class));
    }

    @Test
    public void testVoidSourceToPrimitive() {
        // Void → int primitive should return zero
        int i = converter.convert(null, int.class);
        assertEquals(0, i);
    }

    @Test
    public void testVoidSourceToBooleanPrimitive() {
        boolean b = converter.convert(null, boolean.class);
        assertFalse(b);
    }
}
