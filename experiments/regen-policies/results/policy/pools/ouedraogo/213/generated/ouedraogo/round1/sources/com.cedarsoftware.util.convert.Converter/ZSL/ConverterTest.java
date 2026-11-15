package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.Assert.*;

public class ConverterTest {

    private Converter converter;

    /**
     * Minimal {@link ConverterOptions} implementation needed for the tests.
     */
    private static class SimpleOptions implements ConverterOptions {
        @Override
        public Map<Converter.ConversionPair, Convert<?>> getConverterOverrides() {
            return new HashMap<>();
        }

        // All other default methods return {@code null} – the Converter does not depend on them for the tests.
    }

    @Before
    public void setUp() {
        converter = new Converter(new SimpleOptions());
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Verify that {@code convert(null, int.class)} returns the default primitive value (0).
     */
    @Test
    public void testConvertNullToPrimitiveInt() {
        int result = converter.convert(null, int.class);
        assertEquals(0, result);
    }

    /**
     * Verify that {@code convert(null, Boolean.class)} returns {@code null}.
     */
    @Test
    public void testConvertNullToObject() {
        Boolean result = converter.convert(null, Boolean.class);
        assertNull(result);
    }

    /**
     * Basic conversion from {@link String} to {@link Integer}.
     */
    @Test
    public void testStringToInteger() {
        Integer value = converter.convert("12345", Integer.class);
        assertEquals(Integer.valueOf(12345), value);
    }

    /**
     * Verify that a custom conversion added via {@link Converter#addConversion}
     * is honoured before the built‑in conversion.
     */
    @Test
    public void testAddCustomConversionOverridesBuiltIn() {
        // Custom conversion: String -> Boolean (invert the normal Boolean conversion)
        Converter.addConversion(String.class, Boolean.class, (from, conv) -> !Boolean.parseBoolean((String) from));

        Boolean result = converter.convert("true", Boolean.class);
        assertFalse("Custom conversion should invert the value", result);
    }

    /**
     * Verify that {@link Converter#identity} simply returns the original object.
     */
    @Test
    public void testIdentityConversion() {
        String original = "identity-test";
        String returned = Converter.identity(original, converter);
        assertSame("Identity should return the same instance", original, returned);
    }

    /**
     * Test {@link Converter#isCollectionConversionSupported} for a few valid and invalid cases.
     */
    @Test
    public void testIsCollectionConversionSupported() {
        // Array → List (valid)
        assertTrue(Converter.isCollectionConversionSupported(String[].class, List.class));

        // List → Array (valid)
        assertTrue(Converter.isCollectionConversionSupported(ArrayList.class, Integer[].class));

        // EnumSet target – should throw IllegalArgumentException
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("To convert to EnumSet");
        Converter.isCollectionConversionSupported(String[].class, EnumSet.class);
    }

    /**
     * Test {@link Converter#isSimpleTypeConversionSupported} for simple non‑collection types.
     */
    @Test
    public void testIsSimpleTypeConversionSupported() {
        // Simple supported conversion
        assertTrue(converter.isSimpleTypeConversionSupported(String.class, Integer.class));

        // Unsupported conversion (collection involved)
        assertFalse(converter.isSimpleTypeConversionSupported(List.class, Set.class));

        // Unsupported conversion (no mapping)
        assertFalse(converter.isSimpleTypeConversionSupported(String.class, UUID.class));
    }

    /**
     * Test {@link Converter#isConversionSupportedFor} for array‑to‑array conversion that also checks
     * component‑type conversion support.
     */
    @Test
    public void testIsConversionSupportedForArrayToArray() {
        // String[] → Integer[] : component conversion exists, so overall conversion is supported.
        assertTrue(converter.isConversionSupportedFor(String[].class, Integer[].class));

        // String[] → UUID[] : component conversion does NOT exist, so overall conversion is NOT supported.
        assertFalse(converter.isConversionSupportedFor(String[].class, UUID[].class));
    }

    /**
     * Verify that {@link Converter#getSupportedConversions} contains a known built‑in mapping.
     */
    @Test
    public void testGetSupportedConversionsContainsKnownMapping() {
        Map<String, Set<String>> map = Converter.getSupportedConversions();

        // The built‑in conversion from String to Integer should be present.
        Set<String> targets = map.get("String");
        assertNotNull("String should be a source key", targets);
        assertTrue("String → Integer mapping must exist", targets.contains("Integer"));
    }

    /**
     * Test {@link Converter#getShortName} for normal classes, arrays and the special
     * {@code java.sql.Date[]} custom name.
     */
    @Test
    public void testGetShortName() {
        assertEquals("Integer", Converter.getShortName(Integer.class));
        assertEquals("int[]", Converter.getShortName(int[].class));
        // Custom name for java.sql.Date[]
        assertEquals("java.sql.Date[]", Converter.getShortName(java.sql.Date[].class));
    }

    /**
     * Test {@link Converter#name(Object)} for null and a regular object.
     */
    @Test
    public void testNameMethod() {
        assertEquals("null", invokePrivateName(null));

        // Use an object that has a meaningful toString()
        Date now = new Date(0L);
        String expected = "Date (Thu Jan 01 00:00:00 UTC 1970)";
        assertEquals(expected, invokePrivateName(now));
    }

    /**
     * Helper that uses reflection to invoke the private static {@code name} method.
     */
    private static String invokePrivateName(Object obj) {
        try {
            java.lang.reflect.Method m = Converter.class.getDeclaredMethod("name", Object.class);
            m.setAccessible(true);
            return (String) m.invoke(null, obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Ensure that {@link Converter#addConversion} clears any cached converter for the given
     * source/target pair.
     */
    @Test
    public void testAddConversionInvalidatesCache() {
        // First, trigger a cache entry by performing a conversion.
        Integer before = converter.convert("99", Integer.class);
        assertEquals(Integer.valueOf(99), before);

        // Add a new conversion for the same pair.
        Converter.addConversion(String.class, Integer.class, (from, conv) -> 0);

        // The new conversion should be used now.
        Integer after = converter.convert("99", Integer.class);
        assertEquals(Integer.valueOf(0), after);
    }

    /**
     * Verify that {@link Converter#isConversionSupportedFor} correctly reports support for
     * primitive wrapper conversions (e.g., {@code Integer} → {@code Long}).
     */
    @Test
    public void testPrimitiveWrapperConversionSupported() {
        assertTrue(converter.isConversionSupportedFor(Integer.class, Long.class));
    }

    /**
     * Verify that {@link Converter#isConversionSupportedFor} returns {@code false}
     * for a completely unsupported conversion (e.g., {@code String} → {@code Thread}).
     */
    @Test
    public void testUnsupportedConversion() {
        assertFalse(converter.isConversionSupportedFor(String.class, Thread.class));
    }

    /**
     * Test that a custom conversion added via {@link Converter#addConversion}
     * works when the source is {@code null}.  The custom conversion receives the
     * {@code null} value and can decide what to return.
     */
    @Test
    public void testCustomConversionWithNullSource() {
        Converter.addConversion(String.class, AtomicInteger.class,
                (from, conv) -> new AtomicInteger(((String) from).length()));

        // The source is null, so the custom conversion should not be invoked;
        // the default null handling for an object target should return null.
        AtomicInteger result = converter.convert(null, AtomicInteger.class);
        assertNull(result);
    }

    /**
     * Verify that the {@link Converter#isConversionSupportedFor} method
     * correctly handles the case where the source and target are identical.
     */
    @Test
    public void testIdentityConversionSupported() {
        assertTrue(converter.isConversionSupportedFor(Integer.class, Integer.class));
        assertTrue(converter.isConversionSupportedFor(int.class, int.class));
    }

    /**
     * Ensure that {@link Converter#isCollectionConversionSupported} returns {@code false}
     * when neither source nor target are collections/arrays.
     */
    @Test
    public void testIsCollectionConversionSupportedNegative() {
        assertFalse(Converter.isCollectionConversionSupported(Integer.class, String.class));
    }

    /**
     * Verify that {@link Converter#isSimpleTypeConversionSupported} returns {@code false}
     * for primitive target types because they are handled via the full conversion path,
     * not the simple‑type shortcut.
     */
    @Test
    public void testSimpleTypeConversionWithPrimitiveTarget() {
        assertFalse(converter.isSimpleTypeConversionSupported(String.class, int.class));
    }

    /**
     * Test that {@link Converter#addConversion} can replace an existing user conversion.
     */
    @Test
    public void testAddConversionReplacesExisting() {
        // First conversion
        Converter.addConversion(String.class, Integer.class, (from, conv) -> 1);
        Integer first = converter.convert("any", Integer.class);
        assertEquals(Integer.valueOf(1), first);

        // Replace it
        Converter.addConversion(String.class, Integer.class, (from, conv) -> 2);
        Integer second = converter.convert("any", Integer.class);
        assertEquals(Integer.valueOf(2), second);
    }
}
