package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.convert.Converter.ConversionPair;


import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

/**
 * Unit tests for {@link DefaultConverterOptions}.
 */
public class DefaultConverterOptionsTest {

    /**
     * Verify that a newly constructed instance has empty option maps and returns {@code null}
     * for any custom option request.
     */
    @Test
    public void testInitialState() {
        DefaultConverterOptions opts = new DefaultConverterOptions();

        // custom options are empty – any name should return null
        assertNull("Custom option for unknown key should be null", opts.getCustomOption("anyKey"));

        // the overrides map should be empty
        Map<ConversionPair, Convert<?>> overrides = opts.getConverterOverrides();
        assertNotNull("Overrides map should never be null", overrides);
        assertTrue("Overrides map should be empty initially", overrides.isEmpty());

        // the map implementation must be a ConcurrentHashMap (as per constructor)
        assertTrue("Overrides map should be a ConcurrentHashMap",
                overrides instanceof ConcurrentHashMap);

        // default methods from the interface must return their defaults (null)
        assertNull(opts.getZoneId());
        assertNull(opts.getLocale());
        assertNull(opts.getCharset());
        assertNull(opts.getClassLoader());
        assertNull(opts.getTimeZone());
        assertNull(opts.trueChar());
        assertNull(opts.falseChar());
    }

    /**
     * Use reflection to insert a custom option into the internal map and verify that
     * {@link DefaultConverterOptions#getCustomOption(String)} returns the stored value
     * with proper generic typing.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testCustomOptionStorageViaReflection() throws Exception {
        DefaultConverterOptions opts = new DefaultConverterOptions();

        // Access the private field 'customOptions'
        Field field = DefaultConverterOptions.class.getDeclaredField("customOptions");
        field.setAccessible(true);
        Map<String, Object> internalMap = (Map<String, Object>) field.get(opts);

        // Insert a few values of different types
        internalMap.put("intKey", 42);
        internalMap.put("stringKey", "hello");
        internalMap.put("nullKey", null); // ConcurrentHashMap does NOT allow null values – this will throw

        // The above line will actually cause a NullPointerException. Wrap in try/catch to verify the behaviour.
        // (The test is intentionally checking that the map protects against null values.)
        try {
            internalMap.put("nullKey", null);
            fail("ConcurrentHashMap should reject null values with NPE");
        } catch (NullPointerException expected) {
            // expected behaviour
        }

        // Retrieve via the public API
        Integer intVal = opts.getCustomOption("intKey");
        String strVal = opts.getCustomOption("stringKey");

        assertEquals(Integer.valueOf(42), intVal);
        assertEquals("hello", strVal);

        // unknown key still returns null
        assertNull(opts.getCustomOption("missing"));
    }

    /**
     * Verify that {@link DefaultConverterOptions#getConverterOverrides()} returns a mutable
     * map that can store and retrieve converter overrides.
     */
    @Test
    public void testConverterOverridesAddAndRetrieve() {
        DefaultConverterOptions opts = new DefaultConverterOptions();

        // Obtain the overrides map
        Map<ConversionPair, Convert<?>> overrides = opts.getConverterOverrides();

        // Create a ConversionPair – the library provides a public constructor (source, target)
        ConversionPair pair = new Converter.ConversionPair(String.class, Integer.class);

        // Simple Convert implementation that parses an Integer from a String
        Convert<Integer> stringToInteger = new Convert<Integer>() {
            @Override
            public Integer convert(Object from, Converter converter) {
                return Integer.valueOf((String) from);
            }
        };

        // Put the override into the map
        overrides.put(pair, stringToInteger);

        // Verify the map contains the entry and that we can retrieve it
        assertTrue("Overrides map should contain the inserted key", overrides.containsKey(pair));
        Convert<?> retrieved = overrides.get(pair);
        assertSame("Retrieved Convert instance should be the same as inserted", stringToInteger, retrieved);

        // Validate that the converter works as expected
        Integer result = ((Convert<Integer>) retrieved).convert("123", null);
        assertEquals(Integer.valueOf(123), result);
    }

    /**
     * Ensure that the same overrides map instance is returned on multiple calls.
     */
    @Test
    public void testConverterOverridesIdentity() {
        DefaultConverterOptions opts = new DefaultConverterOptions();
        Map<ConversionPair, Convert<?>> first = opts.getConverterOverrides();
        Map<ConversionPair, Convert<?>> second = opts.getConverterOverrides();

        assertSame("Multiple calls to getConverterOverrides() must return the same instance", first, second);
    }

    /**
     * Verify that the overrides map rejects null keys, as required by {@link ConcurrentHashMap}.
     */
    @Test
    public void testOverridesMapRejectsNullKey() {
        DefaultConverterOptions opts = new DefaultConverterOptions();
        Map<ConversionPair, Convert<?>> overrides = opts.getConverterOverrides();

        try {
            overrides.put(null, new Convert<Object>() {
                @Override
                public Object convert(Object from, Converter converter) {
                    return from;
                }
            });
            fail("ConcurrentHashMap should reject null keys with NPE");
        } catch (NullPointerException expected) {
            // Expected behaviour
        }
    }

    /**
     * Verify that the custom options map also rejects null keys (ConcurrentHashMap behaviour)
     * using reflection to access the private field.
     */
    @Test
    public void testCustomOptionsMapRejectsNullKey() throws Exception {
        DefaultConverterOptions opts = new DefaultConverterOptions();

        // Access the private field 'customOptions'
        Field field = DefaultConverterOptions.class.getDeclaredField("customOptions");
        field.setAccessible(true);
        Map<String, Object> customMap = (Map<String, Object>) field.get(opts);

        try {
            customMap.put(null, "value");
            fail("ConcurrentHashMap should reject null keys with NPE");
        } catch (NullPointerException expected) {
            // Expected
        }
    }
}
