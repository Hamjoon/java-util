package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.convert.Converter.ConversionPair;


import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.Assert.*;

public class DefaultConverterOptionsTest {

    private DefaultConverterOptions options;

    @Before
    public void setUp() {
        options = new DefaultConverterOptions();
    }

    /**
     * Verify that a newly created instance contains non‑null, empty maps
     * and that a request for an unknown custom option returns {@code null}.
     */
    @Test
    public void testInitialState() {
        assertNotNull("converterOverrides map should be non‑null", options.getConverterOverrides());
        assertTrue("converterOverrides map should be empty initially", options.getConverterOverrides().isEmpty());
        assertNull("custom option for unknown key should be null", options.getCustomOption("nonexistent"));
    }

    /**
     * Insert a value directly into the private {@code customOptions} map
     * via reflection and verify that {@code getCustomOption} returns it.
     */
    @Test
    public void testGetCustomOptionWithValidEntry() throws Exception {
        Field field = DefaultConverterOptions.class.getDeclaredField("customOptions");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> customMap = (Map<String, Object>) field.get(options);

        customMap.put("key1", 123);
        Integer result = options.getCustomOption("key1");
        assertEquals(Integer.valueOf(123), result);
    }

    /**
     * Passing {@code null} as the option name should simply return {@code null}
     * (ConcurrentHashMap permits a {@code null} key lookup, it just returns {@code null}).
     */
    @Test
    public void testGetCustomOptionWithNullKey() {
        assertNull("getCustomOption(null) should return null", options.getCustomOption(null));
    }

    /**
     * If the stored value cannot be cast to the requested generic type,
     * a {@link ClassCastException} is expected.
     */
    @Test(expected = ClassCastException.class)
    public void testGetCustomOptionTypeMismatch() throws Exception {
        Field field = DefaultConverterOptions.class.getDeclaredField("customOptions");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> customMap = (Map<String, Object>) field.get(options);

        customMap.put("stringKey", "value");
        // The following cast to Integer should fail
        Integer ignored = options.getCustomOption("stringKey");
    }

    /**
     * The map returned by {@code getConverterOverrides()} must be the live
     * internal map – modifications through the returned reference must be
     * visible on subsequent calls.
     */
    @Test
    public void testConverterOverridesMapIsLive() {
        Map<ConversionPair, Convert<?>> overrides = options.getConverterOverrides();

        ConversionPair pair = new ConversionPair(String.class, Integer.class);
        Convert<Integer> convert = new Convert<Integer>() {
            @Override
            public Integer convert(Object from, Converter converter) {
                return Integer.valueOf(from.toString());
            }
        };

        overrides.put(pair, convert);

        assertTrue("Overrides map should contain the inserted entry", overrides.containsKey(pair));
        assertEquals("Stored Convert instance should be retrievable", convert,
                options.getConverterOverrides().get(pair));
    }

    /**
     * {@code ConcurrentHashMap} does not allow {@code null} keys – attempting to
     * put a {@code null} key must raise {@link NullPointerException}.
     */
    @Test(expected = NullPointerException.class)
    public void testConverterOverridesRejectNullKey() {
        options.getConverterOverrides().put(null,
                (Convert<Object>) (from, converter) -> null);
    }

    /**
     * {@code ConcurrentHashMap} does not allow {@code null} values – attempting to
     * put a {@code null} value must raise {@link NullPointerException}.
     */
    @Test(expected = NullPointerException.class)
    public void testConverterOverridesRejectNullValue() {
        ConversionPair pair = new ConversionPair(Integer.class, String.class);
        options.getConverterOverrides().put(pair, null);
    }
}
