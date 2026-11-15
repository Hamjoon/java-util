package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.Assert.*;

public class DefaultConverterOptionsTest {

    private DefaultConverterOptions options;

    @Before
    public void setUp() {
        options = new DefaultConverterOptions();
    }

    /*** Helper to access the private customOptions map via reflection ***/
    @SuppressWarnings("unchecked")
    private ConcurrentMap<String, Object> getCustomOptionsMap() throws Exception {
        Field field = DefaultConverterOptions.class.getDeclaredField("customOptions");
        field.setAccessible(true);
        return (ConcurrentMap<String, Object>) field.get(options);
    }

    /*** 1. Basic functionality tests ***/

    @Test
    public void testConstructorInitialState() {
        // No custom options set yet – any lookup should return null
        assertNull(options.getCustomOption("anyKey"));

        // Overrides map must be empty
        Map<?, ?> overrides = options.getConverterOverrides();
        assertNotNull(overrides);
        assertTrue(overrides.isEmpty());
    }

    @Test
    public void testGetCustomOptionReturnsStoredValue() throws Exception {
        ConcurrentMap<String, Object> map = getCustomOptionsMap();
        map.put("myKey", "myValue");

        String result = options.getCustomOption("myKey");
        assertEquals("myValue", result);
    }

    @Test
    public void testGetConverterOverridesIsEmptyInitially() {
        assertTrue(options.getConverterOverrides().isEmpty());
    }

    @Test
    public void testGetConverterOverridesIsLiveMap() {
        Map.Entry<Class<?>, Class<?>> key = new AbstractMap.SimpleEntry<>(String.class, Integer.class);
        Convert<Integer> dummyConvert = new Convert<Integer>() {
            @Override
            public Integer convert(Object from, Converter converter) {
                return 42;
            }
        };

        options.getConverterOverrides().put(key, dummyConvert);
        assertFalse(options.getConverterOverrides().isEmpty());
        assertSame(dummyConvert, options.getConverterOverrides().get(key));
    }

    @Test
    public void testGetConverterOverridesReturnsSameInstance() {
        Map<?, ?> first = options.getConverterOverrides();
        Map<?, ?> second = options.getConverterOverrides();
        assertSame(first, second);
    }

    /*** 2. Edge‑case and exception tests ***/

    @Test
    public void testGetCustomOptionWithNullNameThrowsNPE() {
        try {
            options.getCustomOption(null);
            fail("Expected NullPointerException when name is null");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void testGetCustomOptionMissingKeyReturnsNull() {
        assertNull(options.getCustomOption("nonExistingKey"));
    }

    @Test
    public void testGetCustomOptionTypeMismatchThrowsClassCastException() throws Exception {
        ConcurrentMap<String, Object> map = getCustomOptionsMap();
        map.put("number", Integer.valueOf(123));

        try {
            // Attempt to retrieve the Integer as a String – should throw ClassCastException
            String value = options.getCustomOption("number");
            fail("Expected ClassCastException but got value: " + value);
        } catch (ClassCastException expected) {
            // expected
        }
    }

    @Test
    public void testConverterOverridesRejectsNullKey() {
        Convert<String> dummy = (from, converter) -> from.toString();
        try {
            options.getConverterOverrides().put(null, dummy);
            fail("Expected NullPointerException when putting null key");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void testConverterOverridesRejectsNullValue() {
        Map.Entry<Class<?>, Class<?>> key = new AbstractMap.SimpleEntry<>(String.class, Integer.class);
        try {
            options.getConverterOverrides().put(key, null);
            fail("Expected NullPointerException when putting null value");
        } catch (NullPointerException expected) {
            // expected
        }
    }
}
