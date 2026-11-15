package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unit tests for {@link DefaultConverterOptions}.
 *
 * The class under test has no public mutator methods, therefore the tests
 * manipulate the internal {@code customOptions} and {@code converterOverrides}
 * maps via reflection.  This approach verifies that the getters return the
 * underlying mutable structures and that the generic type handling of
 * {@code getCustomOption} works as expected.
 */
public class DefaultConverterOptionsTest {

    private DefaultConverterOptions options;
    private Field customOptionsField;
    private Field converterOverridesField;

    @Before
    public void setUp() throws Exception {
        options = new DefaultConverterOptions();

        // Obtain private fields for direct manipulation.
        customOptionsField = DefaultConverterOptions.class.getDeclaredField("customOptions");
        customOptionsField.setAccessible(true);

        converterOverridesField = DefaultConverterOptions.class.getDeclaredField("converterOverrides");
        converterOverridesField.setAccessible(true);
    }

    /** Helper to retrieve the internal customOptions map. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getCustomOptionsMap() throws IllegalAccessException {
        return (Map<String, Object>) customOptionsField.get(options);
    }

    /** Helper to retrieve the internal converterOverrides map. */
    @SuppressWarnings("unchecked")
    private Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> getConverterOverridesMap() throws IllegalAccessException {
        return (Map<Map.Entry<Class<?>, Class<?>>, Convert<?>>) converterOverridesField.get(options);
    }

    // ----------------------------------------------------------------------
    // Basic contract tests
    // ----------------------------------------------------------------------

    @Test
    public void testInitialState_isEmpty() {
        // No custom options should be present.
        assertNull("Unknown custom option should return null", options.getCustomOption("any"));

        // The overrides map should be empty but mutable.
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> overrides = options.getConverterOverrides();
        assertNotNull("Overrides map must never be null", overrides);
        assertTrue("Overrides map should be empty on construction", overrides.isEmpty());
    }

    // ----------------------------------------------------------------------
    // Custom options handling (via reflection)
    // ----------------------------------------------------------------------

    @Test
    public void testGetCustomOption_returnsStoredValue() throws Exception {
        Map<String, Object> map = getCustomOptionsMap();
        map.put("stringOption", "hello");
        map.put("intOption", 42);

        // Retrieve with the generic method – the cast is performed inside the method.
        String stringResult = options.getCustomOption("stringOption");
        Integer intResult = options.getCustomOption("intOption");

        assertEquals("hello", stringResult);
        assertEquals(Integer.valueOf(42), intResult);
    }

    @Test
    public void testGetCustomOption_overwritesExistingValue() throws Exception {
        Map<String, Object> map = getCustomOptionsMap();
        map.put("dupKey", "first");
        assertEquals("first", options.getCustomOption("dupKey"));

        // Overwrite with a new value.
        map.put("dupKey", "second");
        assertEquals("second", options.getCustomOption("dupKey"));
    }

    @Test(expected = NullPointerException.class)
    public void testCustomOptionsMapRejectsNullKey() throws Exception {
        // ConcurrentHashMap does not allow null keys; the exception should bubble up.
        Map<String, Object> map = getCustomOptionsMap();
        map.put(null, "value");
    }

    @Test(expected = NullPointerException.class)
    public void testCustomOptionsMapRejectsNullValue() throws Exception {
        // ConcurrentHashMap does not allow null values; the exception should bubble up.
        Map<String, Object> map = getCustomOptionsMap();
        map.put("nullValue", null);
    }

    // ----------------------------------------------------------------------
    // Converter overrides handling
    // ----------------------------------------------------------------------

    @Test
    public void testGetConverterOverrides_returnsMutableMap() {
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> overrides = options.getConverterOverrides();

        // Verify that the map returned is the same instance used internally.
        try {
            Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> internal = getConverterOverridesMap();
            assertSame("Returned map should be the internal map instance", internal, overrides);
        } catch (Exception e) {
            fail("Reflection access to internal map failed: " + e);
        }

        // Create a simple Convert implementation that appends a suffix.
        Convert<String> appendConvert = new Convert<String>() {
            @Override
            public String convert(Object from, Converter converter) {
                return from.toString() + "_converted";
            }
        };

        // Build the key entry: source type = Integer, target type = String.
        Map.Entry<Class<?>, Class<?>> key = new AbstractMap.SimpleEntry<>(Integer.class, String.class);

        // Put the override into the map.
        overrides.put(key, appendConvert);

        // Verify the map now contains the entry.
        assertEquals("Overrides map should contain one entry", 1, overrides.size());
        assertTrue("Overrides map must contain the inserted key", overrides.containsKey(key));

        // Retrieve the Convert and invoke it.
        @SuppressWarnings("unchecked")
        Convert<String> retrieved = (Convert<String>) overrides.get(key);
        String result = retrieved.convert(10, null); // 'converter' argument not used by our implementation.
        assertEquals("10_converted", result);
    }

    @Test
    public void testMultipleConverterOverrides_areStoredIndependently() {
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> overrides = options.getConverterOverrides();

        Convert<Integer> identityConvert = new Convert<Integer>() {
            @Override
            public Integer convert(Object from, Converter converter) {
                return (Integer) from;
            }
        };

        Map.Entry<Class<?>, Class<?>> key1 = new AbstractMap.SimpleEntry<>(String.class, Integer.class);
        Map.Entry<Class<?>, Class<?>> key2 = new AbstractMap.SimpleEntry<>(Double.class, Integer.class);

        overrides.put(key1, identityConvert);
        overrides.put(key2, identityConvert);

        assertEquals("Two distinct overrides should be present", 2, overrides.size());
        assertTrue(overrides.containsKey(key1));
        assertTrue(overrides.containsKey(key2));
    }

    // ----------------------------------------------------------------------
    // Defensive checks – ensure that the internal maps are indeed ConcurrentHashMap
    // ----------------------------------------------------------------------

    @Test
    public void testInternalMaps_areConcurrentHashMapInstances() throws Exception {
        assertTrue("customOptions should be a ConcurrentHashMap",
                ConcurrentHashMap.class.isInstance(getCustomOptionsMap()));
        assertTrue("converterOverrides should be a ConcurrentHashMap",
                ConcurrentHashMap.class.isInstance(getConverterOverridesMap()));
    }
}
