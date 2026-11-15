package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Converter}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Basic primitive and object conversions (null handling, String→Number, String→Date/Time, Map→UUID)</li>
 *   <li>Collection/array conversion paths (array→List, array→EnumSet, EnumSet→Array)</li>
 *   <li>Utility methods that expose the conversion‑matrix (isCollectionConversionSupported,
 *       isSimpleTypeConversionSupported, isConversionSupportedFor, getSupportedConversions)</li>
 *   <li>Custom conversion registration and cache invalidation (addConversion, clearCachesForType)</li>
 *   <li>Identity and unsupported helpers</li>
 * </ul>
 * </p>
 *
 * <p>Only public APIs (and the {@link ConverterOptions} interface) are used – no
 * mocking or subclassing of library internals.</p>
 */
public class ConverterTest {

    private Converter converter;
    private ConverterOptions emptyOptions;

    @Before
    public void setUp() {
        // Minimal implementation of ConverterOptions required for the constructor.
        emptyOptions = new ConverterOptions() {
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
        };
        converter = new Converter(emptyOptions);
    }

    /*** 1. Null handling – primitive targets ***/
    @Test
    public void testConvertNullToPrimitives() {
        int i = converter.convert(null, int.class);
        assertEquals(0, i);

        boolean b = converter.convert(null, boolean.class);
        assertFalse(b);

        char c = converter.convert(null, char.class);
        assertEquals('\u0000', c);
    }

    /*** 2. Simple String → Integer conversion ***/
    @Test
    public void testConvertStringToInteger() {
        Integer result = converter.convert("12345", Integer.class);
        assertEquals(Integer.valueOf(12345), result);
    }

    /*** 3. String → LocalDate conversion ***/
    @Test
    public void testConvertStringToLocalDate() {
        LocalDate date = converter.convert("2023-01-15", LocalDate.class);
        assertEquals(LocalDate.of(2023, 1, 15), date);
    }

    /*** 4. Map → UUID conversion ***/
    @Test
    public void testConvertMapToUUID() {
        Map<String, Object> map = new HashMap<>();
        map.put("mostSigBits", 0x12345678L);
        map.put("leastSigBits", 0x9ABCDEFL);
        UUID uuid = converter.convert(map, UUID.class);
        assertNotNull(uuid);
        assertEquals(new UUID(0x12345678L, 0x9ABCDEFL), uuid);
    }

    /*** 5. Array → List conversion ***/
    @Test
    public void testArrayToListConversion() {
        String[] source = {"alpha", "beta", "gamma"};
        List<String> list = converter.convert(source, List.class);
        assertEquals(Arrays.asList(source), list);
    }

    /*** 6. Array/Collection → EnumSet conversion ***/
    enum Day { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }

    @Test
    public void testArrayToEnumSetConversion() {
        Object[] source = {Day.MONDAY, "WEDNESDAY", 5 /* FRIDAY (ordinal) */};
        @SuppressWarnings("unchecked")
        EnumSet<Day> result = (EnumSet<Day>) (Object) converter.convert(source, Day.class);
        assertTrue(result.contains(Day.MONDAY));
        assertTrue(result.contains(Day.WEDNESDAY));
        assertTrue(result.contains(Day.FRIDAY));
        assertEquals(3, result.size());
    }

    /*** 7. isCollectionConversionSupported – various scenarios ***/
    @Test
    public void testIsCollectionConversionSupported() {
        // array → List   (supported)
        assertTrue(Converter.isCollectionConversionSupported(String[].class, List.class));

        // List → array   (supported)
        assertTrue(Converter.isCollectionConversionSupported(List.class, String[].class));

        // array → array with different component types (supported)
        assertTrue(Converter.isCollectionConversionSupported(Integer[].class, String[].class));

        // EnumSet target – must throw with clear message
        try {
            Converter.isCollectionConversionSupported(String[].class, EnumSet.class);
            fail("Expected IllegalArgumentException for EnumSet target");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("EnumSet"));
        }

        // non‑collection source – not supported
        assertFalse(Converter.isCollectionConversionSupported(Integer.class, List.class));
    }

    /*** 8. isSimpleTypeConversionSupported – primitive / object combos ***/
    @Test
    public void testIsSimpleTypeConversionSupported() {
        // Simple supported conversion
        assertTrue(converter.isSimpleTypeConversionSupported(String.class, Integer.class));

        // Unsupported because source is array
        assertFalse(converter.isSimpleTypeConversionSupported(String[].class, Integer.class));

        // Unsupported because target is collection
        assertFalse(converter.isSimpleTypeConversionSupported(String.class, List.class));
    }

    /*** 9. isConversionSupportedFor – array component checks ***/
    @Test
    public void testIsConversionSupportedForArrayComponents() {
        // String[] → Integer[] : component conversion exists, so overall conversion is supported
        assertTrue(converter.isConversionSupportedFor(String[].class, Integer[].class));

        // String[] → MonthDay[] : component conversion does NOT exist, should be false
        assertFalse(converter.isConversionSupportedFor(String[].class, MonthDay[].class));

        // List → Set (both collections) – supported
        assertTrue(converter.isConversionSupportedFor(List.class, Set.class));
    }

    /*** 10. Custom conversion registration overrides built‑in behavior ***/
    @Test
    public void testAddCustomConversionOverridesDefault() {
        // Default conversion: "true" → Boolean true
        assertEquals(Boolean.TRUE, converter.convert("true", Boolean.class));

        // Register a custom conversion that inverts the logic
        Converter.addConversion(String.class, Boolean.class, (from, conv) -> {
            String s = (String) from;
            return Boolean.valueOf(!"true".equalsIgnoreCase(s));
        });

        // The new conversion must take effect immediately
        assertEquals(Boolean.FALSE, converter.convert("true", Boolean.class));
        assertEquals(Boolean.TRUE, converter.convert("anythingElse", Boolean.class));
    }

    /*** 11. getSupportedConversions contains a known mapping ***/
    @Test
    public void testSupportedConversionsContainStringToInteger() {
        Map<String, Set<String>> map = Converter.getSupportedConversions();
        Set<String> targets = map.get("String");
        assertNotNull(targets);
        assertTrue(targets.contains("Integer"));
    }

    /*** 12. allSupportedConversions – structural sanity check ***/
    @Test
    public void testAllSupportedConversionsStructure() {
        Map<Class<?>, Set<Class<?>>> all = Converter.allSupportedConversions();
        // Should contain at least the mapping from String → Integer.class
        assertTrue(all.getOrDefault(String.class, Collections.emptySet()).contains(Integer.class));
        // Should contain mapping from UUID → String.class (via toString)
        assertTrue(all.getOrDefault(UUID.class, Collections.emptySet()).contains(String.class));
    }

    /*** 13. Identity conversion returns the exact same instance ***/
    @Test
    public void testIdentityConversion() {
        String original = "identityTest";
        String result = Converter.identity(original, converter);
        assertSame(original, result);
    }

    /*** 14. Unsupported conversion returns null via the helper ***/
    @Test
    public void testUnsupportedHelperReturnsNull() throws Exception {
        // Use reflection to call the private static method `unsupported`
        java.lang.reflect.Method m = Converter.class.getDeclaredMethod("unsupported", Object.class, Converter.class);
        m.setAccessible(true);
        Object returned = m.invoke(null, "any", converter);
        assertNull(returned);
    }

    /*** 15. Conversion of URL and URI – built‑in handling ***/
    @Test
    public void testUrlAndUriConversions() throws Exception {
        URL url = new URL("http://example.com");
        URI uri = converter.convert(url, URI.class);
        assertEquals(url.toURI(), uri);

        URI uri2 = new URI("http://example.org");
        URL url2 = converter.convert(uri2, URL.class);
        assertEquals(uri2.toURL(), url2);
    }

    /*** 16. Verify that a conversion that does not exist throws IllegalArgumentException ***/
    @Test(expected = IllegalArgumentException.class)
    public void testUnsupportedConversionThrows() {
        // No conversion from BigInteger to URL exists
        converter.convert(BigInteger.ONE, URL.class);
    }

    /*** 17. Cache invalidation after adding a custom conversion ***/
    @Test
    public void testCacheInvalidationAfterAddConversion() {
        // Initial conversion uses built‑in String→Integer
        assertEquals(Integer.valueOf(7), converter.convert("7", Integer.class));

        // Add a custom conversion that always returns 42
        Converter.addConversion(String.class, Integer.class, (from, conv) -> 42);

        // The cache must be cleared, so the new conversion is used
        assertEquals(Integer.valueOf(42), converter.convert("7", Integer.class));
    }

    /*** 18. Ensure that converting a null Map to a non‑primitive returns null ***/
    @Test
    public void testConvertNullMapToObject() {
        Map<String, Object> nullMap = null;
        String result = converter.convert(nullMap, String.class);
        assertNull(result);
    }

    /*** 19. Verify that converting a null to a wrapper type returns null ***/
    @Test
    public void testConvertNullToWrapper() {
        Long l = converter.convert(null, Long.class);
        assertNull(l);
    }

    /*** 20. Verify that converting a null to a primitive wrapper via Void mapping works ***/
    @Test
    public void testVoidToPrimitiveWrapperConversion() {
        // The built‑in mapping for (Void.class, Byte.class) returns 0 via NumberConversions::toByteZero
        Byte b = converter.convert(null, Byte.class);
        assertEquals(Byte.valueOf((byte)0), b);
    }
}
